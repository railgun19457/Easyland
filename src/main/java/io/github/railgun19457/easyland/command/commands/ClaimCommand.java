package io.github.railgun19457.easyland.command.commands;

import io.github.railgun19457.easyland.manager.LanguageManager;
import io.github.railgun19457.easyland.command.SubCommand;
import io.github.railgun19457.easyland.service.LandService;
import io.github.railgun19457.easyland.service.ServiceResult;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * 认领领地命令
 */
public class ClaimCommand extends SubCommand {
    private final LandService landService;
    private final LanguageManager languageManager;

    public ClaimCommand(LandService landService, LanguageManager languageManager) {
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
        String landId = args[0];

        ServiceResult<io.github.railgun19457.easyland.domain.Land> result =
            landService.claimLand(player, landId);

        if (result.isSuccess()) {
            player.sendMessage(languageManager.getMessage("success.land-claimed"));
        } else {
            player.sendMessage(languageManager.getMessage("error.prefix") + result.getMessage());
        }

        return result.isSuccess();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            // 返回所有未认领领地的ID
            List<String> landIds = new ArrayList<>();
            landService.getAllUnclaimedLands().forEach(land -> {
                if (land.getLandId() != null && !land.getLandId().isEmpty()) {
                    landIds.add(land.getLandId());
                }
            });
            return landIds;
        }
        return super.tabComplete(sender, args);
    }

    @Override
    public String getName() {
        return "claim";
    }

    @Override
    public String getDescription() {
        return languageManager.getMessage("command.claim.description");
    }

    @Override
    public String getUsage() {
        return "/easyland claim <领地ID>";
    }
}
