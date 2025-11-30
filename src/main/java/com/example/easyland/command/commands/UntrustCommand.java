package com.example.easyland.command.commands;

import com.example.easyland.manager.LanguageManager;
import com.example.easyland.command.SubCommand;
import com.example.easyland.service.LandService;
import com.example.easyland.service.ServiceResult;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 取消信任玩家命令
 */
public class UntrustCommand extends SubCommand {
    private final LandService landService;
    private final LanguageManager languageManager;

    public UntrustCommand(LandService landService, LanguageManager languageManager) {
        this.landService = landService;
        this.languageManager = languageManager;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = getPlayer(sender);
        if (player == null) return false;

        if (args.length < 1) {
            languageManager.sendMessage(player, "command.untrust.usage");
            return false;
        }

        String targetPlayerName = args[0];
        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);

        if (targetPlayer == null) {
            languageManager.sendMessage(player, "error.player-not-found", targetPlayerName);
            return false;
        }

        ServiceResult<Void> result = landService.untrustPlayer(player, targetPlayer.getUniqueId().toString());

        if (result.isSuccess()) {
            languageManager.sendMessage(player, "command.untrust.success", targetPlayer.getName());
        } else {
            player.sendMessage("§c" + result.getMessage());
        }

        return result.isSuccess();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
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
        return "untrust";
    }

    @Override
    public String getDescription() {
        return languageManager.getMessage("command.untrust.description");
    }

    @Override
    public String getUsage() {
        return "/easyland untrust <玩家名>";
    }

    @Override
    public boolean requiresPlayer() {
        return true;
    }
}
