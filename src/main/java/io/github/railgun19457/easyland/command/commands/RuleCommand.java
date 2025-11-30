package io.github.railgun19457.easyland.command.commands;

import io.github.railgun19457.easyland.manager.ConfigManager;
import io.github.railgun19457.easyland.manager.LanguageManager;
import io.github.railgun19457.easyland.command.SubCommand;
import io.github.railgun19457.easyland.domain.Land;
import io.github.railgun19457.easyland.service.LandService;
import io.github.railgun19457.easyland.service.ServiceResult;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 设置保护规则命令
 */
public class RuleCommand extends SubCommand {
    private final LandService landService;
    private final LanguageManager languageManager;

    public RuleCommand(LandService landService, LanguageManager languageManager) {
        this.landService = landService;
        this.languageManager = languageManager;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = getPlayer(sender);
        if (player == null) return false;

        if (args.length < 3) {
            languageManager.sendMessage(player, "command.rule.usage");
            return false;
        }

        String landId = args[0];
        String ruleName = args[1];
        String value = args[2].toLowerCase();

        // 验证规则名称
        if (!isValidRule(ruleName)) {
            languageManager.sendMessage(player, "error.invalid-rule", ruleName);
            return false;
        }

        // 解析布尔值
        boolean enabled;
        if (value.equals("true") || value.equals("on") || value.equals("enable")) {
            enabled = true;
        } else if (value.equals("false") || value.equals("off") || value.equals("disable")) {
            enabled = false;
        } else {
            languageManager.sendMessage(player, "error.invalid-value", value);
            return false;
        }

        ServiceResult<Void> result = landService.setProtectionRule(player, landId, ruleName, enabled);

        if (result.isSuccess()) {
            String status = enabled ? languageManager.getMessage("log.enabled") : languageManager.getMessage("log.disabled");
            languageManager.sendMessage(player, "command.rule.success", ruleName, status);
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
        } else if (args.length == 2) {
            // 补全保护规则名称
            List<String> rules = Arrays.asList(ConfigManager.getProtectionRules());
            return rules.stream()
                    .filter(rule -> rule.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 3) {
            // 补全 true/false
            List<String> values = Arrays.asList("true", "false", "enable", "disable", "on", "off");
            return values.stream()
                    .filter(val -> val.startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * 验证规则名称是否有效
     */
    private boolean isValidRule(String ruleName) {
        String[] validRules = ConfigManager.getProtectionRules();
        for (String rule : validRules) {
            if (rule.equals(ruleName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getName() {
        return "rule";
    }

    @Override
    public String getDescription() {
        return languageManager.getMessage("command.rule.description");
    }

    @Override
    public String getUsage() {
        return "/easyland rule <领地ID> <规则名> <true|false>";
    }

    @Override
    public boolean requiresPlayer() {
        return true;
    }
}
