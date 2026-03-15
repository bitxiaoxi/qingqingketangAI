<template>
  <section class="page-stack">
    <PageHeader
      eyebrow="Students"
      title="学生管理"
      description="集中查看学生档案、课程规模和剩余课时，作为排课与收费的统一入口。"
    >
      <template #actions>
        <el-button type="primary" @click="openCreateDialog">新增学生</el-button>
      </template>
    </PageHeader>

    <el-card shadow="never" class="student-hero">
      <div class="student-hero__body">
        <div class="student-hero__intro">
          <span class="student-hero__eyebrow">Student Directory</span>
          <h3>标准化学生档案工作台</h3>
          <p>支持快速搜索、紧凑查看、排课跳转和收费记录联动，适合日常高频运营使用。</p>
        </div>

        <div class="student-hero__stats">
          <article v-for="stat in overviewStats" :key="stat.label" class="student-stat">
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
          <el-input v-model="filters.keyword" placeholder="输入学生姓名" clearable />
        </div>
        <div class="filter-bar__group">
          <span class="filter-bar__label">年级筛选</span>
          <el-select v-model="filters.grade" placeholder="全部年级" clearable>
            <el-option v-for="grade in grades" :key="grade" :label="grade" :value="grade" />
          </el-select>
        </div>
        <div class="filter-bar__actions">
          <el-button @click="resetFilters">重置</el-button>
        </div>
      </div>
    </el-card>

    <el-card shadow="never" class="panel-card student-table-card">
      <template #header>
        <div class="card-head">
          <div>
            <span class="card-head__eyebrow">Directory</span>
            <h3>学生列表</h3>
          </div>
          <span class="card-head__meta">当前共 {{ filteredStudents.length }} 位学生</span>
        </div>
      </template>

      <div v-if="loading" class="page-state">学生数据加载中…</div>
      <el-alert v-else-if="error" :title="error" type="error" show-icon :closable="false" />
      <el-table
        v-else-if="filteredStudents.length"
        :data="filteredStudents"
        stripe
        height="auto"
        class="student-table"
      >
        <el-table-column label="姓名" min-width="180">
          <template #default="{ row }">
            <div class="student-name-cell">
              <el-avatar class="student-name-cell__avatar">
                {{ row.name?.slice(0, 1) ?? '学' }}
              </el-avatar>
              <div>
                <strong>{{ row.name }}</strong>
                <span>录入于 {{ shortTimestamp(row.createdAt) }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="gender" label="性别" width="88" />
        <el-table-column prop="grade" label="年级" min-width="180" />
        <el-table-column prop="courseCount" label="课程数量" width="110" />
        <el-table-column label="剩余课时" width="120">
          <template #default="{ row }">
            <span class="student-badge">{{ row.remainingLessons ?? 0 }} 节</span>
          </template>
        </el-table-column>
        <el-table-column label="录入时间" min-width="170">
          <template #default="{ row }">
            {{ formatTimestamp(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="270" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button link type="primary" @click="openDetail(row)">查看</el-button>
              <el-button link @click="openEditDialog(row)">编辑</el-button>
              <el-button link @click="goToSchedule(row)">排课</el-button>
              <el-button link @click="goToFinance(row)">收费记录</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="暂无匹配的学生" :image-size="72" />
    </el-card>

    <el-dialog
      v-model="formDialogVisible"
      :title="isEditMode ? '编辑学生' : '新增学生'"
      width="640px"
      destroy-on-close
    >
      <div class="dialog-intro">
        <span class="dialog-intro__eyebrow">{{ isEditMode ? 'Edit Student' : 'Create Student' }}</span>
        <p>{{ isEditMode ? '更新学生基础资料。' : '录入学生后，可直接进入排课和收费流程。' }}</p>
      </div>

      <el-form label-position="top" class="dialog-form">
        <section class="dialog-block">
          <div class="dialog-block__head">
            <h4>基础信息</h4>
            <small>姓名、性别和年级会用于排课和试听等模块展示。</small>
          </div>

          <el-form-item label="姓名" required>
            <el-input v-model="studentForm.name" placeholder="如：李晓明" />
          </el-form-item>

          <div class="dialog-grid">
            <el-form-item label="性别" required>
              <el-select v-model="studentForm.gender">
                <el-option v-for="gender in genders" :key="gender" :label="gender" :value="gender" />
              </el-select>
            </el-form-item>

            <el-form-item label="年级" required>
              <el-select v-model="studentForm.grade">
                <el-option v-for="grade in grades" :key="grade" :label="grade" :value="grade" />
              </el-select>
            </el-form-item>
          </div>
        </section>

        <section v-if="!isEditMode" class="dialog-block">
          <div class="dialog-block__head">
            <h4>首笔收费</h4>
            <small>创建学生时可同步录入首笔学费和课时。</small>
          </div>

          <div class="dialog-grid">
            <el-form-item label="首笔学费（元）" required>
              <el-input v-model="studentForm.tuitionPaid" type="number" min="0" placeholder="12000" />
            </el-form-item>

            <el-form-item label="首笔课时（节）" required>
              <el-input v-model="studentForm.lessonCount" type="number" min="0" placeholder="40" />
            </el-form-item>
          </div>
        </section>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="formDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="submitStudentForm">
            {{ isEditMode ? '保存修改' : '创建学生' }}
          </el-button>
        </div>
      </template>
    </el-dialog>

    <el-drawer v-model="detailDrawerVisible" title="学生详情" size="460px" destroy-on-close>
      <div v-if="selectedStudent" class="student-detail">
        <div class="student-detail__hero">
          <el-avatar class="student-detail__avatar" :size="56">
            {{ selectedStudent.name?.slice(0, 1) ?? '学' }}
          </el-avatar>
          <div>
            <strong>{{ selectedStudent.name }}</strong>
            <p>{{ selectedStudent.grade }} · {{ selectedStudent.gender }}</p>
          </div>
        </div>

        <div class="student-detail__stats">
          <article class="student-detail__stat">
            <span>课程数量</span>
            <strong>{{ selectedStudent.courseCount ?? 0 }}</strong>
          </article>
          <article class="student-detail__stat">
            <span>剩余课时</span>
            <strong>{{ selectedStudent.remainingLessons ?? 0 }}</strong>
          </article>
        </div>

        <el-descriptions :column="1" border class="student-detail__descriptions">
          <el-descriptions-item label="累计学费">{{ formatAmount(selectedStudent.tuitionPaid) }}</el-descriptions-item>
          <el-descriptions-item label="平均单价">{{ formatAmount(selectedStudent.avgFeePerLesson) }}</el-descriptions-item>
          <el-descriptions-item label="录入时间">{{ formatTimestamp(selectedStudent.createdAt) }}</el-descriptions-item>
        </el-descriptions>

        <div class="student-detail__actions">
          <el-button @click="openEditDialog(selectedStudent)">编辑学生</el-button>
          <el-button type="primary" @click="goToSchedule(selectedStudent)">去排课</el-button>
          <el-button type="primary" plain @click="goToFinance(selectedStudent)">收费记录</el-button>
        </div>
      </div>
    </el-drawer>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import PageHeader from '../components/common/PageHeader.vue';
import { genders, grades } from '../constants/options';
import { api } from '../services/api';
import { formatAmount, formatTimestamp, normalizeError } from '../utils/format';

const route = useRoute();
const router = useRouter();

const loading = ref(false);
const error = ref('');
const students = ref([]);
const submitting = ref(false);
const formDialogVisible = ref(false);
const detailDrawerVisible = ref(false);
const selectedStudent = ref(null);
const formMode = ref('create');
const filters = reactive({
  keyword: '',
  grade: ''
});
const studentForm = reactive({
  id: null,
  name: '',
  gender: genders[0],
  grade: grades[0],
  tuitionPaid: '',
  lessonCount: ''
});

const isEditMode = computed(() => formMode.value === 'edit');

const overviewStats = computed(() => {
  const totalRemainingLessons = students.value.reduce((sum, student) => sum + Number(student.remainingLessons ?? 0), 0);
  const totalCourses = students.value.reduce((sum, student) => sum + Number(student.courseCount ?? 0), 0);
  return [
    {
      label: '学生总数',
      value: `${students.value.length}`,
      helper: '当前在册学生规模'
    },
    {
      label: '课程总量',
      value: `${totalCourses}`,
      helper: '全部学生已生成课程数'
    },
    {
      label: '剩余课时',
      value: `${totalRemainingLessons}`,
      helper: '全体学生尚未消耗的课时'
    }
  ];
});

const shortTimestamp = (value) => {
  return value ? formatTimestamp(value).slice(0, 10) : '--';
};

const loadStudents = async () => {
  loading.value = true;
  error.value = '';
  try {
    students.value = await api.listStudents();
  } catch (requestError) {
    error.value = normalizeError(requestError, '学生数据加载失败');
  } finally {
    loading.value = false;
  }
};

const filteredStudents = computed(() => {
  const keyword = filters.keyword.trim();
  return students.value.filter((student) => {
    const matchesKeyword = !keyword || student.name?.includes(keyword);
    const matchesGrade = !filters.grade || student.grade === filters.grade;
    return matchesKeyword && matchesGrade;
  });
});

const resetForm = () => {
  studentForm.id = null;
  studentForm.name = '';
  studentForm.gender = genders[0];
  studentForm.grade = grades[0];
  studentForm.tuitionPaid = '';
  studentForm.lessonCount = '';
};

const openCreateDialog = () => {
  formMode.value = 'create';
  resetForm();
  formDialogVisible.value = true;
};

const openEditDialog = (student) => {
  formMode.value = 'edit';
  studentForm.id = student.id;
  studentForm.name = student.name;
  studentForm.gender = student.gender;
  studentForm.grade = student.grade;
  studentForm.tuitionPaid = '';
  studentForm.lessonCount = '';
  formDialogVisible.value = true;
};

const openDetail = (student) => {
  selectedStudent.value = student;
  detailDrawerVisible.value = true;
};

const submitStudentForm = async () => {
  if (!studentForm.name.trim() || !studentForm.gender || !studentForm.grade) {
    ElMessage.error('请完整填写学生基础信息');
    return;
  }

  submitting.value = true;
  try {
    if (isEditMode.value) {
      await api.updateStudent(studentForm.id, {
        name: studentForm.name.trim(),
        gender: studentForm.gender,
        grade: studentForm.grade
      });
      ElMessage.success('学生信息已更新');
    } else {
      await api.createStudent({
        name: studentForm.name.trim(),
        gender: studentForm.gender,
        grade: studentForm.grade,
        tuitionPaid: Number(studentForm.tuitionPaid),
        lessonCount: Number(studentForm.lessonCount)
      });
      ElMessage.success('学生已创建');
    }
    formDialogVisible.value = false;
    await loadStudents();
  } catch (requestError) {
    ElMessage.error(normalizeError(requestError, isEditMode.value ? '更新学生失败' : '创建学生失败'));
  } finally {
    submitting.value = false;
  }
};

const goToSchedule = (student) => {
  router.push({
    path: '/planner',
    query: {
      action: 'create',
      studentId: student.id
    }
  });
};

const goToFinance = (student) => {
  router.push({
    path: '/finance',
    query: {
      studentId: student.id
    }
  });
};

const resetFilters = () => {
  filters.keyword = '';
  filters.grade = '';
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

onMounted(loadStudents);
</script>

<style scoped>
.student-hero,
.panel-card {
  background: var(--app-surface);
}

.student-hero__body {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(320px, 1fr);
  gap: 20px;
}

.student-hero__eyebrow,
.card-head__eyebrow,
.dialog-intro__eyebrow {
  display: inline-flex;
  margin-bottom: 8px;
  color: var(--app-text-tertiary);
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.student-hero__intro h3 {
  margin-bottom: 10px;
  font-size: 28px;
  line-height: 1.15;
}

.student-hero__intro p {
  color: var(--app-text-secondary);
  line-height: 1.7;
}

.student-hero__stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.student-stat {
  padding: 16px;
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-sm);
  background: var(--app-surface-muted);
}

.student-stat span {
  display: block;
  margin-bottom: 10px;
  color: var(--app-text-secondary);
  font-size: 13px;
}

.student-stat strong {
  display: block;
  margin-bottom: 6px;
  font-size: 28px;
  line-height: 1;
}

.student-stat small {
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

.student-table-card :deep(.el-card__body) {
  padding-top: 8px;
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

.student-table :deep(.el-table__cell) {
  padding: 10px 0;
}

.student-name-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.student-name-cell__avatar,
.student-detail__avatar {
  background: rgba(59, 130, 246, 0.14);
  color: var(--app-primary);
  font-weight: 700;
}

.student-name-cell strong {
  display: block;
  margin-bottom: 3px;
  font-size: 14px;
}

.student-name-cell span {
  color: var(--app-text-tertiary);
  font-size: 12px;
}

.student-badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 999px;
  background: var(--app-primary-soft);
  color: var(--app-primary);
  font-size: 12px;
  font-weight: 600;
}

.table-actions {
  display: flex;
  flex-wrap: wrap;
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

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.student-detail {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.student-detail__hero {
  display: flex;
  align-items: center;
  gap: 14px;
}

.student-detail__hero strong {
  display: block;
  margin-bottom: 4px;
  font-size: 22px;
}

.student-detail__hero p {
  color: var(--app-text-secondary);
}

.student-detail__stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.student-detail__stat {
  padding: 16px;
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius-sm);
  background: var(--app-surface-muted);
}

.student-detail__stat span {
  display: block;
  margin-bottom: 8px;
  color: var(--app-text-secondary);
  font-size: 13px;
}

.student-detail__stat strong {
  font-size: 24px;
}

.student-detail__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.page-state {
  color: var(--app-text-secondary);
  font-size: 14px;
}

@media (max-width: 1200px) {
  .student-hero__body,
  .student-hero__stats {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .filter-bar,
  .dialog-grid,
  .student-detail__stats {
    grid-template-columns: 1fr;
  }

  .filter-bar__actions {
    justify-content: flex-start;
  }
}
</style>
