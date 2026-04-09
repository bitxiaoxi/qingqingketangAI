package com.qingqingketang.student.service.impl;

import com.qingqingketang.student.entity.StudentPayment;
import com.qingqingketang.student.entity.StudentSchedule;
import com.qingqingketang.student.mapper.StudentMapper;
import com.qingqingketang.student.mapper.StudentPaymentMapper;
import com.qingqingketang.student.mapper.StudentScheduleMapper;
import com.qingqingketang.student.service.StudentScheduleService;
import com.qingqingketang.student.service.StudentScheduleSnapshotRepairService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class StudentScheduleSnapshotRepairServiceImpl implements StudentScheduleSnapshotRepairService {

    private static final Logger log = LoggerFactory.getLogger(StudentScheduleSnapshotRepairServiceImpl.class);

    private final StudentMapper studentMapper;
    private final StudentScheduleMapper studentScheduleMapper;
    private final StudentPaymentMapper studentPaymentMapper;
    private final StudentScheduleService studentScheduleService;

    public StudentScheduleSnapshotRepairServiceImpl(StudentMapper studentMapper,
                                                    StudentScheduleMapper studentScheduleMapper,
                                                    StudentPaymentMapper studentPaymentMapper,
                                                    StudentScheduleService studentScheduleService) {
        this.studentMapper = studentMapper;
        this.studentScheduleMapper = studentScheduleMapper;
        this.studentPaymentMapper = studentPaymentMapper;
        this.studentScheduleService = studentScheduleService;
    }

    @Override
    @Transactional
    public void backfillCompletedSchedules() {
        List<StudentSchedule> schedules = studentScheduleMapper.findCompletedWithoutConsumption();
        for (StudentSchedule schedule : schedules) {
            lockStudent(schedule.getStudentId());
            StudentPayment payment = resolvePayment(schedule);
            if (payment == null) {
                log.warn("Skip backfilling schedule snapshot for schedule {} of student {}: no available payment batch",
                        schedule.getId(), schedule.getStudentId());
                continue;
            }

            schedule.setPaymentId(payment.getId());
            if (schedule.getLessonPrice() == null) {
                schedule.setLessonPrice(payment.getAvgFeePerLesson() == null ? BigDecimal.ZERO : payment.getAvgFeePerLesson());
            }
            if (schedule.getConsumedAt() == null) {
                schedule.setConsumedAt(resolveConsumedAt(schedule));
            }
            studentScheduleService.updateById(schedule);
        }
    }

    private void lockStudent(Long studentId) {
        if (studentId == null || studentMapper.findByIdForUpdate(studentId) == null) {
            throw new IllegalStateException("回填销课快照时未找到对应学生: " + studentId);
        }
    }

    private StudentPayment resolvePayment(StudentSchedule schedule) {
        if (schedule.getPaymentId() != null) {
            return studentPaymentMapper.selectById(schedule.getPaymentId());
        }
        return studentPaymentMapper.findNextConsumablePayment(schedule.getStudentId());
    }

    private LocalDateTime resolveConsumedAt(StudentSchedule schedule) {
        if (schedule.getConsumedAt() != null) {
            return schedule.getConsumedAt();
        }
        if (schedule.getEndTime() != null) {
            return schedule.getEndTime();
        }
        if (schedule.getStartTime() != null) {
            return schedule.getStartTime();
        }
        if (schedule.getCreatedAt() != null) {
            return schedule.getCreatedAt();
        }
        return LocalDateTime.now();
    }
}
