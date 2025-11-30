package com.example.easyland.command.commands;

import com.example.easyland.manager.LanguageManager;
import com.example.easyland.command.SubCommand;
import com.example.easyland.service.LandService;
import com.example.easyland.service.ServiceResult;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 信任玩家命令
 */
public class TrustCommand extends SubCommand {
    private final LandService landService;
    private final LanguageManager languageManager;

    public TrustCommand(LandService landService, LanguageManager languageManager) {
        this.landService = landService;
        this.languageManager = languageManager;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!checkPlayer(sender)) {
            sender.sendMessage(languageManager.getMessage("error.player-only"));
            return false;
        }

        if (args.length < 1) {
            sender.sendMessage(languageManager.getMessage("error.usage") + " " + getUsage());
            return false;
        }

        Player player = getPlayer(sender);
        Player targetPlayer = Bukkit.getPlayer(args[0]);

        if (targetPlayer == null) {
            player.sendMessage(languageManager.getMessage("error.player-not-found"));
            return false;
        }

        String trustedUuid = targetPlayer.getUniqueId().toString();
        ServiceResult<Void> result = landService.trustPlayer(player, trustedUuid);

        if (result.isSuccess()) {
            player.sendMessage(languageManager.getMessage("success.player-trusted", targetPlayer.getName()));
        } else {
            player.sendMessage(languageManager.getMessage("error.prefix") + result.getMessage());
        }

        return result.isSuccess();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            // 返回在线玩家列表
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return super.tabComplete(sender, args);
    }

    @Override
    public String getName() {
        return "trust";
    }

    @Override
    public String getDescription() {
        return languageManager.getMessage("command.trust.description");
    }

    @Override
    public String getUsage() {
        return "/easyland trust <玩家名>";
    }
}
