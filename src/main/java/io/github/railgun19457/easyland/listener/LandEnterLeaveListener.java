package io.github.railgun19457.easyland.listener;

import io.github.railgun19457.easyland.EasyLand;
import io.github.railgun19457.easyland.I18nManager;
import io.github.railgun19457.easyland.core.LandManager;
import io.github.railgun19457.easyland.model.Land;
import io.github.railgun19457.easyland.model.Player;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Optional;

public class LandEnterLeaveListener implements Listener {

    private final EasyLand plugin;
    private final LandManager landManager;
    private final I18nManager i18nManager;

    public LandEnterLeaveListener(EasyLand plugin) {
        this.plugin = plugin;
        this.landManager = plugin.getLandManager();
        this.i18nManager = plugin.getI18nManager();
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        handleMove(event.getPlayer(), event.getFrom(), event.getTo());
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // 优化性能：只在方块变化时检查
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        
        handleMove(event.getPlayer(), event.getFrom(), event.getTo());
    }

    private void handleMove(org.bukkit.entity.Player player, org.bukkit.Location from, org.bukkit.Location to) {
        Land fromLand = landManager.getLandAt(from);
        Land toLand = landManager.getLandAt(to);

        // 情况1：从野外进入领地
        if (fromLand == null && toLand != null) {
            sendEnterNotification(player, toLand);
        }
        // 情况2：从领地进入野外
        else if (fromLand != null && toLand == null) {
            sendLeaveNotification(player, fromLand);
        }
        // 情况3：从一个领地进入另一个领地
        else if (fromLand != null && toLand != null && fromLand.getId() != toLand.getId()) {
            // 显示进入新领地
            sendEnterNotification(player, toLand);
        }
    }

    private void sendEnterNotification(org.bukkit.entity.Player player, Land land) {
        String landName = land.getName() != null ? land.getName() : String.valueOf(land.getId());
        String ownerName = getOwnerName(land.getOwnerId());
        String message = i18nManager.getMessage("notification.enter-land", landName, ownerName);
        player.sendActionBar(Component.text(message));
    }

    private void sendLeaveNotification(org.bukkit.entity.Player player, Land land) {
        String landName = land.getName() != null ? land.getName() : String.valueOf(land.getId());
        String message = i18nManager.getMessage("notification.leave-land", landName);
        player.sendActionBar(Component.text(message));
    }
    
    private String getOwnerName(int ownerId) {
        if (ownerId == 0) return "无";
        try {
            Optional<Player> playerOpt = plugin.getPlayerDAO().getPlayerById(ownerId);
            return playerOpt.map(Player::getName).orElse("未知");
        } catch (Exception e) {
            return "未知";
        }
    }
}
