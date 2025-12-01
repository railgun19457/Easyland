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

import java.util.Arrays;
import java.util.Collections;
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
        if (args.length < 1) {
            sender.sendMessage(languageManager.getMessage("error.usage") + " " + getUsage());
            return false;
        }

        Player player = (Player) sender;
        String targetPlayerName = args[0];

        // 使用InputValidator验证玩家名格式和存在性
        if (InputValidator.validatePlayerExists(targetPlayerName) instanceof ValidationResult.Failure failure) {
            player.sendMessage("§c" + failure.errorMessage());
            return false;
        }

        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
        //noinspection ConstantConditions
        String trustedUuid = targetPlayer.getUniqueId().toString();
        ServiceResult<Void> result = landService.trustPlayer(player, trustedUuid);

        if (result.isSuccess()) {
            player.sendMessage(languageManager.getMessage("success.player-trusted", targetPlayer.getName()));
        } else {
            player.sendMessage(languageManager.getMessage("error.prefix") + result.message());
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
        return super.tabComplete(sender, args);
    }

    @Override
    public String getName() {
        return "trust";
    }

    @Override
    public String getPermission() {
        return io.github.railgun19457.easyland.util.Permission.PlayerPermission.TRUST.getNode();
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
