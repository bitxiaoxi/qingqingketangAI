package com.qingqingketang.student.web.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LessonBalanceSummaryTest {

    @Test
    void shouldReturnZeroWhenNoPaymentAndNoSchedule() {
        LessonBalanceSummary summary = LessonBalanceSummary.from(null, null);

        assertEquals(0, summary.getPurchasedLessons());
        assertEquals(0, summary.getScheduledLessons());
        assertEquals(0, summary.getCompletedLessons());
        assertEquals(0, summary.getRemainingLessons());
        assertEquals(0, summary.getSchedulableLessons());
    }

    @Test
    void shouldKeepAllLessonsSchedulableBeforeScheduling() {
        PaymentSummary paymentSummary = paymentSummary(20);

        LessonBalanceSummary summary = LessonBalanceSummary.from(paymentSummary, null);

        assertEquals(20, summary.getPurchasedLessons());
        assertEquals(0, summary.getScheduledLessons());
        assertEquals(0, summary.getCompletedLessons());
        assertEquals(20, summary.getRemainingLessons());
        assertEquals(20, summary.getSchedulableLessons());
    }

    @Test
    void shouldSubtractPlannedAndCompletedLessons() {
        PaymentSummary paymentSummary = paymentSummary(20);
        ScheduleCountSummary scheduleCountSummary = scheduleCountSummary(6, 5);

        LessonBalanceSummary summary = LessonBalanceSummary.from(paymentSummary, scheduleCountSummary);

        assertEquals(20, summary.getPurchasedLessons());
        assertEquals(6, summary.getScheduledLessons());
        assertEquals(5, summary.getCompletedLessons());
        assertEquals(15, summary.getRemainingLessons());
        assertEquals(9, summary.getSchedulableLessons());
    }

    @Test
    void shouldKeepRemainingLessonsWhenOnlyCompletedLessonsExist() {
        PaymentSummary paymentSummary = paymentSummary(12);
        ScheduleCountSummary scheduleCountSummary = scheduleCountSummary(0, 4);

        LessonBalanceSummary summary = LessonBalanceSummary.from(paymentSummary, scheduleCountSummary);

        assertEquals(12, summary.getPurchasedLessons());
        assertEquals(0, summary.getScheduledLessons());
        assertEquals(4, summary.getCompletedLessons());
        assertEquals(8, summary.getRemainingLessons());
        assertEquals(8, summary.getSchedulableLessons());
    }

    @Test
    void shouldClampNegativeValuesToZeroWhenOverConsumed() {
        PaymentSummary paymentSummary = paymentSummary(2);
        ScheduleCountSummary scheduleCountSummary = scheduleCountSummary(1, 5);

        LessonBalanceSummary summary = LessonBalanceSummary.from(paymentSummary, scheduleCountSummary);

        assertEquals(2, summary.getPurchasedLessons());
        assertEquals(1, summary.getScheduledLessons());
        assertEquals(5, summary.getCompletedLessons());
        assertEquals(0, summary.getRemainingLessons());
        assertEquals(0, summary.getSchedulableLessons());
    }

    private PaymentSummary paymentSummary(int lessonCount) {
        PaymentSummary paymentSummary = new PaymentSummary();
        paymentSummary.setTotalLessonCount(lessonCount);
        return paymentSummary;
    }

    private ScheduleCountSummary scheduleCountSummary(int scheduledLessons, int completedLessons) {
        ScheduleCountSummary scheduleCountSummary = new ScheduleCountSummary();
        scheduleCountSummary.setScheduledLessons(scheduledLessons);
        scheduleCountSummary.setCompletedLessons(completedLessons);
        return scheduleCountSummary;
    }
}
