package com.qingqingketang.student.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qingqingketang.student.entity.Student;
import com.qingqingketang.student.entity.StudentLessonBalance;
import com.qingqingketang.student.entity.StudentSchedule;
import com.qingqingketang.student.mapper.StudentLessonBalanceMapper;
import com.qingqingketang.student.mapper.StudentPaymentMapper;
import com.qingqingketang.student.service.StudentLessonBalanceService;
import com.qingqingketang.student.service.StudentScheduleService;
import com.qingqingketang.student.service.StudentService;
import com.qingqingketang.student.web.dto.PaymentSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StudentLessonBalanceServiceImpl extends ServiceImpl<StudentLessonBalanceMapper, StudentLessonBalance> implements StudentLessonBalanceService {

    private static final String STATUS_PLANNED = "PLANNED";
    private static final String STATUS_COMPLETED = "COMPLETED";

    private final StudentService studentService;
    private final StudentPaymentMapper studentPaymentMapper;
    private final StudentScheduleService studentScheduleService;

    public StudentLessonBalanceServiceImpl(StudentService studentService,
                                           StudentPaymentMapper studentPaymentMapper,
                                           StudentScheduleService studentScheduleService) {
        this.studentService = studentService;
        this.studentPaymentMapper = studentPaymentMapper;
        this.studentScheduleService = studentScheduleService;
    }

    @Override
    @Transactional
    public StudentLessonBalance refreshStudentBalance(Long studentId) {
        if (studentId == null) {
            throw new IllegalArgumentException("studentId 不能为空");
        }

        List<PaymentSummary> summaries = studentPaymentMapper.sumByStudentIds(Collections.singletonList(studentId));
        PaymentSummary summary = summaries.isEmpty() ? null : summaries.get(0);
        int purchasedLessons = summary != null && summary.getTotalLessonCount() != null
                ? summary.getTotalLessonCount()
                : 0;
        int scheduledLessons = Math.toIntExact(studentScheduleService.count(
                Wrappers.<StudentSchedule>lambdaQuery()
                        .eq(StudentSchedule::getStudentId, studentId)
                        .eq(StudentSchedule::getStatus, STATUS_PLANNED)
        ));
        int completedLessons = Math.toIntExact(studentScheduleService.count(
                Wrappers.<StudentSchedule>lambdaQuery()
                        .eq(StudentSchedule::getStudentId, studentId)
                        .eq(StudentSchedule::getStatus, STATUS_COMPLETED)
        ));
        int remainingLessons = Math.max(purchasedLessons - completedLessons, 0);
        int schedulableLessons = Math.max(remainingLessons - scheduledLessons, 0);

        StudentLessonBalance balance = baseMapper.findByStudentIdForUpdate(studentId);
        LocalDateTime now = LocalDateTime.now();
        if (balance == null) {
            balance = new StudentLessonBalance();
            balance.setStudentId(studentId);
        }
        balance.setPurchasedLessons(purchasedLessons);
        balance.setScheduledLessons(scheduledLessons);
        balance.setCompletedLessons(completedLessons);
        balance.setRemainingLessons(remainingLessons);
        balance.setSchedulableLessons(schedulableLessons);
        balance.setUpdatedAt(now);

        if (balance.getId() == null) {
            save(balance);
        } else {
            updateById(balance);
        }
        return balance;
    }

    @Override
    @Transactional
    public void refreshAllStudentBalances() {
        List<Long> studentIds = studentService.list().stream()
                .map(Student::getId)
                .collect(Collectors.toList());
        for (Long studentId : studentIds) {
            refreshStudentBalance(studentId);
        }
    }
}
