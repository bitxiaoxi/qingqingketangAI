<template>
  <section class="page-stack finance-page">
    <PageHeader title="财务管理" />

    <div class="finance-stack">
      <el-card shadow="never" class="panel-card summary-card">
        <div class="summary-grid">
          <article v-for="card in metricCards" :key="card.label" class="summary-item">
            <div class="summary-item__head">
              <span>{{ card.label }}</span>
              <i :class="card.tone"></i>
            </div>
            <strong>{{ card.value }}</strong>
            <small>{{ card.helper }}</small>
          </article>
        </div>
      </el-card>

      <div class="finance-layout">
        <el-card shadow="never" class="panel-card renew-card">
          <template #header>
            <div class="panel-head">
              <div>
                <h3>续费登记</h3>
                <p>选择学生后录入金额、课时和备注。</p>
              </div>
            </div>
          </template>

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

            <div v-if="selectedRenewStudent" class="renew-student">
              <strong>{{ selectedRenewStudent.name }}</strong>
              <span>{{ selectedRenewStudent.grade }}</span>
            </div>

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
                :rows="4"
                placeholder="如：春季续费、转账到账、家长备注等"
              />
            </el-form-item>

            <div class="renew-preview">
              <div class="renew-preview__head">
                <span>本次登记预览</span>
                <strong>{{ renewPreviewTitle }}</strong>
              </div>
              <div class="renew-preview__grid">
                <article
                  v-for="item in renewPreviewItems"
                  :key="item.label"
                  class="renew-preview__item"
                >
                  <span>{{ item.label }}</span>
                  <strong>{{ item.value }}</strong>
                </article>
              </div>
            </div>

            <div class="renew-form__actions">
              <el-button @click="resetRenewForm()">清空</el-button>
              <el-button type="primary" :loading="submitting" @click="submitRenewForm">登记收费</el-button>
            </div>
          </el-form>
        </el-card>

        <el-card shadow="never" class="panel-card record-card">
          <template #header>
            <div class="panel-head panel-head--with-filter">
              <div>
                <h3>收费记录</h3>
                <p>
                  {{ normalizedRecordKeyword
                    ? `当前搜索“${recordKeyword}”，共 ${filteredRecordGroups.length} 位学生。`
                    : `按学生聚合展示，共 ${filteredRecordGroups.length} 位学生。`
                  }}
                </p>
              </div>
              <div class="record-toolbar">
                <el-select v-model="recordSort" class="record-sort">
                  <el-option
                    v-for="option in recordSortOptions"
                    :key="option.value"
                    :label="option.label"
                    :value="option.value"
                  />
                </el-select>
                <el-input
                  v-model="recordKeyword"
                  clearable
                  placeholder="搜索学生姓名"
                  class="record-filter"
                />
              </div>
            </div>
          </template>

          <div v-if="loading" class="page-state">财务数据加载中…</div>
          <el-alert v-else-if="error" :title="error" type="error" show-icon :closable="false" />
          <div v-else-if="filteredRecordGroups.length" class="record-list">
            <article
              v-for="group in filteredRecordGroups"
              :key="group.groupKey"
              class="record-item"
            >
              <div class="record-item__top">
                <div class="student-cell">
                  <el-avatar class="student-cell__avatar">
                    {{ group.studentName?.slice(0, 1) ?? '学' }}
                  </el-avatar>
                  <div>
                    <strong>{{ group.studentName }}</strong>
                    <span>{{ group.recordCount }} 笔收费流水</span>
                  </div>
                </div>

                <el-button type="primary" plain @click="openRecordDetail(group)">详情</el-button>
              </div>

              <div class="record-item__meta">
                <span class="record-meta-item">
                  <em>总收费</em>
                  <strong>{{ formatAmount(group.totalAmount) }}</strong>
                </span>
                <span class="record-meta-item">
                  <em>总课时</em>
                  <strong>{{ group.totalLessons }} 节</strong>
                </span>
                <span class="record-time">最近收费 {{ formatTimestamp(group.lastPaidAt) }}</span>
              </div>
            </article>
          </div>
          <el-empty v-else description="暂无收费流水" :image-size="72" />
        </el-card>
      </div>
    </div>

    <el-drawer
      v-model="detailDrawerVisible"
      title="收费明细"
      size="620px"
      destroy-on-close
      class="payment-detail-drawer"
    >
      <div v-if="selectedRecordGroup" class="payment-detail">
        <div class="payment-detail__summary">
          <strong>{{ selectedRecordGroup.studentName }}</strong>
          <div class="payment-detail__stats">
            <article class="payment-detail__stat">
              <span>总收费</span>
              <strong>{{ formatAmount(selectedRecordGroup.totalAmount) }}</strong>
            </article>
            <article class="payment-detail__stat">
              <span>总课时</span>
              <strong>{{ selectedRecordGroup.totalLessons }} 节</strong>
            </article>
            <article class="payment-detail__stat">
              <span>流水笔数</span>
              <strong>{{ selectedRecordGroup.recordCount }} 笔</strong>
            </article>
          </div>
        </div>

        <div class="payment-flow-list">
          <article
            v-for="record in selectedRecordGroup.records"
            :key="record.id"
            class="payment-flow-item"
          >
            <div class="payment-flow-item__head">
              <strong>{{ formatAmount(record.tuitionPaid) }}</strong>
              <span>{{ record.lessonCount }} 节</span>
            </div>
            <div class="payment-flow-item__meta">
              <span>{{ formatTimestamp(record.paidAt) }}</span>
              <span v-if="record.remark">{{ record.remark }}</span>
              <span v-else class="payment-flow-item__meta--muted">无备注</span>
            </div>
          </article>
        </div>
      </div>
    </el-drawer>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import PageHeader from '../components/common/PageHeader.vue';
import { api } from '../services/api';
import '../assets/finance-page.css';
import {
  formatAmount,
  formatCurrency,
  formatTimestamp,
  isCurrentMonth,
  normalizeError
} from '../utils/format';

const route = useRoute();

const loading = ref(false);
const error = ref('');
const submitting = ref(false);
const students = ref([]);
const records = ref([]);
const detailDrawerVisible = ref(false);
const selectedRecordGroup = ref(null);
const recordKeyword = ref('');
const recordSort = ref('latest');
const tuitionOverview = ref({
  totalReceived: 0,
  totalConsumed: 0,
  totalPending: 0
});
const animatedSummaryAmounts = reactive({
  totalReceived: 0,
  monthIncome: 0,
  totalConsumed: 0,
  totalPending: 0
});
const amountAnimationFrames = {
  totalReceived: 0,
  monthIncome: 0,
  totalConsumed: 0,
  totalPending: 0
};
const renewForm = reactive({
  studentId: route.query.studentId ? Number(route.query.studentId) : null,
  tuitionPaid: '',
  lessonCount: '',
  remark: ''
});
const recordSortOptions = [
  { label: '最近收费优先', value: 'latest' },
  { label: '收费金额从高到低', value: 'amount-desc' },
  { label: '收费金额从低到高', value: 'amount-asc' }
];

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

const monthIncomeTotal = computed(() => {
  return records.value
    .filter((record) => isCurrentMonth(record.paidAt))
    .reduce((sum, record) => sum + Number(record.tuitionPaid ?? 0), 0);
});

const metricCards = computed(() => [
  {
    label: '总收入',
    value: formatAmount(animatedSummaryAmounts.totalReceived / 100),
    helper: '累计收费流水',
    tone: 'summary-item__dot--blue'
  },
  {
    label: '本月收入',
    value: formatAmount(animatedSummaryAmounts.monthIncome / 100),
    helper: '按本月收费时间汇总',
    tone: 'summary-item__dot--green'
  },
  {
    label: '已核销费用',
    value: formatAmount(animatedSummaryAmounts.totalConsumed / 100),
    helper: '已完成销课核销',
    tone: 'summary-item__dot--violet'
  },
  {
    label: '待核销费用',
    value: formatAmount(animatedSummaryAmounts.totalPending / 100),
    helper: '待后续销课核销',
    tone: 'summary-item__dot--amber'
  }
]);

const prefersReducedMotion = () => {
  if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') {
    return false;
  }
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches;
};

const normalizeAmountCents = (value) => {
  const amount = Number(value ?? 0);
  if (Number.isNaN(amount) || amount < 0) {
    return 0;
  }
  return Math.round(amount * 100);
};

const animateSummaryAmount = (key, targetValue) => {
  if (amountAnimationFrames[key]) {
    cancelAnimationFrame(amountAnimationFrames[key]);
    amountAnimationFrames[key] = 0;
  }

  const normalizedTarget = normalizeAmountCents(targetValue);
  if (prefersReducedMotion()) {
    animatedSummaryAmounts[key] = normalizedTarget;
    return;
  }

  const fromValue = normalizeAmountCents(animatedSummaryAmounts[key] / 100);
  if (normalizedTarget === fromValue) {
    animatedSummaryAmounts[key] = normalizedTarget;
    return;
  }

  const distance = Math.abs(normalizedTarget - fromValue);
  const direction = normalizedTarget > fromValue ? 1 : -1;
  const duration = Math.min(1500, Math.max(720, Math.sqrt(distance) * 14));
  const startTime = performance.now();
  const easeOutCubic = (progress) => 1 - Math.pow(1 - progress, 3);

  const tick = (timestamp) => {
    const progress = Math.min((timestamp - startTime) / duration, 1);
    const easedProgress = easeOutCubic(progress);
    const nextValue = fromValue + (normalizedTarget - fromValue) * easedProgress;
    const roundedValue = direction > 0 ? Math.floor(nextValue) : Math.ceil(nextValue);

    animatedSummaryAmounts[key] = direction > 0
      ? Math.min(Math.max(roundedValue, fromValue), normalizedTarget)
      : Math.max(Math.min(roundedValue, fromValue), normalizedTarget);

    if (progress < 1) {
      amountAnimationFrames[key] = requestAnimationFrame(tick);
      return;
    }

    animatedSummaryAmounts[key] = normalizedTarget;
    amountAnimationFrames[key] = 0;
  };

  amountAnimationFrames[key] = requestAnimationFrame(tick);
};

const getTimeValue = (value) => {
  if (!value) {
    return 0;
  }
  const timestamp = new Date(value).getTime();
  return Number.isNaN(timestamp) ? 0 : timestamp;
};

const compareRecordGroups = (left, right) => {
  const latestPaidAtDiff = getTimeValue(right.lastPaidAt) - getTimeValue(left.lastPaidAt);

  if (recordSort.value === 'amount-desc') {
    const amountDiff = Number(right.totalAmount ?? 0) - Number(left.totalAmount ?? 0);
    if (amountDiff !== 0) {
      return amountDiff;
    }
    return latestPaidAtDiff;
  }

  if (recordSort.value === 'amount-asc') {
    const amountDiff = Number(left.totalAmount ?? 0) - Number(right.totalAmount ?? 0);
    if (amountDiff !== 0) {
      return amountDiff;
    }
    return latestPaidAtDiff;
  }

  if (latestPaidAtDiff !== 0) {
    return latestPaidAtDiff;
  }

  return Number(right.totalAmount ?? 0) - Number(left.totalAmount ?? 0);
};

const groupedRecords = computed(() => {
  const groupMap = new Map();

  records.value.forEach((record) => {
    const groupKey = record.studentId ?? `unknown-${record.studentName ?? ''}`;
    const existing = groupMap.get(groupKey);

    if (existing) {
      existing.totalAmount += Number(record.tuitionPaid ?? 0);
      existing.totalLessons += Number(record.lessonCount ?? 0);
      existing.recordCount += 1;
      existing.records.push(record);
      if (!existing.lastPaidAt || new Date(record.paidAt).getTime() > new Date(existing.lastPaidAt).getTime()) {
        existing.lastPaidAt = record.paidAt;
      }
      return;
    }

    groupMap.set(groupKey, {
      groupKey,
      studentId: record.studentId,
      studentName: record.studentName ?? '未知学生',
      totalAmount: Number(record.tuitionPaid ?? 0),
      totalLessons: Number(record.lessonCount ?? 0),
      recordCount: 1,
      lastPaidAt: record.paidAt,
      records: [record]
    });
  });

  return Array.from(groupMap.values())
    .map((group) => ({
      ...group,
      records: group.records.slice().sort((left, right) => getTimeValue(right.paidAt) - getTimeValue(left.paidAt))
    }))
    .sort(compareRecordGroups);
});

const selectedRenewStudent = computed(() => {
  return students.value.find((student) => student.id === renewForm.studentId) ?? null;
});

const normalizedRecordKeyword = computed(() => recordKeyword.value.trim().toLowerCase());

const filteredRecordGroups = computed(() => {
  if (!normalizedRecordKeyword.value) {
    return groupedRecords.value;
  }
  return groupedRecords.value.filter((group) => {
    return String(group.studentName ?? '').toLowerCase().includes(normalizedRecordKeyword.value);
  });
});

const renewPreviewTitle = computed(() => {
  if (selectedRenewStudent.value) {
    return `${selectedRenewStudent.value.name} · ${selectedRenewStudent.value.grade}`;
  }
  return '未选择学生';
});

const renewPreviewItems = computed(() => {
  const amountValue = renewForm.tuitionPaid ? formatAmount(renewForm.tuitionPaid) : '待填写';
  const lessonValue = renewForm.lessonCount ? `${renewForm.lessonCount} 节` : '待填写';
  const amount = Number(renewForm.tuitionPaid ?? 0);
  const lessons = Number(renewForm.lessonCount ?? 0);
  const unitPriceValue = amount > 0 && lessons > 0
    ? `${formatCurrency(amount / lessons, { maximumFractionDigits: 2 })} 元/节`
    : '待计算';

  return [
    { label: '收费金额', value: amountValue },
    { label: '课时数量', value: lessonValue },
    { label: '单节均价', value: unitPriceValue }
  ];
});

const resetRenewForm = (studentId = route.query.studentId ? Number(route.query.studentId) : null) => {
  renewForm.studentId = studentId;
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
    resetRenewForm(renewForm.studentId);
    await loadFinanceData();
  } catch (requestError) {
    ElMessage.error(normalizeError(requestError, '收费登记失败'));
  } finally {
    submitting.value = false;
  }
};

const openRecordDetail = (group) => {
  selectedRecordGroup.value = group;
  detailDrawerVisible.value = true;
};

watch(
  () => route.query.studentId,
  (studentId) => {
    const value = studentId ? Number(studentId) : null;
    renewForm.studentId = value;
    if (!value) {
      recordKeyword.value = '';
      return;
    }
    const matchedStudent = students.value.find((student) => student.id === value);
    if (matchedStudent) {
      recordKeyword.value = matchedStudent.name ?? '';
    }
  },
  { immediate: true }
);

watch(students, (nextStudents) => {
  const routeStudentId = route.query.studentId ? Number(route.query.studentId) : null;
  if (!routeStudentId || recordKeyword.value) {
    return;
  }
  const matchedStudent = nextStudents.find((student) => student.id === routeStudentId);
  if (matchedStudent) {
    recordKeyword.value = matchedStudent.name ?? '';
  }
});

watch(filteredRecordGroups, (groups) => {
  if (!selectedRecordGroup.value) {
    return;
  }
  const nextGroup = groups.find((group) => group.studentId === selectedRecordGroup.value.studentId);
  if (nextGroup) {
    selectedRecordGroup.value = nextGroup;
    return;
  }
  detailDrawerVisible.value = false;
  selectedRecordGroup.value = null;
});

watch(
  () => [
    tuitionOverview.value.totalReceived,
    monthIncomeTotal.value,
    tuitionOverview.value.totalConsumed,
    tuitionOverview.value.totalPending
  ],
  ([totalReceived, monthIncome, totalConsumed, totalPending]) => {
    animateSummaryAmount('totalReceived', totalReceived);
    animateSummaryAmount('monthIncome', monthIncome);
    animateSummaryAmount('totalConsumed', totalConsumed);
    animateSummaryAmount('totalPending', totalPending);
  },
  { immediate: true }
);

onMounted(loadFinanceData);

onBeforeUnmount(() => {
  Object.keys(amountAnimationFrames).forEach((key) => {
    if (amountAnimationFrames[key]) {
      cancelAnimationFrame(amountAnimationFrames[key]);
      amountAnimationFrames[key] = 0;
    }
  });
});
</script>
