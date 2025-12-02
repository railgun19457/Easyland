package io.github.railgun19457.easyland.command;

import io.github.railgun19457.easyland.EasyLand;
import io.github.railgun19457.easyland.I18nManager;
import io.github.railgun19457.easyland.exception.MigrationFileNotFoundException;
import io.github.railgun19457.easyland.migration.MigrationManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.logging.Logger;

/**
 * 处理数据迁移命令。
 */
public class MigrateCommand implements CommandExecutor {
    
    @SuppressWarnings("unused")
    private final EasyLand plugin;
    private final I18nManager i18nManager;
    private final Logger logger;
    private final MigrationManager migrationManager;
    
    /**
     * 构造函数。
     *
     * @param plugin 插件主类实例
     */
    public MigrateCommand(EasyLand plugin) {
        this.plugin = plugin;
        this.i18nManager = plugin.getI18nManager();
        this.logger = plugin.getLogger();
        this.migrationManager = new MigrationManager(plugin);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 检查权限
        if (!sender.hasPermission("easyland.admin.migrate")) {
            sender.sendMessage(i18nManager.getMessage("general.no-permission"));
            return true;
        }
        
        // 如果有参数，处理确认或取消
        if (args.length > 0) {
            String action = args[0].toLowerCase();
            
            if (action.equals("confirm")) {
                try {
                    // 执行迁移
                    boolean success = migrationManager.runMigration(sender);
                    
                    if (success) {
                        sender.sendMessage(i18nManager.getMessage("migrate.success-short"));
                        logger.info("数据迁移成功完成，由 " + sender.getName() + " 执行。");
                    } else {
                        sender.sendMessage(i18nManager.getMessage("migrate.failed"));
                        logger.warning("数据迁移失败，由 " + sender.getName() + " 执行。");
                    }
                } catch (Exception e) {
                    // 特别处理文件不存在的情况
                    if (e.getCause() instanceof MigrationFileNotFoundException) {
                        MigrationFileNotFoundException ex = (MigrationFileNotFoundException) e.getCause();
                        sender.sendMessage(i18nManager.getMessage("migrate.failed-with-reason", ex.getMessage()));
                        sender.sendMessage(i18nManager.getMessage("migrate.failed-file-instruction"));
                        logger.warning("迁移失败，由 " + sender.getName() + " 执行。缺失文件: " + ex.getFileName());
                    } else {
                        sender.sendMessage(i18nManager.getMessage("migrate.failed-with-reason", e.getMessage()));
                        sender.sendMessage(i18nManager.getMessage("migrate.failed-check-console"));
                        logger.severe("数据迁移失败，由 " + sender.getName() + " 执行: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
                
            } else if (action.equals("cancel")) {
                sender.sendMessage(i18nManager.getMessage("migrate.cancelled"));
                
            } else {
                sender.sendMessage(i18nManager.getMessage("general.invalid-args", "/el migrate [confirm|cancel]"));
            }
        } else {
            // 没有参数时，显示确认信息
            sender.sendMessage(i18nManager.getMessage("migrate.warning"));
            sender.sendMessage(i18nManager.getMessage("migrate.warning-details"));
            sender.sendMessage(i18nManager.getMessage("migrate.backup-reminder"));
            sender.sendMessage(i18nManager.getMessage("migrate.confirm-instruction"));
            sender.sendMessage(i18nManager.getMessage("migrate.cancel-instruction"));
        }
        
        return true;
    }
}