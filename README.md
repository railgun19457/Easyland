# Easyland

一个领地插件，支持区块选择、领地创建、认领、玩家信任、显示范围等功能。

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
| easyland.bypass   | 绕过领地保护                  | op     |

## 配置项

- `max-lands-per-player`：每名玩家最大可拥有领地数，默认5
- `max-chunks-per-land`：单个领地最大区块数，默认256
- `land-boundary-particle`：显示领地边界的粒子类型，默认FLAME
- `show-duration-seconds`：显示领地范围的默认秒数，默认10
- `max-show-duration-seconds`：显示领地范围的最大秒数，默认300
- `protect-from-mob-griefing`：是否保护领地不被生物破坏，默认为`false`

> 注意：如 `protect-from-mob-griefing` 配置为 `false`，领地内将允许生物破坏（如苦力怕爆炸等）。

## 数据文件
- 领地数据保存在 `plugins/Easyland/lands.yml`

### 更新日志
#### V1.2
- 优化 `/easyland show` 指令，支持自定义显示时间参数
- 新增配置项 `max-show-duration-seconds`，限制最大显示时间
- 支持灵活的参数顺序：`/easyland show [领地ID] [时间(秒)]`
- 优化 `/easyland trust` 和 `/easyland untrust` 指令：
  - 支持信任从未加入过服务器的玩家（预信任功能）
  - 添加玩家名格式验证，防止输入无效玩家名
  - 防止玩家信任自己
- 改进 `/easyland untrust` 指令的Tab补全，只显示已信任的玩家列表

#### V1.1
- 新增 `/easyland trustlist` 指令，可查看自己领地的信任玩家列表

#### V1.0
- 实现插件基本功能 
- 仅在paperMC1.21.4进行了测试
