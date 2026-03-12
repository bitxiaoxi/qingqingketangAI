package com.qingqingketang.student.web.dto;

import java.math.BigDecimal;

public class PaymentSummary {
    private Long studentId;
    private BigDecimal totalTuitionPaid;
    private Integer totalLessonCount;
    private BigDecimal avgFeePerLesson;

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public BigDecimal getTotalTuitionPaid() {
        return totalTuitionPaid;
    }

    public void setTotalTuitionPaid(BigDecimal totalTuitionPaid) {
        this.totalTuitionPaid = totalTuitionPaid;
    }

    public Integer getTotalLessonCount() {
        return totalLessonCount;
    }

    public void setTotalLessonCount(Integer totalLessonCount) {
        this.totalLessonCount = totalLessonCount;
    }

    public BigDecimal getAvgFeePerLesson() {
        return avgFeePerLesson;
    }

    public void setAvgFeePerLesson(BigDecimal avgFeePerLesson) {
        this.avgFeePerLesson = avgFeePerLesson;
    }
}
