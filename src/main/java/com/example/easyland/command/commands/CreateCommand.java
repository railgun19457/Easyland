package com.example.easyland.command.commands;

import com.example.easyland.manager.LanguageManager;
import com.example.easyland.command.SubCommand;
import com.example.easyland.service.LandService;
import com.example.easyland.service.ServiceResult;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * 创建领地命令
 */
public class CreateCommand extends SubCommand {
    private final LandService landService;
    private final LanguageManager languageManager;
    private final Map<String, Location[]> selections;

    public CreateCommand(LandService landService, LanguageManager languageManager, Map<String, Location[]> selections) {
        this.landService = landService;
        this.languageManager = languageManager;
        this.selections = selections;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!checkPlayer(sender)) {
            sender.sendMessage(languageManager.getMessage("error.player-only"));
            return false;
        }

        Player player = getPlayer(sender);
        String playerUuid = player.getUniqueId().toString();

        // 检查是否有选区
        Location[] selection = selections.get(playerUuid);
        if (selection == null || selection[0] == null || selection[1] == null) {
            player.sendMessage(languageManager.getMessage("error.no-selection"));
            return false;
        }

        // 获取领地ID（可选）
        String landId = args.length > 0 ? args[0] : "";

        // 创建领地（使用世界坐标）
        ServiceResult<com.example.easyland.domain.Land> result =
            landService.createLand(selection[0], selection[1], landId);

        if (result.isSuccess()) {
            player.sendMessage(languageManager.getMessage("success.land-created"));
            selections.remove(playerUuid);
        } else {
            player.sendMessage(languageManager.getMessage("error.prefix") + result.getMessage());
        }

        return result.isSuccess();
    }

    @Override
    public String getName() {
        return "create";
    }

    @Override
    public String getDescription() {
        return languageManager.getMessage("command.create.description");
    }

    @Override
    public String getUsage() {
        return "/easyland create [领地ID]";
    }
}
