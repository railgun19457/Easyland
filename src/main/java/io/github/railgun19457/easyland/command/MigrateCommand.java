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
        
        // 显示确认信息
        sender.sendMessage("§e[Easyland] §c警告: 您即将执行数据迁移操作！");
        sender.sendMessage("§e[Easyland] §c此操作将从旧版YAML文件迁移数据到新的SQLite数据库。");
        sender.sendMessage("§e[Easyland] §c建议在执行前备份您的数据。");
        sender.sendMessage("§e[Easyland] §a输入 §f/el migrate confirm §a来确认执行迁移。");
        sender.sendMessage("§e[Easyland] §a输入 §f/el migrate cancel §a来取消操作。");
        
        // 如果有参数，处理确认或取消
        if (args.length > 0) {
            String action = args[0].toLowerCase();
            
            if (action.equals("confirm")) {
                try {
                    // 执行迁移
                    boolean success = migrationManager.runMigration(sender);
                    
                    if (success) {
                        sender.sendMessage("§a[Easyland] 数据迁移成功完成！");
                        logger.info("数据迁移成功完成，由 " + sender.getName() + " 执行。");
                    } else {
                        sender.sendMessage("§c[Easyland] 数据迁移失败，请检查控制台获取详细信息。");
                        logger.warning("数据迁移失败，由 " + sender.getName() + " 执行。");
                    }
                } catch (Exception e) {
                    // 特别处理文件不存在的情况
                    if (e.getCause() instanceof MigrationFileNotFoundException) {
                        MigrationFileNotFoundException ex = (MigrationFileNotFoundException) e.getCause();
                        sender.sendMessage("§c[Easyland] 迁移失败: " + ex.getMessage());
                        sender.sendMessage("§e[Easyland] 请确保旧版数据文件已正确放置在插件数据目录的 oldfiles/ 文件夹中。");
                        logger.warning("迁移失败，由 " + sender.getName() + " 执行。缺失文件: " + ex.getFileName());
                    } else {
                        sender.sendMessage("§c[Easyland] 数据迁移失败: " + e.getMessage());
                        sender.sendMessage("§e[Easyland] 请检查控制台获取详细错误信息。");
                        logger.severe("数据迁移失败，由 " + sender.getName() + " 执行: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
                
            } else if (action.equals("cancel")) {
                sender.sendMessage("§e[Easyland] 数据迁移已取消。");
                
            } else {
                sender.sendMessage(i18nManager.getMessage("general.invalid-args", "/el migrate [confirm|cancel]"));
            }
        }
        
        return true;
    }
}