<template>
  <section class="page-stack referral-page">
    <PageHeader
      title="转介绍管理"
      description="绑定学生之间的转介绍关系，系统会自动给介绍人赠送 1 节课并写入课时流水。"
    />

    <div class="referral-stack">
      <el-card shadow="never" class="panel-card referral-summary-card">
        <div class="referral-summary-grid">
          <article v-for="card in summaryCards" :key="card.label" class="referral-summary-item">
            <span>{{ card.label }}</span>
            <strong>{{ card.value }}</strong>
            <small>{{ card.helper }}</small>
          </article>
        </div>
      </el-card>

      <el-card shadow="never" class="panel-card referral-bind-card">
        <div class="panel-head referral-bind-card__head">
          <div>
            <h3>绑定转介绍</h3>
            <p>选择介绍人与被介绍人，保存后会自动为介绍人新增一笔 0 元 1 节的奖励课时。</p>
          </div>
          <el-tag type="success" effect="plain" round>自动赠送 1 节课</el-tag>
        </div>

        <div v-if="loading" class="page-state">转介绍数据加载中…</div>
        <el-alert v-else-if="error" :title="error" type="error" show-icon :closable="false" />
        <el-form v-else label-position="top" class="referral-form">
          <div class="referral-form__grid">
            <el-form-item label="介绍人" required>
              <el-select
                v-model="referralForm.referrerStudentId"
                filterable
                clearable
                placeholder="请选择介绍人"
              >
                <el-option
                  v-for="student in referrerOptions"
                  :key="student.id"
                  :label="`${student.name} · ${student.grade}`"
                  :value="student.id"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="被介绍人" required>
              <el-select
                v-model="referralForm.referredStudentId"
                filterable
                clearable
                placeholder="请选择被介绍人"
              >
                <el-option
                  v-for="student in referredOptions"
                  :key="student.id"
                  :label="buildReferredOptionLabel(student)"
                  :value="student.id"
                  :disabled="student.disabled"
                />
              </el-select>
            </el-form-item>
          </div>

          <div class="referral-preview">
            <div class="referral-preview__head">
              <span>奖励预览</span>
              <strong>{{ referralPreviewTitle }}</strong>
            </div>
            <p>{{ referralPreviewDescription }}</p>
          </div>

          <el-form-item label="备注">
            <el-input
              v-model="referralForm.remark"
              type="textarea"
              :rows="3"
              placeholder="如：家长转介绍、同班同学推荐等"
            />
          </el-form-item>

          <div class="referral-form__actions">
            <el-button @click="resetReferralForm">清空</el-button>
            <el-button type="primary" :loading="submitting" @click="submitReferralForm">保存绑定</el-button>
          </div>
        </el-form>
      </el-card>

      <el-card shadow="never" class="panel-card referral-record-card">
        <div class="panel-head panel-head--with-filter">
          <div>
            <h3>转介绍关系</h3>
            <p>
              {{ normalizedKeyword
                ? `当前搜索“${keyword}”，共 ${filteredReferrals.length} 条关系。`
                : `共 ${filteredReferrals.length} 条已绑定关系。`
              }}
            </p>
          </div>
          <el-input
            v-model="keyword"
            clearable
            placeholder="搜索介绍人或被介绍人"
            class="referral-record-card__filter"
          />
        </div>

        <div v-if="loading" class="page-state">转介绍关系加载中…</div>
        <el-alert v-else-if="error" :title="error" type="error" show-icon :closable="false" />
        <el-empty v-else-if="!filteredReferrals.length" description="暂无转介绍关系" :image-size="72" />
        <div v-else class="referral-record-list">
          <article
            v-for="referral in filteredReferrals"
            :key="referral.id"
            class="referral-record-item"
          >
            <div class="referral-record-item__top">
              <div class="referral-record-item__title">
                <strong>{{ referral.referrerStudentName }}</strong>
                <span>介绍了 {{ referral.referredStudentName }}</span>
              </div>
              <el-tag type="success" effect="plain">赠送 {{ referral.rewardLessonCount }} 节</el-tag>
            </div>

            <div class="referral-record-item__meta">
              <span>介绍人：{{ referral.referrerStudentGrade || '未填写年级' }}</span>
              <span>被介绍人：{{ referral.referredStudentGrade || '未填写年级' }}</span>
              <span>奖励时间：{{ formatTimestamp(referral.rewardedAt) }}</span>
            </div>

            <div class="referral-record-item__foot">
              <span v-if="referral.remark">{{ referral.remark }}</span>
              <span v-else class="referral-record-item__muted">无备注</span>
              <small>奖励流水 #{{ referral.rewardPaymentId }}</small>
            </div>
          </article>
        </div>
      </el-card>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import PageHeader from '../components/common/PageHeader.vue';
import { api } from '../services/api';
import '../assets/referral-page.css';
import { formatTimestamp, normalizeError } from '../utils/format';

const loading = ref(false);
const error = ref('');
const submitting = ref(false);
const students = ref([]);
const referrals = ref([]);
const keyword = ref('');
const referralForm = reactive({
  referrerStudentId: null,
  referredStudentId: null,
  remark: ''
});

const loadReferralData = async () => {
  loading.value = true;
  error.value = '';
  try {
    const [studentsData, referralsData] = await Promise.all([
      api.listStudents(),
      api.listReferrals()
    ]);
    students.value = studentsData ?? [];
    referrals.value = referralsData ?? [];
  } catch (requestError) {
    error.value = normalizeError(requestError, '转介绍数据加载失败');
  } finally {
    loading.value = false;
  }
};

const normalizedKeyword = computed(() => keyword.value.trim().toLowerCase());

const referredStudentIdSet = computed(() => {
  return new Set((referrals.value ?? []).map((item) => item.referredStudentId));
});

const referrerOptions = computed(() => {
  return (students.value ?? []).filter((student) => student.id !== referralForm.referredStudentId);
});

const referredOptions = computed(() => {
  return (students.value ?? []).map((student) => ({
    ...student,
    disabled: student.id === referralForm.referrerStudentId || referredStudentIdSet.value.has(student.id)
  }));
});

const selectedReferrer = computed(() => {
  return (students.value ?? []).find((student) => student.id === referralForm.referrerStudentId) ?? null;
});

const selectedReferred = computed(() => {
  return (students.value ?? []).find((student) => student.id === referralForm.referredStudentId) ?? null;
});

const summaryCards = computed(() => {
  const referralCount = referrals.value.length;
  const rewardLessonCount = referrals.value.reduce((total, item) => total + Number(item.rewardLessonCount ?? 0), 0);
  const referrerCount = new Set(referrals.value.map((item) => item.referrerStudentId)).size;
  return [
    {
      label: '已绑定关系',
      value: `${referralCount} 条`,
      helper: '已创建的转介绍记录'
    },
    {
      label: '已奖励课时',
      value: `${rewardLessonCount} 节`,
      helper: '系统自动写入奖励课时流水'
    },
    {
      label: '介绍学生数',
      value: `${referrerCount} 位`,
      helper: '至少成功介绍过 1 位学生'
    }
  ];
});

const filteredReferrals = computed(() => {
  const records = referrals.value ?? [];
  if (!normalizedKeyword.value) {
    return records;
  }
  return records.filter((item) => {
    const haystacks = [
      item.referrerStudentName,
      item.referredStudentName,
      item.remark
    ]
      .filter(Boolean)
      .join(' ')
      .toLowerCase();
    return haystacks.includes(normalizedKeyword.value);
  });
});

const referralPreviewTitle = computed(() => {
  if (!selectedReferrer.value) {
    return '介绍人将获得 1 节赠课';
  }
  return `${selectedReferrer.value.name} 将获得 1 节赠课`;
});

const referralPreviewDescription = computed(() => {
  if (selectedReferrer.value && selectedReferred.value) {
    return `保存后会把 ${selectedReferred.value.name} 绑定为 ${selectedReferrer.value.name} 的转介绍学生，并自动新增奖励课时流水。`;
  }
  return '绑定完成后，系统会自动写入一笔 0 元 1 节的奖励课时流水，现有课时和销课统计会立即生效。';
});

const buildReferredOptionLabel = (student) => {
  if (referredStudentIdSet.value.has(student.id)) {
    return `${student.name} · ${student.grade}（已绑定）`;
  }
  return `${student.name} · ${student.grade}`;
};

const resetReferralForm = () => {
  referralForm.referrerStudentId = null;
  referralForm.referredStudentId = null;
  referralForm.remark = '';
};

const submitReferralForm = async () => {
  if (!referralForm.referrerStudentId) {
    ElMessage.warning('请选择介绍人');
    return;
  }
  if (!referralForm.referredStudentId) {
    ElMessage.warning('请选择被介绍人');
    return;
  }
  if (referralForm.referrerStudentId === referralForm.referredStudentId) {
    ElMessage.warning('介绍人和被介绍人不能是同一位学生');
    return;
  }

  submitting.value = true;
  try {
    const referral = await api.createReferral({
      referrerStudentId: referralForm.referrerStudentId,
      referredStudentId: referralForm.referredStudentId,
      remark: referralForm.remark
    });
    ElMessage.success(`绑定成功，已向 ${referral.referrerStudentName} 赠送 1 节课`);
    resetReferralForm();
    await loadReferralData();
  } catch (requestError) {
    ElMessage.error(normalizeError(requestError, '转介绍绑定失败'));
  } finally {
    submitting.value = false;
  }
};

onMounted(() => {
  loadReferralData();
});
</script>
