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
      label: '本周课程表',
      caption: '周课表查看',
      title: '本周课程表',
      description: '集中查看本周课程表图片，支持按学生筛选和周切换。'
    }
  },
  {
    path: '/planner',
    name: 'planner',
    component: () => import('../views/PlannerView.vue'),
    icon: EditPen,
    meta: {
      label: '排课管理',
      caption: '手动与 AI',
      title: '排课管理',
      description: '集中处理手动排课与 AI 排课，为学生快速生成正式课课表。'
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
