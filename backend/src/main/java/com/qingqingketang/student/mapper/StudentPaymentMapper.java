package com.qingqingketang.student.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qingqingketang.student.entity.StudentPayment;
import com.qingqingketang.student.web.dto.PaymentSummary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface StudentPaymentMapper extends BaseMapper<StudentPayment> {

    @Select({
            "<script>",
            "SELECT student_id AS studentId,",
            "COALESCE(SUM(tuition_paid),0) AS totalTuitionPaid,",
            "COALESCE(SUM(lesson_count),0) AS totalLessonCount,",
            "COALESCE(SUM(tuition_paid)/NULLIF(SUM(lesson_count),0),0) AS avgFeePerLesson",
            "FROM student_payments",
            "WHERE student_id IN",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "GROUP BY student_id",
            "</script>"
    })
    List<PaymentSummary> sumByStudentIds(@Param("ids") List<Long> ids);

    @Select({
            "SELECT p.*",
            "FROM student_payments p",
            "LEFT JOIN (",
            "    SELECT payment_id, COUNT(*) AS consumedCount",
            "    FROM student_lesson_consumptions",
            "    GROUP BY payment_id",
            ") c ON c.payment_id = p.id",
            "WHERE p.student_id = #{studentId}",
            "  AND p.lesson_count > 0",
            "  AND COALESCE(c.consumedCount, 0) < p.lesson_count",
            "ORDER BY p.paid_at ASC, p.id ASC",
            "LIMIT 1 FOR UPDATE"
    })
    StudentPayment findNextConsumablePayment(@Param("studentId") Long studentId);

    @Select("SELECT COALESCE(SUM(tuition_paid),0) FROM student_payments")
    BigDecimal totalTuitionPaid();
}
