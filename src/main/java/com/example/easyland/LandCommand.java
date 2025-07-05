package com.example.easyland;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.text.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class LandCommand implements CommandExecutor, TabCompleter {
    private final LandManager landManager;
    private final LandSelectListener landSelectListener;
    private final int showDurationSeconds;
    private final JavaPlugin plugin;
    private static final String BASE_PERMISSION = "easyland.base";
    private static final String SELECT_PERMISSION = "easyland.select";
    private static final String CREATE_PERMISSION = "easyland.create";
    private static final String CLAIM_PERMISSION = "easyland.claim";
    private static final String UNCLAIM_PERMISSION = "easyland.unclaim";
    private static final String TRUST_PERMISSION = "easyland.trust";
    private static final String UNTRUST_PERMISSION = "easyland.untrust";

    public LandCommand(JavaPlugin plugin, LandManager landManager, LandSelectListener landSelectListener, int showDurationSeconds) {
        this.plugin = plugin;
        this.landManager = landManager;
        this.landSelectListener = landSelectListener;
        this.showDurationSeconds = showDurationSeconds;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("只有玩家可以使用此指令。");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission(BASE_PERMISSION)) {
            player.sendMessage("§c你没有权限使用此指令！");
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("select")) {
            if (!player.hasPermission(SELECT_PERMISSION)) {
                player.sendMessage("§c你没有权限选择领地！");
                return true;
            }
            ItemStack wand = new ItemStack(Material.WOODEN_HOE);
            ItemMeta meta = wand.getItemMeta();
            meta.displayName(Component.text("§a领地选择"));
            wand.setItemMeta(meta);
            player.getInventory().addItem(wand);
            player.sendMessage("已获得领地选择工具，请右键区块内任意方块进行选点。");
        } else if (args.length == 1 && args[0].equalsIgnoreCase("create")) {
            if (!player.hasPermission(CREATE_PERMISSION)) {
                player.sendMessage("§c你没有权限创建领地！");
                return true;
            }
            Chunk[] selects = landSelectListener.getPlayerSelects(player);
            if (selects[0] == null || selects[1] == null) {
                player.sendMessage("请先用工具右键选择两个区块。");
                return true;
            }
            boolean success = landManager.createLandByChunk(selects[0], selects[1]);
            if (success) {
                player.sendMessage("无主领地已创建，可用 /easyland claim 认领。");
            } else {
                player.sendMessage("该区域已存在领地。");
            }
        } else if (args.length == 1 && args[0].equalsIgnoreCase("claim")) {
            if (!player.hasPermission(CLAIM_PERMISSION)) {
                player.sendMessage("§c你没有权限认领领地！");
                return true;
            }
            Chunk[] selects = landSelectListener.getPlayerSelects(player);
            if (selects[0] == null || selects[1] == null) {
                player.sendMessage("请先用工具右键选择两个区块。");
                return true;
            }
            // 新增：判断玩家当前所处区块是否在无主领地内
            Chunk playerChunk = player.getLocation().getChunk();
            boolean inUnclaimed = false;
            for (ChunkLand land : landManager.getAllUnclaimedLands()) {
                if (land.contains(playerChunk)) {
                    inUnclaimed = true;
                    break;
                }
            }
            if (!inUnclaimed) {
                player.sendMessage("你必须站在无主领地内才能认领！");
                return true;
            }
            boolean success = landManager.claimLand(player, selects[0], selects[1]);
            if (success) {
                player.sendMessage("领地认领成功！");
            } else {
                player.sendMessage("认领失败，该区域无可认领的领地或已被认领。");
            }
        } else if (args.length == 1 && args[0].equalsIgnoreCase("unclaim")) {
            if (!player.hasPermission(UNCLAIM_PERMISSION)) {
                player.sendMessage("§c你没有权限放弃领地！");
                return true;
            }
            boolean success = landManager.unclaimLand(player);
            if (success) {
                player.sendMessage("你已放弃领地，该区域变为无主领地。");
            } else {
                player.sendMessage("你没有可放弃的领地。");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("trust")) {
            if (!player.hasPermission(TRUST_PERMISSION)) {
                player.sendMessage("§c你没有权限信任他人！");
                return true;
            }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            if (target == null || !target.hasPlayedBefore()) {
                player.sendMessage("§c目标玩家不存在。");
                return true;
            }
            boolean success = landManager.trustPlayer(player, target.getUniqueId().toString());
            if (success) {
                player.sendMessage("已信任玩家 " + target.getName() + "，其可在你的领地内交互。");
            } else {
                player.sendMessage("信任失败，你没有领地或已信任该玩家。");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("untrust")) {
            if (!player.hasPermission(UNTRUST_PERMISSION)) {
                player.sendMessage("§c你没有权限取消信任！");
                return true;
            }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            if (target == null || !target.hasPlayedBefore()) {
                player.sendMessage("§c目标玩家不存在。");
                return true;
            }
            boolean success = landManager.untrustPlayer(player, target.getUniqueId().toString());
            if (success) {
                player.sendMessage("已取消对玩家 " + target.getName() + " 的信任。");
            } else {
                player.sendMessage("取消信任失败，你没有领地或未信任该玩家。");
            }
        } else if (args.length == 1 && args[0].equalsIgnoreCase("show")) {
            ChunkLand land = landManager.getLand(player);
            if (land == null) {
                player.sendMessage("你没有已认领的领地。");
                return true;
            }
            java.util.List<int[]> ranges = java.util.Collections.singletonList(new int[]{land.getMinX(), land.getMinZ(), land.getMaxX(), land.getMaxZ()});
            LandShowUtil.showLandBoundary(plugin, player, ranges, showDurationSeconds);
            player.sendMessage("§a已为你显示领地范围，持续 " + showDurationSeconds + " 秒。");
            return true;
        } else if (args.length == 1 && args[0].equalsIgnoreCase("list")) {
            player.sendMessage("§e服务器领地列表：");
            int idx = 1;
            for (ChunkLand land : landManager.getAllClaimedLands()) {
                String owner = land.getOwner();
                String ownerName = owner != null ? Bukkit.getOfflinePlayer(UUID.fromString(owner)).getName() : "未知";
                player.sendMessage("§a[已认领] §f世界:" + land.getWorldName() + " 区块范围: [" + land.getMinX() + "," + land.getMinZ() + "] ~ [" + land.getMaxX() + "," + land.getMaxZ() + "] 主人: " + ownerName);
                idx++;
            }
            for (ChunkLand land : landManager.getAllUnclaimedLands()) {
                player.sendMessage("§7[未认领] §f世界:" + land.getWorldName() + " 区块范围: [" + land.getMinX() + "," + land.getMinZ() + "] ~ [" + land.getMaxX() + "," + land.getMaxZ() + "]");
                idx++;
            }
            if (idx == 1) player.sendMessage("§c暂无任何领地。");
            return true;
        } else {
            player.sendMessage("用法: /easyland select | create | claim | unclaim | trust <玩家名> | untrust <玩家名>");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("select", "create", "claim", "unclaim", "trust", "untrust", "show", "list");
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("trust") || args[0].equalsIgnoreCase("untrust"))) {
            // 可选：返回在线玩家名补全
            return null; // 让Bukkit自动补全玩家名
        }
        return Collections.emptyList();
    }
}
