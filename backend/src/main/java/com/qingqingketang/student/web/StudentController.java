package com.qingqingketang.student.web;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.qingqingketang.student.entity.Student;
import com.qingqingketang.student.entity.StudentLessonBalance;
import com.qingqingketang.student.entity.StudentPayment;
import com.qingqingketang.student.mapper.StudentPaymentMapper;
import com.qingqingketang.student.service.StudentPaymentService;
import com.qingqingketang.student.service.StudentLessonBalanceService;
import com.qingqingketang.student.service.StudentService;
import com.qingqingketang.student.web.dto.PaymentSummary;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    public StudentController(StudentService studentService,
                             StudentPaymentService studentPaymentService,
                             StudentPaymentMapper studentPaymentMapper,
                             StudentLessonBalanceService studentLessonBalanceService) {
        this.studentService = studentService;
        this.studentPaymentService = studentPaymentService;
        this.studentPaymentMapper = studentPaymentMapper;
        this.studentLessonBalanceService = studentLessonBalanceService;
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

        return students.stream()
                .map(student -> toView(student, summaryMap.get(student.getId())))
                .collect(Collectors.toList());
    }

    /**
     * 学费概览：聚合已收总学费，预留已销课/待销课字段。
     */
    @GetMapping("/tuition-overview")
    public TuitionOverview getTuitionOverview() {
        TuitionOverview overview = new TuitionOverview();
        overview.setTotalReceived(studentPaymentMapper.totalTuitionPaid());
        overview.setTotalConsumed(null);
        overview.setTotalPending(null);
        return overview;
    }

    /**
     * 续费接口：对已有学生新增一笔缴费并更新课时余额。
     */
    @PostMapping("/{studentId}/payments")
    @ResponseStatus(HttpStatus.CREATED)
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
        payment.setPaidAt(LocalDateTime.now());
        studentPaymentService.save(payment);
        increaseLessonBalance(studentId, lessons);

        List<PaymentSummary> summaries = studentPaymentMapper.sumByStudentIds(Collections.singletonList(studentId));
        PaymentSummary summary = summaries.isEmpty() ? null : summaries.get(0);
        return toView(student, summary);
    }

    /**
     * 录入学生：创建学生基础信息，必要时写入首笔缴费与课时余额。
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
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
        setLessonBalance(student.getId(), lessons);

        PaymentSummary summary = new PaymentSummary();
        summary.setStudentId(student.getId());
        summary.setTotalTuitionPaid(tuition);
        summary.setTotalLessonCount(lessons);
        BigDecimal avgForSummary = lessons > 0
                ? tuition.divide(BigDecimal.valueOf(lessons), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        summary.setAvgFeePerLesson(avgForSummary);
        return toView(student, summary);
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

    public static class StudentView {
        private Long id;
        private String name;
        private String gender;
        private String grade;
        private BigDecimal tuitionPaid;
        private Integer lessonCount;
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
    private StudentView toView(Student student, PaymentSummary summary) {
        StudentView view = new StudentView();
        view.setId(student.getId());
        view.setName(student.getName());
        view.setGender(student.getGender());
        view.setGrade(student.getGrade());
        view.setCreatedAt(student.getCreatedAt());
        BigDecimal tuition = BigDecimal.ZERO;
        int lessons = 0;
        if (summary != null) {
            tuition = summary.getTotalTuitionPaid() == null ? BigDecimal.ZERO : summary.getTotalTuitionPaid();
            lessons = summary.getTotalLessonCount() == null ? 0 : summary.getTotalLessonCount();
        }
        view.setTuitionPaid(tuition);
        view.setLessonCount(lessons);
        if (lessons > 0) {
            BigDecimal summaryAvg = summary != null && summary.getAvgFeePerLesson() != null
                    ? summary.getAvgFeePerLesson()
                    : tuition.divide(BigDecimal.valueOf(lessons), 2, RoundingMode.HALF_UP);
            view.setAvgFeePerLesson(summaryAvg);
        } else {
            view.setAvgFeePerLesson(BigDecimal.ZERO);
        }
        return view;
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

    /**
     * 初始化/覆盖学生课时余额：录入学生时调用。
     */
    private void setLessonBalance(Long studentId, int lessons) {
        // 初始化或覆盖指定学生的剩余课时
        int normalized = Math.max(lessons, 0);
        StudentLessonBalance balance = studentLessonBalanceService.getOne(
                Wrappers.<StudentLessonBalance>lambdaQuery().eq(StudentLessonBalance::getStudentId, studentId));
        LocalDateTime now = LocalDateTime.now();
        if (balance == null) {
            balance = new StudentLessonBalance();
            balance.setStudentId(studentId);
            balance.setRemainingLessons(normalized);
            balance.setUpdatedAt(now);
            studentLessonBalanceService.save(balance);
        } else {
            balance.setRemainingLessons(normalized);
            balance.setUpdatedAt(now);
            studentLessonBalanceService.updateById(balance);
        }
    }

    /**
     * 累加课时余额：续费接口调用；校验 delta<=0 时直接返回。
     */
    private void increaseLessonBalance(Long studentId, int delta) {
        if (delta <= 0) {
            return;
        }
        // 续费时累加课时余额，不存在则创建记录
        StudentLessonBalance balance = studentLessonBalanceService.getOne(
                Wrappers.<StudentLessonBalance>lambdaQuery().eq(StudentLessonBalance::getStudentId, studentId));
        LocalDateTime now = LocalDateTime.now();
        if (balance == null) {
            balance = new StudentLessonBalance();
            balance.setStudentId(studentId);
            balance.setRemainingLessons(delta);
            balance.setUpdatedAt(now);
            studentLessonBalanceService.save(balance);
        } else {
            int current = balance.getRemainingLessons() == null ? 0 : balance.getRemainingLessons();
            balance.setRemainingLessons(current + delta);
            balance.setUpdatedAt(now);
            studentLessonBalanceService.updateById(balance);
        }
    }
}
