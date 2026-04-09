package com.qingqingketang.student.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qingqingketang.student.entity.StudentReferral;
import com.qingqingketang.student.mapper.StudentReferralMapper;
import com.qingqingketang.student.service.StudentReferralService;
import org.springframework.stereotype.Service;

@Service
public class StudentReferralServiceImpl extends ServiceImpl<StudentReferralMapper, StudentReferral> implements StudentReferralService {
}
