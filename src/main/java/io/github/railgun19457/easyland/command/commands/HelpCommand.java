package io.github.railgun19457.easyland.command.commands;

import io.github.railgun19457.easyland.manager.LanguageManager;
import io.github.railgun19457.easyland.command.SubCommand;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * 帮助命令
 */
public class HelpCommand extends SubCommand {
    private final LanguageManager languageManager;
    private final List<SubCommand> allCommands;

    public HelpCommand(LanguageManager languageManager, List<SubCommand> allCommands) {
        this.languageManager = languageManager;
        this.allCommands = allCommands;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        sender.sendMessage("§6========== Easyland 帮助 ==========");

        for (SubCommand cmd : allCommands) {
            sender.sendMessage("§e" + cmd.getUsage() + " §7- " + cmd.getDescription());
        }

        sender.sendMessage("§6===================================");
        return true;
    }

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String getDescription() {
        return languageManager.getMessage("command.help.description");
    }

    @Override
    public String getUsage() {
        return "/easyland help";
    }

    @Override
    public boolean requiresPlayer() {
        return false;
    }
}
