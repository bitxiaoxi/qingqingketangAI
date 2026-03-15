<template>
  <section class="page-stack">
    <PageHeader
      title="排课管理"
      description="集中处理手动排课与 AI 排课，为学生快速生成正式课课表。"
    >
      <template #actions>
        <el-button type="primary" @click="openCreateDialog()">新建排课</el-button>
        <el-button @click="openAssistantDrawer">AI 排课</el-button>
      </template>
    </PageHeader>

    <el-card shadow="never" class="feature-card feature-card--planner">
      <div class="feature-card__head">
        <div>
          <span class="feature-card__eyebrow">Schedule Manager</span>
          <h3>手动排课与 AI 排课</h3>
          <p class="feature-card__description">先选学生，再选择固定排课或自然语言生成排课草案。</p>
        </div>
        <span v-if="selectedCreateStudent" class="feature-card__meta">
          {{ selectedCreateStudent.name }} · {{ selectedCreateStudent.grade }}
        </span>
      </div>

      <div v-if="loading" class="page-state">学生加载中…</div>
      <el-alert v-else-if="error" :title="error" type="error" show-icon :closable="false" />
      <div v-else class="planner-stack">
        <div v-if="selectedCreateStudent" class="planner-focus">
          <span class="planner-focus__eyebrow">当前排课学生</span>
          <strong>{{ selectedCreateStudent.name }} · {{ selectedCreateStudent.grade }}</strong>
          <p>
            剩余 {{ selectedCreateStudent.remainingLessons ?? 0 }} 节课，可直接创建周期课表，或用 AI 帮你生成草案。
          </p>
        </div>

        <div class="planner-mode-grid">
          <button type="button" class="planner-mode planner-mode--manual" @click="openCreateDialog()">
            <span>手动排课</span>
            <strong>新建排课</strong>
          </button>

          <button type="button" class="planner-mode planner-mode--ai" @click="openAssistantDrawer">
            <span>AI 排课</span>
            <strong>智能生成</strong>
          </button>
        </div>

        <div class="planner-notes">
          <article class="planner-note">
            <strong>手动排课</strong>
            <p>适合固定上课日、固定时间段的正式课批量生成。</p>
          </article>
          <article class="planner-note">
            <strong>AI 排课</strong>
            <p>适合用自然语言快速描述学生、上课日和时间，系统会自动解析。</p>
          </article>
        </div>
      </div>
    </el-card>

    <el-dialog
      v-model="createDialogVisible"
      title="新建排课"
      width="760px"
      destroy-on-close
      class="schedule-dialog"
    >
      <div class="dialog-intro">
        <span class="dialog-intro__eyebrow">Create Schedule</span>
        <p>填写学生、上课频率、日期与时间后，系统会按剩余课时自动向后排满课表。</p>
      </div>

      <el-form label-position="top" class="dialog-form">
        <section class="dialog-block">
          <div class="dialog-block__head">
            <h4>学生与周期</h4>
            <small>先确认学生，再设置排课从哪一天开始生效。</small>
          </div>

          <el-form-item label="学生" required>
            <el-select v-model="createForm.studentId" filterable placeholder="请选择学生">
              <el-option
                v-for="student in students"
                :key="student.id"
                :label="`${student.name} · ${student.grade}`"
                :value="student.id"
              />
            </el-select>
          </el-form-item>

          <div class="dialog-grid">
            <el-form-item label="每周上课次数" required>
              <el-select v-model="createForm.weeklySessions">
                <el-option
                  v-for="option in frequencyOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="开始日期" required>
              <el-date-picker
                v-model="createForm.startDate"
                type="date"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                placeholder="选择开始日期"
              />
            </el-form-item>
          </div>
        </section>

        <section class="dialog-block">
          <div class="dialog-block__head">
            <h4>每周上课日</h4>
            <small>{{ weekdaySummary }}</small>
          </div>

          <div class="weekday-chip-grid">
            <button
              v-for="option in weekdayOptions"
              :key="option.value"
              type="button"
              class="weekday-chip"
              :data-active="createForm.weekdays.includes(option.value)"
              :disabled="!createForm.weekdays.includes(option.value) && createForm.weekdays.length >= createForm.weeklySessions"
              @click="toggleWeekday(option.value)"
            >
              {{ option.label }}
            </button>
          </div>
        </section>

        <section class="dialog-block">
          <div class="dialog-block__head">
            <h4>上课时间</h4>
            <small>设置单节课的标准时间段。</small>
          </div>

          <div class="dialog-grid">
            <el-form-item label="开始时间" required>
              <el-time-picker
                v-model="createForm.startTime"
                value-format="HH:mm"
                format="HH:mm"
                placeholder="开始时间"
              />
            </el-form-item>

            <el-form-item label="结束时间" required>
              <el-time-picker
                v-model="createForm.endTime"
                value-format="HH:mm"
                format="HH:mm"
                placeholder="结束时间"
              />
            </el-form-item>
          </div>
        </section>

        <div class="schedule-preview">
          <span class="schedule-preview__eyebrow">Schedule Preview</span>
          <strong>{{ createPreview }}</strong>
          <small v-if="selectedCreateStudent">
            当前学生剩余 {{ selectedCreateStudent.remainingLessons ?? 0 }} 节课，系统会基于这个课时数自动排满。
          </small>
        </div>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="createDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="creating" @click="submitCreateForm">生成课表</el-button>
        </div>
      </template>
    </el-dialog>

    <el-drawer
      v-model="assistantDrawerVisible"
      size="560px"
      title="AI 排课"
      destroy-on-close
      class="assistant-drawer"
    >
      <div class="assistant-stack">
        <div class="assistant-guide">
          <span class="assistant-guide__eyebrow">AI Assistant</span>
          <h4>自然语言生成排课草案</h4>
          <p>直接描述学生、每周上课日、开始日期和时间段，系统会解析并尝试自动排课。</p>
        </div>

        <div class="assistant-examples">
          <button
            v-for="example in scheduleAssistantExamples"
            :key="example"
            type="button"
            class="assistant-example"
            @click="assistantInput = example"
          >
            {{ example }}
          </button>
        </div>

        <div ref="assistantThreadRef" class="assistant-thread">
          <article
            v-for="message in assistantMessages"
            :key="message.id"
            class="assistant-message"
            :data-role="message.role"
          >
            <div class="assistant-message__meta">
              <span>{{ message.role === 'assistant' ? '排课助手' : '你' }}</span>
              <small v-if="message.meta">{{ message.meta }}</small>
            </div>
            <div class="assistant-message__bubble">
              <p>{{ message.content }}</p>
              <ul v-if="message.warnings?.length">
                <li v-for="warning in message.warnings" :key="`${message.id}-${warning}`">{{ warning }}</li>
              </ul>
            </div>
          </article>
        </div>

        <el-input
          v-model="assistantInput"
          type="textarea"
          :rows="4"
          :placeholder="assistantPlaceholder"
        />

        <div class="assistant-actions">
          <span class="field-hint">
            {{ assistantPendingFields.length ? `当前待补：${assistantPendingFields.join('、')}` : '支持多轮补充，系统会记住上下文。' }}
          </span>
          <div class="assistant-actions__buttons">
            <el-button @click="resetAssistantConversation">重置</el-button>
            <el-button type="primary" :loading="assistantSubmitting" @click="submitAssistant">
              发送并排课
            </el-button>
          </div>
        </div>
      </div>
    </el-drawer>
  </section>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import PageHeader from '../components/common/PageHeader.vue';
import { frequencyOptions, scheduleAssistantExamples, weekdayOptions } from '../constants/options';
import { api } from '../services/api';
import {
  buildWeekdaySummary,
  formatDateParam,
  formatParsedIntentSummary,
  normalizeError
} from '../utils/format';

const route = useRoute();

const loading = ref(false);
const error = ref('');
const students = ref([]);
const creating = ref(false);
const createDialogVisible = ref(false);
const assistantDrawerVisible = ref(false);
const createForm = reactive({
  studentId: route.query.studentId ? Number(route.query.studentId) : null,
  weeklySessions: 1,
  weekdays: [weekdayOptions[0].value],
  startDate: formatDateParam(new Date()),
  startTime: '19:00',
  endTime: '20:30'
});

const assistantThreadRef = ref(null);
const assistantMessages = ref([]);
const assistantInput = ref('');
const assistantPendingFields = ref([]);
const assistantSubmitting = ref(false);
let assistantMessageId = 0;

const createAssistantMessage = (role, content, options = {}) => {
  assistantMessageId += 1;
  return {
    id: assistantMessageId,
    role,
    content,
    meta: options.meta ?? '',
    warnings: options.warnings ?? []
  };
};

const resetAssistantConversation = async () => {
  assistantMessages.value = [
    createAssistantMessage(
      'assistant',
      '告诉我学生姓名、每周上课日、时间段和开始日期。你可以分多轮补充，我会记住已经识别到的信息。',
      { meta: 'AI 对话排课' }
    )
  ];
  assistantInput.value = '';
  assistantPendingFields.value = [];
  await scrollAssistantThread();
};

const scrollAssistantThread = async () => {
  await nextTick();
  const target = assistantThreadRef.value;
  if (target) {
    target.scrollTop = target.scrollHeight;
  }
};

const loadStudents = async () => {
  loading.value = true;
  error.value = '';
  try {
    students.value = await api.listStudents();
  } catch (requestError) {
    error.value = normalizeError(requestError, '学生信息加载失败');
  } finally {
    loading.value = false;
  }
};

const weekdaySummary = computed(() => buildWeekdaySummary(createForm.weekdays, createForm.weeklySessions, weekdayOptions));

const selectedCreateStudent = computed(() => {
  return students.value.find((student) => student.id === createForm.studentId) ?? null;
});

const createPreview = computed(() => {
  const studentLabel = selectedCreateStudent.value
    ? `${selectedCreateStudent.value.name} · ${selectedCreateStudent.value.grade}`
    : '未选择学生';
  const weekdays = weekdayOptions
    .filter((option) => createForm.weekdays.includes(option.value))
    .map((option) => option.label)
    .join('、') || '未选择上课日';
  const timeRange = createForm.startTime && createForm.endTime
    ? `${createForm.startTime}-${createForm.endTime}`
    : '未设置时间';
  const startDate = createForm.startDate || '未设置开始日期';
  return `${studentLabel} · ${weekdays} · ${timeRange} · 从 ${startDate} 开始`;
});

const assistantPlaceholder = computed(() => {
  if (assistantPendingFields.value.includes('学生姓名')) {
    return '直接回复学生姓名，例如：李晓明';
  }
  if (assistantPendingFields.value.includes('每周上课日')) {
    return '直接回复每周上课日，例如：周二、周四';
  }
  if (assistantPendingFields.value.includes('开始日期')) {
    return '直接回复开始日期，例如：从 2026-03-16 开始';
  }
  if (assistantPendingFields.value.includes('上课时间')) {
    return '直接回复上课时间，例如：19:00-20:30';
  }
  return '例如：李晓明每周二、周四 19:00-20:30，从下周一开始上课';
});

const resetCreateForm = (studentId = null) => {
  createForm.studentId = studentId;
  createForm.weeklySessions = 1;
  createForm.weekdays = [weekdayOptions[0].value];
  createForm.startDate = formatDateParam(new Date());
  createForm.startTime = '19:00';
  createForm.endTime = '20:30';
};

const openCreateDialog = (studentId = route.query.studentId ? Number(route.query.studentId) : null) => {
  resetCreateForm(studentId);
  createDialogVisible.value = true;
};

const toggleWeekday = (weekday) => {
  const selected = [...createForm.weekdays];
  if (selected.includes(weekday)) {
    if (selected.length === 1) {
      return;
    }
    createForm.weekdays = selected.filter((item) => item !== weekday).sort((left, right) => left - right);
    return;
  }
  if (selected.length >= createForm.weeklySessions) {
    return;
  }
  createForm.weekdays = [...selected, weekday].sort((left, right) => left - right);
};

const submitCreateForm = async () => {
  if (!createForm.studentId || !createForm.startDate || !createForm.startTime || !createForm.endTime) {
    ElMessage.error('请完整填写排课信息');
    return;
  }
  if (createForm.weekdays.length !== createForm.weeklySessions) {
    ElMessage.error(`每周 ${createForm.weeklySessions} 次课，请选择 ${createForm.weeklySessions} 个上课日`);
    return;
  }

  creating.value = true;
  try {
    const generated = await api.generateSchedules(createForm.studentId, {
      weekdays: [...createForm.weekdays].sort((left, right) => left - right),
      startDate: createForm.startDate,
      startTime: createForm.startTime,
      endTime: createForm.endTime
    });
    createDialogVisible.value = false;
    ElMessage.success(`已生成 ${Array.isArray(generated) ? generated.length : 0} 节课`);
  } catch (requestError) {
    ElMessage.error(normalizeError(requestError, '排课失败'));
  } finally {
    creating.value = false;
  }
};

const openAssistantDrawer = async () => {
  assistantDrawerVisible.value = true;
  if (!assistantMessages.value.length) {
    await resetAssistantConversation();
    return;
  }
  await scrollAssistantThread();
};

const submitAssistant = async () => {
  if (!assistantInput.value.trim()) {
    return;
  }

  const userMessage = createAssistantMessage('user', assistantInput.value.trim());
  assistantMessages.value = [...assistantMessages.value, userMessage];
  assistantInput.value = '';
  assistantSubmitting.value = true;
  await scrollAssistantThread();

  try {
    const data = await api.assistantArrange(
      assistantMessages.value.map((message) => ({
        role: message.role,
        content: message.content
      }))
    );

    assistantPendingFields.value = Array.isArray(data?.parsedIntent?.missingFields)
      ? data.parsedIntent.missingFields
      : [];

    const metaParts = [];
    if (data?.analysisMode === 'AI') {
      metaParts.push('AI 解析');
    } else if (data?.analysisMode) {
      metaParts.push('规则解析');
    }
    const parsedSummary = formatParsedIntentSummary(data?.parsedIntent);
    if (parsedSummary) {
      metaParts.push(parsedSummary);
    }
    if (data?.scheduled) {
      metaParts.push(`已排 ${data.scheduledCount ?? 0} 节`);
      assistantPendingFields.value = [];
    }

    assistantMessages.value = [
      ...assistantMessages.value,
      createAssistantMessage('assistant', data?.reply ?? '已收到排课请求。', {
        meta: metaParts.join(' · '),
        warnings: data?.warnings ?? []
      })
    ];
    await scrollAssistantThread();

    if (data?.scheduled) {
      ElMessage.success(`已生成 ${data.scheduledCount ?? 0} 节课`);
    }
  } catch (requestError) {
    const message = normalizeError(requestError, '智能排课失败');
    assistantMessages.value = [
      ...assistantMessages.value,
      createAssistantMessage('assistant', message, { meta: '系统提示' })
    ];
    await scrollAssistantThread();
    ElMessage.error(message);
  } finally {
    assistantSubmitting.value = false;
  }
};

watch(
  () => createForm.weeklySessions,
  (value) => {
    if (createForm.weekdays.length > value) {
      createForm.weekdays = createForm.weekdays.slice(0, value);
    }
  }
);

watch(
  () => route.query.action,
  (action) => {
    if (action === 'create') {
      openCreateDialog(route.query.studentId ? Number(route.query.studentId) : null);
    }
  },
  { immediate: true }
);

watch(
  () => route.query.studentId,
  (studentId) => {
    createForm.studentId = studentId ? Number(studentId) : null;
  },
  { immediate: true }
);

onMounted(async () => {
  await loadStudents();
  await resetAssistantConversation();
});
</script>

<style scoped>
.feature-card {
  background: var(--app-surface);
}

.feature-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.feature-card__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.feature-card__eyebrow {
  display: inline-flex;
  margin-bottom: 8px;
  color: var(--app-text-tertiary);
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.feature-card__head h3 {
  font-size: 24px;
  line-height: 1.15;
}

.feature-card__description {
  margin-top: 8px;
  color: var(--app-text-secondary);
  line-height: 1.7;
}

.feature-card__meta {
  display: inline-flex;
  align-items: center;
  padding: 9px 12px;
  border: 1px solid var(--app-border);
  border-radius: 999px;
  background: var(--app-primary-soft);
  color: var(--app-text-secondary);
  font-size: 13px;
  white-space: nowrap;
}

.planner-stack {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.planner-focus {
  padding: 18px;
  border: 1px solid rgba(59, 130, 246, 0.12);
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(239, 246, 255, 0.92), rgba(255, 255, 255, 0.94));
}

.planner-focus__eyebrow {
  display: inline-flex;
  margin-bottom: 10px;
  color: var(--app-text-tertiary);
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.planner-focus strong {
  display: block;
  margin-bottom: 8px;
  font-size: 22px;
  line-height: 1.1;
}

.planner-focus p {
  color: var(--app-text-secondary);
  line-height: 1.7;
}

.planner-mode-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.planner-mode {
  padding: 20px;
  border: 1px solid var(--app-border);
  border-radius: 18px;
  background: var(--app-surface-muted);
  text-align: left;
  cursor: pointer;
  transition: transform 180ms ease, border-color 180ms ease, box-shadow 180ms ease;
}

.planner-mode:hover {
  transform: translateY(-2px);
  box-shadow: 0 14px 28px rgba(15, 23, 42, 0.08);
}

.planner-mode span {
  display: inline-flex;
  margin-bottom: 16px;
  color: var(--app-text-tertiary);
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.planner-mode strong {
  display: block;
  font-size: 20px;
  line-height: 1.2;
}

.planner-mode--manual {
  border-color: rgba(59, 130, 246, 0.16);
  background: linear-gradient(145deg, rgba(59, 130, 246, 0.08), rgba(255, 255, 255, 0.94));
}

.planner-mode--ai {
  border-color: rgba(245, 158, 11, 0.18);
  background: linear-gradient(145deg, rgba(245, 158, 11, 0.08), rgba(255, 255, 255, 0.94));
}

.planner-notes {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.planner-note {
  padding: 16px;
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-sm);
  background: var(--app-surface-muted);
}

.planner-note strong {
  display: block;
  margin-bottom: 8px;
  font-size: 16px;
}

.planner-note p {
  color: var(--app-text-secondary);
  line-height: 1.7;
}

.dialog-intro {
  margin-bottom: 20px;
  padding: 14px 16px;
  border: 1px solid rgba(59, 130, 246, 0.12);
  border-radius: var(--app-radius-sm);
  background: var(--app-primary-soft);
}

.dialog-intro__eyebrow,
.schedule-preview__eyebrow,
.assistant-guide__eyebrow {
  display: inline-flex;
  margin-bottom: 8px;
  color: var(--app-text-tertiary);
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.dialog-intro p {
  color: var(--app-text-secondary);
  line-height: 1.7;
}

.dialog-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.dialog-block {
  padding: 16px;
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-sm);
  background: var(--app-surface);
}

.dialog-block__head {
  margin-bottom: 16px;
}

.dialog-block__head h4 {
  margin-bottom: 4px;
  font-size: 16px;
}

.dialog-block__head small {
  color: var(--app-text-secondary);
  line-height: 1.6;
}

.dialog-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.weekday-chip-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.weekday-chip {
  padding: 12px 10px;
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-sm);
  background: var(--app-surface);
  color: var(--app-text-secondary);
  cursor: pointer;
  transition: border-color 180ms ease, background 180ms ease, transform 180ms ease;
}

.weekday-chip[data-active='true'] {
  border-color: rgba(59, 130, 246, 0.34);
  background: rgba(59, 130, 246, 0.08);
  color: var(--app-primary);
}

.weekday-chip:disabled {
  cursor: not-allowed;
  opacity: 0.46;
}

.weekday-chip:not(:disabled):hover {
  transform: translateY(-1px);
}

.schedule-preview {
  padding: 16px;
  border-radius: var(--app-radius-sm);
  background: #111827;
  color: rgba(255, 255, 255, 0.92);
}

.schedule-preview strong {
  display: block;
  margin-bottom: 8px;
  font-size: 17px;
  line-height: 1.6;
}

.schedule-preview small {
  color: rgba(255, 255, 255, 0.68);
  line-height: 1.7;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.field-hint {
  display: inline-flex;
  color: var(--app-text-secondary);
  font-size: 12px;
  line-height: 1.6;
}

.assistant-stack {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.assistant-guide {
  padding: 14px 16px;
  border: 1px solid rgba(59, 130, 246, 0.12);
  border-radius: var(--app-radius-sm);
  background: var(--app-primary-soft);
}

.assistant-guide h4 {
  margin-bottom: 8px;
  font-size: 20px;
}

.assistant-guide p {
  color: var(--app-text-secondary);
  line-height: 1.8;
}

.assistant-examples {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.assistant-example {
  padding: 10px 12px;
  border: 1px solid var(--app-border);
  border-radius: 999px;
  background: var(--app-surface);
  color: var(--app-text-secondary);
  font-size: 13px;
  cursor: pointer;
}

.assistant-thread {
  max-height: 360px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-right: 4px;
}

.assistant-message {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.assistant-message__meta {
  display: flex;
  gap: 8px;
  color: var(--app-text-tertiary);
  font-size: 12px;
}

.assistant-message__bubble {
  padding: 12px 14px;
  border-radius: var(--app-radius-sm);
  background: var(--app-surface-muted);
  color: var(--app-text-primary);
  line-height: 1.7;
}

.assistant-message[data-role='user'] .assistant-message__bubble {
  background: var(--app-primary-soft);
}

.assistant-message__bubble ul {
  margin-top: 8px;
  padding-left: 18px;
  color: var(--app-text-secondary);
}

.assistant-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.assistant-actions__buttons {
  display: flex;
  gap: 12px;
}

.page-state {
  color: var(--app-text-secondary);
  font-size: 14px;
}

@media (max-width: 1200px) {
  .assistant-actions,
  .feature-card__head {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 900px) {
  .dialog-grid,
  .planner-mode-grid,
  .planner-notes,
  .weekday-chip-grid {
    grid-template-columns: 1fr;
  }

  .assistant-actions {
    width: 100%;
    flex-direction: column;
    align-items: flex-start;
  }

  .page-header__actions {
    width: 100%;
  }
}
</style>
