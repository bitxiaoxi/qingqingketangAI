package com.qingqingketang.student.web;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.qingqingketang.student.entity.Student;
import com.qingqingketang.student.entity.StudentLessonBalance;
import com.qingqingketang.student.entity.StudentPayment;
import com.qingqingketang.student.mapper.StudentLessonConsumptionMapper;
import com.qingqingketang.student.mapper.StudentPaymentMapper;
import com.qingqingketang.student.service.StudentPaymentService;
import com.qingqingketang.student.service.StudentLessonBalanceService;
import com.qingqingketang.student.service.StudentService;
import com.qingqingketang.student.web.dto.PaymentSummary;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/students")
@CrossOrigin(origins = "http://localhost:5173", maxAge = 3600)
public class StudentController {

    private final StudentService studentService;
    private final StudentPaymentService studentPaymentService;
    private final StudentPaymentMapper studentPaymentMapper;
    private final StudentLessonBalanceService studentLessonBalanceService;
    private final StudentLessonConsumptionMapper studentLessonConsumptionMapper;

    public StudentController(StudentService studentService,
                             StudentPaymentService studentPaymentService,
                             StudentPaymentMapper studentPaymentMapper,
                             StudentLessonBalanceService studentLessonBalanceService,
                             StudentLessonConsumptionMapper studentLessonConsumptionMapper) {
        this.studentService = studentService;
        this.studentPaymentService = studentPaymentService;
        this.studentPaymentMapper = studentPaymentMapper;
        this.studentLessonBalanceService = studentLessonBalanceService;
        this.studentLessonConsumptionMapper = studentLessonConsumptionMapper;
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
        Map<Long, StudentLessonBalance> balanceMap = studentLessonBalanceService.list(
                        Wrappers.<StudentLessonBalance>lambdaQuery()
                                .in(StudentLessonBalance::getStudentId, ids))
                .stream()
                .collect(Collectors.toMap(StudentLessonBalance::getStudentId, balance -> balance));

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
        BigDecimal totalConsumed = studentLessonConsumptionMapper.totalConsumedAmount();
        overview.setTotalReceived(totalReceived);
        overview.setTotalConsumed(totalConsumed);
        overview.setTotalPending(totalReceived.subtract(totalConsumed));
        return overview;
    }

    /**
     * 续费接口：对已有学生新增一笔缴费并更新课时余额。
     */
    @PostMapping("/{studentId}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public StudentView createPayment(@PathVariable Long studentId, @Valid @RequestBody PaymentRequest request) {
        Student student = studentService.getById(studentId);
        if (student == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "学生不存在");
        }

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
        studentLessonBalanceService.refreshStudentBalance(studentId);

        List<PaymentSummary> summaries = studentPaymentMapper.sumByStudentIds(Collections.singletonList(studentId));
        PaymentSummary summary = summaries.isEmpty() ? null : summaries.get(0);
        return buildSingleStudentView(student, summary);
    }

    /**
     * 录入学生：创建学生基础信息，必要时写入首笔缴费与课时余额。
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
        studentLessonBalanceService.refreshStudentBalance(student.getId());

        PaymentSummary summary = new PaymentSummary();
        summary.setStudentId(student.getId());
        summary.setTotalTuitionPaid(tuition);
        summary.setTotalLessonCount(lessons);
        BigDecimal avgForSummary = lessons > 0
                ? tuition.divide(BigDecimal.valueOf(lessons), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        summary.setAvgFeePerLesson(avgForSummary);
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
    private StudentView toView(Student student, PaymentSummary summary, StudentLessonBalance balance) {
        StudentView view = new StudentView();
        view.setId(student.getId());
        view.setName(student.getName());
        view.setGender(student.getGender());
        view.setGrade(student.getGrade());
        view.setCreatedAt(student.getCreatedAt());
        BigDecimal tuition = BigDecimal.ZERO;
        int purchasedLessons = balance != null && balance.getPurchasedLessons() != null
                ? balance.getPurchasedLessons()
                : 0;
        if (summary != null) {
            tuition = summary.getTotalTuitionPaid() == null ? BigDecimal.ZERO : summary.getTotalTuitionPaid();
            if (purchasedLessons <= 0) {
                purchasedLessons = summary.getTotalLessonCount() == null ? 0 : summary.getTotalLessonCount();
            }
        }
        int scheduledLessons = balance != null && balance.getScheduledLessons() != null
                ? balance.getScheduledLessons()
                : 0;
        int completedLessons = balance != null && balance.getCompletedLessons() != null
                ? balance.getCompletedLessons()
                : 0;
        int remainingLessons = balance != null && balance.getRemainingLessons() != null
                ? balance.getRemainingLessons()
                : Math.max(purchasedLessons - completedLessons, 0);
        int schedulableLessons = balance != null && balance.getSchedulableLessons() != null
                ? balance.getSchedulableLessons()
                : Math.max(remainingLessons - scheduledLessons, 0);
        view.setTuitionPaid(tuition);
        view.setLessonCount(Math.max(purchasedLessons, 0));
        view.setScheduledLessons(Math.max(scheduledLessons, 0));
        view.setCompletedLessons(Math.max(completedLessons, 0));
        view.setRemainingLessons(Math.max(remainingLessons, 0));
        view.setSchedulableLessons(Math.max(schedulableLessons, 0));
        view.setCourseCount(Math.max(scheduledLessons + completedLessons, 0));
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
        StudentLessonBalance balance = studentLessonBalanceService.getOne(
                Wrappers.<StudentLessonBalance>lambdaQuery().eq(StudentLessonBalance::getStudentId, student.getId()));
        return toView(student, summary, balance);
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

}
