package com.qingqingketang.student.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qingqingketang.student.entity.StudentLessonBalance;

public interface StudentLessonBalanceService extends IService<StudentLessonBalance> {

    StudentLessonBalance refreshStudentBalance(Long studentId);

    void refreshAllStudentBalances();
}
