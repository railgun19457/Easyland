package io.github.railgun19457.easyland.command.commands;

import io.github.railgun19457.easyland.manager.LanguageManager;
import io.github.railgun19457.easyland.command.SubCommand;
import io.github.railgun19457.easyland.domain.Land;
import io.github.railgun19457.easyland.service.LandService;
import io.github.railgun19457.easyland.util.InputValidator;
import io.github.railgun19457.easyland.util.ValidationResult;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 列出领地命令
 */
public class ListCommand extends SubCommand {
    private final LandService landService;
    private final LanguageManager languageManager;

    public ListCommand(LandService landService, LanguageManager languageManager) {
        this.landService = landService;
        this.languageManager = languageManager;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        // 添加性能监控：记录命令开始时间
        long startTime = System.currentTimeMillis();
        org.bukkit.Bukkit.getLogger().info("ListCommand executed by " + sender.getName() +
                                          " with args: " + java.util.Arrays.toString(args));
        
        Player player = getPlayer(sender);
        if (player == null) return false;

        // 如果指定了玩家名，查看该玩家的领地（需要权限）
        Player targetPlayer = player;
        if (args.length > 0) {
            if (!sender.hasPermission("easyland.list.others")) {
                languageManager.sendMessage(player, "error.no-permission");
                return false;
            }
            
            String targetPlayerName = args[0];
            
            // 验证玩家名格式和存在性
            ValidationResult validation = InputValidator.validatePlayerExists(targetPlayerName);
            if (validation instanceof ValidationResult.Failure failure) {
                Bukkit.getLogger().warning("ListCommand: Invalid player name: " + targetPlayerName +
                                         " - " + failure.errorMessage());
                player.sendMessage("§c" + failure.errorMessage());
                return false;
            }
            
            targetPlayer = Bukkit.getPlayer(targetPlayerName);
        }

        // 添加性能监控：记录数据库查询开始时间
        long queryStartTime = System.currentTimeMillis();
        List<Land> lands = landService.findClaimedLandsByOwner(targetPlayer.getUniqueId());
        long queryTime = System.currentTimeMillis() - queryStartTime;
        
        // 添加性能日志
        org.bukkit.Bukkit.getLogger().info("ListCommand: Database query took " + queryTime + "ms, " +
                                          "returned " + lands.size() + " lands");

        if (lands.isEmpty()) {
            if (targetPlayer.equals(player)) {
                languageManager.sendMessage(player, "command.list.no-lands");
            } else {
                languageManager.sendMessage(player, "command.list.no-lands-other", targetPlayer.getName());
            }
            return true;
        }

        // 添加性能监控：检查是否需要分页显示
        if (lands.size() > 20) {
            player.sendMessage("§e警告: 领地数量较多(" + lands.size() + "个)，可能影响显示效果");
            // 这里可以添加分页逻辑
        }

        // 显示领地列表
        player.sendMessage("§6========== " + targetPlayer.getName() + " 的领地 ==========");
        for (Land land : lands) {
            String status = land.isClaimed() ? "§a已认领" : "§7未认领";
            // 使用 getArea() 代替过时的 getChunkCount()，并转换为更直观的显示
            int area = land.getArea();
            String areaDisplay;
            if (area >= 10000) {
                areaDisplay = String.format("%.1f万方块", area / 10000.0);
            } else {
                areaDisplay = area + "方块";
            }
            player.sendMessage(String.format("§e%s §7- %s §7(%s) §7世界: %s",
                    land.landId(),
                    status,
                    areaDisplay,
                    land.worldName()));
        }
        player.sendMessage("§6总计: §e" + lands.size() + " §6个领地");
        
        // 添加性能监控：记录总执行时间
        long totalTime = System.currentTimeMillis() - startTime;
        org.bukkit.Bukkit.getLogger().info("ListCommand: Total execution time: " + totalTime + "ms");

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1 && sender.hasPermission("easyland.list.others")) {
            String partial = args[0];
            
            // 验证输入格式，如果包含危险字符则不提供补全
            if (InputValidator.validatePlayerName(partial) instanceof ValidationResult.Failure) {
                return Collections.emptyList();
            }
            
            // 补全在线玩家名称
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(partial.toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public String getName() {
        return "list";
    }

    @Override
    public String getDescription() {
        return languageManager.getMessage("command.list.description");
    }

    @Override
    public String getUsage() {
        return "/easyland list [玩家名]";
    }

    @Override
    public boolean requiresPlayer() {
        return true;
    }
}
