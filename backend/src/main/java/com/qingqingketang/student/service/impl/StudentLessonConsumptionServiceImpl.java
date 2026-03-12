package com.qingqingketang.student.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qingqingketang.student.entity.StudentLessonConsumption;
import com.qingqingketang.student.mapper.StudentLessonConsumptionMapper;
import com.qingqingketang.student.service.StudentLessonConsumptionService;
import org.springframework.stereotype.Service;

@Service
public class StudentLessonConsumptionServiceImpl extends ServiceImpl<StudentLessonConsumptionMapper, StudentLessonConsumption>
        implements StudentLessonConsumptionService {
}
