package io.github.railgun19457.easyland.migration;

import java.io.File;
import java.util.logging.Logger;

/**
 * 数据迁移工具
 * 用于将数据从 YAML 格式迁移到 SQLite
 */
public class DataMigration {
    private final Logger logger;

    public DataMigration(Logger logger) {
        this.logger = logger;
    }


    /**
     * 检查是否需要迁移
     *
     * @param yamlFile YAML 文件
     * @param sqliteFile SQLite 文件
     * @return 是否需要迁移
     */
    public boolean needsMigration(File yamlFile, File sqliteFile) {
        // 如果 YAML 文件存在且 SQLite 文件不存在，则需要迁移
        return yamlFile.exists() && !sqliteFile.exists();
    }

    /**
     * 迁移结果类
     */
    public static class MigrationResult {
        private boolean success;
        private String message;
        private int totalRecords;
        private int successCount;
        private int failCount;

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public int getTotalRecords() {
            return totalRecords;
        }

        public void setTotalRecords(int totalRecords) {
            this.totalRecords = totalRecords;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public void setSuccessCount(int successCount) {
            this.successCount = successCount;
        }

        public int getFailCount() {
            return failCount;
        }

        public void setFailCount(int failCount) {
            this.failCount = failCount;
        }
    }
}
