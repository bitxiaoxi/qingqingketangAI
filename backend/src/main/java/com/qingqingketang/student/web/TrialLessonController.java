package com.qingqingketang.student.web;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.qingqingketang.student.entity.TrialLesson;
import com.qingqingketang.student.service.TrialLessonService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/trials")
@CrossOrigin(origins = "http://localhost:5173", maxAge = 3600)
public class TrialLessonController {

    private static final String STATUS_PENDING = "PENDING";

    private final TrialLessonService trialLessonService;

    public TrialLessonController(TrialLessonService trialLessonService) {
        this.trialLessonService = trialLessonService;
    }

    /**
     * 试听记录列表：按预约时间倒序返回，方便前端展示。
     */
    @GetMapping
    public List<TrialLessonView> list() {
        return trialLessonService.list(
                        Wrappers.<TrialLesson>lambdaQuery()
                                .orderByDesc(TrialLesson::getTrialTime)
                                .orderByDesc(TrialLesson::getCreatedAt))
                .stream()
                .map(this::toView)
                .collect(Collectors.toList());
    }

    /**
     * 录入试听：接收姓名、年级、试听时间，保存后返回记录。
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TrialLessonView create(@Valid @RequestBody TrialLessonRequest request) {
        TrialLesson lesson = new TrialLesson();
        lesson.setName(request.getName());
        lesson.setGrade(request.getGrade());
        lesson.setTrialTime(request.getTrialTime());
        lesson.setStatus(resolveStatus(request.getStatus()));
        lesson.setNote(request.getNote());
        lesson.setCreatedAt(LocalDateTime.now());
        trialLessonService.save(lesson);
        return toView(lesson);
    }

    @PutMapping("/{trialId}")
    public TrialLessonView update(@PathVariable Long trialId, @Valid @RequestBody TrialLessonRequest request) {
        TrialLesson lesson = trialLessonService.getById(trialId);
        if (lesson == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "试听记录不存在");
        }

        lesson.setName(request.getName());
        lesson.setGrade(request.getGrade());
        lesson.setTrialTime(request.getTrialTime());
        lesson.setStatus(resolveStatus(request.getStatus()));
        lesson.setNote(request.getNote());
        trialLessonService.updateById(lesson);
        return toView(lesson);
    }

    private TrialLessonView toView(TrialLesson lesson) {
        TrialLessonView view = new TrialLessonView();
        view.setId(lesson.getId());
        view.setName(lesson.getName());
        view.setGrade(lesson.getGrade());
        view.setTrialTime(lesson.getTrialTime());
        view.setStatus(resolveStatus(lesson.getStatus()));
        view.setNote(lesson.getNote());
        view.setCreatedAt(lesson.getCreatedAt());
        return view;
    }

    private String resolveStatus(String status) {
        return (status == null || status.trim().isEmpty()) ? STATUS_PENDING : status.trim().toUpperCase();
    }

    public static class TrialLessonRequest {
        @NotBlank(message = "姓名不能为空")
        private String name;

        @NotBlank(message = "年级不能为空")
        private String grade;

        @NotNull(message = "试听时间不能为空")
        private LocalDateTime trialTime;

        private String status;

        private String note;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getGrade() {
            return grade;
        }

        public void setGrade(String grade) {
            this.grade = grade;
        }

        public LocalDateTime getTrialTime() {
            return trialTime;
        }

        public void setTrialTime(LocalDateTime trialTime) {
            this.trialTime = trialTime;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getNote() {
            return note;
        }

        public void setNote(String note) {
            this.note = note;
        }
    }

    public static class TrialLessonView {
        private Long id;
        private String name;
        private String grade;
        private LocalDateTime trialTime;
        private String status;
        private String note;
        private LocalDateTime createdAt;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getGrade() {
            return grade;
        }

        public void setGrade(String grade) {
            this.grade = grade;
        }

        public LocalDateTime getTrialTime() {
            return trialTime;
        }

        public void setTrialTime(LocalDateTime trialTime) {
            this.trialTime = trialTime;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getNote() {
            return note;
        }

        public void setNote(String note) {
            this.note = note;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }
    }
}
