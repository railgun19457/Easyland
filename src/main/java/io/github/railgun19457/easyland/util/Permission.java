package io.github.railgun19457.easyland.util;

import org.jetbrains.annotations.NotNull;

public sealed interface Permission {
    @NotNull String getNode();

    record PlayerPermission(@NotNull String node) implements Permission {
        public static final PlayerPermission CREATE = new PlayerPermission("easyland.create");
        public static final PlayerPermission CLAIM = new PlayerPermission("easyland.claim");
        public static final PlayerPermission UNCLAIM = new PlayerPermission("easyland.unclaim");
        public static final PlayerPermission REMOVE = new PlayerPermission("easyland.remove");
        public static final PlayerPermission LIST = new PlayerPermission("easyland.list");
        public static final PlayerPermission SHOW = new PlayerPermission("easyland.show");
        public static final PlayerPermission TRUST = new PlayerPermission("easyland.trust");
        public static final PlayerPermission UNTRUST = new PlayerPermission("easyland.untrust");
        public static final PlayerPermission TRUST_LIST = new PlayerPermission("easyland.trustlist");
        public static final PlayerPermission RULE = new PlayerPermission("easyland.rule");
        public static final PlayerPermission SELECT = new PlayerPermission("easyland.select");
        public static final PlayerPermission HELP = new PlayerPermission("easyland.help");

        @Override
        public @NotNull String getNode() {
            return node;
        }
    }

    record AdminPermission(@NotNull String node) implements Permission {
        public static final AdminPermission RELOAD = new AdminPermission("easyland.admin.reload");
        public static final AdminPermission CACHE = new AdminPermission("easyland.admin.cache");

        @Override
        public @NotNull String getNode() {
            return node;
        }
    }
}