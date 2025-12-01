package io.github.railgun19457.easyland.command.commands;

import io.github.railgun19457.easyland.manager.LanguageManager;
import io.github.railgun19457.easyland.command.SubCommand;
import io.github.railgun19457.easyland.domain.Land;
import io.github.railgun19457.easyland.service.LandService;
import io.github.railgun19457.easyland.service.ServiceResult;
import io.github.railgun19457.easyland.util.InputValidator;
import io.github.railgun19457.easyland.util.ValidationResult;
import org.bukkit.Bukkit;
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
        
        // 验证领地ID格式
        ValidationResult validation = InputValidator.validateLandId(landId);
        if (validation instanceof ValidationResult.Failure failure) {
            Bukkit.getLogger().warning("RemoveCommand: Invalid land ID: " + landId +
                                     " - " + failure.errorMessage());
            player.sendMessage("§c" + failure.errorMessage());
            return false;
        }
        
        ServiceResult<Void> result = landService.removeLand(player, landId);

        if (result.isSuccess()) {
            languageManager.sendMessage(player, "command.remove.success", landId);
        } else {
            player.sendMessage("§c" + result.message());
        }

        return result.isSuccess();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1 && sender instanceof Player) {
            Player player = (Player) sender;
            String partial = args[0];
            
            // 验证输入格式，如果包含危险字符则不提供补全
            if (InputValidator.validateLandId(partial) instanceof ValidationResult.Failure) {
                return Collections.emptyList();
            }
            
            // 补全玩家拥有的领地ID
            return landService.findClaimedLandsByOwner(player.getUniqueId()).stream()
                    .map(Land::landId)
                    .filter(id -> id != null && !id.isEmpty()) // 确保ID不为空
                    .filter(id -> id.toLowerCase().startsWith(partial.toLowerCase()))
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
