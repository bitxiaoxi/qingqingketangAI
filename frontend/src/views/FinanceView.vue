<template>
  <section class="page-stack">
    <PageHeader
      eyebrow="Finance"
      title="财务管理"
      description="把收费记录与续费登记集中到一个清晰、紧凑的后台页面中。"
    >
      <template #actions>
        <el-button @click="loadFinanceData">刷新数据</el-button>
      </template>
    </PageHeader>

    <el-card shadow="never" class="finance-hero">
      <div class="finance-hero__body">
        <div class="finance-hero__intro">
          <span class="finance-hero__eyebrow">Finance Center</span>
          <h3>收入、续费与收费流水一页查看</h3>
          <p>适合日常登记收费、核对本月收入和快速查看学生缴费历史，符合常见后台系统的财务工作流。</p>
        </div>

        <div class="metric-grid">
          <el-card v-for="card in metricCards" :key="card.label" shadow="hover" class="metric-card">
            <div class="metric-card__head">
              <span class="metric-card__label">{{ card.label }}</span>
              <span class="metric-card__tone" :class="card.tone"></span>
            </div>
            <strong>{{ card.value }}</strong>
            <small>{{ card.helper }}</small>
          </el-card>
        </div>
      </div>
    </el-card>

    <div class="finance-grid">
      <el-card shadow="never" class="panel-card renew-card">
        <template #header>
          <div class="card-head">
            <div>
              <span class="card-head__eyebrow">Renewal</span>
              <h3>续费登记</h3>
            </div>
          </div>
        </template>

        <div class="renew-guide">
          <span class="renew-guide__eyebrow">Quick Form</span>
          <p>录入学生、缴费金额、课时与备注后，会自动更新财务数据和课时余额。</p>
        </div>

        <el-form label-position="top" class="renew-form">
          <el-form-item label="学生" required>
            <el-select v-model="renewForm.studentId" filterable placeholder="请选择学生">
              <el-option
                v-for="student in students"
                :key="student.id"
                :label="`${student.name} · ${student.grade}`"
                :value="student.id"
              />
            </el-select>
          </el-form-item>

          <div class="renew-form__grid">
            <el-form-item label="缴费金额（元）" required>
              <el-input v-model="renewForm.tuitionPaid" type="number" min="0" placeholder="12000" />
            </el-form-item>

            <el-form-item label="课时（节）" required>
              <el-input v-model="renewForm.lessonCount" type="number" min="1" placeholder="40" />
            </el-form-item>
          </div>

          <el-form-item label="备注">
            <el-input
              v-model="renewForm.remark"
              type="textarea"
              :rows="3"
              placeholder="如：春季续费、转账到账、家长备注等"
            />
          </el-form-item>

          <div class="renew-preview">
            <span class="renew-preview__eyebrow">Payment Preview</span>
            <strong>{{ renewPreview }}</strong>
            <small>登记成功后，这笔收费会同步进入下方收费记录。</small>
          </div>

          <div class="renew-form__actions">
            <el-button @click="resetRenewForm">清空</el-button>
            <el-button type="primary" :loading="submitting" @click="submitRenewForm">登记收费</el-button>
          </div>
        </el-form>
      </el-card>

      <el-card shadow="never" class="panel-card finance-side-card">
        <template #header>
          <div class="card-head">
            <div>
              <span class="card-head__eyebrow">Filters</span>
              <h3>流水筛选</h3>
            </div>
          </div>
        </template>

        <div class="filter-stack">
          <div class="filter-stack__group">
            <span class="filter-stack__label">按学生筛选</span>
            <el-select v-model="filters.studentId" filterable clearable placeholder="全部学生">
              <el-option
                v-for="student in students"
                :key="student.id"
                :label="`${student.name} · ${student.grade}`"
                :value="student.id"
              />
            </el-select>
          </div>

          <div class="filter-stack__summary">
            <span class="filter-stack__summary-label">当前结果</span>
            <strong>{{ filteredRecords.length }} 条收费流水</strong>
            <small v-if="selectedFilterStudent">
              已筛选 {{ selectedFilterStudent.name }} · {{ selectedFilterStudent.grade }}
            </small>
            <small v-else>当前显示全部学生的收费记录</small>
          </div>

          <el-button @click="resetFilters">重置筛选</el-button>
        </div>
      </el-card>
    </div>

    <el-card shadow="never" class="panel-card finance-table-card">
      <template #header>
        <div class="card-head">
          <div>
            <span class="card-head__eyebrow">Records</span>
            <h3>收费记录</h3>
          </div>
          <span class="card-head__meta">共 {{ filteredRecords.length }} 条收费流水</span>
        </div>
      </template>

      <div v-if="loading" class="page-state">财务数据加载中…</div>
      <el-alert v-else-if="error" :title="error" type="error" show-icon :closable="false" />
      <el-table
        v-else-if="filteredRecords.length"
        :data="filteredRecords"
        stripe
        class="finance-table"
      >
        <el-table-column label="学生" min-width="180">
          <template #default="{ row }">
            <div class="student-cell">
              <el-avatar class="student-cell__avatar">
                {{ row.studentName?.slice(0, 1) ?? '学' }}
              </el-avatar>
              <div>
                <strong>{{ row.studentName }}</strong>
                <span>收费流水</span>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="金额" min-width="120">
          <template #default="{ row }">
            <strong class="finance-amount">{{ formatAmount(row.tuitionPaid) }}</strong>
          </template>
        </el-table-column>

        <el-table-column prop="lessonCount" label="课时" width="90" />

        <el-table-column label="时间" min-width="180">
          <template #default="{ row }">
            {{ formatTimestamp(row.paidAt) }}
          </template>
        </el-table-column>

        <el-table-column prop="remark" label="备注" min-width="220" show-overflow-tooltip />

        <el-table-column label="操作" min-width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goToStudent(row.studentId)">查看学生</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="暂无收费流水" :image-size="72" />
    </el-card>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import PageHeader from '../components/common/PageHeader.vue';
import { api } from '../services/api';
import {
  formatAmount,
  formatCurrency,
  formatTimestamp,
  isCurrentMonth,
  normalizeError
} from '../utils/format';

const route = useRoute();
const router = useRouter();

const loading = ref(false);
const error = ref('');
const submitting = ref(false);
const students = ref([]);
const records = ref([]);
const tuitionOverview = ref({
  totalReceived: 0,
  totalConsumed: 0,
  totalPending: 0
});
const filters = reactive({
  studentId: route.query.studentId ? Number(route.query.studentId) : null
});
const renewForm = reactive({
  studentId: route.query.studentId ? Number(route.query.studentId) : null,
  tuitionPaid: '',
  lessonCount: '',
  remark: ''
});

const loadFinanceData = async () => {
  loading.value = true;
  error.value = '';
  try {
    const [studentsData, overviewData, recordsData] = await Promise.all([
      api.listStudents(),
      api.getTuitionOverview(),
      api.listPaymentRecords()
    ]);
    students.value = studentsData ?? [];
    tuitionOverview.value = overviewData ?? tuitionOverview.value;
    records.value = recordsData ?? [];
  } catch (requestError) {
    error.value = normalizeError(requestError, '财务数据加载失败');
  } finally {
    loading.value = false;
  }
};

const monthIncome = computed(() => {
  const total = records.value
    .filter((record) => isCurrentMonth(record.paidAt))
    .reduce((sum, record) => sum + Number(record.tuitionPaid ?? 0), 0);
  return `${formatCurrency(total)} 元`;
});

const metricCards = computed(() => [
  {
    label: '总收入',
    value: formatAmount(tuitionOverview.value.totalReceived),
    helper: '系统累计收费流水',
    tone: 'metric-card__tone--blue'
  },
  {
    label: '本月收入',
    value: monthIncome.value,
    helper: '按本月收费时间汇总',
    tone: 'metric-card__tone--green'
  },
  {
    label: '待缴费用',
    value: formatAmount(tuitionOverview.value.totalPending),
    helper: '已收但尚未完成核销',
    tone: 'metric-card__tone--amber'
  }
]);

const filteredRecords = computed(() => {
  return records.value.filter((record) => !filters.studentId || record.studentId === filters.studentId);
});

const selectedFilterStudent = computed(() => {
  return students.value.find((student) => student.id === filters.studentId) ?? null;
});

const selectedRenewStudent = computed(() => {
  return students.value.find((student) => student.id === renewForm.studentId) ?? null;
});

const renewPreview = computed(() => {
  const studentLabel = selectedRenewStudent.value
    ? `${selectedRenewStudent.value.name} · ${selectedRenewStudent.value.grade}`
    : '未选择学生';
  const amount = renewForm.tuitionPaid ? `${renewForm.tuitionPaid} 元` : '未填写金额';
  const lessons = renewForm.lessonCount ? `${renewForm.lessonCount} 节` : '未填写课时';
  return `${studentLabel} · ${amount} · ${lessons}`;
});

const resetRenewForm = () => {
  renewForm.studentId = filters.studentId || null;
  renewForm.tuitionPaid = '';
  renewForm.lessonCount = '';
  renewForm.remark = '';
};

const submitRenewForm = async () => {
  if (!renewForm.studentId || !renewForm.tuitionPaid || !renewForm.lessonCount) {
    ElMessage.error('请完整填写收费信息');
    return;
  }

  submitting.value = true;
  try {
    await api.createPayment(renewForm.studentId, {
      tuitionPaid: Number(renewForm.tuitionPaid),
      lessonCount: Number(renewForm.lessonCount),
      remark: renewForm.remark?.trim() || ''
    });
    ElMessage.success('收费登记成功');
    resetRenewForm();
    await loadFinanceData();
  } catch (requestError) {
    ElMessage.error(normalizeError(requestError, '收费登记失败'));
  } finally {
    submitting.value = false;
  }
};

const resetFilters = () => {
  filters.studentId = null;
};

const goToStudent = (studentId) => {
  router.push({
    path: '/students',
    query: {
      studentId
    }
  });
};

watch(
  () => route.query.studentId,
  (studentId) => {
    const value = studentId ? Number(studentId) : null;
    filters.studentId = value;
    if (value && !renewForm.studentId) {
      renewForm.studentId = value;
    }
  },
  { immediate: true }
);

onMounted(loadFinanceData);
</script>

<style scoped>
.finance-hero,
.panel-card,
.metric-card {
  background: var(--app-surface);
}

.finance-hero__body {
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(420px, 1fr);
  gap: 20px;
}

.finance-hero__eyebrow,
.card-head__eyebrow,
.renew-guide__eyebrow,
.renew-preview__eyebrow {
  display: inline-flex;
  margin-bottom: 8px;
  color: var(--app-text-tertiary);
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.finance-hero__intro h3 {
  margin-bottom: 10px;
  font-size: 28px;
  line-height: 1.15;
}

.finance-hero__intro p {
  color: var(--app-text-secondary);
  line-height: 1.7;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.metric-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.metric-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.metric-card__label {
  color: var(--app-text-secondary);
  font-size: 13px;
}

.metric-card__tone {
  width: 10px;
  height: 10px;
  border-radius: 999px;
}

.metric-card__tone--blue {
  background: #3b82f6;
}

.metric-card__tone--green {
  background: #22c55e;
}

.metric-card__tone--amber {
  background: #f59e0b;
}

.metric-card strong {
  font-size: 30px;
  line-height: 1;
}

.metric-card small {
  color: var(--app-text-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.finance-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(320px, 0.85fr);
  gap: 16px;
}

.renew-guide {
  margin-bottom: 18px;
  padding: 14px 16px;
  border: 1px solid rgba(59, 130, 246, 0.12);
  border-radius: var(--app-radius-sm);
  background: var(--app-primary-soft);
}

.renew-guide p {
  color: var(--app-text-secondary);
  line-height: 1.7;
}

.renew-form {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.renew-form__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.renew-preview {
  padding: 16px;
  border-radius: var(--app-radius-sm);
  background: #111827;
  color: rgba(255, 255, 255, 0.92);
}

.renew-preview strong {
  display: block;
  margin-bottom: 6px;
  font-size: 16px;
  line-height: 1.6;
}

.renew-preview small {
  color: rgba(255, 255, 255, 0.68);
  line-height: 1.7;
}

.renew-form__actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.filter-stack {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.filter-stack__group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.filter-stack__label,
.card-head__meta {
  color: var(--app-text-secondary);
  font-size: 13px;
}

.filter-stack__summary {
  padding: 16px;
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-sm);
  background: var(--app-surface-muted);
}

.filter-stack__summary-label {
  display: block;
  margin-bottom: 8px;
  color: var(--app-text-tertiary);
  font-size: 12px;
}

.filter-stack__summary strong {
  display: block;
  margin-bottom: 6px;
  font-size: 24px;
}

.filter-stack__summary small {
  color: var(--app-text-secondary);
  line-height: 1.6;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.card-head h3 {
  font-size: 18px;
}

.finance-table :deep(.el-table__cell) {
  padding: 10px 0;
}

.student-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.student-cell__avatar {
  background: rgba(59, 130, 246, 0.14);
  color: var(--app-primary);
  font-weight: 700;
}

.student-cell strong {
  display: block;
  margin-bottom: 3px;
  font-size: 14px;
}

.student-cell span {
  color: var(--app-text-tertiary);
  font-size: 12px;
}

.finance-amount {
  font-size: 15px;
  color: var(--app-text-primary);
}

.page-state {
  color: var(--app-text-secondary);
  font-size: 14px;
}

@media (max-width: 1200px) {
  .finance-hero__body,
  .metric-grid,
  .finance-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .renew-form__grid {
    grid-template-columns: 1fr;
  }
}
</style>
