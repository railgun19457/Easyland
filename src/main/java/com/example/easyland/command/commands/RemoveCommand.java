package com.example.easyland.command.commands;

import com.example.easyland.manager.LanguageManager;
import com.example.easyland.command.SubCommand;
import com.example.easyland.domain.Land;
import com.example.easyland.service.LandService;
import com.example.easyland.service.ServiceResult;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 删除领地命令
 */
public class RemoveCommand extends SubCommand {
    private final LandService landService;
    private final LanguageManager languageManager;

    public RemoveCommand(LandService landService, LanguageManager languageManager) {
        this.landService = landService;
        this.languageManager = languageManager;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = getPlayer(sender);
        if (player == null) return false;

        if (args.length < 1) {
            languageManager.sendMessage(player, "command.remove.usage");
            return false;
        }

        String landId = args[0];
        ServiceResult<Void> result = landService.removeLand(player, landId);

        if (result.isSuccess()) {
            languageManager.sendMessage(player, "command.remove.success", landId);
        } else {
            player.sendMessage("§c" + result.getMessage());
        }

        return result.isSuccess();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1 && sender instanceof Player) {
            Player player = (Player) sender;
            // 补全玩家拥有的领地ID
            return landService.findClaimedLandsByOwner(player.getUniqueId()).stream()
                    .map(Land::getLandId)
                    .filter(id -> id.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public String getName() {
        return "remove";
    }

    @Override
    public String getDescription() {
        return languageManager.getMessage("command.remove.description");
    }

    @Override
    public String getUsage() {
        return "/easyland remove <领地ID>";
    }

    @Override
    public boolean requiresPlayer() {
        return true;
    }
}
