package com.qingqingketang.student.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qingqingketang.student.entity.StudentSchedule;
import com.qingqingketang.student.mapper.StudentScheduleMapper;
import com.qingqingketang.student.service.StudentScheduleService;
import org.springframework.stereotype.Service;

@Service
public class StudentScheduleServiceImpl extends ServiceImpl<StudentScheduleMapper, StudentSchedule> implements StudentScheduleService {
}
