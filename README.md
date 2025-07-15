# Easyland

一个功能完整的领地插件，支持区块选择、领地创建、认领、玩家信任、显示范围等功能，专为 PaperMC 1.21.4 开发。

## 功能特性

- **完整的领地管理系统**：创建、认领、删除领地
- **灵活的权限信任系统**：支持预信任从未加入服务器的玩家
- **模块化保护系统**：可独立控制方块、爆炸、容器、玩家保护，内置消息冷却防止刷屏
- **直观的领地可视化**：粒子效果显示领地边界，支持自定义显示时间
- **智能配置管理**：自动验证配置值，清理无效配置项
- **友好的用户界面**：完整的Tab补全和权限控制

## 指令说明

主命令：`/easyland`（别名：`/el`, `/land`）

| 子命令                | 用法及参数                                 | 说明                     | 所需权限              |
|----------------------|------------------------------------------|------------------------|----------------------|
| select               | /easyland select                          | 获取领地选择工具         | easyland.select      |
| create [id]          | /easyland create [自定义ID]               | 创建无主领地             | easyland.create      |
| claim [id]           | /easyland claim [ID]                      | 认领无主领地             | easyland.claim       |
| unclaim [id]         | /easyland unclaim [ID]                    | 放弃已认领领地           | easyland.unclaim     |
| trust <玩家名>        | /easyland trust <玩家名>                   | 信任玩家进入领地         | easyland.trust       |
| untrust <玩家名>      | /easyland untrust <玩家名>                 | 取消信任玩家             | easyland.untrust     |
| trustlist            | /easyland trustlist                       | 查看自己领地的信任玩家列表| easyland.trust       |
| show [id] [时间]      | /easyland show [ID] [时间(秒)]             | 显示领地范围             | easyland.show        |
| list                 | /easyland list                            | 查看所有领地列表         | easyland.list        |
| remove <id>          | /easyland remove <ID>                     | 删除指定ID的领地         | easyland.remove      |

## 权限节点

| 权限节点           | 作用描述                       | 默认值   |
|-------------------|------------------------------|--------|
| easyland.select   | 选择领地工具                  | op     |
| easyland.create   | 创建无主领地                  | op     |
| easyland.claim    | 认领领地                      | true   |
| easyland.unclaim  | 放弃领地                      | true   |
| easyland.trust    | 信任他人、查看信任列表         | true   |
| easyland.untrust  | 取消信任                      | true   |
| easyland.show     | 显示领地范围                  | true   |
| easyland.list     | 查看领地列表                  | true   |
| easyland.remove   | 删除领地                      | op     |
| easyland.admin    | 管理员权限组                  | op     |
| easyland.bypass   | 绕过领地保护                  | op     |

> **权限组说明**：`easyland.admin` 权限组包含了 `easyland.remove` 和 `easyland.bypass` 权限，方便管理员统一分配管理权限。

## 配置项

- `max-lands-per-player`：每名玩家最大可拥有领地数，默认5
- `max-chunks-per-land`：单个领地最大区块数，默认256
- `land-boundary-particle`：显示领地边界的粒子类型，默认FLAME
- `show-duration-seconds`：显示领地范围的默认秒数，默认10
- `max-show-duration-seconds`：显示领地范围的最大秒数，默认300

### 领地保护规则配置
- `protection.block-protection`：方块保护（包含破坏、放置方块，倒桶等），默认为`true`
- `protection.explosion-protection`：爆炸保护（保护领地不被爆炸破坏），默认为`true`
- `protection.container-protection`：容器保护（保护箱子、熔炉等容器不被他人访问），默认为`true`
- `protection.player-protection`：玩家保护（领主和受信任的玩家在领地内不会受到伤害），默认为`false`

### 消息与提示配置
- `message-cooldown-seconds`：保护提示消息的冷却时间（秒），范围0-60，默认3秒。设为0可禁用冷却，但可能导致消息刷屏



## 使用说明

### 基本流程
1. 使用 `/easyland select` 获取选择工具
2. 右键点击两个区块内的任意方块来选择区域
3. 使用 `/easyland create [ID]` 创建无主领地
4. 使用 `/easyland claim [ID]` 认领领地
5. 使用 `/easyland trust <玩家名>` 信任其他玩家

### 领地保护
- **方块保护**：防止非信任玩家破坏/放置方块
- **爆炸保护**：防止爆炸破坏领地内的方块
- **容器保护**：防止非信任玩家访问箱子、熔炉等容器
- **玩家保护**：保护领主和受信任的玩家在领地内免受伤害（可选）

### 管理员功能
- 使用 `/easyland remove <ID>` 删除任意领地
- 使用 `/easyland reload` 重载配置文件
- 拥有 `easyland.bypass` 权限可绕过所有领地保护

## 注意事项

- 插件专为 **PaperMC 1.21.4** 开发和测试
- 信任系统支持预信任功能，可以信任尚未加入服务器的玩家
- 建议定期备份 `plugins/Easyland/lands.yml` 领地数据文件
> 注意：插件移除了对旧配置 `protect-from-mob-griefing` 的兼容性，请使用新的 `protection.explosion-protection` 配置。

## 数据文件
- 领地数据保存在 `plugins/Easyland/lands.yml`
- 配置文件位于 `plugins/Easyland/config.yml`

### 更新日志
#### V1.2
- **新增配置文件管理系统**：
  - 自动验证配置值的有效性和范围
  - 自动清理无效和过时的配置项
- **新增管理员权限组 `easyland.admin`**：
  - 包含 `easyland.remove` 和 `easyland.bypass` 权限
  - 方便服务器管理员统一分配管理权限
- 优化 `/easyland show` 指令，支持自定义显示时间参数
- 新增配置项 `max-show-duration-seconds`，限制最大显示时间
- 支持灵活的参数顺序：`/easyland show [领地ID] [时间(秒)]`
- 优化 `/easyland trust` 和 `/easyland untrust` 指令：
  - 支持信任从未加入过服务器的玩家（预信任功能）
  - 添加玩家名格式验证，防止输入无效玩家名
  - 防止玩家信任自己
- 改进 `/easyland untrust` 指令的Tab补全，只显示已信任的玩家列表
- **重构领地保护系统**：
  - 将保护功能模块化，分为方块保护、爆炸保护、容器保护、玩家保护四个独立模块
  - 新增 `protection` 配置节，可独立控制各种保护规则
  - 新增玩家保护功能，领主和受信任的玩家在领地内可免受伤害
  - 改进容器保护，支持更多容器类型的保护
  - **移除对旧配置 `protect-from-mob-griefing` 的兼容性，请使用新的 `protection.explosion-protection` 配置**
- **消息系统优化**：
  - 实现消息冷却机制，防止连续伤害等情况下的消息刷屏
  - 新增 `message-cooldown-seconds` 配置项，可自定义消息冷却时间（0-60秒）
  - 自动清理过期的冷却记录，优化内存使用

#### V1.1
- 新增 `/easyland trustlist` 指令，可查看自己领地的信任玩家列表

#### V1.0
- 实现插件基本功能
- 仅在 PaperMC 1.21.4 进行了测试

## 技术信息

- **开发版本**：Java 17
- **服务端支持**：PaperMC 1.21.4
- **API版本**：1.21
- **当前版本**：1.2
