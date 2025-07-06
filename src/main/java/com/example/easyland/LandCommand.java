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
import org.bukkit.Location;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class LandCommand implements CommandExecutor, TabCompleter {
    private final LandManager landManager;
    private final LandSelectListener landSelectListener;
    private final int showDurationSeconds;
    private final JavaPlugin plugin;
    private static final String SELECT_PERMISSION = "easyland.select";
    private static final String CREATE_PERMISSION = "easyland.create";
    private static final String CLAIM_PERMISSION = "easyland.claim";
    private static final String UNCLAIM_PERMISSION = "easyland.unclaim";
    private static final String TRUST_PERMISSION = "easyland.trust";
    private static final String UNTRUST_PERMISSION = "easyland.untrust";
    private static final String REMOVE_PERMISSION = "easyland.remove";

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
        } else if ((args.length == 1 || args.length == 2) && args[0].equalsIgnoreCase("create")) {
            if (!player.hasPermission(CREATE_PERMISSION)) {
                player.sendMessage("§c你没有权限创建领地！");
                return true;
            }
            Chunk[] selects = landSelectListener.getPlayerSelects(player);
            if (selects[0] == null || selects[1] == null) {
                player.sendMessage("请先用工具右键选择两个区块。");
                return true;
            }
            String id;
            if (args.length == 2) {
                id = args[1];
                // 校验id唯一性
                boolean exists = false;
                for (ChunkLand land : landManager.getAllClaimedLands()) {
                    if (id.equalsIgnoreCase(land.getId())) { exists = true; break; }
                }
                for (ChunkLand land : landManager.getAllUnclaimedLands()) {
                    if (id.equalsIgnoreCase(land.getId())) { exists = true; break; }
                }
                if (exists) {
                    player.sendMessage("§c该ID已被占用，请更换一个唯一ID。");
                    return true;
                }
                if (id.length() > 32 || !id.matches("[a-zA-Z0-9\u4e00-\u9fa5_-]+")) {
                    player.sendMessage("§cID不合法，仅支持中英文、数字、下划线、短横线，且不超过32字符。");
                    return true;
                }
            } else {
                id = "land-" + System.currentTimeMillis();
            }
            boolean success = landManager.createLandByChunk(selects[0], selects[1], id);
            if (success) {
                player.sendMessage("无主领地已创建，ID: " + id + "，可用 /easyland claim 认领。");
            } else {
                player.sendMessage("该区域已存在领地。");
            }
        } else if ((args.length == 1 || args.length == 2) && args[0].equalsIgnoreCase("claim")) {
            if (!player.hasPermission(CLAIM_PERMISSION)) {
                player.sendMessage("§c你没有权限认领领地！");
                return true;
            }
            if (args.length == 2) {
                // 通过id认领无主领地
                String id = args[1];
                ChunkLand land = null;
                for (ChunkLand l : landManager.getAllUnclaimedLands()) {
                    if (id.equalsIgnoreCase(l.getId())) { land = l; break; }
                }
                if (land == null) {
                    player.sendMessage("§c未找到该ID的无主领地。");
                    return true;
                }
                boolean success = landManager.claimLandById(player, id);
                if (success) {
                    player.sendMessage("领地认领成功！");
                } else {
                    player.sendMessage("认领失败，该领地已被认领或操作异常。");
                }
                return true;
            } else {
                Chunk[] selects = landSelectListener.getPlayerSelects(player);
                if (selects[0] == null || selects[1] == null) {
                    player.sendMessage("请先用工具右键选择两个区块。");
                    return true;
                }
                // 需站在无主领地内
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
            }
        } else if ((args.length == 1 || args.length == 2) && args[0].equalsIgnoreCase("unclaim")) {
            if (!player.hasPermission(UNCLAIM_PERMISSION)) {
                player.sendMessage("§c你没有权限放弃领地！");
                return true;
            }
            if (args.length == 2) {
                // 通过id放弃自己已认领的领地
                String id = args[1];
                ChunkLand land = null;
                for (ChunkLand l : landManager.getAllClaimedLands()) {
                    if (id.equalsIgnoreCase(l.getId()) && player.getUniqueId().toString().equals(l.getOwner())) {
                        land = l; break;
                    }
                }
                if (land == null) {
                    player.sendMessage("§c未找到你拥有的该ID领地。");
                    return true;
                }
                boolean success = landManager.unclaimLandById(player, id);
                if (success) {
                    player.sendMessage("你已放弃领地，该区域变为无主领地。");
                } else {
                    player.sendMessage("放弃失败，操作异常。");
                }
                return true;
            } else {
                boolean success = landManager.unclaimLand(player);
                if (success) {
                    player.sendMessage("你已放弃领地，该区域变为无主领地。");
                } else {
                    player.sendMessage("你没有可放弃的领地。");
                }
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
        } else if ((args.length == 1 || args.length == 2) && args[0].equalsIgnoreCase("show")) {
            if (!player.hasPermission("easyland.show")) {
                player.sendMessage("§c你没有权限显示领地范围！");
                return true;
            }
            ChunkLand land = null;
            if (args.length == 2) {
                String id = args[1];
                for (ChunkLand l : landManager.getAllClaimedLands()) {
                    if (id.equalsIgnoreCase(l.getId())) { land = l; break; }
                }
                if (land == null) {
                    for (ChunkLand l : landManager.getAllUnclaimedLands()) {
                        if (id.equalsIgnoreCase(l.getId())) { land = l; break; }
                    }
                }
                if (land == null) {
                    player.sendMessage("§c未找到该ID的领地。");
                    return true;
                }
            } else {
                // 查找距离最近的领地（无主和已认领都可）
                Location loc = player.getLocation();
                double minDist = Double.MAX_VALUE;
                for (ChunkLand l : landManager.getAllClaimedLands()) {
                    double dist = getLandDistance(l, loc);
                    if (dist < minDist) { minDist = dist; land = l; }
                }
                for (ChunkLand l : landManager.getAllUnclaimedLands()) {
                    double dist = getLandDistance(l, loc);
                    if (dist < minDist) { minDist = dist; land = l; }
                }
                if (land == null) {
                    player.sendMessage("§c服务器暂无任何领地。");
                    return true;
                }
            }
            java.util.List<int[]> ranges = java.util.Collections.singletonList(new int[]{land.getMinX(), land.getMinZ(), land.getMaxX(), land.getMaxZ()});
            LandShowUtil.showLandBoundary(plugin, player, ranges, showDurationSeconds);
            player.sendMessage("§a已为你显示领地范围，持续 " + showDurationSeconds + " 秒。ID: " + land.getId());
            return true;
        } else if (args.length == 1 && args[0].equalsIgnoreCase("list")) {
            if (!player.hasPermission("easyland.list")) {
                player.sendMessage("§c你没有权限查看领地列表！");
                return true;
            }
            player.sendMessage("§e服务器领地列表：");
            int idx = 1;
            for (ChunkLand land : landManager.getAllClaimedLands()) {
                String owner = land.getOwner();
                String ownerName = owner != null ? Bukkit.getOfflinePlayer(UUID.fromString(owner)).getName() : "未知";
                player.sendMessage("§a[已认领] §fID:" + land.getId() + " 世界:" + land.getWorldName() + " 区块范围: [" + land.getMinX() + "," + land.getMinZ() + "] ~ [" + land.getMaxX() + "," + land.getMaxZ() + "] 主人: " + ownerName);
                idx++;
            }
            for (ChunkLand land : landManager.getAllUnclaimedLands()) {
                player.sendMessage("§7[未认领] §fID:" + land.getId() + " 世界:" + land.getWorldName() + " 区块范围: [" + land.getMinX() + "," + land.getMinZ() + "] ~ [" + land.getMaxX() + "," + land.getMaxZ() + "]");
                idx++;
            }
            if (idx == 1) player.sendMessage("§c暂无任何领地。");
            return true;
        } else if (args.length >= 1 && args[0].equalsIgnoreCase("remove")) {
            if (!player.hasPermission(REMOVE_PERMISSION)) {
                player.sendMessage("§c你没有权限删除领地！");
                return true;
            }
            boolean success = false;
            if (args.length == 2) {
                // /easyland remove <id>
                success = landManager.removeLandById(args[1]);
            } else {
                player.sendMessage("用法: /easyland remove <id>");
                return true;
            }
            if (success) {
                player.sendMessage("§c领地已被删除。");
            } else {
                player.sendMessage("§c未找到可删除的领地。");
            }
            return true;
        } else if (args.length == 1 && args[0].equalsIgnoreCase("trustlist")) {
            if (!player.hasPermission("easyland.trust")) {
                player.sendMessage("§c你没有权限查看信任列表！");
                return true;
            }
            ChunkLand land = landManager.getLand(player);
            if (land == null) {
                player.sendMessage("§c你没有已认领的领地。");
                return true;
            }
            java.util.Set<String> trusted = land.getTrusted();
            if (trusted.isEmpty()) {
                player.sendMessage("§e你的领地当前没有信任任何玩家。");
                return true;
            }
            player.sendMessage("§a你的领地信任列表：");
            for (String uuid : trusted) {
                org.bukkit.OfflinePlayer op = org.bukkit.Bukkit.getOfflinePlayer(java.util.UUID.fromString(uuid));
                String name = op.getName() != null ? op.getName() : uuid;
                player.sendMessage("§f- " + name);
            }
            return true;
        } else {
            player.sendMessage("无效指令");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> cmds = new java.util.ArrayList<>();
            if (sender.hasPermission(SELECT_PERMISSION)) cmds.add("select");
            if (sender.hasPermission(CREATE_PERMISSION)) cmds.add("create");
            if (sender.hasPermission(CLAIM_PERMISSION)) cmds.add("claim");
            if (sender.hasPermission(UNCLAIM_PERMISSION)) cmds.add("unclaim");
            if (sender.hasPermission(TRUST_PERMISSION)) cmds.add("trust");
            if (sender.hasPermission(UNTRUST_PERMISSION)) cmds.add("untrust");
            if (sender.hasPermission("easyland.show")) cmds.add("show");
            if (sender.hasPermission("easyland.list")) cmds.add("list");
            if (sender.hasPermission(REMOVE_PERMISSION)) cmds.add("remove");
            if (sender.hasPermission("easyland.trust")) cmds.add("trustlist");
            String input = args[0].toLowerCase();
            return cmds.stream().filter(cmd -> cmd.startsWith(input)).toList();
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            String input = args[1].toLowerCase();
            if (sub.equals("remove") && sender.hasPermission(REMOVE_PERMISSION)) {
                List<String> ids = new java.util.ArrayList<>();
                for (ChunkLand land : landManager.getAllClaimedLands()) {
                    if (land.getId() != null && !land.getId().isEmpty()) ids.add(land.getId());
                }
                for (ChunkLand land : landManager.getAllUnclaimedLands()) {
                    if (land.getId() != null && !land.getId().isEmpty()) ids.add(land.getId());
                }
                return ids.stream().filter(id -> id.toLowerCase().startsWith(input)).toList();
            }
            if (sub.equals("claim") && sender.hasPermission(CLAIM_PERMISSION)) {
                List<String> ids = new java.util.ArrayList<>();
                for (ChunkLand land : landManager.getAllUnclaimedLands()) {
                    if (land.getId() != null && !land.getId().isEmpty()) ids.add(land.getId());
                }
                return ids.stream().filter(id -> id.toLowerCase().startsWith(input)).toList();
            }
            if (sub.equals("unclaim") && sender.hasPermission(UNCLAIM_PERMISSION) && sender instanceof Player player) {
                List<String> ids = new java.util.ArrayList<>();
                for (ChunkLand land : landManager.getAllClaimedLands()) {
                    if (land.getId() != null && !land.getId().isEmpty() && player.getUniqueId().toString().equals(land.getOwner())) ids.add(land.getId());
                }
                return ids.stream().filter(id -> id.toLowerCase().startsWith(input)).toList();
            }
            if (sub.equals("show") && sender.hasPermission("easyland.show")) {
                List<String> ids = new java.util.ArrayList<>();
                for (ChunkLand land : landManager.getAllClaimedLands()) {
                    if (land.getId() != null && !land.getId().isEmpty()) ids.add(land.getId());
                }
                for (ChunkLand land : landManager.getAllUnclaimedLands()) {
                    if (land.getId() != null && !land.getId().isEmpty()) ids.add(land.getId());
                }
                return ids.stream().filter(id -> id.toLowerCase().startsWith(input)).toList();
            }
            if ((sub.equals("trust") && sender.hasPermission(TRUST_PERMISSION)) || (sub.equals("untrust") && sender.hasPermission(UNTRUST_PERMISSION))) {
                List<String> names = new java.util.ArrayList<>();
                for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                    names.add(p.getName());
                }
                return names.stream().filter(name -> name.toLowerCase().startsWith(input)).toList();
            }
        }
        return Collections.emptyList();
    }

    // 工具方法：
    private double getLandDistance(ChunkLand land, org.bukkit.Location loc) {
        if (land == null || loc == null) return Double.MAX_VALUE;
        if (!land.getWorldName().equals(loc.getWorld().getName())) return Double.MAX_VALUE;
        int minX = land.getMinX();
        int maxX = land.getMaxX();
        int minZ = land.getMinZ();
        int maxZ = land.getMaxZ();
        int px = loc.getBlockX() >> 4;
        int pz = loc.getBlockZ() >> 4;
        int dx = 0, dz = 0;
        if (px < minX) dx = minX - px;
        else if (px > maxX) dx = px - maxX;
        if (pz < minZ) dz = minZ - pz;
        else if (pz > maxZ) dz = pz - maxZ;
        return Math.sqrt(dx * dx + dz * dz);
    }
}
