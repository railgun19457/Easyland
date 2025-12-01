package io.github.railgun19457.easyland.command.commands;

import io.github.railgun19457.easyland.manager.LanguageManager;
import io.github.railgun19457.easyland.command.SubCommand;
import io.github.railgun19457.easyland.service.LandService;
import io.github.railgun19457.easyland.service.ServiceResult;
import io.github.railgun19457.easyland.util.InputValidator;
import io.github.railgun19457.easyland.util.ValidationResult;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
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
        if (args.length < 1) {
            sender.sendMessage(languageManager.getMessage("error.usage") + " " + getUsage());
            return false;
        }

        Player player = (Player) sender;
        String landId = args[0];

        // 验证领地ID格式
        if (InputValidator.validateLandId(landId) instanceof ValidationResult.Failure failure) {
            player.sendMessage("§c" + failure.errorMessage());
            return false;
        }

        ServiceResult<io.github.railgun19457.easyland.domain.Land> result = landService.claimLand(player, landId);

        if (result.isSuccess()) {
            player.sendMessage(languageManager.getMessage("success.land-claimed"));
        } else {
            player.sendMessage(languageManager.getMessage("error.prefix") + result.message());
        }
        return result.isSuccess();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            String partial = args[0];
            
            // 验证输入格式，如果包含危险字符则不提供补全
            if (InputValidator.validateLandId(partial) instanceof ValidationResult.Failure) {
                return Collections.emptyList();
            }
            
            // 返回所有未认领领地的ID
            List<String> landIds = new ArrayList<>();
            landService.getAllUnclaimedLands().forEach(land -> {
                if (land.landId() != null && !land.landId().isEmpty()) {
                    landIds.add(land.landId());
                }
            });
            
            // 过滤匹配的ID并限制数量
            return landIds.stream()
                    .filter(id -> id.toLowerCase().startsWith(partial.toLowerCase()))
                    .limit(30) // 限制返回数量
                    .collect(java.util.stream.Collectors.toList());
        }
        return super.tabComplete(sender, args);
    }

    @Override
    public String getName() {
        return "claim";
    }

    @Override
    public String getPermission() {
        return io.github.railgun19457.easyland.util.Permission.PlayerPermission.CLAIM.getNode();
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
