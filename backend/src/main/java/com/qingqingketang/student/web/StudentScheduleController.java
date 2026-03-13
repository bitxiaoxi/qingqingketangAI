package com.qingqingketang.student.web;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.qingqingketang.student.entity.Student;
import com.qingqingketang.student.entity.StudentLessonBalance;
import com.qingqingketang.student.entity.StudentLessonConsumption;
import com.qingqingketang.student.entity.StudentPayment;
import com.qingqingketang.student.entity.StudentSchedule;
import com.qingqingketang.student.mapper.StudentLessonConsumptionMapper;
import com.qingqingketang.student.mapper.StudentPaymentMapper;
import com.qingqingketang.student.mapper.StudentScheduleMapper;
import com.qingqingketang.student.service.StudentLessonBalanceService;
import com.qingqingketang.student.service.StudentLessonConsumptionService;
import com.qingqingketang.student.service.StudentScheduleService;
import com.qingqingketang.student.service.StudentService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
    private final StudentScheduleService studentScheduleService;
    private final StudentScheduleMapper studentScheduleMapper;
    private final StudentLessonBalanceService studentLessonBalanceService;
    private final StudentPaymentMapper studentPaymentMapper;
    private final StudentLessonConsumptionMapper studentLessonConsumptionMapper;
    private final StudentLessonConsumptionService studentLessonConsumptionService;

    public StudentScheduleController(StudentService studentService,
                                     StudentScheduleService studentScheduleService,
                                     StudentScheduleMapper studentScheduleMapper,
                                     StudentLessonBalanceService studentLessonBalanceService,
                                     StudentPaymentMapper studentPaymentMapper,
                                     StudentLessonConsumptionMapper studentLessonConsumptionMapper,
                                     StudentLessonConsumptionService studentLessonConsumptionService) {
        this.studentService = studentService;
        this.studentScheduleService = studentScheduleService;
        this.studentScheduleMapper = studentScheduleMapper;
        this.studentLessonBalanceService = studentLessonBalanceService;
        this.studentPaymentMapper = studentPaymentMapper;
        this.studentLessonConsumptionMapper = studentLessonConsumptionMapper;
        this.studentLessonConsumptionService = studentLessonConsumptionService;
    }

    /**
     * 根据开始日期 + 每周上课日自动生成课表：支持每周 1 到 7 次课。
     */
    @PostMapping("/students/{studentId}/auto-generate")
    @ResponseStatus(HttpStatus.CREATED)
    public List<ScheduleView> autoGenerate(@PathVariable Long studentId,
                                           @Valid @RequestBody ScheduleGenerateRequest request) {
        Student student = studentService.getById(studentId);
        if (student == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "学生不存在");
        }

        // 读取课时余额来确定需要生成的总课次数
        StudentLessonBalance balance = studentLessonBalanceService.getOne(
                Wrappers.<StudentLessonBalance>lambdaQuery().eq(StudentLessonBalance::getStudentId, studentId));
        int remainingLessons = balance != null && balance.getRemainingLessons() != null
                ? balance.getRemainingLessons()
                : 0;
        if (remainingLessons <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该学生暂无剩余课时，请先录入学费和课时信息");
        }

        List<Integer> weekdayValues = resolveWeekdays(request);
        LocalDate startDate = resolveStartDate(request);
        LocalTime startTime = request.getStartTime();
        LocalTime endTime = request.getEndTime();
        if (!endTime.isAfter(startTime)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "结束时间需晚于开始时间");
        }

        List<StudentSchedule> schedules = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        List<LocalDate> lessonDates = generateLessonDates(startDate, weekdayValues, remainingLessons);
        for (LocalDate currentDate : lessonDates) {
            StudentSchedule schedule = new StudentSchedule();
            schedule.setStudentId(studentId);
            schedule.setSubject(DEFAULT_SUBJECT);
            LocalDateTime slotStart = LocalDateTime.of(currentDate, startTime);
            LocalDateTime slotEnd = LocalDateTime.of(currentDate, endTime);
            StudentSchedule conflict = studentScheduleMapper.findFirstConflict(slotStart, slotEnd);
            if (conflict != null) {
                Student holder = studentService.getById(conflict.getStudentId());
                String holderName = holder != null ? holder.getName() : "其他学生";
                String conflictDate = conflict.getStartTime().format(DATE_FORMATTER);
                String conflictRange = conflict.getStartTime().format(TIME_FORMATTER) + " - "
                        + conflict.getEndTime().format(TIME_FORMATTER);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        String.format("与 %s 的课程冲突（%s %s），请调整排课时间", holderName, conflictDate, conflictRange));
            }
            schedule.setStartTime(slotStart);
            schedule.setEndTime(slotEnd);
            schedule.setStatus(STATUS_PLANNED);
            schedule.setCreatedAt(now);
            schedules.add(schedule);
        }

        studentScheduleService.saveBatch(schedules);
        return schedules.stream()
                .map(schedule -> toView(schedule, student.getName(), null))
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
        List<Long> scheduleIds = schedules.stream()
                .map(StudentSchedule::getId)
                .collect(Collectors.toList());
        Map<Long, Student> studentMap = studentService.list(Wrappers.<Student>lambdaQuery()
                        .in(Student::getId, studentIds))
                .stream()
                .collect(Collectors.toMap(Student::getId, s -> s));
        Map<Long, StudentLessonConsumption> consumptionMap = studentLessonConsumptionMapper.findByScheduleIds(scheduleIds)
                .stream()
                .collect(Collectors.toMap(StudentLessonConsumption::getScheduleId, c -> c));

        return schedules.stream()
                .map(schedule -> {
                    Student s = studentMap.get(schedule.getStudentId());
                    String studentName = s != null ? s.getName() : "未知学生";
                    return toView(schedule, studentName, consumptionMap.get(schedule.getId()));
                })
                .collect(Collectors.toList());
    }

    /**
     * 销课：将指定排课记录标记为完成，并扣减剩余课时。
     */
    @PostMapping("/{scheduleId}/complete")
    @Transactional
    public ScheduleView completeSchedule(@PathVariable Long scheduleId) {
        StudentSchedule schedule = studentScheduleService.getById(scheduleId);
        if (schedule == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "课程不存在");
        }

        Student student = studentService.getById(schedule.getStudentId());
        if (student == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "关联学生不存在");
        }

        StudentLessonConsumption consumption = studentLessonConsumptionMapper.findByScheduleId(scheduleId);
        if (!STATUS_COMPLETED.equalsIgnoreCase(schedule.getStatus())) {
            StudentLessonBalance balance = studentLessonBalanceService.getOne(
                    Wrappers.<StudentLessonBalance>lambdaQuery().eq(StudentLessonBalance::getStudentId, schedule.getStudentId()));
            if (balance == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该学生尚未录入课时信息");
            }
            int remaining = balance.getRemainingLessons() == null ? 0 : balance.getRemainingLessons();
            if (remaining <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该学生剩余课时不足，无法销课");
            }
            StudentPayment payment = studentPaymentMapper.findNextConsumablePayment(schedule.getStudentId());
            if (payment == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该学生暂无可核销的缴费批次，请先录入有效课时");
            }

            LocalDateTime now = LocalDateTime.now();
            if (consumption == null) {
                consumption = new StudentLessonConsumption();
                consumption.setScheduleId(scheduleId);
                consumption.setStudentId(schedule.getStudentId());
                consumption.setPaymentId(payment.getId());
                consumption.setLessonPrice(payment.getAvgFeePerLesson() == null ? BigDecimal.ZERO : payment.getAvgFeePerLesson());
                consumption.setConsumedAt(now);
                studentLessonConsumptionService.save(consumption);
            }

            balance.setRemainingLessons(remaining - 1);
            balance.setUpdatedAt(now);
            studentLessonBalanceService.updateById(balance);

            schedule.setStatus(STATUS_COMPLETED);
            studentScheduleService.updateById(schedule);
        }

        return toView(schedule, student.getName(), consumption);
    }

    /**
     * 撤销销课：把排课恢复为待上课，并回滚销课明细与剩余课时。
     */
    @PostMapping("/{scheduleId}/undo-complete")
    @Transactional
    public ScheduleView undoCompleteSchedule(@PathVariable Long scheduleId) {
        StudentSchedule schedule = studentScheduleService.getById(scheduleId);
        if (schedule == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "课程不存在");
        }

        Student student = studentService.getById(schedule.getStudentId());
        if (student == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "关联学生不存在");
        }

        StudentLessonConsumption consumption = studentLessonConsumptionMapper.findByScheduleId(scheduleId);
        if (STATUS_COMPLETED.equalsIgnoreCase(schedule.getStatus())) {
            LocalDateTime now = LocalDateTime.now();
            StudentLessonBalance balance = studentLessonBalanceService.getOne(
                    Wrappers.<StudentLessonBalance>lambdaQuery().eq(StudentLessonBalance::getStudentId, schedule.getStudentId()));
            if (balance == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该学生尚未录入课时信息");
            }

            int remaining = balance.getRemainingLessons() == null ? 0 : balance.getRemainingLessons();
            balance.setRemainingLessons(remaining + 1);
            balance.setUpdatedAt(now);
            studentLessonBalanceService.updateById(balance);

            if (consumption != null) {
                studentLessonConsumptionService.removeById(consumption.getId());
                consumption = null;
            }

            schedule.setStatus(STATUS_PLANNED);
            studentScheduleService.updateById(schedule);
        }

        return toView(schedule, student.getName(), consumption);
    }

    /**
     * 把排课实体转换为前端展示对象。
     */
    private ScheduleView toView(StudentSchedule schedule, String studentName, StudentLessonConsumption consumption) {
        ScheduleView view = new ScheduleView();
        view.setId(schedule.getId());
        view.setStudentId(schedule.getStudentId());
        view.setStudentName(studentName);
        view.setSubject(schedule.getSubject());
        view.setStartTime(schedule.getStartTime());
        view.setEndTime(schedule.getEndTime());
        view.setStatus(schedule.getStatus());
        view.setLessonPrice(consumption != null ? consumption.getLessonPrice() : null);
        return view;
    }

    /**
     * 排课请求体：支持单周几兼容模式，也支持每周多个上课日。
     */
    public static class ScheduleGenerateRequest {
        private List<Integer> weekdays;

        private Integer weekday;

        private LocalDate startDate;

        private LocalDate firstLessonDate;

        @NotNull(message = "请选择开始时间")
        private LocalTime startTime;

        @NotNull(message = "请选择结束时间")
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

    public static class ScheduleView {
        private Long id;
        private Long studentId;
        private String studentName;
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
