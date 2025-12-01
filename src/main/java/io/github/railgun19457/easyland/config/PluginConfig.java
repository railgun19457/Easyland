package io.github.railgun19457.easyland.config;

import java.util.Map;

/**
* 一个不可变的 record，用于持有插件的所有配置值。
*
* @param language 语言设置
* @param maxLandsPerPlayer 每个玩家的最大领地数量
* @param maxChunksPerLand 每个领地的最大区块数量
* @param landBoundaryParticle 领地边界粒子效果
* @param showDurationSeconds 显示边界的持续时间（秒）
* @param maxShowDurationSeconds 最大显示边界持续时间（秒）
* @param messageCooldownSeconds 进入/离开领地消息的冷却时间（秒）
* @param protectionRules 保护规则的配置
*/
public record PluginConfig(
    String language,
    int maxLandsPerPlayer,
    int maxChunksPerLand,
    String landBoundaryParticle,
    int showDurationSeconds,
    int maxShowDurationSeconds,
    int messageCooldownSeconds,
    Map<String, ProtectionRule> protectionRules
) {
    /**
    * 代表单个保护规则的配置。
    *
    * @param enable 是否在服务器级别启用此规则
    * @param defaultValue 创建新领地时此规则的默认状态
    */
    public record ProtectionRule(boolean enable, boolean defaultValue) {}
}