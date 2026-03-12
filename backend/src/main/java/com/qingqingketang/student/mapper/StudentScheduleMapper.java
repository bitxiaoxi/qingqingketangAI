package com.qingqingketang.student.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qingqingketang.student.entity.StudentSchedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface StudentScheduleMapper extends BaseMapper<StudentSchedule> {

    @Select("SELECT * FROM student_schedules WHERE start_time BETWEEN #{start} AND #{end} ORDER BY start_time ASC")
    List<StudentSchedule> findWithin(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("SELECT * FROM student_schedules WHERE start_time < #{end} AND end_time > #{start} ORDER BY start_time LIMIT 1")
    StudentSchedule findFirstConflict(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select({
            "SELECT s.*",
            "FROM student_schedules s",
            "LEFT JOIN student_lesson_consumptions c ON c.schedule_id = s.id",
            "WHERE s.status = 'COMPLETED' AND c.id IS NULL",
            "ORDER BY s.student_id ASC, s.start_time ASC, s.id ASC"
    })
    List<StudentSchedule> findCompletedWithoutConsumption();
}
