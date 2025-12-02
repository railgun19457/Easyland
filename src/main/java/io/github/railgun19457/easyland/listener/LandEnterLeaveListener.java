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
    public void onPlayerMove(PlayerMoveEvent event) {
        // 优化性能：只在方块变化时检查
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Land fromLand = landManager.getLandAt(event.getFrom());
        Land toLand = landManager.getLandAt(event.getTo());

        // 情况1：从野外进入领地
        if (fromLand == null && toLand != null) {
            sendEnterNotification(event.getPlayer(), toLand);
        }
        // 情况2：从领地进入野外
        else if (fromLand != null && toLand == null) {
            sendLeaveNotification(event.getPlayer(), fromLand);
        }
        // 情况3：从一个领地进入另一个领地
        else if (fromLand != null && toLand != null && fromLand.getId() != toLand.getId()) {
            // 显示进入新领地
            sendEnterNotification(event.getPlayer(), toLand);
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
