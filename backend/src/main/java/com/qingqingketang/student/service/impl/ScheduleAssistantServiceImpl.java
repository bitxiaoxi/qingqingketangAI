package com.qingqingketang.student.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qingqingketang.student.entity.Student;
import com.qingqingketang.student.service.ScheduleAssistantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ScheduleAssistantServiceImpl implements ScheduleAssistantService {

    private static final Logger log = LoggerFactory.getLogger(ScheduleAssistantServiceImpl.class);

    private static final Pattern FULL_DATE_PATTERN =
            Pattern.compile("(20\\d{2})\\s*[-/.年]\\s*(\\d{1,2})\\s*[-/.月]\\s*(\\d{1,2})\\s*[日号]?");
    private static final Pattern MONTH_DAY_PATTERN =
            Pattern.compile("(\\d{1,2})\\s*月\\s*(\\d{1,2})\\s*[日号]?");
    private static final Pattern RELATIVE_WEEKDAY_PATTERN =
            Pattern.compile("(本周|下周)([一二三四五六日天])");
    private static final Pattern WEEKLY_COMPACT_PATTERN =
            Pattern.compile("每周([一二三四五六日天、和与及,，\\s]{1,20})");
    private static final Pattern SINGLE_WEEKDAY_PATTERN =
            Pattern.compile("(?:周|星期)([一二三四五六日天])");
    private static final Pattern TIME_RANGE_PATTERN =
            Pattern.compile("(?:(上午|中午|下午|晚上|晚|早上|凌晨))?\\s*(\\d{1,2})(?:[:：点时](\\d{1,2}|半)?)?\\s*(?:到|至|\\-|~|—)\\s*(?:(上午|中午|下午|晚上|晚|早上|凌晨))?\\s*(\\d{1,2})(?:[:：点时](\\d{1,2}|半)?)?");

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${schedule.ai.enabled:false}")
    private boolean aiEnabled;

    @Value("${schedule.ai.base-url:}")
    private String aiBaseUrl;

    @Value("${schedule.ai.api-key:}")
    private String aiApiKey;

    @Value("${schedule.ai.model:}")
    private String aiModel;

    public ScheduleAssistantServiceImpl(ObjectMapper objectMapper,
                                        @Value("${schedule.ai.timeout-ms:20000}") int timeoutMs) {
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeoutMs);
        requestFactory.setReadTimeout(timeoutMs);
        this.restTemplate = new RestTemplate(requestFactory);
    }

    @Override
    public AssistantInterpretation interpret(List<ConversationMessage> messages, List<Student> students) {
        List<ConversationMessage> sanitizedMessages = sanitizeMessages(messages);
        if (sanitizedMessages.isEmpty()) {
            return buildMissingInformationReply(Collections.singletonList("排课需求"));
        }

        if (!isAiConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI 排课服务未配置，请先检查 schedule.ai 配置。");
        }

        try {
            AssistantInterpretation aiInterpretation = interpretWithAi(sanitizedMessages, students);
            if (aiInterpretation == null || !StringUtils.hasText(aiInterpretation.getIntent())) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 排课服务返回结果异常，请稍后重试。");
            }
            aiInterpretation.setAnalysisMode("AI");
            normalizeInterpretation(aiInterpretation);
            return aiInterpretation;
        } catch (ResponseStatusException error) {
            log.warn("AI 排课流程失败: status={}, message={}", error.getStatus(), error.getReason(), error);
            throw error;
        } catch (Exception error) {
            log.error("AI 排课流程异常", error);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI 排课服务当前不可用，请稍后重试。", error);
        }
    }

    private boolean isAiConfigured() {
        return aiEnabled
                && StringUtils.hasText(aiBaseUrl)
                && StringUtils.hasText(aiApiKey)
                && StringUtils.hasText(aiModel);
    }

    private List<ConversationMessage> sanitizeMessages(List<ConversationMessage> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return Collections.emptyList();
        }
        return messages.stream()
                .filter(message -> message != null && StringUtils.hasText(message.getContent()))
                .skip(Math.max(0, messages.size() - 12))
                .collect(Collectors.toList());
    }

    private AssistantInterpretation interpretWithAi(List<ConversationMessage> messages, List<Student> students)
            throws IOException {
        List<Map<String, String>> payloadMessages = new ArrayList<>();
        payloadMessages.add(buildChatMessage("system", buildSystemPrompt(students)));
        for (ConversationMessage message : messages) {
            String role = normalizeRole(message.getRole());
            payloadMessages.add(buildChatMessage(role, message.getContent().trim()));
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", aiModel);
        body.put("temperature", 0.1);
        body.put("messages", payloadMessages);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(aiApiKey);

        String url = normalizeBaseUrl(aiBaseUrl) + "/chat/completions";
        log.info("AI 排课请求开始: url={}, model={}, conversation={}",
                url,
                aiModel,
                formatMessagesForLog(messages));
        long startedAt = System.currentTimeMillis();

        ResponseEntity<Map> response;
        try {
            response = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), Map.class);
        } catch (RestClientResponseException error) {
            log.warn("AI 排课 HTTP 异常: durationMs={}, rawStatus={}, statusText={}",
                    System.currentTimeMillis() - startedAt,
                    error.getRawStatusCode(),
                    error.getStatusText());
            throw buildUpstreamException(error);
        } catch (Exception error) {
            log.error("AI 排课请求异常: durationMs={}, errorType={}, message={}",
                    System.currentTimeMillis() - startedAt,
                    error.getClass().getName(),
                    error.getMessage(),
                    error);
            throw error;
        }

        log.info("AI 排课原始响应: durationMs={}, status={}, body={}",
                System.currentTimeMillis() - startedAt,
                response.getStatusCodeValue(),
                toJsonSafely(response.getBody()));

        String content = extractAssistantContent(response.getBody());
        if (!StringUtils.hasText(content)) {
            log.warn("AI 排课响应缺少可解析 content: body={}", toJsonSafely(response.getBody()));
            throw new IOException("AI 返回内容为空");
        }

        log.info("AI 排课提取内容: {}", content);

        Map<String, Object> parsed = objectMapper.readValue(stripCodeFence(content), new TypeReference<Map<String, Object>>() {
        });
        AssistantInterpretation interpretation = new AssistantInterpretation();
        interpretation.setIntent(asUpperText(parsed.get("intent"), "ASK_CLARIFICATION"));
        interpretation.setReply(asText(parsed.get("reply")));
        interpretation.setStudentName(asText(parsed.get("studentName")));
        interpretation.setWeekdays(parseWeekdaysFromObject(parsed.get("weekdays")));
        interpretation.setStartDate(parseDateValue(asText(parsed.get("startDate"))));
        interpretation.setStartTime(parseTimeValue(asText(parsed.get("startTime"))));
        interpretation.setEndTime(parseTimeValue(asText(parsed.get("endTime"))));
        interpretation.setMissingFields(parseStringList(parsed.get("missingFields")));
        interpretation.setWarnings(parseStringList(parsed.get("warnings")));
        log.info("AI 排课解析结果: intent={}, studentName={}, weekdays={}, startDate={}, startTime={}, endTime={}, missingFields={}, warnings={}",
                interpretation.getIntent(),
                interpretation.getStudentName(),
                interpretation.getWeekdays(),
                interpretation.getStartDate(),
                interpretation.getStartTime(),
                interpretation.getEndTime(),
                interpretation.getMissingFields(),
                interpretation.getWarnings());
        return interpretation;
    }

    private ResponseStatusException buildUpstreamException(RestClientResponseException error) {
        HttpStatus statusCode;
        try {
            statusCode = HttpStatus.resolve(error.getRawStatusCode());
            if (statusCode == null) {
                statusCode = HttpStatus.BAD_GATEWAY;
            }
        } catch (Exception ignored) {
            statusCode = HttpStatus.BAD_GATEWAY;
        }

        String fallbackMessage = StringUtils.hasText(error.getStatusText())
                ? error.getStatusText()
                : "AI 排课服务调用失败";
        String message = extractUpstreamErrorMessage(error.getResponseBodyAsString(), fallbackMessage);
        log.warn("AI 排课上游报错: status={}, statusText={}, responseBody={}",
                error.getRawStatusCode(),
                error.getStatusText(),
                error.getResponseBodyAsString());
        return new ResponseStatusException(statusCode, message, error);
    }

    private String toJsonSafely(Object value) {
        if (value == null) {
            return "null";
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ignored) {
            return String.valueOf(value);
        }
    }

    private String formatMessagesForLog(List<ConversationMessage> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return "[]";
        }
        return messages.stream()
                .filter(message -> message != null && StringUtils.hasText(message.getContent()))
                .map(message -> {
                    String role = StringUtils.hasText(message.getRole()) ? message.getRole() : "user";
                    return role + ": " + truncateLogText(message.getContent(), 200);
                })
                .collect(Collectors.joining(" | ", "[", "]"));
    }

    private String truncateLogText(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }

    private String extractUpstreamErrorMessage(String responseBody, String fallbackMessage) {
        if (!StringUtils.hasText(responseBody)) {
            return fallbackMessage;
        }
        try {
            Map<String, Object> payload = objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {
            });
            String message = asText(payload.get("msg"));
            if (StringUtils.hasText(message)) {
                return message;
            }
            message = asText(payload.get("message"));
            if (StringUtils.hasText(message)) {
                return message;
            }
            Object errorObject = payload.get("error");
            if (errorObject instanceof Map) {
                Map<?, ?> errorMap = (Map<?, ?>) errorObject;
                message = asText(errorMap.get("message"));
                if (StringUtils.hasText(message)) {
                    return message;
                }
            }
        } catch (Exception ignored) {
            return fallbackMessage;
        }
        return fallbackMessage;
    }

    private AssistantInterpretation interpretWithRules(List<ConversationMessage> messages, List<Student> students) {
        List<ConversationMessage> userMessages = extractUserMessages(messages);
        AssistantInterpretation interpretation = new AssistantInterpretation();
        interpretation.setAnalysisMode("RULE_BASED");
        interpretation.setIntent("ASK_CLARIFICATION");

        if (userMessages.isEmpty()) {
            interpretation.setReply("请先告诉我排课需求。你可以分几次说，我会把学生、周几、时间和开始日期逐步补齐。");
            interpretation.getMissingFields().add("排课需求");
            return interpretation;
        }

        LocalDate today = LocalDate.now();
        for (ConversationMessage message : userMessages) {
            String content = asText(message.getContent());
            if (!StringUtils.hasText(content)) {
                continue;
            }

            String matchedStudentName = matchStudentName(content, students);
            if (StringUtils.hasText(matchedStudentName)) {
                interpretation.setStudentName(matchedStudentName);
            }

            LocalDate parsedStartDate = parseStartDate(content, today);
            if (parsedStartDate != null) {
                interpretation.setStartDate(parsedStartDate);
            }

            List<Integer> parsedWeekdays = parseWeekdays(content);
            if (!CollectionUtils.isEmpty(parsedWeekdays)) {
                interpretation.setWeekdays(parsedWeekdays);
            }

            TimeRange timeRange = parseTimeRange(content);
            if (timeRange != null) {
                interpretation.setStartTime(timeRange.getStartTime());
                interpretation.setEndTime(timeRange.getEndTime());
            }
        }

        List<String> missingFields = collectMissingFields(interpretation);
        interpretation.setMissingFields(missingFields);
        if (!missingFields.isEmpty()) {
            interpretation.setReply(buildClarificationReply(interpretation));
            return interpretation;
        }

        interpretation.setIntent("AUTO_GENERATE_SCHEDULE");
        interpretation.setReply(buildConfirmationReply(interpretation));
        return interpretation;
    }

    private AssistantInterpretation buildMissingInformationReply(List<String> missingFields) {
        AssistantInterpretation interpretation = new AssistantInterpretation();
        interpretation.setIntent("ASK_CLARIFICATION");
        interpretation.setAnalysisMode("AI");
        interpretation.setMissingFields(missingFields);
        interpretation.setReply(buildClarificationReply(interpretation));
        return interpretation;
    }

    private List<ConversationMessage> extractUserMessages(List<ConversationMessage> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return Collections.emptyList();
        }
        List<ConversationMessage> userMessages = new ArrayList<>();
        for (ConversationMessage message : messages) {
            if (message == null) {
                continue;
            }
            if ("user".equalsIgnoreCase(message.getRole()) && StringUtils.hasText(message.getContent())) {
                userMessages.add(message);
            }
        }
        return userMessages;
    }

    private List<String> collectMissingFields(AssistantInterpretation interpretation) {
        List<String> missingFields = new ArrayList<>();
        if (!StringUtils.hasText(interpretation.getStudentName())) {
            missingFields.add("学生姓名");
        }
        if (CollectionUtils.isEmpty(interpretation.getWeekdays())) {
            missingFields.add("每周上课日");
        }
        if (interpretation.getStartDate() == null) {
            missingFields.add("开始日期");
        }
        if (interpretation.getStartTime() == null || interpretation.getEndTime() == null) {
            missingFields.add("上课时间");
        }
        return missingFields;
    }

    private String buildClarificationReply(AssistantInterpretation interpretation) {
        List<String> missingFields = interpretation.getMissingFields();
        if (CollectionUtils.isEmpty(missingFields)) {
            return "请继续补充排课信息。";
        }

        StringBuilder reply = new StringBuilder();
        String recognizedSummary = buildRecognizedSummary(interpretation);
        if (StringUtils.hasText(recognizedSummary)) {
            reply.append("我已经记住：").append(recognizedSummary).append("。");
        }
        reply.append("还差").append(String.join("、", missingFields)).append("。");
        if (missingFields.size() == 1) {
            reply.append(buildSingleFieldPrompt(missingFields.get(0)));
        } else {
            reply.append("你可以分开发，我会继续补全，不需要把整句话重新发一遍。");
        }
        return reply.toString();
    }

    private String buildConfirmationReply(AssistantInterpretation interpretation) {
        return String.format(
                "我理解为：给 %s 从 %s 开始，安排每周 %s %s-%s 的正式课程。",
                interpretation.getStudentName(),
                interpretation.getStartDate(),
                formatWeekdays(interpretation.getWeekdays()),
                interpretation.getStartTime(),
                interpretation.getEndTime());
    }

    private String buildRecognizedSummary(AssistantInterpretation interpretation) {
        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(interpretation.getStudentName())) {
            parts.add("学生 " + interpretation.getStudentName());
        }
        if (!CollectionUtils.isEmpty(interpretation.getWeekdays())) {
            parts.add("每周 " + formatWeekdays(interpretation.getWeekdays()));
        }
        if (interpretation.getStartTime() != null && interpretation.getEndTime() != null) {
            parts.add("时间 " + interpretation.getStartTime() + "-" + interpretation.getEndTime());
        }
        if (interpretation.getStartDate() != null) {
            parts.add("从 " + interpretation.getStartDate() + " 开始");
        }
        return String.join("，", parts);
    }

    private String buildSingleFieldPrompt(String missingField) {
        switch (missingField) {
            case "学生姓名":
                return "直接回复学生名字即可。";
            case "每周上课日":
                return "直接回复例如“周二、周四”即可。";
            case "开始日期":
                return "直接回复例如“从 2026-03-16 开始”或“下周一开始”。";
            case "上课时间":
                return "直接回复例如“19:00-20:30”即可。";
            default:
                return "你可以分开发，我会继续补全。";
        }
    }

    private String buildSystemPrompt(List<Student> students) {
        String studentNames = students == null || students.isEmpty()
                ? "当前暂无已录入学生"
                : students.stream()
                .map(Student::getName)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.joining("、"));
        return "你是教培后台的排课助手，只负责理解用户的自然语言排课请求。\n"
                + "今天日期是 " + LocalDate.now() + "。\n"
                + "当前学生名单：" + studentNames + "。\n"
                + "你会收到完整对话历史，必须结合整段对话累计提取学生、周几、开始日期和上课时间。\n"
                + "如果用户只补充一个字段，你要保留之前已经确认的信息，只追问剩余缺失字段，不要要求用户重发完整需求。\n"
                + "你必须只输出 JSON，不要输出 markdown，不要输出额外解释。\n"
                + "JSON 字段如下：\n"
                + "{\n"
                + "  \"intent\": \"AUTO_GENERATE_SCHEDULE\" | \"ASK_CLARIFICATION\" | \"UNSUPPORTED\",\n"
                + "  \"reply\": \"给前端展示的中文回复\",\n"
                + "  \"studentName\": \"学生姓名，没有则为空字符串\",\n"
                + "  \"weekdays\": [1,2,3,4,5,6,7],\n"
                + "  \"startDate\": \"yyyy-MM-dd，没有则为空字符串\",\n"
                + "  \"startTime\": \"HH:mm，没有则为空字符串\",\n"
                + "  \"endTime\": \"HH:mm，没有则为空字符串\",\n"
                + "  \"missingFields\": [\"学生姓名\",\"每周上课日\",\"开始日期\",\"上课时间\"],\n"
                + "  \"warnings\": [\"可选提示\"]\n"
                + "}\n"
                + "规则：\n"
                + "1. 如果用户想排正式课，intent 返回 AUTO_GENERATE_SCHEDULE。\n"
                + "2. 如果信息缺失，intent 返回 ASK_CLARIFICATION，并明确指出缺了哪些字段。\n"
                + "3. 如果对话前文已经提供了某些字段，后续 reply 里要明确说明你已经记住了哪些内容。\n"
                + "4. 如果只缺一个字段，reply 应该直接追问这个字段，例如“我已经记住时间和开始日期，还差学生姓名”。\n"
                + "5. 所有日期必须换算成 yyyy-MM-dd，所有时间必须换算成 24 小时制 HH:mm。\n"
                + "6. weekdays 使用周一到周日对应 1 到 7。\n"
                + "7. reply 用中文，简短专业。";
    }

    private String extractLatestUserMessage(List<ConversationMessage> messages) {
        for (int index = messages.size() - 1; index >= 0; index--) {
            ConversationMessage message = messages.get(index);
            if ("user".equalsIgnoreCase(message.getRole()) && StringUtils.hasText(message.getContent())) {
                return message.getContent().trim();
            }
        }
        return messages.get(messages.size() - 1).getContent().trim();
    }

    private String matchStudentName(String text, List<Student> students) {
        if (!StringUtils.hasText(text) || CollectionUtils.isEmpty(students)) {
            return null;
        }
        String normalizedText = normalizeKeyword(text);
        return students.stream()
                .map(Student::getName)
                .filter(StringUtils::hasText)
                .sorted(Comparator.comparingInt(String::length).reversed())
                .filter(name -> normalizedText.contains(normalizeKeyword(name)))
                .findFirst()
                .orElse(null);
    }

    private LocalDate parseStartDate(String text, LocalDate today) {
        if (!StringUtils.hasText(text)) {
            return null;
        }

        Matcher fullDateMatcher = FULL_DATE_PATTERN.matcher(text);
        if (fullDateMatcher.find()) {
            return LocalDate.of(
                    Integer.parseInt(fullDateMatcher.group(1)),
                    Integer.parseInt(fullDateMatcher.group(2)),
                    Integer.parseInt(fullDateMatcher.group(3)));
        }

        Matcher monthDayMatcher = MONTH_DAY_PATTERN.matcher(text);
        if (monthDayMatcher.find()) {
            return LocalDate.of(
                    today.getYear(),
                    Integer.parseInt(monthDayMatcher.group(1)),
                    Integer.parseInt(monthDayMatcher.group(2)));
        }

        if (text.contains("今天")) {
            return today;
        }
        if (text.contains("明天")) {
            return today.plusDays(1);
        }
        if (text.contains("后天")) {
            return today.plusDays(2);
        }

        Matcher relativeWeekdayMatcher = RELATIVE_WEEKDAY_PATTERN.matcher(text);
        if (relativeWeekdayMatcher.find()) {
            String prefix = relativeWeekdayMatcher.group(1);
            DayOfWeek target = toDayOfWeek(relativeWeekdayMatcher.group(2));
            if (target == null) {
                return null;
            }
            if ("下周".equals(prefix)) {
                return today.with(TemporalAdjusters.next(target));
            }
            return today.with(TemporalAdjusters.nextOrSame(target));
        }

        return null;
    }

    private List<Integer> parseWeekdays(String text) {
        if (!StringUtils.hasText(text)) {
            return Collections.emptyList();
        }

        Set<Integer> weekdays = new LinkedHashSet<>();
        String withoutStartDateHints = RELATIVE_WEEKDAY_PATTERN.matcher(text).replaceAll("");
        withoutStartDateHints = FULL_DATE_PATTERN.matcher(withoutStartDateHints).replaceAll("");
        withoutStartDateHints = MONTH_DAY_PATTERN.matcher(withoutStartDateHints).replaceAll("");

        Matcher compactMatcher = WEEKLY_COMPACT_PATTERN.matcher(withoutStartDateHints);
        if (compactMatcher.find()) {
            addWeekdaysFromFragment(compactMatcher.group(1), weekdays);
        }

        Matcher singleMatcher = SINGLE_WEEKDAY_PATTERN.matcher(withoutStartDateHints);
        while (singleMatcher.find()) {
            Integer weekday = toWeekdayNumber(singleMatcher.group(1));
            if (weekday != null) {
                weekdays.add(weekday);
            }
        }

        return new ArrayList<>(weekdays);
    }

    private void addWeekdaysFromFragment(String fragment, Set<Integer> weekdays) {
        if (!StringUtils.hasText(fragment)) {
            return;
        }
        for (char ch : fragment.toCharArray()) {
            Integer weekday = toWeekdayNumber(String.valueOf(ch));
            if (weekday != null) {
                weekdays.add(weekday);
            }
        }
    }

    private TimeRange parseTimeRange(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        Matcher matcher = TIME_RANGE_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }

        String startMeridiem = matcher.group(1);
        String endMeridiem = matcher.group(4);
        LocalTime start = parseTime(matcher.group(2), matcher.group(3), startMeridiem);
        LocalTime end = parseTime(matcher.group(5), matcher.group(6), StringUtils.hasText(endMeridiem) ? endMeridiem : startMeridiem);
        if (start == null || end == null || !end.isAfter(start)) {
            return null;
        }
        return new TimeRange(start, end);
    }

    private LocalTime parseTime(String hourText, String minuteText, String meridiem) {
        if (!StringUtils.hasText(hourText)) {
            return null;
        }
        int hour = Integer.parseInt(hourText);
        int minute = 0;
        if (StringUtils.hasText(minuteText)) {
            minute = "半".equals(minuteText) ? 30 : Integer.parseInt(minuteText);
        }

        if (StringUtils.hasText(meridiem)) {
            if (Arrays.asList("下午", "晚上", "晚").contains(meridiem) && hour < 12) {
                hour += 12;
            } else if ("中午".equals(meridiem) && hour < 11) {
                hour += 12;
            } else if ("凌晨".equals(meridiem) && hour == 12) {
                hour = 0;
            }
        }

        if (hour > 23 || minute > 59) {
            return null;
        }
        return LocalTime.of(hour, minute);
    }

    private Map<String, String> buildChatMessage(String role, String content) {
        Map<String, String> message = new LinkedHashMap<>();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    private String normalizeRole(String role) {
        if (!StringUtils.hasText(role)) {
            return "user";
        }
        String normalized = role.trim().toLowerCase(Locale.ROOT);
        if ("assistant".equals(normalized) || "system".equals(normalized)) {
            return normalized;
        }
        return "user";
    }

    private String normalizeBaseUrl(String baseUrl) {
        String trimmed = baseUrl.trim();
        if (trimmed.endsWith("/")) {
            return trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String extractAssistantContent(Map responseBody) {
        if (responseBody == null) {
            return null;
        }
        Object choicesObject = responseBody.get("choices");
        if (!(choicesObject instanceof List) || ((List) choicesObject).isEmpty()) {
            return null;
        }
        Object firstChoice = ((List) choicesObject).get(0);
        if (!(firstChoice instanceof Map)) {
            return null;
        }
        Object messageObject = ((Map) firstChoice).get("message");
        if (!(messageObject instanceof Map)) {
            return null;
        }
        Object contentObject = ((Map) messageObject).get("content");
        if (contentObject instanceof String) {
            return (String) contentObject;
        }
        if (contentObject instanceof List) {
            return (String) ((List<?>) contentObject).stream()
                    .filter(Map.class::isInstance)
                    .map(Map.class::cast)
                    .map(item -> item.get("text"))
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .collect(Collectors.joining());
        }
        return null;
    }

    private String stripCodeFence(String content) {
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(?:json)?", "");
            trimmed = trimmed.replaceFirst("```$", "");
        }
        return trimmed.trim();
    }

    private String asText(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return StringUtils.hasText(text) ? text : null;
    }

    private String asUpperText(Object value, String defaultValue) {
        String text = asText(value);
        return text == null ? defaultValue : text.toUpperCase(Locale.ROOT);
    }

    private LocalDate parseDateValue(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return LocalDate.parse(value.trim());
    }

    private LocalTime parseTimeValue(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return LocalTime.parse(value.trim());
    }

    private List<String> parseStringList(Object value) {
        if (!(value instanceof List)) {
            return new ArrayList<>();
        }
        List<String> values = new ArrayList<>();
        for (Object item : (List<?>) value) {
            String text = asText(item);
            if (StringUtils.hasText(text)) {
                values.add(text);
            }
        }
        return values;
    }

    private List<Integer> parseWeekdaysFromObject(Object value) {
        if (!(value instanceof List)) {
            return new ArrayList<>();
        }
        List<Integer> weekdays = new ArrayList<>();
        for (Object item : (List<?>) value) {
            if (item instanceof Number) {
                weekdays.add(((Number) item).intValue());
                continue;
            }
            Integer parsed = toWeekdayNumber(asText(item));
            if (parsed != null) {
                weekdays.add(parsed);
            }
        }
        return weekdays.stream()
                .filter(day -> day >= 1 && day <= 7)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    private void normalizeInterpretation(AssistantInterpretation interpretation) {
        if (interpretation.getMissingFields() == null) {
            interpretation.setMissingFields(new ArrayList<String>());
        }
        if (interpretation.getWarnings() == null) {
            interpretation.setWarnings(new ArrayList<String>());
        }
        if (interpretation.getWeekdays() == null) {
            interpretation.setWeekdays(new ArrayList<Integer>());
        }
        if (!StringUtils.hasText(interpretation.getReply())) {
            if ("AUTO_GENERATE_SCHEDULE".equalsIgnoreCase(interpretation.getIntent())) {
                interpretation.setReply("我已经理解你的排课意图，准备开始排课。");
            } else {
                interpretation.setReply(buildClarificationReply(interpretation));
            }
        }
    }

    private String formatWeekdays(List<Integer> weekdays) {
        return weekdays.stream()
                .map(day -> {
                    switch (day) {
                        case 1:
                            return "周一";
                        case 2:
                            return "周二";
                        case 3:
                            return "周三";
                        case 4:
                            return "周四";
                        case 5:
                            return "周五";
                        case 6:
                            return "周六";
                        case 7:
                            return "周日";
                        default:
                            return "";
                    }
                })
                .filter(StringUtils::hasText)
                .collect(Collectors.joining("、"));
    }

    private String normalizeKeyword(String value) {
        return value == null ? "" : value.replaceAll("[\\s·•，,。；;：:（）()]", "");
    }

    private Integer toWeekdayNumber(String weekdayText) {
        if (!StringUtils.hasText(weekdayText)) {
            return null;
        }
        String normalized = weekdayText.trim();
        switch (normalized) {
            case "1":
            case "一":
                return 1;
            case "2":
            case "二":
                return 2;
            case "3":
            case "三":
                return 3;
            case "4":
            case "四":
                return 4;
            case "5":
            case "五":
                return 5;
            case "6":
            case "六":
                return 6;
            case "7":
            case "日":
            case "天":
            case "七":
                return 7;
            default:
                return null;
        }
    }

    private DayOfWeek toDayOfWeek(String weekdayText) {
        Integer weekday = toWeekdayNumber(weekdayText);
        return weekday == null ? null : DayOfWeek.of(weekday);
    }

    private static class TimeRange {
        private final LocalTime startTime;
        private final LocalTime endTime;

        private TimeRange(LocalTime startTime, LocalTime endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }

        public LocalTime getStartTime() {
            return startTime;
        }

        public LocalTime getEndTime() {
            return endTime;
        }
    }
}
