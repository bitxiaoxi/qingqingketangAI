package com.qingqingketang.student.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class StudentScheduleSnapshotRepairRunner implements ApplicationRunner {

    private final StudentScheduleSnapshotRepairService studentScheduleSnapshotRepairService;

    public StudentScheduleSnapshotRepairRunner(StudentScheduleSnapshotRepairService studentScheduleSnapshotRepairService) {
        this.studentScheduleSnapshotRepairService = studentScheduleSnapshotRepairService;
    }

    @Override
    public void run(ApplicationArguments args) {
        studentScheduleSnapshotRepairService.backfillCompletedSchedules();
    }
}
