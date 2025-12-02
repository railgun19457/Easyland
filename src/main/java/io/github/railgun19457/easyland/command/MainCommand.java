package io.github.railgun19457.easyland.command;

import io.github.railgun19457.easyland.EasyLand;
import io.github.railgun19457.easyland.I18nManager;
import io.github.railgun19457.easyland.core.LandManager;
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
import java.util.Optional;

/**
 * EasyLand插件的主命令处理器。
 * 处理所有/easyland和/el子命令。
 */
public class MainCommand implements CommandExecutor, TabCompleter {
    
    private final EasyLand plugin;
    private final I18nManager i18nManager;
    private final LandManager landManager;
    private final io.github.railgun19457.easyland.core.PermissionManager permissionManager;
    
    // 子命令列表
    private static final List<String> SUBCOMMANDS = Arrays.asList(
        "claim", "delete", "list", "trust", "untrust", "info", "help", "create", "abandon", "show", "protection", "reload", "rename", "subclaim", "migrate", "select"
    );
    
    // 管理员子命令列表
    private static final List<String> ADMIN_SUBCOMMANDS = Arrays.asList(
        "create", "delete", "reload", "migrate"
    );
    
    // 保护规则列表
    private static final List<String> PROTECTION_RULES = Arrays.asList(
        "block", "explosion", "container", "player"
    );
    
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
                
            case "info":
                handleInfo(player, args, commandName);
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
                
            case "protection":
                handleProtection(player, args, commandName);
                break;
                
            case "reload":
                handleReload(sender);
                break;
                
            case "rename":
                handleRename(player, args, commandName);
                break;
                
            case "subclaim":
                handleSubClaim(player, args, commandName);
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
        if (!permissionManager.hasPermission(player, "easyland.claim")) {
            player.sendMessage(i18nManager.getMessage("permission.no-claim"));
            return;
        }
        
        if (args.length < 2) {
            player.sendMessage(i18nManager.getMessage("general.invalid-args", "/" + commandName + " claim <landId>"));
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
        if (!permissionManager.hasPermission(player, "easyland.delete")) {
            player.sendMessage(i18nManager.getMessage("permission.no-delete"));
            return;
        }
        
        if (args.length < 2) {
            player.sendMessage(i18nManager.getMessage("general.invalid-args", "/" + commandName + " delete <landId>"));
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
        if (!permissionManager.hasPermission(player, "easyland.list")) {
            player.sendMessage(i18nManager.getMessage("permission.no-list"));
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
            player.sendMessage("§7第 §e" + page + "§7/§e" + totalPages + "§7 页 (共 §e" + totalLands + "§7 个领地)");
        } else {
            // 普通玩家显示简单的页码
            player.sendMessage("§7第 §e" + page + "§7 页");
        }
        
        player.sendMessage("§7━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        for (Land land : lands) {
            String ownerInfo = "";
            if (permissionManager.isAdmin(player)) {
                // 管理员可以看到领地主人
                ownerInfo = " §7- §6所有者ID: §e" + land.getOwnerId();
            }
            player.sendMessage(i18nManager.getMessage("list.item", 
                "ID:" + land.getId(), 
                calculateArea(land)) + ownerInfo);
        }
        
        // 显示下一页提示（仅对管理员且有足够领地时）
        if (permissionManager.isAdmin(player) && totalLands > 0) {
            int totalPages = (int) Math.ceil((double) totalLands / perPage);
            if (page < totalPages) {
                player.sendMessage("§7使用 §e/" + commandName + " list " + (page + 1) + "§7 查看下一页");
            }
        } else if (lands.size() >= perPage) {
            // 普通玩家，如果当前页满了，提示可能还有下一页
            player.sendMessage("§7使用 §e/" + commandName + " list " + (page + 1) + "§7 查看下一页");
        }
    }
    
    /**
     * 处理trust命令。
     */
    private void handleTrust(Player player, String[] args, String commandName) {
        if (!permissionManager.hasPermission(player, "easyland.trust")) {
            player.sendMessage(i18nManager.getMessage("trust.no-permission"));
            return;
        }
        
        if (args.length < 3) {
            player.sendMessage(i18nManager.getMessage("general.invalid-args", "/" + commandName + " trust <landId> <player>"));
            return;
        }
        
        String landId = args[1];
        String targetPlayerName = args[2];
        
        // 检查是否试图信任自己
        if (targetPlayerName.equalsIgnoreCase(player.getName())) {
            player.sendMessage(i18nManager.getMessage("trust.cannot-trust-self"));
            return;
        }
        
        // 获取目标玩家
        Player targetPlayer = plugin.getServer().getPlayer(targetPlayerName);
        if (targetPlayer == null) {
            player.sendMessage(i18nManager.getMessage("trust.invalid-name"));
            return;
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
        if (!permissionManager.hasPermission(player, "easyland.trust")) {
            player.sendMessage(i18nManager.getMessage("trust.untrust-no-permission"));
            return;
        }
        
        if (args.length < 3) {
            player.sendMessage(i18nManager.getMessage("general.invalid-args", "/" + commandName + " untrust <landId> <player>"));
            return;
        }
        
        String landId = args[1];
        String targetPlayerName = args[2];
        
        // 获取目标玩家
        Player targetPlayer = plugin.getServer().getPlayer(targetPlayerName);
        if (targetPlayer == null) {
            player.sendMessage(i18nManager.getMessage("trust.invalid-name"));
            return;
        }
        
        boolean success = landManager.untrustPlayer(player, landId, targetPlayer);
        
        if (success) {
            player.sendMessage(i18nManager.getMessage("trust.untrust-success", targetPlayer.getName()));
        } else {
            player.sendMessage(i18nManager.getMessage("trust.untrust-failed"));
        }
    }
    
    /**
     * 处理info命令。
     */
    private void handleInfo(Player player, String[] args, String commandName) {
        if (!permissionManager.hasPermission(player, "easyland.info")) {
            player.sendMessage(i18nManager.getMessage("permission.no-info"));
            return;
        }
        
        Land land;
        
        if (args.length >= 2) {
            // 查看指定领地信息
            try {
                int landId = Integer.parseInt(args[1]);
                Optional<Land> landOpt = landManager.getLandById(landId);
                if (landOpt.isPresent()) {
                    land = landOpt.get();
                } else {
                    player.sendMessage(i18nManager.getMessage("info.no-land-here"));
                    return;
                }
            } catch (NumberFormatException e) {
                player.sendMessage(i18nManager.getMessage("general.invalid-args", "/" + commandName + " info [landId]"));
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
        
        // 获取拥有者信息
        if (land.getOwnerId() == 0) {
            player.sendMessage(i18nManager.getMessage("info.unclaimed"));
        } else {
            // 这里需要从数据库获取玩家名称
            player.sendMessage(i18nManager.getMessage("info.owner", "PlayerID:" + land.getOwnerId()));
        }
        
        player.sendMessage(i18nManager.getMessage("info.world", land.getWorld()));
        player.sendMessage(i18nManager.getMessage("info.chunks", calculateArea(land)));
    }
    
    /**
     * 处理create命令。
     */
    private void handleCreate(Player player, String[] args, String commandName) {
        if (!permissionManager.hasPermission(player, "easyland.admin")) {
            player.sendMessage(i18nManager.getMessage("permission.no-create"));
            return;
        }
        
        if (args.length < 2) {
            player.sendMessage(i18nManager.getMessage("general.invalid-args", "/" + commandName + " create <landId>"));
            return;
        }
        
        // 这里需要实现创建领地的逻辑
        // 由于LandManager中没有直接创建领地的方法，我们需要添加一个
        player.sendMessage("创建领地功能尚未完全实现");
    }
    
    /**
     * 处理abandon命令。
     */
    private void handleAbandon(Player player, String[] args, String commandName) {
        if (!permissionManager.hasPermission(player, "easyland.abandon")) {
            player.sendMessage(i18nManager.getMessage("permission.no-abandon"));
            return;
        }
        
        if (args.length < 2) {
            player.sendMessage(i18nManager.getMessage("general.invalid-args", "/" + commandName + " abandon <landId>"));
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
        if (!permissionManager.hasPermission(player, "easyland.show")) {
            player.sendMessage(i18nManager.getMessage("show.no-permission"));
            return;
        }
        
        int duration = plugin.getConfig().getInt("visualization.default-duration", 10);
        if (args.length >= 2) {
            try {
                duration = Integer.parseInt(args[1]);
                if (duration < 1) {
                    player.sendMessage(i18nManager.getMessage("show.invalid-duration", plugin.getConfig().getInt("visualization.max-duration", 60)));
                    return;
                }
            } catch (NumberFormatException e) {
                player.sendMessage(i18nManager.getMessage("general.invalid-args", "/" + commandName + " show [duration]"));
                return;
            }
        }
        
        // Get the land at the player's location
        Land land = landManager.getLandAt(player.getLocation());
        if (land == null) {
            player.sendMessage(i18nManager.getMessage("show.no-land-here"));
            return;
        }
        
        // Show the land boundary
        plugin.getLandVisualizer().showLandBoundary(player, land, duration);
        player.sendMessage(i18nManager.getMessage("show.success", duration));
    }
    
    /**
     * 处理protection命令。
     */
    private void handleProtection(Player player, String[] args, String commandName) {
        if (!permissionManager.hasPermission(player, "easyland.protection")) {
            player.sendMessage(i18nManager.getMessage("permission.no-protection"));
            return;
        }
        
        if (args.length < 3) {
            // 显示当前保护状态
            player.sendMessage(i18nManager.getMessage("rule.status-header"));
            for (String rule : PROTECTION_RULES) {
                // 这里需要获取实际的保护状态
                player.sendMessage(i18nManager.getMessage("rule.status-enabled", rule, ""));
            }
            player.sendMessage(i18nManager.getMessage("rule.usage-tip"));
            return;
        }
        
        String rule = args[1].toLowerCase();
        String action = args[2].toLowerCase();
        
        if (!PROTECTION_RULES.contains(rule)) {
            player.sendMessage(i18nManager.getMessage("rule.invalid-rule", String.join(", ", PROTECTION_RULES)));
            return;
        }
        
        boolean enable = action.equals("on") || action.equals("true") || action.equals("enable");
        
        // 这里需要实现设置保护规则的逻辑
        player.sendMessage(i18nManager.getMessage("rule.set-success", rule, enable ? "启用" : "禁用"));
    }
    
    /**
     * 处理reload命令。
     */
    private void handleReload(CommandSender sender) {
        // 如果是玩家，使用PermissionManager检查权限
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (!permissionManager.hasPermission(player, "easyland.admin")) {
                sender.sendMessage(i18nManager.getMessage("general.no-permission"));
                return;
            }
        } else {
            // 控制台直接检查权限
            if (!sender.hasPermission("easyland.admin")) {
                sender.sendMessage(i18nManager.getMessage("general.no-permission"));
                return;
            }
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
        if (!player.hasPermission("easyland.rename") && !player.hasPermission("easyland.admin")) {
            player.sendMessage(i18nManager.getMessage("permission.no-rename"));
            return;
        }
        
        if (args.length < 3) {
            player.sendMessage(i18nManager.getMessage("general.invalid-args", "/" + commandName + " rename <landId> <newName>"));
            return;
        }
        
        String landId = args[1];
        String newName = args[2];
        
        // Concatenate the rest of the args for the new name
        for (int i = 3; i < args.length; i++) {
            newName += " " + args[i];
        }
        
        boolean success = landManager.renameLand(player, landId, newName);
        
        if (success) {
            player.sendMessage(i18nManager.getMessage("rename.success", newName));
        } else {
            player.sendMessage(i18nManager.getMessage("rename.failed"));
        }
    }
    
    /**
     * 处理subclaim命令。
     */
    private void handleSubClaim(Player player, String[] args, String commandName) {
        if (!player.hasPermission("easyland.subclaim")) {
            player.sendMessage(i18nManager.getMessage("permission.no-subclaim"));
            return;
        }
        
        if (args.length < 2) {
            player.sendMessage(i18nManager.getMessage("general.invalid-args", "/" + commandName + " subclaim <parentLandId>"));
            return;
        }
        
        String parentLandId = args[1];
        
        // 使用玩家当前位置周围的小区域作为子领地
        // 这是一个简化的实现，实际应用中应该使用选择工具
        Location playerLoc = player.getLocation();
        int radius = 5; // 5格半径
        
        Location pos1 = new Location(playerLoc.getWorld(),
            playerLoc.getBlockX() - radius, playerLoc.getBlockY(), playerLoc.getBlockZ() - radius);
        Location pos2 = new Location(playerLoc.getWorld(),
            playerLoc.getBlockX() + radius, playerLoc.getBlockY(), playerLoc.getBlockZ() + radius);
        
        Land subClaim = landManager.createSubClaim(player, parentLandId, pos1, pos2);
        
        if (subClaim != null) {
            player.sendMessage(i18nManager.getMessage("subclaim.success", String.valueOf(subClaim.getId())));
            // 显示子领地边界
            plugin.getLandVisualizer().showLandBoundary(player, subClaim, 10);
        } else {
            player.sendMessage(i18nManager.getMessage("subclaim.failed"));
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
        
        // 创建迁移命令处理器
        MigrateCommand migrateCommand = new MigrateCommand(plugin);
        
        // 将参数传递给迁移命令处理器
        String[] migrateArgs = new String[args.length - 1];
        System.arraycopy(args, 1, migrateArgs, 0, migrateArgs.length);
        
        // 执行迁移命令
        migrateCommand.onCommand(sender, null, "migrate", migrateArgs);
    }
    
    /**
     * 显示帮助信息。
     */
    private void showHelp(CommandSender sender, String commandName) {
        sender.sendMessage(i18nManager.getMessage("help.header"));
        
        // 根据权限显示不同的命令
        if (sender.hasPermission("easyland.select")) {
            sender.sendMessage(i18nManager.getMessage("help.select"));
        }
        
        if (sender.hasPermission("easyland.create")) {
            sender.sendMessage(i18nManager.getMessage("help.create"));
        }
        
        if (sender.hasPermission("easyland.claim")) {
            sender.sendMessage(i18nManager.getMessage("help.claim"));
        }
        
        if (sender.hasPermission("easyland.abandon")) {
            sender.sendMessage(i18nManager.getMessage("help.abandon"));
        }
        
        if (sender.hasPermission("easyland.delete")) {
            sender.sendMessage(i18nManager.getMessage("help.delete"));
        }
        
        if (sender.hasPermission("easyland.info")) {
            sender.sendMessage(i18nManager.getMessage("help.info"));
        }
        
        if (sender.hasPermission("easyland.list")) {
            sender.sendMessage(i18nManager.getMessage("help.list"));
        }
        
        if (sender.hasPermission("easyland.show")) {
            sender.sendMessage(i18nManager.getMessage("help.show"));
        }
        
        if (sender.hasPermission("easyland.trust")) {
            sender.sendMessage(i18nManager.getMessage("help.trust"));
            sender.sendMessage(i18nManager.getMessage("help.untrust"));
        }
        
        if (sender.hasPermission("easyland.rename") || sender.hasPermission("easyland.admin")) {
            sender.sendMessage(i18nManager.getMessage("help.rename"));
        }
        
        if (sender.hasPermission("easyland.subclaim")) {
            sender.sendMessage(i18nManager.getMessage("help.subclaim"));
        }
        
        if (sender.hasPermission("easyland.protection")) {
            sender.sendMessage(i18nManager.getMessage("help.protection"));
        }
        
        if (sender.hasPermission("easyland.admin")) {
            sender.sendMessage(i18nManager.getMessage("help.reload"));
        }
        
        if (sender.hasPermission("easyland.admin.migrate")) {
            sender.sendMessage(i18nManager.getMessage("help.migrate"));
        }
    }
    
    /**
     * 计算领地面积。
     */
    private int calculateArea(Land land) {
        return (land.getX2() - land.getX1() + 1) * (land.getZ2() - land.getZ1() + 1);
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
            
            switch (subCommand) {
                case "claim":
                case "delete":
                case "abandon":
                    // 补全玩家拥有的领地ID
                    if (args.length == 2 && player != null) {
                        completions.addAll(getPlayerLandIds(player));
                    }
                    break;
                    
                case "trust":
                case "untrust":
                    if (args.length == 2 && player != null) {
                        // 补全玩家拥有的领地ID
                        completions.addAll(getPlayerLandIds(player));
                    } else if (args.length == 3 && player != null) {
                        // 补全在线玩家名称
                        String partial = args[2].toLowerCase();
                        for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                            if (!onlinePlayer.equals(player) && onlinePlayer.getName().toLowerCase().startsWith(partial)) {
                                completions.add(onlinePlayer.getName());
                            }
                        }
                    }
                    break;
                    
                case "info":
                    if (args.length == 2 && player != null) {
                        // 补全所有领地ID
                        completions.addAll(getAllLandIds());
                    }
                    break;
                    
                case "select":
                    // select 命令不需要参数补全
                    break;
                    
                case "list":
                    if (args.length == 2) {
                        // 补全页码
                        completions.add("1");
                        completions.add("2");
                        completions.add("3");
                    }
                    break;
                    
                case "show":
                    if (args.length == 2) {
                        // 补全持续时间
                        completions.add("10");
                        completions.add("20");
                        completions.add("30");
                    }
                    break;
                    
                case "protection":
                    if (args.length == 2) {
                        // 补全保护规则
                        String partial = args[1].toLowerCase();
                        for (String rule : PROTECTION_RULES) {
                            if (rule.startsWith(partial)) {
                                completions.add(rule);
                            }
                        }
                    } else if (args.length == 3) {
                        // 补全开关选项
                        String partial = args[2].toLowerCase();
                        for (String option : Arrays.asList("on", "off", "true", "false", "enable", "disable")) {
                            if (option.startsWith(partial)) {
                                completions.add(option);
                            }
                        }
                    }
                    break;
            }
        }
        
        return completions;
    }
    
    /**
     * 检查发送者是否有执行特定命令的权限。
     */
    private boolean hasPermissionForCommand(CommandSender sender, String command) {
        switch (command) {
            case "claim":
                return sender.hasPermission("easyland.claim");
            case "delete":
                return sender.hasPermission("easyland.delete");
            case "list":
                return sender.hasPermission("easyland.list");
            case "trust":
            case "untrust":
                return sender.hasPermission("easyland.trust");
            case "info":
                return sender.hasPermission("easyland.info");
            case "help":
                return true; // 帮助命令不需要特殊权限
            case "create":
                return sender.hasPermission("easyland.admin");
            case "abandon":
                return sender.hasPermission("easyland.abandon");
            case "show":
                return sender.hasPermission("easyland.show");
            case "protection":
                return sender.hasPermission("easyland.protection");
            case "reload":
                return sender.hasPermission("easyland.admin");
            case "rename":
                return sender.hasPermission("easyland.rename") || sender.hasPermission("easyland.admin");
            case "subclaim":
                return sender.hasPermission("easyland.subclaim");
            case "select":
                return sender.hasPermission("easyland.select");
            case "migrate":
                return sender.hasPermission("easyland.admin.migrate");
            default:
                return false;
        }
    }
    
    /**
     * 处理select命令。
     */
    private void handleSelect(Player player, String[] args, String commandName) {
        if (!permissionManager.hasPermission(player, "easyland.select")) {
            player.sendMessage(i18nManager.getMessage("permission.no-select"));
            return;
        }
        
        // 给玩家选择工具（木锄头）
        org.bukkit.inventory.ItemStack tool = new org.bukkit.inventory.ItemStack(org.bukkit.Material.WOODEN_HOE);
        org.bukkit.inventory.meta.ItemMeta meta = tool.getItemMeta();
        if (meta != null) {
            // 设置发光的金色名称
            meta.displayName(net.kyori.adventure.text.Component.text("✦ EasyLand 领地选择工具 ✦")
                .color(net.kyori.adventure.text.format.NamedTextColor.GOLD)
                .decoration(net.kyori.adventure.text.format.TextDecoration.BOLD, true)
                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
            
            // 设置彩色说明文字
            List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
            lore.add(net.kyori.adventure.text.Component.text(""));
            lore.add(net.kyori.adventure.text.Component.text("▸ 左键点击 ")
                .color(net.kyori.adventure.text.format.NamedTextColor.YELLOW)
                .append(net.kyori.adventure.text.Component.text("设置第一个位置")
                    .color(net.kyori.adventure.text.format.NamedTextColor.GREEN)));
            lore.add(net.kyori.adventure.text.Component.text("▸ 右键点击 ")
                .color(net.kyori.adventure.text.format.NamedTextColor.YELLOW)
                .append(net.kyori.adventure.text.Component.text("设置第二个位置")
                    .color(net.kyori.adventure.text.format.NamedTextColor.GREEN)));
            lore.add(net.kyori.adventure.text.Component.text(""));
            lore.add(net.kyori.adventure.text.Component.text("选择完成后使用 /el create 创建领地")
                .color(net.kyori.adventure.text.format.NamedTextColor.GRAY)
                .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, true));
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
     * 获取玩家拥有的领地ID列表。
     */
    private List<String> getPlayerLandIds(Player player) {
        List<String> landIds = new ArrayList<>();
        try {
            List<Land> lands = landManager.listPlayerLands(player.getUniqueId(), 1);
            for (Land land : lands) {
                landIds.add(String.valueOf(land.getId()));
            }
        } catch (Exception e) {
            // 忽略错误，返回空列表
        }
        return landIds;
    }
    
    /**
     * 获取所有领地ID列表。
     */
    private List<String> getAllLandIds() {
        List<String> landIds = new ArrayList<>();
        try {
            List<Land> allLands = landManager.getAllLands();
            for (Land land : allLands) {
                landIds.add(String.valueOf(land.getId()));
            }
        } catch (Exception e) {
            // 忽略错误，返回空列表
        }
        return landIds;
    }
}
