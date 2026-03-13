<template>
  <section class="page-stack">
    <PageHeader
      eyebrow="Dashboard"
      title="运营总览"
      description="首页只展示核心运营信息，把录入和管理动作交给各业务模块页面。"
    />

    <div class="metric-grid">
      <el-card v-for="card in metricCards" :key="card.label" shadow="hover" class="metric-card">
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

    <div class="dashboard-grid">
      <el-card shadow="never" class="dashboard-card">
        <template #header>
          <div class="card-head">
            <div>
              <span class="card-head__eyebrow">Today</span>
              <h3>今日课程列表</h3>
            </div>
            <div class="card-head__side">
              <span class="card-head__meta">{{ todayLabel }}</span>
              <el-button text @click="router.push('/courses')">查看全部</el-button>
            </div>
          </div>
        </template>

        <div v-if="loading" class="page-state">数据加载中…</div>
        <el-alert v-else-if="error" :title="error" type="error" show-icon :closable="false" />
        <div v-else-if="todayCourseRows.length" class="today-course-list">
          <article v-for="row in todayCourseRows" :key="row.id" class="today-course-item">
            <div class="today-course-item__time">{{ row.time }}</div>
            <div class="today-course-item__content">
              <strong>{{ row.studentName }}</strong>
              <span>{{ row.courseType }}</span>
            </div>
            <el-tag :type="row.statusType" effect="plain">{{ row.statusLabel }}</el-tag>
          </article>
        </div>
        <el-empty v-else description="今天还没有课程安排" :image-size="72" />
      </el-card>

      <el-card shadow="never" class="dashboard-card">
        <template #header>
          <div class="card-head">
            <div>
              <span class="card-head__eyebrow">Actions</span>
              <h3>快捷操作</h3>
            </div>
          </div>
        </template>

        <div class="quick-action-grid">
          <button
            v-for="action in quickActions"
            :key="action.label"
            type="button"
            class="quick-action"
            @click="router.push(action.to)"
          >
            <span class="quick-action__eyebrow">{{ action.eyebrow }}</span>
            <el-icon class="quick-action__icon" :class="action.tone">
              <component :is="action.icon" />
            </el-icon>
            <strong>{{ action.label }}</strong>
            <span>{{ action.description }}</span>
          </button>
        </div>
      </el-card>
    </div>

    <el-card shadow="never" class="dashboard-card">
      <template #header>
        <div class="card-head">
          <div>
            <span class="card-head__eyebrow">Students</span>
            <h3>最近新增学生</h3>
          </div>
          <el-button text @click="router.push('/students')">学生管理</el-button>
        </div>
      </template>

      <div v-if="loading" class="page-state">数据加载中…</div>
      <el-alert v-else-if="error" :title="error" type="error" show-icon :closable="false" />
      <div v-else-if="recentStudents.length" class="recent-student-list">
        <article v-for="student in recentStudents" :key="student.id" class="recent-student-item">
          <div class="recent-student-item__identity">
            <el-avatar class="recent-student-item__avatar">
              {{ student.name?.slice(0, 1) ?? '学' }}
            </el-avatar>
            <div>
              <strong>{{ student.name }}</strong>
              <p>{{ formatTimestamp(student.createdAt) }}</p>
            </div>
          </div>
          <div class="recent-student-item__meta">
            <el-tag effect="plain">{{ student.grade }}</el-tag>
            <span>剩余 {{ student.remainingLessons ?? 0 }} 节</span>
          </div>
        </article>
      </div>
      <el-empty v-else description="还没有学生数据" :image-size="72" />
    </el-card>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { Plus, Calendar, Promotion, WalletFilled, User, Opportunity } from '@element-plus/icons-vue';
import PageHeader from '../components/common/PageHeader.vue';
import { api } from '../services/api';
import {
  formatCurrency,
  formatDateParam,
  formatLongDate,
  formatTimeRange,
  formatTimestamp,
  getWeekStart,
  isCurrentMonth,
  normalizeError
} from '../utils/format';

const router = useRouter();

const loading = ref(false);
const error = ref('');
const students = ref([]);
const paymentRecords = ref([]);
const trials = ref([]);
const weekSchedules = ref([]);

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

const todayCourseRows = computed(() => {
  const today = formatDateParam(new Date());
  return weekSchedules.value
    .filter((item) => formatDateParam(item.startTime) === today)
    .map((item) => {
      const isCompleted = String(item.status ?? '').toUpperCase() === 'COMPLETED';
      return {
        id: item.id,
        time: formatTimeRange(item.startTime, item.endTime),
        studentName: item.studentName ?? '未命名',
        courseType: item.subject ?? '正式课',
        statusLabel: isCompleted ? '已销课' : '待上课',
        statusType: isCompleted ? 'success' : 'warning'
      };
    });
});

const monthIncome = computed(() => {
  const total = paymentRecords.value
    .filter((item) => isCurrentMonth(item.paidAt))
    .reduce((sum, item) => sum + Number(item.tuitionPaid ?? 0), 0);
  return formatCurrency(total);
});

const pendingTrials = computed(() => {
  return trials.value.filter((item) => String(item.status ?? 'PENDING').toUpperCase() === 'PENDING').length;
});

const todayLabel = computed(() => formatLongDate(new Date()));

const metricCards = computed(() => [
  {
    label: '今日课程数量',
    value: `${todayCourseRows.value.length}`,
    helper: '今日待执行课程安排',
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
    value: `${monthIncome.value} 元`,
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

const recentStudents = computed(() => students.value.slice(0, 5));

const quickActions = [
  {
    label: '新增学生',
    description: '快速创建学生档案',
    to: { path: '/students', query: { action: 'create' } },
    icon: Plus,
    eyebrow: 'Students',
    tone: 'quick-action__icon--blue'
  },
  {
    label: '排课',
    description: '进入课程管理发起排课',
    to: { path: '/courses', query: { action: 'create' } },
    icon: Calendar,
    eyebrow: 'Schedule',
    tone: 'quick-action__icon--slate'
  },
  {
    label: '录入试听',
    description: '新建试听记录并跟进状态',
    to: { path: '/trials', query: { action: 'create' } },
    icon: Promotion,
    eyebrow: 'Trial',
    tone: 'quick-action__icon--amber'
  },
  {
    label: '收费登记',
    description: '进入财务页登记收费',
    to: { path: '/finance', query: { action: 'renew' } },
    icon: WalletFilled,
    eyebrow: 'Finance',
    tone: 'quick-action__icon--green'
  }
];

onMounted(loadDashboard);
</script>

<style scoped>
.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.metric-card {
  background: var(--app-surface);
}

.metric-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  gap: 12px;
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
}

.metric-card small {
  color: var(--app-text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(320px, 0.9fr);
  gap: 16px;
}

.dashboard-card {
  background: var(--app-surface);
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
  grid-template-columns: 110px minmax(0, 1fr) auto;
  align-items: center;
  gap: 14px;
  padding: 14px;
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-sm);
  background: var(--app-surface-muted);
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
}

.today-course-item__content strong {
  display: block;
  margin-bottom: 4px;
  font-size: 15px;
}

.today-course-item__content span {
  color: var(--app-text-secondary);
  font-size: 13px;
}

.quick-action-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.quick-action {
  padding: 16px;
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-sm);
  background: var(--app-surface-muted);
  text-align: left;
  cursor: pointer;
  transition: transform 180ms ease, border-color 180ms ease, box-shadow 180ms ease;
}

.quick-action:hover {
  transform: translateY(-1px);
  border-color: rgba(59, 130, 246, 0.28);
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.06);
}

.quick-action__eyebrow {
  display: inline-flex;
  margin-bottom: 10px;
  color: var(--app-text-tertiary);
  font-size: 11px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.quick-action__icon {
  display: inline-flex;
  margin-bottom: 14px;
  padding: 9px;
  border-radius: var(--app-radius-sm);
  font-size: 18px;
}

.quick-action__icon--blue {
  background: rgba(59, 130, 246, 0.12);
  color: var(--app-primary);
}

.quick-action__icon--slate {
  background: rgba(15, 23, 42, 0.08);
  color: #334155;
}

.quick-action__icon--amber {
  background: rgba(245, 158, 11, 0.12);
  color: #d97706;
}

.quick-action__icon--green {
  background: rgba(34, 197, 94, 0.12);
  color: #16a34a;
}

.quick-action strong {
  display: block;
  margin-bottom: 6px;
  font-size: 16px;
}

.quick-action span {
  display: block;
  color: var(--app-text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.recent-student-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.recent-student-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px;
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-sm);
  background: var(--app-surface-muted);
}

.recent-student-item__identity {
  display: flex;
  align-items: center;
  gap: 12px;
}

.recent-student-item__avatar {
  background: rgba(59, 130, 246, 0.14);
  color: var(--app-primary);
  font-weight: 700;
}

.recent-student-item__identity strong {
  display: block;
  margin-bottom: 4px;
  font-size: 15px;
}

.recent-student-item__identity p {
  color: var(--app-text-secondary);
  font-size: 13px;
}

.recent-student-item__meta {
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--app-text-secondary);
  font-size: 13px;
}

.page-state {
  color: var(--app-text-secondary);
  font-size: 14px;
}

@media (max-width: 1200px) {
  .metric-grid,
  .dashboard-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 900px) {
  .metric-grid,
  .dashboard-grid,
  .quick-action-grid {
    grid-template-columns: 1fr;
  }

  .today-course-item,
  .recent-student-item {
    grid-template-columns: 1fr;
    align-items: flex-start;
  }

  .today-course-item {
    display: flex;
    flex-direction: column;
  }

  .recent-student-item {
    flex-direction: column;
  }

  .recent-student-item__meta,
  .card-head__side {
    width: 100%;
    justify-content: space-between;
  }
}
</style>
