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
    private final LanguageManager languageManager;
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
    private static final String RELOAD_PERMISSION = "easyland.reload";
    private static final String HELP_PERMISSION = "easyland.help";

    public LandCommand(JavaPlugin plugin, LandManager landManager, LandSelectListener landSelectListener,
            ConfigManager configManager, LanguageManager languageManager,
            int showDurationSeconds, int maxShowDurationSeconds) {
        this.plugin = plugin;
        this.landManager = landManager;
        this.landSelectListener = landSelectListener;
        this.configManager = configManager;
        this.languageManager = languageManager;
        this.showDurationSeconds = showDurationSeconds;
        this.maxShowDurationSeconds = maxShowDurationSeconds;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(languageManager.getMessage("general.only-players"));
            return true;
        }
        Player player = (Player) sender;
        if (args.length == 1 && args[0].equalsIgnoreCase("select")) {
            if (!player.hasPermission(SELECT_PERMISSION)) {
                languageManager.sendMessage(player, "permission.no-select");
                return true;
            }
            ItemStack wand = new ItemStack(Material.WOODEN_HOE);
            ItemMeta meta = wand.getItemMeta();
            meta.displayName(Component.text(languageManager.getMessage("select.tool-name")));
            wand.setItemMeta(meta);
            player.getInventory().addItem(wand);
            languageManager.sendMessage(player, "select.tool-received");
        } else if ((args.length == 1 || args.length == 2) && args[0].equalsIgnoreCase("create")) {
            if (!player.hasPermission(CREATE_PERMISSION)) {
                languageManager.sendMessage(player, "permission.no-create");
                return true;
            }
            Chunk[] selects = landSelectListener.getPlayerSelects(player);
            if (selects[0] == null || selects[1] == null) {
                languageManager.sendMessage(player, "select.need-selection");
                return true;
            }
            String id;
            if (args.length == 2) {
                id = args[1];
                // 校验id唯一性
                boolean exists = false;
                for (ChunkLand land : landManager.getAllClaimedLands()) {
                    if (id.equalsIgnoreCase(land.getId())) {
                        exists = true;
                        break;
                    }
                }
                for (ChunkLand land : landManager.getAllUnclaimedLands()) {
                    if (id.equalsIgnoreCase(land.getId())) {
                        exists = true;
                        break;
                    }
                }
                if (exists) {
                    languageManager.sendMessage(player, "create.id-taken");
                    return true;
                }
                if (id.length() > 32 || !id.matches("[a-zA-Z0-9\u4e00-\u9fa5_-]+")) {
                    languageManager.sendMessage(player, "create.invalid-id");
                    return true;
                }
            } else {
                id = "land-" + System.currentTimeMillis();
            }
            boolean success = landManager.createLandByChunk(selects[0], selects[1], id);
            if (success) {
                languageManager.sendMessage(player, "create.success", id);
            } else {
                languageManager.sendMessage(player, "create.area-occupied");
            }
        } else if ((args.length == 1 || args.length == 2) && args[0].equalsIgnoreCase("claim")) {
            if (!player.hasPermission(CLAIM_PERMISSION)) {
                languageManager.sendMessage(player, "permission.no-claim");
                return true;
            }
            if (args.length == 2) {
                // 通过id认领无主领地
                String id = args[1];
                ChunkLand land = null;
                for (ChunkLand l : landManager.getAllUnclaimedLands()) {
                    if (id.equalsIgnoreCase(l.getId())) {
                        land = l;
                        break;
                    }
                }
                if (land == null) {
                    languageManager.sendMessage(player, "claim.not-found");
                    return true;
                }
                boolean success = landManager.claimLandById(player, id);
                if (success) {
                    languageManager.sendMessage(player, "claim.success");
                } else {
                    languageManager.sendMessage(player, "claim.failed");
                }
                return true;
            } else {
                Chunk[] selects = landSelectListener.getPlayerSelects(player);
                if (selects[0] == null || selects[1] == null) {
                    languageManager.sendMessage(player, "select.need-selection");
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
                    languageManager.sendMessage(player, "claim.not-in-unclaimed");
                    return true;
                }
                boolean success = landManager.claimLand(player, selects[0], selects[1]);
                if (success) {
                    languageManager.sendMessage(player, "claim.success");
                } else {
                    languageManager.sendMessage(player, "claim.no-unclaimed-here");
                }
            }
        } else if ((args.length == 1 || args.length == 2) && args[0].equalsIgnoreCase("unclaim")) {
            if (!player.hasPermission(UNCLAIM_PERMISSION)) {
                languageManager.sendMessage(player, "permission.no-abandon");
                return true;
            }
            if (args.length == 2) {
                // 通过id放弃自己已认领的领地
                String id = args[1];
                ChunkLand land = null;
                for (ChunkLand l : landManager.getAllClaimedLands()) {
                    if (id.equalsIgnoreCase(l.getId()) && player.getUniqueId().toString().equals(l.getOwner())) {
                        land = l;
                        break;
                    }
                }
                if (land == null) {
                    languageManager.sendMessage(player, "abandon.not-found");
                    return true;
                }
                boolean success = landManager.unclaimLandById(player, id);
                if (success) {
                    languageManager.sendMessage(player, "abandon.success");
                } else {
                    languageManager.sendMessage(player, "abandon.failed");
                }
                return true;
            } else {
                boolean success = landManager.unclaimLand(player);
                if (success) {
                    languageManager.sendMessage(player, "abandon.success");
                } else {
                    languageManager.sendMessage(player, "abandon.no-land-to-abandon");
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("trust")) {
            if (!player.hasPermission(TRUST_PERMISSION)) {
                languageManager.sendMessage(player, "trust.no-permission");
                return true;
            }
            String targetName = args[1];
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

            // 检查是否是有效的玩家名（允许信任从未加入过服务器的玩家）
            if (targetName.length() < 3 || targetName.length() > 16 || !targetName.matches("[a-zA-Z0-9_]+")) {
                languageManager.sendMessage(player, "trust.invalid-name");
                return true;
            }

            // 防止信任自己
            if (targetName.equalsIgnoreCase(player.getName())) {
                languageManager.sendMessage(player, "trust.cannot-trust-self");
                return true;
            }

            boolean success = landManager.trustPlayer(player, target.getUniqueId().toString());
            if (success) {
                languageManager.sendMessage(player, "trust.success", targetName);
            } else {
                languageManager.sendMessage(player, "trust.failed");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("untrust")) {
            if (!player.hasPermission(UNTRUST_PERMISSION)) {
                languageManager.sendMessage(player, "trust.untrust-no-permission");
                return true;
            }
            String targetName = args[1];
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

            // 检查是否是有效的玩家名
            if (targetName.length() < 3 || targetName.length() > 16 || !targetName.matches("[a-zA-Z0-9_]+")) {
                languageManager.sendMessage(player, "trust.invalid-name");
                return true;
            }

            boolean success = landManager.untrustPlayer(player, target.getUniqueId().toString());
            if (success) {
                languageManager.sendMessage(player, "trust.untrust-success", targetName);
            } else {
                languageManager.sendMessage(player, "trust.untrust-failed");
            }
        } else if ((args.length >= 1 && args.length <= 3) && args[0].equalsIgnoreCase("show")) {
            if (!player.hasPermission("easyland.show")) {
                languageManager.sendMessage(player, "show.no-permission");
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
                        languageManager.sendMessage(player, "show.duration-too-small");
                        return true;
                    }
                    if (parsedDuration > maxShowDurationSeconds) {
                        languageManager.sendMessage(player, "show.duration-too-large", maxShowDurationSeconds);
                        return true;
                    }
                    duration = parsedDuration;
                } catch (NumberFormatException e) {
                    // 不是数字，当作领地ID处理
                    if (landId == null) {
                        landId = arg;
                    } else {
                        languageManager.sendMessage(player, "show.invalid-args");
                        return true;
                    }
                }
            }

            // 查找领地
            if (landId != null) {
                for (ChunkLand l : landManager.getAllClaimedLands()) {
                    if (landId.equalsIgnoreCase(l.getId())) {
                        land = l;
                        break;
                    }
                }
                if (land == null) {
                    for (ChunkLand l : landManager.getAllUnclaimedLands()) {
                        if (landId.equalsIgnoreCase(l.getId())) {
                            land = l;
                            break;
                        }
                    }
                }
                if (land == null) {
                    languageManager.sendMessage(player, "show.land-not-found");
                    return true;
                }
            } else {
                // 查找距离最近的领地（无主和已认领都可）
                Location loc = player.getLocation();
                double minDist = Double.MAX_VALUE;
                for (ChunkLand l : landManager.getAllClaimedLands()) {
                    double dist = getLandDistance(l, loc);
                    if (dist < minDist) {
                        minDist = dist;
                        land = l;
                    }
                }
                for (ChunkLand l : landManager.getAllUnclaimedLands()) {
                    double dist = getLandDistance(l, loc);
                    if (dist < minDist) {
                        minDist = dist;
                        land = l;
                    }
                }
                if (land == null) {
                    languageManager.sendMessage(player, "show.no-lands");
                    return true;
                }
            }

            java.util.List<int[]> ranges = java.util.Collections
                    .singletonList(new int[] { land.getMinX(), land.getMinZ(), land.getMaxX(), land.getMaxZ() });
            LandShowUtil.showLandBoundary(plugin, player, ranges, duration);
            languageManager.sendMessage(player, "show.showing", duration, land.getId());
            return true;
        } else if (args.length == 1 && args[0].equalsIgnoreCase("list")) {
            if (!player.hasPermission("easyland.list")) {
                languageManager.sendMessage(player, "permission.no-list");
                return true;
            }
            languageManager.sendMessage(player, "list.server-list");
            int idx = 1;
            for (ChunkLand land : landManager.getAllClaimedLands()) {
                String owner = land.getOwner();
                String ownerName = owner != null ? Bukkit.getOfflinePlayer(UUID.fromString(owner)).getName()
                        : languageManager.getMessage("list.unknown-owner");
                languageManager.sendMessage(player, "list.claimed-item",
                        land.getId(), land.getWorldName(),
                        land.getMinX(), land.getMinZ(),
                        land.getMaxX(), land.getMaxZ(),
                        ownerName);
                idx++;
            }
            for (ChunkLand land : landManager.getAllUnclaimedLands()) {
                languageManager.sendMessage(player, "list.unclaimed-item",
                        land.getId(), land.getWorldName(),
                        land.getMinX(), land.getMinZ(),
                        land.getMaxX(), land.getMaxZ());
                idx++;
            }
            if (idx == 1)
                languageManager.sendMessage(player, "list.no-lands");
            return true;
        } else if (args.length >= 1 && args[0].equalsIgnoreCase("remove")) {
            if (!player.hasPermission(REMOVE_PERMISSION)) {
                languageManager.sendMessage(player, "permission.no-delete");
                return true;
            }
            boolean success = false;
            if (args.length == 2) {
                // /easyland remove <id>
                success = landManager.removeLandById(args[1]);
            } else {
                languageManager.sendMessage(player, "delete.usage");
                return true;
            }
            if (success) {
                languageManager.sendMessage(player, "delete.success-msg");
            } else {
                languageManager.sendMessage(player, "delete.not-found-msg");
            }
            return true;
        } else if (args.length == 1 && args[0].equalsIgnoreCase("trustlist")) {
            if (!player.hasPermission("easyland.trust")) {
                languageManager.sendMessage(player, "trust.list-no-permission");
                return true;
            }
            ChunkLand land = landManager.getLand(player);
            if (land == null) {
                languageManager.sendMessage(player, "trust.no-land");
                return true;
            }
            java.util.Set<String> trusted = land.getTrusted();
            if (trusted.isEmpty()) {
                languageManager.sendMessage(player, "trust.list-empty");
                return true;
            }
            languageManager.sendMessage(player, "trust.list-header");
            for (String uuid : trusted) {
                org.bukkit.OfflinePlayer op = org.bukkit.Bukkit.getOfflinePlayer(java.util.UUID.fromString(uuid));
                String name = op.getName() != null ? op.getName() : uuid;
                player.sendMessage("§f- " + name);
            }
            return true;
        } else if (args.length >= 1 && args[0].equalsIgnoreCase("rule")) {
            if (!player.hasPermission(RULE_PERMISSION)) {
                languageManager.sendMessage(player, "rule.no-permission");
                return true;
            }

            ChunkLand land = landManager.getLand(player);
            if (land == null) {
                languageManager.sendMessage(player, "rule.no-land");
                return true;
            }

            if (args.length == 1) {
                // 显示当前领地的保护规则状态
                languageManager.sendMessage(player, "rule.status-header");
                String[] ruleNames = ConfigManager.getProtectionRules();

                for (String ruleName : ruleNames) {
                    String displayName = languageManager
                            .getMessage("protection.rules." + ruleName.replace("-protection", ""));
                    boolean enabled = land.getProtectionRule(ruleName);
                    boolean serverAllowed = configManager.isProtectionRuleEnabled(ruleName);

                    String serverStatus = serverAllowed ? "" : languageManager.getMessage("rule.server-disabled");
                    if (enabled) {
                        languageManager.sendMessage(player, "rule.status-enabled", displayName, serverStatus);
                    } else {
                        languageManager.sendMessage(player, "rule.status-disabled", displayName, serverStatus);
                    }
                }
                languageManager.sendMessage(player, "rule.usage-tip");
                return true;
            } else if (args.length == 3) {
                // 切换保护规则 /easyland rule <规则名> <true/false>
                String ruleName = args[1].toLowerCase();
                String action = args[2].toLowerCase();

                // 验证规则名
                boolean isValidRule = Arrays.stream(ConfigManager.getProtectionRules())
                        .anyMatch(rule -> rule.equals(ruleName));

                if (!isValidRule) {
                    languageManager.sendMessage(player, "rule.invalid-rule",
                            String.join(", ", ConfigManager.getProtectionRules()));
                    return true;
                }

                // 检查服务器是否允许此规则
                if (!configManager.isProtectionRuleEnabled(ruleName)) {
                    languageManager.sendMessage(player, "rule.server-disabled-rule");
                    return true;
                }

                boolean newState;
                if (action.equals("true") || action.equals("on") || action.equals("enable")) {
                    newState = true;
                } else if (action.equals("false") || action.equals("off") || action.equals("disable")) {
                    newState = false;
                } else {
                    languageManager.sendMessage(player, "rule.invalid-action");
                    return true;
                }

                land.setProtectionRule(ruleName, newState);
                landManager.saveLands();

                String ruleDisplayName = languageManager
                        .getMessage("protection.rules." + ruleName.replace("-protection", ""));
                String stateText = newState ? languageManager.getMessage("log.enabled")
                        : languageManager.getMessage("log.disabled");
                languageManager.sendMessage(player, "rule.set-success", ruleDisplayName, stateText);
                return true;
            } else {
                languageManager.sendMessage(player, "rule.usage");
                return true;
            }
        } else if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!(player.hasPermission(RELOAD_PERMISSION) || player.hasPermission("easyland.admin"))) {
                languageManager.sendMessage(player, "general.no-permission");
                return true;
            }
            plugin.reloadConfig();
            languageManager.reload();
            languageManager.sendMessage(player, "general.reload-success");
            return true;
        } else if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("help"))) {
            if (!player.hasPermission(HELP_PERMISSION)) {
                languageManager.sendMessage(player, "general.no-permission");
                return true;
            }
            languageManager.sendMessage(player, "help.header");
            if (player.hasPermission(SELECT_PERMISSION)) languageManager.sendMessage(player, "help.select");
            if (player.hasPermission(CREATE_PERMISSION)) languageManager.sendMessage(player, "help.create");
            if (player.hasPermission(CLAIM_PERMISSION)) languageManager.sendMessage(player, "help.claim");
            if (player.hasPermission(UNCLAIM_PERMISSION)) languageManager.sendMessage(player, "help.abandon");
            if (player.hasPermission(REMOVE_PERMISSION)) languageManager.sendMessage(player, "help.delete");
            // info 未单独权限节点，默认展示
            languageManager.sendMessage(player, "help.info");
            if (player.hasPermission("easyland.list")) languageManager.sendMessage(player, "help.list");
            if (player.hasPermission("easyland.show")) languageManager.sendMessage(player, "help.show");
            if (player.hasPermission(RULE_PERMISSION)) languageManager.sendMessage(player, "help.protection");
            if (player.hasPermission(RELOAD_PERMISSION) || player.hasPermission("easyland.admin")) languageManager.sendMessage(player, "help.reload");
            return true;
        } else {
            languageManager.sendMessage(player, "general.invalid-args", "/easyland help");
        }
        return true;
    }

    /**
     * 获取规则的显示名称
     */

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> cmds = new java.util.ArrayList<>();
            if (sender.hasPermission(SELECT_PERMISSION))
                cmds.add("select");
            if (sender.hasPermission(CREATE_PERMISSION))
                cmds.add("create");
            if (sender.hasPermission(CLAIM_PERMISSION))
                cmds.add("claim");
            if (sender.hasPermission(UNCLAIM_PERMISSION))
                cmds.add("unclaim");
            if (sender.hasPermission(TRUST_PERMISSION))
                cmds.add("trust");
            if (sender.hasPermission(UNTRUST_PERMISSION))
                cmds.add("untrust");
            if (sender.hasPermission("easyland.show"))
                cmds.add("show");
            if (sender.hasPermission("easyland.list"))
                cmds.add("list");
            if (sender.hasPermission(REMOVE_PERMISSION))
                cmds.add("remove");
            if (sender.hasPermission("easyland.trust"))
                cmds.add("trustlist");
            if (sender.hasPermission(RULE_PERMISSION))
                cmds.add("rule");
            if (sender.hasPermission(HELP_PERMISSION)) cmds.add("help");
            if (sender.hasPermission(RELOAD_PERMISSION) || sender.hasPermission("easyland.admin"))
                cmds.add("reload");
            String input = args[0].toLowerCase();
            return cmds.stream().filter(cmd -> cmd.startsWith(input)).toList();
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            String input = args[1].toLowerCase();
            if (sub.equals("remove") && sender.hasPermission(REMOVE_PERMISSION)) {
                List<String> ids = new java.util.ArrayList<>();
                for (ChunkLand land : landManager.getAllClaimedLands()) {
                    if (land.getId() != null && !land.getId().isEmpty())
                        ids.add(land.getId());
                }
                for (ChunkLand land : landManager.getAllUnclaimedLands()) {
                    if (land.getId() != null && !land.getId().isEmpty())
                        ids.add(land.getId());
                }
                return ids.stream().filter(id -> id.toLowerCase().startsWith(input)).toList();
            }
            if (sub.equals("claim") && sender.hasPermission(CLAIM_PERMISSION)) {
                List<String> ids = new java.util.ArrayList<>();
                for (ChunkLand land : landManager.getAllUnclaimedLands()) {
                    if (land.getId() != null && !land.getId().isEmpty())
                        ids.add(land.getId());
                }
                return ids.stream().filter(id -> id.toLowerCase().startsWith(input)).toList();
            }
            if (sub.equals("unclaim") && sender.hasPermission(UNCLAIM_PERMISSION) && sender instanceof Player player) {
                List<String> ids = new java.util.ArrayList<>();
                for (ChunkLand land : landManager.getAllClaimedLands()) {
                    if (land.getId() != null && !land.getId().isEmpty()
                            && player.getUniqueId().toString().equals(land.getOwner()))
                        ids.add(land.getId());
                }
                return ids.stream().filter(id -> id.toLowerCase().startsWith(input)).toList();
            }
            if (sub.equals("show") && sender.hasPermission("easyland.show")) {
                List<String> ids = new java.util.ArrayList<>();
                for (ChunkLand land : landManager.getAllClaimedLands()) {
                    if (land.getId() != null && !land.getId().isEmpty())
                        ids.add(land.getId());
                }
                for (ChunkLand land : landManager.getAllUnclaimedLands()) {
                    if (land.getId() != null && !land.getId().isEmpty())
                        ids.add(land.getId());
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
                            org.bukkit.OfflinePlayer op = org.bukkit.Bukkit
                                    .getOfflinePlayer(java.util.UUID.fromString(uuid));
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
        if (land == null || loc == null)
            return Double.MAX_VALUE;
        if (!land.getWorldName().equals(loc.getWorld().getName()))
            return Double.MAX_VALUE;
        int minX = land.getMinX();
        int maxX = land.getMaxX();
        int minZ = land.getMinZ();
        int maxZ = land.getMaxZ();
        int px = loc.getBlockX() >> 4;
        int pz = loc.getBlockZ() >> 4;
        int dx = 0, dz = 0;
        if (px < minX)
            dx = minX - px;
        else if (px > maxX)
            dx = px - maxX;
        if (pz < minZ)
            dz = minZ - pz;
        else if (pz > maxZ)
            dz = pz - maxZ;
        return Math.sqrt(dx * dx + dz * dz);
    }
}
