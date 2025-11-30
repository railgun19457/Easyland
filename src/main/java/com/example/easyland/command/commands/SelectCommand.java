package com.example.easyland.command.commands;

import com.example.easyland.manager.LanguageManager;
import com.example.easyland.command.SubCommand;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;

import java.util.Collections;
import java.util.List;

/**
 * 选择命令 - 给予玩家选择工具
 */
public class SelectCommand extends SubCommand {
    private final LanguageManager languageManager;

    public SelectCommand(LanguageManager languageManager) {
        this.languageManager = languageManager;
    }

    @Override
    public String getName() {
        return "select";
    }

    @Override
    public String getDescription() {
        return languageManager.getMessage("command.select.description");
    }

    @Override
    public String getUsage() {
        return "/land select";
    }

    @Override
    public String getPermission() {
        return "easyland.select";
    }

    @Override
    public boolean requiresPlayer() {
        return true;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = (Player) sender;

        // 创建选择工具（木锄）
        ItemStack selectTool = new ItemStack(Material.WOODEN_HOE);
        ItemMeta meta = selectTool.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(languageManager.getMessage("select.tool-name")));
            meta.lore(Collections.singletonList(
                Component.text(languageManager.getMessage("select.tool-lore"))
            ));
            selectTool.setItemMeta(meta);
        }

        // 给予玩家工具
        player.getInventory().addItem(selectTool);
        languageManager.sendMessage(player, "select.tool-given");

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
