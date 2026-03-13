# 青青课堂后台管理系统

现代化的教培机构后台，包含 Java Spring Boot（2.7.x，JDK 8）+ MyBatis-Plus 后端与 Vue 3 + Vite 前端。当前版本提供静态仪表盘首页，展示学生管理、本周课程表与学费管理模块，可在此基础上继续扩展业务逻辑。

## 项目结构

```
backend/   # Spring Boot + MyBatis-Plus 服务
frontend/  # Vue 3 + Vite 前端，已含首页 UI
```

## 后端快速开始

1. 准备环境：安装 JDK 8（或更高）与 Maven。
2. 进入目录并安装依赖：
   ```bash
   cd backend
   mvn clean package
   ```
3. 确保本地 MySQL 已创建数据库 `qingqingketang`，账号 `root`，密码 `Abcd1234!`（可在 `src/main/resources/application.yml` 中修改）。
4. 启动服务：
   ```bash
   mvn spring-boot:run
   ```
5. 验证健康状态：访问 `http://localhost:8080/api/status`。

### AI 排课密钥配置

- 不要把 `schedule.ai.api-key` 直接写进 `application.yml` 提交到仓库。
- 本项目已改为从环境变量读取 AI 密钥：`SCHEDULE_AI_API_KEY`。
- 启动前先在当前终端设置：
  ```bash
  export SCHEDULE_AI_API_KEY=你的真实密钥
  cd backend
  mvn spring-boot:run
  ```
- 如果你用 IDE 启动后端，把 `SCHEDULE_AI_API_KEY` 配到 Run Configuration 的环境变量里即可。

### 数据库初始化

- 启动 Spring Boot 时会自动执行 `backend/src/main/resources/schema.sql`（已配置 `spring.sql.init`），若表已存在则会忽略错误。
- 如需手动执行，可直接在 MySQL 中运行 `schema.sql` 中的建表语句，创建 `students` 与 `student_payments` 表：前者保存基础信息，后者记录每笔缴费流水。

## 前端快速开始

1. 安装依赖：
   ```bash
   cd frontend
   npm install
   ```
2. 运行开发服务器：
   ```bash
   npm run dev
   ```
3. 浏览器访问 `http://localhost:5173` 查看“青青课堂”首页。

### 前端接口配置

- 默认前端会调用 `http://localhost:8080`，如需自定义后端地址，可创建 `frontend/.env` 并添加 `VITE_API_BASE=http://your-api-host:port`。

## 学生管理接口

- `GET /api/students`：按录入时间倒序返回所有学生。
- `POST /api/students`：接收 `name`、`gender`、`grade`、`tuitionPaid`、`lessonCount` 字段，自动创建学生基础信息并写入一条缴费流水。
- `POST /api/students/{studentId}/payments`：为指定学生新增续费记录，需要 `tuitionPaid`、`lessonCount`。
- 前端“学生管理”面板现在包含录入表单，会直接调用以上接口并即时刷新列表。

## 下一步建议

- 在后端补充实体、Mapper 与 Service，并与 MyBatis-Plus 代码生成器结合。
- 定义统一的接口契约，然后将前端展示数据改为实时接口数据。
- 根据业务需要增加身份校验（如 Spring Security + JWT）。
