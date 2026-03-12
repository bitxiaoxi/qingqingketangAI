package com.qingqingketang.student.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qingqingketang.student.entity.StudentLessonBalance;
import com.qingqingketang.student.mapper.StudentLessonBalanceMapper;
import com.qingqingketang.student.service.StudentLessonBalanceService;
import org.springframework.stereotype.Service;

@Service
public class StudentLessonBalanceServiceImpl extends ServiceImpl<StudentLessonBalanceMapper, StudentLessonBalance> implements StudentLessonBalanceService {
}
