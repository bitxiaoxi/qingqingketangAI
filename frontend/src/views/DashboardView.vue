<template>
  <section class="page-stack dashboard-home">
    <PageHeader
      eyebrow="Dashboard"
      title="运营总览"
    />

    <div class="metric-grid">
      <el-card
        v-for="(card, index) in metricCards"
        :key="card.label"
        shadow="hover"
        class="metric-card"
        :style="{ '--enter-delay': `${index * 80}ms` }"
      >
        <div class="metric-card__head">
          <span class="metric-card__label">{{ card.label }}</span>
          <span class="metric-card__icon" :class="card.tone">
            <el-icon>
              <component :is="card.icon" />
            </el-icon>
          </span>
        </div>
        <strong>{{ card.value }}</strong>
        <small>{{ card.helper }}</small>
      </el-card>
    </div>

    <el-card shadow="never" class="dashboard-card" style="--enter-delay: 260ms;">
      <template #header>
        <div class="card-head">
          <div>
            <span class="card-head__eyebrow">Today</span>
            <h3>今日待办</h3>
          </div>
          <div class="card-head__side">
            <span class="card-head__meta">{{ todayLabel }}</span>
          </div>
        </div>
      </template>

      <div v-if="loading" class="page-state">数据加载中…</div>
      <el-alert v-else-if="error" :title="error" type="error" show-icon :closable="false" />
      <div v-else-if="todayTodoRows.length" class="today-course-list">
        <article
          v-for="(row, index) in todayTodoRows"
          :key="row.id"
          class="today-course-item"
          :data-done="row.isDone"
          :style="{ '--enter-delay': `${340 + index * 70}ms` }"
        >
          <button
            type="button"
            class="today-course-item__toggle"
            :data-done="row.isDone"
            :disabled="processingTodoId === row.id"
            :aria-label="getTodoToggleLabel(row)"
            @click="toggleTodoStatus(row)"
          >
            <span class="today-course-item__toggle-line"></span>
          </button>

          <div class="today-course-item__main">
            <div class="today-course-item__time">{{ row.time }}</div>
            <div class="today-course-item__content">
              <strong>{{ row.studentName }}</strong>
              <span>{{ row.courseType }}</span>
            </div>
          </div>

          <div class="today-course-item__side">
            <el-tag :type="row.statusType" effect="plain">{{ row.statusLabel }}</el-tag>
          </div>
        </article>
      </div>
      <el-empty v-else description="今天还没有课程或试听安排" :image-size="72" />
    </el-card>

  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { Calendar, WalletFilled, User, Opportunity } from '@element-plus/icons-vue';
import PageHeader from '../components/common/PageHeader.vue';
import { api } from '../services/api';
import {
  formatCurrency,
  formatClock,
  formatDateParam,
  formatLongDate,
  formatTimeRange,
  getWeekStart,
  isCurrentMonth,
  normalizeError
} from '../utils/format';

const loading = ref(false);
const error = ref('');
const students = ref([]);
const paymentRecords = ref([]);
const trials = ref([]);
const weekSchedules = ref([]);
const processingTodoId = ref('');
const trialRestoreStatusMap = ref({});
const animatedMonthIncome = ref(0);
let incomeAnimationFrame = 0;

const loadDashboard = async () => {
  loading.value = true;
  error.value = '';
  try {
    const weekStart = formatDateParam(getWeekStart(new Date()));
    const [studentsData, paymentsData, trialsData, schedulesData] = await Promise.all([
      api.listStudents(),
      api.listPaymentRecords(),
      api.listTrials(),
      api.listWeekSchedules(weekStart)
    ]);
    students.value = studentsData ?? [];
    paymentRecords.value = paymentsData ?? [];
    trials.value = trialsData ?? [];
    weekSchedules.value = schedulesData ?? [];
  } catch (requestError) {
    error.value = normalizeError(requestError, 'Dashboard 数据加载失败');
  } finally {
    loading.value = false;
  }
};

const normalizedTrialStatus = (status) => String(status ?? 'PENDING').toUpperCase();
const normalizedScheduleStatus = (status) => String(status ?? 'PLANNED').toUpperCase();

const trialStatusMetaMap = {
  PENDING: { label: '待试听', type: 'warning' },
  COMPLETED: { label: '已试听', type: 'success' },
  ENROLLED: { label: '已报名', type: 'primary' },
  LOST: { label: '未报名', type: 'info' }
};

const todayTodoRows = computed(() => {
  const today = formatDateParam(new Date());
  const scheduleRows = weekSchedules.value
    .filter((item) => formatDateParam(item.startTime) === today)
    .map((item) => {
      const isDone = normalizedScheduleStatus(item.status) === 'COMPLETED';
      return {
        id: `schedule-${item.id}`,
        entityId: item.id,
        kind: 'schedule',
        sortTime: new Date(item.startTime).getTime(),
        time: formatTimeRange(item.startTime, item.endTime),
        studentName: item.studentName ?? '未命名',
        courseType: item.subject ? `正式课 · ${item.subject}` : '正式课',
        statusLabel: isDone ? '已销课' : '待上课',
        statusType: isDone ? 'success' : 'warning',
        isDone
      };
    });

  const trialRows = trials.value
    .filter((item) => formatDateParam(item.trialTime) === today)
    .map((item) => {
      const status = normalizedTrialStatus(item.status);
      const statusMeta = trialStatusMetaMap[status] ?? trialStatusMetaMap.PENDING;
      return {
        id: `trial-${item.id}`,
        entityId: item.id,
        kind: 'trial',
        sortTime: new Date(item.trialTime).getTime(),
        time: formatClock(item.trialTime),
        studentName: item.name ?? '未命名',
        courseType: '试听课',
        statusLabel: statusMeta.label,
        statusType: statusMeta.type,
        isDone: status === 'COMPLETED'
      };
    });

  return [...scheduleRows, ...trialRows]
    .sort((left, right) => left.sortTime - right.sortTime)
    .map(({ sortTime, ...row }) => row);
});

const monthIncome = computed(() => {
  const total = paymentRecords.value
    .filter((item) => isCurrentMonth(item.paidAt))
    .reduce((sum, item) => sum + Number(item.tuitionPaid ?? 0), 0);
  return total;
});

const monthIncomeLabel = computed(() => formatCurrency(animatedMonthIncome.value));

const todayCourseCount = computed(() => {
  const today = formatDateParam(new Date());
  const todaySchedules = weekSchedules.value.filter((item) => formatDateParam(item.startTime) === today).length;
  const todayTrials = trials.value.filter((item) => formatDateParam(item.trialTime) === today).length;
  return todaySchedules + todayTrials;
});

const pendingTrials = computed(() => {
  return trials.value.filter((item) => String(item.status ?? 'PENDING').toUpperCase() === 'PENDING').length;
});

const todayLabel = computed(() => formatLongDate(new Date()));

const metricCards = computed(() => [
  {
    label: '今日课程数',
    value: `${todayCourseCount.value}`,
    helper: '今日正式课与试听课总数',
    icon: Calendar,
    tone: 'metric-card__icon--blue'
  },
  {
    label: '学生总数',
    value: `${students.value.length}`,
    helper: '当前在册学生规模',
    icon: User,
    tone: 'metric-card__icon--slate'
  },
  {
    label: '本月收入',
    value: `${monthIncomeLabel.value} 元`,
    helper: '按收费流水自动汇总',
    icon: WalletFilled,
    tone: 'metric-card__icon--green'
  },
  {
    label: '待试听人数',
    value: `${pendingTrials.value}`,
    helper: '等待试听或继续跟进',
    icon: Opportunity,
    tone: 'metric-card__icon--amber'
  }
]);

const updateScheduleStatusLocally = (scheduleId, status) => {
  weekSchedules.value = weekSchedules.value.map((item) => {
    if (item.id !== scheduleId) {
      return item;
    }
    return {
      ...item,
      status
    };
  });
};

const updateTrialStatusLocally = (trialId, status) => {
  trials.value = trials.value.map((item) => {
    if (item.id !== trialId) {
      return item;
    }
    return {
      ...item,
      status
    };
  });
};

const buildTrialUpdatePayload = (trial, status) => ({
  name: trial.name,
  grade: trial.grade,
  trialTime: trial.trialTime,
  status,
  note: trial.note ?? ''
});

const getTodoToggleLabel = (row) => {
  if (row.kind === 'trial') {
    return row.isDone ? '恢复试听状态' : '标记试听完成';
  }
  return row.isDone ? '恢复待上课' : '标记为已销课';
};

const toggleTodoStatus = async (row) => {
  processingTodoId.value = row.id;
  try {
    if (row.kind === 'schedule') {
      if (row.isDone) {
        await api.undoCompleteSchedule(row.entityId);
        updateScheduleStatusLocally(row.entityId, 'PLANNED');
        ElMessage.success('已恢复为待上课');
      } else {
        await api.completeSchedule(row.entityId);
        updateScheduleStatusLocally(row.entityId, 'COMPLETED');
        ElMessage.success('已销课');
      }
      return;
    }

    const currentTrial = trials.value.find((item) => item.id === row.entityId);
    if (!currentTrial) {
      throw new Error('试听记录不存在');
    }

    const currentStatus = normalizedTrialStatus(currentTrial.status);
    if (currentStatus === 'COMPLETED') {
      const restoreStatus = trialRestoreStatusMap.value[row.entityId] ?? 'PENDING';
      await api.updateTrial(row.entityId, buildTrialUpdatePayload(currentTrial, restoreStatus));
      updateTrialStatusLocally(row.entityId, restoreStatus);
      delete trialRestoreStatusMap.value[row.entityId];
      ElMessage.success('已恢复试听状态');
      return;
    }

    await api.updateTrial(row.entityId, buildTrialUpdatePayload(currentTrial, 'COMPLETED'));
    trialRestoreStatusMap.value[row.entityId] = currentStatus;
    updateTrialStatusLocally(row.entityId, 'COMPLETED');
    ElMessage.success('已完成试听');
  } catch (requestError) {
    ElMessage.error(normalizeError(requestError, '更新今日待办状态失败'));
  } finally {
    processingTodoId.value = '';
  }
};

const prefersReducedMotion = () => {
  if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') {
    return false;
  }
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches;
};

const animateMonthIncome = (targetValue) => {
  if (incomeAnimationFrame) {
    cancelAnimationFrame(incomeAnimationFrame);
    incomeAnimationFrame = 0;
  }

  const normalizedTarget = Number(targetValue ?? 0);
  if (prefersReducedMotion()) {
    animatedMonthIncome.value = normalizedTarget;
    return;
  }

  const fromValue = animatedMonthIncome.value;
  if (Math.abs(normalizedTarget - fromValue) < 0.01) {
    animatedMonthIncome.value = normalizedTarget;
    return;
  }

  const duration = 1400;
  const startTime = performance.now();
  const easeOutQuart = (progress) => 1 - Math.pow(1 - progress, 4);

  const tick = (timestamp) => {
    const progress = Math.min((timestamp - startTime) / duration, 1);
    const easedProgress = easeOutQuart(progress);
    animatedMonthIncome.value = fromValue + (normalizedTarget - fromValue) * easedProgress;

    if (progress < 1) {
      incomeAnimationFrame = requestAnimationFrame(tick);
      return;
    }

    animatedMonthIncome.value = normalizedTarget;
    incomeAnimationFrame = 0;
  };

  incomeAnimationFrame = requestAnimationFrame(tick);
};

watch(monthIncome, (value) => {
  animateMonthIncome(value);
}, { immediate: true });

onMounted(loadDashboard);

onBeforeUnmount(() => {
  if (incomeAnimationFrame) {
    cancelAnimationFrame(incomeAnimationFrame);
  }
});
</script>

<style scoped>
.dashboard-home {
  --dashboard-enter-duration: 640ms;
  --dashboard-enter-ease: cubic-bezier(0.22, 1, 0.36, 1);
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.metric-card {
  position: relative;
  overflow: hidden;
  isolation: isolate;
  background: var(--app-surface);
  opacity: 0;
  animation: dashboard-fade 320ms ease both;
  animation-delay: var(--enter-delay, 0ms);
  transition:
    transform 240ms cubic-bezier(0.22, 1, 0.36, 1),
    box-shadow 240ms ease,
    border-color 240ms ease,
    background-color 240ms ease;
}

.metric-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  gap: 12px;
  position: relative;
  z-index: 1;
  transform: translateY(18px) scale(0.985);
  animation: dashboard-rise-inner var(--dashboard-enter-duration) var(--dashboard-enter-ease) both;
  animation-delay: var(--enter-delay, 0ms);
}

.metric-card::before {
  content: '';
  position: absolute;
  top: -26px;
  right: -18px;
  width: 118px;
  height: 118px;
  border-radius: 999px;
  background: radial-gradient(circle, rgba(59, 130, 246, 0.12), transparent 72%);
  pointer-events: none;
  animation: dashboard-glow 7s ease-in-out infinite;
  animation-delay: calc(var(--enter-delay, 0ms) + 480ms);
}

.metric-card::after {
  content: '';
  position: absolute;
  inset: -20% auto -20% -36%;
  width: 44%;
  background: linear-gradient(
    90deg,
    rgba(255, 255, 255, 0),
    rgba(255, 255, 255, 0.66),
    rgba(255, 255, 255, 0)
  );
  transform: rotate(14deg) translateX(-140%);
  opacity: 0;
  pointer-events: none;
  transition: transform 520ms ease, opacity 320ms ease;
  z-index: 0;
}

.metric-card:hover {
  transform: translateY(-8px) scale(1.018);
  box-shadow: 0 20px 36px rgba(15, 23, 42, 0.12);
  border-color: rgba(59, 130, 246, 0.18);
}

.metric-card:hover::after {
  opacity: 1;
  transform: rotate(14deg) translateX(340%);
}

.metric-card:hover .metric-card__icon {
  transform: translateY(-3px) scale(1.06);
}

.metric-card__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.metric-card__label {
  color: var(--app-text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.metric-card__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: var(--app-radius-sm);
  font-size: 17px;
  animation: dashboard-float 4.8s ease-in-out infinite;
  animation-delay: calc(var(--enter-delay, 0ms) + 640ms);
  transition: transform 220ms ease, box-shadow 220ms ease;
}

.metric-card__icon--blue {
  background: rgba(59, 130, 246, 0.12);
  color: var(--app-primary);
}

.metric-card__icon--slate {
  background: rgba(15, 23, 42, 0.08);
  color: #334155;
}

.metric-card__icon--green {
  background: rgba(34, 197, 94, 0.12);
  color: #16a34a;
}

.metric-card__icon--amber {
  background: rgba(245, 158, 11, 0.12);
  color: #d97706;
}

.metric-card strong {
  font-size: 30px;
  line-height: 1.05;
  font-variant-numeric: tabular-nums;
}

.metric-card small {
  color: var(--app-text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.dashboard-card {
  position: relative;
  overflow: hidden;
  background: var(--app-surface);
  opacity: 0;
  transform: translateY(24px);
  animation: dashboard-rise var(--dashboard-enter-duration) var(--dashboard-enter-ease) both;
  animation-delay: var(--enter-delay, 0ms);
}

.dashboard-card::before {
  content: '';
  position: absolute;
  inset: auto auto -110px -80px;
  width: 220px;
  height: 220px;
  border-radius: 999px;
  background: radial-gradient(circle, rgba(59, 130, 246, 0.08), transparent 72%);
  pointer-events: none;
  animation: dashboard-glow 8.4s ease-in-out infinite;
  animation-delay: 820ms;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.card-head__side {
  display: flex;
  align-items: center;
  gap: 12px;
}

.card-head__meta {
  color: var(--app-text-tertiary);
  font-size: 13px;
}

.card-head__eyebrow {
  display: inline-flex;
  align-items: center;
  margin-bottom: 6px;
  color: var(--app-text-tertiary);
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.card-head h3 {
  font-size: 18px;
}

.today-course-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.today-course-item {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  position: relative;
  align-items: center;
  gap: 14px;
  padding: 14px;
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-sm);
  background: var(--app-surface-muted);
  overflow: hidden;
  transition: border-color 180ms ease, background 180ms ease;
  opacity: 0;
  transform: translateY(14px);
  animation: dashboard-rise 540ms var(--dashboard-enter-ease) both;
  animation-delay: var(--enter-delay, 0ms);
}

.today-course-item::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 14px;
  right: 14px;
  height: 3px;
  border-radius: 999px;
  background: linear-gradient(
    90deg,
    rgba(22, 163, 74, 0.12),
    rgba(22, 163, 74, 0.78),
    rgba(22, 163, 74, 0.12)
  );
  opacity: 0;
  transform: translateY(-50%) scaleX(0.96);
  transform-origin: center;
  pointer-events: none;
  transition: opacity 180ms ease, transform 180ms ease;
  z-index: 2;
}

.today-course-item[data-done='true'] {
  border-color: rgba(34, 197, 94, 0.18);
  background: rgba(34, 197, 94, 0.05);
}

.today-course-item[data-done='true']::after {
  opacity: 1;
  transform: translateY(-50%) scaleX(1);
}

.today-course-item__toggle {
  position: relative;
  width: 24px;
  height: 24px;
  border: 1.5px solid var(--app-border-strong);
  border-radius: 999px;
  background: var(--app-surface);
  cursor: pointer;
  transition: border-color 180ms ease, background 180ms ease, color 180ms ease;
  z-index: 3;
}

.today-course-item__toggle:hover {
  border-color: var(--app-primary);
}

.today-course-item__toggle:disabled {
  cursor: wait;
  opacity: 0.6;
}

.today-course-item__toggle[data-done='true'] {
  border-color: rgba(34, 197, 94, 0.38);
  background: rgba(34, 197, 94, 0.1);
  color: #16a34a;
}

.today-course-item__toggle-line {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 10px;
  height: 2px;
  border-radius: 999px;
  background: transparent;
  transform: translate(-50%, -50%);
  transition: background 180ms ease;
}

.today-course-item__toggle[data-done='true'] .today-course-item__toggle-line {
  background: currentColor;
}

.today-course-item__main {
  display: grid;
  grid-template-columns: 110px minmax(0, 1fr);
  align-items: center;
  gap: 14px;
  position: relative;
  z-index: 1;
}

.today-course-item__time {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 8px 10px;
  border-radius: var(--app-radius-sm);
  background: var(--app-primary-soft);
  color: var(--app-primary);
  font-size: 13px;
  font-weight: 600;
  animation: dashboard-chip-pulse 3.6s ease-in-out infinite;
  animation-delay: calc(var(--enter-delay, 0ms) + 820ms);
}

.today-course-item__side {
  display: flex;
  align-items: center;
  position: relative;
  z-index: 1;
}

.today-course-item__content strong {
  display: block;
  margin-bottom: 4px;
  font-size: 15px;
}

.today-course-item[data-done='true'] .today-course-item__main,
.today-course-item[data-done='true'] .today-course-item__side {
  opacity: 0.72;
}

.today-course-item__content span {
  color: var(--app-text-secondary);
  font-size: 13px;
}

.page-state {
  color: var(--app-text-secondary);
  font-size: 14px;
}

@keyframes dashboard-rise {
  0% {
    opacity: 0;
    transform: translateY(24px) scale(0.985);
  }

  100% {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@keyframes dashboard-fade {
  from {
    opacity: 0;
  }

  to {
    opacity: 1;
  }
}

@keyframes dashboard-rise-inner {
  0% {
    transform: translateY(18px) scale(0.985);
  }

  100% {
    transform: translateY(0) scale(1);
  }
}

@keyframes dashboard-float {
  0%,
  100% {
    transform: translateY(0);
  }

  50% {
    transform: translateY(-4px);
  }
}

@keyframes dashboard-chip-pulse {
  0%,
  100% {
    box-shadow: 0 0 0 rgba(59, 130, 246, 0);
    transform: translateY(0);
  }

  50% {
    box-shadow: 0 10px 18px rgba(59, 130, 246, 0.12);
    transform: translateY(-1px);
  }
}

@keyframes dashboard-glow {
  0%,
  100% {
    transform: translate3d(0, 0, 0) scale(1);
    opacity: 0.72;
  }

  50% {
    transform: translate3d(-8px, 10px, 0) scale(1.08);
    opacity: 1;
  }
}

@media (max-width: 1200px) {
  .metric-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 900px) {
  .metric-grid {
    grid-template-columns: 1fr;
  }

  .today-course-item {
    grid-template-columns: 1fr;
    align-items: flex-start;
  }

  .today-course-item__main {
    grid-template-columns: 1fr;
  }

  .card-head__side {
    width: 100%;
    justify-content: space-between;
  }
}

@media (prefers-reduced-motion: reduce) {
  .metric-card,
  .dashboard-card,
  .today-course-item,
  .metric-card__icon,
  .today-course-item__time,
  .metric-card::before,
  .dashboard-card::before {
    animation: none !important;
    opacity: 1;
    transform: none;
  }

  .metric-card::after {
    transition: none;
  }
}
</style>
