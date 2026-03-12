package com.qingqingketang.student.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("students")
public class Student {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String gender;

    private String grade;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
