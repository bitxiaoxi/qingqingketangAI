package com.qingqingketang.student.web.dto;

public class ScheduleCountSummary {
    private Long studentId;
    private Integer scheduledLessons;
    private Integer completedLessons;

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public Integer getScheduledLessons() {
        return scheduledLessons;
    }

    public void setScheduledLessons(Integer scheduledLessons) {
        this.scheduledLessons = scheduledLessons;
    }

    public Integer getCompletedLessons() {
        return completedLessons;
    }

    public void setCompletedLessons(Integer completedLessons) {
        this.completedLessons = completedLessons;
    }
}
