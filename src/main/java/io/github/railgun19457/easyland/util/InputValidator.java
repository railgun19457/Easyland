package io.github.railgun19457.easyland.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * 输入验证工具类
 * 提供统一的输入验证方法，增强安全性
 */
public class InputValidator {
    private static final Logger logger = Logger.getLogger(InputValidator.class.getName());
    
    // Player names: letters, numbers, and underscores, length 3-16.
    // This is a common pattern for Minecraft usernames.
    private static final Pattern PLAYER_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,16}$");

    // Land IDs: alphanumeric characters and underscores, length 1-32.
    private static final Pattern LAND_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{1,32}$");

    // UUID pattern
    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    // Pattern for unexpected control characters that shouldn't be in simple inputs.
    // This helps prevent issues like log injection or unexpected behavior in string formatting.
    private static final Pattern CONTROL_CHARS_PATTERN = Pattern.compile("[\\p{Cntrl}&&[^\\t\\n\\r]]");
    
    /**
     * 验证玩家名格式
     * @param playerName 玩家名
     * @return 验证结果
     */
    public static ValidationResult validatePlayerName(String playerName) {
        if (playerName == null || playerName.isBlank()) {
            return new ValidationResult.Failure("Player name cannot be empty.");
        }

        if (!PLAYER_NAME_PATTERN.matcher(playerName).matches()) {
            return new ValidationResult.Failure("Player name must be 3-16 characters long and contain only letters, numbers, and underscores.");
        }

        if (CONTROL_CHARS_PATTERN.matcher(playerName).find()) {
            logger.warning("Player name contains unexpected control characters: " + playerName);
            return new ValidationResult.Failure("Player name contains invalid characters.");
        }

        return new ValidationResult.Success();
    }
    
    /**
     * 验证领地ID格式
     * @param landId 领地ID
     * @return 验证结果
     */
    public static ValidationResult validateLandId(String landId) {
        if (landId == null || landId.isBlank()) {
            return new ValidationResult.Failure("Land ID cannot be empty.");
        }

        if (!LAND_ID_PATTERN.matcher(landId).matches()) {
            return new ValidationResult.Failure("Land ID must be 1-32 characters long and can only contain letters, numbers, and underscores.");
        }

        if (CONTROL_CHARS_PATTERN.matcher(landId).find()) {
            logger.warning("Land ID contains unexpected control characters: " + landId);
            return new ValidationResult.Failure("Land ID contains invalid characters.");
        }

        return new ValidationResult.Success();
    }
    
    /**
     * 验证UUID格式
     * @param uuid UUID字符串
     * @return 验证结果
     */
    public static ValidationResult validateUUID(String uuid) {
        if (uuid == null || uuid.isBlank()) {
            return new ValidationResult.Failure("UUID cannot be empty.");
        }

        if (!UUID_PATTERN.matcher(uuid).matches()) {
            return new ValidationResult.Failure("Invalid UUID format.");
        }

        try {
            UUID.fromString(uuid);
            return new ValidationResult.Success();
        } catch (IllegalArgumentException e) {
            return new ValidationResult.Failure("Invalid UUID format.");
        }
    }
    
    /**
     * 验证玩家是否存在
     * @param playerName 玩家名
     * @return 验证结果
     */
    public static ValidationResult validatePlayerExists(String playerName) {
        ValidationResult nameValidation = validatePlayerName(playerName);
        if (nameValidation instanceof ValidationResult.Failure failure) {
            return failure;
        }

        Player player = Bukkit.getPlayer(playerName);
        if (player == null) {
            return new ValidationResult.Failure("Player not found or not online.");
        }

        return new ValidationResult.Success();
    }
    
    /**
     * 验证布尔值字符串
     * @param value 布尔值字符串
     * @return 验证结果
     */
    public static ValidationResult validateBoolean(String value) {
        if (value == null || value.isBlank()) {
            return new ValidationResult.Failure("Value cannot be empty.");
        }

        return switch (value.toLowerCase()) {
            case "true", "false", "on", "off", "enable", "disable" -> new ValidationResult.Success();
            default -> new ValidationResult.Failure("Value must be one of true/false, on/off, or enable/disable.");
        };
    }
    
    /**
     * 解析布尔值字符串
     * @param value 布尔值字符串
     * @return 解析结果
     */
    public static boolean parseBoolean(String value) {
        if (value == null) return false;
        String lowerValue = value.toLowerCase();
        return lowerValue.equals("true") || lowerValue.equals("on") || lowerValue.equals("enable");
    }
    
    /**
    /**
     * Validates that a string is safe to be used in SQL queries.
     * While PreparedStatement should be used to prevent SQL injection,
     * this provides an extra layer of defense against unexpected characters.
     * @param sqlParam The string to validate
     * @return The validation result
     */
    public static ValidationResult validateSqlSafe(String sqlParam) {
        if (sqlParam == null) {
            return new ValidationResult.Success(); // Null is safe for PreparedStatement
        }
        if (CONTROL_CHARS_PATTERN.matcher(sqlParam).find()) {
            logger.warning("Potential unsafe SQL parameter detected (contains control characters): " + sqlParam);
            return new ValidationResult.Failure("Input contains invalid characters.");
        }
        return new ValidationResult.Success();
    }
}