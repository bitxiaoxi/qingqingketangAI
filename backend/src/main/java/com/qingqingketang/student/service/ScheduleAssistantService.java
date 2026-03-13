package com.qingqingketang.student.service;

import com.qingqingketang.student.entity.Student;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public interface ScheduleAssistantService {

    AssistantInterpretation interpret(List<ConversationMessage> messages, List<Student> students);

    class ConversationMessage {
        private String role;
        private String content;

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

    class AssistantInterpretation {
        private String intent;
        private String reply;
        private String studentName;
        private List<Integer> weekdays = new ArrayList<>();
        private LocalDate startDate;
        private LocalTime startTime;
        private LocalTime endTime;
        private String analysisMode;
        private List<String> missingFields = new ArrayList<>();
        private List<String> warnings = new ArrayList<>();

        public String getIntent() {
            return intent;
        }

        public void setIntent(String intent) {
            this.intent = intent;
        }

        public String getReply() {
            return reply;
        }

        public void setReply(String reply) {
            this.reply = reply;
        }

        public String getStudentName() {
            return studentName;
        }

        public void setStudentName(String studentName) {
            this.studentName = studentName;
        }

        public List<Integer> getWeekdays() {
            return weekdays;
        }

        public void setWeekdays(List<Integer> weekdays) {
            this.weekdays = weekdays;
        }

        public LocalDate getStartDate() {
            return startDate;
        }

        public void setStartDate(LocalDate startDate) {
            this.startDate = startDate;
        }

        public LocalTime getStartTime() {
            return startTime;
        }

        public void setStartTime(LocalTime startTime) {
            this.startTime = startTime;
        }

        public LocalTime getEndTime() {
            return endTime;
        }

        public void setEndTime(LocalTime endTime) {
            this.endTime = endTime;
        }

        public String getAnalysisMode() {
            return analysisMode;
        }

        public void setAnalysisMode(String analysisMode) {
            this.analysisMode = analysisMode;
        }

        public List<String> getMissingFields() {
            return missingFields;
        }

        public void setMissingFields(List<String> missingFields) {
            this.missingFields = missingFields;
        }

        public List<String> getWarnings() {
            return warnings;
        }

        public void setWarnings(List<String> warnings) {
            this.warnings = warnings;
        }
    }
}
