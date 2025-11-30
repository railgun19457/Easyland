package com.example.easyland.command.commands;

import com.example.easyland.manager.LanguageManager;
import com.example.easyland.command.SubCommand;
import com.example.easyland.domain.Land;
import com.example.easyland.repository.LandRepository;
import com.example.easyland.service.LandService;
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
        Optional<Land> landOpt = landRepository.findByLandId(landId);

        if (landOpt.isEmpty()) {
            languageManager.sendMessage(player, "error.land-not-found", landId);
            return false;
        }

        Land land = landOpt.get();

        // 显示领地详细信息
        player.sendMessage("§6========== 领地信息 ==========");
        player.sendMessage("§e领地ID: §f" + land.getLandId());

        if (land.isClaimed()) {
            String ownerName = Bukkit.getOfflinePlayer(java.util.UUID.fromString(land.getOwner())).getName();
            player.sendMessage("§e所有者: §f" + (ownerName != null ? ownerName : "未知"));
        } else {
            player.sendMessage("§e状态: §7未认领");
        }

        player.sendMessage("§e世界: §f" + land.getWorldName());
        player.sendMessage("§e区块范围: §f(" + land.getMinX() + ", " + land.getMinZ() + ") -> ("
                + land.getMaxX() + ", " + land.getMaxZ() + ")");
        player.sendMessage("§e区块数量: §f" + land.getChunkCount());

        // 显示信任列表
        if (!land.getTrusted().isEmpty()) {
            player.sendMessage("§e信任玩家: §f" + land.getTrusted().size() + " 人");
            StringBuilder trustedNames = new StringBuilder();
            for (String uuid : land.getTrusted()) {
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
        land.getProtectionRules().forEach((rule, enabled) -> {
            String status = enabled ? "§a启用" : "§c禁用";
            player.sendMessage("  §7" + rule + ": " + status);
        });

        player.sendMessage("§6==============================");

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            // 补全所有领地ID
            return landRepository.findAll().stream()
                    .map(Land::getLandId)
                    .filter(id -> id.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
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
