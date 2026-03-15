package com.qingqingketang.student.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qingqingketang.student.entity.StudentLessonBalance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface StudentLessonBalanceMapper extends BaseMapper<StudentLessonBalance> {

    @Select("SELECT * FROM student_lesson_balances WHERE student_id = #{studentId} LIMIT 1 FOR UPDATE")
    StudentLessonBalance findByStudentIdForUpdate(@Param("studentId") Long studentId);
}
