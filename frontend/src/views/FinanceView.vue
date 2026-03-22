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

      <el-card shadow="never" class="panel-card detail-card">
        <div class="panel-head panel-head--with-filter">
          <div>
            <h3>收费记录</h3>
            <p>
              {{ normalizedRecordKeyword
                ? `当前搜索“${recordKeyword}”，共 ${filteredRecordGroups.length} 位学生。`
                : `按学生聚合展示，共 ${filteredRecordGroups.length} 位学生。`
              }}
              <span v-if="filteredRecordGroups.length"> {{ recordPaginationSummary }} </span>
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
            <el-button type="primary" @click="renewDrawerVisible = true">续费登记</el-button>
          </div>
        </div>

        <div v-if="loading" class="page-state">财务数据加载中…</div>
        <el-alert v-else-if="error" :title="error" type="error" show-icon :closable="false" />
        <el-empty v-else-if="!filteredRecordGroups.length" description="暂无收费流水" :image-size="72" />
        <div v-else class="record-list">
          <article
            v-for="group in paginatedRecordGroups"
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

          <div class="record-pagination">
            <el-pagination
              v-model:current-page="recordCurrentPage"
              v-model:page-size="recordPageSize"
              background
              layout="total, sizes, prev, pager, next"
              :page-sizes="recordPageSizeOptions"
              :total="filteredRecordGroups.length"
            />
          </div>
        </div>
      </el-card>

      <el-card shadow="never" class="panel-card detail-card write-off-card">
        <div class="panel-head write-off-card__head">
          <div>
            <h3>可核销金额</h3>
            <p>按上课学生人次汇总，已核销取实际批次单价，待核销取当前消耗批次单价。</p>
          </div>
          <div class="write-off-calendar-actions">
            <el-button plain :disabled="writeOffLoading" @click="changeWriteOffMonth(-1)">上一月</el-button>
            <el-button plain :disabled="writeOffLoading || isCurrentWriteOffMonth" @click="goToCurrentWriteOffMonth">回到本月</el-button>
            <el-button plain :disabled="writeOffLoading" @click="changeWriteOffMonth(1)">下一月</el-button>
          </div>
        </div>

        <div v-if="loading || writeOffLoading" class="page-state">可核销金额加载中…</div>
        <el-alert v-else-if="writeOffError" :title="writeOffError" type="error" show-icon :closable="false" />
        <div v-else class="write-off-calendar-section">
          <div class="write-off-calendar-summary">
            <div class="write-off-calendar-summary__month">
              <span>当前账期</span>
              <strong>{{ writeOffMonthTitle }}</strong>
            </div>
            <div class="write-off-calendar-summary__amount">
              <span>本月可核销金额</span>
              <strong>{{ formatAmount(writeOffOverview.monthAmount ?? 0) }}</strong>
            </div>
          </div>

          <div class="write-off-calendar-shell">
            <div class="write-off-calendar">
              <div class="write-off-calendar__weekdays">
                <span
                  v-for="(weekday, index) in writeOffWeekdayLabels"
                  :key="weekday"
                  :data-weekend="index >= 5 ? 'true' : 'false'"
                >
                  {{ weekday }}
                </span>
              </div>

              <div class="write-off-calendar__grid">
                <article
                  v-for="day in writeOffCalendarDays"
                  :key="day.key"
                  class="write-off-calendar__day"
                  :data-current-month="day.isCurrentMonth ? 'true' : 'false'"
                  :data-today="day.isToday ? 'true' : 'false'"
                  :data-selected="day.isSelected ? 'true' : 'false'"
                  :data-weekend="day.isWeekend ? 'true' : 'false'"
                  :data-has-amount="day.isCurrentMonth && day.amount > 0 ? 'true' : 'false'"
                  :data-empty="day.isCurrentMonth && day.amount <= 0 ? 'true' : 'false'"
                >
                  <div class="write-off-calendar__day-head">
                    <span>{{ day.dayNumber }}</span>
                    <em v-if="day.isToday">今天</em>
                  </div>

                  <strong
                    v-if="day.isCurrentMonth && day.amount > 0"
                    class="write-off-calendar__amount"
                  >
                    {{ formatWriteOffCalendarAmount(day.amount) }}
                  </strong>
                  <small v-else-if="day.isCurrentMonth" class="write-off-calendar__empty">暂无可核销</small>
                </article>
              </div>
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <el-drawer
      v-model="renewDrawerVisible"
      title="续费登记"
      size="520px"
      destroy-on-close
      class="finance-action-drawer"
    >
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
    </el-drawer>

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
  normalizeError
} from '../utils/format';

const route = useRoute();

const loading = ref(false);
const error = ref('');
const writeOffLoading = ref(true);
const writeOffError = ref('');
const submitting = ref(false);
const students = ref([]);
const records = ref([]);
const writeOffMonth = ref(new Date(new Date().getFullYear(), new Date().getMonth(), 1));
const selectedWriteOffDateKey = ref('');
const renewDrawerVisible = ref(false);
const detailDrawerVisible = ref(false);
const selectedRecordGroup = ref(null);
const recordKeyword = ref('');
const recordSort = ref('latest');
const recordCurrentPage = ref(1);
const recordPageSize = ref(5);
const tuitionOverview = ref({
  totalReceived: 0,
  totalConsumed: 0,
  totalPending: 0
});
const writeOffOverview = ref({
  month: '',
  monthAmount: 0,
  monthStudentLessonCount: 0,
  dailyAmounts: []
});
const animatedSummaryAmounts = reactive({
  totalReceived: 0,
  totalConsumed: 0,
  totalPending: 0
});
const amountAnimationFrames = {
  totalReceived: 0,
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
const recordPageSizeOptions = [5, 10, 20, 50];
const writeOffWeekdayLabels = ['周一', '周二', '周三', '周四', '周五', '周六', '周日'];

const getMonthStart = (value) => {
  const date = new Date(value);
  return new Date(date.getFullYear(), date.getMonth(), 1);
};

const formatWriteOffMonthParam = (value) => {
  const date = getMonthStart(value);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
};

const getWriteOffDateKey = (value) => {
  const date = new Date(value);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
};

const getWriteOffWeekdayIndex = (value) => {
  const day = new Date(value).getDay();
  return day === 0 ? 6 : day - 1;
};

const isSameCalendarDay = (left, right) => {
  return getWriteOffDateKey(left) === getWriteOffDateKey(right);
};

const createEmptyWriteOffOverview = (monthDate = writeOffMonth.value) => ({
  month: formatWriteOffMonthParam(monthDate),
  monthAmount: 0,
  monthStudentLessonCount: 0,
  dailyAmounts: []
});

const loadWriteOffOverview = async (monthDate = writeOffMonth.value) => {
  const targetMonth = getMonthStart(monthDate);
  writeOffMonth.value = targetMonth;
  writeOffLoading.value = true;
  writeOffError.value = '';
  try {
    const overviewData = await api.getWriteOffOverview(formatWriteOffMonthParam(targetMonth));
    writeOffOverview.value = {
      ...createEmptyWriteOffOverview(targetMonth),
      ...(overviewData ?? {})
    };
  } catch (requestError) {
    writeOffError.value = normalizeError(requestError, '可核销金额加载失败');
  } finally {
    writeOffLoading.value = false;
  }
};

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
  await loadWriteOffOverview(writeOffMonth.value);
};

const metricCards = computed(() => [
  {
    label: '总收入',
    value: formatAmount(animatedSummaryAmounts.totalReceived / 100),
    helper: '累计收费流水',
    tone: 'summary-item__dot--blue'
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

const writeOffMonthTitle = computed(() => {
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: 'long'
  }).format(writeOffMonth.value);
});

const isCurrentWriteOffMonth = computed(() => {
  const currentMonth = getMonthStart(new Date());
  return currentMonth.getFullYear() === writeOffMonth.value.getFullYear()
    && currentMonth.getMonth() === writeOffMonth.value.getMonth();
});

const writeOffDailyAmountMap = computed(() => {
  return new Map(
    (writeOffOverview.value.dailyAmounts ?? []).map((item) => [getWriteOffDateKey(item.date), item])
  );
});

const writeOffCalendarDays = computed(() => {
  const monthStart = getMonthStart(writeOffMonth.value);
  const calendarStart = new Date(monthStart);
  const firstWeekday = (monthStart.getDay() + 6) % 7;
  calendarStart.setDate(monthStart.getDate() - firstWeekday);
  const today = new Date();

  return Array.from({ length: 42 }, (_, index) => {
    const date = new Date(calendarStart);
    date.setDate(calendarStart.getDate() + index);
    const key = getWriteOffDateKey(date);
    const dayAmount = writeOffDailyAmountMap.value.get(key);
    const amount = Number(dayAmount?.amount ?? 0);
    const studentLessonCount = Number(dayAmount?.studentLessonCount ?? 0);
    const weekdayIndex = getWriteOffWeekdayIndex(date);
    const isCurrentMonth = date.getFullYear() === monthStart.getFullYear() && date.getMonth() === monthStart.getMonth();

    return {
      key,
      dayNumber: date.getDate(),
      isCurrentMonth,
      isToday: isSameCalendarDay(date, today),
      isSelected: key === selectedWriteOffDateKey.value,
      isWeekend: weekdayIndex >= 5,
      amount: Number.isFinite(amount) ? amount : 0,
      studentLessonCount: Number.isFinite(studentLessonCount) ? studentLessonCount : 0
    };
  });
});

const formatWriteOffCalendarAmount = (value) => {
  return `${formatCurrency(value, { maximumFractionDigits: 2 })} 元`;
};

const changeWriteOffMonth = async (offset) => {
  const nextMonth = getMonthStart(writeOffMonth.value);
  nextMonth.setMonth(nextMonth.getMonth() + offset);
  await loadWriteOffOverview(nextMonth);
};

const goToCurrentWriteOffMonth = async () => {
  if (isCurrentWriteOffMonth.value) {
    return;
  }
  await loadWriteOffOverview(new Date());
};

const resolveDefaultSelectedWriteOffDateKey = (days) => {
  const currentMonthDays = days.filter((day) => day.isCurrentMonth);
  if (!currentMonthDays.length) {
    return '';
  }
  const existingSelectedDay = currentMonthDays.find((day) => day.key === selectedWriteOffDateKey.value);
  if (existingSelectedDay) {
    return existingSelectedDay.key;
  }
  const firstAmountDay = currentMonthDays.find((day) => day.amount > 0);
  if (firstAmountDay) {
    return firstAmountDay.key;
  }
  const todayDay = currentMonthDays.find((day) => day.isToday);
  if (todayDay) {
    return todayDay.key;
  }
  return currentMonthDays[0].key;
};

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

const recordTotalPages = computed(() => {
  return Math.max(1, Math.ceil(filteredRecordGroups.value.length / recordPageSize.value));
});

const paginatedRecordGroups = computed(() => {
  const startIndex = (recordCurrentPage.value - 1) * recordPageSize.value;
  return filteredRecordGroups.value.slice(startIndex, startIndex + recordPageSize.value);
});

const recordPaginationSummary = computed(() => {
  if (!filteredRecordGroups.value.length) {
    return '';
  }
  return `当前第 ${recordCurrentPage.value}/${recordTotalPages.value} 页，每页 ${recordPageSize.value} 位学生。`;
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
    renewDrawerVisible.value = false;
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

watch(renewDrawerVisible, (visible) => {
  if (!visible && !submitting.value) {
    resetRenewForm();
  }
});

watch([recordKeyword, recordSort], () => {
  recordCurrentPage.value = 1;
});

watch(recordPageSize, () => {
  recordCurrentPage.value = 1;
});

watch(filteredRecordGroups, (groups) => {
  const totalPages = Math.max(1, Math.ceil(groups.length / recordPageSize.value));
  if (recordCurrentPage.value > totalPages) {
    recordCurrentPage.value = totalPages;
  }
});

watch(filteredRecordGroups, (groups) => {
  if (!selectedRecordGroup.value) {
    return;
  }
  const nextGroup = groups.find((group) => group.groupKey === selectedRecordGroup.value.groupKey);
  if (nextGroup) {
    selectedRecordGroup.value = nextGroup;
    return;
  }
  detailDrawerVisible.value = false;
  selectedRecordGroup.value = null;
});

watch(
  writeOffCalendarDays,
  (days) => {
    const nextSelectedDateKey = resolveDefaultSelectedWriteOffDateKey(days);
    if (nextSelectedDateKey !== selectedWriteOffDateKey.value) {
      selectedWriteOffDateKey.value = nextSelectedDateKey;
    }
  },
  { immediate: true }
);

watch(
  () => [
    tuitionOverview.value.totalReceived,
    tuitionOverview.value.totalConsumed,
    tuitionOverview.value.totalPending
  ],
  ([totalReceived, totalConsumed, totalPending]) => {
    animateSummaryAmount('totalReceived', totalReceived);
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
