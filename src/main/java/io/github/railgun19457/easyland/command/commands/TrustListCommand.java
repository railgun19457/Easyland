package io.github.railgun19457.easyland.command.commands;

import io.github.railgun19457.easyland.manager.LanguageManager;
import io.github.railgun19457.easyland.command.SubCommand;
import io.github.railgun19457.easyland.domain.Land;
import io.github.railgun19457.easyland.service.LandService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * 显示信任列表命令
 */
public class TrustListCommand extends SubCommand {
    private final LandService landService;
    private final LanguageManager languageManager;

    public TrustListCommand(LandService landService, LanguageManager languageManager) {
        this.landService = landService;
        this.languageManager = languageManager;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = getPlayer(sender);
        if (player == null) return false;

        List<Land> lands = landService.findClaimedLandsByOwner(player.getUniqueId());

        if (lands.isEmpty()) {
            languageManager.sendMessage(player, "command.trustlist.no-lands");
            return true;
        }

        player.sendMessage("§6========== 信任列表 ==========");

        for (Land land : lands) {
            player.sendMessage("§e领地: §f" + land.getLandId());

            if (land.getTrusted().isEmpty()) {
                player.sendMessage("  §7无信任玩家");
            } else {
                player.sendMessage("  §7信任玩家 (" + land.getTrusted().size() + "):");
                for (String uuid : land.getTrusted()) {
                    String name = Bukkit.getOfflinePlayer(java.util.UUID.fromString(uuid)).getName();
                    player.sendMessage("    §f- " + (name != null ? name : uuid));
                }
            }
        }

        player.sendMessage("§6==============================");

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }

    @Override
    public String getName() {
        return "trustlist";
    }

    @Override
    public String getDescription() {
        return languageManager.getMessage("command.trustlist.description");
    }

    @Override
    public String getUsage() {
        return "/easyland trustlist";
    }

    @Override
    public boolean requiresPlayer() {
        return true;
    }
}
