package io.github.railgun19457.easyland.command;

import io.github.railgun19457.easyland.EasyLand;
import io.github.railgun19457.easyland.I18nManager;
import io.github.railgun19457.easyland.core.LandManager;
import io.github.railgun19457.easyland.exception.MigrationFileNotFoundException;
import io.github.railgun19457.easyland.exception.SubClaimException;
import io.github.railgun19457.easyland.migration.MigrationManager;
import io.github.railgun19457.easyland.model.Land;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * EasyLand插件的主命令处理器。
 * 处理所有/easyland和/el子命令。
 */
public class MainCommand implements CommandExecutor, TabCompleter {
    
    private final EasyLand plugin;
    private final I18nManager i18nManager;
    private final LandManager landManager;
    private final io.github.railgun19457.easyland.core.PermissionManager permissionManager;
    private final Logger logger;
    private final MigrationManager migrationManager;
    
    // 子命令列表
    private static final List<String> SUBCOMMANDS = Arrays.asList(
        "claim", "delete", "list", "trust", "untrust", "info", "help", "create", "abandon", "show", "reload", "rename", "subcreate", "migrate", "select", "setspawn", "tp", "rule", "trustlist"
    );
    
    // 管理员子命令列表
    private static final List<String> ADMIN_SUBCOMMANDS = Arrays.asList(
        "create", "delete", "reload", "migrate"
    );
    
    // 保护规则列表
    private static final List<String> PROTECTION_RULES = java.util.Arrays.stream(io.github.railgun19457.easyland.model.LandFlag.values())
        .map(io.github.railgun19457.easyland.model.LandFlag::getName)
        .collect(java.util.stream.Collectors.toList());
    
    // 命令到权限的映射表（不包含 help 命令，因为它不需要权限）
    private static final Map<String, String> COMMAND_PERMISSIONS = Map.ofEntries(
        Map.entry("claim", "easyland.claim"),
        Map.entry("delete", "easyland.delete"),
        Map.entry("list", "easyland.list"),
        Map.entry("trust", "easyland.trust"),
        Map.entry("trustlist", "easyland.trust"),
        Map.entry("untrust", "easyland.trust"),
        Map.entry("info", "easyland.info"),
        Map.entry("create", "easyland.create"),
        Map.entry("abandon", "easyland.abandon"),
        Map.entry("show", "easyland.show"),
        Map.entry("reload", "easyland.admin"),
        Map.entry("rename", "easyland.rename"),
        Map.entry("subcreate", "easyland.subcreate"),
        Map.entry("select", "easyland.select"),
        Map.entry("migrate", "easyland.admin.migrate"),
        Map.entry("setspawn", "easyland.setspawn"),
        Map.entry("tp", "easyland.tp"),
        Map.entry("rule", "easyland.rule")
    );
    
    // 帮助消息映射表：权限 -> 帮助消息键列表
    private static final Map<String, List<String>> HELP_MESSAGES = Map.ofEntries(
        Map.entry("easyland.select", List.of("help.select")),
        Map.entry("easyland.create", List.of("help.create")),
        Map.entry("easyland.claim", List.of("help.claim")),
        Map.entry("easyland.abandon", List.of("help.abandon")),
        Map.entry("easyland.delete", List.of("help.delete")),
        Map.entry("easyland.info", List.of("help.info")),
        Map.entry("easyland.list", List.of("help.list")),
        Map.entry("easyland.show", List.of("help.show")),
        Map.entry("easyland.trust", List.of("help.trust", "help.untrust", "help.trustlist")),
        Map.entry("easyland.subcreate", List.of("help.subcreate")),
        Map.entry("easyland.admin", List.of("help.reload")),
        Map.entry("easyland.admin.migrate", List.of("help.migrate")),
        Map.entry("easyland.setspawn", List.of("help.setspawn")),
        Map.entry("easyland.tp", List.of("help.tp")),
        Map.entry("easyland.rule", List.of("help.rule"))
    );
    
    /**
     * 验证领地名称是否合法。
     * 只允许字母、数字、下划线、中划线和中文字符。
     *
     * @param name 领地名称
     * @return 是否合法
     */
    private boolean isValidLandName(String name) {
        return name.matches("^[a-zA-Z0-9_\\-\u4e00-\u9fa5]+$");
    }
    
    /**
     * 构造函数。
     *
     * @param plugin 插件主类实例
     */
    public MainCommand(EasyLand plugin) {
        this.plugin = plugin;
        this.i18nManager = plugin.getI18nManager();
        this.landManager = plugin.getLandManager();
        this.permissionManager = plugin.getPermissionManager();
        this.logger = plugin.getLogger();
        this.migrationManager = new MigrationManager(plugin);
    }
    
    /**
     * 检查玩家权限的辅助方法。
     *
     * @param player       玩家
     * @param permission   权限节点
     * @param errorMessage 错误消息的国际化键
     * @return 如果有权限返回true，否则发送错误消息并返回false
     */
    private boolean checkPermission(Player player, String permission, String errorMessage) {
        if (!permissionManager.hasPermission(player, permission)) {
            player.sendMessage(i18nManager.getMessage(errorMessage));
            return false;
        }
        return true;
    }
    
    /**
     * 验证命令参数的辅助方法。
     *
     * @param player      玩家
     * @param args        命令参数
     * @param minArgs     最小参数数量
     * @param usageKey    用法消息的国际化键
     * @param commandName 命令名称
     * @return 如果参数有效返回true，否则发送用法消息并返回false
     */
    private boolean validateArgs(Player player, String[] args, int minArgs, String usageKey, String commandName) {
        if (args.length < minArgs) {
            player.sendMessage(i18nManager.getMessage("general.invalid-args", "/" + commandName + " " + usageKey));
            return false;
        }
        return true;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 获取实际使用的命令名称（可能是 easyland 或 el）
        String commandName = label != null ? label : command.getName();
        
        // 如果没有参数，显示帮助
        if (args.length == 0) {
            showHelp(sender, commandName);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        // 检查是否为控制台可执行的管理命令
        // 无论使用 easyland 还是 el 命令，都应该允许这些子命令从控制台执行
        boolean isConsoleCommand = !(sender instanceof Player) &&
            (subCommand.equals("reload") || subCommand.equals("migrate") || subCommand.equals("admin"));
        
        // 如果不是玩家且不是控制台可执行命令，返回错误
        if (!(sender instanceof Player) && !isConsoleCommand) {
            sender.sendMessage(i18nManager.getMessage("general.only-players"));
            return true;
        }
        
        // 对于需要玩家上下文的命令，检查发送者是否为玩家
        boolean requiresPlayerContext = !subCommand.equals("reload") &&
            !subCommand.equals("migrate") && !subCommand.equals("admin") &&
            !subCommand.equals("help");
        
        if (requiresPlayerContext && !(sender instanceof Player)) {
            sender.sendMessage(i18nManager.getMessage("general.player-only-command"));
            return true;
        }
        
        Player player = (sender instanceof Player) ? (Player) sender : null;
        
        // 处理不同的子命令
        switch (subCommand) {
            case "claim":
                handleClaim(player, args, commandName);
                break;
                
            case "delete":
                handleDelete(player, args, commandName);
                break;
                
            case "list":
                handleList(player, args, commandName);
                break;
                
            case "trust":
                handleTrust(player, args, commandName);
                break;
                
            case "untrust":
                handleUntrust(player, args, commandName);
                break;

            case "trustlist":
                handleTrustList(player, args, commandName);
                break;
                
            case "info":
                handleInfo(player, args, commandName);
                break;

            case "setspawn":
                handleSetSpawn(player, args, commandName);
                break;

            case "tp":
                handleTeleport(player, args, commandName);
                break;
                
            case "help":
                showHelp(sender, commandName);
                break;
                
            case "create":
                handleCreate(player, args, commandName);
                break;
                
            case "abandon":
                handleAbandon(player, args, commandName);
                break;
                
            case "show":
                handleShow(player, args, commandName);
                break;
                
            case "rule":
                handleRule(player, args, commandName);
                break;
                
            case "reload":
                handleReload(sender);
                break;
                
            case "rename":
                handleRename(player, args, commandName);
                break;
                
            case "subcreate":
                handleSubCreate(player, args, commandName);
                break;
                
            case "migrate":
                handleMigrate(sender, args);
                break;
                
            case "select":
                handleSelect(player, args, commandName);
                break;
                
            default:
                String helpCommand = "/" + commandName + " help";
                if (player != null) {
                    player.sendMessage(i18nManager.getMessage("general.invalid-args", helpCommand));
                } else {
                    sender.sendMessage(i18nManager.getMessage("general.invalid-args", helpCommand));
                }
                break;
        }
        
        return true;
    }
    

    
    /**
     * 处理claim命令。
     */
    private void handleClaim(Player player, String[] args, String commandName) {
        if (!checkPermission(player, "easyland.claim", "permission.no-claim")) {
            return;
        }
        
        if (!validateArgs(player, args, 2, "claim <landId>", commandName)) {
            return;
        }
        
        String landId = args[1];
        boolean success = landManager.claimLand(player, landId);
        
        if (success) {
            player.sendMessage(i18nManager.getMessage("claim.success"));
        } else {
            player.sendMessage(i18nManager.getMessage("claim.failed"));
        }
    }
    
    /**
     * 处理delete命令。
     */
    private void handleDelete(Player player, String[] args, String commandName) {
        if (!checkPermission(player, "easyland.delete", "permission.no-delete")) {
            return;
        }
        
        if (!validateArgs(player, args, 2, "delete <landId>", commandName)) {
            return;
        }
        
        String landId = args[1];
        boolean success = landManager.deleteLand(player, landId);
        
        if (success) {
            player.sendMessage(i18nManager.getMessage("delete.success"));
        } else {
            player.sendMessage(i18nManager.getMessage("delete.not-found"));
        }
    }
    
    /**
     * 处理list命令。
     */
    private void handleList(Player player, String[] args, String commandName) {
        if (!checkPermission(player, "easyland.list", "permission.no-list")) {
            return;
        }
        
        int page = 1;
        if (args.length >= 2) {
            try {
                page = Integer.parseInt(args[1]);
                if (page < 1) page = 1;
            } catch (NumberFormatException e) {
                player.sendMessage(i18nManager.getMessage("general.invalid-args", "/" + commandName + " list [page]"));
                return;
            }
        }
        
        List<Land> lands;
        String listOwner;
        int totalLands = 0;
        int perPage = plugin.getConfigManager().getListPerPage();
        
        // 管理员可以查看所有领地
        if (permissionManager.isAdmin(player)) {
            // 获取所有领地用于计算总数
            List<Land> allLands = landManager.getAllLands();
            totalLands = allLands.size();
            
            // 应用分页
            int startIndex = (page - 1) * perPage;
            int endIndex = Math.min(startIndex + perPage, totalLands);
            
            if (startIndex >= totalLands) {
                lands = List.of();
            } else {
                lands = allLands.subList(startIndex, endIndex);
            }
            
            listOwner = i18nManager.getMessage("list.all-lands");
        } else {
            // 先获取第一页来判断是否有数据
            lands = landManager.listPlayerLands(player.getUniqueId(), page);
            // 由于无法直接获取总数，我们使用一个简化的显示方式
            // 如果有结果，就显示当前页，如果没有结果就提示为空
            listOwner = player.getName();
        }
        
        if (lands.isEmpty()) {
            player.sendMessage(i18nManager.getMessage("list.empty"));
            return;
        }
        
        // 显示头部，包含页码信息
        player.sendMessage(i18nManager.getMessage("list.header", listOwner));
        
        if (permissionManager.isAdmin(player) && totalLands > 0) {
            // 管理员显示详细分页信息
            int totalPages = (int) Math.ceil((double) totalLands / perPage);
            player.sendMessage(i18nManager.getMessage("list.page-info", String.valueOf(page), String.valueOf(totalPages), String.valueOf(totalLands)));
        } else {
            // 普通玩家显示简单的页码
            player.sendMessage(i18nManager.getMessage("list.page-simple", String.valueOf(page)));
        }
        
        player.sendMessage(i18nManager.getMessage("list.separator"));
        
        for (Land land : lands) {
            // 获取领地主人名称
            String ownerName = getOwnerName(land.getOwnerId());
            
            // 格式: 领地ID | 主人名称 | 坐标
            String displayName = land.getName() != null ? land.getName() : "ID:" + land.getId();
            
            // 计算坐标显示
            String coords;
            if (land.getTeleportX() != null) {
                coords = String.format("(%.0f, %.0f, %.0f)", 
                    land.getTeleportX(), land.getTeleportY(), land.getTeleportZ());
            } else {
                // 如果未设置传送点，显示领地中心（与 /el tp 逻辑一致）
                int centerX = (land.getX1() + land.getX2()) / 2;
                int centerZ = (land.getZ1() + land.getZ2()) / 2;
                coords = String.format("(%d, ~, %d)", centerX, centerZ);
            }
            
            // 构建可点击的消息
            // 对应 list.item: "§e#%s §f%s §7| §b主人: §a%s §7| §d%s"
            net.kyori.adventure.text.Component message = net.kyori.adventure.text.Component.text()
                .append(net.kyori.adventure.text.Component.text("#" + land.getId(), net.kyori.adventure.text.format.NamedTextColor.YELLOW))
                .append(net.kyori.adventure.text.Component.space())
                .append(net.kyori.adventure.text.Component.text(displayName, net.kyori.adventure.text.format.NamedTextColor.WHITE))
                .append(net.kyori.adventure.text.Component.text(" | ", net.kyori.adventure.text.format.NamedTextColor.GRAY))
                .append(net.kyori.adventure.text.Component.text("主人: ", net.kyori.adventure.text.format.NamedTextColor.AQUA))
                .append(net.kyori.adventure.text.Component.text(ownerName, net.kyori.adventure.text.format.NamedTextColor.GREEN))
                .append(net.kyori.adventure.text.Component.text(" | ", net.kyori.adventure.text.format.NamedTextColor.GRAY))
                .append(net.kyori.adventure.text.Component.text(coords, net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE)
                    .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(net.kyori.adventure.text.Component.text("点击传送")))
                    .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/" + commandName + " tp " + (land.getName() != null ? land.getName() : land.getId()))))
                .build();
            
            player.sendMessage(message);
        }
        
        // 显示下一页提示（仅对管理员且有足够领地时）
        if (permissionManager.isAdmin(player) && totalLands > 0) {
            int totalPages = (int) Math.ceil((double) totalLands / perPage);
            if (page < totalPages) {
                player.sendMessage(i18nManager.getMessage("list.next-page", "/" + commandName + " list " + (page + 1)));
            }
        } else if (lands.size() >= perPage) {
            // 普通玩家，如果当前页满了，提示可能还有下一页
            player.sendMessage(i18nManager.getMessage("list.next-page", "/" + commandName + " list " + (page + 1)));
        }
    }
    
    /**
     * 处理trust命令。
     */
    private void handleTrust(Player player, String[] args, String commandName) {
        if (!checkPermission(player, "easyland.trust", "trust.no-permission")) {
            return;
        }
        
        if (!validateArgs(player, args, 3, "trust <landId> <player>", commandName)) {
            return;
        }
        
        String landId = args[1];
        String targetPlayerName = args[2];
        
        // 检查是否试图信任自己
        if (targetPlayerName.equalsIgnoreCase(player.getName())) {
            player.sendMessage(i18nManager.getMessage("trust.cannot-trust-self"));
            return;
        }
        
        // 尝试获取在线玩家，如果不在线则获取离线玩家
        org.bukkit.OfflinePlayer targetPlayer = plugin.getServer().getPlayer(targetPlayerName);
        if (targetPlayer == null) {
            // 尝试获取离线玩家
            targetPlayer = plugin.getServer().getOfflinePlayer(targetPlayerName);
            // 检查该玩家是否曾经在服务器上玩过
            if (!targetPlayer.hasPlayedBefore() && !targetPlayer.isOnline()) {
                player.sendMessage(i18nManager.getMessage("trust.invalid-name"));
                return;
            }
        }
        
        boolean success = landManager.trustPlayer(player, landId, targetPlayer);
        
        if (success) {
            player.sendMessage(i18nManager.getMessage("trust.success", targetPlayer.getName()));
        } else {
            player.sendMessage(i18nManager.getMessage("trust.failed"));
        }
    }
    
    /**
     * 处理untrust命令。
     */
    private void handleUntrust(Player player, String[] args, String commandName) {
        if (!checkPermission(player, "easyland.trust", "trust.untrust-no-permission")) {
            return;
        }
        
        if (!validateArgs(player, args, 3, "untrust <landId> <player>", commandName)) {
            return;
        }
        
        String landId = args[1];
        String targetPlayerName = args[2];
        
        // 尝试获取在线玩家，如果不在线则获取离线玩家
        org.bukkit.OfflinePlayer targetPlayer = plugin.getServer().getPlayer(targetPlayerName);
        if (targetPlayer == null) {
            // 尝试获取离线玩家
            targetPlayer = plugin.getServer().getOfflinePlayer(targetPlayerName);
            // 检查该玩家是否曾经在服务器上玩过
            if (!targetPlayer.hasPlayedBefore() && !targetPlayer.isOnline()) {
                player.sendMessage(i18nManager.getMessage("trust.invalid-name"));
                return;
            }
        }
        
        boolean success = landManager.untrustPlayer(player, landId, targetPlayer);
        
        if (success) {
            player.sendMessage(i18nManager.getMessage("trust.untrust-success", targetPlayer.getName()));
        } else {
            player.sendMessage(i18nManager.getMessage("trust.untrust-failed"));
        }
    }

    /**
     * 处理trustlist命令。
     */
    private void handleTrustList(Player player, String[] args, String commandName) {
        if (!checkPermission(player, "easyland.trust", "permission.no-trust")) {
            return;
        }
        
        if (!validateArgs(player, args, 2, "trustlist <landId>", commandName)) {
            return;
        }
        
        String landId = args[1];
        Optional<io.github.railgun19457.easyland.model.Land> landOpt = landManager.getLandByIdOrName(landId);
        
        if (landOpt.isEmpty()) {
            player.sendMessage(i18nManager.getMessage("delete.not-found"));
            return;
        }
        
        io.github.railgun19457.easyland.model.Land land = landOpt.get();
        
        List<io.github.railgun19457.easyland.model.Player> trustedPlayers = land.getTrustedPlayers();
        if (trustedPlayers == null || trustedPlayers.isEmpty()) {
            player.sendMessage(i18nManager.getMessage("trust.list.empty", land.getName()));
            return;
        }
        
        String playerNames = trustedPlayers.stream()
            .map(io.github.railgun19457.easyland.model.Player::getName)
            .collect(java.util.stream.Collectors.joining(", "));
            
        player.sendMessage(i18nManager.getMessage("trust.list.header", land.getName()));
        player.sendMessage(i18nManager.getMessage("trust.list.players", playerNames));
    }
    
    /**
     * 处理info命令。
     */
    private void handleInfo(Player player, String[] args, String commandName) {
        if (!checkPermission(player, "easyland.info", "permission.no-info")) {
            return;
        }
        
        Land land;
        
        if (args.length >= 2) {
            // 查看指定领地信息
            String landIdOrName = args[1];
            Optional<Land> landOpt = landManager.getLandByIdOrName(landIdOrName);
            if (landOpt.isPresent()) {
                land = landOpt.get();
            } else {
                player.sendMessage(i18nManager.getMessage("info.no-land-here"));
                return;
            }
        } else {
            // 查看当前位置的领地信息
            land = landManager.getLandAt(player.getLocation());
            if (land == null) {
                player.sendMessage(i18nManager.getMessage("info.no-land-here"));
                return;
            }
        }
        
        // 显示领地信息
        player.sendMessage(i18nManager.getMessage("info.header"));
        player.sendMessage(i18nManager.getMessage("info.id", String.valueOf(land.getId())));
        if (land.getName() != null) {
            player.sendMessage(i18nManager.getMessage("info.name", land.getName()));
        }
        
        // 获取拥有者信息
        if (land.getOwnerId() == 0) {
            player.sendMessage(i18nManager.getMessage("info.unclaimed"));
        } else {
            String ownerName = "Unknown";
            try {
                Optional<io.github.railgun19457.easyland.model.Player> ownerOpt = plugin.getPlayerDAO().getPlayerById(land.getOwnerId());
                if (ownerOpt.isPresent()) {
                    io.github.railgun19457.easyland.model.Player owner = ownerOpt.get();
                    ownerName = owner.getName();
                    
                    // 尝试修复 Unknown 的名字
                    if ("Unknown".equals(ownerName) || ownerName == null) {
                        java.util.UUID ownerUuid = owner.getUuid();
                        org.bukkit.OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(ownerUuid);
                        if (offlinePlayer != null && offlinePlayer.getName() != null) {
                            ownerName = offlinePlayer.getName();
                            // 更新数据库
                            owner.setName(ownerName);
                            plugin.getPlayerDAO().updatePlayer(owner);
                        }
                    }
                }
            } catch (java.sql.SQLException e) {
                logger.warning("Error fetching owner name for land " + land.getId() + ": " + e.getMessage());
            }
            player.sendMessage(i18nManager.getMessage("info.owner", ownerName));
        }
        
        player.sendMessage(i18nManager.getMessage("info.world", land.getWorld()));
        
        String coords;
        if (land.getTeleportX() != null) {
            coords = String.format("%.1f, %.1f, %.1f", 
                land.getTeleportX(), land.getTeleportY(), land.getTeleportZ());
        } else {
            int centerX = (land.getX1() + land.getX2()) / 2;
            int centerZ = (land.getZ1() + land.getZ2()) / 2;
            coords = String.format("%d, ~, %d", centerX, centerZ);
        }
        player.sendMessage(i18nManager.getMessage("info.coords", coords));
        
        player.sendMessage(i18nManager.getMessage("info.area", String.valueOf(calculateArea(land))));
        
        // 显示保护规则
        player.sendMessage(i18nManager.getMessage("info.rules-header"));
        java.util.Map<io.github.railgun19457.easyland.model.LandFlag, Boolean> flagMap = land.getFlagMap();
        
        for (io.github.railgun19457.easyland.model.LandFlag flag : io.github.railgun19457.easyland.model.LandFlag.values()) {
            boolean enabled = flagMap.getOrDefault(flag, false);
            String status = enabled ? i18nManager.getMessage("rule.status-allow") : i18nManager.getMessage("rule.status-deny");
            player.sendMessage(i18nManager.getMessage("info.rule-format", i18nManager.getMessage("flags." + flag.getName()), flag.getName(), status));
        }
    }
    
    /**
     * 处理create命令。
     */
    private void handleCreate(Player player, String[] args, String commandName) {
        if (!checkPermission(player, "easyland.create", "permission.no-create")) {
            return;
        }
        
        // 检查玩家是否有完整的选区
        io.github.railgun19457.easyland.core.SelectionManager selectionManager = plugin.getSelectionManager();
        if (!selectionManager.hasCompleteSelection(player)) {
            player.sendMessage(i18nManager.getMessage("select.incomplete"));
            return;
        }
        
        // 检查选区是否在同一个世界
        if (!selectionManager.isSelectionInSameWorld(player)) {
            player.sendMessage(i18nManager.getMessage("select.different-worlds"));
            return;
        }
        
        // 获取选区位置
        Location pos1 = selectionManager.getPos1(player);
        Location pos2 = selectionManager.getPos2(player);
        
        // 获取可选的领地名称
        String name = null;
        if (args.length > 1) {
            name = args[1];
            if (!isValidLandName(name)) {
                player.sendMessage(i18nManager.getMessage("general.invalid-name-format"));
                return;
            }
        }
        
        // 创建领地
        Land land = landManager.createLand(player, pos1, pos2, name);
        
        if (land != null) {
            player.sendMessage(i18nManager.getMessage("create.success", String.valueOf(land.getId())));
            // 显示领地边界
            plugin.getLandVisualizer().showLandBoundary(player, land, 10);
            // 清除选区
            selectionManager.clearSelection(player);
        } else {
            player.sendMessage(i18nManager.getMessage("create.failed"));
        }
    }
    
    /**
     * 处理abandon命令。
     */
    private void handleAbandon(Player player, String[] args, String commandName) {
        if (!checkPermission(player, "easyland.abandon", "permission.no-abandon")) {
            return;
        }
        
        if (!validateArgs(player, args, 2, "abandon <landId>", commandName)) {
            return;
        }
        
        String landId = args[1];
        boolean success = landManager.abandonLand(player, landId);
        
        if (success) {
            player.sendMessage(i18nManager.getMessage("abandon.success"));
        } else {
            player.sendMessage(i18nManager.getMessage("abandon.failed"));
        }
    }
    
    /**
     * 处理show命令。
     */
    private void handleShow(Player player, String[] args, String commandName) {
        if (!checkPermission(player, "easyland.show", "show.no-permission")) {
            return;
        }
        
        int duration = plugin.getConfig().getInt("visualization.default-duration", 10);
        String targetLandName = null;

        if (args.length >= 2) {
            // 尝试解析第一个参数为持续时间
            try {
                duration = Integer.parseInt(args[1]);
                // 如果成功，说明是持续时间，没有指定领地（使用当前/最近）
            } catch (NumberFormatException e) {
                // 不是数字，视为领地名称/ID
                targetLandName = args[1];
                
                // 检查第二个参数是否为持续时间
                if (args.length >= 3) {
                    try {
                        duration = Integer.parseInt(args[2]);
                    } catch (NumberFormatException ex) {
                        // 忽略无效的持续时间
                    }
                }
            }
        }
        
        if (duration < 1) {
            player.sendMessage(i18nManager.getMessage("show.invalid-duration", String.valueOf(plugin.getConfig().getInt("visualization.max-duration", 60))));
            return;
        }
        
        Land land = null;
        if (targetLandName != null) {
            Optional<Land> landOpt = landManager.getLandByIdOrName(targetLandName);
            if (landOpt.isPresent()) {
                land = landOpt.get();
            } else {
                player.sendMessage(i18nManager.getMessage("show.no-land-found"));
                return;
            }
        } else {
            // 未指定目标，尝试当前位置
            land = landManager.getLandAt(player.getLocation());
            
            // 如果不在领地内，尝试最近的领地
            if (land == null) {
                Optional<Land> nearest = landManager.getNearestLand(player.getLocation());
                if (nearest.isPresent()) {
                    land = nearest.get();
                    String landDisplayName = land.getName() != null ? land.getName() : String.valueOf(land.getId());
                    player.sendMessage(i18nManager.getMessage("show.showing-nearest", landDisplayName));
                }
            }
        }
        
        if (land == null) {
            player.sendMessage(i18nManager.getMessage("show.no-land-here"));
            return;
        }
        
        // Show the land boundary
        plugin.getLandVisualizer().showLandBoundary(player, land, duration);
        player.sendMessage(i18nManager.getMessage("show.success", String.valueOf(duration)));
    }
    
    /**
     * 处理rule命令。
     */
    private void handleRule(Player player, String[] args, String commandName) {
        // 移除统一的权限检查，改为在具体操作中检查（允许领地主人管理自己的领地）
        // if (!checkPermission(player, "easyland.rule", "permission.no-rule")) {
        //     return;
        // }
        
        String landId = null;
        String rule = null;
        String valueStr = null;
        
        if (args.length == 2) {
            // rule <land> (显示该领地所有规则) 或 rule <rule> (显示当前领地该规则)
            String arg1 = args[1];
            Optional<Land> landOpt = landManager.getLandByIdOrName(arg1);
            
            if (landOpt.isPresent()) {
                Land land = landOpt.get();
                if (!checkRulePermission(player, land)) return;
                showLandRules(player, land);
                return;
            } else {
                if (PROTECTION_RULES.contains(arg1.toLowerCase())) {
                    Land currentLand = landManager.getLandAt(player.getLocation());
                    if (currentLand == null) {
                        player.sendMessage(i18nManager.getMessage("general.not-in-land"));
                        return;
                    }
                    if (!checkRulePermission(player, currentLand)) return;
                    showRuleStatus(player, currentLand, arg1);
                    return;
                } else {
                    player.sendMessage(i18nManager.getMessage("show.no-land-found"));
                    return;
                }
            }
        } else if (args.length == 3) {
            // rule <land> <rule> (显示状态) 或 rule <rule> <value> (设置当前领地)
            String arg1 = args[1];
            String arg2 = args[2];
            
            Optional<Land> landOpt = landManager.getLandByIdOrName(arg1);
            if (landOpt.isPresent()) {
                if (PROTECTION_RULES.contains(arg2.toLowerCase())) {
                    Land land = landOpt.get();
                    if (!checkRulePermission(player, land)) return;
                    showRuleStatus(player, land, arg2);
                    return;
                } else {
                    player.sendMessage(i18nManager.getMessage("rule.invalid-rule", String.join(", ", PROTECTION_RULES)));
                    return;
                }
            } else {
                Land currentLand = landManager.getLandAt(player.getLocation());
                if (currentLand == null) {
                    player.sendMessage(i18nManager.getMessage("general.not-in-land"));
                    return;
                }
                // 检查权限
                if (!checkRulePermission(player, currentLand)) return;
                
                landId = String.valueOf(currentLand.getId());
                rule = arg1;
                valueStr = arg2;
            }
        } else if (args.length == 4) {
            // rule <land> <rule> <value>
            Optional<Land> landOpt = landManager.getLandByIdOrName(args[1]);
            if (!landOpt.isPresent()) {
                player.sendMessage(i18nManager.getMessage("show.no-land-found"));
                return;
            }
            Land land = landOpt.get();
            if (!checkRulePermission(player, land)) return;
            
            landId = args[1];
            rule = args[2];
            valueStr = args[3];
        } else {
            player.sendMessage(i18nManager.getMessage("general.invalid-args", "/" + commandName + " rule [land] [rule] [true|false]"));
            return;
        }
        
        if (!PROTECTION_RULES.contains(rule.toLowerCase())) {
            player.sendMessage(i18nManager.getMessage("rule.invalid-rule", String.join(", ", PROTECTION_RULES)));
            return;
        }
        
        boolean value;
        if (valueStr.equalsIgnoreCase("true") || valueStr.equalsIgnoreCase("on") || valueStr.equalsIgnoreCase("allow")) {
            value = true;
        } else if (valueStr.equalsIgnoreCase("false") || valueStr.equalsIgnoreCase("off") || valueStr.equalsIgnoreCase("deny")) {
            value = false;
        } else {
            player.sendMessage(i18nManager.getMessage("general.invalid-boolean"));
            return;
        }
        
        if (landManager.setLandFlag(player, landId, rule, value)) {
            player.sendMessage(i18nManager.getMessage("rule.success", rule, String.valueOf(value)));
        } else {
            player.sendMessage(i18nManager.getMessage("rule.failed"));
        }
    }

    /**
     * 检查玩家是否有权限使用rule命令。
     * 允许拥有 easyland.rule 权限的玩家，或者领地主人。
     */
    private boolean checkRulePermission(Player player, Land land) {
        if (player.hasPermission("easyland.rule")) {
            return true;
        }
        if (land != null && permissionManager.isLandOwner(player, land)) {
            return true;
        }
        player.sendMessage(i18nManager.getMessage("permission.no-rule"));
        return false;
    }

    private void showLandRules(Player player, Land land) {
        player.sendMessage(i18nManager.getMessage("info.header"));
        player.sendMessage(i18nManager.getMessage("info.name", land.getName() != null ? land.getName() : String.valueOf(land.getId())));
        
        for (String rule : PROTECTION_RULES) {
            showRuleStatus(player, land, rule);
        }
    }

    private void showRuleStatus(Player player, Land land, String rule) {
        io.github.railgun19457.easyland.model.LandFlag flag = null;
        for (io.github.railgun19457.easyland.model.LandFlag f : io.github.railgun19457.easyland.model.LandFlag.values()) {
            if (f.getName().equalsIgnoreCase(rule)) {
                flag = f;
                break;
            }
        }

        if (flag == null) {
            return;
        }

        java.util.Map<io.github.railgun19457.easyland.model.LandFlag, Boolean> flags = land.getFlagMap();
        // 数据库完整性检查确保了所有标志都存在
        boolean enabled = Boolean.TRUE.equals(flags.get(flag));

        String status = enabled ? i18nManager.getMessage("rule.status-allow") : i18nManager.getMessage("rule.status-deny");
        player.sendMessage(i18nManager.getMessage("rule.format", i18nManager.getMessage("flags." + flag.getName()), flag.getName(), status));
    }
    
    /**
     * 处理reload命令。
     */
    private void handleReload(CommandSender sender) {
        // 统一的权限检查逻辑
        boolean hasPermission = (sender instanceof Player p) 
            ? permissionManager.hasPermission(p, "easyland.admin") 
            : sender.hasPermission("easyland.admin");
        
        if (!hasPermission) {
            sender.sendMessage(i18nManager.getMessage("general.no-permission"));
            return;
        }
        
        // 重新加载配置
        plugin.reloadConfig();
        i18nManager.reload();
        
        sender.sendMessage(i18nManager.getMessage("general.reload-success"));
    }
    
    /**
     * 处理rename命令。
     */
    private void handleRename(Player player, String[] args, String commandName) {
        // 检查是否有 rename 或 admin 权限
        boolean hasRenamePermission = permissionManager.hasPermission(player, "easyland.rename");
        boolean hasAdminPermission = permissionManager.hasPermission(player, "easyland.admin");
        
        if (!hasRenamePermission && !hasAdminPermission) {
            player.sendMessage(i18nManager.getMessage("permission.no-rename"));
            return;
        }
        
        if (!validateArgs(player, args, 3, "rename <landId> <newName>", commandName)) {
            return;
        }
        
        String landId = args[1];
        String newName = args[2];
        
        if (!isValidLandName(newName)) {
            player.sendMessage(i18nManager.getMessage("general.invalid-name-format"));
            return;
        }
        
        boolean success = landManager.renameLand(player, landId, newName);
        
        if (success) {
            player.sendMessage(i18nManager.getMessage("rename.success", newName));
        } else {
            player.sendMessage(i18nManager.getMessage("rename.failed"));
        }
    }
    
    /**
     * 处理subcreate命令。
     */
    private void handleSubCreate(Player player, String[] args, String commandName) {
        if (!checkPermission(player, "easyland.subcreate", "permission.no-subcreate")) {
            return;
        }
        
        if (!validateArgs(player, args, 2, "subcreate <parentLand> [name]", commandName)) {
            return;
        }
        
        String parentLandIdOrName = args[1];
        String name = null;
        if (args.length > 2) {
            name = args[2];
            if (!isValidLandName(name)) {
                player.sendMessage(i18nManager.getMessage("general.invalid-name-format"));
                return;
            }
        }
        
        // 检查玩家是否有完整的选区
        io.github.railgun19457.easyland.core.SelectionManager selectionManager = plugin.getSelectionManager();
        if (!selectionManager.hasCompleteSelection(player)) {
            player.sendMessage(i18nManager.getMessage("select.incomplete"));
            return;
        }
        
        // 检查选区是否在同一个世界
        if (!selectionManager.isSelectionInSameWorld(player)) {
            player.sendMessage(i18nManager.getMessage("select.different-worlds"));
            return;
        }
        
        // 获取选区位置
        Location pos1 = selectionManager.getPos1(player);
        Location pos2 = selectionManager.getPos2(player);
        
        try {
            Land subClaim = landManager.createSubClaim(player, parentLandIdOrName, pos1, pos2, name);
            
            if (subClaim != null) {
                player.sendMessage(i18nManager.getMessage("subcreate.success", String.valueOf(subClaim.getId())));
                // 显示子领地边界
                plugin.getLandVisualizer().showLandBoundary(player, subClaim, 10);
                // 清除选区
                selectionManager.clearSelection(player);
            }
        } catch (SubClaimException e) {
            player.sendMessage("§c" + e.getMessage());
        }
    }
    
    /**
     * 处理migrate命令。
     */
    private void handleMigrate(CommandSender sender, String[] args) {
        if (!sender.hasPermission("easyland.admin.migrate")) {
            sender.sendMessage(i18nManager.getMessage("general.no-permission"));
            return;
        }
        
        // 如果有参数，处理确认或取消
        if (args.length > 1) {
            String action = args[1].toLowerCase();
            
            if (action.equals("confirm")) {
                try {
                    // 使用可复用的MigrationManager实例执行迁移
                    boolean success = migrationManager.runMigration(sender);
                    
                    if (success) {
                        sender.sendMessage(i18nManager.getMessage("migrate.success-short"));
                        logger.info("数据迁移成功完成，由 " + sender.getName() + " 执行。");
                    } else {
                        sender.sendMessage(i18nManager.getMessage("migrate.failed"));
                        logger.warning("数据迁移失败，由 " + sender.getName() + " 执行。");
                    }
                } catch (Exception e) {
                    // 特别处理文件不存在的情况
                    if (e.getCause() instanceof MigrationFileNotFoundException) {
                        MigrationFileNotFoundException ex = (MigrationFileNotFoundException) e.getCause();
                        sender.sendMessage(i18nManager.getMessage("migrate.failed-with-reason", ex.getMessage()));
                        sender.sendMessage(i18nManager.getMessage("migrate.failed-file-instruction"));
                        logger.warning("迁移失败，由 " + sender.getName() + " 执行。缺失文件: " + ex.getFileName());
                    } else {
                        sender.sendMessage(i18nManager.getMessage("migrate.failed-with-reason", e.getMessage()));
                        sender.sendMessage(i18nManager.getMessage("migrate.failed-check-console"));
                        logger.severe("数据迁移失败，由 " + sender.getName() + " 执行: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
                
            } else if (action.equals("cancel")) {
                sender.sendMessage(i18nManager.getMessage("migrate.cancelled"));
                
            } else {
                sender.sendMessage(i18nManager.getMessage("general.invalid-args", "/el migrate [confirm|cancel]"));
            }
        } else {
            // 没有参数时，显示确认信息
            sender.sendMessage(i18nManager.getMessage("migrate.warning"));
            sender.sendMessage(i18nManager.getMessage("migrate.warning-details"));
            sender.sendMessage(i18nManager.getMessage("migrate.backup-reminder"));
            sender.sendMessage(i18nManager.getMessage("migrate.confirm-instruction"));
            sender.sendMessage(i18nManager.getMessage("migrate.cancel-instruction"));
        }
    }
    
    /**
     * 显示帮助信息。
     */
    private void showHelp(CommandSender sender, String commandName) {
        sender.sendMessage(i18nManager.getMessage("help.header"));
        
        // 使用映射表循环显示帮助消息
        for (Map.Entry<String, List<String>> entry : HELP_MESSAGES.entrySet()) {
            String permission = entry.getKey();
            // 特殊处理rename需要admin或rename权限
            if (permission.equals("easyland.rename")) {
                if (sender.hasPermission("easyland.rename") || sender.hasPermission("easyland.admin")) {
                    for (String messageKey : entry.getValue()) {
                        sender.sendMessage(i18nManager.getMessage(messageKey));
                    }
                }
            } else if (sender.hasPermission(permission)) {
                for (String messageKey : entry.getValue()) {
                    sender.sendMessage(i18nManager.getMessage(messageKey));
                }
            }
        }
    }
    
    /**
     * 计算领地面积。
     */
    private int calculateArea(Land land) {
        return (land.getX2() - land.getX1() + 1) * (land.getZ2() - land.getZ1() + 1);
    }
    
    /**
     * Handles the setspawn command.
     */
    private void handleSetSpawn(Player player, String[] args, String commandName) {
        if (!checkPermission(player, "easyland.setspawn", "permission.no-setspawn")) {
            return;
        }

        if (!validateArgs(player, args, 2, "setspawn <land>", commandName)) {
            return;
        }

        String landName = args[1];
        if (landManager.setLandSpawn(player, landName)) {
            player.sendMessage(i18nManager.getMessage("setspawn.success", landName));
        } else {
            player.sendMessage(i18nManager.getMessage("setspawn.failed", landName));
        }
    }

    /**
     * Handles the tp command.
     */
    private void handleTeleport(Player player, String[] args, String commandName) {
        if (!checkPermission(player, "easyland.tp", "permission.no-tp")) {
            return;
        }

        if (!validateArgs(player, args, 2, "tp <land>", commandName)) {
            return;
        }

        String landName = args[1];
        if (landManager.teleportToLand(player, landName)) {
            player.sendMessage(i18nManager.getMessage("tp.success", landName));
        } else {
            player.sendMessage(i18nManager.getMessage("tp.failed", landName));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        // 第一个参数：子命令
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            
            for (String subCommand : SUBCOMMANDS) {
                if (subCommand.startsWith(partial)) {
                    // 检查权限
                    if (hasPermissionForCommand(sender, subCommand)) {
                        completions.add(subCommand);
                    }
                }
            }
            return completions;
        }
        
        // 第二个参数及以后：根据子命令提供不同的补全
        if (args.length >= 2) {
            String subCommand = args[0].toLowerCase();
            
            // 对于控制台，只提供管理命令的补全
            if (!(sender instanceof Player) && !ADMIN_SUBCOMMANDS.contains(subCommand)) {
                return completions;
            }
            
            // 对于需要玩家上下文的命令，如果发送者不是玩家，不提供补全
            boolean requiresPlayerContext = !subCommand.equals("reload") &&
                !subCommand.equals("migrate") && !subCommand.equals("admin") &&
                !subCommand.equals("help");
                
            if (requiresPlayerContext && !(sender instanceof Player)) {
                return completions;
            }
            
            Player player = (sender instanceof Player) ? (Player) sender : null;
            String currentArg = args[args.length - 1].toLowerCase();
            
            switch (subCommand) {
                case "claim":
                    // 补全无主领地（ownerId == 0）
                    if (args.length == 2) {
                        filterAndAdd(completions, getUnownedLandCompletions(), currentArg);
                    }
                    break;

                case "delete":
                    // 管理员可以补全所有领地，普通玩家只能补全自己的领地
                    if (player != null && args.length == 2) {
                        if (player.hasPermission("easyland.admin.manage") || player.hasPermission("easyland.admin")) {
                            filterAndAdd(completions, getAllLandCompletions(), currentArg);
                        } else {
                            filterAndAdd(completions, getPlayerLandCompletions(player), currentArg);
                        }
                    }
                    break;

                case "abandon":
                case "rename":
                case "setspawn":
                case "subcreate":
                case "trustlist":
                    // 补全玩家拥有的领地ID和名称
                    if (args.length == 2 && player != null) {
                        filterAndAdd(completions, getPlayerLandCompletions(player), currentArg);
                    }
                    break;
                    
                case "trust":
                case "untrust":
                    if (args.length == 2 && player != null) {
                        // 补全玩家拥有的领地ID和名称
                        filterAndAdd(completions, getPlayerLandCompletions(player), currentArg);
                    } else if (args.length == 3 && player != null) {
                        // 补全在线玩家名称
                        for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                            if (!onlinePlayer.equals(player) && onlinePlayer.getName().toLowerCase().startsWith(currentArg)) {
                                completions.add(onlinePlayer.getName());
                            }
                        }
                    }
                    break;
                    
                case "info":
                case "tp":
                    if (args.length == 2 && player != null) {
                        // 补全所有领地ID和名称
                        filterAndAdd(completions, getAllLandCompletions(), currentArg);
                    }
                    break;
                    
                case "select":
                    // select 命令不需要参数补全
                    break;
                    
                case "list":
                    if (args.length == 2) {
                        // 补全页码
                        filterAndAdd(completions, Arrays.asList("1", "2", "3"), currentArg);
                    }
                    break;
                    
                case "show":
                    if (args.length == 2) {
                        // 补全领地名称
                        filterAndAdd(completions, getAllLandCompletions(), currentArg);
                    }
                    break;
                    
                case "rule":
                    if (args.length == 2) {
                        // 只补全领地
                        if (player != null) {
                            filterAndAdd(completions, getPlayerLandCompletions(player), currentArg);
                        }
                    } else if (args.length == 3) {
                        // args[1] 是领地，args[2] 是规则
                        filterAndAdd(completions, PROTECTION_RULES, currentArg);
                    } else if (args.length == 4) {
                        // args[1] 领地，args[2] 规则，args[3] 值
                        filterAndAdd(completions, Arrays.asList("true", "false"), currentArg);
                    }
                    break;
            }
        }
        
        return completions;
    }

    /**
     * 过滤并添加补全建议。
     *
     * @param target 目标列表
     * @param source 源列表
     * @param partial 部分参数
     */
    private void filterAndAdd(List<String> target, List<String> source, String partial) {
        for (String s : source) {
            if (s.toLowerCase().startsWith(partial)) {
                target.add(s);
            }
        }
    }
    
    /**
     * 检查发送者是否有执行特定命令的权限。
     */
    private boolean hasPermissionForCommand(CommandSender sender, String command) {
        // 使用映射表查找权限
        String permission = COMMAND_PERMISSIONS.get(command);
        
        // help命令不需要权限（null表示不需要权限）
        if (permission == null) {
            return true;
        }
        
        // rename需要特殊处理：rename或admin权限都可以
        if (command.equals("rename")) {
            return sender.hasPermission("easyland.rename") || sender.hasPermission("easyland.admin");
        }

        // rule命令特殊处理：玩家可以使用（针对自己的领地），或者有easyland.rule权限
        if (command.equals("rule")) {
            return (sender instanceof Player) || sender.hasPermission("easyland.rule");
        }
        
        return sender.hasPermission(permission);
    }
    
    /**
     * 处理select命令。
     */
    private void handleSelect(Player player, String[] args, String commandName) {
        if (!checkPermission(player, "easyland.select", "permission.no-select")) {
            return;
        }
        
        // 给玩家选择工具（木锄头）
        org.bukkit.inventory.ItemStack tool = new org.bukkit.inventory.ItemStack(org.bukkit.Material.WOODEN_HOE);
        org.bukkit.inventory.meta.ItemMeta meta = tool.getItemMeta();
        if (meta != null) {
            // 设置名称
            String name = i18nManager.getMessage("tool.name");
            meta.displayName(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(name)
                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
            
            // 设置说明文字
            List<String> loreStrings = i18nManager.getStringList("tool.lore");
            List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
            for (String line : loreStrings) {
                lore.add(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(line)
                    .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
            }
            meta.lore(lore);
            
            // 添加附魔效果（发光）
            meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
            
            // 设置为不可破坏
            meta.setUnbreakable(true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_UNBREAKABLE);
            
            tool.setItemMeta(meta);
        }
        
        player.getInventory().addItem(tool);
        player.sendMessage(i18nManager.getMessage("select.given"));
    }
    
    /**
     * 获取玩家拥有的领地ID和名称列表。
     */
    private List<String> getPlayerLandCompletions(Player player) {
        List<String> completions = new ArrayList<>();
        try {
            // 获取所有领地（不分页，或者获取足够多）
            // 这里假设 listPlayerLands 支持获取所有，或者我们只获取第一页
            // 实际上应该有一个方法获取所有领地而不分页，或者分页很大
            // 为了简单起见，我们暂时只获取第一页，或者需要修改 LandManager 增加 getAllPlayerLands
            // 考虑到性能，tab补全不应该查询太多数据。
            // 暂时使用 listPlayerLands(uuid, 1) 并假设每页数量足够大，或者循环获取
            // 但为了响应速度，只获取第一页可能是一个妥协。
            // 更好的做法是在 LandManager 中增加 getPlayerLandNames(uuid)
            
            List<Land> lands = landManager.listPlayerLands(player.getUniqueId(), 1);
            for (Land land : lands) {
                if (land.getName() != null) {
                    completions.add(land.getName());
                }
            }
        } catch (Exception e) {
            // 忽略错误
        }
        return completions;
    }
    
    /**
     * 获取所有领地ID和名称列表。
     */
    private List<String> getAllLandCompletions() {
        List<String> completions = new ArrayList<>();
        try {
            List<Land> allLands = landManager.getAllLands(); // 这个方法返回所有领地
            for (Land land : allLands) {
                if (land.getName() != null) {
                    completions.add(land.getName());
                }
            }
        } catch (Exception e) {
            // 忽略错误
        }
        return completions;
    }
    
    /**
     * 获取所有无主领地（ownerId=0）的名称列表。
     */
    private List<String> getUnownedLandCompletions() {
        List<String> completions = new ArrayList<>();
        try {
            List<Land> allLands = landManager.getAllLands();
            for (Land land : allLands) {
                if (land.getOwnerId() == 0) {
                    if (land.getName() != null && !land.getName().isEmpty()) {
                        completions.add(land.getName());
                    }
                }
            }
        } catch (Exception e) {
            // 忽略错误
        }
        return completions;
    }

    /**
     * 根据玩家ID获取玩家名称。
     *
     * @param ownerId 玩家数据库ID
     * @return 玩家名称，如果找不到则返回 "未知"
     */
    private String getOwnerName(int ownerId) {
        try {
            Optional<io.github.railgun19457.easyland.model.Player> playerOpt = 
                plugin.getPlayerDAO().getPlayerById(ownerId);
            if (playerOpt.isPresent()) {
                return playerOpt.get().getName();
            }
        } catch (Exception e) {
            logger.warning("Failed to get owner name for ID " + ownerId + ": " + e.getMessage());
        }
        return i18nManager.getMessage("general.unknown");
    }
}
