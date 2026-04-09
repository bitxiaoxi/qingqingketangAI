package com.qingqingketang.student.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("student_referrals")
public class StudentReferral {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("referrer_student_id")
    private Long referrerStudentId;

    @TableField("referred_student_id")
    private Long referredStudentId;

    @TableField("reward_lesson_count")
    private Integer rewardLessonCount;

    @TableField("reward_payment_id")
    private Long rewardPaymentId;

    private String remark;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("rewarded_at")
    private LocalDateTime rewardedAt;
}
