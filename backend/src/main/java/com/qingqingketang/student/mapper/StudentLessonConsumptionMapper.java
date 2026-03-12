package com.qingqingketang.student.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qingqingketang.student.entity.StudentLessonConsumption;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface StudentLessonConsumptionMapper extends BaseMapper<StudentLessonConsumption> {

    @Select({
            "<script>",
            "SELECT * FROM student_lesson_consumptions",
            "WHERE schedule_id IN",
            "<foreach collection='scheduleIds' item='scheduleId' open='(' separator=',' close=')'>",
            "#{scheduleId}",
            "</foreach>",
            "</script>"
    })
    List<StudentLessonConsumption> findByScheduleIds(@Param("scheduleIds") List<Long> scheduleIds);

    @Select("SELECT * FROM student_lesson_consumptions WHERE schedule_id = #{scheduleId} LIMIT 1")
    StudentLessonConsumption findByScheduleId(@Param("scheduleId") Long scheduleId);

    @Select("SELECT COALESCE(SUM(lesson_price), 0) FROM student_lesson_consumptions")
    BigDecimal totalConsumedAmount();
}
