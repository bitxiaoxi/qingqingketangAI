package com.qingqingketang.student.web;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.qingqingketang.student.entity.Student;
import com.qingqingketang.student.entity.StudentPayment;
import com.qingqingketang.student.entity.StudentSchedule;
import com.qingqingketang.student.mapper.StudentMapper;
import com.qingqingketang.student.mapper.StudentPaymentMapper;
import com.qingqingketang.student.mapper.StudentScheduleMapper;
import com.qingqingketang.student.service.ScheduleAssistantService;
import com.qingqingketang.student.service.StudentScheduleService;
import com.qingqingketang.student.service.StudentService;
import com.qingqingketang.student.web.dto.LessonBalanceSummary;
import com.qingqingketang.student.web.dto.PaymentSummary;
import com.qingqingketang.student.web.dto.ScheduleCountSummary;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/schedules")
@CrossOrigin(origins = "http://localhost:5173", maxAge = 3600)
public class StudentScheduleController {

    private static final String DEFAULT_SUBJECT = "英语";
    private static final String STATUS_PLANNED = "PLANNED";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final StudentService studentService;
    private final StudentMapper studentMapper;
    private final StudentScheduleService studentScheduleService;
    private final StudentScheduleMapper studentScheduleMapper;
    private final StudentPaymentMapper studentPaymentMapper;
    private final ScheduleAssistantService scheduleAssistantService;

    public StudentScheduleController(StudentService studentService,
                                     StudentMapper studentMapper,
                                     StudentScheduleService studentScheduleService,
                                     StudentScheduleMapper studentScheduleMapper,
                                     StudentPaymentMapper studentPaymentMapper,
                                     ScheduleAssistantService scheduleAssistantService) {
        this.studentService = studentService;
        this.studentMapper = studentMapper;
        this.studentScheduleService = studentScheduleService;
        this.studentScheduleMapper = studentScheduleMapper;
        this.studentPaymentMapper = studentPaymentMapper;
        this.scheduleAssistantService = scheduleAssistantService;
    }

    /**
     * 根据开始日期 + 每周上课日自动生成课表：支持每周 1 到 7 次课。
     */
    @PostMapping("/students/{studentId}/auto-generate")
    @Transactional
    @ResponseStatus(HttpStatus.CREATED)
    public List<ScheduleView> autoGenerate(@PathVariable Long studentId,
                                           @Valid @RequestBody ScheduleGenerateRequest request) {
        Student student = lockStudent(studentId);
        LessonBalanceSummary balance = loadLessonBalance(studentId);
        if (balance.getRemainingLessons() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该学生暂无剩余课时，请先录入学费和课时信息");
        }

        int schedulableLessons = balance.getSchedulableLessons();
        if (schedulableLessons <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该学生剩余课时已全部排入课表，请先调整现有排课或新增课时");
        }

        LocalDate startDate = resolveStartDate(request);
        Student sameClassStudent = resolveSameClassStudent(studentId, request.getSameClassStudentId());

        List<StudentSchedule> schedules = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        if (sameClassStudent != null) {
            List<WeeklySlotTemplate> slotTemplates = resolveSameClassTemplates(sameClassStudent);
            List<GeneratedSlot> generatedSlots = generateSameClassSlots(
                    sameClassStudent,
                    startDate,
                    slotTemplates,
                    schedulableLessons);
            for (GeneratedSlot generatedSlot : generatedSlots) {
                validateNoUnexpectedSameClassConflict(studentId, generatedSlot.getStartTime(), generatedSlot.getEndTime());

                StudentSchedule schedule = new StudentSchedule();
                schedule.setStudentId(studentId);
                schedule.setSubject(StringUtils.hasText(generatedSlot.getSubject()) ? generatedSlot.getSubject() : DEFAULT_SUBJECT);
                schedule.setStartTime(generatedSlot.getStartTime());
                schedule.setEndTime(generatedSlot.getEndTime());
                schedule.setStatus(STATUS_PLANNED);
                schedule.setCreatedAt(now);
                schedules.add(schedule);
            }
        } else {
            List<Integer> weekdayValues = resolveWeekdays(request);
            LocalTime startTime = requireStartTime(request);
            LocalTime endTime = requireEndTime(request);
            if (!endTime.isAfter(startTime)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "结束时间需晚于开始时间");
            }

            List<LocalDate> lessonDates = generateLessonDates(startDate, weekdayValues, schedulableLessons);
            for (LocalDate currentDate : lessonDates) {
                StudentSchedule schedule = new StudentSchedule();
                schedule.setStudentId(studentId);
                schedule.setSubject(DEFAULT_SUBJECT);
                LocalDateTime slotStart = LocalDateTime.of(currentDate, startTime);
                LocalDateTime slotEnd = LocalDateTime.of(currentDate, endTime);
                StudentSchedule conflict = studentScheduleMapper.findFirstConflict(slotStart, slotEnd);
                if (conflict != null) {
                    throw buildConflictException(conflict);
                }
                schedule.setStartTime(slotStart);
                schedule.setEndTime(slotEnd);
                schedule.setStatus(STATUS_PLANNED);
                schedule.setCreatedAt(now);
                schedules.add(schedule);
            }
        }

        studentScheduleService.saveBatch(schedules);
        return schedules.stream()
                .map(schedule -> toView(schedule, student.getName(), student.getGrade()))
                .collect(Collectors.toList());
    }

    private List<Integer> resolveWeekdays(ScheduleGenerateRequest request) {
        TreeSet<Integer> selected = new TreeSet<>();
        if (!CollectionUtils.isEmpty(request.getWeekdays())) {
            selected.addAll(request.getWeekdays());
        } else if (request.getWeekday() != null) {
            selected.add(request.getWeekday());
        }
        if (selected.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请至少选择一个上课日");
        }
        for (Integer weekday : selected) {
            if (weekday == null || weekday < 1 || weekday > 7) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择有效的周几");
            }
        }
        return new ArrayList<>(selected);
    }

    private LocalDate resolveStartDate(ScheduleGenerateRequest request) {
        if (request.getStartDate() != null) {
            return request.getStartDate();
        }
        if (request.getFirstLessonDate() != null) {
            return request.getFirstLessonDate();
        }
        return LocalDate.now();
    }

    private List<LocalDate> generateLessonDates(LocalDate startDate, List<Integer> weekdayValues, int totalLessons) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate weekStart = startDate.minusDays(startDate.getDayOfWeek().getValue() - 1L);
        while (dates.size() < totalLessons) {
            for (Integer weekdayValue : weekdayValues) {
                LocalDate candidate = weekStart.plusDays(weekdayValue - 1L);
                if (candidate.isBefore(startDate)) {
                    continue;
                }
                dates.add(candidate);
                if (dates.size() >= totalLessons) {
                    break;
                }
            }
            weekStart = weekStart.plusWeeks(1);
        }
        return dates;
    }

    private LocalTime requireStartTime(ScheduleGenerateRequest request) {
        if (request == null || request.getStartTime() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择开始时间");
        }
        return request.getStartTime();
    }

    private LocalTime requireEndTime(ScheduleGenerateRequest request) {
        if (request == null || request.getEndTime() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择结束时间");
        }
        return request.getEndTime();
    }

    private Student resolveSameClassStudent(Long studentId, Long sameClassStudentId) {
        if (sameClassStudentId == null) {
            return null;
        }
        if (sameClassStudentId.equals(studentId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不能把学生加入自己的同班课程，请重新选择");
        }
        return requireStudent(sameClassStudentId);
    }

    private List<WeeklySlotTemplate> resolveSameClassTemplates(Student sameClassStudent) {
        if (sameClassStudent == null) {
            return Collections.emptyList();
        }
        List<StudentSchedule> plannedSchedules = studentScheduleMapper.findPlannedByStudentId(sameClassStudent.getId());
        if (CollectionUtils.isEmpty(plannedSchedules)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    String.format("%s 当前还没有可参考的待上课表，请先为该学生生成正式课表", sameClassStudent.getName()));
        }

        return resolveRecurringTemplates(plannedSchedules);
    }

    private List<WeeklySlotTemplate> resolveRecurringTemplates(List<StudentSchedule> plannedSchedules) {
        if (CollectionUtils.isEmpty(plannedSchedules)) {
            return Collections.emptyList();
        }

        Map<String, SlotTemplateCounter> templateCounterMap = new LinkedHashMap<>();
        for (StudentSchedule schedule : plannedSchedules) {
            if (schedule.getStartTime() == null || schedule.getEndTime() == null) {
                continue;
            }
            WeeklySlotTemplate template = WeeklySlotTemplate.fromSchedule(schedule);
            String key = template.uniqueKey();
            SlotTemplateCounter counter = templateCounterMap.get(key);
            if (counter == null) {
                templateCounterMap.put(key, new SlotTemplateCounter(template, 1));
            } else {
                counter.increment();
            }
        }

        List<WeeklySlotTemplate> recurringTemplates = templateCounterMap.values().stream()
                .filter(counter -> counter.getCount() >= 2)
                .map(SlotTemplateCounter::getTemplate)
                .sorted(Comparator.comparingInt(WeeklySlotTemplate::getWeekday)
                        .thenComparing(WeeklySlotTemplate::getStartTime))
                .collect(Collectors.toList());
        if (!recurringTemplates.isEmpty()) {
            return recurringTemplates;
        }

        return templateCounterMap.values().stream()
                .map(SlotTemplateCounter::getTemplate)
                .sorted(Comparator.comparingInt(WeeklySlotTemplate::getWeekday)
                        .thenComparing(WeeklySlotTemplate::getStartTime))
                .collect(Collectors.toList());
    }

    private List<GeneratedSlot> generateSameClassSlots(Student sameClassStudent,
                                                       LocalDate startDate,
                                                       List<WeeklySlotTemplate> slotTemplates,
                                                       int totalLessons) {
        if (sameClassStudent == null || CollectionUtils.isEmpty(slotTemplates)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "未找到可沿用的同班上课时间");
        }

        return generateSlotsFromTemplates(startDate, slotTemplates, totalLessons);
    }

    private List<GeneratedSlot> generateSlotsFromTemplates(LocalDate startDate,
                                                           List<WeeklySlotTemplate> slotTemplates,
                                                           int totalLessons) {
        if (startDate == null || CollectionUtils.isEmpty(slotTemplates) || totalLessons <= 0) {
            return Collections.emptyList();
        }

        List<GeneratedSlot> generatedSlots = new ArrayList<>();
        LocalDate weekStart = startDate.minusDays(startDate.getDayOfWeek().getValue() - 1L);
        while (generatedSlots.size() < totalLessons) {
            for (WeeklySlotTemplate template : slotTemplates) {
                LocalDate candidateDate = weekStart.plusDays(template.getWeekday() - 1L);
                if (candidateDate.isBefore(startDate)) {
                    continue;
                }
                generatedSlots.add(new GeneratedSlot(
                        LocalDateTime.of(candidateDate, template.getStartTime()),
                        LocalDateTime.of(candidateDate, template.getEndTime()),
                        template.getSubject()));
                if (generatedSlots.size() >= totalLessons) {
                    break;
                }
            }
            weekStart = weekStart.plusWeeks(1);
        }
        return generatedSlots;
    }

    private GeneratedSlot generateNextRecurringSlot(StudentSchedule lastSchedule,
                                                    List<WeeklySlotTemplate> slotTemplates) {
        if (lastSchedule == null || lastSchedule.getStartTime() == null || lastSchedule.getEndTime() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "当前课表缺少可顺延的末尾课程");
        }
        List<GeneratedSlot> generatedSlots = generateSlotsFromTemplates(
                lastSchedule.getStartTime().toLocalDate().plusDays(1),
                slotTemplates,
                1);
        if (CollectionUtils.isEmpty(generatedSlots)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "未找到可顺延到课表末尾的课程时段");
        }
        return generatedSlots.get(0);
    }

    private void validateNoUnexpectedSameClassConflict(Long targetStudentId,
                                                       LocalDateTime targetStart,
                                                       LocalDateTime targetEnd) {
        validateNoUnexpectedConflict(
                targetStudentId,
                targetStart,
                targetEnd,
                null,
                "该学生在 %s 已经有同一时段课程，请勿重复加入同班");
    }

    /**
     * 查询某周课表：前端传入周一日期，服务端返回该周内所有排课。
     */
    @GetMapping("/week")
    public List<ScheduleView> findWeekSchedules(
            @RequestParam("start")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = startDate.plusDays(6).atTime(LocalTime.MAX);
        // 查询当前周内所有课程记录
        List<StudentSchedule> schedules = studentScheduleMapper.findWithin(start, end);
        if (CollectionUtils.isEmpty(schedules)) {
            return Collections.emptyList();
        }
        // 一次性查出涉及到的学生，避免 N+1
        Set<Long> studentIds = schedules.stream()
                .map(StudentSchedule::getStudentId)
                .collect(Collectors.toSet());
        Map<Long, Student> studentMap = studentService.list(Wrappers.<Student>lambdaQuery()
                        .in(Student::getId, studentIds))
                .stream()
                .collect(Collectors.toMap(Student::getId, s -> s));

        return schedules.stream()
                .map(schedule -> {
                    Student s = studentMap.get(schedule.getStudentId());
                    String studentName = s != null ? s.getName() : "未知学生";
                    String studentGrade = s != null ? s.getGrade() : "";
                    return toView(schedule, studentName, studentGrade);
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/students/{studentId}/planned")
    public List<ScheduleView> listStudentPlannedSchedules(@PathVariable Long studentId) {
        Student student = studentService.getById(studentId);
        if (student == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "学生不存在");
        }

        return studentScheduleMapper.findPlannedByStudentId(studentId).stream()
                .map(schedule -> toView(schedule, student.getName(), student.getGrade()))
                .collect(Collectors.toList());
    }

    /**
     * 销课：将指定排课记录标记为完成，并扣减剩余课时。
     */
    @PostMapping("/{scheduleId}/complete")
    @Transactional
    public ScheduleView completeSchedule(@PathVariable Long scheduleId) {
        LockedScheduleContext context = lockScheduleContext(scheduleId);
        StudentSchedule schedule = context.getSchedule();
        Student student = context.getStudent();

        if (!STATUS_COMPLETED.equalsIgnoreCase(schedule.getStatus())) {
            LessonBalanceSummary balance = loadLessonBalance(schedule.getStudentId());
            if (balance.getRemainingLessons() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该学生剩余课时不足，无法销课");
            }
            StudentPayment payment = studentPaymentMapper.findNextConsumablePayment(schedule.getStudentId());
            if (payment == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该学生暂无可核销的缴费批次，请先录入有效课时");
            }

            LocalDateTime now = LocalDateTime.now();
            schedule.setPaymentId(payment.getId());
            schedule.setLessonPrice(payment.getAvgFeePerLesson() == null ? BigDecimal.ZERO : payment.getAvgFeePerLesson());
            schedule.setConsumedAt(now);
            schedule.setStatus(STATUS_COMPLETED);
            studentScheduleService.updateById(schedule);
        }

        return toView(schedule, student.getName(), student.getGrade());
    }

    /**
     * 撤销销课：把排课恢复为待上课，并回滚销课明细与剩余课时。
     */
    @PostMapping("/{scheduleId}/undo-complete")
    @Transactional
    public ScheduleView undoCompleteSchedule(@PathVariable Long scheduleId) {
        LockedScheduleContext context = lockScheduleContext(scheduleId);
        StudentSchedule schedule = context.getSchedule();
        Student student = context.getStudent();

        if (STATUS_COMPLETED.equalsIgnoreCase(schedule.getStatus())) {
            schedule.setStatus(STATUS_PLANNED);
            schedule.setPaymentId(null);
            schedule.setLessonPrice(null);
            schedule.setConsumedAt(null);
            studentScheduleService.updateById(schedule);
        }

        return toView(schedule, student.getName(), student.getGrade());
    }

    @PostMapping("/students/{studentId}/temporary-lesson")
    @Transactional
    public TemporaryAdjustmentView createTemporaryLesson(@PathVariable Long studentId,
                                                         @Valid @RequestBody ScheduleSlotRequest request) {
        Student student = lockStudent(studentId);
        validateSlotRequest(request);

        StudentSchedule lastSchedule = studentScheduleMapper.findLastPlannedByStudentId(studentId);
        if (lastSchedule == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该学生当前没有可替换的待上课程，请先生成正式课表");
        }

        LocalDateTime targetStart = LocalDateTime.of(request.getLessonDate(), request.getStartTime());
        LocalDateTime targetEnd = LocalDateTime.of(request.getLessonDate(), request.getEndTime());
        if (sameSlot(lastSchedule, targetStart, targetEnd)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "临时加课时间与课表最后一节课完全相同，请直接使用课程改时间");
        }
        validateNoUnexpectedConflict(
                studentId,
                targetStart,
                targetEnd,
                null,
                "该学生在 %s 已经有同一时段课程，请勿重复排课");

        StudentSchedule addedSchedule = new StudentSchedule();
        addedSchedule.setStudentId(studentId);
        addedSchedule.setSubject(StringUtils.hasText(lastSchedule.getSubject()) ? lastSchedule.getSubject() : DEFAULT_SUBJECT);
        addedSchedule.setStartTime(targetStart);
        addedSchedule.setEndTime(targetEnd);
        addedSchedule.setStatus(STATUS_PLANNED);
        addedSchedule.setCreatedAt(LocalDateTime.now());
        studentScheduleService.save(addedSchedule);
        studentScheduleService.removeById(lastSchedule.getId());

        TemporaryAdjustmentView view = new TemporaryAdjustmentView();
        view.setStudentId(studentId);
        view.setStudentName(student.getName());
        view.setMessage(String.format(
                "已为 %s 补入 %s 的课程，并移除课表末尾的 %s。",
                student.getName(),
                formatSlot(targetStart, targetEnd),
                formatSlot(lastSchedule.getStartTime(), lastSchedule.getEndTime())));
        view.setAddedSchedule(toView(addedSchedule, student.getName(), student.getGrade()));
        view.setRemovedSchedule(toView(lastSchedule, student.getName(), student.getGrade()));
        return view;
    }

    @PostMapping("/students/{studentId}/leave")
    @Transactional
    public TemporaryAdjustmentView delayLatestPlannedLesson(@PathVariable Long studentId) {
        Student student = lockStudent(studentId);
        List<StudentSchedule> plannedSchedules = studentScheduleMapper.findPlannedByStudentId(studentId);
        if (CollectionUtils.isEmpty(plannedSchedules)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该学生当前没有待上课程，无法执行请假顺延");
        }

        StudentSchedule removedSchedule = plannedSchedules.get(0);
        StudentSchedule lastSchedule = plannedSchedules.get(plannedSchedules.size() - 1);
        List<WeeklySlotTemplate> slotTemplates = resolveRecurringTemplates(plannedSchedules);
        if (CollectionUtils.isEmpty(slotTemplates)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "当前课表缺少可参考的固定时段，无法自动顺延");
        }

        GeneratedSlot generatedSlot = generateNextRecurringSlot(lastSchedule, slotTemplates);
        validateNoUnexpectedConflict(
                studentId,
                generatedSlot.getStartTime(),
                generatedSlot.getEndTime(),
                null,
                "该学生在 %s 已经有同一时段课程，请勿重复排课");

        StudentSchedule addedSchedule = new StudentSchedule();
        addedSchedule.setStudentId(studentId);
        addedSchedule.setSubject(StringUtils.hasText(generatedSlot.getSubject())
                ? generatedSlot.getSubject()
                : (StringUtils.hasText(removedSchedule.getSubject()) ? removedSchedule.getSubject() : DEFAULT_SUBJECT));
        addedSchedule.setStartTime(generatedSlot.getStartTime());
        addedSchedule.setEndTime(generatedSlot.getEndTime());
        addedSchedule.setStatus(STATUS_PLANNED);
        addedSchedule.setCreatedAt(LocalDateTime.now());
        studentScheduleService.save(addedSchedule);
        studentScheduleService.removeById(removedSchedule.getId());

        TemporaryAdjustmentView view = new TemporaryAdjustmentView();
        view.setStudentId(studentId);
        view.setStudentName(student.getName());
        view.setMessage(String.format(
                "已为 %s 请假顺延 1 节，移除最近待上课的 %s，并在课表末尾补入 %s。",
                student.getName(),
                formatSlot(removedSchedule.getStartTime(), removedSchedule.getEndTime()),
                formatSlot(addedSchedule.getStartTime(), addedSchedule.getEndTime())));
        view.setAddedSchedule(toView(addedSchedule, student.getName(), student.getGrade()));
        view.setRemovedSchedule(toView(removedSchedule, student.getName(), student.getGrade()));
        return view;
    }

    @PostMapping("/{scheduleId}/reschedule")
    @Transactional
    public ScheduleView rescheduleSchedule(@PathVariable Long scheduleId,
                                           @Valid @RequestBody ScheduleSlotRequest request) {
        validateSlotRequest(request);

        LockedScheduleContext context = lockScheduleContext(scheduleId);
        StudentSchedule schedule = context.getSchedule();
        if (!STATUS_PLANNED.equalsIgnoreCase(schedule.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "只有待上课程可以改时间，如需调整已销课程请先撤销销课");
        }

        Student student = context.getStudent();
        LocalDateTime targetStart = LocalDateTime.of(request.getLessonDate(), request.getStartTime());
        LocalDateTime targetEnd = LocalDateTime.of(request.getLessonDate(), request.getEndTime());
        if (sameSlot(schedule, targetStart, targetEnd)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "新时间与原课程时间一致，无需改动");
        }

        validateNoUnexpectedConflict(
                schedule.getStudentId(),
                targetStart,
                targetEnd,
                scheduleId,
                "该学生在 %s 已经有同一时段课程，请勿重复排课");

        schedule.setStartTime(targetStart);
        schedule.setEndTime(targetEnd);
        studentScheduleService.updateById(schedule);
        return toView(schedule, student.getName(), student.getGrade());
    }

    @PostMapping("/{scheduleId}/reschedule-following")
    @Transactional
    public FutureAdjustmentView rescheduleFollowingSchedules(@PathVariable Long scheduleId,
                                                             @Valid @RequestBody ScheduleSlotRequest request) {
        validateSlotRequest(request);

        LockedScheduleContext context = lockScheduleContext(scheduleId);
        StudentSchedule referenceSchedule = context.getSchedule();
        if (!STATUS_PLANNED.equalsIgnoreCase(referenceSchedule.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "只有待上课程可以批量改时间，已销课程不受影响");
        }

        Student student = context.getStudent();
        LocalDateTime targetStart = LocalDateTime.of(request.getLessonDate(), request.getStartTime());
        LocalDateTime targetEnd = LocalDateTime.of(request.getLessonDate(), request.getEndTime());
        if (sameSlot(referenceSchedule, targetStart, targetEnd)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "新时间与生效起点课程一致，无需批量改动");
        }

        List<StudentSchedule> followingSchedules = studentScheduleMapper.findPlannedByStudentId(referenceSchedule.getStudentId())
                .stream()
                .filter(schedule -> isFollowingRecurringSchedule(referenceSchedule, schedule))
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(followingSchedules)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "未找到可批量调整的后续待上课程");
        }

        String referenceSlot = formatSlot(referenceSchedule.getStartTime(), referenceSchedule.getEndTime());
        List<ShiftedScheduleSlot> shiftedSchedules = followingSchedules.stream()
                .map(schedule -> buildShiftedScheduleSlot(referenceSchedule, schedule, targetStart, targetEnd))
                .collect(Collectors.toList());
        validateShiftedScheduleConflicts(shiftedSchedules);

        shiftedSchedules.forEach(shiftedSchedule -> {
            shiftedSchedule.getSchedule().setStartTime(shiftedSchedule.getTargetStart());
            shiftedSchedule.getSchedule().setEndTime(shiftedSchedule.getTargetEnd());
            studentScheduleService.updateById(shiftedSchedule.getSchedule());
        });

        FutureAdjustmentView view = new FutureAdjustmentView();
        view.setStudentId(student.getId());
        view.setStudentName(student.getName());
        view.setUpdatedCount(shiftedSchedules.size());
        view.setMessage(String.format(
                "已将 %s 从 %s 开始的 %d 节后续待上课程统一改时间，首节已调整为 %s，已销课课程不受影响。",
                student.getName(),
                referenceSlot,
                shiftedSchedules.size(),
                formatSlot(targetStart, targetEnd)));
        view.setUpdatedSchedules(shiftedSchedules.stream()
                .map(shiftedSchedule -> toView(shiftedSchedule.getSchedule(), student.getName(), student.getGrade()))
                .collect(Collectors.toList()));
        return view;
    }

    /**
     * 自然语言排课：接收聊天消息，由 AI/规则解析意图后复用现有自动排课能力。
     */
    @PostMapping("/assistant/arrange")
    public AssistantArrangeResponse arrangeByAssistant(@Valid @RequestBody AssistantArrangeRequest request) {
        List<Student> students = studentService.list(
                Wrappers.<Student>lambdaQuery().orderByAsc(Student::getName)
        );
        ScheduleAssistantService.AssistantInterpretation interpretation =
                scheduleAssistantService.interpret(request.getMessages(), students);

        AssistantArrangeResponse response = new AssistantArrangeResponse();
        response.setAnalysisMode(interpretation.getAnalysisMode());
        response.setReply(interpretation.getReply());
        response.setWarnings(interpretation.getWarnings());
        response.setParsedIntent(toParsedIntentView(interpretation));

        if (!"AUTO_GENERATE_SCHEDULE".equalsIgnoreCase(interpretation.getIntent())) {
            return response;
        }

        StudentResolution resolution = resolveStudent(interpretation.getStudentName(), students);
        if (resolution.getStudent() == null) {
            response.getParsedIntent().setIntent("ASK_CLARIFICATION");
            addMissingField(response.getParsedIntent(), "学生姓名");
            response.setReply(buildAssistantClarificationReply(response.getParsedIntent(), resolution.getMessage(), false));
            if (StringUtils.hasText(resolution.getMessage())) {
                response.getWarnings().add(resolution.getMessage());
            }
            return response;
        }

        if (CollectionUtils.isEmpty(interpretation.getWeekdays())
                || interpretation.getStartDate() == null
                || interpretation.getStartTime() == null
                || interpretation.getEndTime() == null) {
            response.getParsedIntent().setIntent("ASK_CLARIFICATION");
            if (CollectionUtils.isEmpty(interpretation.getWeekdays())) {
                addMissingField(response.getParsedIntent(), "每周上课日");
            }
            if (interpretation.getStartDate() == null) {
                addMissingField(response.getParsedIntent(), "开始日期");
            }
            if (interpretation.getStartTime() == null || interpretation.getEndTime() == null) {
                addMissingField(response.getParsedIntent(), "上课时间");
            }
            response.setReply(buildAssistantClarificationReply(
                    response.getParsedIntent(),
                    "我已经理解这是正式课排课请求，但还不能直接生成课表。",
                    true));
            return response;
        }

        ScheduleGenerateRequest generateRequest = new ScheduleGenerateRequest();
        generateRequest.setWeekdays(interpretation.getWeekdays());
        generateRequest.setStartDate(interpretation.getStartDate());
        generateRequest.setStartTime(interpretation.getStartTime());
        generateRequest.setEndTime(interpretation.getEndTime());

        try {
            List<ScheduleView> schedules = autoGenerate(resolution.getStudent().getId(), generateRequest);
            response.setScheduled(true);
            response.setScheduledCount(schedules.size());
            response.setGeneratedSchedules(schedules);
            response.setResolvedStudentName(resolution.getStudent().getName());
            if (!StringUtils.hasText(response.getReply())) {
                response.setReply(String.format(
                        "已为 %s 从 %s 开始安排每周 %s %s-%s 的课程，共生成 %d 节课。",
                        resolution.getStudent().getName(),
                        interpretation.getStartDate(),
                        formatWeekdays(interpretation.getWeekdays()),
                        interpretation.getStartTime(),
                        interpretation.getEndTime(),
                        schedules.size()));
            }
        } catch (ResponseStatusException error) {
            response.setReply(error.getReason() == null ? "排课失败" : error.getReason());
        }

        return response;
    }

    private Student requireStudent(Long studentId) {
        Student student = studentService.getById(studentId);
        if (student == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "学生不存在");
        }
        return student;
    }

    private Student lockStudent(Long studentId) {
        Student student = studentMapper.findByIdForUpdate(studentId);
        if (student == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "学生不存在");
        }
        return student;
    }

    private LockedScheduleContext lockScheduleContext(Long scheduleId) {
        StudentSchedule schedule = studentScheduleService.getById(scheduleId);
        if (schedule == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "课程不存在");
        }
        Student student = lockStudent(schedule.getStudentId());
        StudentSchedule lockedSchedule = studentScheduleService.getById(scheduleId);
        if (lockedSchedule == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "课程不存在");
        }
        return new LockedScheduleContext(student, lockedSchedule);
    }

    private LessonBalanceSummary loadLessonBalance(Long studentId) {
        List<PaymentSummary> paymentSummaries = studentPaymentMapper.sumByStudentIds(Collections.singletonList(studentId));
        PaymentSummary paymentSummary = paymentSummaries.isEmpty() ? null : paymentSummaries.get(0);
        List<ScheduleCountSummary> scheduleCountSummaries = studentScheduleMapper.sumByStudentIds(Collections.singletonList(studentId));
        ScheduleCountSummary scheduleCountSummary = scheduleCountSummaries.isEmpty() ? null : scheduleCountSummaries.get(0);
        return LessonBalanceSummary.from(paymentSummary, scheduleCountSummary);
    }

    private void validateSlotRequest(ScheduleSlotRequest request) {
        if (request == null || request.getLessonDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择课程日期");
        }
        if (request.getStartTime() == null || request.getEndTime() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请完整选择上课时间");
        }
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "结束时间需晚于开始时间");
        }
    }

    private boolean sameSlot(StudentSchedule schedule, LocalDateTime targetStart, LocalDateTime targetEnd) {
        return schedule != null
                && targetStart != null
                && targetEnd != null
                && targetStart.equals(schedule.getStartTime())
                && targetEnd.equals(schedule.getEndTime());
    }

    private void validateNoUnexpectedConflict(Long targetStudentId,
                                              LocalDateTime targetStart,
                                              LocalDateTime targetEnd,
                                              Long excludeScheduleId,
                                              String sameSlotErrorTemplate) {
        StudentSchedule unexpectedConflict = findUnexpectedConflict(
                targetStudentId,
                targetStart,
                targetEnd,
                excludeScheduleId);
        if (unexpectedConflict == null) {
            return;
        }
        if (sameSlot(unexpectedConflict, targetStart, targetEnd)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    String.format(sameSlotErrorTemplate, formatSlot(targetStart, targetEnd)));
        }
        throw buildConflictException(unexpectedConflict);
    }

    private StudentSchedule findUnexpectedConflict(Long targetStudentId,
                                                   LocalDateTime targetStart,
                                                   LocalDateTime targetEnd,
                                                   Long excludeScheduleId) {
        return studentScheduleMapper.findConflicts(targetStart, targetEnd).stream()
                .filter(conflict -> excludeScheduleId == null || !excludeScheduleId.equals(conflict.getId()))
                .filter(conflict -> isUnexpectedConflict(conflict, targetStudentId, targetStart, targetEnd))
                .findFirst()
                .orElse(null);
    }

    private boolean isUnexpectedConflict(StudentSchedule conflict,
                                         Long targetStudentId,
                                         LocalDateTime targetStart,
                                         LocalDateTime targetEnd) {
        if (sameSlot(conflict, targetStart, targetEnd)) {
            return conflict.getStudentId() != null && conflict.getStudentId().equals(targetStudentId);
        }
        return true;
    }

    private boolean isFollowingRecurringSchedule(StudentSchedule referenceSchedule, StudentSchedule candidate) {
        return referenceSchedule != null
                && candidate != null
                && referenceSchedule.getId() != null
                && candidate.getId() != null
                && referenceSchedule.getStartTime() != null
                && referenceSchedule.getEndTime() != null
                && candidate.getStartTime() != null
                && candidate.getEndTime() != null
                && candidate.getStudentId() != null
                && candidate.getStudentId().equals(referenceSchedule.getStudentId())
                && !candidate.getStartTime().isBefore(referenceSchedule.getStartTime())
                && candidate.getStartTime().getDayOfWeek().equals(referenceSchedule.getStartTime().getDayOfWeek())
                && candidate.getStartTime().toLocalTime().equals(referenceSchedule.getStartTime().toLocalTime())
                && candidate.getEndTime().toLocalTime().equals(referenceSchedule.getEndTime().toLocalTime())
                && Objects.equals(candidate.getSubject(), referenceSchedule.getSubject());
    }

    private ShiftedScheduleSlot buildShiftedScheduleSlot(StudentSchedule referenceSchedule,
                                                         StudentSchedule schedule,
                                                         LocalDateTime targetStart,
                                                         LocalDateTime targetEnd) {
        long dayOffset = ChronoUnit.DAYS.between(
                referenceSchedule.getStartTime().toLocalDate(),
                schedule.getStartTime().toLocalDate());
        return new ShiftedScheduleSlot(
                schedule,
                targetStart.plusDays(dayOffset),
                targetEnd.plusDays(dayOffset));
    }

    private void validateShiftedScheduleConflicts(List<ShiftedScheduleSlot> shiftedSchedules) {
        for (ShiftedScheduleSlot shiftedSchedule : shiftedSchedules) {
            validateNoUnexpectedConflict(
                    shiftedSchedule.getSchedule().getStudentId(),
                    shiftedSchedule.getTargetStart(),
                    shiftedSchedule.getTargetEnd(),
                    shiftedSchedule.getSchedule().getId(),
                    "该学生在 %s 已经有同一时段课程，请勿重复排课");
        }
    }

    private ResponseStatusException buildConflictException(StudentSchedule conflict) {
        Student holder = studentService.getById(conflict.getStudentId());
        String holderName = holder != null ? holder.getName() : "其他学生";
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                String.format("与 %s 的课程冲突（%s），请调整时间", holderName, formatSlot(conflict.getStartTime(), conflict.getEndTime())));
    }

    private String formatSlot(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            return "";
        }
        return startTime.format(DATE_FORMATTER) + " "
                + startTime.format(TIME_FORMATTER) + " - "
                + endTime.format(TIME_FORMATTER);
    }

    /**
     * 把排课实体转换为前端展示对象。
     */
    private ScheduleView toView(StudentSchedule schedule,
                                String studentName,
                                String studentGrade) {
        ScheduleView view = new ScheduleView();
        view.setId(schedule.getId());
        view.setStudentId(schedule.getStudentId());
        view.setStudentName(studentName);
        view.setStudentGrade(studentGrade);
        view.setSubject(schedule.getSubject());
        view.setStartTime(schedule.getStartTime());
        view.setEndTime(schedule.getEndTime());
        view.setStatus(schedule.getStatus());
        view.setLessonPrice(schedule.getLessonPrice());
        return view;
    }

    private StudentResolution resolveStudent(String studentName, List<Student> students) {
        if (!StringUtils.hasText(studentName)) {
            return new StudentResolution(null, "还没有识别到学生姓名，请在消息里明确写出学生名字。");
        }
        if (CollectionUtils.isEmpty(students)) {
            return new StudentResolution(null, "当前还没有已录入学生，暂时无法通过自然语言排课。");
        }

        String normalizedTarget = normalizeStudentName(studentName);
        List<Student> exactMatches = students.stream()
                .filter(student -> normalizedTarget.equals(normalizeStudentName(student.getName())))
                .collect(Collectors.toList());
        if (exactMatches.size() == 1) {
            return new StudentResolution(exactMatches.get(0), null);
        }
        if (exactMatches.size() > 1) {
            return new StudentResolution(null, "存在重名学生，请先在学生管理中区分姓名后再使用 AI 排课。");
        }

        List<Student> fuzzyMatches = students.stream()
                .filter(student -> normalizeStudentName(student.getName()).contains(normalizedTarget)
                        || normalizedTarget.contains(normalizeStudentName(student.getName())))
                .collect(Collectors.toList());
        if (fuzzyMatches.size() == 1) {
            return new StudentResolution(fuzzyMatches.get(0), null);
        }
        if (fuzzyMatches.size() > 1) {
            String candidates = fuzzyMatches.stream()
                    .map(Student::getName)
                    .distinct()
                    .collect(Collectors.joining("、"));
            return new StudentResolution(null, "匹配到多个学生：" + candidates + "，请把姓名说得更完整一些。");
        }
        return new StudentResolution(null, "没有找到学生“" + studentName + "”，请确认学生已录入并检查姓名。");
    }

    private String normalizeStudentName(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.replaceAll("[\\s·•，,。；;：:（）()]", "");
    }

    private String formatWeekdays(List<Integer> weekdays) {
        if (CollectionUtils.isEmpty(weekdays)) {
            return "";
        }
        return weekdays.stream()
                .map(weekday -> {
                    switch (weekday) {
                        case 1:
                            return "周一";
                        case 2:
                            return "周二";
                        case 3:
                            return "周三";
                        case 4:
                            return "周四";
                        case 5:
                            return "周五";
                        case 6:
                            return "周六";
                        case 7:
                            return "周日";
                        default:
                            return "";
                    }
                })
                .filter(StringUtils::hasText)
                .collect(Collectors.joining("、"));
    }

    private void addMissingField(ParsedIntentView parsedIntentView, String field) {
        if (parsedIntentView == null || !StringUtils.hasText(field)) {
            return;
        }
        if (!parsedIntentView.getMissingFields().contains(field)) {
            parsedIntentView.getMissingFields().add(field);
        }
    }

    private static class LockedScheduleContext {
        private final Student student;
        private final StudentSchedule schedule;

        private LockedScheduleContext(Student student, StudentSchedule schedule) {
            this.student = student;
            this.schedule = schedule;
        }

        private Student getStudent() {
            return student;
        }

        private StudentSchedule getSchedule() {
            return schedule;
        }
    }

    private String buildAssistantClarificationReply(ParsedIntentView parsedIntentView,
                                                    String issueMessage,
                                                    boolean includeStudentName) {
        StringBuilder reply = new StringBuilder();
        String recognizedSummary = buildRecognizedSummary(parsedIntentView, includeStudentName);
        if (StringUtils.hasText(recognizedSummary)) {
            reply.append("我已经记住：").append(recognizedSummary).append("。");
        }

        if (StringUtils.hasText(issueMessage)) {
            reply.append(issueMessage);
            if (!issueMessage.endsWith("。")) {
                reply.append("。");
            }
        } else if (!CollectionUtils.isEmpty(parsedIntentView.getMissingFields())) {
            reply.append("还差").append(String.join("、", parsedIntentView.getMissingFields())).append("。");
        }

        if (!CollectionUtils.isEmpty(parsedIntentView.getMissingFields())) {
            if (parsedIntentView.getMissingFields().size() == 1) {
                reply.append(buildSingleFieldPrompt(parsedIntentView.getMissingFields().get(0)));
            } else {
                reply.append("你可以分开发，我会继续补全，不需要把整句话重新发一遍。");
            }
        }
        return reply.toString();
    }

    private String buildRecognizedSummary(ParsedIntentView parsedIntentView, boolean includeStudentName) {
        if (parsedIntentView == null) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        if (includeStudentName && StringUtils.hasText(parsedIntentView.getStudentName())) {
            parts.add("学生 " + parsedIntentView.getStudentName());
        }
        if (!CollectionUtils.isEmpty(parsedIntentView.getWeekdays())) {
            parts.add("每周 " + formatWeekdays(parsedIntentView.getWeekdays()));
        }
        if (parsedIntentView.getStartTime() != null && parsedIntentView.getEndTime() != null) {
            parts.add("时间 " + parsedIntentView.getStartTime() + "-" + parsedIntentView.getEndTime());
        }
        if (parsedIntentView.getStartDate() != null) {
            parts.add("从 " + parsedIntentView.getStartDate() + " 开始");
        }
        return String.join("，", parts);
    }

    private String buildSingleFieldPrompt(String missingField) {
        switch (missingField) {
            case "学生姓名":
                return "直接回复准确的学生姓名即可。";
            case "每周上课日":
                return "直接回复例如“周二、周四”即可。";
            case "开始日期":
                return "直接回复例如“从 2026-03-16 开始”或“下周一开始”。";
            case "上课时间":
                return "直接回复例如“19:00-20:30”即可。";
            default:
                return "请继续补充。";
        }
    }

    private ParsedIntentView toParsedIntentView(ScheduleAssistantService.AssistantInterpretation interpretation) {
        ParsedIntentView parsedIntentView = new ParsedIntentView();
        parsedIntentView.setIntent(interpretation.getIntent());
        parsedIntentView.setStudentName(interpretation.getStudentName());
        parsedIntentView.setWeekdays(interpretation.getWeekdays());
        parsedIntentView.setStartDate(interpretation.getStartDate());
        parsedIntentView.setStartTime(interpretation.getStartTime());
        parsedIntentView.setEndTime(interpretation.getEndTime());
        parsedIntentView.setMissingFields(interpretation.getMissingFields());
        return parsedIntentView;
    }

    /**
     * 排课请求体：支持单周几兼容模式，也支持每周多个上课日。
     */
    public static class ScheduleGenerateRequest {
        private List<Integer> weekdays;

        private Integer weekday;

        private Long sameClassStudentId;

        private LocalDate startDate;

        private LocalDate firstLessonDate;

        private LocalTime startTime;

        private LocalTime endTime;

        public List<Integer> getWeekdays() {
            return weekdays;
        }

        public void setWeekdays(List<Integer> weekdays) {
            this.weekdays = weekdays;
        }

        public Integer getWeekday() {
            return weekday;
        }

        public void setWeekday(Integer weekday) {
            this.weekday = weekday;
        }

        public Long getSameClassStudentId() {
            return sameClassStudentId;
        }

        public void setSameClassStudentId(Long sameClassStudentId) {
            this.sameClassStudentId = sameClassStudentId;
        }

        public LocalDate getStartDate() {
            return startDate;
        }

        public void setStartDate(LocalDate startDate) {
            this.startDate = startDate;
        }

        public LocalDate getFirstLessonDate() {
            return firstLessonDate;
        }

        public void setFirstLessonDate(LocalDate firstLessonDate) {
            this.firstLessonDate = firstLessonDate;
        }

        public LocalTime getStartTime() {
            return startTime;
        }

        public void setStartTime(LocalTime startTime) {
            this.startTime = startTime;
        }

        public LocalTime getEndTime() {
            return endTime;
        }

        public void setEndTime(LocalTime endTime) {
            this.endTime = endTime;
        }
    }

    public static class ScheduleSlotRequest {
        @NotNull(message = "请选择课程日期")
        private LocalDate lessonDate;

        @NotNull(message = "请选择开始时间")
        private LocalTime startTime;

        @NotNull(message = "请选择结束时间")
        private LocalTime endTime;

        public LocalDate getLessonDate() {
            return lessonDate;
        }

        public void setLessonDate(LocalDate lessonDate) {
            this.lessonDate = lessonDate;
        }

        public LocalTime getStartTime() {
            return startTime;
        }

        public void setStartTime(LocalTime startTime) {
            this.startTime = startTime;
        }

        public LocalTime getEndTime() {
            return endTime;
        }

        public void setEndTime(LocalTime endTime) {
            this.endTime = endTime;
        }
    }

    public static class AssistantArrangeRequest {
        @NotNull(message = "消息不能为空")
        private List<ScheduleAssistantService.ConversationMessage> messages;

        public List<ScheduleAssistantService.ConversationMessage> getMessages() {
            return messages;
        }

        public void setMessages(List<ScheduleAssistantService.ConversationMessage> messages) {
            this.messages = messages;
        }
    }

    public static class AssistantArrangeResponse {
        private String reply;
        private String analysisMode;
        private boolean scheduled;
        private int scheduledCount;
        private String resolvedStudentName;
        private List<String> warnings = new ArrayList<>();
        private ParsedIntentView parsedIntent;
        private List<ScheduleView> generatedSchedules = new ArrayList<>();

        public String getReply() {
            return reply;
        }

        public void setReply(String reply) {
            this.reply = reply;
        }

        public String getAnalysisMode() {
            return analysisMode;
        }

        public void setAnalysisMode(String analysisMode) {
            this.analysisMode = analysisMode;
        }

        public boolean isScheduled() {
            return scheduled;
        }

        public void setScheduled(boolean scheduled) {
            this.scheduled = scheduled;
        }

        public int getScheduledCount() {
            return scheduledCount;
        }

        public void setScheduledCount(int scheduledCount) {
            this.scheduledCount = scheduledCount;
        }

        public String getResolvedStudentName() {
            return resolvedStudentName;
        }

        public void setResolvedStudentName(String resolvedStudentName) {
            this.resolvedStudentName = resolvedStudentName;
        }

        public List<String> getWarnings() {
            return warnings;
        }

        public void setWarnings(List<String> warnings) {
            this.warnings = warnings == null ? new ArrayList<String>() : warnings;
        }

        public ParsedIntentView getParsedIntent() {
            return parsedIntent;
        }

        public void setParsedIntent(ParsedIntentView parsedIntent) {
            this.parsedIntent = parsedIntent;
        }

        public List<ScheduleView> getGeneratedSchedules() {
            return generatedSchedules;
        }

        public void setGeneratedSchedules(List<ScheduleView> generatedSchedules) {
            this.generatedSchedules = generatedSchedules == null ? new ArrayList<ScheduleView>() : generatedSchedules;
        }
    }

    public static class TemporaryAdjustmentView {
        private Long studentId;
        private String studentName;
        private String message;
        private ScheduleView addedSchedule;
        private ScheduleView removedSchedule;

        public Long getStudentId() {
            return studentId;
        }

        public void setStudentId(Long studentId) {
            this.studentId = studentId;
        }

        public String getStudentName() {
            return studentName;
        }

        public void setStudentName(String studentName) {
            this.studentName = studentName;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public ScheduleView getAddedSchedule() {
            return addedSchedule;
        }

        public void setAddedSchedule(ScheduleView addedSchedule) {
            this.addedSchedule = addedSchedule;
        }

        public ScheduleView getRemovedSchedule() {
            return removedSchedule;
        }

        public void setRemovedSchedule(ScheduleView removedSchedule) {
            this.removedSchedule = removedSchedule;
        }
    }

    public static class FutureAdjustmentView {
        private Long studentId;
        private String studentName;
        private String message;
        private int updatedCount;
        private List<ScheduleView> updatedSchedules = new ArrayList<>();

        public Long getStudentId() {
            return studentId;
        }

        public void setStudentId(Long studentId) {
            this.studentId = studentId;
        }

        public String getStudentName() {
            return studentName;
        }

        public void setStudentName(String studentName) {
            this.studentName = studentName;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public int getUpdatedCount() {
            return updatedCount;
        }

        public void setUpdatedCount(int updatedCount) {
            this.updatedCount = updatedCount;
        }

        public List<ScheduleView> getUpdatedSchedules() {
            return updatedSchedules;
        }

        public void setUpdatedSchedules(List<ScheduleView> updatedSchedules) {
            this.updatedSchedules = updatedSchedules == null ? new ArrayList<ScheduleView>() : updatedSchedules;
        }
    }

    public static class ParsedIntentView {
        private String intent;
        private String studentName;
        private List<Integer> weekdays = new ArrayList<>();
        private LocalDate startDate;
        private LocalTime startTime;
        private LocalTime endTime;
        private List<String> missingFields = new ArrayList<>();

        public String getIntent() {
            return intent;
        }

        public void setIntent(String intent) {
            this.intent = intent;
        }

        public String getStudentName() {
            return studentName;
        }

        public void setStudentName(String studentName) {
            this.studentName = studentName;
        }

        public List<Integer> getWeekdays() {
            return weekdays;
        }

        public void setWeekdays(List<Integer> weekdays) {
            this.weekdays = weekdays == null ? new ArrayList<Integer>() : weekdays;
        }

        public LocalDate getStartDate() {
            return startDate;
        }

        public void setStartDate(LocalDate startDate) {
            this.startDate = startDate;
        }

        public LocalTime getStartTime() {
            return startTime;
        }

        public void setStartTime(LocalTime startTime) {
            this.startTime = startTime;
        }

        public LocalTime getEndTime() {
            return endTime;
        }

        public void setEndTime(LocalTime endTime) {
            this.endTime = endTime;
        }

        public List<String> getMissingFields() {
            return missingFields;
        }

        public void setMissingFields(List<String> missingFields) {
            this.missingFields = missingFields == null ? new ArrayList<String>() : missingFields;
        }
    }

    private static class StudentResolution {
        private final Student student;
        private final String message;

        private StudentResolution(Student student, String message) {
            this.student = student;
            this.message = message;
        }

        public Student getStudent() {
            return student;
        }

        public String getMessage() {
            return message;
        }
    }

    private static class SlotTemplateCounter {
        private final WeeklySlotTemplate template;
        private int count;

        private SlotTemplateCounter(WeeklySlotTemplate template, int count) {
            this.template = template;
            this.count = count;
        }

        public WeeklySlotTemplate getTemplate() {
            return template;
        }

        public int getCount() {
            return count;
        }

        public void increment() {
            this.count += 1;
        }
    }

    private static class WeeklySlotTemplate {
        private final int weekday;
        private final LocalTime startTime;
        private final LocalTime endTime;
        private final String subject;

        private WeeklySlotTemplate(int weekday, LocalTime startTime, LocalTime endTime, String subject) {
            this.weekday = weekday;
            this.startTime = startTime;
            this.endTime = endTime;
            this.subject = subject;
        }

        public static WeeklySlotTemplate fromSchedule(StudentSchedule schedule) {
            return new WeeklySlotTemplate(
                    schedule.getStartTime().getDayOfWeek().getValue(),
                    schedule.getStartTime().toLocalTime(),
                    schedule.getEndTime().toLocalTime(),
                    schedule.getSubject());
        }

        public int getWeekday() {
            return weekday;
        }

        public LocalTime getStartTime() {
            return startTime;
        }

        public LocalTime getEndTime() {
            return endTime;
        }

        public String getSubject() {
            return subject;
        }

        public String uniqueKey() {
            return weekday + "|" + startTime + "|" + endTime + "|" + (subject == null ? "" : subject);
        }
    }

    private static class GeneratedSlot {
        private final LocalDateTime startTime;
        private final LocalDateTime endTime;
        private final String subject;

        private GeneratedSlot(LocalDateTime startTime, LocalDateTime endTime, String subject) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.subject = subject;
        }

        public LocalDateTime getStartTime() {
            return startTime;
        }

        public LocalDateTime getEndTime() {
            return endTime;
        }

        public String getSubject() {
            return subject;
        }
    }

    private static class ShiftedScheduleSlot {
        private final StudentSchedule schedule;
        private final LocalDateTime targetStart;
        private final LocalDateTime targetEnd;

        private ShiftedScheduleSlot(StudentSchedule schedule, LocalDateTime targetStart, LocalDateTime targetEnd) {
            this.schedule = schedule;
            this.targetStart = targetStart;
            this.targetEnd = targetEnd;
        }

        public StudentSchedule getSchedule() {
            return schedule;
        }

        public LocalDateTime getTargetStart() {
            return targetStart;
        }

        public LocalDateTime getTargetEnd() {
            return targetEnd;
        }
    }

    public static class ScheduleView {
        private Long id;
        private Long studentId;
        private String studentName;
        private String studentGrade;
        private String subject;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String status;
        private BigDecimal lessonPrice;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getStudentId() {
            return studentId;
        }

        public void setStudentId(Long studentId) {
            this.studentId = studentId;
        }

        public String getStudentName() {
            return studentName;
        }

        public void setStudentName(String studentName) {
            this.studentName = studentName;
        }

        public String getStudentGrade() {
            return studentGrade;
        }

        public void setStudentGrade(String studentGrade) {
            this.studentGrade = studentGrade;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public LocalDateTime getStartTime() {
            return startTime;
        }

        public void setStartTime(LocalDateTime startTime) {
            this.startTime = startTime;
        }

        public LocalDateTime getEndTime() {
            return endTime;
        }

        public void setEndTime(LocalDateTime endTime) {
            this.endTime = endTime;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public BigDecimal getLessonPrice() {
            return lessonPrice;
        }

        public void setLessonPrice(BigDecimal lessonPrice) {
            this.lessonPrice = lessonPrice;
        }
    }
}
