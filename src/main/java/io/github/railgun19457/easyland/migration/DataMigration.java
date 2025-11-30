package io.github.railgun19457.easyland.migration;

import io.github.railgun19457.easyland.domain.Land;
import io.github.railgun19457.easyland.repository.YamlLandRepository;
import io.github.railgun19457.easyland.repository.SqliteLandRepository;

import java.io.File;
import java.util.List;
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
     * 执行从 YAML 到 SQLite 的迁移
     *
     * @param yamlFile YAML 数据文件
     * @param sqliteFile SQLite 数据库文件
     * @return 迁移结果
     */
    public MigrationResult migrateFromYamlToSqlite(File yamlFile, File sqliteFile) {
        MigrationResult result = new MigrationResult();

        if (!yamlFile.exists()) {
            logger.info("No YAML file found at: " + yamlFile.getAbsolutePath() + ", skipping migration");
            result.setSuccess(true);
            result.setMessage("No data to migrate");
            return result;
        }

        try {
            logger.info("Starting migration from YAML to SQLite...");
            logger.info("Source: " + yamlFile.getAbsolutePath());
            logger.info("Target: " + sqliteFile.getAbsolutePath());

            // 初始化 YAML Repository
            YamlLandRepository yamlRepo = new YamlLandRepository(yamlFile);
            yamlRepo.initialize();

            // 初始化 SQLite Repository
            SqliteLandRepository sqliteRepo = new SqliteLandRepository(sqliteFile);
            sqliteRepo.initialize();

            // 读取所有领地
            List<Land> allLands = yamlRepo.findAll();
            logger.info("Found " + allLands.size() + " lands to migrate");

            if (allLands.isEmpty()) {
                result.setSuccess(true);
                result.setMessage("No lands found in YAML file");
                yamlRepo.close();
                sqliteRepo.close();
                return result;
            }

            // 迁移每个领地
            int successCount = 0;
            int failCount = 0;

            for (Land land : allLands) {
                try {
                    // 重置 ID，让 SQLite 自动生成
                    land.setId(null);
                    sqliteRepo.save(land);
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    logger.warning("Failed to migrate land: " + land.getLandId() + " - " + e.getMessage());
                }
            }

            // 关闭连接
            yamlRepo.close();
            sqliteRepo.close();

            // 设置结果
            result.setSuccess(failCount == 0);
            result.setTotalRecords(allLands.size());
            result.setSuccessCount(successCount);
            result.setFailCount(failCount);
            result.setMessage(String.format("Migration completed: %d success, %d failed out of %d total",
                    successCount, failCount, allLands.size()));

            logger.info(result.getMessage());

            // 如果迁移成功，备份原文件
            if (result.isSuccess()) {
                backupYamlFile(yamlFile);
            }

        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Migration failed: " + e.getMessage());
            logger.severe("Migration failed: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    /**
     * 备份 YAML 文件
     */
    private void backupYamlFile(File yamlFile) {
        try {
            File backupFile = new File(yamlFile.getParentFile(), yamlFile.getName() + ".backup");
            if (yamlFile.renameTo(backupFile)) {
                logger.info("Original YAML file backed up to: " + backupFile.getAbsolutePath());
            } else {
                logger.warning("Failed to backup YAML file");
            }
        } catch (Exception e) {
            logger.warning("Failed to backup YAML file: " + e.getMessage());
        }
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
