package io.github.railgun19457.easyland.command.commands;

import io.github.railgun19457.easyland.manager.LanguageManager;
import io.github.railgun19457.easyland.command.SubCommand;
import io.github.railgun19457.easyland.domain.Land;
import io.github.railgun19457.easyland.repository.LandRepository;
import io.github.railgun19457.easyland.service.LandService;
import io.github.railgun19457.easyland.util.InputValidator;
import io.github.railgun19457.easyland.util.ValidationResult;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 显示领地信息命令
 */
public class ShowCommand extends SubCommand {
    private final LandService landService;
    private final LandRepository landRepository;
    private final LanguageManager languageManager;

    public ShowCommand(LandService landService, LandRepository landRepository, LanguageManager languageManager) {
        this.landService = landService;
        this.landRepository = landRepository;
        this.languageManager = languageManager;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = getPlayer(sender);
        if (player == null) return false;

        if (args.length < 1) {
            languageManager.sendMessage(player, "command.show.usage");
            return false;
        }

        String landId = args[0];
        
        // 验证领地ID格式
        ValidationResult validation = InputValidator.validateLandId(landId);
        if (validation instanceof ValidationResult.Failure failure) {
            Bukkit.getLogger().warning("ShowCommand: Invalid land ID: " + landId +
                                     " - " + failure.errorMessage());
            player.sendMessage("§c" + failure.errorMessage());
            return false;
        }
        
        Optional<Land> landOpt = landRepository.findByLandId(landId);

        if (landOpt.isEmpty()) {
            languageManager.sendMessage(player, "error.land-not-found", landId);
            return false;
        }

        Land land = landOpt.get();

        // 显示领地详细信息
        player.sendMessage("§6========== 领地信息 ==========");
        player.sendMessage("§e领地ID: §f" + land.landId());

        if (land.isClaimed()) {
            String ownerName = Bukkit.getOfflinePlayer(java.util.UUID.fromString(land.owner())).getName();
            player.sendMessage("§e所有者: §f" + (ownerName != null ? ownerName : "未知"));
        } else {
            player.sendMessage("§e状态: §7未认领");
        }

        player.sendMessage("§e世界: §f" + land.worldName());
        player.sendMessage("§e坐标范围: §f(" + land.getMinX() + ", " + land.getMinZ() + ") -> ("
                + land.getMaxX() + ", " + land.getMaxZ() + ")");
        // 使用 getArea() 代替过时的 getChunkCount()，并转换为更直观的显示
        int area = land.getArea();
        String areaDisplay;
        if (area >= 10000) {
            areaDisplay = String.format("%.1f万方块", area / 10000.0);
        } else {
            areaDisplay = area + "方块";
        }
        player.sendMessage("§e面积: §f" + areaDisplay);

        // 显示信任列表
        if (!land.trusted().isEmpty()) {
            player.sendMessage("§e信任玩家: §f" + land.trusted().size() + " 人");
            StringBuilder trustedNames = new StringBuilder();
            for (String uuid : land.trusted()) {
                String name = Bukkit.getOfflinePlayer(java.util.UUID.fromString(uuid)).getName();
                if (name != null) {
                    if (trustedNames.length() > 0) trustedNames.append(", ");
                    trustedNames.append(name);
                }
            }
            if (trustedNames.length() > 0) {
                player.sendMessage("  §7" + trustedNames.toString());
            }
        } else {
            player.sendMessage("§e信任玩家: §7无");
        }

        // 显示保护规则
        player.sendMessage("§e保护规则:");
        land.protectionRules().forEach((rule, enabled) -> {
            String status = enabled ? "§a启用" : "§c禁用";
            player.sendMessage("  §7" + rule + ": " + status);
        });

        player.sendMessage("§6==============================");

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            // 添加调试日志：记录Tab补全请求
            Bukkit.getLogger().info("ShowCommand tabComplete requested by " + sender.getName() +
                                   " with partial: " + args[0]);
            
            String partial = args[0];
            
            // 验证输入格式，如果包含危险字符则不提供补全
            if (InputValidator.validateLandId(partial) instanceof ValidationResult.Failure) {
                Bukkit.getLogger().warning("ShowCommand: Invalid input for tab completion: " + partial);
                return Collections.emptyList();
            }
            
            // 检查权限：只有有权限的用户才能看到所有领地
            if (!sender.hasPermission("easyland.show.all")) {
                // 如果没有查看所有领地的权限，只返回用户自己的领地
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    Bukkit.getLogger().info("ShowCommand: Limiting tab completion to player's own lands for " + player.getName());
                    return landService.findClaimedLandsByOwner(player.getUniqueId()).stream()
                            .map(Land::landId)
                            .filter(id -> id != null && !id.isEmpty()) // 确保ID不为空
                            .filter(id -> id.toLowerCase().startsWith(partial.toLowerCase()))
                            .limit(20) // 限制返回数量，防止信息泄露
                            .collect(Collectors.toList());
                }
                return Collections.emptyList();
            }
            
            // 有权限的用户可以看到所有领地，但仍然需要限制数量
            List<String> allLandIds = landRepository.findAll().stream()
                    .map(Land::landId)
                    .filter(id -> id != null && !id.isEmpty()) // 确保ID不为空
                    .filter(id -> id.toLowerCase().startsWith(partial.toLowerCase()))
                    .limit(50) // 限制返回数量，防止信息泄露
                    .collect(Collectors.toList());
            
            // 添加调试日志：记录返回的补全结果数量
            Bukkit.getLogger().info("ShowCommand: Returning " + allLandIds.size() +
                                   " land IDs for tab completion (limited for security)");
            
            return allLandIds;
        }
        return Collections.emptyList();
    }

    @Override
    public String getName() {
        return "show";
    }

    @Override
    public String getDescription() {
        return languageManager.getMessage("command.show.description");
    }

    @Override
    public String getUsage() {
        return "/easyland show <领地ID>";
    }

    @Override
    public boolean requiresPlayer() {
        return true;
    }
}
