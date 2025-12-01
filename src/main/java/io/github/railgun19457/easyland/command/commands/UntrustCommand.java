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
        
        // 使用InputValidator验证玩家名格式和存在性
        ValidationResult validation = InputValidator.validatePlayerExists(targetPlayerName);
        if (validation instanceof ValidationResult.Failure failure) {
            Bukkit.getLogger().warning("UntrustCommand: Invalid player name: " + targetPlayerName +
                                     " - " + failure.errorMessage());
            player.sendMessage("§c" + failure.errorMessage());
            return false;
        }
        
        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);

        ServiceResult<Void> result = landService.untrustPlayer(player, targetPlayer.getUniqueId().toString());

        if (result.isSuccess()) {
            languageManager.sendMessage(player, "command.untrust.success", targetPlayer.getName());
        } else {
            player.sendMessage("§c" + result.message());
        }

        return result.isSuccess();
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            // 返回在线玩家列表，但先验证输入
            String partial = args[0];
            
            // 如果输入包含危险字符，不提供补全
            if (InputValidator.validatePlayerName(partial) instanceof ValidationResult.Failure) {
                return Collections.emptyList();
            }
            
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(partial.toLowerCase()))
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
