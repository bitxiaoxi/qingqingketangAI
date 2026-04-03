export const genders = ['女', '男'];

export const grades = [
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

export const weekdayOptions = [
  { value: 1, label: '周一' },
  { value: 2, label: '周二' },
  { value: 3, label: '周三' },
  { value: 4, label: '周四' },
  { value: 5, label: '周五' },
  { value: 6, label: '周六' },
  { value: 7, label: '周日' }
];

export const frequencyOptions = [
  { value: 1, label: '每周一次' },
  { value: 2, label: '每周两次' },
  { value: 3, label: '每周三次' },
  { value: 4, label: '每周四次' },
  { value: 5, label: '每周五次' },
  { value: 6, label: '每周六次' },
  { value: 7, label: '每天上课' }
];

export const trialStatusOptions = [
  { value: 'PENDING', label: '待试听', type: 'warning' },
  { value: 'COMPLETED', label: '已试听', type: 'success' },
  { value: 'ENROLLED', label: '已报名', type: 'primary' },
  { value: 'LOST', label: '未报名', type: 'info' }
];
