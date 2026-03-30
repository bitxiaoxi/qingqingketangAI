<template>
  <section class="page-stack">
    <PageHeader title="排课管理" />

    <el-card shadow="never" class="feature-card feature-card--planner">
      <div v-if="loading" class="page-state">学生加载中…</div>
      <el-alert v-else-if="error" :title="error" type="error" show-icon :closable="false" />
      <div v-else class="planner-stack">
        <div class="planner-toolbar">
          <h3>选择排课方式</h3>
          <span v-if="selectedCreateStudent" class="planner-student-chip">
            {{ selectedCreateStudent.name }} · {{ selectedCreateStudent.grade }} · 可排 {{ getSchedulableLessons(selectedCreateStudent) }} 节
          </span>
        </div>

        <div class="planner-mode-grid">
          <button type="button" class="planner-mode planner-mode--manual" @click="openCreateDialog()">
            <span class="planner-mode__tag">手动排课</span>
            <strong>固定周期排课</strong>
            <small>按学生、上课日和时间生成</small>
          </button>

          <button type="button" class="planner-mode planner-mode--ai" @click="openAssistantDrawer">
            <span class="planner-mode__tag">AI 排课</span>
            <strong>自然语言排课</strong>
            <small>一句话生成排课草案</small>
          </button>

          <button type="button" class="planner-mode planner-mode--temporary" @click="openAdjustDialog('temporary')">
            <span class="planner-mode__tag">临时加课</span>
            <strong>补入一节课</strong>
            <small>选定一个新时段补课，并自动移除末尾课程</small>
          </button>

          <button type="button" class="planner-mode planner-mode--reschedule" @click="openAdjustDialog('reschedule')">
            <span class="planner-mode__tag">课程改时间</span>
            <strong>平移既有课程</strong>
            <small>把一节待上课程改到新的日期和时段</small>
          </button>

          <button type="button" class="planner-mode planner-mode--future" @click="openAdjustDialog('future')">
            <span class="planner-mode__tag">后续改时间</span>
            <strong>批量改后续课程</strong>
            <small>从一节待上课开始，统一调整后续同周期课程</small>
          </button>
        </div>
      </div>
    </el-card>

    <el-dialog
      v-model="createDialogVisible"
      title="新建排课"
      width="760px"
      destroy-on-close
      class="schedule-dialog"
    >
      <div class="dialog-banner">
        <div>
          <strong>3 步完成手动排课</strong>
          <p>先选学生和周期，再选上课日，最后设置时间段。</p>
        </div>
        <div class="dialog-steps">
          <span class="dialog-step">1. 学生</span>
          <span class="dialog-step">2. 上课日</span>
          <span class="dialog-step">3. 时间</span>
        </div>
      </div>

      <el-form label-position="top" class="dialog-form">
        <section class="dialog-block dialog-block--primary">
          <div class="dialog-block__head">
            <h4>基础信息</h4>
            <small>先确定学生、频率和首次课日期</small>
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
            <strong>排课关系</strong>
            <small>{{ isSameClassCreateMode ? '加入已有班级，允许与指定学生的同一时段课程重叠。' : '默认按单独上课处理，冲突时会拦截。' }}</small>
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

          <div v-if="selectedSameClassStudent" class="dialog-inline-summary dialog-inline-summary--group">
            <strong>与 {{ selectedSameClassStudent.name }} 同班上课</strong>
            <span>系统会从首次课日期起自动沿用对方的班级时段</span>
          </div>

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

          <p class="dialog-field-hint">
            {{ isSameClassCreateMode ? '加入同班后，会从首次课日期起自动沿用对方现有班级课表。' : '每周上课次数会限制可选的上课日数量。' }}
          </p>
        </section>

        <section v-if="!isSameClassCreateMode" class="dialog-block">
          <div class="dialog-block__head">
            <h4>每周上课日</h4>
            <small>{{ weekdaySummary }}</small>
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
            <span class="dialog-meta-pill">已选 {{ createForm.weekdays.length }} 天</span>
            <span class="dialog-meta-pill" :data-muted="remainingWeekdaySlots === 0">
              {{ remainingWeekdaySlots > 0 ? `还可再选 ${remainingWeekdaySlots} 天` : '已达到本周次数' }}
            </span>
          </div>
        </section>

        <section v-if="!isSameClassCreateMode" class="dialog-block">
          <div class="dialog-block__head">
            <h4>上课时间</h4>
            <small>设置单节课标准时段</small>
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

          <p class="dialog-field-hint">建议按固定时段排课，后续查看课程表会更清晰。</p>
        </section>

        <section v-else class="dialog-block">
          <div class="dialog-block__head">
            <h4>同班课表说明</h4>
            <small>无需再手动选择每周次数、上课日和时间。</small>
          </div>

          <div class="weekday-auto-state weekday-auto-state--group">
            <strong>{{ selectedSameClassStudent ? `沿用 ${selectedSameClassStudent.name} 的班级安排` : '先选择同班学生' }}</strong>
            <span>{{ sameClassPreviewHint }}</span>
          </div>
        </section>

        <div class="schedule-preview">
          <span class="schedule-preview__eyebrow">预览</span>
          <strong>{{ createPreview }}</strong>
          <small v-if="selectedCreateStudent">
            {{ selectedSameClassStudent ? `本次将加入 ${selectedSameClassStudent.name} 的同班课程` : `当前可排 ${getSchedulableLessons(selectedCreateStudent)} 节课` }}
          </small>
        </div>
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
      <div class="dialog-banner dialog-banner--adjust">
        <div>
          <strong>{{ adjustBannerTitle }}</strong>
          <p>{{ adjustBannerDescription }}</p>
        </div>
        <div class="dialog-steps">
          <span class="dialog-step">1. 选学生</span>
          <span class="dialog-step">2. {{ adjustMiddleStepLabel }}</span>
          <span class="dialog-step">3. 确认时间</span>
        </div>
      </div>

      <el-form label-position="top" class="dialog-form">
        <section class="dialog-block dialog-block--primary">
          <div class="dialog-block__head">
            <h4>调课对象</h4>
            <small>先选学生，系统会读取该学生当前所有待上课程。</small>
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
            <span>待上 {{ plannedSchedules.length }} 节 · 可排 {{ getSchedulableLessons(selectedAdjustStudent) }} 节</span>
          </div>

          <div class="adjustment-meta-grid">
            <article class="adjustment-meta-card">
              <span>当前待上课程</span>
              <strong>{{ plannedSchedules.length }} 节</strong>
              <small>这里只显示还未销课的正式课。</small>
            </article>
            <article class="adjustment-meta-card">
              <span>{{ adjustLastMetaTitle }}</span>
              <strong>{{ adjustLastSchedule ? formatScheduleOptionLabel(adjustLastSchedule) : '暂未生成正式课表' }}</strong>
              <small>{{ adjustLastMetaHint }}</small>
            </article>
          </div>

          <div v-if="adjustLoadingSchedules" class="adjustment-state">待上课程加载中…</div>
          <div v-else-if="adjustStudentId && !plannedSchedules.length" class="adjustment-state adjustment-state--empty">
            当前没有可调整的待上课程，请先为该学生生成正式课表。
          </div>
        </section>

        <section v-if="adjustMode === 'temporary'" class="dialog-block">
          <div class="dialog-block__head">
            <h4>补入一节临时课</h4>
            <small>适合节假日补课或临时插课，系统会自动把末尾课程腾出来。</small>
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
              <small>这节课会直接插入到你选定的时段。</small>
            </div>
            <div class="adjustment-swap-card__block">
              <span>自动移除</span>
              <strong>{{ adjustLastSchedule ? formatScheduleOptionLabel(adjustLastSchedule) : '暂未找到可替换课程' }}</strong>
              <small>{{ adjustLastSchedule ? '默认移除当前课表最后一节待上课。' : '请先生成正式课表，再执行临时加课。' }}</small>
            </div>
          </div>

          <p class="dialog-field-hint">系统会先校验时间冲突，再完成“补 1 节、减 1 节”的替换动作。</p>
        </section>

        <section v-else-if="adjustMode === 'reschedule'" class="dialog-block">
          <div class="dialog-block__head">
            <h4>把一节课改到新时间</h4>
            <small>适合学生请假、临时顺延或与家长重新约课。</small>
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

          <div class="adjustment-swap-card">
            <div class="adjustment-swap-card__block">
              <span>当前时间</span>
              <strong>{{ selectedRescheduleSchedule ? formatScheduleOptionLabel(selectedRescheduleSchedule) : '请选择一节课程' }}</strong>
              <small>只能调整待上课程，已销课程请先撤销销课。</small>
            </div>
            <div class="adjustment-swap-card__block">
              <span>调整后</span>
              <strong>{{ rescheduleTargetLabel }}</strong>
              <small>系统会检查和其他课程是否冲突。</small>
            </div>
          </div>

          <p class="dialog-field-hint">课程改时间不会新增或减少课时，只会更新这节课的具体落位。</p>
        </section>

        <section v-else class="dialog-block">
          <div class="dialog-block__head">
            <h4>把后续课程统一改到新时间</h4>
            <small>适合固定上课时段整体变更，已销课课程不会受影响。</small>
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

          <div class="adjustment-swap-card">
            <div class="adjustment-swap-card__block">
              <span>当前起点</span>
              <strong>{{ selectedFutureRescheduleSchedule ? formatScheduleOptionLabel(selectedFutureRescheduleSchedule) : '请选择一节课程' }}</strong>
              <small>系统会从这节课开始，匹配后续同一固定时段的待上课程。</small>
            </div>
            <div class="adjustment-swap-card__block">
              <span>调整后首节</span>
              <strong>{{ futureRescheduleTargetLabel }}</strong>
              <small>{{ futureRescheduleCandidates.length ? `预计同步更新 ${futureRescheduleCandidates.length} 节待上课程` : '请选择生效起点课程后再预估影响范围' }}</small>
            </div>
          </div>

          <p class="dialog-field-hint">系统只会修改这节课及其后续同一固定时段的待上课程，不会影响已经销课的记录。</p>
        </section>

        <div class="schedule-preview schedule-preview--adjust">
          <span class="schedule-preview__eyebrow">操作预览</span>
          <strong>{{ adjustPreview }}</strong>
          <small>{{ adjustPreviewHint }}</small>
        </div>
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

    <el-drawer
      v-model="assistantDrawerVisible"
      size="560px"
      title="AI 排课"
      destroy-on-close
      class="assistant-drawer"
    >
      <div class="assistant-stack">
        <div class="assistant-examples">
          <button
            v-for="example in scheduleAssistantExamples"
            :key="example"
            type="button"
            class="assistant-example"
            @click="assistantInput = example"
          >
            {{ example }}
          </button>
        </div>

        <div ref="assistantThreadRef" class="assistant-thread">
          <article
            v-for="message in assistantMessages"
            :key="message.id"
            class="assistant-message"
            :data-role="message.role"
          >
            <div class="assistant-message__meta">
              <span>{{ message.role === 'assistant' ? '排课助手' : '你' }}</span>
              <small v-if="message.meta">{{ message.meta }}</small>
            </div>
            <div class="assistant-message__bubble">
              <p>{{ message.content }}</p>
              <ul v-if="message.warnings?.length">
                <li v-for="warning in message.warnings" :key="`${message.id}-${warning}`">{{ warning }}</li>
              </ul>
            </div>
          </article>
        </div>

        <el-input
          v-model="assistantInput"
          type="textarea"
          :rows="4"
          :placeholder="assistantPlaceholder"
        />

        <div class="assistant-actions">
          <span class="field-hint">
            {{ assistantPendingFields.length ? `当前待补：${assistantPendingFields.join('、')}` : '支持多轮补充，系统会记住上下文。' }}
          </span>
          <div class="assistant-actions__buttons">
            <el-button @click="resetAssistantConversation">重置</el-button>
            <el-button type="primary" :loading="assistantSubmitting" @click="submitAssistant">
              发送并排课
            </el-button>
          </div>
        </div>
      </div>
    </el-drawer>
  </section>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import PageHeader from '../components/common/PageHeader.vue';
import { frequencyOptions, scheduleAssistantExamples, weekdayOptions } from '../constants/options';
import { api } from '../services/api';
import {
  buildWeekdaySummary,
  formatClock,
  formatDateParam,
  formatLongDate,
  formatParsedIntentSummary,
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
const assistantDrawerVisible = ref(false);
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

const assistantThreadRef = ref(null);
const assistantMessages = ref([]);
const assistantInput = ref('');
const assistantPendingFields = ref([]);
const assistantSubmitting = ref(false);
const allWeekdayValues = weekdayOptions.map((option) => option.value);
let assistantMessageId = 0;

const getSchedulableLessons = (student) => {
  return Math.max(0, Number(student?.schedulableLessons ?? student?.remainingLessons ?? 0));
};

const createAssistantMessage = (role, content, options = {}) => {
  assistantMessageId += 1;
  return {
    id: assistantMessageId,
    role,
    content,
    meta: options.meta ?? '',
    warnings: options.warnings ?? []
  };
};

const resetAssistantConversation = async () => {
  assistantMessages.value = [
    createAssistantMessage(
      'assistant',
      '告诉我学生姓名、每周上课日、时间段和开始日期。你可以分多轮补充，我会记住已经识别到的信息。',
      { meta: 'AI 对话排课' }
    )
  ];
  assistantInput.value = '';
  assistantPendingFields.value = [];
  await scrollAssistantThread();
};

const scrollAssistantThread = async () => {
  await nextTick();
  const target = assistantThreadRef.value;
  if (target) {
    target.scrollTop = target.scrollHeight;
  }
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
const weekdaySummary = computed(() => {
  if (isDailySchedule.value) {
    return '每天上课 · 自动包含周一到周日';
  }
  return buildWeekdaySummary(normalizedCreateWeekdays.value, createForm.weeklySessions, weekdayOptions);
});
const remainingWeekdaySlots = computed(() => {
  if (isDailySchedule.value) {
    return 0;
  }
  return Math.max(0, createForm.weeklySessions - normalizedCreateWeekdays.value.length);
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
const adjustLastSchedule = computed(() => {
  return plannedSchedules.value[plannedSchedules.value.length - 1] ?? null;
});
const adjustLastMetaTitle = computed(() => {
  return adjustMode.value === 'temporary' ? '课表最后一节' : '当前最晚一节';
});
const adjustLastMetaHint = computed(() => {
  if (adjustMode.value === 'temporary') {
    return '临时加课时，系统会自动移除这一节。';
  }
  if (adjustMode.value === 'future') {
    return '批量改时间只会调整匹配到的后续待上课程。';
  }
  return '这里只做当前课表参考，不会自动移除。';
});
const selectedRescheduleSchedule = computed(() => {
  return plannedSchedules.value.find((schedule) => schedule.id === rescheduleForm.scheduleId) ?? null;
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

const createPreview = computed(() => {
  const studentLabel = selectedCreateStudent.value
    ? `${selectedCreateStudent.value.name} · ${selectedCreateStudent.value.grade}`
    : '未选择学生';
  if (isSameClassCreateMode.value) {
    const firstLessonLabel = createForm.startDate
      ? `从 ${createForm.startDate} 起`
      : '未设置首次课日期';
    const classLabel = selectedSameClassStudent.value
      ? `加入 ${selectedSameClassStudent.value.name} 的同班课程`
      : '未选择同班学生';
    return `${studentLabel} · ${classLabel} · ${sameClassSlotSummary.value} · ${firstLessonLabel}`;
  }
  const weekdays = weekdayOptions
    .filter((option) => normalizedCreateWeekdays.value.includes(option.value))
    .map((option) => option.label)
    .join('、') || '未选择上课日';
  const timeRange = createForm.startTime && createForm.endTime
    ? `${createForm.startTime}-${createForm.endTime}`
    : '未设置时间';
  const firstLessonLabel = createForm.startDate
    ? `首次课 ${createForm.startDate}`
    : '未设置首次课日期';
  const classLabel = selectedSameClassStudent.value
    ? `与 ${selectedSameClassStudent.value.name} 同班`
    : '单独上课';
  return `${studentLabel} · ${classLabel} · ${weekdays} · ${timeRange} · ${firstLessonLabel}`;
});
const sameClassPreviewHint = computed(() => {
  if (!selectedSameClassStudent.value) {
    return '选择同班学生后，系统会直接沿用对方已排好的班级时段。';
  }
  if (loadingSameClassSchedules.value) {
    return '正在读取对方班级课表，马上展示具体时段。';
  }
  if (!sameClassSlotTemplates.value.length) {
    return '当前还没有读取到可沿用的班级时段，请先确认对方已有待上课表。';
  }
  return `从 ${createForm.startDate || '所选日期'} 起，系统会按 ${sameClassSlotSummary.value} 自动补入同班课程。`;
});
const temporaryTargetLabel = computed(() => {
  return buildSlotLabel(temporaryForm.lessonDate, temporaryForm.startTime, temporaryForm.endTime);
});
const rescheduleTargetLabel = computed(() => {
  return buildSlotLabel(rescheduleForm.lessonDate, rescheduleForm.startTime, rescheduleForm.endTime);
});
const futureRescheduleTargetLabel = computed(() => {
  return buildSlotLabel(futureRescheduleForm.lessonDate, futureRescheduleForm.startTime, futureRescheduleForm.endTime);
});
const temporaryPreview = computed(() => {
  const studentLabel = selectedAdjustStudent.value
    ? `${selectedAdjustStudent.value.name} · ${selectedAdjustStudent.value.grade}`
    : '未选择学生';
  const removedLabel = adjustLastSchedule.value
    ? formatScheduleOptionLabel(adjustLastSchedule.value)
    : '暂未找到可替换课程';
  return `${studentLabel} · 补入 ${temporaryTargetLabel.value} · 末尾移除 ${removedLabel}`;
});
const temporaryPreviewHint = computed(() => {
  if (!selectedAdjustStudent.value) {
    return '先选学生，再设置临时补课时间。';
  }
  if (!adjustLastSchedule.value) {
    return '该学生当前没有待上课程，无法执行“补 1 节、减 1 节”的替换。';
  }
  return '系统会保持总课时不变，并自动校验是否与其他课程冲突。';
});
const reschedulePreview = computed(() => {
  const studentLabel = selectedAdjustStudent.value
    ? `${selectedAdjustStudent.value.name} · ${selectedAdjustStudent.value.grade}`
    : '未选择学生';
  const sourceLabel = selectedRescheduleSchedule.value
    ? formatScheduleOptionLabel(selectedRescheduleSchedule.value)
    : '未选择待调整课程';
  return `${studentLabel} · 从 ${sourceLabel} 调整到 ${rescheduleTargetLabel.value}`;
});
const reschedulePreviewHint = computed(() => {
  if (!selectedAdjustStudent.value) {
    return '先选学生，再选择需要改时间的课程。';
  }
  if (!selectedRescheduleSchedule.value) {
    return '请选择一节待上课程作为改时间对象。';
  }
  return '改时间只会调整这一节课的日期和时段，不会新增或减少课时。';
});
const futureReschedulePreview = computed(() => {
  const studentLabel = selectedAdjustStudent.value
    ? `${selectedAdjustStudent.value.name} · ${selectedAdjustStudent.value.grade}`
    : '未选择学生';
  const sourceLabel = selectedFutureRescheduleSchedule.value
    ? formatScheduleOptionLabel(selectedFutureRescheduleSchedule.value)
    : '未选择生效起点课程';
  const affectedCount = futureRescheduleCandidates.value.length;
  return `${studentLabel} · 从 ${sourceLabel} 开始，统一改到 ${futureRescheduleTargetLabel.value} · 预计影响 ${affectedCount} 节待上课`;
});
const futureReschedulePreviewHint = computed(() => {
  if (!selectedAdjustStudent.value) {
    return '先选学生，再选择后续课程改时间的生效起点。';
  }
  if (!selectedFutureRescheduleSchedule.value) {
    return '请选择一节待上课程作为后续改时间的起点。';
  }
  if (!futureRescheduleCandidates.value.length) {
    return '当前没有可批量调整的后续课程。';
  }
  return '系统会只修改所选课程及其后续同一固定时段的待上课程，已销课课程不受影响。';
});
const adjustDialogTitle = computed(() => {
  if (adjustMode.value === 'temporary') {
    return '临时加课';
  }
  if (adjustMode.value === 'future') {
    return '后续课程改时间';
  }
  return '课程改时间';
});
const adjustBannerTitle = computed(() => {
  if (adjustMode.value === 'temporary') {
    return '临时加课，不改总课时';
  }
  if (adjustMode.value === 'future') {
    return '后续统一改时间，不动已销课';
  }
  return '课程改时间，直接更新落位';
});
const adjustBannerDescription = computed(() => {
  if (adjustMode.value === 'temporary') {
    return '补一节临时课时，系统会自动移除末尾课程，适合补课和临时插课。';
  }
  if (adjustMode.value === 'future') {
    return '从一节待上课开始，统一调整后续同一固定时段的课程，适合整体换上课时间。';
  }
  return '把一节待上课程直接改到新的日期和时段，适合请假顺延或重新约课。';
});
const adjustMiddleStepLabel = computed(() => {
  if (adjustMode.value === 'temporary') {
    return '设时段';
  }
  return '选课程';
});
const adjustSubmitLabel = computed(() => {
  if (adjustMode.value === 'temporary') {
    return '确认临时加课';
  }
  if (adjustMode.value === 'future') {
    return '确认批量改时间';
  }
  return '确认改时间';
});
const adjustPreview = computed(() => {
  if (adjustMode.value === 'temporary') {
    return temporaryPreview.value;
  }
  if (adjustMode.value === 'future') {
    return futureReschedulePreview.value;
  }
  return reschedulePreview.value;
});
const adjustPreviewHint = computed(() => {
  if (adjustMode.value === 'temporary') {
    return temporaryPreviewHint.value;
  }
  if (adjustMode.value === 'future') {
    return futureReschedulePreviewHint.value;
  }
  return reschedulePreviewHint.value;
});

const assistantPlaceholder = computed(() => {
  if (assistantPendingFields.value.includes('学生姓名')) {
    return '直接回复学生姓名，例如：李晓明';
  }
  if (assistantPendingFields.value.includes('每周上课日')) {
    return '直接回复每周上课日，例如：周二、周四';
  }
  if (assistantPendingFields.value.includes('开始日期')) {
    return '直接回复开始日期，例如：从 2026-03-16 开始';
  }
  if (assistantPendingFields.value.includes('上课时间')) {
    return '直接回复上课时间，例如：19:00-20:30';
  }
  return '例如：李晓明每周二、周四 19:00-20:30，从下周一开始上课';
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
  if (adjustMode.value === 'future') {
    await submitFutureReschedule();
    return;
  }
  await submitReschedule();
};

const openAssistantDrawer = async () => {
  assistantDrawerVisible.value = true;
  if (!assistantMessages.value.length) {
    await resetAssistantConversation();
    return;
  }
  await scrollAssistantThread();
};

const submitAssistant = async () => {
  if (!assistantInput.value.trim()) {
    return;
  }

  const userMessage = createAssistantMessage('user', assistantInput.value.trim());
  assistantMessages.value = [...assistantMessages.value, userMessage];
  assistantInput.value = '';
  assistantSubmitting.value = true;
  await scrollAssistantThread();

  try {
    const data = await api.assistantArrange(
      assistantMessages.value.map((message) => ({
        role: message.role,
        content: message.content
      }))
    );

    assistantPendingFields.value = Array.isArray(data?.parsedIntent?.missingFields)
      ? data.parsedIntent.missingFields
      : [];

    const metaParts = [];
    if (data?.analysisMode === 'AI') {
      metaParts.push('AI 解析');
    } else if (data?.analysisMode) {
      metaParts.push('规则解析');
    }
    const parsedSummary = formatParsedIntentSummary(data?.parsedIntent);
    if (parsedSummary) {
      metaParts.push(parsedSummary);
    }
    if (data?.scheduled) {
      metaParts.push(`已排 ${data.scheduledCount ?? 0} 节`);
      assistantPendingFields.value = [];
    }

    assistantMessages.value = [
      ...assistantMessages.value,
      createAssistantMessage('assistant', data?.reply ?? '已收到排课请求。', {
        meta: metaParts.join(' · '),
        warnings: data?.warnings ?? []
      })
    ];
    await scrollAssistantThread();

    if (data?.scheduled) {
      await loadStudents();
      ElMessage.success(`已生成 ${data.scheduledCount ?? 0} 节课`);
    }
  } catch (requestError) {
    const message = normalizeError(requestError, '智能排课失败');
    assistantMessages.value = [
      ...assistantMessages.value,
      createAssistantMessage('assistant', message, { meta: '系统提示' })
    ];
    await scrollAssistantThread();
    ElMessage.error(message);
  } finally {
    assistantSubmitting.value = false;
  }
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
  await resetAssistantConversation();
});
</script>

<style scoped>
.feature-card {
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
}

.feature-card--planner {
  position: relative;
  overflow: hidden;
}

.feature-card--planner::before {
  content: '';
  position: absolute;
  inset: 0 0 auto 0;
  height: 160px;
  background:
    radial-gradient(circle at top right, rgba(125, 211, 252, 0.18), transparent 26%),
    linear-gradient(135deg, rgba(59, 130, 246, 0.08), transparent 52%);
  pointer-events: none;
}

.feature-card :deep(.el-card__body) {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.planner-stack {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.planner-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.planner-toolbar h3 {
  font-size: 24px;
  line-height: 1.12;
  letter-spacing: -0.03em;
}

.planner-student-chip {
  display: inline-flex;
  align-items: center;
  max-width: 100%;
  padding: 10px 14px;
  border: 1px solid rgba(191, 219, 254, 0.9);
  border-radius: 999px;
  background: rgba(239, 246, 255, 0.92);
  color: #1e3a8a;
  font-size: 13px;
  white-space: normal;
}

.planner-mode-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 14px;
}

.planner-mode {
  padding: 24px;
  border: 1px solid rgba(226, 232, 240, 0.92);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.92);
  text-align: left;
  cursor: pointer;
  box-shadow: 0 14px 28px rgba(15, 23, 42, 0.04);
  transition: transform 180ms ease, border-color 180ms ease, box-shadow 180ms ease, background 180ms ease;
}

.planner-mode:hover {
  transform: translateY(-2px);
  box-shadow: 0 18px 34px rgba(15, 23, 42, 0.08);
}

.planner-mode__tag {
  display: inline-flex;
  margin-bottom: 14px;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.66);
  color: var(--app-text-secondary);
  font-size: 12px;
  letter-spacing: 0.08em;
  line-height: 1;
}

.planner-mode strong {
  display: block;
  margin-bottom: 8px;
  font-size: 22px;
  line-height: 1.2;
}

.planner-mode small {
  display: block;
  color: var(--app-text-secondary);
  line-height: 1.6;
}

.planner-mode--manual {
  border-color: rgba(147, 197, 253, 0.72);
  background: linear-gradient(145deg, rgba(219, 234, 254, 0.78), rgba(255, 255, 255, 0.96));
}

.planner-mode--ai {
  border-color: rgba(253, 186, 116, 0.72);
  background: linear-gradient(145deg, rgba(255, 237, 213, 0.78), rgba(255, 255, 255, 0.96));
}

.planner-mode--temporary {
  border-color: rgba(110, 231, 183, 0.78);
  background: linear-gradient(145deg, rgba(220, 252, 231, 0.84), rgba(255, 255, 255, 0.96));
}

.planner-mode--reschedule {
  border-color: rgba(196, 181, 253, 0.78);
  background: linear-gradient(145deg, rgba(237, 233, 254, 0.84), rgba(255, 255, 255, 0.96));
}

.planner-mode--future {
  border-color: rgba(251, 191, 36, 0.78);
  background: linear-gradient(145deg, rgba(254, 243, 199, 0.84), rgba(255, 255, 255, 0.96));
}

.schedule-dialog :deep(.el-dialog) {
  border-radius: 28px;
}

.schedule-dialog :deep(.el-dialog__body),
.assistant-drawer :deep(.el-drawer__body) {
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
}

.schedule-preview__eyebrow {
  display: inline-flex;
  margin-bottom: 8px;
  color: var(--app-text-tertiary);
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.dialog-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
  padding: 18px 20px;
  border: 1px solid rgba(191, 219, 254, 0.92);
  border-radius: 22px;
  background: linear-gradient(135deg, rgba(239, 246, 255, 0.98), rgba(255, 255, 255, 0.94));
}

.dialog-banner strong {
  display: block;
  margin-bottom: 4px;
  font-size: 18px;
}

.dialog-banner--adjust {
  border-color: rgba(167, 243, 208, 0.9);
  background: linear-gradient(135deg, rgba(236, 253, 245, 0.98), rgba(255, 255, 255, 0.94));
}

.dialog-banner p {
  color: var(--app-text-secondary);
  line-height: 1.6;
}

.dialog-steps {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.dialog-step {
  display: inline-flex;
  align-items: center;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(191, 219, 254, 0.88);
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 700;
}

.dialog-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.dialog-block {
  padding: 18px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.92);
}

.dialog-block--primary {
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.98), rgba(248, 250, 252, 0.96));
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.62);
}

.dialog-block__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.dialog-block__head h4 {
  margin: 0;
  font-size: 16px;
}

.dialog-block__head small {
  margin: 0;
  color: var(--app-text-secondary);
  line-height: 1.6;
}

.dialog-block__subhead {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-top: 2px;
  margin-bottom: 12px;
}

.dialog-block__subhead strong {
  font-size: 14px;
  color: var(--app-text-primary);
}

.dialog-block__subhead small {
  color: var(--app-text-secondary);
  line-height: 1.6;
}

.create-mode-toggle {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 14px;
}

.create-mode-toggle__item {
  padding: 12px 14px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.92);
  color: var(--app-text-secondary);
  cursor: pointer;
  transition: border-color 180ms ease, background 180ms ease, transform 180ms ease, box-shadow 180ms ease;
}

.create-mode-toggle__item[data-active='true'] {
  border-color: rgba(59, 130, 246, 0.36);
  background: rgba(219, 234, 254, 0.82);
  box-shadow: inset 0 0 0 1px rgba(191, 219, 254, 0.72);
  color: var(--app-primary);
}

.create-mode-toggle__item:hover {
  transform: translateY(-1px);
}

.dialog-inline-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: -4px;
  margin-bottom: 14px;
  padding: 12px 14px;
  border-radius: 16px;
  background: rgba(239, 246, 255, 0.82);
  border: 1px solid rgba(191, 219, 254, 0.88);
}

.dialog-inline-summary strong {
  font-size: 14px;
  line-height: 1.4;
}

.dialog-inline-summary span {
  color: #1d4ed8;
  font-size: 12px;
  white-space: nowrap;
}

.dialog-inline-summary--group {
  background: rgba(236, 253, 245, 0.92);
  border-color: rgba(167, 243, 208, 0.92);
}

.dialog-inline-summary--group span {
  color: #047857;
}

.adjustment-meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.adjustment-meta-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px 16px;
  border-radius: 18px;
  border: 1px solid rgba(226, 232, 240, 0.88);
  background: rgba(248, 250, 252, 0.92);
}

.adjustment-meta-card span {
  color: var(--app-text-secondary);
  font-size: 12px;
}

.adjustment-meta-card strong {
  font-size: 15px;
  line-height: 1.5;
  color: var(--app-text-primary);
}

.adjustment-meta-card small {
  color: var(--app-text-tertiary);
  line-height: 1.6;
}

.adjustment-state {
  margin-top: 14px;
  padding: 14px 16px;
  border-radius: 18px;
  border: 1px dashed rgba(191, 219, 254, 0.92);
  background: rgba(239, 246, 255, 0.72);
  color: #1d4ed8;
  font-size: 13px;
}

.adjustment-state--empty {
  border-color: rgba(253, 186, 116, 0.88);
  background: rgba(255, 247, 237, 0.92);
  color: #c2410c;
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
  padding: 14px 16px;
  border: 1px solid rgba(191, 219, 254, 0.88);
  border-radius: 18px;
  background: linear-gradient(145deg, rgba(239, 246, 255, 0.92), rgba(255, 255, 255, 0.96));
}

.weekday-auto-state strong {
  color: #1d4ed8;
  font-size: 14px;
}

.weekday-auto-state span {
  color: var(--app-text-secondary);
  font-size: 12px;
  line-height: 1.6;
}

.weekday-auto-state--group {
  border-color: rgba(167, 243, 208, 0.92);
  background: linear-gradient(145deg, rgba(236, 253, 245, 0.94), rgba(255, 255, 255, 0.98));
}

.weekday-auto-state--group strong {
  color: #047857;
}

.weekday-chip {
  padding: 12px 10px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.92);
  color: var(--app-text-secondary);
  cursor: pointer;
  transition: border-color 180ms ease, background 180ms ease, transform 180ms ease, box-shadow 180ms ease;
}

.weekday-chip[data-active='true'] {
  border-color: rgba(59, 130, 246, 0.34);
  background: rgba(219, 234, 254, 0.78);
  box-shadow: inset 0 0 0 1px rgba(191, 219, 254, 0.65);
  color: var(--app-primary);
}

.weekday-chip:disabled {
  cursor: not-allowed;
  opacity: 0.46;
}

.weekday-chip:not(:disabled):hover {
  transform: translateY(-1px);
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
  padding: 7px 10px;
  border-radius: 999px;
  background: rgba(248, 250, 252, 0.96);
  border: 1px solid rgba(226, 232, 240, 0.88);
  color: var(--app-text-secondary);
  font-size: 12px;
}

.dialog-meta-pill[data-muted='true'] {
  color: #1d4ed8;
  border-color: rgba(191, 219, 254, 0.88);
  background: rgba(239, 246, 255, 0.88);
}

.dialog-field-hint {
  margin-top: 12px;
  color: var(--app-text-secondary);
  font-size: 12px;
  line-height: 1.7;
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
  min-height: 126px;
  padding: 16px 18px;
  border-radius: 20px;
  border: 1px solid rgba(191, 219, 254, 0.88);
  background: linear-gradient(145deg, rgba(239, 246, 255, 0.92), rgba(255, 255, 255, 0.96));
}

.adjustment-swap-card__block span {
  color: var(--app-text-secondary);
  font-size: 12px;
}

.adjustment-swap-card__block strong {
  color: #0f172a;
  font-size: 16px;
  line-height: 1.6;
}

.adjustment-swap-card__block small {
  color: var(--app-text-tertiary);
  line-height: 1.6;
}

.schedule-preview {
  padding: 18px;
  border: 1px solid rgba(191, 219, 254, 0.9);
  border-radius: 20px;
  background: linear-gradient(145deg, #0f172a, #1e3a8a);
  color: rgba(255, 255, 255, 0.94);
}

.schedule-preview strong {
  display: block;
  margin-bottom: 6px;
  font-size: 17px;
  line-height: 1.6;
}

.schedule-preview small {
  color: rgba(255, 255, 255, 0.68);
  line-height: 1.7;
}

.schedule-preview--adjust {
  margin-top: 2px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.field-hint {
  display: inline-flex;
  color: var(--app-text-secondary);
  font-size: 12px;
  line-height: 1.6;
}

.assistant-stack {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.assistant-examples {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.assistant-example {
  padding: 10px 12px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  color: var(--app-text-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: border-color 180ms ease, transform 180ms ease, background 180ms ease;
}

.assistant-example:hover {
  transform: translateY(-1px);
  border-color: rgba(191, 219, 254, 0.88);
  background: rgba(239, 246, 255, 0.9);
}

.assistant-thread {
  max-height: 360px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 6px;
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.92);
}

.assistant-message {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.assistant-message__meta {
  display: flex;
  gap: 8px;
  color: var(--app-text-tertiary);
  font-size: 12px;
}

.assistant-message__bubble {
  padding: 12px 14px;
  border-radius: 16px;
  background: rgba(248, 250, 252, 0.98);
  color: var(--app-text-primary);
  line-height: 1.7;
}

.assistant-message[data-role='user'] .assistant-message__bubble {
  background: rgba(219, 234, 254, 0.92);
}

.assistant-message__bubble ul {
  margin-top: 8px;
  padding-left: 18px;
  color: var(--app-text-secondary);
}

.assistant-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.assistant-actions__buttons {
  display: flex;
  gap: 12px;
}

.dialog-form :deep(.el-select__wrapper),
.dialog-form :deep(.el-input__wrapper),
.dialog-form :deep(.el-textarea__inner),
.dialog-form :deep(.el-date-editor.el-input__wrapper),
.dialog-form :deep(.el-date-editor.el-input__wrapper.is-focus),
.dialog-form :deep(.el-input__wrapper.is-focus),
.dialog-form :deep(.el-select__wrapper.is-focused),
.dialog-form :deep(.el-textarea__inner:focus),
.assistant-stack :deep(.el-textarea__inner) {
  border-radius: 16px;
}

.dialog-form :deep(.el-select__wrapper),
.dialog-form :deep(.el-input__wrapper),
.dialog-form :deep(.el-date-editor.el-input__wrapper) {
  min-height: 46px;
  background: rgba(248, 250, 252, 0.92);
  box-shadow: none;
}

.dialog-form :deep(.el-input__wrapper.is-focus),
.dialog-form :deep(.el-select__wrapper.is-focused),
.dialog-form :deep(.el-date-editor.el-input__wrapper.is-focus) {
  background: #ffffff;
  box-shadow: 0 0 0 1px rgba(147, 197, 253, 0.82) inset;
}

.assistant-stack :deep(.el-textarea__inner) {
  min-height: 112px;
  padding: 14px 16px;
}

.page-state {
  color: var(--app-text-secondary);
  font-size: 14px;
}

@media (max-width: 1200px) {
  .assistant-actions,
  .planner-toolbar,
  .dialog-banner,
  .dialog-inline-summary,
  .dialog-block__subhead {
    flex-direction: column;
    align-items: flex-start;
  }

  .adjustment-meta-grid,
  .adjustment-swap-card {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .dialog-grid,
  .planner-mode-grid,
  .weekday-chip-grid,
  .create-mode-toggle {
    grid-template-columns: 1fr;
  }

  .assistant-actions {
    width: 100%;
    flex-direction: column;
    align-items: flex-start;
  }

  .page-header__actions {
    width: 100%;
  }
}
</style>
