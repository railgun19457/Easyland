package io.github.railgun19457.easyland.core;

import io.github.railgun19457.easyland.I18nManager;
import io.github.railgun19457.easyland.exception.LandNotFoundException;
import io.github.railgun19457.easyland.model.Land;
import io.github.railgun19457.easyland.model.LandTrust;
import io.github.railgun19457.easyland.storage.LandDAO;
import io.github.railgun19457.easyland.storage.LandTrustDAO;
import io.github.railgun19457.easyland.storage.PlayerDAO;
import org.bukkit.Location;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Core service for managing land claims in the Easyland plugin.
 * This class handles all business logic related to land creation, management,
 * and permissions.
 */
public class LandManager {
    private final Logger logger;
    private final LandDAO landDAO;
    private final PlayerDAO playerDAO;
    private final LandTrustDAO landTrustDAO;
    @SuppressWarnings("unused")
    private final I18nManager i18nManager;
    private final ConfigManager configManager;
    private final LandCache landCache;
    private final PermissionManager permissionManager;

    /**
     * Constructor for LandManager.
     *
     * @param logger            The plugin logger
     * @param landDAO           The land data access object
     * @param playerDAO         The player data access object
     * @param landTrustDAO      The land trust data access object
     * @param i18nManager       The internationalization manager
     * @param configManager     The configuration manager
     * @param landCache         The land cache manager
     * @param permissionManager The permission manager
     */
    public LandManager(Logger logger, LandDAO landDAO, PlayerDAO playerDAO,
                      LandTrustDAO landTrustDAO, I18nManager i18nManager, ConfigManager configManager, 
                      LandCache landCache, PermissionManager permissionManager) {
        this.logger = logger;
        this.landDAO = landDAO;
        this.playerDAO = playerDAO;
        this.landTrustDAO = landTrustDAO;
        this.i18nManager = i18nManager;
        this.configManager = configManager;
        this.landCache = landCache;
        this.permissionManager = permissionManager;
    }

    /**
     * Creates a new land claim.
     *
     * @param creator The player creating the land
     * @param pos1    The first corner position
     * @param pos2    The second corner position
     * @return The created land, or null if creation failed
     */
    public Land createLand(org.bukkit.entity.Player creator, Location pos1, Location pos2) {
        try {
            // Validate positions
            if (!pos1.getWorld().equals(pos2.getWorld())) {
                logger.warning("Positions must be in the same world");
                return null;
            }

            // Get or create player in database
            io.github.railgun19457.easyland.model.Player dbPlayer = playerDAO.getOrCreatePlayer(
                creator.getUniqueId(), creator.getName());
            
            // Check player's land count
            int landCount = landDAO.getLandCountByOwner(dbPlayer.getId());
            if (landCount >= configManager.getMaxLandsPerPlayer()) {
                logger.info("Player " + creator.getName() + " has reached maximum land count");
                return null;
            }

            // Calculate land area
            int x1 = Math.min(pos1.getBlockX(), pos2.getBlockX());
            int z1 = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
            int x2 = Math.max(pos1.getBlockX(), pos2.getBlockX());
            int z2 = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
            int area = (x2 - x1 + 1) * (z2 - z1 + 1);
            
            // Check if area is within limits
            if (area < configManager.getMinLandArea()) {
                logger.info("Land area " + area + " is below minimum " + configManager.getMinLandArea());
                return null;
            }
            
            if (area > configManager.getMaxLandArea()) {
                logger.info("Land area " + area + " exceeds maximum " + configManager.getMaxLandArea());
                return null;
            }

            // Check for overlapping lands
            List<Land> overlappingLands = landDAO.getOverlappingLands(
                pos1.getWorld().getName(), x1, z1, x2, z2);
            
            if (!overlappingLands.isEmpty()) {
                logger.info("Land overlaps with existing lands");
                return null;
            }

            // Create the land
            Land land = new Land(pos1.getWorld().getName(), x1, z1, x2, z2, dbPlayer.getId());
            landDAO.createLand(land);
            
            // Invalidate cache for the affected area
            landCache.invalidateCacheInArea(land.getWorld(), land.getX1(), land.getZ1(), land.getX2(), land.getZ2());
            
            logger.info("Created land " + land.getId() + " for player " + creator.getName());
            return land;
            
        } catch (SQLException e) {
            logger.severe("Failed to create land: " + e.getMessage());
            return null;
        }
    }

    /**
     * Claims an unowned land.
     *
     * @param player The player claiming the land
     * @param landId The ID of the land to claim
     * @return true if claiming was successful, false otherwise
     */
    public boolean claimLand(org.bukkit.entity.Player player, String landId) {
        try {
            // Parse land ID
            int id;
            try {
                id = Integer.parseInt(landId);
            } catch (NumberFormatException e) {
                logger.info("Invalid land ID: " + landId);
                return false;
            }

            // Get the land
            Optional<Land> landOpt = landDAO.getLandById(id);
            if (!landOpt.isPresent()) {
                logger.info("Land not found: " + landId);
                return false;
            }

            Land land = landOpt.get();
            
            // Check if land is already owned
            if (land.getOwnerId() != 0) {
                logger.info("Land is already owned: " + landId);
                return false;
            }

            // Get or create player in database
            io.github.railgun19457.easyland.model.Player dbPlayer = playerDAO.getOrCreatePlayer(
                player.getUniqueId(), player.getName());
            
            // Check player's land count
            int landCount = landDAO.getLandCountByOwner(dbPlayer.getId());
            if (landCount >= configManager.getMaxLandsPerPlayer()) {
                logger.info("Player " + player.getName() + " has reached maximum land count");
                return false;
            }

            // Claim the land
            land.setOwnerId(dbPlayer.getId());
            landDAO.updateLand(land);
            
            // Invalidate cache for the affected land
            landCache.invalidateLandCache(land.getId());
            
            logger.info("Player " + player.getName() + " claimed land " + landId);
            return true;
            
        } catch (SQLException e) {
            logger.severe("Failed to claim land: " + e.getMessage());
            return false;
        }
    }

    /**
     * Deletes a land.
     *
     * @param player The player deleting the land
     * @param landId The ID of the land to delete
     * @return true if deletion was successful, false otherwise
     */
    public boolean deleteLand(org.bukkit.entity.Player player, String landId) {
        try {
            // 使用辅助方法验证领地所有权
            Land land = getAndVerifyLandOwner(player, landId);
            if (land == null) {
                return false;
            }

            // 删除领地
            landDAO.deleteLand(land.getId());
            
            // 使受影响区域的缓存失效
            landCache.invalidateCacheInArea(land.getWorld(), land.getX1(), land.getZ1(), land.getX2(), land.getZ2());
            
            logger.info("Player " + player.getName() + " deleted land " + landId);
            return true;
            
        } catch (LandNotFoundException e) {
            logger.info("Land not found for deletion: " + e.getLandId());
            return false;
        } catch (SQLException e) {
            logger.severe("Failed to delete land: " + e.getMessage());
            return false;
        }
    }

    /**
     * Abandons a land, making it unowned.
     *
     * @param player The player abandoning the land
     * @param landId The ID of the land to abandon
     * @return true if abandoning was successful, false otherwise
     */
    public boolean abandonLand(org.bukkit.entity.Player player, String landId) {
        try {
            // 使用辅助方法验证领地所有权
            Land land = getAndVerifyLandOwner(player, landId);
            if (land == null) {
                return false;
            }

            // 放弃领地，将所有者设置为0
            land.setOwnerId(0);
            landDAO.updateLand(land);
            
            // 使受影响的领地缓存失效
            landCache.invalidateLandCache(land.getId());
            
            logger.info("Player " + player.getName() + " abandoned land " + landId);
            return true;
            
        } catch (LandNotFoundException e) {
            logger.info("Land not found for abandonment: " + e.getLandId());
            return false;
        } catch (SQLException e) {
            logger.severe("Failed to abandon land: " + e.getMessage());
            return false;
        }
    }

    /**
     * Renames a land.
     *
     * @param player  The player renaming the land
     * @param landId  The ID of the land to rename
     * @param newName The new name for the land
     * @return true if renaming was successful, false otherwise
     */
    public boolean renameLand(org.bukkit.entity.Player player, String landId, String newName) {
        try {
            // 使用辅助方法验证领地所有权或管理员权限
            Land land = getAndVerifyLandOwnerOrAdmin(player, landId);
            if (land == null) {
                return false;
            }

            // 重命名领地
            land.setName(newName);
            landDAO.updateLand(land);
            
            // 使受影响的领地缓存失效
            landCache.invalidateLandCache(land.getId());
            
            logger.info("Player " + player.getName() + " renamed land " + landId + " to " + newName);
            return true;
            
        } catch (LandNotFoundException e) {
            logger.info("Land not found for renaming: " + e.getLandId());
            return false;
        } catch (SQLException e) {
            logger.severe("Failed to rename land: " + e.getMessage());
            return false;
        }
    }

    /**
     * Lists all lands in the database (for admins).
     *
     * @param page The page number (1-based)
     * @return A list of all lands
     */
    public List<Land> listAllLands(int page) {
        try {
            // Get all lands
            List<Land> allLands = landDAO.getAllLands();
            
            // Apply pagination
            int startIndex = (page - 1) * configManager.getListPerPage();
            int endIndex = Math.min(startIndex + configManager.getListPerPage(), allLands.size());
            
            if (startIndex >= allLands.size()) {
                return List.of();
            }
            
            return allLands.subList(startIndex, endIndex);
            
        } catch (SQLException e) {
            logger.severe("Failed to list all lands: " + e.getMessage());
            return List.of();
        }
    }
    
    /**
     * Lists all lands owned by a player.
     *
     * @param playerUuid The UUID of the player
     * @param page       The page number (1-based)
     * @return A list of lands owned by the player
     */
    public List<Land> listPlayerLands(UUID playerUuid, int page) {
        try {
            // Get player from database
            Optional<io.github.railgun19457.easyland.model.Player> playerOpt = playerDAO.getPlayerByUuid(playerUuid);
            if (!playerOpt.isPresent()) {
                logger.info("Player not found in database: " + playerUuid);
                return List.of();
            }

            io.github.railgun19457.easyland.model.Player dbPlayer = playerOpt.get();
            
            // Get all lands owned by the player
            List<Land> allLands = landDAO.getLandsByOwner(dbPlayer.getId());
            
            // Apply pagination
            int startIndex = (page - 1) * configManager.getListPerPage();
            int endIndex = Math.min(startIndex + configManager.getListPerPage(), allLands.size());
            
            if (startIndex >= allLands.size()) {
                return List.of();
            }
            
            return allLands.subList(startIndex, endIndex);
            
        } catch (SQLException e) {
            logger.severe("Failed to list player lands: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Trusts a player on a land.
     *
     * @param owner      The land owner
     * @param landId     The ID of the land
     * @param targetPlayer The player to trust
     * @return true if trusting was successful, false otherwise
     */
    public boolean trustPlayer(org.bukkit.entity.Player owner, String landId, org.bukkit.entity.Player targetPlayer) {
        try {
            // 使用辅助方法验证领地所有权
            Land land = getAndVerifyLandOwner(owner, landId);
            if (land == null) {
                return false;
            }

            // 获取或创建目标玩家
            io.github.railgun19457.easyland.model.Player dbTargetPlayer = playerDAO.getOrCreatePlayer(
                targetPlayer.getUniqueId(), targetPlayer.getName());
            
            // 创建信任关系
            LandTrust landTrust = new LandTrust(land.getId(), dbTargetPlayer.getId());
            landTrustDAO.createLandTrust(landTrust);
            
            // 使受影响的领地缓存失效
            landCache.invalidateLandCache(land.getId());
            
            logger.info("Player " + owner.getName() + " trusted " + targetPlayer.getName() + " on land " + landId);
            return true;
            
        } catch (LandNotFoundException e) {
            logger.info("Land not found for trusting: " + e.getLandId());
            return false;
        } catch (SQLException e) {
            logger.severe("Failed to trust player: " + e.getMessage());
            return false;
        }
    }

    /**
     * Untrusts a player from a land.
     *
     * @param owner      The land owner
     * @param landId     The ID of the land
     * @param targetPlayer The player to untrust
     * @return true if untrusting was successful, false otherwise
     */
    public boolean untrustPlayer(org.bukkit.entity.Player owner, String landId, org.bukkit.entity.Player targetPlayer) {
        try {
            // 使用辅助方法验证领地所有权
            Land land = getAndVerifyLandOwner(owner, landId);
            if (land == null) {
                return false;
            }

            // 获取目标玩家
            Optional<io.github.railgun19457.easyland.model.Player> dbTargetPlayerOpt = playerDAO.getPlayerByUuid(
                targetPlayer.getUniqueId());
            if (!dbTargetPlayerOpt.isPresent()) {
                logger.info("Target player not found in database: " + targetPlayer.getName());
                return false;
            }

            io.github.railgun19457.easyland.model.Player dbTargetPlayer = dbTargetPlayerOpt.get();
            
            // 删除信任关系
            landTrustDAO.deleteLandTrust(land.getId(), dbTargetPlayer.getId());
            
            // 使受影响的领地缓存失效
            landCache.invalidateLandCache(land.getId());
            
            logger.info("Player " + owner.getName() + " untrusted " + targetPlayer.getName() + " from land " + landId);
            return true;
            
        } catch (LandNotFoundException e) {
            logger.info("Land not found for untrusting: " + e.getLandId());
            return false;
        } catch (SQLException e) {
            logger.severe("Failed to untrust player: " + e.getMessage());
            return false;
        }
    }

    /**
     * Lists all trusted players on a land.
     *
     * @param landId The ID of the land
     * @return A list of trusted players
     */
    public List<io.github.railgun19457.easyland.model.Player> listTrustedPlayers(String landId) {
        try {
            // Parse land ID
            int id;
            try {
                id = Integer.parseInt(landId);
            } catch (NumberFormatException e) {
                logger.info("Invalid land ID: " + landId);
                return List.of();
            }

            // Get the land
            Optional<Land> landOpt = landDAO.getLandById(id);
            if (!landOpt.isPresent()) {
                logger.info("Land not found: " + landId);
                return List.of();
            }

            // Get all trusts for the land
            List<LandTrust> trusts = landTrustDAO.getTrustsByLand(id);
            
            // Get player details for each trust
            return trusts.stream()
                .map(trust -> {
                    try {
                        return playerDAO.getPlayerById(trust.getPlayerId());
                    } catch (SQLException e) {
                        logger.warning("Failed to get player " + trust.getPlayerId() + ": " + e.getMessage());
                        return Optional.<io.github.railgun19457.easyland.model.Player>empty();
                    }
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
            
        } catch (SQLException e) {
            logger.severe("Failed to list trusted players: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Creates a sub-claim within a parent land.
     *
     * @param owner       The land owner
     * @param parentLandId The ID of the parent land
     * @param pos1        The first corner position
     * @param pos2        The second corner position
     * @return The created sub-claim, or null if creation failed
     */
    public Land createSubClaim(org.bukkit.entity.Player owner, String parentLandId, Location pos1, Location pos2) {
        try {
            // Parse parent land ID
            int parentId;
            try {
                parentId = Integer.parseInt(parentLandId);
            } catch (NumberFormatException e) {
                logger.info("Invalid parent land ID: " + parentLandId);
                return null;
            }

            // Get the parent land
            Optional<Land> parentLandOpt = landDAO.getLandById(parentId);
            if (!parentLandOpt.isPresent()) {
                logger.info("Parent land not found: " + parentLandId);
                return null;
            }

            Land parentLand = parentLandOpt.get();
            
            // Get owner from database
            Optional<io.github.railgun19457.easyland.model.Player> dbPlayerOpt = playerDAO.getPlayerByUuid(
                owner.getUniqueId());
            if (!dbPlayerOpt.isPresent()) {
                logger.info("Player not found in database: " + owner.getName());
                return null;
            }

            io.github.railgun19457.easyland.model.Player dbPlayer = dbPlayerOpt.get();
            
            // Check if player owns the parent land
            if (parentLand.getOwnerId() != dbPlayer.getId()) {
                logger.info("Player does not own parent land: " + parentLandId);
                return null;
            }

            // Check sub-claim limits
            if (!checkSubClaimLimits(parentLand)) {
                logger.info("Sub-claim limits exceeded for parent land: " + parentLandId);
                return null;
            }

            // Validate positions
            if (!pos1.getWorld().equals(pos2.getWorld()) || !pos1.getWorld().getName().equals(parentLand.getWorld())) {
                logger.warning("Positions must be in the same world as the parent land");
                return null;
            }

            // Calculate sub-claim coordinates
            int x1 = Math.min(pos1.getBlockX(), pos2.getBlockX());
            int z1 = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
            int x2 = Math.max(pos1.getBlockX(), pos2.getBlockX());
            int z2 = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
            
            // Check if sub-claim is within parent land boundaries
            if (x1 < parentLand.getX1() || x2 > parentLand.getX2() ||
                z1 < parentLand.getZ1() || z2 > parentLand.getZ2()) {
                logger.info("Sub-claim is not within parent land boundaries");
                return null;
            }

            // Check for overlapping lands (including other sub-claims)
            List<Land> overlappingLands = landDAO.getOverlappingLands(
                pos1.getWorld().getName(), x1, z1, x2, z2);
            
            if (!overlappingLands.isEmpty()) {
                logger.info("Sub-claim overlaps with existing lands");
                return null;
            }

            // Create the sub-claim
            Land subClaim = new Land(pos1.getWorld().getName(), x1, z1, x2, z2, dbPlayer.getId());
            subClaim.setParentLandId(parentId);
            landDAO.createLand(subClaim);
            
            // Invalidate cache for the affected area
            landCache.invalidateCacheInArea(subClaim.getWorld(), subClaim.getX1(), subClaim.getZ1(), subClaim.getX2(), subClaim.getZ2());
            
            logger.info("Created sub-claim " + subClaim.getId() + " for player " + owner.getName() + " under parent land " + parentLandId);
            return subClaim;
            
        } catch (SQLException e) {
            logger.severe("Failed to create sub-claim: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 检查子领地限制是否满足。
     *
     * @param parentLand 父领地
     * @return 如果限制满足返回 true，否则返回 false
     */
    private boolean checkSubClaimLimits(Land parentLand) throws SQLException {
        // 检查每个父领地允许的最大子领地数量
        List<Land> existingSubClaims = landDAO.getSubLands(parentLand.getId());
        if (existingSubClaims.size() >= configManager.getMaxSubClaimsPerLand()) {
            logger.info("Parent land " + parentLand.getId() + " has reached maximum sub-claim limit of " +
                configManager.getMaxSubClaimsPerLand());
            return false;
        }
        
        // 检查子领地嵌套层级
        int currentDepth = getSubClaimDepth(parentLand);
        if (currentDepth >= configManager.getMaxSubClaimDepth()) {
            logger.info("Parent land " + parentLand.getId() + " has reached maximum sub-claim depth of " +
                configManager.getMaxSubClaimDepth());
            return false;
        }
        
        return true;
    }
    
    /**
     * 获取领地的子领地嵌套深度。
     *
     * @param land 要检查的领地
     * @return 嵌套深度
     */
    private int getSubClaimDepth(Land land) throws SQLException {
        if (land.getParentLandId() == null) {
            return 0; // 顶级领地
        }
        
        int depth = 1;
        Integer currentParentId = land.getParentLandId();
        
        while (currentParentId != null) {
            Optional<Land> parentLandOpt = landDAO.getLandById(currentParentId);
            if (!parentLandOpt.isPresent()) {
                break; // 领地链中断
            }
            
            Land parentLand = parentLandOpt.get();
            if (parentLand.getParentLandId() == null) {
                break; // 到达顶级领地
            }
            
            depth++;
            currentParentId = parentLand.getParentLandId();
            
            // 防止无限循环
            if (depth > configManager.getMaxSubClaimDepth() * 2) {
                logger.warning("Detected potential infinite loop in sub-claim hierarchy for land " + land.getId());
                break;
            }
        }
        
        return depth;
    }

    /**
     * Checks if a player has permission to perform an action on a land.
     *
     * @param player The player to check
     * @param land   The land to check
     * @param action The action to check
     * @return true if the player has permission, false otherwise
     */
    public boolean hasPermission(org.bukkit.entity.Player player, Land land, String action) {
        return permissionManager.hasLandPermission(player, land, action);
    }

    /**
     * Gets the land at a specific location.
     *
     * @param location The location to check
     * @return The land at the location, or null if no land exists
     */
    public Land getLandAt(Location location) {
        // Use cache to get land at location
        return landCache.getLandAt(location);
    }
    
    /**
     * Gets a land by its ID.
     *
     * @param id The ID of the land
     * @return An Optional containing the land if found, otherwise empty
     */
    public Optional<Land> getLandById(int id) {
        try {
            return landDAO.getLandById(id);
        } catch (SQLException e) {
            logger.severe("Failed to get land by ID: " + e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Gets all lands in the database.
     *
     * @return A list of all lands
     */
    public List<Land> getAllLands() {
        try {
            return landDAO.getAllLands();
        } catch (SQLException e) {
            logger.severe("Failed to get all lands: " + e.getMessage());
            return List.of();
        }
    }
    
    /**
     * Gets all sub-lands of a parent land.
     *
     * @param parentLandId The ID of the parent land
     * @return A list of sub-lands
     */
    public List<Land> getSubLands(int parentLandId) {
        try {
            return landDAO.getSubLands(parentLandId);
        } catch (SQLException e) {
            logger.severe("Failed to get sub-lands: " + e.getMessage());
            return List.of();
        }
    }
    
    /**
     * Checks if a land is a sub-land of another land.
     *
     * @param landId The ID of the land to check
     * @param parentLandId The ID of the potential parent land
     * @return true if the land is a sub-land of the parent land, false otherwise
     */
    public boolean isSubLandOf(int landId, int parentLandId) {
        try {
            Optional<Land> landOpt = landDAO.getLandById(landId);
            if (landOpt.isPresent()) {
                Land land = landOpt.get();
                return parentLandId == land.getParentLandId();
            }
            return false;
        } catch (SQLException e) {
            logger.severe("Failed to check sub-land relationship: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取并验证玩家是否为领地的所有者。
     *
     * @param player 要验证的玩家
     * @param landId 领地ID
     * @return 如果验证通过返回领地对象
     * @throws LandNotFoundException 如果领地不存在
     */
    private Land getAndVerifyLandOwner(org.bukkit.entity.Player player, String landId) throws LandNotFoundException {
        try {
            // 解析领地ID
            int id;
            try {
                id = Integer.parseInt(landId);
            } catch (NumberFormatException e) {
                logger.info("Invalid land ID: " + landId);
                throw new LandNotFoundException(landId);
            }

            // 获取领地
            Optional<Land> landOpt = landDAO.getLandById(id);
            if (!landOpt.isPresent()) {
                logger.info("Land not found: " + landId);
                throw new LandNotFoundException(landId);
            }

            Land land = landOpt.get();
            
            // 从数据库获取玩家
            Optional<io.github.railgun19457.easyland.model.Player> dbPlayerOpt = playerDAO.getPlayerByUuid(
                player.getUniqueId());
            if (!dbPlayerOpt.isPresent()) {
                logger.info("Player not found in database: " + player.getName());
                return null;
            }

            io.github.railgun19457.easyland.model.Player dbPlayer = dbPlayerOpt.get();
            
            // 检查玩家是否拥有该领地
            if (land.getOwnerId() != dbPlayer.getId()) {
                logger.info("Player does not own land: " + landId);
                return null;
            }
            
            return land;
            
        } catch (SQLException e) {
            logger.severe("Failed to verify land owner: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 获取并验证玩家是否为领地的所有者或管理员。
     *
     * @param player 要验证的玩家
     * @param landId 领地ID
     * @return 如果验证通过返回领地对象
     * @throws LandNotFoundException 如果领地不存在
     */
    private Land getAndVerifyLandOwnerOrAdmin(org.bukkit.entity.Player player, String landId) throws LandNotFoundException {
        try {
            // 解析领地ID
            int id;
            try {
                id = Integer.parseInt(landId);
            } catch (NumberFormatException e) {
                logger.info("Invalid land ID: " + landId);
                throw new LandNotFoundException(landId);
            }

            // 获取领地
            Optional<Land> landOpt = landDAO.getLandById(id);
            if (!landOpt.isPresent()) {
                logger.info("Land not found: " + landId);
                throw new LandNotFoundException(landId);
            }

            Land land = landOpt.get();
            
            // 使用PermissionManager检查是否可以管理领地
            if (!permissionManager.canManageLand(player, land)) {
                logger.info("Player does not own land and is not admin: " + landId);
                return null;
            }
            
            return land;
            
        } catch (SQLException e) {
            logger.severe("Failed to verify land owner or admin: " + e.getMessage());
            return null;
        }
    }
}