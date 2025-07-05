# Easyland 领地插件

## 插件简介
Easyland 是一款适用于 PaperMC 1.21.4 的简易领地插件，支持玩家自助创建、认领、放弃和管理领地，支持领地保护、信任玩家、权限控制等功能。

## 主要功能
- 玩家可用木铲选择区块，创建矩形领地
- 支持无主领地、认领、放弃
- 支持信任/取消信任其他玩家
- 领地保护：禁止非主人与未被信任者在领地内破坏方块、交互容器
- 进入/离开领地时提示
- 支持最大领地数、单个领地最大区块数限制
- 权限节点细分，便于服主管理
- 所有数据自动保存于 `lands.yml`，配置项在 `config.yml`

## 指令
- `/easyland select` 获得领地选择木铲
- `/easyland create` 用选定区块创建无主领地
- `/easyland claim` 认领无主领地为自己所有
- `/easyland unclaim` 放弃自己已认领的领地
- `/easyland trust <玩家名>` 信任玩家
- `/easyland untrust <玩家名>` 取消信任

别名：`/el`、`/land`

## 权限节点
- `easyland.base` 使用主指令
- `easyland.select` 选择领地
- `easyland.create` 创建无主领地
- `easyland.claim` 认领领地
- `easyland.unclaim` 放弃领地
- `easyland.trust` 信任他人
- `easyland.untrust` 取消信任
- `easyland.bypass` 管理员绕过保护

## 配置项
- `max-lands-per-player` 每个玩家最大领地数
- `max-chunks-per-land` 单个领地最大区块数

## 数据存储
- 领地数据：`lands.yml`
- 配置文件：`config.yml`

## 构建与安装
1. 使用 `mvn clean package` 打包，生成 jar 文件于 `target/` 目录
2. 将 jar 文件放入服务器 `plugins` 文件夹，重启服务器

## 适用环境
- PaperMC 1.21.4
- Java 17 及以上

## 开源地址
- https://github.com/railgun19457/Easyland

如有建议或问题欢迎反馈！
