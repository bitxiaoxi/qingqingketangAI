<template>
  <div class="app-shell">
    <header class="top-bar">
      <div class="brand-head">
        <div class="logo-mark">
          <img :src="logo" alt="青青课堂 logo" />
        </div>
        <div>
          <p class="eyebrow">青青课堂 · Qingqing Academy</p>
          <h1>后台运营总览</h1>
        </div>
      </div>
    </header>

    <section class="today-section">
      <article class="panel today-card">
        <div class="panel__header">
          <div>
            <h2>今日安排</h2>
            <small>关注今日关键事项</small>
          </div>
          <span class="today-date">{{ todayLabel }}</span>
        </div>
        <ul class="today-list">
          <li v-for="item in todayPlan" :key="item.time + item.title">
            <div class="today-node">
              <p class="today-time">{{ item.time }}</p>
              <p class="today-title">{{ item.title }}</p>
              <small>{{ item.owner }} · {{ item.location }}</small>
            </div>
            <span class="tag" :data-state="item.state">{{ item.stateLabel }}</span>
          </li>
        </ul>
      </article>
    </section>

    <section class="modules">
      <div class="module-stack">
        <article class="panel students">
        <div class="panel__header">
          <div>
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
        <table v-else-if="students.length">
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
        <p v-else class="empty-state">暂未录入学生，填写上方表单即可创建首位学生。</p>
        </article>
      </div>

      <div class="module-grid">
        <article class="panel schedule">
          <div class="panel__header schedule-header">
            <div>
              <h2>课程管理</h2>
              <small>排课与查看课表，一站完成</small>
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
            <p class="panel__helper">选择学生后，输入首节日期与时间段，系统将根据剩余课时自动生成课表</p>
            <form class="student-form arrange-form" @submit.prevent="submitArrangeDraft">
              <div class="form-row">
                <label class="form-field">
                  <span>学生</span>
                  <select v-model="arrangeDraft.studentId" required>
                    <option value="">请选择学生</option>
                    <option v-for="student in students" :key="student.id" :value="student.id">
                      {{ student.name }} · {{ student.grade }}
                    </option>
                  </select>
                </label>
                <label class="form-field">
                  <span>周几</span>
                  <select v-model="arrangeDraft.weekday" required>
                    <option v-for="option in weekdayOptions" :key="option.value" :value="option.value">
                      {{ option.label }}
                    </option>
                  </select>
                </label>
                <label class="form-field">
                  <span>第一节日期</span>
                  <input v-model="arrangeDraft.firstDate" type="date" required />
                </label>
                <label class="form-field">
                  <span>开始时间</span>
                  <input v-model="arrangeDraft.startTime" type="time" required />
                </label>
                <label class="form-field">
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
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="slot in weekScheduleRows" :key="slot.id">
                    <td>{{ slot.weekday }}</td>
                    <td>{{ slot.subject }}</td>
                    <td>{{ slot.timeRange }}</td>
                    <td>{{ slot.student }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
            <p v-else class="panel__helper">本周暂无排课</p>
          </section>
        </article>

        <article class="panel tuition">
          <div class="panel__header">
            <div>
              <h2>学费管理</h2>
              <small>当前学费指标概览</small>
            </div>
            <button class="ghost" @click="loadTuitionOverview" :disabled="tuitionLoading">
              刷新数据
            </button>
          </div>
          <p v-if="tuitionError" class="panel__helper error">{{ tuitionError }}</p>
          <p v-else-if="tuitionLoading" class="panel__helper">学费数据加载中…</p>
          <div v-else class="tuition-stats">
            <div>
              <p>已收总学费</p>
              <strong>{{ formatAmount(tuitionOverview.totalReceived) }}</strong>
            </div>
            <div>
              <p>已销课学费</p>
              <strong>{{ formatAmount(tuitionOverview.totalConsumed) }}</strong>
              <small>计划对接中</small>
            </div>
            <div>
              <p>待销课学费</p>
              <strong>{{ formatAmount(tuitionOverview.totalPending) }}</strong>
              <small>后续功能</small>
            </div>
          </div>
        </article>

        <article class="panel trial">
          <div class="panel__header">
            <div>
              <h2>试听管理</h2>
              <small>录入试听信息并跟进预约</small>
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
                {{ trialSubmitting ? '提交中...' : '录入试听' }}
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
                <small>{{ formatTimestamp(lead.trialTime) }}</small>
              </div>
              <span class="pill-btn normal">{{ formatTimestamp(lead.createdAt) }}</span>
            </li>
          </ul>
          <p v-else class="empty-state">暂无试听记录，填写上方表单即可添加。</p>
        </article>

        <article class="panel renew">
          <div class="panel__header">
            <div>
              <h2>续费管理</h2>
              <small>选择学生并录入续费金额与课时</small>
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
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
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
const tuitionLoading = ref(false);
const tuitionError = ref('');
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
  { id: 'week', label: '查看本周课程表' }
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
const arrangeDraft = reactive({
  studentId: '',
  subject: '英语',
  weekday: weekdayOptions[0].value,
  firstDate: '',
  startTime: '',
  endTime: ''
});
const arrangeSubmitting = ref(false);
const arrangeFeedback = ref('');
const arrangeError = ref('');
const currentWeekStart = ref(getWeekStart(new Date()));
const scheduleLoading = ref(false);
const scheduleError = ref('');
const weekSchedule = ref([]);
const todayLabel = new Intl.DateTimeFormat('zh-CN', {
  month: 'long',
  day: 'numeric',
  weekday: 'long'
}).format(new Date());
const todayPlan = [
  { time: '09:00', title: '家长回访：李小龙家长', owner: '教务 · 王老师', location: '电话', state: 'pending', stateLabel: '待跟进' },
  { time: '14:00', title: '初三英语模考讲评', owner: '教学 · 陈老师', location: '3F 智慧教室', state: 'progress', stateLabel: '进行中' },
  { time: '19:30', title: '高一晚自习巡课', owner: '班主任 · 刘老师', location: '云直播间', state: 'ready', stateLabel: '待开始' }
];
const feimanFocus = [
  { student: '何思源', topic: '物理 · 能量守恒再讲', mentor: '刘老师', progress: 65 },
  { student: '江语桐', topic: '数学 · 二次函数举例', mentor: '孙老师', progress: 45 },
  { student: '吴浩然', topic: '化学 · 离子反应', mentor: '李老师', progress: 82 }
];
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
    return (0).toFixed(fractionDigits);
  }
  if (fractionDigits > 0) {
    return amount.toFixed(fractionDigits);
  }
  return Math.round(amount).toString();
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

const weekScheduleRows = computed(() => {
  return weekSchedule.value.map((slot) => ({
    id: slot.id ?? `${slot.studentId}-${slot.startTime}`,
    weekday: formatWeekday(slot.startTime),
    subject: slot.subject ?? '英语',
    timeRange: formatTimeRange(slot.startTime, slot.endTime),
    student: slot.studentName ?? ''
  }));
});

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

const resetArrangeDraft = (keepMessage = false) => {
  arrangeDraft.studentId = '';
  arrangeDraft.subject = '英语';
  arrangeDraft.weekday = weekdayOptions[0].value;
  arrangeDraft.firstDate = '';
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
    renewSuccess.value = '续费已录入';
    resetRenewForm();
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
    tuitionOverview.value = await response.json();
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
  loadStudents();
  loadTuitionOverview();
  loadWeekSchedule();
  loadTrials();
});

const submitArrangeDraft = async () => {
  arrangeError.value = '';
  arrangeFeedback.value = '';
  if (!arrangeDraft.studentId || !arrangeDraft.weekday || !arrangeDraft.firstDate || !arrangeDraft.startTime || !arrangeDraft.endTime) {
    arrangeError.value = '请完整选择学生、日期与时间';
    return;
  }

  arrangeSubmitting.value = true;
  try {
    const response = await fetch(`${API_BASE}/api/schedules/students/${arrangeDraft.studentId}/auto-generate`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          weekday: arrangeDraft.weekday,
          firstLessonDate: arrangeDraft.firstDate,
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

const formatTimeRange = (start, end) => {
  if (!start || !end) return '--';
  return `${timeFormatter.format(new Date(start))}-${timeFormatter.format(new Date(end))}`;
};

</script>
