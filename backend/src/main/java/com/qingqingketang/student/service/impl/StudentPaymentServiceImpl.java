package com.qingqingketang.student.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qingqingketang.student.entity.StudentPayment;
import com.qingqingketang.student.mapper.StudentPaymentMapper;
import com.qingqingketang.student.service.StudentPaymentService;
import org.springframework.stereotype.Service;

@Service
public class StudentPaymentServiceImpl extends ServiceImpl<StudentPaymentMapper, StudentPayment> implements StudentPaymentService {
}
