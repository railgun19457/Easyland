# Easyland 领地管理插件

![Version](https://img.shields.io/badge/version-1.3-blue)
![Minecraft](https://img.shields.io/badge/minecraft-1.21.4-green)
![Java](https://img.shields.io/badge/java-17-orange)

专为 PaperMC 1.21.4 开发的高性能领地管理插件，支持领地创建、权限管理和多种保护功能。

## ✨ 主要功能

- 🏗️ **领地管理**：创建、认领、删除、查看领地
- 👥 **信任系统**：玩家权限管理，支持预信任功能
- 🛡️ **保护系统**：方块、爆炸、容器、玩家保护
- ✨ **可视化**：粒子效果显示领地边界

## 📋 指令列表

主命令：`/easyland`（别名：`/el`, `/land`）

| 指令 | 说明 | 权限 |
|------|------|------|
| `select` | 获取选择工具 | easyland.select |
| `create [id]` | 创建领地 | easyland.create |
| `claim [id]` | 认领领地 | easyland.claim |
| `unclaim [id]` | 放弃领地 | easyland.unclaim |
| `trust <玩家>` | 信任玩家 | easyland.trust |
| `untrust <玩家>` | 取消信任 | easyland.untrust |
| `trustlist` | 查看信任列表 | easyland.trust |
| `show [id] [时间]` | 显示边界 | easyland.show |
| `list` | 查看领地列表 | easyland.list |
| `remove <id>` | 删除领地 | easyland.remove |
| `rule [规则] [on/off]` | 管理保护规则 | easyland.rule |

## 🔐 权限配置

### 基础权限
| 权限节点 | 说明 | 默认值 |
|----------|------|--------|
| easyland.select | 选择工具 | op |
| easyland.create | 创建领地 | op |
| easyland.claim | 认领领地 | true |
| easyland.unclaim | 放弃领地 | true |
| easyland.trust | 信任管理 | true |
| easyland.untrust | 取消信任 | true |
| easyland.show | 显示边界 | true |
| easyland.list | 查看列表 | true |
| easyland.rule | 保护规则 | true |
| easyland.remove | 删除领地 | op |
| easyland.admin | 管理员权限组 | op |
| easyland.bypass | 绕过保护 | op |

## ⚙️ 主要配置

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `max-lands-per-player` | 每人最大领地数 | 5 |
| `max-chunks-per-land` | 单领地最大区块数 | 256 |
| `show-duration-seconds` | 默认显示时间 | 10 |
| `message-cooldown-seconds` | 消息冷却时间 | 3 |
| `land-boundary-particle` | 边界粒子类型 | firework |

### 保护规则配置
每个保护规则都有两个设置项：
- **enable**: 服务器是否允许启用此规则
- **default**: 新创建领地的默认开启状态

| 配置路径| 保护规则 | 功能说明  |
|----------|----------|----------|
| `protection.block-protection` | 方块保护 | 防止破坏/放置方块 |
| `protection.explosion-protection` | 爆炸保护 | 防止爆炸破坏方块 |
| `protection.container-protection` | 容器保护 | 防止访问箱子等容器 |
| `protection.player-protection` | 玩家保护 | 保护玩家免受伤害 |

## 📖 快速开始

1. **获取工具**：`/easyland select`
2. **选择区域**：右键点击两个区块
3. **创建领地**：`/easyland create [ID]`
4. **认领领地**：`/easyland claim [ID]`
5. **信任玩家**：`/easyland trust <玩家名>`
6. **设置保护**：`/easyland rule <规则名> on`

## 📦 安装

1. 下载 `easyland-1.3.jar`
2. 放入 `plugins` 目录
3. 重启服务器
4. 配置权限节点

## 📝 更新日志

### V1.3.1 (最新版)
- 空间索引优化：查找性能提升 60-80%
- 并发安全：全面使用 ConcurrentHashMap
- 内存优化：减少 20-30% 内存使用
- 代码重构：移除硬编码，提升可维护性

### V1.3
- 革命性领地保护系统重构
- 新增 `/easyland rule` 指令系统
- 保护规则按领地独立存储
- 配置文件结构升级

### V1.2
- 新增配置文件管理系统
- 新增管理员权限组
- 优化 show 指令，支持自定义时间

### V1.1
- 优化信任系统，支持预信任
- 新增 trustlist 指令

### V1.0
- 基础功能实现


---

💝 **感谢使用 Easyland！**
