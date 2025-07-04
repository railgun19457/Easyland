package com.example.easyland;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LandCommand implements CommandExecutor {
    private final LandManager landManager;
    private Location pos1, pos2;

    public LandCommand(LandManager landManager) {
        this.landManager = landManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("只有玩家可以使用此命令。");
            return true;
        }
        Player player = (Player) sender;
        if (args.length == 1 && args[0].equalsIgnoreCase("pos1")) {
            pos1 = player.getLocation();
            player.sendMessage("已设置第一个点。");
        } else if (args.length == 1 && args[0].equalsIgnoreCase("pos2")) {
            pos2 = player.getLocation();
            player.sendMessage("已设置第二个点。");
        } else if (args.length == 1 && args[0].equalsIgnoreCase("create")) {
            if (pos1 == null || pos2 == null) {
                player.sendMessage("请先设置两个点（/land pos1 和 /land pos2）");
                return true;
            }
            boolean success = landManager.createLand(player, pos1, pos2);
            if (success) {
                player.sendMessage("领地创建成功！");
            } else {
                player.sendMessage("你已经拥有一个领地。");
            }
        } else {
            player.sendMessage("用法: /land pos1 | pos2 | create");
        }
        return true;
    }
}
