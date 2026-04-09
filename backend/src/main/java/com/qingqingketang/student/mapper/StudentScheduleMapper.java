package com.qingqingketang.student.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qingqingketang.student.entity.StudentSchedule;
import com.qingqingketang.student.web.dto.ScheduleCountSummary;
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

    @Select("SELECT * FROM student_schedules WHERE start_time < #{end} AND end_time > #{start} ORDER BY start_time ASC, id ASC")
    List<StudentSchedule> findConflicts(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("SELECT * FROM student_schedules WHERE start_time < #{end} AND end_time > #{start} AND id <> #{excludeId} ORDER BY start_time LIMIT 1")
    StudentSchedule findFirstConflictExcludingId(@Param("start") LocalDateTime start,
                                                 @Param("end") LocalDateTime end,
                                                 @Param("excludeId") Long excludeId);

    @Select({
            "SELECT * FROM student_schedules",
            "WHERE student_id = #{studentId} AND status = 'PLANNED'",
            "ORDER BY start_time ASC, id ASC"
    })
    List<StudentSchedule> findPlannedByStudentId(@Param("studentId") Long studentId);

    @Select({
            "SELECT * FROM student_schedules",
            "WHERE student_id = #{studentId} AND status = 'PLANNED'",
            "ORDER BY start_time DESC, id DESC",
            "LIMIT 1"
    })
    StudentSchedule findLastPlannedByStudentId(@Param("studentId") Long studentId);

    @Select({
            "<script>",
            "SELECT student_id AS studentId,",
            "       COALESCE(SUM(CASE WHEN status = 'PLANNED' THEN 1 ELSE 0 END), 0) AS scheduledLessons,",
            "       COALESCE(SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END), 0) AS completedLessons",
            "FROM student_schedules",
            "WHERE student_id IN",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "GROUP BY student_id",
            "</script>"
    })
    List<ScheduleCountSummary> sumByStudentIds(@Param("ids") List<Long> ids);

    @Select({
            "SELECT s.*",
            "FROM student_schedules s",
            "WHERE s.status = 'COMPLETED'",
            "  AND (s.payment_id IS NULL OR s.lesson_price IS NULL OR s.consumed_at IS NULL)",
            "ORDER BY s.student_id ASC, s.start_time ASC, s.id ASC"
    })
    List<StudentSchedule> findCompletedWithoutConsumption();

    @Select({
            "SELECT COALESCE(SUM(lesson_price), 0)",
            "FROM student_schedules",
            "WHERE status = 'COMPLETED' AND lesson_price IS NOT NULL"
    })
    java.math.BigDecimal totalConsumedAmount();
}
