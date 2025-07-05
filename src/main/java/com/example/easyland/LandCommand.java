package com.example.easyland;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;

public class LandCommand implements CommandExecutor {
    private final LandManager landManager;
    private final LandSelectListener landSelectListener;

    public LandCommand(LandManager landManager, LandSelectListener landSelectListener) {
        this.landManager = landManager;
        this.landSelectListener = landSelectListener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("只有玩家可以使用此命令。");
            return true;
        }
        Player player = (Player) sender;
        if (args.length == 1 && args[0].equalsIgnoreCase("select")) {
            ItemStack wand = new ItemStack(Material.WOODEN_SHOVEL);
            ItemMeta meta = wand.getItemMeta();
            meta.displayName(Component.text("§a领地选择"));
            wand.setItemMeta(meta);
            player.getInventory().addItem(wand);
            player.sendMessage("已获得领地选择木铲，请右键区块内任意方块进行选点。");
        } else if (args.length == 1 && args[0].equalsIgnoreCase("create")) {
            Chunk[] selects = landSelectListener.getPlayerSelects(player);
            if (selects[0] == null || selects[1] == null) {
                player.sendMessage("请先用木铲右键选择两个区块。");
                return true;
            }
            boolean success = landManager.createLandByChunk(player, selects[0], selects[1]);
            if (success) {
                player.sendMessage("领地创建成功！");
            } else {
                player.sendMessage("你已经拥有一个领地。");
            }
        } else {
            player.sendMessage("用法: /easyland select | create");
        }
        return true;
    }
}
