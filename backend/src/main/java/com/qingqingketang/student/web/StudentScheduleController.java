package com.qingqingketang.student.web;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.qingqingketang.student.entity.Student;
import com.qingqingketang.student.entity.StudentLessonBalance;
import com.qingqingketang.student.entity.StudentSchedule;
import com.qingqingketang.student.mapper.StudentScheduleMapper;
import com.qingqingketang.student.service.StudentLessonBalanceService;
import com.qingqingketang.student.service.StudentScheduleService;
import com.qingqingketang.student.service.StudentService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/schedules")
@CrossOrigin(origins = "http://localhost:5173", maxAge = 3600)
public class StudentScheduleController {

    private static final String DEFAULT_SUBJECT = "英语";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final StudentService studentService;
    private final StudentScheduleService studentScheduleService;
    private final StudentScheduleMapper studentScheduleMapper;
    private final StudentLessonBalanceService studentLessonBalanceService;

    public StudentScheduleController(StudentService studentService,
                                     StudentScheduleService studentScheduleService,
                                     StudentScheduleMapper studentScheduleMapper,
                                     StudentLessonBalanceService studentLessonBalanceService) {
        this.studentService = studentService;
        this.studentScheduleService = studentScheduleService;
        this.studentScheduleMapper = studentScheduleMapper;
        this.studentLessonBalanceService = studentLessonBalanceService;
    }

    /**
     * 根据周几+时段自动生成课表：读取剩余课时并按周循环排满。
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

        int weekdayValue = request.getWeekday();
        if (weekdayValue < 1 || weekdayValue > 7) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择有效的周几");
        }
        // 如果前端指定了第一节课日期，则以该日期为基准，否则按照今天推算最近的一次
        LocalDate lessonDate = request.getFirstLessonDate();
        if (lessonDate == null) {
            LocalDate today = LocalDate.now();
            int todayValue = today.getDayOfWeek().getValue();
            int offset = weekdayValue - todayValue;
            if (offset < 0) {
                offset += 7;
            }
            lessonDate = today.plusDays(offset);
        } else {
            int providedWeekday = lessonDate.getDayOfWeek().getValue();
            if (providedWeekday != weekdayValue) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "第一节日期与选择的周几不一致");
            }
        }
        LocalTime startTime = request.getStartTime();
        LocalTime endTime = request.getEndTime();
        if (!endTime.isAfter(startTime)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "结束时间需晚于开始时间");
        }

        List<StudentSchedule> schedules = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        // 根据剩余课时生成 N 次排课，固定每周一次
        for (int i = 0; i < remainingLessons; i++) {
            StudentSchedule schedule = new StudentSchedule();
            schedule.setStudentId(studentId);
            schedule.setSubject(DEFAULT_SUBJECT);
            LocalDate currentDate = lessonDate.plusWeeks(i);
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
            schedule.setStatus("PLANNED");
            schedule.setCreatedAt(now);
            schedules.add(schedule);
        }

        studentScheduleService.saveBatch(schedules);
        // 扣减课时余额，确保下次排课不会重复生成
        if (balance != null) {
            balance.setRemainingLessons(Math.max(0, remainingLessons - schedules.size()));
            balance.setUpdatedAt(now);
            studentLessonBalanceService.updateById(balance);
        }
        return schedules.stream()
                .map(schedule -> toView(schedule, student.getName()))
                .collect(Collectors.toList());
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
                    return toView(schedule, studentName);
                })
                .collect(Collectors.toList());
    }

    /**
     * 把排课实体转换为前端展示对象。
     */
    private ScheduleView toView(StudentSchedule schedule, String studentName) {
        ScheduleView view = new ScheduleView();
        view.setId(schedule.getId());
        view.setStudentId(schedule.getStudentId());
        view.setStudentName(studentName);
        view.setSubject(schedule.getSubject());
        view.setStartTime(schedule.getStartTime());
        view.setEndTime(schedule.getEndTime());
        return view;
    }

    /**
     * 排课请求体：前端只需要传递周几与时间段。
     */
    public static class ScheduleGenerateRequest {
        @NotNull(message = "请选择周几")
        private Integer weekday;

        private LocalDate firstLessonDate;

        @NotNull(message = "请选择开始时间")
        private LocalTime startTime;

        @NotNull(message = "请选择结束时间")
        private LocalTime endTime;

        public Integer getWeekday() {
            return weekday;
        }

        public void setWeekday(Integer weekday) {
            this.weekday = weekday;
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
    }
}
