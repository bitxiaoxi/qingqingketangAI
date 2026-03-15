package com.qingqingketang.student.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(100)
public class StudentLessonBalanceSyncRunner implements ApplicationRunner {

    private final StudentLessonBalanceService studentLessonBalanceService;

    public StudentLessonBalanceSyncRunner(StudentLessonBalanceService studentLessonBalanceService) {
        this.studentLessonBalanceService = studentLessonBalanceService;
    }

    @Override
    public void run(ApplicationArguments args) {
        studentLessonBalanceService.refreshAllStudentBalances();
    }
}
