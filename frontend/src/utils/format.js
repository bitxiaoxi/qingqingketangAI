export const normalizeError = (error, fallback) => {
  const message = error?.message ?? '';
  if (!message || message === 'Failed to fetch' || message.includes('NetworkError')) {
    return fallback;
  }
  return message;
};

export const formatCurrency = (value, options = {}) => {
  const amount = Number(value ?? 0);
  if (Number.isNaN(amount)) {
    return '--';
  }
  const minimumFractionDigits = options.minimumFractionDigits ?? 0;
  const maximumFractionDigits = options.maximumFractionDigits ?? Math.max(2, minimumFractionDigits);
  return amount.toLocaleString('zh-CN', {
    minimumFractionDigits,
    maximumFractionDigits
  });
};

export const formatAmount = (value) => {
  if (value === null || value === undefined) {
    return '--';
  }
  return `${formatCurrency(value)} 元`;
};

export const formatTimestamp = (value) => {
  if (!value) return '--';
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value));
};

export const formatDate = (value) => {
  if (!value) return '--';
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit'
  }).format(new Date(value));
};

export const formatLongDate = (value) => {
  if (!value) return '--';
  return new Intl.DateTimeFormat('zh-CN', {
    month: 'long',
    day: 'numeric',
    weekday: 'long'
  }).format(new Date(value));
};

export const formatClock = (value) => {
  if (!value) return '--';
  return new Intl.DateTimeFormat('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  }).format(new Date(value));
};

export const formatTimeRange = (start, end) => {
  if (!start || !end) return '--';
  return `${formatClock(start)}-${formatClock(end)}`;
};

export const formatDateParam = (value) => {
  const date = new Date(value);
  const year = date.getFullYear();
  const month = `${date.getMonth() + 1}`.padStart(2, '0');
  const day = `${date.getDate()}`.padStart(2, '0');
  return `${year}-${month}-${day}`;
};

export const getWeekStart = (value) => {
  const date = new Date(value);
  const weekday = date.getDay() === 0 ? 7 : date.getDay();
  date.setHours(0, 0, 0, 0);
  date.setDate(date.getDate() - weekday + 1);
  return date;
};

export const getWeekDays = (weekStart) => {
  const start = new Date(weekStart);
  return Array.from({ length: 7 }, (_, index) => {
    const date = new Date(start);
    date.setDate(start.getDate() + index);
    return date;
  });
};

export const formatWeekRange = (weekStart) => {
  const start = new Date(weekStart);
  const end = new Date(weekStart);
  end.setDate(start.getDate() + 6);
  return `${formatDate(start)} - ${formatDate(end)}`;
};

export const formatWeekday = (value) => {
  if (!value) return '--';
  return ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][new Date(value).getDay()];
};

export const isSameDay = (left, right) => {
  if (!left || !right) return false;
  return formatDateParam(left) === formatDateParam(right);
};

export const isCurrentMonth = (value) => {
  if (!value) return false;
  const target = new Date(value);
  const now = new Date();
  return target.getFullYear() === now.getFullYear() && target.getMonth() === now.getMonth();
};

export const buildWeekdaySummary = (weekdays, weeklySessions, weekdayOptions) => {
  const labels = weekdayOptions
    .filter((option) => weekdays.includes(option.value))
    .map((option) => option.label);
  if (!labels.length) {
    return `已选 0/${weeklySessions}`;
  }
  return `已选 ${labels.length}/${weeklySessions} · ${labels.join('、')}`;
};
