package com.qingqingketang.student.web;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.qingqingketang.student.entity.Student;
import com.qingqingketang.student.entity.StudentPayment;
import com.qingqingketang.student.entity.StudentSchedule;
import com.qingqingketang.student.mapper.StudentMapper;
import com.qingqingketang.student.mapper.StudentPaymentMapper;
import com.qingqingketang.student.mapper.StudentScheduleMapper;
import com.qingqingketang.student.service.StudentPaymentService;
import com.qingqingketang.student.service.StudentScheduleService;
import com.qingqingketang.student.service.StudentService;
import com.qingqingketang.student.web.dto.LessonBalanceSummary;
import com.qingqingketang.student.web.dto.PaymentSummary;
import com.qingqingketang.student.web.dto.ScheduleCountSummary;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/students")
@CrossOrigin(origins = "http://localhost:5173", maxAge = 3600)
public class StudentController {
    private static final String STATUS_PLANNED = "PLANNED";
    private static final String STATUS_COMPLETED = "COMPLETED";

    private final StudentService studentService;
    private final StudentMapper studentMapper;
    private final StudentPaymentService studentPaymentService;
    private final StudentPaymentMapper studentPaymentMapper;
    private final StudentScheduleMapper studentScheduleMapper;
    private final StudentScheduleService studentScheduleService;

    public StudentController(StudentService studentService,
                             StudentMapper studentMapper,
                             StudentPaymentService studentPaymentService,
                             StudentPaymentMapper studentPaymentMapper,
                             StudentScheduleMapper studentScheduleMapper,
                             StudentScheduleService studentScheduleService) {
        this.studentService = studentService;
        this.studentMapper = studentMapper;
        this.studentPaymentService = studentPaymentService;
        this.studentPaymentMapper = studentPaymentMapper;
        this.studentScheduleMapper = studentScheduleMapper;
        this.studentScheduleService = studentScheduleService;
    }

    /**
     * 获取学生列表：带出学费与课时汇总，用于前端学生管理列表。
     */
    @GetMapping
    public List<StudentView> findAll() {
        // 统一按照创建时间倒序展示最新录入的学生
        List<Student> students = studentService.list(
                Wrappers.<Student>lambdaQuery()
                        .orderByDesc(Student::getCreatedAt)
        );
        if (students.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量查询学费汇总，避免循环查询
        List<Long> ids = students.stream()
                .map(Student::getId)
                .collect(Collectors.toList());
        Map<Long, PaymentSummary> summaryMap = new HashMap<>();
        List<PaymentSummary> summaries = studentPaymentMapper.sumByStudentIds(ids);
        for (PaymentSummary summary : summaries) {
            summaryMap.put(summary.getStudentId(), summary);
        }
        Map<Long, LessonBalanceSummary> balanceMap = buildLessonBalanceMap(ids, summaryMap);

        return students.stream()
                .map(student -> toView(
                        student,
                        summaryMap.get(student.getId()),
                        balanceMap.get(student.getId())))
                .collect(Collectors.toList());
    }

    @GetMapping("/payment-records")
    public List<PaymentRecordView> listPaymentRecords() {
        List<StudentPayment> payments = studentPaymentService.list(
                Wrappers.<StudentPayment>lambdaQuery()
                        .orderByDesc(StudentPayment::getPaidAt)
                        .orderByDesc(StudentPayment::getId)
        );
        if (payments.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> studentIds = payments.stream()
                .map(StudentPayment::getStudentId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> studentNameMap = studentService.list(
                        Wrappers.<Student>lambdaQuery().in(Student::getId, studentIds))
                .stream()
                .collect(Collectors.toMap(Student::getId, Student::getName));

        return payments.stream()
                .map(payment -> {
                    PaymentRecordView view = new PaymentRecordView();
                    view.setId(payment.getId());
                    view.setStudentId(payment.getStudentId());
                    view.setStudentName(studentNameMap.getOrDefault(payment.getStudentId(), "未知学生"));
                    view.setTuitionPaid(payment.getTuitionPaid());
                    view.setLessonCount(payment.getLessonCount());
                    view.setPaidAt(payment.getPaidAt());
                    view.setRemark(payment.getRemark());
                    return view;
                })
                .collect(Collectors.toList());
    }

    /**
     * 学费概览：聚合已收总学费，预留已销课/待销课字段。
     */
    @GetMapping("/tuition-overview")
    public TuitionOverview getTuitionOverview() {
        TuitionOverview overview = new TuitionOverview();
        BigDecimal totalReceived = studentPaymentMapper.totalTuitionPaid();
        BigDecimal totalConsumed = studentScheduleMapper.totalConsumedAmount();
        overview.setTotalReceived(totalReceived);
        overview.setTotalConsumed(totalConsumed);
        overview.setTotalPending(totalReceived.subtract(totalConsumed));
        return overview;
    }

    @GetMapping("/write-off-overview")
    public WriteOffOverview getWriteOffOverview(@RequestParam(value = "month", required = false) String month) {
        YearMonth targetMonth;
        try {
            targetMonth = (month == null || month.trim().isEmpty()) ? YearMonth.now() : YearMonth.parse(month.trim());
        } catch (DateTimeParseException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "月份格式错误，应为 yyyy-MM");
        }

        LocalDate monthStartDate = targetMonth.atDay(1);
        LocalDate nextMonthStartDate = targetMonth.plusMonths(1).atDay(1);
        LocalDateTime monthStart = monthStartDate.atStartOfDay();
        LocalDateTime nextMonthStart = nextMonthStartDate.atStartOfDay();
        WriteOffOverview overview = new WriteOffOverview();
        overview.setMonth(targetMonth.toString());
        overview.setMonthAmount(BigDecimal.ZERO);
        overview.setMonthStudentLessonCount(0);
        overview.setDailyAmounts(buildEmptyWriteOffDays(monthStartDate, targetMonth.lengthOfMonth()));

        List<StudentSchedule> schedules = studentScheduleService.list(
                Wrappers.<StudentSchedule>lambdaQuery()
                        .ge(StudentSchedule::getStartTime, monthStart)
                        .lt(StudentSchedule::getStartTime, nextMonthStart)
                        .in(StudentSchedule::getStatus, STATUS_PLANNED, STATUS_COMPLETED)
        );
        if (schedules.isEmpty()) {
            return overview;
        }

        List<Long> studentIds = schedules.stream()
                .map(StudentSchedule::getStudentId)
                .distinct()
                .collect(Collectors.toList());

        List<StudentPayment> payments = studentPaymentService.list(
                Wrappers.<StudentPayment>lambdaQuery()
                        .in(StudentPayment::getStudentId, studentIds)
                        .orderByAsc(StudentPayment::getStudentId)
                        .orderByAsc(StudentPayment::getPaidAt)
                        .orderByAsc(StudentPayment::getId)
        );
        Map<Long, List<StudentPayment>> paymentsByStudentId = payments.stream()
                .collect(Collectors.groupingBy(StudentPayment::getStudentId));
        Map<Long, StudentPayment> paymentById = payments.stream()
                .collect(Collectors.toMap(
                        StudentPayment::getId,
                        payment -> payment
                ));

        List<StudentSchedule> completedSchedules = studentScheduleService.list(
                Wrappers.<StudentSchedule>lambdaQuery()
                        .in(StudentSchedule::getStudentId, studentIds)
                        .eq(StudentSchedule::getStatus, STATUS_COMPLETED)
        );
        Map<Long, Integer> consumedCountByPaymentId = new HashMap<>();
        for (StudentSchedule completedSchedule : completedSchedules) {
            if (completedSchedule.getPaymentId() == null) {
                continue;
            }
            consumedCountByPaymentId.merge(completedSchedule.getPaymentId(), 1, Integer::sum);
        }

        Map<Long, BigDecimal> currentBatchPriceByStudentId = new HashMap<>();
        for (Long studentId : studentIds) {
            List<StudentPayment> studentPayments = paymentsByStudentId.getOrDefault(studentId, Collections.emptyList());
            BigDecimal currentBatchPrice = BigDecimal.ZERO;
            for (StudentPayment payment : studentPayments) {
                int lessonCount = payment.getLessonCount() == null ? 0 : payment.getLessonCount();
                if (lessonCount <= 0 || payment.getId() == null) {
                    continue;
                }
                int consumedCount = consumedCountByPaymentId.getOrDefault(payment.getId(), 0);
                if (consumedCount < lessonCount) {
                    currentBatchPrice = resolvePaymentUnitPrice(payment);
                    break;
                }
            }
            currentBatchPriceByStudentId.put(studentId, currentBatchPrice);
        }

        BigDecimal monthAmount = BigDecimal.ZERO;
        int monthStudentLessonCount = 0;
        Map<LocalDate, WriteOffDayAmount> dailyAmountMap = new LinkedHashMap<>();
        for (WriteOffDayAmount dayAmount : overview.getDailyAmounts()) {
            dailyAmountMap.put(dayAmount.getDate(), dayAmount);
        }

        for (StudentSchedule schedule : schedules) {
            LocalDateTime startTime = schedule.getStartTime();
            if (startTime == null) {
                continue;
            }

            BigDecimal lessonPrice = resolveWriteOffLessonPrice(
                    schedule,
                    paymentById,
                    currentBatchPriceByStudentId
            );
            LocalDate scheduleDate = startTime.toLocalDate();
            WriteOffDayAmount dayAmount = dailyAmountMap.get(scheduleDate);
            if (dayAmount != null) {
                dayAmount.setAmount(dayAmount.getAmount().add(lessonPrice));
                dayAmount.setStudentLessonCount(dayAmount.getStudentLessonCount() + 1);
            }
            monthAmount = monthAmount.add(lessonPrice);
            monthStudentLessonCount += 1;
        }

        overview.setMonthAmount(monthAmount);
        overview.setMonthStudentLessonCount(monthStudentLessonCount);
        return overview;
    }

    private List<WriteOffDayAmount> buildEmptyWriteOffDays(LocalDate monthStartDate, int daysInMonth) {
        List<WriteOffDayAmount> days = new ArrayList<>();
        for (int index = 0; index < daysInMonth; index += 1) {
            WriteOffDayAmount dayAmount = new WriteOffDayAmount();
            dayAmount.setDate(monthStartDate.plusDays(index));
            dayAmount.setAmount(BigDecimal.ZERO);
            dayAmount.setStudentLessonCount(0);
            days.add(dayAmount);
        }
        return days;
    }

    private BigDecimal resolveWriteOffLessonPrice(StudentSchedule schedule,
                                                  Map<Long, StudentPayment> paymentById,
                                                  Map<Long, BigDecimal> currentBatchPriceByStudentId) {
        if (schedule.getLessonPrice() != null) {
            return schedule.getLessonPrice();
        }
        if (schedule.getPaymentId() != null) {
            return resolvePaymentUnitPrice(paymentById.get(schedule.getPaymentId()));
        }
        return currentBatchPriceByStudentId.getOrDefault(schedule.getStudentId(), BigDecimal.ZERO);
    }

    private BigDecimal resolvePaymentUnitPrice(StudentPayment payment) {
        if (payment == null) {
            return BigDecimal.ZERO;
        }
        if (payment.getAvgFeePerLesson() != null) {
            return payment.getAvgFeePerLesson();
        }
        BigDecimal tuition = payment.getTuitionPaid() == null ? BigDecimal.ZERO : payment.getTuitionPaid();
        int lessonCount = payment.getLessonCount() == null ? 0 : payment.getLessonCount();
        if (lessonCount <= 0) {
            return BigDecimal.ZERO;
        }
        return tuition.divide(BigDecimal.valueOf(lessonCount), 2, RoundingMode.HALF_UP);
    }

    /**
     * 续费接口：对已有学生新增一笔缴费，并返回实时课时汇总。
     */
    @PostMapping("/{studentId}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public StudentView createPayment(@PathVariable Long studentId, @Valid @RequestBody PaymentRequest request) {
        Student student = lockStudent(studentId);

        BigDecimal tuition = request.getTuitionPaid();
        Integer lessons = request.getLessonCount();

        StudentPayment payment = new StudentPayment();
        // 学费/课时写入缴费流水，并驱动后续课时余额
        payment.setStudentId(studentId);
        payment.setTuitionPaid(tuition);
        payment.setLessonCount(lessons);
        BigDecimal avg = lessons > 0
                ? tuition.divide(BigDecimal.valueOf(lessons), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        payment.setAvgFeePerLesson(avg);
        payment.setRemark(request.getRemark());
        payment.setPaidAt(LocalDateTime.now());
        studentPaymentService.save(payment);

        List<PaymentSummary> summaries = studentPaymentMapper.sumByStudentIds(Collections.singletonList(studentId));
        PaymentSummary summary = summaries.isEmpty() ? null : summaries.get(0);
        return buildSingleStudentView(student, summary);
    }

    /**
     * 录入学生：创建学生基础信息，必要时写入首笔缴费。
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public StudentView create(@Valid @RequestBody StudentRequest request) {
        Student student = new Student();
        student.setName(request.getName());
        student.setGender(request.getGender());
        student.setGrade(request.getGrade());
        student.setCreatedAt(LocalDateTime.now());
        studentService.save(student);

        BigDecimal tuition = request.getTuitionPaid() == null ? BigDecimal.ZERO : request.getTuitionPaid();
        Integer lessons = request.getLessonCount() == null ? 0 : request.getLessonCount();

        if (tuition.compareTo(BigDecimal.ZERO) > 0 || lessons > 0) {
            lockStudent(student.getId());
            // 同步写入首笔缴费，方便统计学费及课时
            StudentPayment payment = new StudentPayment();
            payment.setStudentId(student.getId());
            payment.setTuitionPaid(tuition);
            payment.setLessonCount(lessons);
            BigDecimal avg = lessons > 0
                    ? tuition.divide(BigDecimal.valueOf(lessons), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            payment.setAvgFeePerLesson(avg);
            payment.setPaidAt(LocalDateTime.now());
            studentPaymentService.save(payment);
        }

        List<PaymentSummary> summaries = studentPaymentMapper.sumByStudentIds(Collections.singletonList(student.getId()));
        PaymentSummary summary = summaries.isEmpty() ? null : summaries.get(0);
        return buildSingleStudentView(student, summary);
    }

    @PutMapping("/{studentId}")
    public StudentView update(@PathVariable Long studentId, @Valid @RequestBody StudentUpdateRequest request) {
        Student student = studentService.getById(studentId);
        if (student == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "学生不存在");
        }

        student.setName(request.getName());
        student.setGender(request.getGender());
        student.setGrade(request.getGrade());
        studentService.updateById(student);

        List<PaymentSummary> summaries = studentPaymentMapper.sumByStudentIds(Collections.singletonList(studentId));
        PaymentSummary summary = summaries.isEmpty() ? null : summaries.get(0);
        return buildSingleStudentView(student, summary);
    }

    public static class StudentRequest {
        @NotBlank(message = "姓名不能为空")
        private String name;

        @NotBlank(message = "性别不能为空")
        private String gender;

        @NotBlank(message = "年级不能为空")
        private String grade;

        @NotNull(message = "学费不能为空")
        @DecimalMin(value = "0.0", inclusive = true, message = "学费需为非负数")
        private BigDecimal tuitionPaid;

        @NotNull(message = "课时不能为空")
        @Min(value = 0, message = "课时需为非负数")
        private Integer lessonCount;

        public StudentRequest() {
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getGender() {
            return gender;
        }

        public void setGender(String gender) {
            this.gender = gender;
        }

        public String getGrade() {
            return grade;
        }

        public void setGrade(String grade) {
            this.grade = grade;
        }

        public BigDecimal getTuitionPaid() {
            return tuitionPaid;
        }

        public void setTuitionPaid(BigDecimal tuitionPaid) {
            this.tuitionPaid = tuitionPaid;
        }

        public Integer getLessonCount() {
            return lessonCount;
        }

        public void setLessonCount(Integer lessonCount) {
            this.lessonCount = lessonCount;
        }
    }

    public static class PaymentRequest {

        @NotNull(message = "学费不能为空")
        @DecimalMin(value = "0.0", inclusive = false, message = "学费需大于0")
        private BigDecimal tuitionPaid;

        @NotNull(message = "课时不能为空")
        @Min(value = 1, message = "课时需大于0")
        private Integer lessonCount;

        private String remark;

        public BigDecimal getTuitionPaid() {
            return tuitionPaid;
        }

        public void setTuitionPaid(BigDecimal tuitionPaid) {
            this.tuitionPaid = tuitionPaid;
        }

        public Integer getLessonCount() {
            return lessonCount;
        }

        public void setLessonCount(Integer lessonCount) {
            this.lessonCount = lessonCount;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }
    }

    public static class StudentUpdateRequest {
        @NotBlank(message = "姓名不能为空")
        private String name;

        @NotBlank(message = "性别不能为空")
        private String gender;

        @NotBlank(message = "年级不能为空")
        private String grade;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getGender() {
            return gender;
        }

        public void setGender(String gender) {
            this.gender = gender;
        }

        public String getGrade() {
            return grade;
        }

        public void setGrade(String grade) {
            this.grade = grade;
        }
    }

    public static class StudentView {
        private Long id;
        private String name;
        private String gender;
        private String grade;
        private BigDecimal tuitionPaid;
        private Integer lessonCount;
        private Integer scheduledLessons;
        private Integer completedLessons;
        private Integer remainingLessons;
        private Integer schedulableLessons;
        private Integer courseCount;
        private LocalDateTime createdAt;
        private BigDecimal avgFeePerLesson;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getGender() {
            return gender;
        }

        public void setGender(String gender) {
            this.gender = gender;
        }

        public String getGrade() {
            return grade;
        }

        public void setGrade(String grade) {
            this.grade = grade;
        }

        public BigDecimal getTuitionPaid() {
            return tuitionPaid;
        }

        public void setTuitionPaid(BigDecimal tuitionPaid) {
            this.tuitionPaid = tuitionPaid;
        }

        public Integer getLessonCount() {
            return lessonCount;
        }

        public void setLessonCount(Integer lessonCount) {
            this.lessonCount = lessonCount;
        }

        public Integer getScheduledLessons() {
            return scheduledLessons;
        }

        public void setScheduledLessons(Integer scheduledLessons) {
            this.scheduledLessons = scheduledLessons;
        }

        public Integer getCompletedLessons() {
            return completedLessons;
        }

        public void setCompletedLessons(Integer completedLessons) {
            this.completedLessons = completedLessons;
        }

        public Integer getRemainingLessons() {
            return remainingLessons;
        }

        public void setRemainingLessons(Integer remainingLessons) {
            this.remainingLessons = remainingLessons;
        }

        public Integer getSchedulableLessons() {
            return schedulableLessons;
        }

        public void setSchedulableLessons(Integer schedulableLessons) {
            this.schedulableLessons = schedulableLessons;
        }

        public Integer getCourseCount() {
            return courseCount;
        }

        public void setCourseCount(Integer courseCount) {
            this.courseCount = courseCount;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public BigDecimal getAvgFeePerLesson() {
            return avgFeePerLesson;
        }

        public void setAvgFeePerLesson(BigDecimal avgFeePerLesson) {
            this.avgFeePerLesson = avgFeePerLesson;
        }
    }

    /**
     * 把实体与缴费汇总转换成视图对象，统一输出到前端。
     */
    private StudentView toView(Student student, PaymentSummary summary, LessonBalanceSummary balance) {
        StudentView view = new StudentView();
        view.setId(student.getId());
        view.setName(student.getName());
        view.setGender(student.getGender());
        view.setGrade(student.getGrade());
        view.setCreatedAt(student.getCreatedAt());
        BigDecimal tuition = summary != null && summary.getTotalTuitionPaid() != null
                ? summary.getTotalTuitionPaid()
                : BigDecimal.ZERO;
        int purchasedLessons = balance.getPurchasedLessons();
        int scheduledLessons = balance.getScheduledLessons();
        int completedLessons = balance.getCompletedLessons();
        int remainingLessons = balance.getRemainingLessons();
        int schedulableLessons = balance.getSchedulableLessons();
        view.setTuitionPaid(tuition);
        view.setLessonCount(purchasedLessons);
        view.setScheduledLessons(scheduledLessons);
        view.setCompletedLessons(completedLessons);
        view.setRemainingLessons(remainingLessons);
        view.setSchedulableLessons(schedulableLessons);
        view.setCourseCount(scheduledLessons + completedLessons);
        if (purchasedLessons > 0) {
            BigDecimal summaryAvg = summary != null && summary.getAvgFeePerLesson() != null
                    ? summary.getAvgFeePerLesson()
                    : tuition.divide(BigDecimal.valueOf(purchasedLessons), 2, RoundingMode.HALF_UP);
            view.setAvgFeePerLesson(summaryAvg);
        } else {
            view.setAvgFeePerLesson(BigDecimal.ZERO);
        }
        return view;
    }

    private StudentView buildSingleStudentView(Student student, PaymentSummary summary) {
        LessonBalanceSummary balance = loadLessonBalance(student.getId(), summary);
        return toView(student, summary, balance);
    }

    private Map<Long, LessonBalanceSummary> buildLessonBalanceMap(List<Long> studentIds, Map<Long, PaymentSummary> paymentSummaryMap) {
        Map<Long, ScheduleCountSummary> scheduleCountMap = new HashMap<>();
        if (!studentIds.isEmpty()) {
            List<ScheduleCountSummary> scheduleCountSummaries = studentScheduleMapper.sumByStudentIds(studentIds);
            for (ScheduleCountSummary scheduleCountSummary : scheduleCountSummaries) {
                scheduleCountMap.put(scheduleCountSummary.getStudentId(), scheduleCountSummary);
            }
        }

        Map<Long, LessonBalanceSummary> balanceMap = new HashMap<>();
        for (Long studentId : studentIds) {
            balanceMap.put(studentId, LessonBalanceSummary.from(
                    paymentSummaryMap.get(studentId),
                    scheduleCountMap.get(studentId)));
        }
        return balanceMap;
    }

    private LessonBalanceSummary loadLessonBalance(Long studentId, PaymentSummary paymentSummary) {
        List<ScheduleCountSummary> scheduleCountSummaries = studentScheduleMapper.sumByStudentIds(Collections.singletonList(studentId));
        ScheduleCountSummary scheduleCountSummary = scheduleCountSummaries.isEmpty() ? null : scheduleCountSummaries.get(0);
        return LessonBalanceSummary.from(paymentSummary, scheduleCountSummary);
    }

    private Student lockStudent(Long studentId) {
        Student student = studentMapper.findByIdForUpdate(studentId);
        if (student == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "学生不存在");
        }
        return student;
    }

    public static class PaymentRecordView {
        private Long id;
        private Long studentId;
        private String studentName;
        private BigDecimal tuitionPaid;
        private Integer lessonCount;
        private LocalDateTime paidAt;
        private String remark;

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

        public BigDecimal getTuitionPaid() {
            return tuitionPaid;
        }

        public void setTuitionPaid(BigDecimal tuitionPaid) {
            this.tuitionPaid = tuitionPaid;
        }

        public Integer getLessonCount() {
            return lessonCount;
        }

        public void setLessonCount(Integer lessonCount) {
            this.lessonCount = lessonCount;
        }

        public LocalDateTime getPaidAt() {
            return paidAt;
        }

        public void setPaidAt(LocalDateTime paidAt) {
            this.paidAt = paidAt;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }
    }

    public static class TuitionOverview {
        private BigDecimal totalReceived;
        private BigDecimal totalConsumed;
        private BigDecimal totalPending;

        public BigDecimal getTotalReceived() {
            return totalReceived;
        }

        public void setTotalReceived(BigDecimal totalReceived) {
            this.totalReceived = totalReceived;
        }

        public BigDecimal getTotalConsumed() {
            return totalConsumed;
        }

        public void setTotalConsumed(BigDecimal totalConsumed) {
            this.totalConsumed = totalConsumed;
        }

        public BigDecimal getTotalPending() {
            return totalPending;
        }

        public void setTotalPending(BigDecimal totalPending) {
            this.totalPending = totalPending;
        }
    }

    public static class WriteOffOverview {
        private String month;
        private BigDecimal monthAmount;
        private Integer monthStudentLessonCount;
        private List<WriteOffDayAmount> dailyAmounts;

        public String getMonth() {
            return month;
        }

        public void setMonth(String month) {
            this.month = month;
        }

        public BigDecimal getMonthAmount() {
            return monthAmount;
        }

        public void setMonthAmount(BigDecimal monthAmount) {
            this.monthAmount = monthAmount;
        }

        public Integer getMonthStudentLessonCount() {
            return monthStudentLessonCount;
        }

        public void setMonthStudentLessonCount(Integer monthStudentLessonCount) {
            this.monthStudentLessonCount = monthStudentLessonCount;
        }

        public List<WriteOffDayAmount> getDailyAmounts() {
            return dailyAmounts;
        }

        public void setDailyAmounts(List<WriteOffDayAmount> dailyAmounts) {
            this.dailyAmounts = dailyAmounts;
        }
    }

    public static class WriteOffDayAmount {
        private LocalDate date;
        private BigDecimal amount;
        private Integer studentLessonCount;

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public Integer getStudentLessonCount() {
            return studentLessonCount;
        }

        public void setStudentLessonCount(Integer studentLessonCount) {
            this.studentLessonCount = studentLessonCount;
        }
    }

}
