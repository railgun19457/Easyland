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
        Player player = getPlayer(sender);
        if (player == null) return false;

        // 如果指定了玩家名，查看该玩家的领地（需要权限）
        Player targetPlayer = player;
        if (args.length > 0) {
            if (!sender.hasPermission("easyland.list.others")) {
                languageManager.sendMessage(player, "error.no-permission");
                return false;
            }
            targetPlayer = Bukkit.getPlayer(args[0]);
            if (targetPlayer == null) {
                languageManager.sendMessage(player, "error.player-not-found", args[0]);
                return false;
            }
        }

        List<Land> lands = landService.findClaimedLandsByOwner(targetPlayer.getUniqueId());

        if (lands.isEmpty()) {
            if (targetPlayer.equals(player)) {
                languageManager.sendMessage(player, "command.list.no-lands");
            } else {
                languageManager.sendMessage(player, "command.list.no-lands-other", targetPlayer.getName());
            }
            return true;
        }

        // 显示领地列表
        player.sendMessage("§6========== " + targetPlayer.getName() + " 的领地 ==========");
        for (Land land : lands) {
            String status = land.isClaimed() ? "§a已认领" : "§7未认领";
            player.sendMessage(String.format("§e%s §7- %s §7(%d 区块) §7世界: %s",
                    land.getLandId(),
                    status,
                    land.getChunkCount(),
                    land.getWorldName()));
        }
        player.sendMessage("§6总计: §e" + lands.size() + " §6个领地");

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1 && sender.hasPermission("easyland.list.others")) {
            // 补全在线玩家名称
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
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
