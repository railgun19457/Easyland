package io.github.railgun19457.easyland.command.commands;

import io.github.railgun19457.easyland.manager.LanguageManager;
import io.github.railgun19457.easyland.command.SubCommand;
import io.github.railgun19457.easyland.service.LandService;
import io.github.railgun19457.easyland.service.ServiceResult;
import io.github.railgun19457.easyland.util.InputValidator;
import io.github.railgun19457.easyland.util.ValidationResult;
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
        // 添加调试日志：记录命令执行
        org.bukkit.Bukkit.getLogger().info("CreateCommand executed by " + sender.getName() +
                                          " with args: " + java.util.Arrays.toString(args));
        
        if (!checkPlayer(sender)) {
            sender.sendMessage(languageManager.getMessage("error.player-only"));
            return false;
        }

        Player player = getPlayer(sender);
        String playerUuid = player.getUniqueId().toString();

        // 检查是否有选区
        Location[] selection = selections.get(playerUuid);
        if (selection == null || selection[0] == null || selection[1] == null) {
            // 添加调试日志：记录选区缺失
            org.bukkit.Bukkit.getLogger().info("CreateCommand: No selection found for player " + player.getName());
            player.sendMessage(languageManager.getMessage("error.no-selection"));
            // 添加用户友好的提示
            player.sendMessage("§7提示: 使用 /easyland select 获取选择工具");
            return false;
        }

        // 获取领地ID（可选）
        String landId = args.length > 0 ? args[0] : "";
        
        // 添加参数验证
        if (!landId.isEmpty()) {
            ValidationResult validation = InputValidator.validateLandId(landId);
            if (validation instanceof ValidationResult.Failure failure) {
                org.bukkit.Bukkit.getLogger().warning("CreateCommand: Invalid land ID: " + landId +
                                                     " - " + failure.errorMessage());
                player.sendMessage("§c" + failure.errorMessage());
                return false;
            }
        }

        // 添加调试日志：记录领地创建尝试
        org.bukkit.Bukkit.getLogger().info("CreateCommand: Attempting to create land '" + landId +
                                          "' for player " + player.getName());

        // 创建领地（使用世界坐标）
        ServiceResult<io.github.railgun19457.easyland.domain.Land> result =
            landService.createLand(selection[0], selection[1], landId);

        if (result.isSuccess()) {
            player.sendMessage(languageManager.getMessage("success.land-created"));
            if (!landId.isEmpty()) {
                player.sendMessage("§a领地ID: " + landId);
            }
            selections.remove(playerUuid);
        } else {
            player.sendMessage(languageManager.getMessage("error.prefix") + result.message());
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
