<template>
  <div class="app-shell">
    <header class="top-bar">
      <div class="top-bar__content">
        <div class="brand-head">
          <div class="logo-mark">
            <img :src="logo" alt="青青课堂 logo" />
          </div>
          <div>
            <p class="eyebrow">Qingqing Ketang Dashboard</p>
            <h1>青青课堂运营后台</h1>
          </div>
        </div>
        <div class="hero-stats">
          <article class="hero-stat">
            <span class="hero-stat__label">今日事项</span>
            <strong>{{ actionableTodayCount }}</strong>
            <small>{{ todayProgressLabel }}</small>
          </article>
          <article class="hero-stat">
            <span class="hero-stat__label">学生档案</span>
            <strong>{{ students.length }}</strong>
            <small>当前已录入学生</small>
          </article>
          <article class="hero-stat">
            <span class="hero-stat__label">查看周排课</span>
            <strong>{{ weekScheduleRows.length }}</strong>
            <small>周课表条目数</small>
          </article>
        </div>
      </div>
    </header>

    <main class="dashboard-shell">
      <section class="overview-grid">
        <article class="panel today-card">
          <div class="panel__header today-card__header">
            <div class="today-card__title">
              <span class="panel-kicker">Today</span>
              <h2>今日安排</h2>
            </div>
            <div class="today-card__meta">
              <span class="today-progress">{{ todayProgressLabel }}</span>
              <span class="today-date">{{ todayLabel }}</span>
            </div>
          </div>
          <div v-if="todayPlanLoading" class="panel__helper">今日安排加载中…</div>
          <p v-else-if="todayPlanError" class="panel__helper error">{{ todayPlanError }}</p>
          <ul v-else class="today-list">
            <li
              v-for="item in todayPlan"
              :key="item.id"
              class="today-item"
              :data-completed="item.isCompleted"
              :data-placeholder="item.type === 'placeholder'"
              :data-loading="item.isLoading"
            >
              <button
                class="today-toggle"
                type="button"
                :aria-pressed="item.isCompleted"
                :aria-label="item.actionLabel"
                :title="item.actionLabel"
                :data-loading="item.isLoading"
                :disabled="!item.isInteractive || item.isLoading"
                @click="handleTodayItemClick(item)"
              >
                <span v-if="item.isLoading" class="today-toggle__spinner"></span>
                <span v-else class="today-toggle__mark"></span>
              </button>
              <div class="today-node">
                <div class="today-node__meta">
                  <p class="today-time">{{ item.time }}</p>
                  <span class="today-kind">{{ item.category }}</span>
                </div>
                <p class="today-title">{{ item.title }}</p>
                <small>{{ item.detail }}</small>
              </div>
            </li>
          </ul>
          <p v-if="todayActionError" class="panel__helper error">{{ todayActionError }}</p>
          <p v-else-if="todayActionMessage" class="panel__helper success">{{ todayActionMessage }}</p>
        </article>

        <article class="panel tuition summary-panel">
          <div class="panel__header">
            <div>
              <span class="panel-kicker">Finance</span>
              <h2>学费管理</h2>
            </div>
          </div>
          <p v-if="tuitionError && !hasTuitionOverview" class="panel__helper error">{{ tuitionError }}</p>
          <p v-else-if="tuitionLoading && !hasTuitionOverview" class="panel__helper">学费数据加载中…</p>
          <div v-else class="tuition-stats" :data-animating="tuitionHighlighting">
            <div class="tuition-stat">
              <p>已收总学费</p>
              <strong>{{ formatAmount(displayedTuitionOverview.totalReceived) }}</strong>
            </div>
            <div class="tuition-stat">
              <p>已销课学费</p>
              <strong>{{ formatAmount(displayedTuitionOverview.totalConsumed) }}</strong>
            </div>
            <div class="tuition-stat">
              <p>待销课学费</p>
              <strong>{{ formatAmount(displayedTuitionOverview.totalPending) }}</strong>
            </div>
          </div>
          <p v-if="tuitionLoading && hasTuitionOverview" class="panel__helper tuition-sync">学费数据同步中…</p>
          <p v-else-if="tuitionError && hasTuitionOverview" class="panel__helper error tuition-sync">{{ tuitionError }}</p>
        </article>
      </section>

      <section class="operations-grid">
        <article class="panel schedule schedule-panel">
          <div class="panel__header schedule-header">
            <div>
              <span class="panel-kicker">Schedule</span>
              <h2>课程管理</h2>
            </div>
            <div class="schedule-actions">
              <button
                v-for="tab in scheduleTabs"
                :key="tab.id"
                type="button"
                class="tab-switch"
                :data-active="activeScheduleTab === tab.id"
                @click="activeScheduleTab = tab.id"
              >
                {{ tab.label }}
              </button>
            </div>
          </div>

          <section v-if="activeScheduleTab === 'arrange'" class="schedule-arrange">
            <p class="panel__helper">选择每周上课次数、上课日和开始日期后，系统会按剩余课时自动往后排满</p>
            <form class="student-form arrange-form" @submit.prevent="submitArrangeDraft">
              <div class="form-block arrange-frequency">
                <div class="form-block__head">
                  <span>每周上课次数</span>
                </div>
                <div class="frequency-switches">
                  <button
                    v-for="option in frequencyOptions"
                    :key="option.value"
                    type="button"
                    class="frequency-chip"
                    :data-active="arrangeDraft.weeklySessions === option.value"
                    @click="setArrangeFrequency(option.value)"
                  >
                    {{ option.label }}
                  </button>
                </div>
              </div>

              <div class="form-block weekday-picker">
                <div class="form-block__head">
                  <span>每周上课日</span>
                  <small>{{ arrangeWeekdaySummary }}</small>
                </div>
                <div class="weekday-grid">
                  <button
                    v-for="option in weekdayOptions"
                    :key="`arrange-${option.value}`"
                    type="button"
                    class="weekday-chip"
                    :data-active="arrangeDraft.weekdays.includes(option.value)"
                    @click="toggleArrangeWeekday(option.value)"
                  >
                    {{ option.label }}
                  </button>
                </div>
              </div>

              <div class="form-row">
                <label class="form-field form-field--quarter">
                  <span>学生</span>
                  <select v-model="arrangeDraft.studentId" required>
                    <option value="">请选择学生</option>
                    <option v-for="student in students" :key="student.id" :value="student.id">
                      {{ student.name }} · {{ student.grade }}
                    </option>
                  </select>
                </label>
                <label class="form-field form-field--quarter">
                  <span>开始日期</span>
                  <input v-model="arrangeDraft.startDate" type="date" required />
                </label>
                <label class="form-field form-field--quarter">
                  <span>开始时间</span>
                  <input v-model="arrangeDraft.startTime" type="time" required />
                </label>
                <label class="form-field form-field--quarter">
                  <span>结束时间</span>
                  <input v-model="arrangeDraft.endTime" type="time" required />
                </label>
              </div>
              <div class="form-actions">
                <p class="form-note">排课草案仅供内部确认，稍后可导出至正式课表</p>
                <button class="ghost" type="button" @click="resetArrangeDraft()" :disabled="arrangeSubmitting">清空</button>
                <button class="primary" type="submit" :disabled="arrangeSubmitting">
                  {{ arrangeSubmitting ? '提交中...' : '确定排课' }}
                </button>
              </div>
              <p v-if="arrangeError" class="form-error">{{ arrangeError }}</p>
              <p v-if="arrangeFeedback" class="form-success">{{ arrangeFeedback }}</p>
            </form>
          </section>

          <section v-else class="schedule-week">
            <div class="schedule-week__toolbar">
              <div class="schedule-week__range">
                <span>当前查看</span>
                <strong>{{ currentWeekLabel }}</strong>
              </div>
              <div class="schedule-week__nav">
                <button class="ghost" type="button" :disabled="scheduleLoading" @click="shiftWeek(-1)">上一周</button>
                <button class="ghost" type="button" :disabled="scheduleLoading || isViewingCurrentWeek" @click="jumpToCurrentWeek">
                  回到本周
                </button>
                <button class="ghost" type="button" :disabled="scheduleLoading" @click="shiftWeek(1)">下一周</button>
              </div>
            </div>
            <p v-if="scheduleLoading" class="panel__helper">课表加载中…</p>
            <p v-else-if="scheduleError" class="panel__helper error">{{ scheduleError }}</p>
            <div v-else-if="weekScheduleRows.length" class="table-scroller">
              <table class="schedule-table">
                <thead>
                  <tr>
                    <th>日期</th>
                    <th>学科</th>
                    <th>时间段</th>
                    <th>学生</th>
                    <th>状态</th>
                    <th>操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="slot in weekScheduleRows" :key="slot.id">
                    <td>{{ slot.weekday }}</td>
                    <td>{{ slot.subject }}</td>
                    <td>{{ slot.timeRange }}</td>
                    <td>{{ slot.student }}</td>
                    <td>
                      <span class="schedule-status" :data-status="slot.status">
                        {{ slot.statusLabel }}
                      </span>
                    </td>
                    <td>
                      <button
                        v-if="slot.canToggle"
                        type="button"
                        class="complete-btn"
                        :disabled="!slot.scheduleId || isCompleting(slot.scheduleId)"
                        @click="toggleScheduleCompletion(slot.scheduleId, slot.isCompleted, { source: 'schedule' })"
                      >
                        {{ slot.scheduleId && isCompleting(slot.scheduleId) ? '处理中...' : slot.actionLabel }}
                      </button>
                      <span v-else class="complete-placeholder">不可操作</span>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
            <p v-else class="panel__helper">当前周暂无排课</p>
            <template v-if="!scheduleLoading && !scheduleError">
              <p v-if="scheduleActionError" class="panel__helper error">{{ scheduleActionError }}</p>
              <p v-else-if="scheduleActionMessage" class="panel__helper success">{{ scheduleActionMessage }}</p>
            </template>
          </section>
        </article>

        <div class="operations-stack">
          <article class="panel renew">
            <div class="panel__header">
              <div>
                <span class="panel-kicker">Renewal</span>
                <h2>续费管理</h2>
              </div>
            </div>
            <form class="student-form" @submit.prevent="submitRenew">
              <div class="form-row">
                <label class="form-field">
                  <span>学生</span>
                  <select v-model="renewForm.studentId" required>
                    <option disabled value="">请选择学生</option>
                    <option v-for="student in students" :key="student.id" :value="student.id">
                      {{ student.name }} · {{ student.grade }}
                    </option>
                  </select>
                </label>
                <label class="form-field">
                  <span>本次缴费金额（元）</span>
                  <input
                    v-model="renewForm.tuitionPaid"
                    type="number"
                    min="0"
                    step="1"
                    placeholder="12000"
                    required
                  />
                </label>
                <label class="form-field">
                  <span>本次课时（节）</span>
                  <input
                    v-model="renewForm.lessonCount"
                    type="number"
                    min="1"
                    step="1"
                    placeholder="40"
                    required
                  />
                </label>
              </div>
              <div class="form-actions">
                <p class="form-note">续费成功会自动更新学生列表与学费概览</p>
                <button class="ghost" type="button" @click="resetRenewForm" :disabled="renewSubmitting">清空</button>
                <button class="primary" type="submit" :disabled="!isRenewValid || renewSubmitting">
                  {{ renewSubmitting ? '提交中...' : '提交续费' }}
                </button>
              </div>
              <p v-if="renewError" class="form-error">{{ renewError }}</p>
              <p v-else-if="renewSuccess" class="form-success">{{ renewSuccess }}</p>
            </form>
          </article>

          <article class="panel trial">
            <div class="panel__header">
              <div>
                <span class="panel-kicker">Trial</span>
                <h2>试听/测评管理</h2>
              </div>
            </div>
            <form class="student-form" @submit.prevent="submitTrial">
              <div class="form-row">
                <label class="form-field">
                  <span>姓名</span>
                  <input v-model.trim="trialForm.name" type="text" placeholder="如：王同学" required />
                </label>
                <label class="form-field">
                  <span>年级</span>
                  <select v-model="trialForm.grade">
                    <option v-for="option in grades" :key="`trial-${option}`" :value="option">
                      {{ option }}
                    </option>
                  </select>
                </label>
                <label class="form-field">
                  <span>试听时间</span>
                  <input v-model="trialForm.trialTime" type="datetime-local" required />
                </label>
              </div>
              <div class="form-actions">
                <p class="form-note">新建试听后会自动追加到下方列表</p>
                <button class="ghost" type="button" @click="resetTrialForm" :disabled="trialSubmitting">清空</button>
                <button class="primary" type="submit" :disabled="trialSubmitting">
                  {{ trialSubmitting ? '提交中...' : '录入' }}
                </button>
              </div>
              <p v-if="trialError" class="form-error">{{ trialError }}</p>
              <p v-else-if="trialSuccess" class="form-success">{{ trialSuccess }}</p>
            </form>
            <p v-if="trialLoading" class="panel__helper">试听数据加载中…</p>
            <p v-else-if="trialListError" class="panel__helper error">{{ trialListError }}</p>
            <ul v-else-if="trialLeads.length" class="trial-list">
              <li v-for="lead in trialLeads" :key="lead.id">
                <div>
                  <p class="trial-title">{{ lead.name }} · {{ lead.grade }}</p>
                </div>
                <span class="pill-btn normal">{{ formatTimestamp(lead.trialTime) }}</span>
              </li>
            </ul>
            <p v-else class="empty-state">暂无试听记录，填写上方表单即可添加。</p>
          </article>
        </div>
      </section>

      <section class="records-grid">
        <article class="panel students">
          <div class="panel__header">
            <div>
              <span class="panel-kicker">Students</span>
              <h2>学生管理</h2>
            </div>
            <button class="ghost" type="button" @click="toggleForm">
              {{ showForm ? '收起表单' : '录入学生' }}
            </button>
          </div>

          <form
            v-if="showForm"
            class="student-form"
            @submit.prevent="submitStudent"
          >
            <div class="form-row">
              <label class="form-field">
                <span>姓名</span>
                <input v-model.trim="form.name" type="text" placeholder="如：李晓明" required />
              </label>
              <label class="form-field">
                <span>性别</span>
                <select v-model="form.gender">
                  <option v-for="option in genders" :key="option" :value="option">{{ option }}</option>
                </select>
              </label>
              <label class="form-field">
                <span>年级</span>
                <select v-model="form.grade">
                  <option v-for="option in grades" :key="option" :value="option">{{ option }}</option>
                </select>
              </label>
            </div>
            <div class="form-row">
              <label class="form-field">
                <span>学费（元）</span>
                <input
                  v-model="form.tuitionPaid"
                  type="number"
                  min="0"
                  step="1"
                  placeholder="12000"
                  required
                />
              </label>
              <label class="form-field">
                <span>课时（节）</span>
                <input
                  v-model="form.lessonCount"
                  type="number"
                  min="0"
                  step="1"
                  placeholder="40"
                  required
                />
              </label>
            </div>
            <div class="form-actions">
              <p class="form-note">录入信息将实时同步到下方列表</p>
              <button class="ghost" type="button" @click="resetForm" :disabled="submitting">清空</button>
              <button class="primary" type="submit" :disabled="submitting || !isFormValid">
                {{ submitting ? '录入中...' : '录入学生' }}
              </button>
            </div>
            <p v-if="formError" class="form-error">{{ formError }}</p>
            <p v-else-if="formSuccess" class="form-success">{{ formSuccess }}</p>
          </form>

          <div v-if="loadingStudents" class="panel__helper">正在加载学生信息…</div>
          <p v-else-if="studentLoadError" class="panel__helper error">{{ studentLoadError }}</p>
          <div v-else-if="students.length" class="table-scroller students-table-wrap">
            <table>
              <thead>
                <tr>
                  <th>姓名</th>
                  <th>性别</th>
                  <th>年级</th>
                  <th>录入时间</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="student in students" :key="student.id">
                  <td>{{ student.name }}</td>
                  <td>{{ student.gender }}</td>
                  <td>{{ student.grade }}</td>
                  <td>{{ formatTimestamp(student.createdAt) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <p v-else class="empty-state">暂未录入学生，填写上方表单即可创建首位学生。</p>
        </article>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import logo from '../img/logo.png';

const API_BASE = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080';
const genders = ['女', '男'];
const grades = [
  '幼儿园大班',
  '一年级',
  '二年级',
  '三年级',
  '四年级',
  '五年级',
  '六年级',
  '七年级（初一）',
  '八年级（初二）',
  '九年级（初三）'
];

const students = ref([]);
const loadingStudents = ref(false);
const studentLoadError = ref('');
const showForm = ref(false);
const tuitionOverview = ref({
  totalReceived: null,
  totalConsumed: null,
  totalPending: null
});
const displayedTuitionOverview = ref({
  totalReceived: null,
  totalConsumed: null,
  totalPending: null
});
const tuitionLoading = ref(false);
const tuitionError = ref('');
const tuitionHighlighting = ref(false);
const tuitionMetricKeys = ['totalReceived', 'totalConsumed', 'totalPending'];
const renewForm = reactive({
  studentId: '',
  tuitionPaid: '',
  lessonCount: ''
});
const renewSubmitting = ref(false);
const renewError = ref('');
const renewSuccess = ref('');
const scheduleTabs = [
  { id: 'arrange', label: '排课' },
  { id: 'week', label: '查看周课表' }
];
const activeScheduleTab = ref('arrange');
const weekdayOptions = [
  { value: 1, label: '周一' },
  { value: 2, label: '周二' },
  { value: 3, label: '周三' },
  { value: 4, label: '周四' },
  { value: 5, label: '周五' },
  { value: 6, label: '周六' },
  { value: 7, label: '周日' }
];
const frequencyOptions = [
  { value: 1, label: '每周一次' },
  { value: 2, label: '每周两次' },
  { value: 3, label: '每周三次' },
  { value: 4, label: '每周四次' },
  { value: 5, label: '每周五次' },
  { value: 6, label: '每周六次' },
  { value: 7, label: '每天上课' }
];
const arrangeDraft = reactive({
  studentId: '',
  weeklySessions: 1,
  weekdays: [weekdayOptions[0].value],
  startDate: '',
  startTime: '',
  endTime: ''
});
const arrangeSubmitting = ref(false);
const arrangeFeedback = ref('');
const arrangeError = ref('');
const currentWeekStart = ref(getWeekStart(new Date()));
const todayWeekStart = getWeekStart(new Date());
const scheduleLoading = ref(false);
const scheduleError = ref('');
const weekSchedule = ref([]);
const todayWeekSchedule = ref([]);
const todayScheduleLoading = ref(false);
const todayScheduleError = ref('');
const scheduleActionMessage = ref('');
const scheduleActionError = ref('');
const todayActionMessage = ref('');
const todayActionError = ref('');
const completingScheduleIds = ref(new Set());
const todayLabel = new Intl.DateTimeFormat('zh-CN', {
  month: 'long',
  day: 'numeric',
  weekday: 'long'
}).format(new Date());
const trialLeads = ref([]);
const trialLoading = ref(false);
const trialListError = ref('');
const trialError = ref('');
const trialForm = reactive({
  name: '',
  grade: grades[0],
  trialTime: ''
});
const trialSubmitting = ref(false);
const trialSuccess = ref('');
const todayCompletionIds = ref(new Set());
const feedbackTimeoutIds = {
  today: 0,
  schedule: 0
};
let tuitionAnimationFrameId = 0;
let tuitionHighlightTimeoutId = 0;

const todayPlanLoading = computed(() => trialLoading.value || todayScheduleLoading.value);
const todayPlanError = computed(() => {
  if (trialListError.value) return trialListError.value;
  if (todayScheduleError.value) return todayScheduleError.value;
  return '';
});
const todayPlan = computed(() => {
  if (todayPlanError.value) {
    return [];
  }
  const todayKey = formatDateParam(new Date());
  const events = [];

  trialLeads.value
    .filter((lead) => isSameDay(lead.trialTime, todayKey))
    .forEach((lead) => {
      const timestamp = toTimestamp(lead.trialTime);
      const itemId = `trial-${lead.id ?? timestamp}`;
      events.push({
        id: itemId,
        type: 'trial',
        time: formatClock(lead.trialTime),
        title: `${lead.name ?? '未命名'} · ${lead.grade ?? '未填写年级'} 试听`,
        category: '试听',
        detail: `试听预约 · ${lead.grade ?? '未填写年级'}`,
        toggleable: true,
        isInteractive: true,
        isCompleted: todayCompletionIds.value.has(itemId),
        isLoading: false,
        actionLabel: todayCompletionIds.value.has(itemId) ? '取消试听完成' : '标记试听完成',
        sortKey: timestamp ?? Number.MAX_SAFE_INTEGER
      });
    });

  todayWeekSchedule.value
    .filter((slot) => isSameDay(slot.startTime, todayKey))
    .forEach((slot) => {
      const timestamp = toTimestamp(slot.startTime);
      const scheduleId = slot.id ?? null;
      const status = String(slot.status ?? 'PLANNED').toUpperCase();
      const isCompleted = status === 'COMPLETED';
      const isLoading = scheduleId ? isCompleting(scheduleId) : false;
      events.push({
        id: `schedule-${scheduleId ?? timestamp}`,
        type: 'schedule',
        scheduleId,
        time: formatTimeRange(slot.startTime, slot.endTime),
        title: `${slot.studentName ?? '未命名'} · ${slot.subject ?? '课程'}`,
        category: '正式课',
        detail: `正式课程 · ${formatWeekday(slot.startTime)} · ${isCompleted ? '已销课' : '待销课'}`,
        toggleable: true,
        isInteractive: Boolean(scheduleId),
        isCompleted,
        isLoading,
        actionLabel: isCompleted ? '撤销销课' : '执行销课',
        sortKey: timestamp ?? Number.MAX_SAFE_INTEGER
      });
    });

  events.sort((a, b) => (a.sortKey ?? 0) - (b.sortKey ?? 0));
  if (!events.length) {
    return [
      {
        id: 'empty',
        type: 'placeholder',
        time: '今日',
        title: '今日暂无试听或排课安排',
        category: '空白',
        detail: '当前日程空闲，可在下方模块继续录入安排',
        toggleable: false,
        isInteractive: false,
        isCompleted: false,
        isLoading: false,
        actionLabel: '暂无可操作事项'
      }
    ];
  }
  return events.map(({ sortKey, ...rest }) => rest);
});

const actionableTodayCount = computed(() => {
  return todayPlan.value.filter((item) => item.toggleable).length;
});

const arrangeWeekdaySummary = computed(() => {
  const labels = weekdayOptions
    .filter((option) => arrangeDraft.weekdays.includes(option.value))
    .map((option) => option.label);
  if (!labels.length) {
    return `已选 0/${arrangeDraft.weeklySessions}`;
  }
  return `已选 ${labels.length}/${arrangeDraft.weeklySessions} · ${labels.join('、')}`;
});

const todayProgressLabel = computed(() => {
  if (!actionableTodayCount.value) {
    return '今天已清空';
  }
  const completedCount = todayPlan.value.filter((item) => item.toggleable && item.isCompleted).length;
  return `${completedCount}/${actionableTodayCount.value} 完成`;
});

const hasTuitionOverview = computed(() => {
  return tuitionMetricKeys.some((key) => displayedTuitionOverview.value[key] !== null);
});

const isRenewValid = computed(() => {
  return (
    renewForm.studentId &&
    renewForm.tuitionPaid !== '' &&
    renewForm.lessonCount !== ''
  );
});

const form = reactive({
  name: '',
  gender: genders[0],
  grade: grades[0],
  tuitionPaid: '',
  lessonCount: ''
});

const submitting = ref(false);
const formError = ref('');
const formSuccess = ref('');

const isFormValid = computed(() => {
  return (
    form.name.trim() &&
    form.grade &&
    form.tuitionPaid !== '' &&
    form.lessonCount !== ''
  );
});

const formatCurrency = (value, fractionDigits = 0) => {
  const amount = Number(value ?? 0);
  if (Number.isNaN(amount)) {
    return '0';
  }
  const normalized = Math.round(amount * 100) / 100;
  const digits = fractionDigits > 0 ? fractionDigits : (Number.isInteger(normalized) ? 0 : 2);
  return normalized.toLocaleString('zh-CN', {
    minimumFractionDigits: digits,
    maximumFractionDigits: Math.max(2, digits)
  });
};

const formatTimestamp = (value) => {
  if (!value) return '--';
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value));
};

const formatAmount = (value) => {
  if (value === null || value === undefined) {
    return '--';
  }
  const amount = Number(value);
  if (Number.isNaN(amount)) {
    return '--';
  }
  return `${formatCurrency(amount)} 元`;
};

const toAmountNumber = (value) => {
  const amount = Number(value ?? 0);
  return Number.isNaN(amount) ? 0 : amount;
};

const stopTuitionAnimation = () => {
  if (tuitionAnimationFrameId) {
    window.cancelAnimationFrame(tuitionAnimationFrameId);
    tuitionAnimationFrameId = 0;
  }
};

const pulseTuitionStats = (duration = 1200) => {
  tuitionHighlighting.value = true;
  if (tuitionHighlightTimeoutId) {
    window.clearTimeout(tuitionHighlightTimeoutId);
  }
  tuitionHighlightTimeoutId = window.setTimeout(() => {
    tuitionHighlighting.value = false;
    tuitionHighlightTimeoutId = 0;
  }, duration);
};

const clearFeedbackTimer = (scope) => {
  if (feedbackTimeoutIds[scope]) {
    window.clearTimeout(feedbackTimeoutIds[scope]);
    feedbackTimeoutIds[scope] = 0;
  }
};

const queueActionMessage = (scope, message) => {
  const target = scope === 'today' ? todayActionMessage : scheduleActionMessage;
  clearFeedbackTimer(scope);
  target.value = message;
  feedbackTimeoutIds[scope] = window.setTimeout(() => {
    target.value = '';
    feedbackTimeoutIds[scope] = 0;
  }, 2000);
};

const clearActionFeedback = () => {
  clearFeedbackTimer('today');
  clearFeedbackTimer('schedule');
  todayActionMessage.value = '';
  todayActionError.value = '';
  scheduleActionMessage.value = '';
  scheduleActionError.value = '';
};

const applyTuitionOverview = (overview) => {
  const next = {
    totalReceived: toAmountNumber(overview?.totalReceived),
    totalConsumed: toAmountNumber(overview?.totalConsumed),
    totalPending: toAmountNumber(overview?.totalPending)
  };
  const start = tuitionMetricKeys.reduce((result, key) => {
    const currentValue = displayedTuitionOverview.value[key];
    result[key] = currentValue === null ? 0 : toAmountNumber(currentValue);
    return result;
  }, {});
  const precisionMap = tuitionMetricKeys.reduce((result, key) => {
    result[key] = Number.isInteger(start[key]) && Number.isInteger(next[key]) ? 0 : 2;
    return result;
  }, {});
  const hasChanged = tuitionMetricKeys.some((key) => Math.abs(start[key] - next[key]) >= 0.01);

  tuitionOverview.value = next;

  if (!hasChanged) {
    stopTuitionAnimation();
    displayedTuitionOverview.value = next;
    tuitionHighlighting.value = false;
    return;
  }

  stopTuitionAnimation();
  pulseTuitionStats();

  const duration = 900;
  const animationStart = performance.now();
  const step = (now) => {
    const progress = Math.min((now - animationStart) / duration, 1);
    const easedProgress = 1 - Math.pow(1 - progress, 3);
    const frame = tuitionMetricKeys.reduce((result, key) => {
      const rawValue = start[key] + (next[key] - start[key]) * easedProgress;
      result[key] = precisionMap[key] === 0
        ? Math.round(rawValue)
        : Math.round(rawValue * 100) / 100;
      return result;
    }, {});
    displayedTuitionOverview.value = frame;

    if (progress < 1) {
      tuitionAnimationFrameId = window.requestAnimationFrame(step);
      return;
    }

    displayedTuitionOverview.value = next;
    tuitionAnimationFrameId = 0;
  };

  tuitionAnimationFrameId = window.requestAnimationFrame(step);
};

const weekScheduleRows = computed(() => {
  return weekSchedule.value.map((slot) => {
    const status = String(slot.status ?? 'PLANNED').toUpperCase();
    const scheduleId = slot.id ?? null;
    const isCompleted = status === 'COMPLETED';
    return {
      id: scheduleId ?? `${slot.studentId}-${slot.startTime}`,
      scheduleId,
      weekday: formatWeekday(slot.startTime),
      subject: slot.subject ?? '英语',
      timeRange: formatTimeRange(slot.startTime, slot.endTime),
      student: slot.studentName ?? '',
      status,
      isCompleted,
      statusLabel: isCompleted ? '已销课' : '待上课',
      canToggle: Boolean(scheduleId),
      actionLabel: isCompleted ? '撤销销课' : '销课'
    };
  });
});

const currentWeekLabel = computed(() => {
  const start = new Date(currentWeekStart.value);
  const end = new Date(currentWeekStart.value);
  end.setDate(end.getDate() + 6);
  return `${formatMonthDay(start)} - ${formatMonthDay(end)}`;
});

const isViewingCurrentWeek = computed(() => {
  return formatDateParam(currentWeekStart.value) === formatDateParam(getWeekStart(new Date()));
});

const isCompleting = (scheduleId) => completingScheduleIds.value.has(scheduleId);

const resetForm = () => {
  form.name = '';
  form.gender = genders[0];
  form.grade = grades[0];
  form.tuitionPaid = '';
  form.lessonCount = '';
  formError.value = '';
};

const resetRenewForm = () => {
  renewForm.studentId = '';
  renewForm.tuitionPaid = '';
  renewForm.lessonCount = '';
  renewError.value = '';
  renewSuccess.value = '';
};

const resetTrialForm = () => {
  trialForm.name = '';
  trialForm.grade = grades[0];
  trialForm.trialTime = '';
};

const getTodayCompletionStorageKey = () => {
  return `qingqingketang:today-plan:${formatDateParam(new Date())}`;
};

const loadTodayCompletionIds = () => {
  try {
    const stored = window.localStorage.getItem(getTodayCompletionStorageKey());
    if (!stored) {
      return;
    }
    const parsed = JSON.parse(stored);
    if (Array.isArray(parsed)) {
      todayCompletionIds.value = new Set(parsed);
    }
  } catch (error) {
    todayCompletionIds.value = new Set();
  }
};

const saveTodayCompletionIds = (value) => {
  try {
    window.localStorage.setItem(getTodayCompletionStorageKey(), JSON.stringify(Array.from(value)));
  } catch (error) {
    // Ignore storage errors and keep the toggle local to the current session.
  }
};

const toggleTodayItem = (itemId) => {
  if (!itemId || itemId === 'empty') {
    return;
  }
  const next = new Set(todayCompletionIds.value);
  if (next.has(itemId)) {
    next.delete(itemId);
  } else {
    next.add(itemId);
  }
  todayCompletionIds.value = next;
  saveTodayCompletionIds(next);
};

const handleTodayItemClick = async (item) => {
  if (!item || !item.toggleable) {
    return;
  }
  if (item.type === 'schedule') {
    if (!item.scheduleId || item.isLoading) {
      return;
    }
    await toggleScheduleCompletion(item.scheduleId, item.isCompleted, { source: 'today' });
    return;
  }
  toggleTodayItem(item.id);
};

const sortWeekdayValues = (values) => {
  return [...new Set(values)].sort((a, b) => a - b);
};

const normalizeArrangeWeekdays = (values, targetCount) => {
  const selected = sortWeekdayValues(values.filter((value) => weekdayOptions.some((option) => option.value === value)));
  for (const option of weekdayOptions) {
    if (selected.length >= targetCount) {
      break;
    }
    if (!selected.includes(option.value)) {
      selected.push(option.value);
    }
  }
  return sortWeekdayValues(selected).slice(0, targetCount);
};

const setArrangeFrequency = (value) => {
  const normalized = Math.min(weekdayOptions.length, Math.max(1, Number(value) || 1));
  arrangeDraft.weeklySessions = normalized;
  arrangeDraft.weekdays = normalizeArrangeWeekdays(arrangeDraft.weekdays, normalized);
  arrangeError.value = '';
};

const toggleArrangeWeekday = (weekdayValue) => {
  if (!weekdayValue) {
    return;
  }
  const selected = sortWeekdayValues(arrangeDraft.weekdays);
  if (selected.includes(weekdayValue)) {
    if (selected.length === 1) {
      return;
    }
    arrangeDraft.weekdays = selected.filter((value) => value !== weekdayValue);
  } else if (selected.length < arrangeDraft.weeklySessions) {
    arrangeDraft.weekdays = sortWeekdayValues([...selected, weekdayValue]);
  } else {
    arrangeDraft.weekdays = sortWeekdayValues([...selected.slice(0, selected.length - 1), weekdayValue]);
  }
  arrangeError.value = '';
};

const resetArrangeDraft = (keepMessage = false) => {
  arrangeDraft.studentId = '';
  arrangeDraft.weeklySessions = 1;
  arrangeDraft.weekdays = [weekdayOptions[0].value];
  arrangeDraft.startDate = '';
  arrangeDraft.startTime = '';
  arrangeDraft.endTime = '';
  arrangeError.value = '';
  if (!keepMessage) {
    arrangeFeedback.value = '';
  }
};

const loadStudents = async () => {
  loadingStudents.value = true;
  studentLoadError.value = '';
  try {
    const response = await fetch(`${API_BASE}/api/students`);
    if (!response.ok) {
      throw new Error('加载学生信息失败');
    }
    students.value = await response.json();
  } catch (error) {
    studentLoadError.value = normalizeError(error, '无法连接后端服务，请确认后端已启动。');
  } finally {
    loadingStudents.value = false;
  }
};

const submitRenew = async () => {
  renewError.value = '';
  renewSuccess.value = '';
  if (!isRenewValid.value) {
    renewError.value = '请完整填写续费信息';
    return;
  }

  renewSubmitting.value = true;
  try {
    const response = await fetch(`${API_BASE}/api/students/${renewForm.studentId}/payments`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        tuitionPaid: Number(renewForm.tuitionPaid),
        lessonCount: Number(renewForm.lessonCount)
      })
    });
    if (!response.ok) {
      const text = await response.text();
      let message = '续费录入失败';
      if (text) {
        try {
          const parsed = JSON.parse(text);
          message = parsed.message ?? message;
        } catch (e) {
          message = text;
        }
      }
      throw new Error(message);
    }
    resetRenewForm();
    renewSuccess.value = '续费已录入';
    await loadStudents();
    await loadTuitionOverview();
  } catch (error) {
    renewError.value = normalizeError(error, '续费录入失败');
  } finally {
    renewSubmitting.value = false;
  }
};

const loadTuitionOverview = async () => {
  tuitionLoading.value = true;
  tuitionError.value = '';
  try {
    const response = await fetch(`${API_BASE}/api/students/tuition-overview`);
    if (!response.ok) {
      throw new Error('学费数据加载失败');
    }
    applyTuitionOverview(await response.json());
  } catch (error) {
    tuitionError.value = normalizeError(error, '无法加载学费概览');
  } finally {
    tuitionLoading.value = false;
  }
};

const toggleForm = () => {
  showForm.value = !showForm.value;
  if (!showForm.value) {
    resetForm();
  }
};

const extractMessage = (text, fallback) => {
  if (!text) return fallback;
  try {
    const parsed = JSON.parse(text);
    return parsed.message ?? parsed.detail ?? fallback;
  } catch (e) {
    return text;
  }
};

const normalizeError = (error, fallback) => {
  const raw = error?.message ?? '';
  if (!raw || raw === 'Failed to fetch' || raw.includes('NetworkError')) {
    return fallback;
  }
  return raw;
};

const submitStudent = async () => {
  formError.value = '';
  formSuccess.value = '';
  if (!isFormValid.value) {
    formError.value = '请完整填写学生信息';
    return;
  }

  const tuitionNumber = Number(form.tuitionPaid);
  const lessonNumber = Number(form.lessonCount);

  if (Number.isNaN(tuitionNumber) || Number.isNaN(lessonNumber)) {
    formError.value = '学费和课时需为数字';
    return;
  }

  submitting.value = true;

  try {
    const response = await fetch(`${API_BASE}/api/students`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        name: form.name.trim(),
        gender: form.gender,
        grade: form.grade,
        tuitionPaid: tuitionNumber,
        lessonCount: lessonNumber
      })
    });

    const bodyText = await response.text();
    let data = null;
    if (bodyText) {
      try {
        data = JSON.parse(bodyText);
      } catch (e) {
        data = null;
      }
    }

    if (!response.ok) {
      const message = data?.message ?? data?.detail ?? '录入学生失败';
      throw new Error(message);
    }

    if (data) {
      students.value = [data, ...students.value];
    } else {
      await loadStudents();
    }
    await loadTuitionOverview();

    formSuccess.value = '学生信息已录入';
    setTimeout(() => {
      formSuccess.value = '';
    }, 2500);
    resetForm();
    showForm.value = false;
  } catch (error) {
    formError.value = normalizeError(error, '录入学生失败，请稍后重试。');
  } finally {
    submitting.value = false;
  }
};

const loadTrials = async () => {
  trialLoading.value = true;
  trialListError.value = '';
  try {
    const response = await fetch(`${API_BASE}/api/trials`);
    if (!response.ok) {
      throw new Error('试听数据加载失败');
    }
    trialLeads.value = await response.json();
  } catch (error) {
    trialListError.value = normalizeError(error, '无法加载试听记录');
  } finally {
    trialLoading.value = false;
  }
};

const submitTrial = async () => {
  trialError.value = '';
  trialSuccess.value = '';
  if (!trialForm.name.trim() || !trialForm.grade || !trialForm.trialTime) {
    trialError.value = '请完整填写试听信息';
    return;
  }

  trialSubmitting.value = true;
  try {
    const response = await fetch(`${API_BASE}/api/trials`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        name: trialForm.name.trim(),
        grade: trialForm.grade,
        trialTime: trialForm.trialTime
      })
    });
    const text = await response.text();
    let data = null;
    if (text) {
      try {
        data = JSON.parse(text);
      } catch (e) {
        data = null;
      }
    }
    if (!response.ok) {
      const message = data?.message ?? data?.detail ?? '录入试听失败';
      throw new Error(message);
    }
    if (data) {
      trialLeads.value = [data, ...trialLeads.value];
    } else {
      await loadTrials();
    }
    trialSuccess.value = '试听记录已创建';
    resetTrialForm();
    setTimeout(() => {
      trialSuccess.value = '';
    }, 2500);
  } catch (error) {
    trialError.value = normalizeError(error, '录入试听失败');
  } finally {
    trialSubmitting.value = false;
  }
};

onMounted(() => {
  loadTodayCompletionIds();
  loadStudents();
  loadTuitionOverview();
  loadWeekSchedule();
  loadTodayWeekSchedule();
  loadTrials();
});

onBeforeUnmount(() => {
  stopTuitionAnimation();
  if (tuitionHighlightTimeoutId) {
    window.clearTimeout(tuitionHighlightTimeoutId);
    tuitionHighlightTimeoutId = 0;
  }
  clearFeedbackTimer('today');
  clearFeedbackTimer('schedule');
});

const submitArrangeDraft = async () => {
  arrangeError.value = '';
  arrangeFeedback.value = '';
  if (!arrangeDraft.studentId || !arrangeDraft.startDate || !arrangeDraft.startTime || !arrangeDraft.endTime) {
    arrangeError.value = '请完整选择学生、开始日期与时间';
    return;
  }
  if (arrangeDraft.weekdays.length !== arrangeDraft.weeklySessions) {
    arrangeError.value = `每周 ${arrangeDraft.weeklySessions} 次课，请选择 ${arrangeDraft.weeklySessions} 个上课日`;
    return;
  }

  arrangeSubmitting.value = true;
  try {
    const response = await fetch(`${API_BASE}/api/schedules/students/${arrangeDraft.studentId}/auto-generate`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          weekdays: sortWeekdayValues(arrangeDraft.weekdays),
          startDate: arrangeDraft.startDate,
          startTime: arrangeDraft.startTime,
          endTime: arrangeDraft.endTime
        })
    });
    const text = await response.text();
    let data = [];
    if (text) {
      try {
        data = JSON.parse(text);
      } catch (e) {
        data = [];
      }
    }
    if (!response.ok) {
      const message = data?.message ?? data?.detail ?? '排课失败';
      throw new Error(message);
    }
    const generatedCount = Array.isArray(data) ? data.length : 0;
    arrangeFeedback.value = `已自动生成 ${generatedCount} 节课`;
    setTimeout(() => {
      arrangeFeedback.value = '';
    }, 2500);
    resetArrangeDraft(true);
    await loadWeekSchedule();
    await loadTodayWeekSchedule();
  } catch (error) {
    arrangeError.value = normalizeError(error, '排课失败，请稍后重试');
  } finally {
    arrangeSubmitting.value = false;
  }
};

const loadWeekSchedule = async () => {
  scheduleLoading.value = true;
  scheduleError.value = '';
  try {
    const startParam = formatDateParam(currentWeekStart.value);
    const response = await fetch(`${API_BASE}/api/schedules/week?start=${startParam}`);
    if (!response.ok) {
      throw new Error('课表加载失败');
    }
    weekSchedule.value = await response.json();
  } catch (error) {
    scheduleError.value = normalizeError(error, '无法加载课表');
    weekSchedule.value = [];
  } finally {
    scheduleLoading.value = false;
  }
};

const loadTodayWeekSchedule = async () => {
  todayScheduleLoading.value = true;
  todayScheduleError.value = '';
  try {
    const startParam = formatDateParam(todayWeekStart);
    const response = await fetch(`${API_BASE}/api/schedules/week?start=${startParam}`);
    if (!response.ok) {
      throw new Error('今日安排加载失败');
    }
    todayWeekSchedule.value = await response.json();
  } catch (error) {
    todayScheduleError.value = normalizeError(error, '无法加载今日安排');
    todayWeekSchedule.value = [];
  } finally {
    todayScheduleLoading.value = false;
  }
};

const shiftWeek = async (offset) => {
  const next = new Date(currentWeekStart.value);
  next.setDate(next.getDate() + offset * 7);
  currentWeekStart.value = getWeekStart(next);
  await loadWeekSchedule();
};

const jumpToCurrentWeek = async () => {
  currentWeekStart.value = getWeekStart(new Date());
  await loadWeekSchedule();
};

const syncScheduleMutationResult = async (data) => {
  if (data && data.id) {
    weekSchedule.value = weekSchedule.value.map((slot) => (slot.id === data.id ? data : slot));
    todayWeekSchedule.value = todayWeekSchedule.value.map((slot) => (slot.id === data.id ? data : slot));
    return;
  }
  await loadWeekSchedule();
  await loadTodayWeekSchedule();
};

const toggleScheduleCompletion = async (scheduleId, isCompleted, options = {}) => {
  if (!scheduleId) return;
  const feedbackScope = options.source === 'today' ? 'today' : 'schedule';
  const actionPath = isCompleted ? 'undo-complete' : 'complete';
  const successMessage = isCompleted ? '已撤销销课' : '销课完成';
  const errorFallback = isCompleted ? '撤销销课失败' : '销课失败';
  clearActionFeedback();
  const pending = new Set(completingScheduleIds.value);
  pending.add(scheduleId);
  completingScheduleIds.value = pending;
  try {
    const response = await fetch(`${API_BASE}/api/schedules/${scheduleId}/${actionPath}`, { method: 'POST' });
    const text = await response.text();
    let data = null;
    if (text) {
      try {
        data = JSON.parse(text);
      } catch (e) {
        data = null;
      }
    }
    if (!response.ok) {
      const message = data?.message ?? data?.detail ?? errorFallback;
      throw new Error(message);
    }
    await syncScheduleMutationResult(data);
    await loadTuitionOverview();
    queueActionMessage(feedbackScope, successMessage);
  } catch (error) {
    const message = normalizeError(error, errorFallback);
    if (feedbackScope === 'today') {
      todayActionError.value = message;
    } else {
      scheduleActionError.value = message;
    }
  } finally {
    const next = new Set(completingScheduleIds.value);
    next.delete(scheduleId);
    completingScheduleIds.value = next;
  }
};

function getWeekStart(date) {
  const result = new Date(date);
  const day = result.getDay() === 0 ? 7 : result.getDay();
  result.setHours(0, 0, 0, 0);
  result.setDate(result.getDate() - day + 1);
  return result;
}

function formatDateParam(date) {
  const d = new Date(date);
  const year = d.getFullYear();
  const month = `${d.getMonth() + 1}`.padStart(2, '0');
  const day = `${d.getDate()}`.padStart(2, '0');
  return `${year}-${month}-${day}`;
}

const timeFormatter = new Intl.DateTimeFormat('zh-CN', {
  hour: '2-digit',
  minute: '2-digit',
  hour12: false
});

const formatClock = (value) => {
  if (!value) return '--';
  return timeFormatter.format(new Date(value));
};

const toTimestamp = (value) => {
  if (!value) return null;
  const ms = new Date(value).getTime();
  return Number.isNaN(ms) ? null : ms;
};

const isSameDay = (value, dateKey) => {
  if (!value) return false;
  return formatDateParam(value) === dateKey;
};

const formatWeekday = (value) => {
  if (!value) return '--';
  const dayIndex = new Date(value).getDay();
  const labels = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
  return labels[dayIndex];
};

const formatDateLabel = (value) => {
  if (!value) return '--';
  const d = new Date(value);
  return `${d.getMonth() + 1}月${d.getDate()}日`;
};

const formatMonthDay = (value) => {
  if (!value) return '--';
  const d = new Date(value);
  return `${d.getMonth() + 1}月${d.getDate()}日`;
};

const formatTimeRange = (start, end) => {
  if (!start || !end) return '--';
  return `${timeFormatter.format(new Date(start))}-${timeFormatter.format(new Date(end))}`;
};

</script>
