package com.qingqingketang.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@Component
public class LegacySchemaRepairRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LegacySchemaRepairRunner.class);

    private final DataSource dataSource;

    public LegacySchemaRepairRunner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        repairLegacyColumns();
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
        } catch (SQLException exception) {
            throw new IllegalStateException("修复历史数据库字段失败", exception);
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
}
