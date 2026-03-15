-- 学生信息表：记录学生基础资料
CREATE TABLE IF NOT EXISTS students (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键',
    name VARCHAR(64) NOT NULL COMMENT '学生姓名',
    gender VARCHAR(8) NOT NULL COMMENT '性别（男/女等）',
    grade VARCHAR(32) NOT NULL COMMENT '年级或班型描述',
    created_at DATETIME NOT NULL COMMENT '录入时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生管理主表';

-- 学费记录表：按笔记录缴费金额与课时
CREATE TABLE IF NOT EXISTS student_payments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键',
    student_id BIGINT NOT NULL COMMENT '学生ID，关联students.id',
    tuition_paid DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '本次缴费金额',
    lesson_count INT NOT NULL DEFAULT 0 COMMENT '本次购买课时数',
    avg_fee_per_lesson DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '单节课平均费用',
    remark VARCHAR(255) NULL COMMENT '备注',
    paid_at DATETIME NOT NULL COMMENT '缴费时间',
    CONSTRAINT fk_student_payments_student
        FOREIGN KEY (student_id) REFERENCES students(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生学费流水';

-- 学生课表：记录排课结果
CREATE TABLE IF NOT EXISTS student_schedules (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    subject VARCHAR(64) NOT NULL COMMENT '科目名称',
    start_time DATETIME NOT NULL COMMENT '上课开始时间',
    end_time DATETIME NOT NULL COMMENT '上课结束时间',
    status VARCHAR(32) NOT NULL DEFAULT 'PLANNED' COMMENT '排课状态',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    CONSTRAINT fk_student_schedule_student
        FOREIGN KEY (student_id) REFERENCES students(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生课表安排';

-- 销课明细表：记录每节课实际消耗的缴费批次与单价
CREATE TABLE IF NOT EXISTS student_lesson_consumptions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键',
    schedule_id BIGINT NOT NULL UNIQUE COMMENT '排课ID',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    payment_id BIGINT NOT NULL COMMENT '对应缴费批次ID',
    lesson_price DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '本节课单价快照',
    consumed_at DATETIME NOT NULL COMMENT '销课时间',
    CONSTRAINT fk_lesson_consumption_schedule
        FOREIGN KEY (schedule_id) REFERENCES student_schedules(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_lesson_consumption_student
        FOREIGN KEY (student_id) REFERENCES students(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_lesson_consumption_payment
        FOREIGN KEY (payment_id) REFERENCES student_payments(id)
        ON DELETE CASCADE,
    INDEX idx_lesson_consumption_student (student_id),
    INDEX idx_lesson_consumption_payment (payment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生销课明细';

-- 学生课时快照表：记录购买、已排、已销、剩余和可排课时
CREATE TABLE IF NOT EXISTS student_lesson_balances (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键',
    student_id BIGINT NOT NULL UNIQUE COMMENT '学生ID',
    purchased_lessons INT NOT NULL DEFAULT 0 COMMENT '累计购买课时数',
    scheduled_lessons INT NOT NULL DEFAULT 0 COMMENT '已排未销课时数',
    completed_lessons INT NOT NULL DEFAULT 0 COMMENT '已销课时数',
    remaining_lessons INT NOT NULL DEFAULT 0 COMMENT '未销课剩余课时数',
    schedulable_lessons INT NOT NULL DEFAULT 0 COMMENT '当前可继续排课时数',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    CONSTRAINT fk_lesson_balance_student
        FOREIGN KEY (student_id) REFERENCES students(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生课时快照表';

-- 试听管理表：记录试听预约信息
CREATE TABLE IF NOT EXISTS trial_lessons (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键',
    name VARCHAR(64) NOT NULL COMMENT '试听学生姓名',
    grade VARCHAR(32) NOT NULL COMMENT '年级',
    trial_time DATETIME NOT NULL COMMENT '试听预约时间',
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '试听状态',
    note VARCHAR(255) NULL COMMENT '备注',
    created_at DATETIME NOT NULL COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试听预约记录';
