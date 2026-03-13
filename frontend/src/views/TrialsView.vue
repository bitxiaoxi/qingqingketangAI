<template>
  <section class="page-stack">
    <PageHeader
      eyebrow="Trial"
      title="试听管理"
      description="集中管理试听预约、试听状态和转化结果，适合培训机构日常运营跟进。"
    >
      <template #actions>
        <el-button type="primary" @click="openCreateDialog">新增试听</el-button>
      </template>
    </PageHeader>

    <el-card shadow="never" class="trial-hero">
      <div class="trial-hero__body">
        <div class="trial-hero__intro">
          <span class="trial-hero__eyebrow">Trial Pipeline</span>
          <h3>试听线索与转化跟进</h3>
          <p>把试听预约、试听执行、报名结果放在同一个清晰页面中，便于快速跟踪与更新。</p>
        </div>

        <div class="trial-hero__stats">
          <article v-for="stat in overviewStats" :key="stat.label" class="trial-stat">
            <span>{{ stat.label }}</span>
            <strong>{{ stat.value }}</strong>
            <small>{{ stat.helper }}</small>
          </article>
        </div>
      </div>
    </el-card>

    <el-card shadow="never" class="panel-card">
      <div class="filter-bar">
        <div class="filter-bar__group">
          <span class="filter-bar__label">姓名搜索</span>
          <el-input v-model="filters.keyword" placeholder="输入试听学生姓名" clearable />
        </div>

        <div class="filter-bar__group">
          <span class="filter-bar__label">试听状态</span>
          <el-select v-model="filters.status" placeholder="全部状态" clearable>
            <el-option
              v-for="status in trialStatusOptions"
              :key="status.value"
              :label="status.label"
              :value="status.value"
            />
          </el-select>
        </div>

        <div class="filter-bar__actions">
          <el-button @click="resetFilters">重置</el-button>
        </div>
      </div>
    </el-card>

    <el-card shadow="never" class="panel-card trial-table-card">
      <template #header>
        <div class="card-head">
          <div>
            <span class="card-head__eyebrow">Records</span>
            <h3>试听列表</h3>
          </div>
          <span class="card-head__meta">当前共 {{ filteredTrials.length }} 条试听记录</span>
        </div>
      </template>

      <div v-if="loading" class="page-state">试听数据加载中…</div>
      <el-alert v-else-if="error" :title="error" type="error" show-icon :closable="false" />
      <el-table
        v-else-if="filteredTrials.length"
        :data="filteredTrials"
        stripe
        class="trial-table"
      >
        <el-table-column label="姓名" min-width="160">
          <template #default="{ row }">
            <div class="trial-name-cell">
              <el-avatar class="trial-name-cell__avatar">
                {{ row.name?.slice(0, 1) ?? '试' }}
              </el-avatar>
              <div>
                <strong>{{ row.name }}</strong>
                <span>{{ row.grade }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="grade" label="年级" min-width="160" />
        <el-table-column label="试听时间" min-width="180">
          <template #default="{ row }">
            {{ formatTimestamp(row.trialTime) }}
          </template>
        </el-table-column>
        <el-table-column label="试听状态" width="130">
          <template #default="{ row }">
            <el-tag :type="statusMetaMap[normalizedStatus(row.status)]?.type ?? 'info'" effect="plain">
              {{ statusMetaMap[normalizedStatus(row.status)]?.label ?? row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="note" label="备注" min-width="220" show-overflow-tooltip />
        <el-table-column label="操作" min-width="180" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
              <el-button link @click="quickUpdateStatus(row, nextStatus(row.status))">推进状态</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="暂无试听记录" :image-size="72" />
    </el-card>

    <el-dialog
      v-model="formDialogVisible"
      :title="isEditMode ? '编辑试听记录' : '新增试听记录'"
      width="660px"
      destroy-on-close
    >
      <div class="dialog-intro">
        <span class="dialog-intro__eyebrow">{{ isEditMode ? 'Edit Trial' : 'Create Trial' }}</span>
        <p>{{ isEditMode ? '更新试听时间、状态与备注。' : '录入试听学生后，可持续更新试听和报名状态。' }}</p>
      </div>

      <el-form label-position="top" class="dialog-form">
        <section class="dialog-block">
          <div class="dialog-block__head">
            <h4>试听基础信息</h4>
            <small>记录学生姓名、年级和试听预约时间。</small>
          </div>

          <el-form-item label="姓名" required>
            <el-input v-model="trialForm.name" placeholder="如：王同学" />
          </el-form-item>

          <div class="dialog-grid">
            <el-form-item label="年级" required>
              <el-select v-model="trialForm.grade">
                <el-option v-for="grade in grades" :key="grade" :label="grade" :value="grade" />
              </el-select>
            </el-form-item>

            <el-form-item label="试听时间" required>
              <el-date-picker
                v-model="trialForm.trialTime"
                type="datetime"
                format="YYYY-MM-DD HH:mm"
                value-format="YYYY-MM-DDTHH:mm:ss"
                placeholder="选择试听时间"
              />
            </el-form-item>
          </div>
        </section>

        <section class="dialog-block">
          <div class="dialog-block__head">
            <h4>试听状态</h4>
            <small>使用统一状态跟进试听结果和报名情况。</small>
          </div>

          <div class="status-chip-grid">
            <button
              v-for="status in trialStatusOptions"
              :key="status.value"
              type="button"
              class="status-chip"
              :data-active="trialForm.status === status.value"
              @click="trialForm.status = status.value"
            >
              <strong>{{ status.label }}</strong>
              <span>{{ statusDescriptionMap[status.value] }}</span>
            </button>
          </div>

          <el-form-item label="备注" class="dialog-note-field">
            <el-input
              v-model="trialForm.note"
              type="textarea"
              :rows="3"
              placeholder="可填写试听反馈、家长意向、未报名原因等"
            />
          </el-form-item>
        </section>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="formDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="submitTrialForm">
            {{ isEditMode ? '保存修改' : '创建试听' }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import PageHeader from '../components/common/PageHeader.vue';
import { grades, trialStatusOptions } from '../constants/options';
import { api } from '../services/api';
import { formatTimestamp, normalizeError } from '../utils/format';

const route = useRoute();

const loading = ref(false);
const error = ref('');
const trials = ref([]);
const submitting = ref(false);
const formDialogVisible = ref(false);
const formMode = ref('create');
const filters = reactive({
  keyword: '',
  status: ''
});
const trialForm = reactive({
  id: null,
  name: '',
  grade: grades[0],
  trialTime: '',
  status: 'PENDING',
  note: ''
});

const statusMetaMap = trialStatusOptions.reduce((result, item) => {
  result[item.value] = item;
  return result;
}, {});

const statusDescriptionMap = {
  PENDING: '等待试听到课',
  COMPLETED: '试听已完成',
  ENROLLED: '试听后已报名',
  LOST: '试听后未报名'
};

const normalizedStatus = (status) => String(status ?? 'PENDING').toUpperCase();

const isEditMode = computed(() => formMode.value === 'edit');

const overviewStats = computed(() => {
  const pending = trials.value.filter((trial) => normalizedStatus(trial.status) === 'PENDING').length;
  const completed = trials.value.filter((trial) => normalizedStatus(trial.status) === 'COMPLETED').length;
  const enrolled = trials.value.filter((trial) => normalizedStatus(trial.status) === 'ENROLLED').length;
  return [
    {
      label: '试听总数',
      value: `${trials.value.length}`,
      helper: '当前所有试听记录'
    },
    {
      label: '待试听',
      value: `${pending}`,
      helper: '待执行试听安排'
    },
    {
      label: '已试听',
      value: `${completed}`,
      helper: '试听已完成待转化'
    },
    {
      label: '已报名',
      value: `${enrolled}`,
      helper: '试听后成功转化'
    }
  ];
});

const loadTrials = async () => {
  loading.value = true;
  error.value = '';
  try {
    trials.value = await api.listTrials();
  } catch (requestError) {
    error.value = normalizeError(requestError, '试听数据加载失败');
  } finally {
    loading.value = false;
  }
};

const filteredTrials = computed(() => {
  const keyword = filters.keyword.trim();
  return trials.value.filter((trial) => {
    const currentStatus = normalizedStatus(trial.status);
    const matchesKeyword = !keyword || trial.name?.includes(keyword);
    const matchesStatus = !filters.status || currentStatus === filters.status;
    return matchesKeyword && matchesStatus;
  });
});

const resetForm = () => {
  trialForm.id = null;
  trialForm.name = '';
  trialForm.grade = grades[0];
  trialForm.trialTime = '';
  trialForm.status = 'PENDING';
  trialForm.note = '';
};

const openCreateDialog = () => {
  formMode.value = 'create';
  resetForm();
  formDialogVisible.value = true;
};

const openEditDialog = (trial) => {
  formMode.value = 'edit';
  trialForm.id = trial.id;
  trialForm.name = trial.name;
  trialForm.grade = trial.grade;
  trialForm.trialTime = trial.trialTime;
  trialForm.status = normalizedStatus(trial.status);
  trialForm.note = trial.note ?? '';
  formDialogVisible.value = true;
};

const submitTrialForm = async () => {
  if (!trialForm.name.trim() || !trialForm.grade || !trialForm.trialTime) {
    ElMessage.error('请完整填写试听信息');
    return;
  }

  submitting.value = true;
  try {
    const payload = {
      name: trialForm.name.trim(),
      grade: trialForm.grade,
      trialTime: trialForm.trialTime,
      status: trialForm.status,
      note: trialForm.note?.trim() || ''
    };
    if (isEditMode.value) {
      await api.updateTrial(trialForm.id, payload);
      ElMessage.success('试听记录已更新');
    } else {
      await api.createTrial(payload);
      ElMessage.success('试听记录已创建');
    }
    formDialogVisible.value = false;
    await loadTrials();
  } catch (requestError) {
    ElMessage.error(normalizeError(requestError, isEditMode.value ? '更新试听失败' : '创建试听失败'));
  } finally {
    submitting.value = false;
  }
};

const nextStatus = (status) => {
  const current = normalizedStatus(status);
  if (current === 'PENDING') return 'COMPLETED';
  if (current === 'COMPLETED') return 'ENROLLED';
  return current;
};

const quickUpdateStatus = async (trial, status) => {
  if (!status || normalizedStatus(status) === normalizedStatus(trial.status)) {
    return;
  }
  try {
    await api.updateTrial(trial.id, {
      name: trial.name,
      grade: trial.grade,
      trialTime: trial.trialTime,
      status,
      note: trial.note ?? ''
    });
    ElMessage.success(`试听状态已更新为${statusMetaMap[status]?.label ?? status}`);
    await loadTrials();
  } catch (requestError) {
    ElMessage.error(normalizeError(requestError, '更新试听状态失败'));
  }
};

const resetFilters = () => {
  filters.keyword = '';
  filters.status = '';
};

watch(
  () => route.query.action,
  (action) => {
    if (action === 'create') {
      openCreateDialog();
    }
  },
  { immediate: true }
);

onMounted(loadTrials);
</script>

<style scoped>
.trial-hero,
.panel-card {
  background: var(--app-surface);
}

.trial-hero__body {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(360px, 1fr);
  gap: 20px;
}

.trial-hero__eyebrow,
.card-head__eyebrow,
.dialog-intro__eyebrow {
  display: inline-flex;
  margin-bottom: 8px;
  color: var(--app-text-tertiary);
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.trial-hero__intro h3 {
  margin-bottom: 10px;
  font-size: 28px;
  line-height: 1.15;
}

.trial-hero__intro p {
  color: var(--app-text-secondary);
  line-height: 1.7;
}

.trial-hero__stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.trial-stat {
  padding: 16px;
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-sm);
  background: var(--app-surface-muted);
}

.trial-stat span {
  display: block;
  margin-bottom: 10px;
  color: var(--app-text-secondary);
  font-size: 13px;
}

.trial-stat strong {
  display: block;
  margin-bottom: 6px;
  font-size: 28px;
  line-height: 1;
}

.trial-stat small {
  color: var(--app-text-tertiary);
  font-size: 12px;
  line-height: 1.6;
}

.filter-bar {
  display: grid;
  grid-template-columns: minmax(240px, 1fr) minmax(220px, 260px) auto;
  gap: 16px;
  align-items: end;
}

.filter-bar__group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.filter-bar__label {
  color: var(--app-text-secondary);
  font-size: 13px;
}

.filter-bar__actions {
  display: flex;
  justify-content: flex-end;
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

.card-head__meta {
  color: var(--app-text-secondary);
  font-size: 13px;
}

.trial-table :deep(.el-table__cell) {
  padding: 10px 0;
}

.trial-name-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.trial-name-cell__avatar {
  background: rgba(59, 130, 246, 0.14);
  color: var(--app-primary);
  font-weight: 700;
}

.trial-name-cell strong {
  display: block;
  margin-bottom: 3px;
  font-size: 14px;
}

.trial-name-cell span {
  color: var(--app-text-tertiary);
  font-size: 12px;
}

.table-actions {
  display: flex;
  gap: 4px;
}

.dialog-intro {
  margin-bottom: 20px;
  padding: 14px 16px;
  border: 1px solid rgba(59, 130, 246, 0.12);
  border-radius: var(--app-radius-sm);
  background: var(--app-primary-soft);
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

.status-chip-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.status-chip {
  padding: 14px;
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-sm);
  background: var(--app-surface);
  text-align: left;
  cursor: pointer;
  transition: border-color 180ms ease, background 180ms ease, transform 180ms ease;
}

.status-chip[data-active='true'] {
  border-color: rgba(59, 130, 246, 0.34);
  background: rgba(59, 130, 246, 0.08);
}

.status-chip:hover {
  transform: translateY(-1px);
}

.status-chip strong {
  display: block;
  margin-bottom: 4px;
  font-size: 14px;
}

.status-chip span {
  color: var(--app-text-secondary);
  font-size: 12px;
  line-height: 1.6;
}

.dialog-note-field {
  margin-top: 14px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.page-state {
  color: var(--app-text-secondary);
  font-size: 14px;
}

@media (max-width: 1200px) {
  .trial-hero__body {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .filter-bar,
  .dialog-grid,
  .status-chip-grid,
  .trial-hero__stats {
    grid-template-columns: 1fr;
  }

  .filter-bar__actions {
    justify-content: flex-start;
  }
}
</style>
