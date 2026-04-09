package com.qingqingketang.student.web.dto;

public class LessonBalanceSummary {
    private final int purchasedLessons;
    private final int scheduledLessons;
    private final int completedLessons;
    private final int remainingLessons;
    private final int schedulableLessons;

    private LessonBalanceSummary(int purchasedLessons,
                                 int scheduledLessons,
                                 int completedLessons,
                                 int remainingLessons,
                                 int schedulableLessons) {
        this.purchasedLessons = purchasedLessons;
        this.scheduledLessons = scheduledLessons;
        this.completedLessons = completedLessons;
        this.remainingLessons = remainingLessons;
        this.schedulableLessons = schedulableLessons;
    }

    public static LessonBalanceSummary from(PaymentSummary paymentSummary, ScheduleCountSummary scheduleCountSummary) {
        int purchasedLessons = paymentSummary != null && paymentSummary.getTotalLessonCount() != null
                ? paymentSummary.getTotalLessonCount()
                : 0;
        int scheduledLessons = scheduleCountSummary != null && scheduleCountSummary.getScheduledLessons() != null
                ? scheduleCountSummary.getScheduledLessons()
                : 0;
        int completedLessons = scheduleCountSummary != null && scheduleCountSummary.getCompletedLessons() != null
                ? scheduleCountSummary.getCompletedLessons()
                : 0;
        int remainingLessons = Math.max(purchasedLessons - completedLessons, 0);
        int schedulableLessons = Math.max(remainingLessons - scheduledLessons, 0);
        return new LessonBalanceSummary(
                Math.max(purchasedLessons, 0),
                Math.max(scheduledLessons, 0),
                Math.max(completedLessons, 0),
                remainingLessons,
                schedulableLessons);
    }

    public int getPurchasedLessons() {
        return purchasedLessons;
    }

    public int getScheduledLessons() {
        return scheduledLessons;
    }

    public int getCompletedLessons() {
        return completedLessons;
    }

    public int getRemainingLessons() {
        return remainingLessons;
    }

    public int getSchedulableLessons() {
        return schedulableLessons;
    }
}
