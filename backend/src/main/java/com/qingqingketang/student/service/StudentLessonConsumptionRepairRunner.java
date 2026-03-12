package com.qingqingketang.student.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class StudentLessonConsumptionRepairRunner implements ApplicationRunner {

    private final StudentLessonConsumptionRepairService studentLessonConsumptionRepairService;

    public StudentLessonConsumptionRepairRunner(StudentLessonConsumptionRepairService studentLessonConsumptionRepairService) {
        this.studentLessonConsumptionRepairService = studentLessonConsumptionRepairService;
    }

    @Override
    public void run(ApplicationArguments args) {
        studentLessonConsumptionRepairService.backfillCompletedSchedules();
    }
}
