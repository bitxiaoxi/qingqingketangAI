package com.qingqingketang.student.service.impl;

import com.qingqingketang.student.entity.StudentLessonConsumption;
import com.qingqingketang.student.entity.StudentPayment;
import com.qingqingketang.student.entity.StudentSchedule;
import com.qingqingketang.student.mapper.StudentPaymentMapper;
import com.qingqingketang.student.mapper.StudentScheduleMapper;
import com.qingqingketang.student.service.StudentLessonConsumptionRepairService;
import com.qingqingketang.student.service.StudentLessonConsumptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class StudentLessonConsumptionRepairServiceImpl implements StudentLessonConsumptionRepairService {

    private static final Logger log = LoggerFactory.getLogger(StudentLessonConsumptionRepairServiceImpl.class);

    private final StudentScheduleMapper studentScheduleMapper;
    private final StudentPaymentMapper studentPaymentMapper;
    private final StudentLessonConsumptionService studentLessonConsumptionService;

    public StudentLessonConsumptionRepairServiceImpl(StudentScheduleMapper studentScheduleMapper,
                                                     StudentPaymentMapper studentPaymentMapper,
                                                     StudentLessonConsumptionService studentLessonConsumptionService) {
        this.studentScheduleMapper = studentScheduleMapper;
        this.studentPaymentMapper = studentPaymentMapper;
        this.studentLessonConsumptionService = studentLessonConsumptionService;
    }

    @Override
    @Transactional
    public void backfillCompletedSchedules() {
        List<StudentSchedule> schedules = studentScheduleMapper.findCompletedWithoutConsumption();
        for (StudentSchedule schedule : schedules) {
            StudentPayment payment = studentPaymentMapper.findNextConsumablePayment(schedule.getStudentId());
            if (payment == null) {
                log.warn("Skip backfilling lesson consumption for schedule {} of student {}: no available payment batch",
                        schedule.getId(), schedule.getStudentId());
                continue;
            }

            StudentLessonConsumption consumption = new StudentLessonConsumption();
            consumption.setScheduleId(schedule.getId());
            consumption.setStudentId(schedule.getStudentId());
            consumption.setPaymentId(payment.getId());
            consumption.setLessonPrice(payment.getAvgFeePerLesson() == null ? BigDecimal.ZERO : payment.getAvgFeePerLesson());
            consumption.setConsumedAt(resolveConsumedAt(schedule));
            studentLessonConsumptionService.save(consumption);
        }
    }

    private LocalDateTime resolveConsumedAt(StudentSchedule schedule) {
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
