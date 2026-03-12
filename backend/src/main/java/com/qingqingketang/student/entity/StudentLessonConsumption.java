package com.qingqingketang.student.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("student_lesson_consumptions")
public class StudentLessonConsumption {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("schedule_id")
    private Long scheduleId;

    @TableField("student_id")
    private Long studentId;

    @TableField("payment_id")
    private Long paymentId;

    @TableField("lesson_price")
    private BigDecimal lessonPrice;

    @TableField("consumed_at")
    private LocalDateTime consumedAt;
}
