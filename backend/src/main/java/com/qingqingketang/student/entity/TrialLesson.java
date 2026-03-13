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
@TableName("trial_lessons")
public class TrialLesson {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String grade;

    @TableField("trial_time")
    private LocalDateTime trialTime;

    private String status;

    private String note;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
