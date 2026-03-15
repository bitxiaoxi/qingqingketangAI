<template>
  <section class="page-stack">
    <PageHeader title="排课管理" />

    <el-card shadow="never" class="feature-card feature-card--planner">
      <div v-if="loading" class="page-state">学生加载中…</div>
      <el-alert v-else-if="error" :title="error" type="error" show-icon :closable="false" />
      <div v-else class="planner-stack">
        <div class="planner-toolbar">
          <h3>选择排课方式</h3>
          <span v-if="selectedCreateStudent" class="planner-student-chip">
            {{ selectedCreateStudent.name }} · {{ selectedCreateStudent.grade }} · 剩余 {{ selectedCreateStudent.remainingLessons ?? 0 }} 节
          </span>
        </div>

        <div class="planner-mode-grid">
          <button type="button" class="planner-mode planner-mode--manual" @click="openCreateDialog()">
            <span class="planner-mode__tag">手动排课</span>
            <strong>固定周期排课</strong>
            <small>按学生、上课日和时间生成</small>
          </button>

          <button type="button" class="planner-mode planner-mode--ai" @click="openAssistantDrawer">
            <span class="planner-mode__tag">AI 排课</span>
            <strong>自然语言排课</strong>
            <small>一句话生成排课草案</small>
          </button>
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
      <div class="dialog-banner">
        <div>
          <strong>3 步完成手动排课</strong>
          <p>先选学生和周期，再选上课日，最后设置时间段。</p>
        </div>
        <div class="dialog-steps">
          <span class="dialog-step">1. 学生</span>
          <span class="dialog-step">2. 上课日</span>
          <span class="dialog-step">3. 时间</span>
        </div>
      </div>

      <el-form label-position="top" class="dialog-form">
        <section class="dialog-block dialog-block--primary">
          <div class="dialog-block__head">
            <h4>基础信息</h4>
            <small>先确定学生、频率和首次课日期</small>
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

          <div v-if="selectedCreateStudent" class="dialog-inline-summary">
            <strong>{{ selectedCreateStudent.name }} · {{ selectedCreateStudent.grade }}</strong>
            <span>剩余 {{ selectedCreateStudent.remainingLessons ?? 0 }} 节课</span>
          </div>

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

            <el-form-item label="首次课日期" required>
              <el-date-picker
                v-model="createForm.startDate"
                type="date"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                placeholder="选择首次课日期"
              />
            </el-form-item>
          </div>

          <p class="dialog-field-hint">每周上课次数会限制可选的上课日数量。</p>
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
              :disabled="createForm.weeklySessions > 1 && !createForm.weekdays.includes(option.value) && createForm.weekdays.length >= createForm.weeklySessions"
              @click="toggleWeekday(option.value)"
            >
              {{ option.label }}
            </button>
          </div>

          <div class="dialog-meta-row">
            <span class="dialog-meta-pill">已选 {{ createForm.weekdays.length }} 天</span>
            <span class="dialog-meta-pill" :data-muted="remainingWeekdaySlots === 0">
              {{ remainingWeekdaySlots > 0 ? `还可再选 ${remainingWeekdaySlots} 天` : '已达到本周次数' }}
            </span>
          </div>
        </section>

        <section class="dialog-block">
          <div class="dialog-block__head">
            <h4>上课时间</h4>
            <small>设置单节课标准时段</small>
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

          <p class="dialog-field-hint">建议按固定时段排课，后续查看课程表会更清晰。</p>
        </section>

        <div class="schedule-preview">
          <span class="schedule-preview__eyebrow">预览</span>
          <strong>{{ createPreview }}</strong>
          <small v-if="selectedCreateStudent">
            剩余 {{ selectedCreateStudent.remainingLessons ?? 0 }} 节课
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
const remainingWeekdaySlots = computed(() => Math.max(0, createForm.weeklySessions - createForm.weekdays.length));

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
  const firstLessonLabel = createForm.startDate
    ? `首次课 ${createForm.startDate}`
    : '未设置首次课日期';
  return `${studentLabel} · ${weekdays} · ${timeRange} · ${firstLessonLabel}`;
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
    if (createForm.weeklySessions === 1) {
      createForm.weekdays = [weekday];
    }
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
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
}

.feature-card--planner {
  position: relative;
  overflow: hidden;
}

.feature-card--planner::before {
  content: '';
  position: absolute;
  inset: 0 0 auto 0;
  height: 160px;
  background:
    radial-gradient(circle at top right, rgba(125, 211, 252, 0.18), transparent 26%),
    linear-gradient(135deg, rgba(59, 130, 246, 0.08), transparent 52%);
  pointer-events: none;
}

.feature-card :deep(.el-card__body) {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.planner-stack {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.planner-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.planner-toolbar h3 {
  font-size: 24px;
  line-height: 1.12;
  letter-spacing: -0.03em;
}

.planner-student-chip {
  display: inline-flex;
  align-items: center;
  max-width: 100%;
  padding: 10px 14px;
  border: 1px solid rgba(191, 219, 254, 0.9);
  border-radius: 999px;
  background: rgba(239, 246, 255, 0.92);
  color: #1e3a8a;
  font-size: 13px;
  white-space: normal;
}

.planner-mode-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.planner-mode {
  padding: 24px;
  border: 1px solid rgba(226, 232, 240, 0.92);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.92);
  text-align: left;
  cursor: pointer;
  box-shadow: 0 14px 28px rgba(15, 23, 42, 0.04);
  transition: transform 180ms ease, border-color 180ms ease, box-shadow 180ms ease, background 180ms ease;
}

.planner-mode:hover {
  transform: translateY(-2px);
  box-shadow: 0 18px 34px rgba(15, 23, 42, 0.08);
}

.planner-mode__tag {
  display: inline-flex;
  margin-bottom: 14px;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.66);
  color: var(--app-text-secondary);
  font-size: 12px;
  letter-spacing: 0.08em;
  line-height: 1;
}

.planner-mode strong {
  display: block;
  margin-bottom: 8px;
  font-size: 22px;
  line-height: 1.2;
}

.planner-mode small {
  display: block;
  color: var(--app-text-secondary);
  line-height: 1.6;
}

.planner-mode--manual {
  border-color: rgba(147, 197, 253, 0.72);
  background: linear-gradient(145deg, rgba(219, 234, 254, 0.78), rgba(255, 255, 255, 0.96));
}

.planner-mode--ai {
  border-color: rgba(253, 186, 116, 0.72);
  background: linear-gradient(145deg, rgba(255, 237, 213, 0.78), rgba(255, 255, 255, 0.96));
}

.schedule-dialog :deep(.el-dialog) {
  border-radius: 28px;
}

.schedule-dialog :deep(.el-dialog__body),
.assistant-drawer :deep(.el-drawer__body) {
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
}

.schedule-preview__eyebrow {
  display: inline-flex;
  margin-bottom: 8px;
  color: var(--app-text-tertiary);
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.dialog-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
  padding: 18px 20px;
  border: 1px solid rgba(191, 219, 254, 0.92);
  border-radius: 22px;
  background: linear-gradient(135deg, rgba(239, 246, 255, 0.98), rgba(255, 255, 255, 0.94));
}

.dialog-banner strong {
  display: block;
  margin-bottom: 4px;
  font-size: 18px;
}

.dialog-banner p {
  color: var(--app-text-secondary);
  line-height: 1.6;
}

.dialog-steps {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.dialog-step {
  display: inline-flex;
  align-items: center;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(191, 219, 254, 0.88);
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 700;
}

.dialog-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.dialog-block {
  padding: 18px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.92);
}

.dialog-block--primary {
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.98), rgba(248, 250, 252, 0.96));
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.62);
}

.dialog-block__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.dialog-block__head h4 {
  margin: 0;
  font-size: 16px;
}

.dialog-block__head small {
  margin: 0;
  color: var(--app-text-secondary);
  line-height: 1.6;
}

.dialog-inline-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: -4px;
  margin-bottom: 14px;
  padding: 12px 14px;
  border-radius: 16px;
  background: rgba(239, 246, 255, 0.82);
  border: 1px solid rgba(191, 219, 254, 0.88);
}

.dialog-inline-summary strong {
  font-size: 14px;
  line-height: 1.4;
}

.dialog-inline-summary span {
  color: #1d4ed8;
  font-size: 12px;
  white-space: nowrap;
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
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.92);
  color: var(--app-text-secondary);
  cursor: pointer;
  transition: border-color 180ms ease, background 180ms ease, transform 180ms ease, box-shadow 180ms ease;
}

.weekday-chip[data-active='true'] {
  border-color: rgba(59, 130, 246, 0.34);
  background: rgba(219, 234, 254, 0.78);
  box-shadow: inset 0 0 0 1px rgba(191, 219, 254, 0.65);
  color: var(--app-primary);
}

.weekday-chip:disabled {
  cursor: not-allowed;
  opacity: 0.46;
}

.weekday-chip:not(:disabled):hover {
  transform: translateY(-1px);
}

.dialog-meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.dialog-meta-pill {
  display: inline-flex;
  align-items: center;
  padding: 7px 10px;
  border-radius: 999px;
  background: rgba(248, 250, 252, 0.96);
  border: 1px solid rgba(226, 232, 240, 0.88);
  color: var(--app-text-secondary);
  font-size: 12px;
}

.dialog-meta-pill[data-muted='true'] {
  color: #1d4ed8;
  border-color: rgba(191, 219, 254, 0.88);
  background: rgba(239, 246, 255, 0.88);
}

.dialog-field-hint {
  margin-top: 12px;
  color: var(--app-text-secondary);
  font-size: 12px;
  line-height: 1.7;
}

.schedule-preview {
  padding: 18px;
  border: 1px solid rgba(191, 219, 254, 0.9);
  border-radius: 20px;
  background: linear-gradient(145deg, #0f172a, #1e3a8a);
  color: rgba(255, 255, 255, 0.94);
}

.schedule-preview strong {
  display: block;
  margin-bottom: 6px;
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
  gap: 14px;
}

.assistant-examples {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.assistant-example {
  padding: 10px 12px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  color: var(--app-text-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: border-color 180ms ease, transform 180ms ease, background 180ms ease;
}

.assistant-example:hover {
  transform: translateY(-1px);
  border-color: rgba(191, 219, 254, 0.88);
  background: rgba(239, 246, 255, 0.9);
}

.assistant-thread {
  max-height: 360px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 6px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.92);
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
  border-radius: 16px;
  background: rgba(248, 250, 252, 0.98);
  color: var(--app-text-primary);
  line-height: 1.7;
}

.assistant-message[data-role='user'] .assistant-message__bubble {
  background: rgba(219, 234, 254, 0.92);
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

.dialog-form :deep(.el-select__wrapper),
.dialog-form :deep(.el-input__wrapper),
.dialog-form :deep(.el-textarea__inner),
.dialog-form :deep(.el-date-editor.el-input__wrapper),
.dialog-form :deep(.el-date-editor.el-input__wrapper.is-focus),
.dialog-form :deep(.el-input__wrapper.is-focus),
.dialog-form :deep(.el-select__wrapper.is-focused),
.dialog-form :deep(.el-textarea__inner:focus),
.assistant-stack :deep(.el-textarea__inner) {
  border-radius: 16px;
}

.dialog-form :deep(.el-select__wrapper),
.dialog-form :deep(.el-input__wrapper),
.dialog-form :deep(.el-date-editor.el-input__wrapper) {
  min-height: 46px;
  background: rgba(248, 250, 252, 0.92);
  box-shadow: none;
}

.dialog-form :deep(.el-input__wrapper.is-focus),
.dialog-form :deep(.el-select__wrapper.is-focused),
.dialog-form :deep(.el-date-editor.el-input__wrapper.is-focus) {
  background: #ffffff;
  box-shadow: 0 0 0 1px rgba(147, 197, 253, 0.82) inset;
}

.assistant-stack :deep(.el-textarea__inner) {
  min-height: 112px;
  padding: 14px 16px;
}

.page-state {
  color: var(--app-text-secondary);
  font-size: 14px;
}

@media (max-width: 1200px) {
  .assistant-actions,
  .planner-toolbar,
  .dialog-banner,
  .dialog-inline-summary {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 900px) {
  .dialog-grid,
  .planner-mode-grid,
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
