package com.qingqingketang.student.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("student_payments")
public class StudentPayment {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("student_id")
    private Long studentId;

    @TableField("tuition_paid")
    private BigDecimal tuitionPaid;

    @TableField("lesson_count")
    private Integer lessonCount;

    @TableField("avg_fee_per_lesson")
    private BigDecimal avgFeePerLesson;

    @TableField("paid_at")
    private LocalDateTime paidAt;
}
