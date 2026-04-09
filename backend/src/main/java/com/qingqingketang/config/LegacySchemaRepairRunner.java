package com.qingqingketang.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@Component
@Order(0)
public class LegacySchemaRepairRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LegacySchemaRepairRunner.class);

    private final DataSource dataSource;

    public LegacySchemaRepairRunner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        repairLegacyColumns();
        repairStudentReferralTable();
        migrateLessonConsumptionSnapshots();
        dropLegacyLessonBalanceTable();
    }

    private void repairLegacyColumns() {
        try (Connection connection = dataSource.getConnection()) {
            ensureColumn(connection,
                    "student_payments",
                    "remark",
                    "ALTER TABLE student_payments ADD COLUMN remark VARCHAR(255) NULL COMMENT '备注'");
            ensureColumn(connection,
                    "trial_lessons",
                    "status",
                    "ALTER TABLE trial_lessons ADD COLUMN status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '试听状态'");
            ensureColumn(connection,
                    "trial_lessons",
                    "note",
                    "ALTER TABLE trial_lessons ADD COLUMN note VARCHAR(255) NULL COMMENT '备注'");
            ensureColumn(connection,
                    "student_schedules",
                    "payment_id",
                    "ALTER TABLE student_schedules ADD COLUMN payment_id BIGINT NULL COMMENT '对应缴费批次ID'");
            ensureColumn(connection,
                    "student_schedules",
                    "lesson_price",
                    "ALTER TABLE student_schedules ADD COLUMN lesson_price DECIMAL(10,2) NULL COMMENT '本节课单价快照'");
            ensureColumn(connection,
                    "student_schedules",
                    "consumed_at",
                    "ALTER TABLE student_schedules ADD COLUMN consumed_at DATETIME NULL COMMENT '销课时间'");
        } catch (SQLException exception) {
            throw new IllegalStateException("修复历史数据库字段失败", exception);
        }
    }

    private void migrateLessonConsumptionSnapshots() {
        try (Connection connection = dataSource.getConnection()) {
            if (!tableExists(connection, "student_lesson_consumptions")) {
                return;
            }

            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(
                        "UPDATE student_schedules s " +
                                "JOIN student_lesson_consumptions c ON c.schedule_id = s.id " +
                                "SET s.payment_id = COALESCE(s.payment_id, c.payment_id), " +
                                "    s.lesson_price = COALESCE(s.lesson_price, c.lesson_price), " +
                                "    s.consumed_at = COALESCE(s.consumed_at, c.consumed_at) " +
                                "WHERE s.payment_id IS NULL OR s.lesson_price IS NULL OR s.consumed_at IS NULL");
                long remaining = queryForLong(statement,
                        "SELECT COUNT(*) FROM student_schedules s " +
                                "JOIN student_lesson_consumptions c ON c.schedule_id = s.id " +
                                "WHERE s.payment_id IS NULL OR s.lesson_price IS NULL OR s.consumed_at IS NULL");
                if (remaining == 0) {
                    try {
                        statement.execute("DROP TABLE student_lesson_consumptions");
                        log.info("Migrated lesson consumption snapshots into student_schedules and dropped legacy table");
                    } catch (SQLException exception) {
                        log.warn("Migrated lesson consumption snapshots into student_schedules but failed to drop legacy table", exception);
                    }
                } else {
                    log.warn("Skipped dropping legacy student_lesson_consumptions table because {} rows were not migrated", remaining);
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("迁移历史销课快照失败", exception);
        }
    }

    private void repairStudentReferralTable() {
        try (Connection connection = dataSource.getConnection()) {
            if (tableExists(connection, "student_referrals")) {
                return;
            }

            try (Statement statement = connection.createStatement()) {
                statement.execute(
                        "CREATE TABLE student_referrals (" +
                                "id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键'," +
                                "referrer_student_id BIGINT NOT NULL COMMENT '介绍人学生ID'," +
                                "referred_student_id BIGINT NOT NULL COMMENT '被介绍人学生ID'," +
                                "reward_lesson_count INT NOT NULL DEFAULT 1 COMMENT '奖励课时数'," +
                                "reward_payment_id BIGINT NULL COMMENT '奖励课时对应的缴费流水ID'," +
                                "remark VARCHAR(255) NULL COMMENT '备注'," +
                                "created_at DATETIME NOT NULL COMMENT '创建时间'," +
                                "rewarded_at DATETIME NOT NULL COMMENT '发放奖励时间'," +
                                "CONSTRAINT fk_student_referrals_referrer " +
                                "    FOREIGN KEY (referrer_student_id) REFERENCES students(id) ON DELETE CASCADE," +
                                "CONSTRAINT fk_student_referrals_referred " +
                                "    FOREIGN KEY (referred_student_id) REFERENCES students(id) ON DELETE CASCADE," +
                                "CONSTRAINT fk_student_referrals_reward_payment " +
                                "    FOREIGN KEY (reward_payment_id) REFERENCES student_payments(id) ON DELETE SET NULL," +
                                "UNIQUE KEY uk_student_referrals_referred (referred_student_id)," +
                                "UNIQUE KEY uk_student_referrals_reward_payment (reward_payment_id)," +
                                "INDEX idx_student_referrals_referrer (referrer_student_id)" +
                                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生转介绍关系'");
                log.info("Patched missing table student_referrals for legacy database");
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("修复转介绍关系表失败", exception);
        }
    }

    private void dropLegacyLessonBalanceTable() {
        try (Connection connection = dataSource.getConnection()) {
            if (!tableExists(connection, "student_lesson_balances")) {
                return;
            }

            try (Statement statement = connection.createStatement()) {
                statement.execute("DROP TABLE student_lesson_balances");
                log.info("Dropped legacy student_lesson_balances table");
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("删除历史课时快照表失败", exception);
        }
    }

    private void ensureColumn(Connection connection, String tableName, String columnName, String ddl) throws SQLException {
        if (columnExists(connection, tableName, columnName)) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute(ddl);
            log.info("Patched missing column {}.{} for legacy database", tableName, columnName);
        }
    }

    private boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        String catalog = connection.getCatalog();
        try (ResultSet resultSet = connection.getMetaData().getColumns(catalog, null, tableName, columnName)) {
            return resultSet.next();
        }
    }

    private boolean tableExists(Connection connection, String tableName) throws SQLException {
        String catalog = connection.getCatalog();
        try (ResultSet resultSet = connection.getMetaData().getTables(catalog, null, tableName, null)) {
            return resultSet.next();
        }
    }

    private long queryForLong(Statement statement, String sql) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery(sql)) {
            if (!resultSet.next()) {
                return 0L;
            }
            return resultSet.getLong(1);
        }
    }
}
