<template>
  <section class="page-stack">
    <PageHeader
      eyebrow="Schedule"
      title="课程管理"
      description="把新建排课、周课表查看和 AI 排课集中在一个清晰的排课工作台。"
    >
      <template #actions>
        <div class="page-actions">
          <el-button type="primary" @click="openCreateDialog()">新建排课</el-button>
          <el-button @click="jumpToCurrentWeek">查看周课表</el-button>
          <el-button type="primary" plain @click="openAssistantDrawer">AI 排课</el-button>
        </div>
      </template>
    </PageHeader>

    <el-card shadow="never" class="planner-hero">
      <div class="planner-hero__body">
        <div class="planner-hero__intro">
          <span class="planner-hero__eyebrow">Schedule Studio</span>
          <h3>专业的排课工作台</h3>
          <p>
            以周视图为核心，统一处理正式课排期、批量生成课表、销课状态和 AI 排课建议。
          </p>
          <div class="planner-hero__chips">
            <span class="planner-chip">{{ currentWeekLabel }}</span>
            <span class="planner-chip">{{ filteredSchedules.length }} 节课程</span>
            <span class="planner-chip">AI 排课已接入</span>
          </div>
        </div>

        <div class="planner-hero__stats">
          <article v-for="item in overviewStats" :key="item.label" class="planner-stat">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <small>{{ item.helper }}</small>
          </article>
        </div>
      </div>
    </el-card>

    <el-card shadow="never" class="schedule-control-card">
      <div class="schedule-control">
        <div class="schedule-control__summary">
          <span class="schedule-control__eyebrow">Week Calendar</span>
          <strong>{{ currentWeekLabel }}</strong>
          <p>{{ filterSummary }}</p>
        </div>

        <div class="schedule-control__tools">
          <el-select
            v-model="filters.studentId"
            filterable
            clearable
            placeholder="按学生筛选"
            class="toolbar-select"
          >
            <el-option
              v-for="student in students"
              :key="student.id"
              :label="`${student.name} · ${student.grade}`"
              :value="student.id"
            />
          </el-select>

          <div class="schedule-control__nav">
            <el-button @click="shiftWeek(-1)">上一周</el-button>
            <el-button @click="jumpToCurrentWeek">回到本周</el-button>
            <el-button @click="shiftWeek(1)">下一周</el-button>
          </div>
        </div>
      </div>

      <div class="schedule-legend">
        <span><i class="schedule-legend__dot schedule-legend__dot--planned"></i>待上课</span>
        <span><i class="schedule-legend__dot schedule-legend__dot--completed"></i>已销课</span>
      </div>
    </el-card>

    <div v-if="loading" class="page-state">课表加载中…</div>
    <el-alert v-else-if="error" :title="error" type="error" show-icon :closable="false" />
    <div v-else class="week-board">
      <el-card v-for="day in weekDays" :key="day.dateKey" shadow="never" class="day-card">
        <template #header>
          <div class="day-card__header">
            <div>
              <span class="day-card__weekday">{{ day.weekday }}</span>
              <strong>{{ day.dateLabel }}</strong>
            </div>
            <span class="day-card__count">{{ day.items.length }} 节</span>
          </div>
        </template>

        <div v-if="day.items.length" class="schedule-list">
          <article
            v-for="slot in day.items"
            :key="slot.id"
            class="schedule-item"
            :data-completed="slot.isCompleted"
          >
            <div class="schedule-item__meta">
              <span class="schedule-item__time">{{ slot.timeRange }}</span>
              <el-tag size="small" :type="slot.tagType" effect="plain">{{ slot.statusLabel }}</el-tag>
            </div>
            <strong>{{ slot.studentName }}</strong>
            <p>{{ slot.subject }}</p>
            <div class="schedule-item__footer">
              <span>{{ slot.isCompleted ? '已完成销课' : '待课前确认' }}</span>
              <el-button
                link
                type="primary"
                :loading="processingId === slot.scheduleId"
                @click="toggleScheduleStatus(slot)"
              >
                {{ slot.isCompleted ? '撤销销课' : '销课' }}
              </el-button>
            </div>
          </article>
        </div>
        <div v-else class="day-card__empty">
          <span>本日空闲</span>
          <small>暂无排课安排</small>
        </div>
      </el-card>
    </div>

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
  formatDate,
  formatDateParam,
  formatParsedIntentSummary,
  formatTimeRange,
  formatWeekRange,
  formatWeekday,
  getWeekDays,
  getWeekStart,
  normalizeError
} from '../utils/format';

const route = useRoute();

const loading = ref(false);
const error = ref('');
const creating = ref(false);
const processingId = ref(null);
const students = ref([]);
const schedules = ref([]);
const filters = reactive({
  studentId: route.query.studentId ? Number(route.query.studentId) : null
});
const currentWeekStart = ref(getWeekStart(new Date()));
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

const loadPageData = async () => {
  loading.value = true;
  error.value = '';
  try {
    const [studentsData, schedulesData] = await Promise.all([
      api.listStudents(),
      api.listWeekSchedules(formatDateParam(currentWeekStart.value))
    ]);
    students.value = studentsData ?? [];
    schedules.value = schedulesData ?? [];
  } catch (requestError) {
    error.value = normalizeError(requestError, '课表加载失败');
  } finally {
    loading.value = false;
  }
};

const loadSchedules = async () => {
  loading.value = true;
  error.value = '';
  try {
    schedules.value = await api.listWeekSchedules(formatDateParam(currentWeekStart.value));
  } catch (requestError) {
    error.value = normalizeError(requestError, '课表加载失败');
  } finally {
    loading.value = false;
  }
};

const filteredSchedules = computed(() => {
  return schedules.value
    .filter((schedule) => !filters.studentId || schedule.studentId === filters.studentId)
    .slice()
    .sort((left, right) => new Date(left.startTime) - new Date(right.startTime));
});

const normalizedSchedules = computed(() => {
  return filteredSchedules.value.map((schedule) => {
    const isCompleted = String(schedule.status ?? '').toUpperCase() === 'COMPLETED';
    return {
      ...schedule,
      scheduleId: schedule.id,
      studentName: schedule.studentName ?? '未命名',
      subject: schedule.subject ?? '正式课',
      timeRange: formatTimeRange(schedule.startTime, schedule.endTime),
      isCompleted,
      statusLabel: isCompleted ? '已销课' : '待上课',
      tagType: isCompleted ? 'success' : 'warning'
    };
  });
});

const currentWeekLabel = computed(() => formatWeekRange(currentWeekStart.value));

const weekDays = computed(() => {
  return getWeekDays(currentWeekStart.value).map((day) => {
    const dateKey = formatDateParam(day);
    return {
      dateKey,
      weekday: formatWeekday(day),
      dateLabel: formatDate(day),
      items: normalizedSchedules.value.filter((schedule) => formatDateParam(schedule.startTime) === dateKey)
    };
  });
});

const completedCount = computed(() => normalizedSchedules.value.filter((item) => item.isCompleted).length);
const pendingCount = computed(() => normalizedSchedules.value.filter((item) => !item.isCompleted).length);
const weeklyStudentCount = computed(() => new Set(filteredSchedules.value.map((item) => item.studentId)).size);

const overviewStats = computed(() => [
  {
    label: '本周课程数',
    value: `${filteredSchedules.value.length}`,
    helper: '当前周视图内的全部课程'
  },
  {
    label: '待上课',
    value: `${pendingCount.value}`,
    helper: '还未销课的课程数量'
  },
  {
    label: '已销课',
    value: `${completedCount.value}`,
    helper: '本周已经确认完成'
  },
  {
    label: '涉及学生',
    value: `${weeklyStudentCount.value}`,
    helper: '本周参与排课的学生数'
  }
]);

const filterSummary = computed(() => {
  const student = students.value.find((item) => item.id === filters.studentId);
  if (student) {
    return `当前筛选：${student.name} · ${student.grade}，共 ${filteredSchedules.value.length} 节课。`;
  }
  return `当前查看本周全部课表，共 ${filteredSchedules.value.length} 节课，${weeklyStudentCount.value} 位学生参与排课。`;
});

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
    const firstSchedule = Array.isArray(generated) ? generated[0] : null;
    if (firstSchedule?.startTime) {
      currentWeekStart.value = getWeekStart(firstSchedule.startTime);
    }
    createDialogVisible.value = false;
    ElMessage.success(`已生成 ${Array.isArray(generated) ? generated.length : 0} 节课`);
    await loadSchedules();
  } catch (requestError) {
    ElMessage.error(normalizeError(requestError, '排课失败'));
  } finally {
    creating.value = false;
  }
};

const toggleScheduleStatus = async (schedule) => {
  if (!schedule.scheduleId) {
    return;
  }
  processingId.value = schedule.scheduleId;
  try {
    if (schedule.isCompleted) {
      await api.undoCompleteSchedule(schedule.scheduleId);
      ElMessage.success('已撤销销课');
    } else {
      await api.completeSchedule(schedule.scheduleId);
      ElMessage.success('销课完成');
    }
    await loadSchedules();
  } catch (requestError) {
    ElMessage.error(normalizeError(requestError, '更新课程状态失败'));
  } finally {
    processingId.value = null;
  }
};

const shiftWeek = async (offset) => {
  const nextWeek = new Date(currentWeekStart.value);
  nextWeek.setDate(nextWeek.getDate() + offset * 7);
  currentWeekStart.value = getWeekStart(nextWeek);
  await loadSchedules();
};

const jumpToCurrentWeek = async () => {
  currentWeekStart.value = getWeekStart(new Date());
  await loadSchedules();
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
      const firstSchedule = Array.isArray(data.generatedSchedules) ? data.generatedSchedules[0] : null;
      if (firstSchedule?.startTime) {
        currentWeekStart.value = getWeekStart(firstSchedule.startTime);
      }
      await loadSchedules();
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
    filters.studentId = studentId ? Number(studentId) : null;
  },
  { immediate: true }
);

onMounted(async () => {
  await loadPageData();
  await resetAssistantConversation();
});
</script>

<style scoped>
.page-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.planner-hero,
.schedule-control-card {
  background: var(--app-surface);
}

.planner-hero__body {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(320px, 0.95fr);
  gap: 20px;
}

.planner-hero__intro {
  padding: 4px 0;
}

.planner-hero__eyebrow {
  display: inline-flex;
  margin-bottom: 10px;
  color: var(--app-text-tertiary);
  font-size: 12px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.planner-hero__intro h3 {
  margin-bottom: 10px;
  font-size: 28px;
  line-height: 1.15;
}

.planner-hero__intro p {
  max-width: 620px;
  color: var(--app-text-secondary);
  line-height: 1.7;
}

.planner-hero__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;
}

.planner-chip {
  display: inline-flex;
  align-items: center;
  padding: 9px 12px;
  border: 1px solid var(--app-border);
  border-radius: 999px;
  background: var(--app-primary-soft);
  color: var(--app-text-secondary);
  font-size: 13px;
}

.planner-hero__stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.planner-stat {
  padding: 16px;
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-sm);
  background: var(--app-surface-muted);
}

.planner-stat span {
  display: block;
  margin-bottom: 10px;
  color: var(--app-text-secondary);
  font-size: 13px;
}

.planner-stat strong {
  display: block;
  margin-bottom: 6px;
  font-size: 28px;
  line-height: 1;
}

.planner-stat small {
  color: var(--app-text-tertiary);
  font-size: 12px;
  line-height: 1.6;
}

.schedule-control {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.schedule-control__summary {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.schedule-control__eyebrow {
  color: var(--app-text-tertiary);
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.schedule-control__summary strong {
  font-size: 22px;
  line-height: 1.1;
}

.schedule-control__summary p {
  color: var(--app-text-secondary);
  font-size: 14px;
  line-height: 1.7;
}

.schedule-control__tools {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 12px;
}

.schedule-control__nav {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.toolbar-select {
  width: 220px;
}

.schedule-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--app-border);
  color: var(--app-text-secondary);
  font-size: 13px;
}

.schedule-legend span {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.schedule-legend__dot {
  width: 10px;
  height: 10px;
  border-radius: 999px;
}

.schedule-legend__dot--planned {
  background: #f59e0b;
}

.schedule-legend__dot--completed {
  background: #22c55e;
}

.week-board {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 12px;
}

.day-card {
  min-height: 320px;
  border: 1px solid var(--app-border);
  background: var(--app-surface);
}

.day-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.day-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.day-card__weekday {
  display: block;
  margin-bottom: 4px;
  color: var(--app-text-tertiary);
  font-size: 12px;
}

.day-card__count {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.06);
  color: var(--app-text-secondary);
  font-size: 12px;
}

.schedule-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.schedule-item {
  position: relative;
  padding: 12px;
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-sm);
  background: var(--app-surface-muted);
}

.schedule-item::before {
  content: '';
  position: absolute;
  top: 14px;
  left: 0;
  bottom: 14px;
  width: 3px;
  border-radius: 999px;
  background: #f59e0b;
}

.schedule-item[data-completed='true'] {
  background: rgba(34, 197, 94, 0.05);
  border-color: rgba(34, 197, 94, 0.18);
}

.schedule-item[data-completed='true']::before {
  background: #22c55e;
}

.schedule-item__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 10px;
}

.schedule-item__time {
  color: var(--app-text-secondary);
  font-size: 13px;
}

.schedule-item strong {
  display: block;
  margin-bottom: 4px;
  font-size: 15px;
}

.schedule-item p {
  color: var(--app-text-secondary);
  font-size: 13px;
}

.schedule-item__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 12px;
  color: var(--app-text-tertiary);
  font-size: 12px;
}

.day-card__empty {
  display: flex;
  flex: 1;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 1px dashed var(--app-border);
  border-radius: var(--app-radius-sm);
  background: var(--app-surface-muted);
  color: var(--app-text-tertiary);
  text-align: center;
}

.day-card__empty span {
  margin-bottom: 4px;
  color: var(--app-text-secondary);
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

@media (max-width: 1440px) {
  .planner-hero__body {
    grid-template-columns: 1fr;
  }

  .week-board {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 1200px) {
  .schedule-control,
  .assistant-actions {
    flex-direction: column;
    align-items: flex-start;
  }

  .schedule-control__tools {
    width: 100%;
    justify-content: flex-start;
  }

  .week-board {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .dialog-grid,
  .week-board,
  .planner-hero__stats,
  .weekday-chip-grid {
    grid-template-columns: 1fr;
  }

  .toolbar-select {
    width: 100%;
  }

  .schedule-item__footer,
  .assistant-actions,
  .schedule-control__nav {
    width: 100%;
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
