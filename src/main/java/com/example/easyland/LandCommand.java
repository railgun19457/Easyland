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
import java.util.Arrays;

public class LandCommand implements CommandExecutor, TabCompleter {
    private final LandManager landManager;
    private final LandSelectListener landSelectListener;
    private final ConfigManager configManager;
    private final int showDurationSeconds;
    private final int maxShowDurationSeconds;
    private final JavaPlugin plugin;
    private static final String SELECT_PERMISSION = "easyland.select";
    private static final String CREATE_PERMISSION = "easyland.create";
    private static final String CLAIM_PERMISSION = "easyland.claim";
    private static final String UNCLAIM_PERMISSION = "easyland.unclaim";
    private static final String TRUST_PERMISSION = "easyland.trust";
    private static final String UNTRUST_PERMISSION = "easyland.untrust";
    private static final String REMOVE_PERMISSION = "easyland.remove";
    private static final String RULE_PERMISSION = "easyland.rule";

    public LandCommand(JavaPlugin plugin, LandManager landManager, LandSelectListener landSelectListener, 
                      ConfigManager configManager, int showDurationSeconds, int maxShowDurationSeconds) {
        this.plugin = plugin;
        this.landManager = landManager;
        this.landSelectListener = landSelectListener;
        this.configManager = configManager;
        this.showDurationSeconds = showDurationSeconds;
        this.maxShowDurationSeconds = maxShowDurationSeconds;
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
            String targetName = args[1];
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            
            // 检查是否是有效的玩家名（允许信任从未加入过服务器的玩家）
            if (targetName.length() < 3 || targetName.length() > 16 || !targetName.matches("[a-zA-Z0-9_]+")) {
                player.sendMessage("§c玩家名格式不正确！");
                return true;
            }
            
            // 防止信任自己
            if (targetName.equalsIgnoreCase(player.getName())) {
                player.sendMessage("§c你不能信任自己！");
                return true;
            }
            
            boolean success = landManager.trustPlayer(player, target.getUniqueId().toString());
            if (success) {
                player.sendMessage("已信任玩家 " + targetName + "，其可在你的领地内交互。");
            } else {
                player.sendMessage("信任失败，你没有领地或已信任该玩家。");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("untrust")) {
            if (!player.hasPermission(UNTRUST_PERMISSION)) {
                player.sendMessage("§c你没有权限取消信任！");
                return true;
            }
            String targetName = args[1];
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            
            // 检查是否是有效的玩家名
            if (targetName.length() < 3 || targetName.length() > 16 || !targetName.matches("[a-zA-Z0-9_]+")) {
                player.sendMessage("§c玩家名格式不正确！");
                return true;
            }
            
            boolean success = landManager.untrustPlayer(player, target.getUniqueId().toString());
            if (success) {
                player.sendMessage("已取消对玩家 " + targetName + " 的信任。");
            } else {
                player.sendMessage("取消信任失败，你没有领地或未信任该玩家。");
            }
        } else if ((args.length >= 1 && args.length <= 3) && args[0].equalsIgnoreCase("show")) {
            if (!player.hasPermission("easyland.show")) {
                player.sendMessage("§c你没有权限显示领地范围！");
                return true;
            }
            
            ChunkLand land = null;
            int duration = showDurationSeconds; // 默认显示时间
            
            // 解析参数
            String landId = null;
            for (int i = 1; i < args.length; i++) {
                String arg = args[i];
                try {
                    // 尝试解析为数字（时间）
                    int parsedDuration = Integer.parseInt(arg);
                    if (parsedDuration <= 0) {
                        player.sendMessage("§c显示时间必须大于0秒！");
                        return true;
                    }
                    if (parsedDuration > maxShowDurationSeconds) {
                        player.sendMessage("§c显示时间不能超过" + maxShowDurationSeconds + "秒！");
                        return true;
                    }
                    duration = parsedDuration;
                } catch (NumberFormatException e) {
                    // 不是数字，当作领地ID处理
                    if (landId == null) {
                        landId = arg;
                    } else {
                        player.sendMessage("§c参数错误！用法: /easyland show [领地ID] [时间(秒)]");
                        return true;
                    }
                }
            }
            
            // 查找领地
            if (landId != null) {
                for (ChunkLand l : landManager.getAllClaimedLands()) {
                    if (landId.equalsIgnoreCase(l.getId())) { land = l; break; }
                }
                if (land == null) {
                    for (ChunkLand l : landManager.getAllUnclaimedLands()) {
                        if (landId.equalsIgnoreCase(l.getId())) { land = l; break; }
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
            LandShowUtil.showLandBoundary(plugin, player, ranges, duration);
            player.sendMessage("§a已为你显示领地范围，持续 " + duration + " 秒。ID: " + land.getId());
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
        } else if (args.length >= 1 && args[0].equalsIgnoreCase("rule")) {
            if (!player.hasPermission(RULE_PERMISSION)) {
                player.sendMessage("§c你没有权限管理领地保护规则！");
                return true;
            }
            
            ChunkLand land = landManager.getLand(player);
            if (land == null) {
                player.sendMessage("§c你没有已认领的领地。");
                return true;
            }
            
            if (args.length == 1) {
                // 显示当前领地的保护规则状态
                player.sendMessage("§a你的领地保护规则状态：");
                String[] ruleNames = ConfigManager.getProtectionRules();
                String[] ruleDisplayNames = {"方块保护", "爆炸保护", "容器保护", "玩家保护"};
                
                for (int i = 0; i < ruleNames.length; i++) {
                    String ruleName = ruleNames[i];
                    String displayName = ruleDisplayNames[i];
                    boolean enabled = land.getProtectionRule(ruleName);
                    boolean serverAllowed = configManager.isProtectionRuleEnabled(ruleName);
                    
                    String status = enabled ? "§a启用" : "§c禁用";
                    String serverStatus = serverAllowed ? "" : " §7(服务器已禁用)";
                    player.sendMessage("§f- " + displayName + ": " + status + serverStatus);
                }
                player.sendMessage("§e使用 §f/easyland rule <规则名> <on/off> §e来切换保护规则");
                return true;
            } else if (args.length == 3) {
                // 切换保护规则 /easyland rule <规则名> <true/false>
                String ruleName = args[1].toLowerCase();
                String action = args[2].toLowerCase();
                
                // 验证规则名
                boolean isValidRule = Arrays.stream(ConfigManager.getProtectionRules())
                    .anyMatch(rule -> rule.equals(ruleName));
                
                if (!isValidRule) {
                    player.sendMessage("§c无效的保护规则！有效规则: " + String.join(", ", ConfigManager.getProtectionRules()));
                    return true;
                }
                
                // 检查服务器是否允许此规则
                if (!configManager.isProtectionRuleEnabled(ruleName)) {
                    player.sendMessage("§c服务器已禁用此保护规则，无法修改！");
                    return true;
                }
                
                boolean newState;
                if (action.equals("true") || action.equals("on") || action.equals("enable")) {
                    newState = true;
                } else if (action.equals("false") || action.equals("off") || action.equals("disable")) {
                    newState = false;
                } else {
                    player.sendMessage("§c无效的操作！请使用: true/false, on/off, enable/disable");
                    return true;
                }
                
                land.setProtectionRule(ruleName, newState);
                landManager.saveLands();
                
                String ruleDisplayName = getRuleDisplayName(ruleName);
                String stateText = newState ? "§a启用" : "§c禁用";
                player.sendMessage("§a已将 " + ruleDisplayName + " 设置为: " + stateText);
                return true;
            } else {
                player.sendMessage("用法: /easyland rule 或 /easyland rule <规则名> <on/off>");
                return true;
            }
        } else {
            player.sendMessage("无效指令");
        }
        return true;
    }
    
    /**
     * 获取规则的显示名称
     */
    private String getRuleDisplayName(String ruleName) {
        switch (ruleName) {
            case "block-protection": return "方块保护";
            case "explosion-protection": return "爆炸保护";
            case "container-protection": return "容器保护";
            case "player-protection": return "玩家保护";
            default: return ruleName;
        }
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
            if (sender.hasPermission(RULE_PERMISSION)) cmds.add("rule");
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
            if (sub.equals("trust") && sender.hasPermission(TRUST_PERMISSION)) {
                List<String> names = new java.util.ArrayList<>();
                for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                    names.add(p.getName());
                }
                return names.stream().filter(name -> name.toLowerCase().startsWith(input)).toList();
            }
            if (sub.equals("untrust") && sender.hasPermission(UNTRUST_PERMISSION) && sender instanceof Player player) {
                List<String> trustedNames = new java.util.ArrayList<>();
                ChunkLand land = landManager.getLand(player);
                if (land != null) {
                    java.util.Set<String> trusted = land.getTrusted();
                    for (String uuid : trusted) {
                        try {
                            org.bukkit.OfflinePlayer op = org.bukkit.Bukkit.getOfflinePlayer(java.util.UUID.fromString(uuid));
                            String name = op.getName();
                            if (name != null) {
                                trustedNames.add(name);
                            }
                        } catch (IllegalArgumentException e) {
                            // 忽略无效的UUID
                        }
                    }
                }
                return trustedNames.stream().filter(name -> name.toLowerCase().startsWith(input)).toList();
            }
            if (sub.equals("rule") && sender.hasPermission(RULE_PERMISSION)) {
                List<String> options = Arrays.asList(ConfigManager.getProtectionRules());
                return options.stream().filter(option -> option.toLowerCase().startsWith(input)).toList();
            }
        }
        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            String ruleName = args[1].toLowerCase();
            String input = args[2].toLowerCase();
            
            if (sub.equals("rule") && sender.hasPermission(RULE_PERMISSION)) {
                if (Arrays.asList(ConfigManager.getProtectionRules()).contains(ruleName)) {
                    List<String> states = Arrays.asList("on", "off");
                    return states.stream().filter(state -> state.toLowerCase().startsWith(input)).toList();
                }
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
