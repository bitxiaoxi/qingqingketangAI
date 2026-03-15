<template>
  <section class="page-stack">
    <el-card shadow="never" class="schedule-board-card">
      <div v-if="loading" class="page-state">课表加载中…</div>
      <el-alert v-else-if="error" :title="error" type="error" show-icon :closable="false" />
      <div v-else class="schedule-poster-section">
        <div class="schedule-poster-frame">
          <div class="schedule-poster-toolbar">
            <el-button class="schedule-print-button" :disabled="!schedulePosterUrl" @click="printSchedulePoster">
              打印课表
            </el-button>
          </div>
          <el-image
            v-if="schedulePosterUrl"
            :src="schedulePosterUrl"
            :preview-src-list="[schedulePosterUrl]"
            class="schedule-poster-image"
            fit="contain"
            preview-teleported
            hide-on-click-modal
          />
          <el-empty v-else description="本周暂无排课安排" :image-size="72" />
        </div>
      </div>
    </el-card>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { api } from '../services/api';
import {
  formatClock,
  formatDate,
  formatDateParam,
  formatTimeRange,
  formatWeekRange,
  formatWeekday,
  getWeekDays,
  getWeekStart,
  normalizeError
} from '../utils/format';

const loading = ref(false);
const error = ref('');
const schedules = ref([]);
const currentWeekStart = ref(getWeekStart(new Date()));

const escapeSvgText = (value) => {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
};

const truncateText = (value, maxLength) => {
  const text = String(value ?? '');
  if (text.length <= maxLength) {
    return text;
  }
  return `${text.slice(0, Math.max(0, maxLength - 1))}…`;
};

const getMinutesOfDay = (value) => {
  const date = new Date(value);
  return date.getHours() * 60 + date.getMinutes();
};

const formatHourMark = (minutes) => {
  const hours = Math.floor(minutes / 60);
  return `${String(hours).padStart(2, '0')}:00`;
};

const createSvgImageUrl = (svg) => {
  return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`;
};

const printSchedulePoster = () => {
  if (!schedulePosterUrl.value || typeof window === 'undefined' || typeof document === 'undefined') {
    return;
  }

  const printFrame = document.createElement('iframe');
  printFrame.setAttribute('aria-hidden', 'true');
  printFrame.style.position = 'fixed';
  printFrame.style.width = '0';
  printFrame.style.height = '0';
  printFrame.style.border = '0';
  printFrame.style.right = '0';
  printFrame.style.bottom = '0';
  document.body.appendChild(printFrame);

  const cleanup = () => {
    window.setTimeout(() => {
      if (document.body.contains(printFrame)) {
        document.body.removeChild(printFrame);
      }
    }, 300);
  };

  const frameWindow = printFrame.contentWindow;
  if (!frameWindow) {
    cleanup();
    return;
  }

  const posterTitle = escapeSvgText(`本周课程表 ${currentWeekLabel.value}`);
  frameWindow.document.open();
  frameWindow.document.write(`
    <!doctype html>
    <html lang="zh-CN">
      <head>
        <meta charset="UTF-8" />
        <title>${posterTitle}</title>
        <style>
          @page {
            size: landscape;
            margin: 6mm;
          }

          html,
          body {
            margin: 0;
            padding: 0;
            background: #ffffff;
            width: 100%;
            height: 100%;
            overflow: hidden;
            -webkit-print-color-adjust: exact;
            print-color-adjust: exact;
          }

          .print-sheet {
            display: flex;
            align-items: center;
            justify-content: center;
            width: 100vw;
            height: 100vh;
            overflow: hidden;
          }

          img {
            display: block;
            width: auto;
            height: auto;
            max-width: 100%;
            max-height: 100%;
            object-fit: contain;
            break-inside: avoid;
            page-break-inside: avoid;
          }
        </style>
      </head>
      <body>
        <div class="print-sheet">
          <img id="poster" src="${schedulePosterUrl.value}" alt="${posterTitle}" />
        </div>
      </body>
    </html>
  `);
  frameWindow.document.close();

  const printImage = frameWindow.document.getElementById('poster');
  const triggerPrint = () => {
    frameWindow.focus();
    frameWindow.print();
  };

  frameWindow.onafterprint = cleanup;

  if (printImage?.complete) {
    window.setTimeout(triggerPrint, 120);
    return;
  }

  printImage?.addEventListener('load', () => {
    window.setTimeout(triggerPrint, 120);
  }, { once: true });

  printImage?.addEventListener('error', cleanup, { once: true });
};

const loadSchedules = async () => {
  loading.value = true;
  error.value = '';
  try {
    schedules.value = await api.listWeekSchedules(formatDateParam(currentWeekStart.value));
  } catch (requestError) {
    error.value = normalizeError(requestError, '课表加载失败');
  } finally {
    loading.value = false;
  }
};

const normalizedSchedules = computed(() => {
  return (schedules.value ?? [])
    .slice()
    .sort((left, right) => new Date(left.startTime) - new Date(right.startTime))
    .map((schedule) => {
      const isCompleted = String(schedule.status ?? '').toUpperCase() === 'COMPLETED';
      return {
        ...schedule,
        scheduleId: schedule.id,
        studentName: schedule.studentName ?? '未命名',
        subject: schedule.subject ?? '正式课',
        timeRange: formatTimeRange(schedule.startTime, schedule.endTime),
        isCompleted
      };
    });
});

const groupedSchedules = computed(() => {
  const groupMap = new Map();

  normalizedSchedules.value.forEach((schedule) => {
    const dateKey = formatDateParam(schedule.startTime);
    const groupKey = [dateKey, schedule.startTime, schedule.endTime, schedule.subject].join('|');
    const existingGroup = groupMap.get(groupKey);

    if (existingGroup) {
      existingGroup.scheduleIds.push(schedule.scheduleId);
      existingGroup.studentIds.add(schedule.studentId ?? schedule.scheduleId);
      existingGroup.studentNames.add(schedule.studentName);
      if (schedule.isCompleted) {
        existingGroup.completedScheduleIds.push(schedule.scheduleId);
      } else {
        existingGroup.pendingScheduleIds.push(schedule.scheduleId);
      }
      return;
    }

    groupMap.set(groupKey, {
      groupKey,
      dateKey,
      startTime: schedule.startTime,
      endTime: schedule.endTime,
      sortTime: new Date(schedule.startTime).getTime(),
      timeRange: schedule.timeRange,
      subject: schedule.subject,
      scheduleIds: [schedule.scheduleId],
      completedScheduleIds: schedule.isCompleted ? [schedule.scheduleId] : [],
      pendingScheduleIds: schedule.isCompleted ? [] : [schedule.scheduleId],
      studentIds: new Set([schedule.studentId ?? schedule.scheduleId]),
      studentNames: new Set([schedule.studentName])
    });
  });

  return Array.from(groupMap.values())
    .map((group) => {
      const studentNames = Array.from(group.studentNames);
      const participantCount = group.studentIds.size || group.scheduleIds.length;
      const completedCount = group.completedScheduleIds.length;
      const pendingCount = group.pendingScheduleIds.length;
      const isCompleted = completedCount === group.scheduleIds.length;
      const isPartial = completedCount > 0 && pendingCount > 0;

      return {
        groupKey: group.groupKey,
        dateKey: group.dateKey,
        startTime: group.startTime,
        endTime: group.endTime,
        sortTime: group.sortTime,
        timeRange: group.timeRange,
        subject: group.subject,
        scheduleIds: group.scheduleIds,
        participantCount,
        participantLabel: `共 ${participantCount} 位学员`,
        studentNamesLabel: studentNames.join('、'),
        completedCount,
        isCompleted,
        isPartial,
        sessionNote: participantCount > 1 ? '小组课' : '单人课'
      };
    })
    .sort((left, right) => left.sortTime - right.sortTime);
});

const weekDays = computed(() => {
  const todayKey = formatDateParam(new Date());
  return getWeekDays(currentWeekStart.value).map((day) => {
    const dateKey = formatDateParam(day);
    const weekday = day.getDay();
    const items = groupedSchedules.value.filter((schedule) => schedule.dateKey === dateKey);
    return {
      dateKey,
      weekday: formatWeekday(day),
      dateLabel: formatDate(day),
      dateNumber: `${day.getDate()}`.padStart(2, '0'),
      monthLabel: `${day.getMonth() + 1}月`,
      isToday: dateKey === todayKey,
      isWeekend: weekday === 0 || weekday === 6,
      items
    };
  });
});

const currentWeekLabel = computed(() => formatWeekRange(currentWeekStart.value));

const nextUpcomingSchedule = computed(() => {
  const now = Date.now();
  return groupedSchedules.value.find((schedule) => {
    return !schedule.isCompleted && new Date(schedule.startTime).getTime() >= now;
  }) ?? null;
});

const schedulePosterUrl = computed(() => {
  if (!weekDays.value.length) {
    return '';
  }

  const width = 1600;
  const outerPadding = 36;
  const leftRail = 122;
  const columnGap = 12;
  const columnWidth = Math.floor((width - outerPadding * 2 - leftRail - columnGap * 6) / 7);
  const headerHeight = 162;
  const dayHeaderHeight = 78;
  const bottomPadding = 38;
  const defaultStart = 8 * 60;
  const defaultEnd = 22 * 60;

  let visibleStart = defaultStart;
  let visibleEnd = defaultEnd;

  if (groupedSchedules.value.length) {
    const minStart = Math.min(...groupedSchedules.value.map((schedule) => getMinutesOfDay(schedule.startTime)));
    const maxEnd = Math.max(...groupedSchedules.value.map((schedule) => getMinutesOfDay(schedule.endTime)));
    visibleStart = Math.max(7 * 60, Math.floor(minStart / 60) * 60 - 60);
    visibleEnd = Math.min(23 * 60, Math.ceil(maxEnd / 60) * 60 + 60);
    if (visibleEnd - visibleStart < 8 * 60) {
      visibleEnd = Math.min(23 * 60, visibleStart + 8 * 60);
    }
  }

  const hourRows = Math.max(1, Math.ceil((visibleEnd - visibleStart) / 60));
  const hourHeight = 84;
  const gridTop = outerPadding + headerHeight + dayHeaderHeight;
  const gridHeight = hourRows * hourHeight;
  const height = gridTop + gridHeight + bottomPadding;
  const gridLeft = outerPadding + leftRail;
  const dayHeaderTop = outerPadding + headerHeight;
  const dayIndexMap = new Map(weekDays.value.map((day, index) => [day.dateKey, index]));
  const gridAreaWidth = 7 * columnWidth + 6 * columnGap;
  const titlePanelWidth = width - outerPadding * 2;
  const titlePanelHeight = headerHeight - 24;
  const titleTextX = outerPadding + 24;
  const boardPanelY = dayHeaderTop - 16;
  const boardPanelHeight = gridHeight + dayHeaderHeight + 8;
  const timelineAxisX = gridLeft - 24;
  const timelineLabelX = outerPadding + 12;
  const timelineLabelWidth = leftRail - 42;
  const timelineLabelHeight = 28;

  const dayHeaders = weekDays.value.map((day, index) => {
    const x = gridLeft + index * (columnWidth + columnGap);
    const fill = day.isToday ? '#dbeafe' : day.isWeekend ? '#f0f9ff' : '#ffffff';
    const stroke = day.isToday ? '#60a5fa' : '#dbe7f3';
    const subtitle = day.items.length ? `${day.items.length} 节课` : '空档';
    return `
      <g>
        <rect x="${x}" y="${dayHeaderTop}" width="${columnWidth}" height="${dayHeaderHeight - 12}" rx="22" fill="${fill}" stroke="${stroke}" />
        <text x="${x + 18}" y="${dayHeaderTop + 30}" font-size="16" font-weight="700" fill="#0f172a">${escapeSvgText(day.weekday)}</text>
        <text x="${x + 18}" y="${dayHeaderTop + 54}" font-size="12" fill="#64748b">${escapeSvgText(`${day.monthLabel} ${day.dateNumber}`)}</text>
        <text x="${x + columnWidth - 18}" y="${dayHeaderTop + 54}" text-anchor="end" font-size="12" fill="${day.isToday ? '#2563eb' : '#94a3b8'}">${escapeSvgText(day.isToday ? '今天' : subtitle)}</text>
      </g>
    `;
  }).join('');

  const verticalLines = weekDays.value.map((day, index) => {
    const x = gridLeft + index * (columnWidth + columnGap);
    return `<rect x="${x}" y="${gridTop}" width="${columnWidth}" height="${gridHeight}" rx="26" fill="${day.isWeekend ? '#f8fcff' : '#ffffff'}" stroke="#e5edf5" />`;
  }).join('');

  const horizontalGrid = Array.from({ length: hourRows + 1 }, (_, index) => {
    const y = gridTop + index * hourHeight;
    return `
      <line x1="${gridLeft}" y1="${y}" x2="${gridLeft + 7 * columnWidth + 6 * columnGap}" y2="${y}" stroke="#e8eef5" />
    `;
  }).join('');

  const timelineRail = `
    <g>
      <line x1="${timelineAxisX}" y1="${gridTop + 14}" x2="${timelineAxisX}" y2="${gridTop + gridHeight - 14}" stroke="rgba(148,163,184,0.38)" stroke-width="3" stroke-linecap="round" />
      ${Array.from({ length: hourRows }, (_, index) => {
        const y = gridTop + index * hourHeight;
        const labelY = y + 8;
        const dotY = labelY + timelineLabelHeight / 2;
        const tickStart = timelineAxisX + 10;
        const tickEnd = gridLeft - 8;
        return `
          <g>
            <rect x="${timelineLabelX}" y="${labelY}" width="${timelineLabelWidth}" height="${timelineLabelHeight}" rx="14" fill="rgba(255,255,255,0.92)" stroke="rgba(191,219,254,0.92)" />
            <text x="${timelineLabelX + timelineLabelWidth / 2}" y="${labelY + 18}" text-anchor="middle" font-size="12" font-weight="700" fill="#475569">${escapeSvgText(formatHourMark(visibleStart + index * 60))}</text>
            <circle cx="${timelineAxisX}" cy="${dotY}" r="5" fill="#ffffff" stroke="#93c5fd" stroke-width="3" />
            <line x1="${tickStart}" y1="${dotY}" x2="${tickEnd}" y2="${dotY}" stroke="rgba(191,219,254,0.78)" stroke-width="2" stroke-linecap="round" />
          </g>
        `;
      }).join('')}
    </g>
  `;

  const statusPalette = (schedule) => {
    if (schedule.isCompleted) {
      return {
        fill: '#f0fdf4',
        stroke: '#86efac',
        accent: '#22c55e',
        badge: '#dcfce7',
        badgeText: '#15803d'
      };
    }
    if (schedule.isPartial) {
      return {
        fill: '#eff6ff',
        stroke: '#93c5fd',
        accent: '#3b82f6',
        badge: '#dbeafe',
        badgeText: '#1d4ed8'
      };
    }
    return {
      fill: '#fff7ed',
      stroke: '#fdba74',
      accent: '#f59e0b',
      badge: '#ffedd5',
      badgeText: '#c2410c'
    };
  };

  const scheduleCards = groupedSchedules.value.map((schedule) => {
    const dayIndex = dayIndexMap.get(schedule.dateKey);
    if (dayIndex === undefined) {
      return '';
    }

    const palette = statusPalette(schedule);
    const x = gridLeft + dayIndex * (columnWidth + columnGap) + 10;
    const y = gridTop + ((getMinutesOfDay(schedule.startTime) - visibleStart) / 60) * hourHeight + 8;
    const widthValue = columnWidth - 20;
    const heightValue = Math.max(76, ((getMinutesOfDay(schedule.endTime) - getMinutesOfDay(schedule.startTime)) / 60) * hourHeight - 12);
    const accentInset = 10;
    const accentLineX = x + 3;
    const compact = heightValue < 102;
    const footerText = schedule.isCompleted
      ? '已销课'
      : schedule.isPartial
        ? `已销 ${schedule.completedCount}/${schedule.scheduleIds.length}`
        : `${schedule.sessionNote} · ${schedule.participantLabel}`;

    return `
      <g filter="url(#cardShadow)">
        <rect x="${x}" y="${y}" width="${widthValue}" height="${heightValue}" rx="20" fill="${palette.fill}" stroke="${palette.stroke}" />
        <line x1="${accentLineX}" y1="${y + accentInset}" x2="${accentLineX}" y2="${y + heightValue - accentInset}" stroke="${palette.accent}" stroke-width="5" stroke-linecap="round" />
        <rect x="${x + 14}" y="${y + 14}" width="74" height="24" rx="12" fill="${palette.badge}" />
        <text x="${x + 51}" y="${y + 30}" text-anchor="middle" font-size="11" font-weight="600" fill="${palette.badgeText}">${escapeSvgText(schedule.timeRange)}</text>
        <text x="${x + 14}" y="${y + 58}" font-size="17" font-weight="700" fill="#0f172a">${escapeSvgText(truncateText(schedule.subject, 14))}</text>
        ${compact ? '' : `<text x="${x + 14}" y="${y + 80}" font-size="12" fill="#475569">${escapeSvgText(truncateText(schedule.studentNamesLabel, 24))}</text>`}
        ${compact ? '' : `<text x="${x + 14}" y="${y + 100}" font-size="11" fill="#64748b">${escapeSvgText(schedule.participantLabel)}</text>`}
        <text x="${x + 14}" y="${y + heightValue - 18}" font-size="11" fill="${palette.badgeText}">${escapeSvgText(truncateText(footerText, 18))}</text>
      </g>
    `;
  }).join('');

  const emptyOverlay = groupedSchedules.value.length
    ? ''
    : `
      <g>
        <rect x="${gridLeft + 120}" y="${gridTop + 110}" width="${gridAreaWidth - 240}" height="220" rx="36" fill="rgba(255,255,255,0.84)" stroke="#dbe7f3" />
        <text x="${width / 2}" y="${gridTop + 206}" text-anchor="middle" font-size="34" font-weight="700" fill="#0f172a">本周暂无课程安排</text>
        <text x="${width / 2}" y="${gridTop + 246}" text-anchor="middle" font-size="16" fill="#64748b">如需新增课程，请前往排课管理。</text>
      </g>
    `;

  const cuteDecorations = `
    <g opacity="0.96">
      <g transform="translate(-54 ${height - 134})" opacity="0.78">
        <path d="M 0 96 A 96 96 0 0 1 192 96" fill="none" stroke="#fb7185" stroke-width="16" stroke-linecap="round" />
        <path d="M 16 96 A 80 80 0 0 1 176 96" fill="none" stroke="#f59e0b" stroke-width="16" stroke-linecap="round" />
        <path d="M 32 96 A 64 64 0 0 1 160 96" fill="none" stroke="#facc15" stroke-width="16" stroke-linecap="round" />
        <path d="M 48 96 A 48 48 0 0 1 144 96" fill="none" stroke="#4ade80" stroke-width="16" stroke-linecap="round" />
        <path d="M 64 96 A 32 32 0 0 1 128 96" fill="none" stroke="#60a5fa" stroke-width="16" stroke-linecap="round" />
        <circle cx="22" cy="98" r="18" fill="rgba(255,255,255,0.95)" />
        <circle cx="42" cy="88" r="22" fill="rgba(255,255,255,0.96)" />
        <circle cx="68" cy="98" r="16" fill="rgba(255,255,255,0.94)" />
        <circle cx="120" cy="98" r="16" fill="rgba(255,255,255,0.94)" />
        <circle cx="148" cy="86" r="24" fill="rgba(255,255,255,0.96)" />
        <circle cx="176" cy="98" r="18" fill="rgba(255,255,255,0.95)" />
      </g>
      <g transform="translate(${width - 650} 22)">
        <path d="M 48 110 C 44 136 44 154 34 182" fill="none" stroke="rgba(148,163,184,0.58)" stroke-width="2.5" stroke-linecap="round" />
        <path d="M 102 96 C 100 126 104 144 96 178" fill="none" stroke="rgba(148,163,184,0.58)" stroke-width="2.5" stroke-linecap="round" />
        <path d="M 154 118 C 152 144 156 162 150 190" fill="none" stroke="rgba(148,163,184,0.58)" stroke-width="2.5" stroke-linecap="round" />
        <ellipse cx="48" cy="70" rx="28" ry="36" fill="rgba(251,113,133,0.84)" stroke="rgba(225,29,72,0.22)" />
        <ellipse cx="102" cy="56" rx="30" ry="38" fill="rgba(96,165,250,0.84)" stroke="rgba(37,99,235,0.22)" />
        <ellipse cx="154" cy="82" rx="26" ry="34" fill="rgba(253,224,71,0.88)" stroke="rgba(217,119,6,0.22)" />
        <path d="M 48 104 L 42 116 L 54 116 Z" fill="rgba(251,113,133,0.90)" />
        <path d="M 102 92 L 96 104 L 108 104 Z" fill="rgba(96,165,250,0.90)" />
        <path d="M 154 114 L 148 126 L 160 126 Z" fill="rgba(253,224,71,0.92)" />
        <circle cx="40" cy="56" r="6" fill="rgba(255,255,255,0.24)" />
        <circle cx="92" cy="42" r="7" fill="rgba(255,255,255,0.24)" />
        <circle cx="146" cy="68" r="6" fill="rgba(255,255,255,0.22)" />
      </g>
      <g transform="translate(${width - 408} 10)">
        <circle cx="70" cy="56" r="48" fill="rgba(253,224,71,0.20)" filter="url(#softBlur)" />
        <circle cx="70" cy="56" r="34" fill="#fde047" stroke="rgba(245,158,11,0.34)" />
        <path d="M 70 0 V 18" stroke="rgba(245,158,11,0.48)" stroke-width="6" stroke-linecap="round" />
        <path d="M 70 94 V 112" stroke="rgba(245,158,11,0.48)" stroke-width="6" stroke-linecap="round" />
        <path d="M 14 56 H 32" stroke="rgba(245,158,11,0.48)" stroke-width="6" stroke-linecap="round" />
        <path d="M 108 56 H 126" stroke="rgba(245,158,11,0.48)" stroke-width="6" stroke-linecap="round" />
        <path d="M 28 16 L 40 28" stroke="rgba(245,158,11,0.48)" stroke-width="6" stroke-linecap="round" />
        <path d="M 100 84 L 112 96" stroke="rgba(245,158,11,0.48)" stroke-width="6" stroke-linecap="round" />
        <path d="M 28 96 L 40 84" stroke="rgba(245,158,11,0.48)" stroke-width="6" stroke-linecap="round" />
        <path d="M 100 28 L 112 16" stroke="rgba(245,158,11,0.48)" stroke-width="6" stroke-linecap="round" />
        <circle cx="58" cy="54" r="4" fill="#334155" />
        <circle cx="82" cy="54" r="4" fill="#334155" />
        <path d="M 56 70 Q 70 82 84 70" fill="none" stroke="#334155" stroke-width="3" stroke-linecap="round" />
        <circle cx="50" cy="66" r="5" fill="rgba(251,113,133,0.26)" />
        <circle cx="90" cy="66" r="5" fill="rgba(251,113,133,0.26)" />
      </g>
      <g transform="translate(${width - 320} 36)">
        <ellipse cx="126" cy="58" rx="62" ry="28" fill="rgba(255,255,255,0.92)" stroke="rgba(148,163,184,0.28)" />
        <circle cx="84" cy="58" r="28" fill="rgba(255,255,255,0.96)" stroke="rgba(148,163,184,0.28)" />
        <circle cx="126" cy="42" r="34" fill="rgba(255,255,255,0.96)" stroke="rgba(148,163,184,0.28)" />
        <circle cx="166" cy="58" r="24" fill="rgba(255,255,255,0.96)" stroke="rgba(148,163,184,0.28)" />
        <circle cx="104" cy="70" r="4" fill="#334155" />
        <circle cx="144" cy="70" r="4" fill="#334155" />
        <circle cx="92" cy="82" r="5" fill="rgba(251,113,133,0.38)" />
        <circle cx="156" cy="82" r="5" fill="rgba(251,113,133,0.38)" />
        <path d="M 114 88 Q 124 98 134 88" fill="none" stroke="#334155" stroke-width="3" stroke-linecap="round" />
        <path d="M 58 28 L 64 16 L 70 28 L 82 34 L 70 40 L 64 52 L 58 40 L 46 34 Z" fill="rgba(253,224,71,0.82)" stroke="rgba(245,158,11,0.44)" />
        <path d="M 198 24 L 202 16 L 206 24 L 214 28 L 206 32 L 202 40 L 198 32 L 190 28 Z" fill="rgba(125,211,252,0.88)" stroke="rgba(59,130,246,0.34)" />
        <path d="M 220 76 L 225 66 L 230 76 L 240 81 L 230 86 L 225 96 L 220 86 L 210 81 Z" fill="rgba(253,224,71,0.72)" stroke="rgba(245,158,11,0.34)" />
      </g>
      <g transform="translate(180 ${height - 208}) rotate(-10)" opacity="0.84">
        <rect x="0" y="18" width="34" height="172" rx="16" fill="#fbbf24" stroke="rgba(180,83,9,0.24)" />
        <rect x="7" y="32" width="20" height="90" rx="10" fill="rgba(255,255,255,0.28)" />
        <rect x="0" y="128" width="34" height="22" rx="8" fill="#fb7185" />
        <path d="M 0 18 L 17 0 L 34 18 Z" fill="#fde68a" stroke="rgba(148,163,184,0.24)" />
        <path d="M 11 8 L 17 2 L 23 8 Z" fill="#475569" />
      </g>
      <g transform="translate(${width - 262} ${height - 180})">
        <rect x="18" y="56" width="118" height="84" rx="20" fill="rgba(255,255,255,0.88)" stroke="rgba(148,163,184,0.26)" />
        <rect x="32" y="44" width="118" height="84" rx="20" fill="rgba(219,234,254,0.88)" stroke="rgba(96,165,250,0.28)" />
        <path d="M 32 74 H 150" stroke="rgba(148,163,184,0.36)" stroke-width="4" stroke-linecap="round" />
        <path d="M 52 92 H 132" stroke="rgba(148,163,184,0.30)" stroke-width="4" stroke-linecap="round" />
        <path d="M 52 108 H 118" stroke="rgba(148,163,184,0.24)" stroke-width="4" stroke-linecap="round" />
        <circle cx="116" cy="22" r="20" fill="rgba(255,255,255,0.96)" stroke="rgba(148,163,184,0.24)" />
        <circle cx="108" cy="20" r="3.5" fill="#334155" />
        <circle cx="124" cy="20" r="3.5" fill="#334155" />
        <path d="M 108 30 Q 116 38 124 30" fill="none" stroke="#334155" stroke-width="3" stroke-linecap="round" />
        <circle cx="102" cy="28" r="4" fill="rgba(251,113,133,0.30)" />
        <circle cx="130" cy="28" r="4" fill="rgba(251,113,133,0.30)" />
      </g>
      <g opacity="0.72">
        <path d="M ${outerPadding + 190} 54 L ${outerPadding + 196} 42 L ${outerPadding + 202} 54 L ${outerPadding + 214} 60 L ${outerPadding + 202} 66 L ${outerPadding + 196} 78 L ${outerPadding + 190} 66 L ${outerPadding + 178} 60 Z" fill="rgba(253,224,71,0.82)" />
        <path d="M ${outerPadding + 226} 114 L ${outerPadding + 230} 106 L ${outerPadding + 234} 114 L ${outerPadding + 242} 118 L ${outerPadding + 234} 122 L ${outerPadding + 230} 130 L ${outerPadding + 226} 122 L ${outerPadding + 218} 118 Z" fill="rgba(125,211,252,0.88)" />
      </g>
    </g>
  `;

  const svg = `
    <svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}">
      <defs>
        <linearGradient id="posterBg" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stop-color="#f8fbff" />
          <stop offset="100%" stop-color="#eef6ff" />
        </linearGradient>
        <radialGradient id="posterGlowTop" cx="50%" cy="50%" r="50%">
          <stop offset="0%" stop-color="#7dd3fc" stop-opacity="0.48" />
          <stop offset="100%" stop-color="#7dd3fc" stop-opacity="0" />
        </radialGradient>
        <radialGradient id="posterGlowBottom" cx="50%" cy="50%" r="50%">
          <stop offset="0%" stop-color="#bfdbfe" stop-opacity="0.56" />
          <stop offset="100%" stop-color="#bfdbfe" stop-opacity="0" />
        </radialGradient>
        <linearGradient id="headerGlass" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stop-color="#ffffff" stop-opacity="0.88" />
          <stop offset="100%" stop-color="#ffffff" stop-opacity="0.52" />
        </linearGradient>
        <linearGradient id="boardGlass" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stop-color="#ffffff" stop-opacity="0.60" />
          <stop offset="100%" stop-color="#ffffff" stop-opacity="0.34" />
        </linearGradient>
        <linearGradient id="titleInk" x1="0%" y1="0%" x2="100%" y2="0%">
          <stop offset="0%" stop-color="#0f172a" />
          <stop offset="55%" stop-color="#1e3a8a" />
          <stop offset="100%" stop-color="#2563eb" />
        </linearGradient>
        <pattern id="posterDots" width="28" height="28" patternUnits="userSpaceOnUse">
          <circle cx="4" cy="4" r="1.8" fill="rgba(191,219,254,0.58)" />
          <circle cx="18" cy="16" r="1.2" fill="rgba(125,211,252,0.42)" />
        </pattern>
        <filter id="softBlur" x="-20%" y="-20%" width="140%" height="140%">
          <feGaussianBlur stdDeviation="18" />
        </filter>
        <filter id="titleShadow" x="-20%" y="-40%" width="160%" height="220%">
          <feDropShadow dx="0" dy="5" stdDeviation="6" flood-color="#1d4ed8" flood-opacity="0.14" />
        </filter>
        <filter id="cardShadow" x="-20%" y="-20%" width="160%" height="180%">
          <feDropShadow dx="0" dy="8" stdDeviation="10" flood-color="#0f172a" flood-opacity="0.10" />
        </filter>
      </defs>
      <rect width="${width}" height="${height}" rx="40" fill="url(#posterBg)" />
      <rect width="${width}" height="${height}" rx="40" fill="url(#posterDots)" opacity="0.42" />
      <ellipse cx="${width - 150}" cy="92" rx="230" ry="150" fill="url(#posterGlowTop)" filter="url(#softBlur)" />
      <ellipse cx="120" cy="${height - 110}" rx="250" ry="180" fill="url(#posterGlowBottom)" filter="url(#softBlur)" />
      <path d="M 0 ${height - 168} C 240 ${height - 236}, 460 ${height - 80}, 760 ${height - 138} S 1290 ${height - 230}, ${width} ${height - 124} L ${width} ${height} L 0 ${height} Z" fill="rgba(255,255,255,0.26)" />
      <path d="M ${width - 410} 54 C ${width - 330} 18, ${width - 232} 20, ${width - 164} 82" fill="none" stroke="rgba(255,255,255,0.60)" stroke-width="3" stroke-linecap="round" />
      <path d="M 110 102 C 190 54, 302 54, 386 98" fill="none" stroke="rgba(59,130,246,0.16)" stroke-width="4" stroke-linecap="round" />
      <rect x="${outerPadding}" y="${outerPadding}" width="${titlePanelWidth}" height="${titlePanelHeight}" rx="34" fill="url(#headerGlass)" stroke="rgba(255,255,255,0.88)" />
      <rect x="${outerPadding + 14}" y="${outerPadding + 14}" width="${titlePanelWidth - 28}" height="${titlePanelHeight - 28}" rx="28" fill="rgba(255,255,255,0.12)" stroke="rgba(191,219,254,0.38)" />
      <rect x="${outerPadding}" y="${boardPanelY}" width="${leftRail - 18}" height="${boardPanelHeight}" rx="30" fill="url(#boardGlass)" stroke="rgba(203,213,225,0.82)" />
      <rect x="${outerPadding + 10}" y="${boardPanelY + 14}" width="${leftRail - 38}" height="${boardPanelHeight - 28}" rx="24" fill="rgba(255,255,255,0.30)" stroke="rgba(255,255,255,0.42)" />
      <rect x="${gridLeft - 18}" y="${boardPanelY}" width="${gridAreaWidth + 36}" height="${boardPanelHeight}" rx="34" fill="url(#boardGlass)" stroke="rgba(191,219,254,0.82)" />
      <circle cx="${width - 96}" cy="90" r="46" fill="rgba(255,255,255,0.32)" />
      <circle cx="${width - 152}" cy="136" r="18" fill="rgba(59,130,246,0.16)" />
      ${cuteDecorations}
      <text x="${titleTextX}" y="${outerPadding + 38}" font-size="19" fill="#2563eb" font-weight="800" letter-spacing="2.8" font-family="'Avenir Next', 'Helvetica Neue', Arial, sans-serif">QINGQINGKETANG</text>
      <text x="${titleTextX}" y="${outerPadding + 88}" font-size="40" fill="url(#titleInk)" font-weight="800" letter-spacing="1.1" font-family="'PingFang SC', 'Hiragino Sans GB', 'Noto Sans SC', sans-serif" filter="url(#titleShadow)">本周课程表</text>
      <text x="${titleTextX}" y="${outerPadding + 120}" font-size="17" fill="#475569" font-weight="700" letter-spacing="0.9" font-family="'Avenir Next', 'DIN Alternate', 'Helvetica Neue', Arial, sans-serif">${escapeSvgText(currentWeekLabel.value)}</text>
      ${dayHeaders}
      ${verticalLines}
      ${timelineRail}
      ${horizontalGrid}
      ${scheduleCards}
      ${emptyOverlay}
    </svg>
  `;

  return createSvgImageUrl(svg);
});

onMounted(async () => {
  await loadSchedules();
});
</script>

<style scoped>
.schedule-board-card {
  position: relative;
  overflow: hidden;
  border: 0;
  border-radius: 30px;
  background: transparent;
  box-shadow: none;
}

.schedule-board-card::before {
  display: none;
}

.schedule-board-card :deep(.el-card__body) {
  position: relative;
  z-index: 1;
  padding: 0;
  background: transparent;
}

.schedule-poster-section {
  margin-top: 0;
}

.schedule-poster-toolbar {
  position: absolute;
  top: 52px;
  right: 34px;
  z-index: 3;
}

.schedule-print-button {
  --el-button-bg-color: rgba(255, 255, 255, 0.72);
  --el-button-border-color: rgba(255, 255, 255, 0.78);
  --el-button-text-color: #1d4ed8;
  --el-button-hover-bg-color: rgba(239, 246, 255, 0.98);
  --el-button-hover-border-color: rgba(147, 197, 253, 0.92);
  --el-button-hover-text-color: #1d4ed8;
  --el-button-disabled-bg-color: rgba(255, 255, 255, 0.6);
  --el-button-disabled-border-color: rgba(255, 255, 255, 0.68);
  --el-button-disabled-text-color: #94a3b8;
  min-height: 36px;
  padding: 0 14px;
  border-radius: 999px;
  backdrop-filter: blur(18px);
  box-shadow: 0 10px 24px rgba(59, 130, 246, 0.12);
  font-size: 13px;
  font-weight: 700;
}

.schedule-poster-frame {
  position: relative;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 0;
  border: 0;
  border-radius: 30px;
  background: transparent;
}

.schedule-poster-image {
  display: block;
  width: 100%;
  border-radius: 30px;
  overflow: hidden;
  box-shadow: none;
  cursor: zoom-in;
}

.schedule-poster-image :deep(.el-image__inner) {
  display: block;
  width: 100%;
  height: auto;
  border-radius: 30px;
}

@media (max-width: 900px) {
  .schedule-poster-toolbar {
    top: 30px;
    right: 20px;
  }

  .schedule-poster-image {
    border-radius: 22px;
  }

  .schedule-poster-frame,
  .schedule-board-card {
    border-radius: 22px;
  }

  .schedule-poster-image :deep(.el-image__inner) {
    border-radius: 22px;
  }
}
</style>
