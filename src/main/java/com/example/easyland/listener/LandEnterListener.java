package com.example.easyland.listener;

import com.example.easyland.domain.Land;
import com.example.easyland.manager.LanguageManager;
import com.example.easyland.service.LandService;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LandEnterListener implements Listener {
    private final LandService landService;
    private final LanguageManager languageManager;
    private final Map<UUID, String> lastLandOwner = new ConcurrentHashMap<>();
    private final Map<String, String> ownerNameCache = new ConcurrentHashMap<>(); // 缓存UUID到玩家名的映射

    public LandEnterListener(LandService landService, LanguageManager languageManager) {
        this.landService = landService;
        this.languageManager = languageManager;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // 只在玩家跨区块移动时触发
        Chunk fromChunk = event.getFrom().getChunk();
        Chunk toChunk = event.getTo().getChunk();

        if (fromChunk.equals(toChunk)) {
            return; // 未跨区块，直接返回
        }

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // 获取目标和来源区块的领地
        Optional<Land> toLandOpt = landService.getLandByChunk(toChunk);
        Optional<Land> fromLandOpt = landService.getLandByChunk(fromChunk);

        // 获取领地主人UUID
        String toOwner = toLandOpt.map(Land::getOwner).orElse(null);
        String fromOwner = fromLandOpt.map(Land::getOwner).orElse(null);

        // 获取玩家上次所在领地的主人
        String lastOwner = lastLandOwner.get(playerId);

        // 检查是否进入了新的领地
        if (toOwner != null && !toOwner.equals(lastOwner)) {
            String ownerName = getOwnerName(toOwner);
            String landInfo = toLandOpt
                    .map(land -> (land.getLandId() != null && !land.getLandId().isEmpty())
                            ? " (ID: " + land.getLandId() + ")"
                            : "")
                    .orElse("");
            languageManager.sendMessage(player, "enter.message", ownerName, landInfo);
        }

        // 检查是否离开了领地
        if (fromOwner != null && !fromOwner.equals(toOwner)) {
            String ownerName = getOwnerName(fromOwner);
            String landInfo = fromLandOpt
                    .map(land -> (land.getLandId() != null && !land.getLandId().isEmpty())
                            ? " (ID: " + land.getLandId() + ")"
                            : "")
                    .orElse("");
            languageManager.sendMessage(player, "leave.message", ownerName, landInfo);
        }

        // 更新玩家当前所在领地的主人
        if (toOwner != null) {
            lastLandOwner.put(playerId, toOwner);
        } else {
            lastLandOwner.remove(playerId);
        }
    }

    /**
     * 获取玩家名称，使用缓存减少数据库查询
     */
    private String getOwnerName(String ownerUuid) {
        if (ownerUuid == null)
            return languageManager.getMessage("land.unknown-owner");

        return ownerNameCache.computeIfAbsent(ownerUuid, uuid -> {
            try {
                UUID playerUuid = UUID.fromString(uuid);
                String name = Bukkit.getOfflinePlayer(playerUuid).getName();
                return name != null ? name : languageManager.getMessage("land.unknown-player");
            } catch (IllegalArgumentException e) {
                return languageManager.getMessage("land.invalid-uuid");
            }
        });
    }

    /**
     * 清理断开连接玩家的缓存数据
     */
    public void cleanupPlayer(UUID playerId) {
        lastLandOwner.remove(playerId);
    }

    /**
     * 清理玩家名称缓存（定期调用以防止内存泄漏）
     */
    public void cleanupNameCache() {
        if (ownerNameCache.size() > 100) {
            ownerNameCache.clear(); // 简单的清理策略
        }
    }
}
