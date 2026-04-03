import { House, Calendar, EditPen, User, Opportunity, Wallet } from '@element-plus/icons-vue';

export const navigationItems = [
  {
    path: '/dashboard',
    name: 'dashboard',
    component: () => import('../views/DashboardView.vue'),
    icon: House,
    meta: {
      label: 'Dashboard',
      caption: '运营总览',
      title: 'Dashboard',
      description: '聚焦今日课程、收入走势与关键运营动作。'
    }
  },
  {
    path: '/courses',
    name: 'courses',
    component: () => import('../views/CoursesView.vue'),
    icon: Calendar,
    meta: {
      label: '课程表',
      caption: '周课表查看',
      title: '课程表',
      description: '集中查看周课表图片，支持切换查看上周、本周和下周。'
    }
  },
  {
    path: '/planner',
    name: 'planner',
    component: () => import('../views/PlannerView.vue'),
    icon: EditPen,
    meta: {
      label: '排课管理',
      caption: '排课与调课',
      title: '排课管理',
      description: '集中处理固定排课、补课、请假顺延和改时间，为学生维护正式课课表。'
    }
  },
  {
    path: '/students',
    name: 'students',
    component: () => import('../views/StudentsView.vue'),
    icon: User,
    meta: {
      label: '学生管理',
      caption: '档案与课时',
      title: '学生管理',
      description: '管理学生资料、剩余课时和后续运营动作。'
    }
  },
  {
    path: '/trials',
    name: 'trials',
    component: () => import('../views/TrialsView.vue'),
    icon: Opportunity,
    meta: {
      label: '试听管理',
      caption: '试听与转化',
      title: '试听管理',
      description: '跟进试听线索、状态流转和报名结果。'
    }
  },
  {
    path: '/finance',
    name: 'finance',
    component: () => import('../views/FinanceView.vue'),
    icon: Wallet,
    meta: {
      label: '财务管理',
      caption: '收费与续费',
      title: '财务管理',
      description: '汇总收入、登记收费，并跟踪续费与流水。'
    }
  }
];
