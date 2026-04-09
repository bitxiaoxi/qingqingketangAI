package com.qingqingketang.student.web;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.qingqingketang.student.entity.Student;
import com.qingqingketang.student.entity.StudentPayment;
import com.qingqingketang.student.entity.StudentReferral;
import com.qingqingketang.student.mapper.StudentMapper;
import com.qingqingketang.student.service.StudentPaymentService;
import com.qingqingketang.student.service.StudentReferralService;
import com.qingqingketang.student.service.StudentService;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/referrals")
@CrossOrigin(origins = "http://localhost:5173", maxAge = 3600)
public class StudentReferralController {

    private static final int DEFAULT_REWARD_LESSON_COUNT = 1;

    private final StudentReferralService studentReferralService;
    private final StudentService studentService;
    private final StudentPaymentService studentPaymentService;
    private final StudentMapper studentMapper;

    public StudentReferralController(StudentReferralService studentReferralService,
                                     StudentService studentService,
                                     StudentPaymentService studentPaymentService,
                                     StudentMapper studentMapper) {
        this.studentReferralService = studentReferralService;
        this.studentService = studentService;
        this.studentPaymentService = studentPaymentService;
        this.studentMapper = studentMapper;
    }

    @GetMapping
    public List<StudentReferralView> list() {
        List<StudentReferral> referrals = studentReferralService.list(
                Wrappers.<StudentReferral>lambdaQuery()
                        .orderByDesc(StudentReferral::getRewardedAt)
                        .orderByDesc(StudentReferral::getId)
        );
        if (referrals.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> studentIds = new LinkedHashSet<>();
        for (StudentReferral referral : referrals) {
            if (referral.getReferrerStudentId() != null) {
                studentIds.add(referral.getReferrerStudentId());
            }
            if (referral.getReferredStudentId() != null) {
                studentIds.add(referral.getReferredStudentId());
            }
        }
        Map<Long, Student> studentMap = studentService.listByIds(studentIds).stream()
                .collect(Collectors.toMap(Student::getId, student -> student));

        return referrals.stream()
                .map(referral -> toView(
                        referral,
                        studentMap.get(referral.getReferrerStudentId()),
                        studentMap.get(referral.getReferredStudentId())))
                .collect(Collectors.toList());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public StudentReferralView create(@Valid @RequestBody StudentReferralRequest request) {
        if (request.getReferrerStudentId().equals(request.getReferredStudentId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "介绍人和被介绍人不能是同一位学生");
        }

        Map<Long, Student> lockedStudents = lockStudents(
                request.getReferrerStudentId(),
                request.getReferredStudentId()
        );
        Student referrer = lockedStudents.get(request.getReferrerStudentId());
        Student referred = lockedStudents.get(request.getReferredStudentId());

        StudentReferral existingReferral = studentReferralService.getOne(
                Wrappers.<StudentReferral>lambdaQuery()
                        .eq(StudentReferral::getReferredStudentId, request.getReferredStudentId())
                        .last("LIMIT 1")
        );
        if (existingReferral != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该学生已绑定转介绍关系");
        }

        LocalDateTime now = LocalDateTime.now();
        StudentPayment rewardPayment = new StudentPayment();
        rewardPayment.setStudentId(referrer.getId());
        rewardPayment.setTuitionPaid(BigDecimal.ZERO);
        rewardPayment.setLessonCount(DEFAULT_REWARD_LESSON_COUNT);
        rewardPayment.setAvgFeePerLesson(BigDecimal.ZERO);
        rewardPayment.setRemark(buildRewardPaymentRemark(referred.getName(), request.getRemark()));
        rewardPayment.setPaidAt(now);
        studentPaymentService.save(rewardPayment);

        StudentReferral referral = new StudentReferral();
        referral.setReferrerStudentId(referrer.getId());
        referral.setReferredStudentId(referred.getId());
        referral.setRewardLessonCount(DEFAULT_REWARD_LESSON_COUNT);
        referral.setRewardPaymentId(rewardPayment.getId());
        referral.setRemark(normalizeRemark(request.getRemark()));
        referral.setCreatedAt(now);
        referral.setRewardedAt(now);
        studentReferralService.save(referral);

        return toView(referral, referrer, referred);
    }

    private Map<Long, Student> lockStudents(Long leftStudentId, Long rightStudentId) {
        List<Long> studentIds = new ArrayList<>();
        studentIds.add(leftStudentId);
        studentIds.add(rightStudentId);
        Collections.sort(studentIds);

        Map<Long, Student> studentMap = new HashMap<>();
        for (Long studentId : studentIds) {
            if (studentMap.containsKey(studentId)) {
                continue;
            }
            Student student = studentMapper.findByIdForUpdate(studentId);
            if (student == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "学生不存在");
            }
            studentMap.put(studentId, student);
        }
        return studentMap;
    }

    private String buildRewardPaymentRemark(String referredStudentName, String remark) {
        StringBuilder builder = new StringBuilder("转介绍赠课");
        if (referredStudentName != null && !referredStudentName.trim().isEmpty()) {
            builder.append("：介绍 ").append(referredStudentName.trim());
        }
        String normalizedRemark = normalizeRemark(remark);
        if (normalizedRemark != null) {
            builder.append("（").append(normalizedRemark).append("）");
        }
        return builder.toString();
    }

    private String normalizeRemark(String remark) {
        if (remark == null) {
            return null;
        }
        String normalizedRemark = remark.trim();
        return normalizedRemark.isEmpty() ? null : normalizedRemark;
    }

    private StudentReferralView toView(StudentReferral referral, Student referrer, Student referred) {
        StudentReferralView view = new StudentReferralView();
        view.setId(referral.getId());
        view.setReferrerStudentId(referral.getReferrerStudentId());
        view.setReferrerStudentName(referrer != null ? referrer.getName() : "未知学生");
        view.setReferrerStudentGrade(referrer != null ? referrer.getGrade() : null);
        view.setReferredStudentId(referral.getReferredStudentId());
        view.setReferredStudentName(referred != null ? referred.getName() : "未知学生");
        view.setReferredStudentGrade(referred != null ? referred.getGrade() : null);
        view.setRewardLessonCount(referral.getRewardLessonCount());
        view.setRewardPaymentId(referral.getRewardPaymentId());
        view.setRemark(referral.getRemark());
        view.setCreatedAt(referral.getCreatedAt());
        view.setRewardedAt(referral.getRewardedAt());
        return view;
    }

    public static class StudentReferralRequest {
        @NotNull(message = "介绍人不能为空")
        private Long referrerStudentId;

        @NotNull(message = "被介绍人不能为空")
        private Long referredStudentId;

        private String remark;

        public Long getReferrerStudentId() {
            return referrerStudentId;
        }

        public void setReferrerStudentId(Long referrerStudentId) {
            this.referrerStudentId = referrerStudentId;
        }

        public Long getReferredStudentId() {
            return referredStudentId;
        }

        public void setReferredStudentId(Long referredStudentId) {
            this.referredStudentId = referredStudentId;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }
    }

    public static class StudentReferralView {
        private Long id;
        private Long referrerStudentId;
        private String referrerStudentName;
        private String referrerStudentGrade;
        private Long referredStudentId;
        private String referredStudentName;
        private String referredStudentGrade;
        private Integer rewardLessonCount;
        private Long rewardPaymentId;
        private String remark;
        private LocalDateTime createdAt;
        private LocalDateTime rewardedAt;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getReferrerStudentId() {
            return referrerStudentId;
        }

        public void setReferrerStudentId(Long referrerStudentId) {
            this.referrerStudentId = referrerStudentId;
        }

        public String getReferrerStudentName() {
            return referrerStudentName;
        }

        public void setReferrerStudentName(String referrerStudentName) {
            this.referrerStudentName = referrerStudentName;
        }

        public String getReferrerStudentGrade() {
            return referrerStudentGrade;
        }

        public void setReferrerStudentGrade(String referrerStudentGrade) {
            this.referrerStudentGrade = referrerStudentGrade;
        }

        public Long getReferredStudentId() {
            return referredStudentId;
        }

        public void setReferredStudentId(Long referredStudentId) {
            this.referredStudentId = referredStudentId;
        }

        public String getReferredStudentName() {
            return referredStudentName;
        }

        public void setReferredStudentName(String referredStudentName) {
            this.referredStudentName = referredStudentName;
        }

        public String getReferredStudentGrade() {
            return referredStudentGrade;
        }

        public void setReferredStudentGrade(String referredStudentGrade) {
            this.referredStudentGrade = referredStudentGrade;
        }

        public Integer getRewardLessonCount() {
            return rewardLessonCount;
        }

        public void setRewardLessonCount(Integer rewardLessonCount) {
            this.rewardLessonCount = rewardLessonCount;
        }

        public Long getRewardPaymentId() {
            return rewardPaymentId;
        }

        public void setRewardPaymentId(Long rewardPaymentId) {
            this.rewardPaymentId = rewardPaymentId;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public LocalDateTime getRewardedAt() {
            return rewardedAt;
        }

        public void setRewardedAt(LocalDateTime rewardedAt) {
            this.rewardedAt = rewardedAt;
        }
    }
}
