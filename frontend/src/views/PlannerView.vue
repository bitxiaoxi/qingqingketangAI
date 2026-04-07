<template>
  <section class="page-stack">
    <PageHeader title="排课管理" />

    <el-card shadow="never" class="feature-card feature-card--planner">
      <div v-if="loading" class="page-state">学生加载中…</div>
      <el-alert v-else-if="error" :title="error" type="error" show-icon :closable="false" />
      <div v-else class="planner-stack">
        <section class="planner-section">
          <div class="planner-section__head">
            <h3>新生排课</h3>
          </div>

          <div class="planner-mode-grid planner-mode-grid--single">
            <button type="button" class="planner-mode" @click="openCreateDialog()">
              <div class="planner-mode__body">
                <strong>生成课表</strong>
                <small>选择学生后开始排课。</small>
              </div>
              <div v-if="selectedCreateStudent" class="planner-mode__meta">
                {{ selectedCreateStudent.name }} · {{ selectedCreateStudent.grade }} · 可排 {{ getSchedulableLessons(selectedCreateStudent) }} 节
              </div>
            </button>
          </div>
        </section>

        <section class="planner-section">
          <div class="planner-section__head">
            <h3>调课处理</h3>
          </div>

          <div class="planner-mode-grid planner-mode-grid--adjust">
            <button type="button" class="planner-mode" @click="openAdjustDialog('temporary')">
              <div class="planner-mode__body">
                <strong>补课</strong>
                <small>补入一节临时课程。</small>
              </div>
            </button>

            <button type="button" class="planner-mode" @click="openAdjustDialog('leave')">
              <div class="planner-mode__body">
                <strong>请假顺延</strong>
                <small>最近一节课程顺延到末尾。</small>
              </div>
            </button>

            <button type="button" class="planner-mode" @click="openAdjustDialog('reschedule')">
              <div class="planner-mode__body">
                <strong>单节改时间</strong>
                <small>调整一节课的日期和时间。</small>
              </div>
            </button>

            <button type="button" class="planner-mode" @click="openAdjustDialog('future')">
              <div class="planner-mode__body">
                <strong>后续改时间</strong>
                <small>统一调整后续课程时间。</small>
              </div>
            </button>
          </div>
        </section>
      </div>
    </el-card>

    <el-dialog
      v-model="createDialogVisible"
      title="新建排课"
      width="760px"
      destroy-on-close
      class="schedule-dialog"
    >
      <el-form label-position="top" class="dialog-form">
        <section class="dialog-block dialog-block--primary">
          <div class="dialog-block__head">
            <h4>基础信息</h4>
          </div>

          <el-form-item label="学生" required>
            <el-select v-model="createForm.studentId" filterable placeholder="请选择学生">
              <el-option
                v-for="student in students"
                :key="student.id"
                :label="`${student.name} · ${student.grade}`"
                :value="student.id"
              />
            </el-select>
          </el-form-item>

          <div v-if="selectedCreateStudent" class="dialog-inline-summary">
            <strong>{{ selectedCreateStudent.name }} · {{ selectedCreateStudent.grade }}</strong>
            <span>当前可排 {{ getSchedulableLessons(selectedCreateStudent) }} 节课</span>
          </div>

          <div class="dialog-block__subhead">
            <strong>排课方式</strong>
          </div>

          <div class="create-mode-toggle">
            <button
              type="button"
              class="create-mode-toggle__item"
              :data-active="!isSameClassCreateMode"
              @click="setCreateClassMode('solo')"
            >
              单独上课
            </button>
            <button
              type="button"
              class="create-mode-toggle__item"
              :data-active="isSameClassCreateMode"
              @click="setCreateClassMode('sameClass')"
            >
              加入同班
            </button>
          </div>

          <el-form-item v-if="isSameClassCreateMode" label="同班学生" required>
            <el-select v-model="createForm.sameClassStudentId" filterable placeholder="请选择同班学生">
              <el-option
                v-for="student in sameClassCandidates"
                :key="student.id"
                :label="`${student.name} · ${student.grade}`"
                :value="student.id"
              />
            </el-select>
          </el-form-item>

          <div v-if="!isSameClassCreateMode" class="dialog-grid">
            <el-form-item label="每周上课次数" required>
              <el-select v-model="createForm.weeklySessions">
                <el-option
                  v-for="option in frequencyOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="首次课日期" required>
              <el-date-picker
                v-model="createForm.startDate"
                type="date"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                placeholder="选择首次课日期"
              />
            </el-form-item>
          </div>

          <el-form-item v-else label="首次课日期" required>
            <el-date-picker
              v-model="createForm.startDate"
              type="date"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              placeholder="选择首次课日期"
            />
          </el-form-item>

        </section>

        <section v-if="!isSameClassCreateMode" class="dialog-block">
          <div class="dialog-block__head">
            <h4>每周上课日</h4>
          </div>

          <div v-if="isDailySchedule" class="weekday-auto-state">
            <strong>每天上课</strong>
            <span>已自动包含周一到周日，无需再选择上课日。</span>
          </div>

          <div v-else class="weekday-chip-grid">
            <button
              v-for="option in weekdayOptions"
              :key="option.value"
              type="button"
              class="weekday-chip"
              :data-active="createForm.weekdays.includes(option.value)"
              :disabled="createForm.weeklySessions > 1 && !createForm.weekdays.includes(option.value) && createForm.weekdays.length >= createForm.weeklySessions"
              @click="toggleWeekday(option.value)"
            >
              {{ option.label }}
            </button>
          </div>

          <div v-if="!isDailySchedule" class="dialog-meta-row">
            <span class="dialog-meta-pill">已选 {{ createForm.weekdays.length }} / {{ createForm.weeklySessions }} 天</span>
          </div>
        </section>

        <section v-if="!isSameClassCreateMode" class="dialog-block">
          <div class="dialog-block__head">
            <h4>上课时间</h4>
          </div>

          <div class="dialog-grid">
            <el-form-item label="开始时间" required>
              <el-time-picker
                v-model="createForm.startTime"
                value-format="HH:mm"
                format="HH:mm"
                placeholder="开始时间"
              />
            </el-form-item>

            <el-form-item label="结束时间" required>
              <el-time-picker
                v-model="createForm.endTime"
                value-format="HH:mm"
                format="HH:mm"
                placeholder="结束时间"
              />
            </el-form-item>
          </div>

        </section>

        <section v-else class="dialog-block">
          <div class="dialog-block__head">
            <h4>班级课表</h4>
          </div>

          <div class="weekday-auto-state weekday-auto-state--group">
            <strong>{{ selectedSameClassStudent ? `沿用 ${selectedSameClassStudent.name} 的班级安排` : '先选择同班学生' }}</strong>
            <span>{{ sameClassSlotSummary }}</span>
          </div>
        </section>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="createDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="creating" @click="submitCreateForm">生成课表</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="adjustDialogVisible"
      :title="adjustDialogTitle"
      width="860px"
      destroy-on-close
      class="schedule-dialog"
    >
      <el-form label-position="top" class="dialog-form">
        <section class="dialog-block dialog-block--primary">
          <div class="dialog-block__head">
            <h4>调课对象</h4>
          </div>

          <el-form-item label="学生" required>
            <el-select v-model="adjustStudentId" filterable placeholder="请选择学生">
              <el-option
                v-for="student in students"
                :key="student.id"
                :label="`${student.name} · ${student.grade}`"
                :value="student.id"
              />
            </el-select>
          </el-form-item>

          <div v-if="selectedAdjustStudent" class="dialog-inline-summary">
            <strong>{{ selectedAdjustStudent.name }} · {{ selectedAdjustStudent.grade }}</strong>
            <span>待上 {{ plannedSchedules.length }} 节</span>
          </div>

          <div v-if="adjustLoadingSchedules" class="adjustment-state">待上课程加载中…</div>
          <div v-else-if="adjustStudentId && !plannedSchedules.length" class="adjustment-state adjustment-state--empty">
            当前没有可调整的待上课程，请先为该学生生成正式课表。
          </div>
        </section>

        <section v-if="adjustMode === 'temporary'" class="dialog-block">
          <div class="dialog-block__head">
            <h4>补课时间</h4>
          </div>

          <el-form-item label="临时上课日期" required>
            <el-date-picker
              v-model="temporaryForm.lessonDate"
              type="date"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              placeholder="选择临时上课日期"
            />
          </el-form-item>

          <div class="dialog-grid">
            <el-form-item label="开始时间" required>
              <el-time-picker
                v-model="temporaryForm.startTime"
                value-format="HH:mm"
                format="HH:mm"
                placeholder="开始时间"
              />
            </el-form-item>

            <el-form-item label="结束时间" required>
              <el-time-picker
                v-model="temporaryForm.endTime"
                value-format="HH:mm"
                format="HH:mm"
                placeholder="结束时间"
              />
            </el-form-item>
          </div>

          <div class="adjustment-swap-card">
            <div class="adjustment-swap-card__block">
              <span>补入课程</span>
              <strong>{{ temporaryTargetLabel }}</strong>
            </div>
            <div class="adjustment-swap-card__block">
              <span>自动移除</span>
              <strong>{{ adjustLastSchedule ? formatScheduleOptionLabel(adjustLastSchedule) : '暂未找到可替换课程' }}</strong>
            </div>
          </div>
        </section>

        <section v-else-if="adjustMode === 'leave'" class="dialog-block">
          <div class="dialog-block__head">
            <h4>请假顺延</h4>
          </div>

          <div class="adjustment-swap-card">
            <div class="adjustment-swap-card__block">
              <span>本次请假移除</span>
              <strong>{{ adjustFirstSchedule ? formatScheduleOptionLabel(adjustFirstSchedule) : '暂未找到待上课程' }}</strong>
            </div>
            <div class="adjustment-swap-card__block">
              <span>自动顺延到末尾</span>
              <strong>{{ leaveTargetScheduleLabel }}</strong>
            </div>
          </div>
        </section>

        <section v-else-if="adjustMode === 'reschedule'" class="dialog-block">
          <div class="dialog-block__head">
            <h4>改课时间</h4>
          </div>

          <el-form-item label="待调整课程" required>
            <el-select
              v-model="rescheduleForm.scheduleId"
              filterable
              placeholder="请选择要改时间的课程"
              :loading="adjustLoadingSchedules"
            >
              <el-option
                v-for="schedule in plannedSchedules"
                :key="schedule.id"
                :label="formatScheduleOptionLabel(schedule)"
                :value="schedule.id"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="调整后日期" required>
            <el-date-picker
              v-model="rescheduleForm.lessonDate"
              type="date"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              placeholder="选择新的上课日期"
            />
          </el-form-item>

          <div class="dialog-grid">
            <el-form-item label="开始时间" required>
              <el-time-picker
                v-model="rescheduleForm.startTime"
                value-format="HH:mm"
                format="HH:mm"
                placeholder="新的开始时间"
              />
            </el-form-item>

            <el-form-item label="结束时间" required>
              <el-time-picker
                v-model="rescheduleForm.endTime"
                value-format="HH:mm"
                format="HH:mm"
                placeholder="新的结束时间"
              />
            </el-form-item>
          </div>

        </section>

        <section v-else class="dialog-block">
          <div class="dialog-block__head">
            <h4>后续改时间</h4>
          </div>

          <el-form-item label="生效起点课程" required>
            <el-select
              v-model="futureRescheduleForm.scheduleId"
              filterable
              placeholder="请选择开始批量调整的课程"
              :loading="adjustLoadingSchedules"
            >
              <el-option
                v-for="schedule in plannedSchedules"
                :key="schedule.id"
                :label="formatScheduleOptionLabel(schedule)"
                :value="schedule.id"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="调整后首节日期" required>
            <el-date-picker
              v-model="futureRescheduleForm.lessonDate"
              type="date"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              placeholder="选择新的首节上课日期"
            />
          </el-form-item>

          <div class="dialog-grid">
            <el-form-item label="开始时间" required>
              <el-time-picker
                v-model="futureRescheduleForm.startTime"
                value-format="HH:mm"
                format="HH:mm"
                placeholder="新的开始时间"
              />
            </el-form-item>

            <el-form-item label="结束时间" required>
              <el-time-picker
                v-model="futureRescheduleForm.endTime"
                value-format="HH:mm"
                format="HH:mm"
                placeholder="新的结束时间"
              />
            </el-form-item>
          </div>

          <div v-if="selectedFutureRescheduleSchedule" class="adjustment-state">
            预计影响 {{ futureRescheduleCandidates.length }} 节课程
          </div>
        </section>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="adjustDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="adjustSubmitting" @click="submitAdjustAction">
            {{ adjustSubmitLabel }}
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
import { frequencyOptions, weekdayOptions } from '../constants/options';
import { api } from '../services/api';
import {
  formatClock,
  formatDateParam,
  formatLongDate,
  formatTimeRange,
  normalizeError
} from '../utils/format';

const route = useRoute();
const getRouteStudentId = () => (route.query.studentId ? Number(route.query.studentId) : null);

const loading = ref(false);
const error = ref('');
const students = ref([]);
const creating = ref(false);
const createDialogVisible = ref(false);
const createClassMode = ref('solo');
const adjustDialogVisible = ref(false);
const adjustMode = ref('temporary');
const adjustStudentId = ref(getRouteStudentId());
const adjustLoadingSchedules = ref(false);
const adjustSubmitting = ref(false);
const plannedSchedules = ref([]);
const sameClassReferenceSchedules = ref([]);
const loadingSameClassSchedules = ref(false);
const createForm = reactive({
  studentId: getRouteStudentId(),
  sameClassStudentId: null,
  weeklySessions: 1,
  weekdays: [weekdayOptions[0].value],
  startDate: formatDateParam(new Date()),
  startTime: '19:00',
  endTime: '20:30'
});
const temporaryForm = reactive({
  lessonDate: formatDateParam(new Date()),
  startTime: '19:00',
  endTime: '20:30'
});
const rescheduleForm = reactive({
  scheduleId: null,
  lessonDate: formatDateParam(new Date()),
  startTime: '19:00',
  endTime: '20:30'
});
const futureRescheduleForm = reactive({
  scheduleId: null,
  lessonDate: formatDateParam(new Date()),
  startTime: '19:00',
  endTime: '20:30'
});

const allWeekdayValues = weekdayOptions.map((option) => option.value);

const getSchedulableLessons = (student) => {
  return Math.max(0, Number(student?.schedulableLessons ?? student?.remainingLessons ?? 0));
};

const loadStudents = async () => {
  loading.value = true;
  error.value = '';
  try {
    students.value = await api.listStudents();
  } catch (requestError) {
    error.value = normalizeError(requestError, '学生信息加载失败');
  } finally {
    loading.value = false;
  }
};

const formatScheduleOptionLabel = (schedule) => {
  if (!schedule?.startTime || !schedule?.endTime) {
    return '时间待定';
  }
  return `${formatLongDate(schedule.startTime)} ${formatTimeRange(schedule.startTime, schedule.endTime)}`;
};

const buildSlotLabel = (lessonDate, startTime, endTime) => {
  if (!lessonDate || !startTime || !endTime) {
    return '时间待定';
  }
  return `${lessonDate} ${startTime}-${endTime}`;
};

const buildDateTimeFromClock = (dateValue, clock) => {
  const [hours, minutes] = String(clock ?? '').split(':').map((value) => Number(value));
  if (Number.isNaN(hours) || Number.isNaN(minutes)) {
    return null;
  }
  const date = new Date(dateValue);
  return new Date(
    date.getFullYear(),
    date.getMonth(),
    date.getDate(),
    hours,
    minutes,
    0,
    0
  );
};

const scheduleMatchesRecurringTemplate = (reference, candidate) => {
  if (!reference?.startTime || !reference?.endTime || !candidate?.startTime || !candidate?.endTime) {
    return false;
  }
  return reference.subject === candidate.subject
    && new Date(candidate.startTime).getTime() >= new Date(reference.startTime).getTime()
    && getWeekdayValue(candidate.startTime) === getWeekdayValue(reference.startTime)
    && formatClock(candidate.startTime) === formatClock(reference.startTime)
    && formatClock(candidate.endTime) === formatClock(reference.endTime);
};

const getWeekdayValue = (value) => {
  const day = new Date(value).getDay();
  return day === 0 ? 7 : day;
};

const buildSameClassSlotTemplates = (schedules) => {
  const templateMap = new Map();
  schedules.forEach((schedule) => {
    if (!schedule?.startTime || !schedule?.endTime) {
      return;
    }
    const weekday = getWeekdayValue(schedule.startTime);
    const startClock = formatClock(schedule.startTime);
    const endClock = formatClock(schedule.endTime);
    const subject = schedule.subject ?? '';
    const key = `${weekday}-${startClock}-${endClock}-${subject}`;
    const existing = templateMap.get(key);
    if (existing) {
      existing.count += 1;
      return;
    }
    templateMap.set(key, {
      key,
      weekday,
      weekdayLabel: weekdayOptions.find((option) => option.value === weekday)?.label ?? `周${weekday}`,
      startClock,
      endClock,
      subject,
      count: 1
    });
  });

  const uniqueTemplates = [...templateMap.values()].sort((left, right) => {
    if (left.weekday !== right.weekday) {
      return left.weekday - right.weekday;
    }
    return left.startClock.localeCompare(right.startClock);
  });
  const recurringTemplates = uniqueTemplates.filter((template) => template.count >= 2);
  return recurringTemplates.length ? recurringTemplates : uniqueTemplates;
};

const buildNextRecurringSchedule = (schedules) => {
  if (!Array.isArray(schedules) || !schedules.length) {
    return null;
  }

  const lastSchedule = schedules[schedules.length - 1];
  if (!lastSchedule?.startTime || !lastSchedule?.endTime) {
    return null;
  }

  const templates = buildSameClassSlotTemplates(schedules);
  if (!templates.length) {
    return null;
  }

  const startDate = new Date(lastSchedule.startTime);
  startDate.setHours(0, 0, 0, 0);
  startDate.setDate(startDate.getDate() + 1);

  let weekStart = new Date(startDate);
  const weekday = weekStart.getDay() === 0 ? 7 : weekStart.getDay();
  weekStart.setDate(weekStart.getDate() - weekday + 1);

  for (let weekOffset = 0; weekOffset < 60; weekOffset += 1) {
    for (const template of templates) {
      const candidateDate = new Date(weekStart);
      candidateDate.setDate(weekStart.getDate() + template.weekday - 1);
      candidateDate.setHours(0, 0, 0, 0);
      if (candidateDate.getTime() < startDate.getTime()) {
        continue;
      }
      const startTime = buildDateTimeFromClock(candidateDate, template.startClock);
      const endTime = buildDateTimeFromClock(candidateDate, template.endClock);
      if (!startTime || !endTime) {
        continue;
      }
      return {
        subject: template.subject ?? lastSchedule.subject ?? '',
        startTime,
        endTime
      };
    }
    weekStart.setDate(weekStart.getDate() + 7);
  }

  return null;
};

const applyScheduleTimeDefaults = (schedule) => {
  if (!schedule?.startTime || !schedule?.endTime) {
    return;
  }
  temporaryForm.startTime = formatClock(schedule.startTime);
  temporaryForm.endTime = formatClock(schedule.endTime);
};

const loadPlannedSchedules = async (studentId = adjustStudentId.value) => {
  if (!studentId) {
    plannedSchedules.value = [];
    rescheduleForm.scheduleId = null;
    futureRescheduleForm.scheduleId = null;
    return;
  }

  adjustLoadingSchedules.value = true;
  try {
    const scheduleList = await api.listStudentPlannedSchedules(studentId);
    plannedSchedules.value = Array.isArray(scheduleList) ? scheduleList : [];
    if (!plannedSchedules.value.some((schedule) => schedule.id === rescheduleForm.scheduleId)) {
      rescheduleForm.scheduleId = plannedSchedules.value[0]?.id ?? null;
    }
    if (!plannedSchedules.value.some((schedule) => schedule.id === futureRescheduleForm.scheduleId)) {
      futureRescheduleForm.scheduleId = plannedSchedules.value[0]?.id ?? null;
    }
    applyScheduleTimeDefaults(plannedSchedules.value[plannedSchedules.value.length - 1] ?? null);
  } catch (requestError) {
    plannedSchedules.value = [];
    rescheduleForm.scheduleId = null;
    futureRescheduleForm.scheduleId = null;
    ElMessage.error(normalizeError(requestError, '待上课程加载失败'));
  } finally {
    adjustLoadingSchedules.value = false;
  }
};

const loadSameClassReferenceSchedules = async (studentId = createForm.sameClassStudentId) => {
  if (!studentId) {
    sameClassReferenceSchedules.value = [];
    return;
  }

  loadingSameClassSchedules.value = true;
  try {
    const scheduleList = await api.listStudentPlannedSchedules(studentId);
    if (studentId !== createForm.sameClassStudentId) {
      return;
    }
    sameClassReferenceSchedules.value = Array.isArray(scheduleList) ? scheduleList : [];
  } catch (requestError) {
    sameClassReferenceSchedules.value = [];
    ElMessage.error(normalizeError(requestError, '同班课表读取失败'));
  } finally {
    loadingSameClassSchedules.value = false;
  }
};

const isDailySchedule = computed(() => createForm.weeklySessions === allWeekdayValues.length);
const normalizedCreateWeekdays = computed(() => {
  if (isDailySchedule.value) {
    return [...allWeekdayValues];
  }
  return [...createForm.weekdays].sort((left, right) => left - right);
});

const selectedCreateStudent = computed(() => {
  return students.value.find((student) => student.id === createForm.studentId) ?? null;
});
const isSameClassCreateMode = computed(() => createClassMode.value === 'sameClass');
const sameClassCandidates = computed(() => {
  return students.value.filter((student) => student.id !== createForm.studentId);
});
const selectedSameClassStudent = computed(() => {
  return students.value.find((student) => student.id === createForm.sameClassStudentId) ?? null;
});
const sameClassSlotTemplates = computed(() => {
  return buildSameClassSlotTemplates(sameClassReferenceSchedules.value);
});
const sameClassSlotSummary = computed(() => {
  if (!selectedSameClassStudent.value) {
    return '未选择同班学生';
  }
  if (loadingSameClassSchedules.value) {
    return '正在读取班级时段';
  }
  if (!sameClassSlotTemplates.value.length) {
    return '暂未读取到可沿用的班级时段';
  }
  return sameClassSlotTemplates.value
    .map((template) => `${template.weekdayLabel} ${template.startClock}-${template.endClock}`)
    .join('、');
});
const selectedAdjustStudent = computed(() => {
  return students.value.find((student) => student.id === adjustStudentId.value) ?? null;
});
const adjustFirstSchedule = computed(() => {
  return plannedSchedules.value[0] ?? null;
});
const adjustLastSchedule = computed(() => {
  return plannedSchedules.value[plannedSchedules.value.length - 1] ?? null;
});
const selectedFutureRescheduleSchedule = computed(() => {
  return plannedSchedules.value.find((schedule) => schedule.id === futureRescheduleForm.scheduleId) ?? null;
});
const futureRescheduleCandidates = computed(() => {
  if (!selectedFutureRescheduleSchedule.value) {
    return [];
  }
  return plannedSchedules.value.filter((schedule) => scheduleMatchesRecurringTemplate(selectedFutureRescheduleSchedule.value, schedule));
});

const temporaryTargetLabel = computed(() => {
  return buildSlotLabel(temporaryForm.lessonDate, temporaryForm.startTime, temporaryForm.endTime);
});
const leaveTargetSchedule = computed(() => {
  return buildNextRecurringSchedule(plannedSchedules.value);
});
const leaveTargetScheduleLabel = computed(() => {
  return leaveTargetSchedule.value
    ? formatScheduleOptionLabel(leaveTargetSchedule.value)
    : '当前还无法推算顺延课程';
});
const adjustDialogTitle = computed(() => {
  if (adjustMode.value === 'temporary') {
    return '临时加课';
  }
  if (adjustMode.value === 'leave') {
    return '请假顺延';
  }
  if (adjustMode.value === 'future') {
    return '后续课程改时间';
  }
  return '课程改时间';
});
const adjustSubmitLabel = computed(() => {
  if (adjustMode.value === 'temporary') {
    return '确认临时加课';
  }
  if (adjustMode.value === 'leave') {
    return '确认请假顺延';
  }
  if (adjustMode.value === 'future') {
    return '确认批量改时间';
  }
  return '确认改时间';
});

const resetCreateForm = (studentId = null) => {
  createClassMode.value = 'solo';
  createForm.studentId = studentId;
  createForm.sameClassStudentId = null;
  createForm.weeklySessions = 1;
  createForm.weekdays = [weekdayOptions[0].value];
  createForm.startDate = formatDateParam(new Date());
  createForm.startTime = '19:00';
  createForm.endTime = '20:30';
  sameClassReferenceSchedules.value = [];
  loadingSameClassSchedules.value = false;
};

const setCreateClassMode = (mode) => {
  createClassMode.value = mode;
  if (mode !== 'sameClass') {
    createForm.sameClassStudentId = null;
    sameClassReferenceSchedules.value = [];
    loadingSameClassSchedules.value = false;
  }
};

const resetAdjustForms = (studentId = getRouteStudentId(), mode = 'temporary') => {
  adjustMode.value = mode;
  adjustStudentId.value = studentId;
  plannedSchedules.value = [];
  temporaryForm.lessonDate = formatDateParam(new Date());
  temporaryForm.startTime = '19:00';
  temporaryForm.endTime = '20:30';
  rescheduleForm.scheduleId = null;
  rescheduleForm.lessonDate = formatDateParam(new Date());
  rescheduleForm.startTime = '19:00';
  rescheduleForm.endTime = '20:30';
  futureRescheduleForm.scheduleId = null;
  futureRescheduleForm.lessonDate = formatDateParam(new Date());
  futureRescheduleForm.startTime = '19:00';
  futureRescheduleForm.endTime = '20:30';
};

const openCreateDialog = (studentId = getRouteStudentId()) => {
  resetCreateForm(studentId);
  createDialogVisible.value = true;
};

const openAdjustDialog = async (mode = 'temporary', studentId = getRouteStudentId()) => {
  resetAdjustForms(studentId, mode);
  adjustDialogVisible.value = true;
  await loadPlannedSchedules(studentId);
};

const toggleWeekday = (weekday) => {
  if (isDailySchedule.value) {
    return;
  }
  const selected = [...createForm.weekdays];
  if (selected.includes(weekday)) {
    if (selected.length === 1) {
      return;
    }
    createForm.weekdays = selected.filter((item) => item !== weekday).sort((left, right) => left - right);
    return;
  }
  if (selected.length >= createForm.weeklySessions) {
    if (createForm.weeklySessions === 1) {
      createForm.weekdays = [weekday];
    }
    return;
  }
  createForm.weekdays = [...selected, weekday].sort((left, right) => left - right);
};

const submitCreateForm = async () => {
  if (!createForm.studentId || !createForm.startDate) {
    ElMessage.error('请完整填写排课信息');
    return;
  }
  if (isSameClassCreateMode.value && !createForm.sameClassStudentId) {
    ElMessage.error('请选择要加入同班的学生');
    return;
  }
  if (!isSameClassCreateMode.value && (!createForm.startTime || !createForm.endTime)) {
    ElMessage.error('请完整填写排课信息');
    return;
  }
  if (!isSameClassCreateMode.value && !isDailySchedule.value && normalizedCreateWeekdays.value.length !== createForm.weeklySessions) {
    ElMessage.error(`每周 ${createForm.weeklySessions} 次课，请选择 ${createForm.weeklySessions} 个上课日`);
    return;
  }

  creating.value = true;
  try {
    const payload = {
      sameClassStudentId: isSameClassCreateMode.value ? createForm.sameClassStudentId : null,
      startDate: createForm.startDate
    };
    if (!isSameClassCreateMode.value) {
      payload.weekdays = normalizedCreateWeekdays.value;
      payload.startTime = createForm.startTime;
      payload.endTime = createForm.endTime;
    }
    const generated = await api.generateSchedules(createForm.studentId, payload);
    await loadStudents();
    createDialogVisible.value = false;
    ElMessage.success(`已生成 ${Array.isArray(generated) ? generated.length : 0} 节课`);
  } catch (requestError) {
    ElMessage.error(normalizeError(requestError, '排课失败'));
  } finally {
    creating.value = false;
  }
};

const submitTemporaryLesson = async () => {
  if (!adjustStudentId.value || !temporaryForm.lessonDate || !temporaryForm.startTime || !temporaryForm.endTime) {
    ElMessage.error('请完整填写临时加课时间');
    return;
  }
  if (!adjustLastSchedule.value) {
    ElMessage.error('当前没有可替换的待上课程，请先生成正式课表');
    return;
  }

  adjustSubmitting.value = true;
  try {
    const result = await api.createTemporaryLesson(adjustStudentId.value, {
      lessonDate: temporaryForm.lessonDate,
      startTime: temporaryForm.startTime,
      endTime: temporaryForm.endTime
    });
    await loadStudents();
    await loadPlannedSchedules(adjustStudentId.value);
    adjustDialogVisible.value = false;
    ElMessage.success(result?.message ?? '临时加课已处理');
  } catch (requestError) {
    ElMessage.error(normalizeError(requestError, '临时加课失败'));
  } finally {
    adjustSubmitting.value = false;
  }
};

const submitLeaveAdjustment = async () => {
  if (!adjustStudentId.value) {
    ElMessage.error('请先选择学生');
    return;
  }
  if (!adjustFirstSchedule.value) {
    ElMessage.error('该学生当前没有待上课程，无法执行请假顺延');
    return;
  }
  if (!leaveTargetSchedule.value) {
    ElMessage.error('当前还无法推算顺延后的末尾课程');
    return;
  }

  adjustSubmitting.value = true;
  try {
    const result = await api.requestScheduleLeave(adjustStudentId.value);
    await loadStudents();
    await loadPlannedSchedules(adjustStudentId.value);
    adjustDialogVisible.value = false;
    ElMessage.success(result?.message ?? '请假顺延已处理');
  } catch (requestError) {
    ElMessage.error(normalizeError(requestError, '请假顺延失败'));
  } finally {
    adjustSubmitting.value = false;
  }
};

const submitReschedule = async () => {
  if (!adjustStudentId.value || !rescheduleForm.scheduleId) {
    ElMessage.error('请先选择需要改时间的课程');
    return;
  }
  if (!rescheduleForm.lessonDate || !rescheduleForm.startTime || !rescheduleForm.endTime) {
    ElMessage.error('请完整填写新的上课时间');
    return;
  }

  adjustSubmitting.value = true;
  try {
    await api.rescheduleSchedule(rescheduleForm.scheduleId, {
      lessonDate: rescheduleForm.lessonDate,
      startTime: rescheduleForm.startTime,
      endTime: rescheduleForm.endTime
    });
    await loadPlannedSchedules(adjustStudentId.value);
    adjustDialogVisible.value = false;
    ElMessage.success('课程时间已更新');
  } catch (requestError) {
    ElMessage.error(normalizeError(requestError, '课程改时间失败'));
  } finally {
    adjustSubmitting.value = false;
  }
};

const submitFutureReschedule = async () => {
  if (!adjustStudentId.value || !futureRescheduleForm.scheduleId) {
    ElMessage.error('请先选择后续改时间的生效起点课程');
    return;
  }
  if (!futureRescheduleForm.lessonDate || !futureRescheduleForm.startTime || !futureRescheduleForm.endTime) {
    ElMessage.error('请完整填写新的上课时间');
    return;
  }

  adjustSubmitting.value = true;
  try {
    const result = await api.rescheduleFollowingSchedules(futureRescheduleForm.scheduleId, {
      lessonDate: futureRescheduleForm.lessonDate,
      startTime: futureRescheduleForm.startTime,
      endTime: futureRescheduleForm.endTime
    });
    await loadPlannedSchedules(adjustStudentId.value);
    adjustDialogVisible.value = false;
    const updatedCount = Number(result?.updatedCount ?? result?.updatedSchedules?.length ?? 0);
    ElMessage.success(updatedCount > 0 ? `已更新 ${updatedCount} 节后续课程` : (result?.message ?? '后续课程时间已更新'));
  } catch (requestError) {
    ElMessage.error(normalizeError(requestError, '后续课程改时间失败'));
  } finally {
    adjustSubmitting.value = false;
  }
};

const submitAdjustAction = async () => {
  if (adjustMode.value === 'temporary') {
    await submitTemporaryLesson();
    return;
  }
  if (adjustMode.value === 'leave') {
    await submitLeaveAdjustment();
    return;
  }
  if (adjustMode.value === 'future') {
    await submitFutureReschedule();
    return;
  }
  await submitReschedule();
};

watch(
  () => createForm.weeklySessions,
  (value) => {
    if (value >= allWeekdayValues.length) {
      createForm.weekdays = [...allWeekdayValues];
      return;
    }
    if (createForm.weekdays.length > value) {
      createForm.weekdays = createForm.weekdays.slice(0, value);
    }
    if (!createForm.weekdays.length) {
      createForm.weekdays = [weekdayOptions[0].value];
    }
  }
);

watch(
  () => createForm.studentId,
  (studentId) => {
    if (studentId && studentId === createForm.sameClassStudentId) {
      createForm.sameClassStudentId = null;
    }
  }
);

watch(
  () => [createDialogVisible.value, createClassMode.value, createForm.sameClassStudentId],
  async ([dialogVisible, createMode, sameClassStudentId]) => {
    if (!dialogVisible || createMode !== 'sameClass' || !sameClassStudentId) {
      sameClassReferenceSchedules.value = [];
      loadingSameClassSchedules.value = false;
      return;
    }
    await loadSameClassReferenceSchedules(sameClassStudentId);
  }
);

watch(
  adjustStudentId,
  async (studentId) => {
    if (!adjustDialogVisible.value) {
      return;
    }
    await loadPlannedSchedules(studentId);
  }
);

watch(
  () => rescheduleForm.scheduleId,
  (scheduleId) => {
    const schedule = plannedSchedules.value.find((item) => item.id === scheduleId);
    if (!schedule?.startTime || !schedule?.endTime) {
      return;
    }
    rescheduleForm.lessonDate = formatDateParam(schedule.startTime);
    rescheduleForm.startTime = formatClock(schedule.startTime);
    rescheduleForm.endTime = formatClock(schedule.endTime);
  }
);

watch(
  () => futureRescheduleForm.scheduleId,
  (scheduleId) => {
    const schedule = plannedSchedules.value.find((item) => item.id === scheduleId);
    if (!schedule?.startTime || !schedule?.endTime) {
      return;
    }
    futureRescheduleForm.lessonDate = formatDateParam(schedule.startTime);
    futureRescheduleForm.startTime = formatClock(schedule.startTime);
    futureRescheduleForm.endTime = formatClock(schedule.endTime);
  }
);

watch(
  () => route.query.action,
  (action) => {
    if (action === 'create') {
      openCreateDialog(getRouteStudentId());
    }
  },
  { immediate: true }
);

watch(
  () => route.query.studentId,
  (studentId) => {
    createForm.studentId = studentId ? Number(studentId) : null;
    adjustStudentId.value = studentId ? Number(studentId) : null;
  },
  { immediate: true }
);

onMounted(async () => {
  await loadStudents();
});
</script>

<style scoped>
.feature-card {
  border-color: rgba(226, 232, 240, 0.9);
  background: #ffffff;
  box-shadow: 0 14px 32px rgba(15, 23, 42, 0.04);
}

.feature-card--planner {
  box-shadow: 0 14px 32px rgba(15, 23, 42, 0.04);
}

.feature-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.feature-card--planner :deep(.el-alert) {
  border-radius: 18px;
  border: 1px solid rgba(248, 113, 113, 0.24);
}

.planner-stack {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.planner-section {
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 24px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 20px;
  background: #ffffff;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.03);
}

.planner-section__head {
  display: flex;
  align-items: center;
  gap: 12px;
}

.planner-section__head h3 {
  margin: 0;
  font-size: 22px;
  line-height: 1.1;
  font-weight: 700;
  letter-spacing: -0.03em;
}

.planner-mode-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 16px;
}

.planner-mode-grid--single {
  grid-template-columns: 1fr;
}

.planner-mode-grid--adjust {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.planner-mode {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 156px;
  padding: 22px;
  border: 1px solid rgba(226, 232, 240, 0.92);
  border-radius: 18px;
  background: #ffffff;
  text-align: left;
  cursor: pointer;
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.03);
  transition: transform 160ms ease, box-shadow 160ms ease, border-color 160ms ease;
}

.planner-mode:hover {
  transform: translateY(-1px);
  border-color: rgba(203, 213, 225, 0.94);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.06);
}

.planner-mode:focus-visible {
  outline: none;
  border-color: rgba(148, 163, 184, 0.78);
  box-shadow:
    0 0 0 4px rgba(226, 232, 240, 0.88),
    0 10px 24px rgba(15, 23, 42, 0.06);
}

.planner-mode__body {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-width: 34ch;
}

.planner-mode strong {
  display: block;
  margin: 0;
  color: #0f172a;
  font-size: 20px;
  line-height: 1.16;
  font-weight: 700;
  letter-spacing: -0.03em;
}

.planner-mode small {
  display: block;
  color: #4f5f75;
  font-size: 13px;
  line-height: 1.6;
}

.planner-mode__meta {
  display: inline-flex;
  align-items: center;
  align-self: flex-start;
  margin-top: auto;
  padding: 8px 12px;
  border-radius: 999px;
  border: 1px solid rgba(226, 232, 240, 0.88);
  background: rgba(248, 250, 252, 0.94);
  color: #475569;
  font-size: 12px;
  font-weight: 600;
  line-height: 1.5;
}

.schedule-dialog :deep(.el-dialog) {
  overflow: hidden;
  border: 1px solid rgba(226, 232, 240, 0.96);
  border-radius: 24px;
  background: #ffffff;
  box-shadow: 0 20px 48px rgba(15, 23, 42, 0.12);
}

.schedule-dialog :deep(.el-dialog__header) {
  margin-right: 0;
  padding: 22px 22px 0;
}

.schedule-dialog :deep(.el-dialog__title) {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -0.03em;
}

.schedule-dialog :deep(.el-dialog__body) {
  padding: 20px 22px 22px;
  background: #ffffff;
}

.schedule-dialog :deep(.el-dialog__footer) {
  padding: 0 22px 22px;
}

.schedule-dialog :deep(.el-dialog__headerbtn) {
  top: 18px;
  right: 18px;
  width: 36px;
  height: 36px;
  border-radius: 12px;
  background: rgba(248, 250, 252, 0.94);
  transition: background 160ms ease;
}

.schedule-dialog :deep(.el-dialog__headerbtn:hover) {
  background: rgba(241, 245, 249, 0.98);
}

.schedule-dialog :deep(.el-dialog__headerbtn .el-dialog__close) {
  color: #64748b;
}

.schedule-dialog :deep(.el-dialog__headerbtn:hover .el-dialog__close) {
  color: #1f2937;
}

.dialog-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.dialog-block {
  padding: 20px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 20px;
  background: #ffffff;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.03);
}

.dialog-block--primary {
  background: #ffffff;
  border-color: rgba(226, 232, 240, 0.9);
}

.dialog-block__head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.dialog-block__head h4 {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
}

.dialog-block__subhead {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.dialog-block__subhead strong {
  font-size: 14px;
  color: var(--app-text-primary);
  font-weight: 700;
}

.create-mode-toggle {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 14px;
}

.create-mode-toggle__item {
  padding: 13px 15px;
  border: 1px solid rgba(226, 232, 240, 0.94);
  border-radius: 16px;
  background: #ffffff;
  color: #536377;
  font-weight: 700;
  cursor: pointer;
  transition: border-color 160ms ease, background 160ms ease, box-shadow 160ms ease, color 160ms ease;
}

.create-mode-toggle__item[data-active='true'] {
  border-color: rgba(148, 163, 184, 0.72);
  background: rgba(248, 250, 252, 0.98);
  box-shadow: inset 0 0 0 1px rgba(226, 232, 240, 0.88);
  color: #334155;
}

.create-mode-toggle__item:hover {
  border-color: rgba(203, 213, 225, 0.94);
}

.dialog-inline-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: -4px;
  margin-bottom: 14px;
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(248, 250, 252, 0.94);
  border: 1px solid rgba(226, 232, 240, 0.88);
}

.dialog-inline-summary strong {
  font-size: 14px;
  line-height: 1.4;
}

.dialog-inline-summary span {
  color: #475569;
  font-size: 12px;
  font-weight: 600;
  white-space: normal;
  text-align: right;
}

.adjustment-state {
  margin-top: 14px;
  padding: 14px 16px;
  border-radius: 18px;
  border: 1px dashed rgba(203, 213, 225, 0.9);
  background: rgba(248, 250, 252, 0.92);
  color: #475569;
  font-size: 13px;
}

.adjustment-state--empty {
  border-color: rgba(203, 213, 225, 0.9);
  background: rgba(248, 250, 252, 0.92);
  color: #475569;
}

.dialog-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.weekday-chip-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.weekday-auto-state {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 18px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 20px;
  background: rgba(248, 250, 252, 0.94);
}

.weekday-auto-state strong {
  color: #334155;
  font-size: 14px;
  font-weight: 700;
}

.weekday-auto-state span {
  color: #56687d;
  font-size: 12px;
  line-height: 1.6;
}

.weekday-auto-state--group {
  border-color: rgba(226, 232, 240, 0.9);
  background: rgba(248, 250, 252, 0.94);
}

.weekday-auto-state--group strong {
  color: #334155;
}

.weekday-chip {
  padding: 13px 10px;
  border: 1px solid rgba(226, 232, 240, 0.92);
  border-radius: 16px;
  background: #ffffff;
  color: #55667b;
  font-weight: 700;
  cursor: pointer;
  transition: border-color 160ms ease, background 160ms ease, box-shadow 160ms ease, color 160ms ease;
}

.weekday-chip[data-active='true'] {
  border-color: rgba(148, 163, 184, 0.74);
  background: rgba(248, 250, 252, 0.98);
  box-shadow: inset 0 0 0 1px rgba(226, 232, 240, 0.88);
  color: #334155;
}

.weekday-chip:disabled {
  cursor: not-allowed;
  opacity: 0.46;
}

.weekday-chip:not(:disabled):hover {
  border-color: rgba(203, 213, 225, 0.94);
}

.dialog-meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.dialog-meta-pill {
  display: inline-flex;
  align-items: center;
  padding: 7px 11px;
  border-radius: 999px;
  background: rgba(248, 250, 252, 0.98);
  border: 1px solid rgba(226, 232, 240, 0.88);
  color: #5c6d81;
  font-size: 12px;
  font-weight: 600;
}

.adjustment-swap-card {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 8px;
}

.adjustment-swap-card__block {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-height: 112px;
  padding: 16px 18px;
  border-radius: 20px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  background: rgba(248, 250, 252, 0.94);
}

.adjustment-swap-card__block span {
  color: #607287;
  font-size: 12px;
}

.adjustment-swap-card__block strong {
  color: #0f172a;
  font-size: 16px;
  font-weight: 700;
  line-height: 1.5;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.dialog-footer :deep(.el-button) {
  min-width: 112px;
  height: 44px;
  border-radius: 14px;
  font-weight: 700;
  transition: border-color 160ms ease, background 160ms ease, box-shadow 160ms ease;
}

.dialog-footer :deep(.el-button--default) {
  border-color: rgba(226, 232, 240, 0.92);
  background: #ffffff;
  box-shadow: none;
}

.dialog-footer :deep(.el-button--default:hover) {
  border-color: rgba(203, 213, 225, 0.94);
  background: rgba(248, 250, 252, 0.98);
}

.dialog-footer :deep(.el-button--primary) {
  border: none;
  background: #2563eb;
  box-shadow: 0 10px 22px rgba(37, 99, 235, 0.18);
}

.dialog-footer :deep(.el-button--primary:hover) {
  background: #1d4ed8;
  box-shadow: 0 12px 24px rgba(37, 99, 235, 0.22);
}

.dialog-form :deep(.el-select__wrapper),
.dialog-form :deep(.el-input__wrapper),
.dialog-form :deep(.el-textarea__inner),
.dialog-form :deep(.el-date-editor.el-input__wrapper),
.dialog-form :deep(.el-date-editor.el-input__wrapper.is-focus),
.dialog-form :deep(.el-input__wrapper.is-focus),
.dialog-form :deep(.el-select__wrapper.is-focused),
.dialog-form :deep(.el-textarea__inner:focus) {
  border-radius: 14px;
}

.dialog-form :deep(.el-form-item__label) {
  padding-bottom: 8px;
  color: #415269;
  font-size: 13px;
  font-weight: 700;
}

.dialog-form :deep(.el-select__wrapper),
.dialog-form :deep(.el-input__wrapper),
.dialog-form :deep(.el-date-editor.el-input__wrapper) {
  min-height: 48px;
  background: #ffffff;
  box-shadow: inset 0 0 0 1px rgba(226, 232, 240, 0.82);
  transition: box-shadow 160ms ease, background 160ms ease;
}

.dialog-form :deep(.el-select__wrapper:hover),
.dialog-form :deep(.el-input__wrapper:hover),
.dialog-form :deep(.el-date-editor.el-input__wrapper:hover) {
  box-shadow: inset 0 0 0 1px rgba(191, 219, 254, 0.9);
}

.dialog-form :deep(.el-input__wrapper.is-focus),
.dialog-form :deep(.el-select__wrapper.is-focused),
.dialog-form :deep(.el-date-editor.el-input__wrapper.is-focus) {
  background: #ffffff;
  box-shadow:
    0 0 0 4px rgba(191, 219, 254, 0.24),
    0 0 0 1px rgba(96, 165, 250, 0.92) inset;
}

.page-state {
  padding: 18px 20px;
  border: 1px solid rgba(226, 232, 240, 0.92);
  border-radius: 20px;
  background: #ffffff;
  color: #55677c;
  font-size: 14px;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.04);
}

@media (max-width: 1200px) {
  .planner-section__head,
  .dialog-inline-summary,
  .dialog-block__subhead {
    flex-direction: column;
    align-items: flex-start;
  }

  .adjustment-swap-card {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .planner-section {
    padding: 18px;
  }

  .dialog-grid,
  .planner-mode-grid,
  .planner-mode-grid--adjust,
  .weekday-chip-grid,
  .create-mode-toggle {
    grid-template-columns: 1fr;
  }

  .planner-mode {
    min-height: 176px;
  }

  .page-header__actions {
    width: 100%;
  }
}
</style>
