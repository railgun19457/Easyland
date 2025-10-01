# Easyland 领地管理插件

**语言 / Language：** [English](README.en.md) · [简体中文](README.md)

![:Easyland](https://count.getloli.com/@Easyland?name=RailgunEasyland&theme=minecraft&padding=6&offset=0&align=top&scale=1&pixelated=1&darkmode=auto)

![Version](https://img.shields.io/badge/version-1.4.1-blue)
![Minecraft](https://img.shields.io/badge/minecraft-1.21.8-green)
![Java](https://img.shields.io/badge/java-21-orange)
![Paper](https://img.shields.io/badge/Paper-1.21.x-brightgreen)

高性能、易上手的 PaperMC 领地管理插件，覆盖领地创建、权限分配、保护规则与可视化展示等完整功能。

## 🧭 目录

- [Easyland 领地管理插件](#easyland-领地管理插件)
  - [🧭 目录](#-目录)
  - [✨ 功能亮点](#-功能亮点)
  - [🛠️ 指令总览](#️-指令总览)
  - [🔐 权限节点](#-权限节点)
  - [⚙️ 核心配置](#️-核心配置)
    - [保护规则](#保护规则)
  - [🌍 多语言支持](#-多语言支持)
  - [🚀 快速上手](#-快速上手)
  - [📦 安装部署](#-安装部署)
    - [运行环境](#运行环境)
    - [安装步骤](#安装步骤)
    - [从源码构建（可选）](#从源码构建可选)
  - [📝 更新日志](#-更新日志)
    - [v1.4.1 · 多语言强化](#v141--多语言强化)
    - [v1.4 · Paper 1.21.8 适配](#v14--paper-1218-适配)
    - [v1.3.1](#v131)
    - [v1.3](#v13)
    - [v1.2](#v12)
    - [v1.1](#v11)
    - [v1.0](#v10)
  - [📄 许可证](#-许可证)
  - [🤝 支持反馈](#-支持反馈)

## ✨ 功能亮点

- 🏗️ **领地管理**：支持创建、认领、删除、列表查看等完整操作
- 👥 **信任系统**：灵活的成员管理，包含预信任机制
- 🛡️ **保护策略**：方块、爆炸、容器、玩家等多维度防护
- 🌈 **可视化边界**：粒子特效展示领地范围，所见即所得
- 🌍 **内置多语言**：提供中文、英文、日文本地化

## 🛠️ 指令总览

主命令：`/easyland`（别名：`/el`、`/land`）

| 子命令                 | 说明         | 权限             |
| ---------------------- | ------------ | ---------------- |
| `select`               | 获取选择工具 | easyland.select  |
| `create [id]`          | 创建领地     | easyland.create  |
| `claim [id]`           | 认领领地     | easyland.claim   |
| `unclaim [id]`         | 放弃领地     | easyland.unclaim |
| `trust <玩家>`         | 信任玩家     | easyland.trust   |
| `untrust <玩家>`       | 取消信任     | easyland.untrust |
| `trustlist`            | 查看信任列表 | easyland.trust   |
| `show [id] [时间]`     | 显示边界     | easyland.show    |
| `list`                 | 查看领地列表 | easyland.list    |
| `remove <id>`          | 删除领地     | easyland.remove  |
| `rule [规则] [on/off]` | 管理保护规则 | easyland.rule    |
| `reload`               | 重载配置     | easyland.reload  |
| `help`                 | 查看帮助     | easyland.help    |

## 🔐 权限节点

| 权限节点         | 描述         | 默认值 |
| ---------------- | ------------ | ------ |
| easyland.select  | 获取选择工具 | op     |
| easyland.create  | 创建领地     | op     |
| easyland.claim   | 认领领地     | true   |
| easyland.unclaim | 放弃领地     | true   |
| easyland.trust   | 信任管理     | true   |
| easyland.untrust | 取消信任     | true   |
| easyland.show    | 显示边界     | true   |
| easyland.list    | 查看列表     | true   |
| easyland.rule    | 保护规则     | true   |
| easyland.remove  | 删除领地     | op     |
| easyland.reload  | 重载配置     | op     |
| easyland.help    | 查看帮助     | true   |
| easyland.bypass  | 绕过保护     | op     |
| easyland.admin   | 管理员合集   | op     |

`easyland.admin` 自动继承上表所有子权限，无需重复分配。

## ⚙️ 核心配置

| 配置项                     | 作用             | 默认值   |
| -------------------------- | ---------------- | -------- |
| `max-lands-per-player`     | 玩家最大领地数   | 5        |
| `max-chunks-per-land`      | 单领地最大区块数 | 256      |
| `show-duration-seconds`    | 默认显示持续时间 | 10       |
| `max-show-duration-seconds`| 最大显示时间限制 | 30       |
| `message-cooldown-seconds` | 消息冷却时间     | 3        |
| `land-boundary-particle`   | 边界粒子样式     | firework |

### 保护规则

保护规则均包含两个字段：

- `enable`：是否允许服务器启用该规则
- `default`：新领地的默认启用状态

| 配置路径                          | 规则名称 | 功能说明           |
| --------------------------------- | -------- | ------------------ |
| `protection.block-protection`     | 方块保护 | 禁止破坏与放置方块 |
| `protection.explosion-protection` | 爆炸保护 | 防止爆炸破坏方块   |
| `protection.container-protection` | 容器保护 | 防止打开容器       |
| `protection.player-protection`    | 玩家保护 | 阻止玩家互相伤害   |

## 🌍 多语言支持

在 `config.yml` 中设置 `language` 完成切换：

```yaml
# 支持的取值：zh_cn, en_us, ja_jp
language: zh_cn
```

| 语言代码 | 语言名称 |
| -------- | -------- |
| `zh_cn`  | 中文简体 |
| `en_us`  | English  |
| `ja_jp`  | 日本語   |

欢迎提供社区翻译，提交 PR 即可加入更多语言。

## 🚀 快速上手

1. `/easyland select` 获取选择工具，设置两个区块顶点。
2. `/easyland create [ID]` 创建无主领地（可选自定义 ID）。
3. `/easyland claim [ID]` 认领无主领地或站在领地内直接认领。
4. `/easyland trust <玩家>` 管理伙伴，共享领地权限。
5. `/easyland rule <规则> on/off` 调整领地保护策略。

## 📦 安装部署

### 运行环境

- 服务端：Paper 1.21.x
- Java 版本：21 或更高
- 兼容性：向下兼容 Paper 1.20.4+

### 安装步骤

1. 下载最新构建 `easyland-1.4.1.jar`。
2. 将插件放入服务器 `plugins` 目录。
3. 启动或重启服务器生成配置文件。
4. 按需调整 `config.yml` 与权限节点。

### 从源码构建（可选）

1. 克隆仓库 `git clone https://github.com/railgun19457/Easyland.git`。
2. 在项目根目录执行 `mvn clean package` 生成产物。
3. 使用 `target/easyland-1.4.1.jar` 部署到服务器。

## 📝 更新日志

### v1.4.1 · 多语言强化

- 🌍 引入完整国际化（i18n）框架
- 🐛 修复与连锁破坏类插件联动导致的无限刷资源问题
- ➕ 新增 `reload`、`help` 命令

### v1.4 · Paper 1.21.8 适配

- 🚀 升级支持 Paper 1.21.8 与 Java 21
- 🐛 修复 `LandEnterListener` 空指针异常
- ⚡ 优化爆炸保护逻辑，减少额外检查
- 🛡️ 玩家保护剥离自然伤害，行为更符合直觉
- ♻️ 兼容 Paper 1.20.4+

### v1.3.1

- 📈 空间索引优化，查询性能提升 60–80%
- 🔒 全面换用 `ConcurrentHashMap` 提升并发安全
- 💾 降低 20–30% 内存占用
- 🧹 重构配置与常量，方便维护

### v1.3

- 🔁 全面重写领地保护系统
- 🆕 新增 `/easyland rule` 指令体系
- 🗂️ 保护规则改为领地级独立存储
- 🧱 配置文件结构升级

### v1.2

- 🗃️ 新增配置文件管理系统
- 🛠️ 增加管理员权限组
- 🎯 `show` 指令支持自定义时间

### v1.1

- 👥 信任系统支持预信任
- 📋 新增 `trustlist` 指令

### v1.0

- 🎉 实现基础领地管理能力

## 📄 许可证

本项目基于 [MIT License](LICENSE) 发布，使用或分发时请保留版权和许可声明。

## 🤝 支持反馈

- ⭐ 欢迎 Star 本仓库支持项目发展
- 🐛 如遇问题请在 Issues 中反馈
- 💬 也可通过 Pull Request 贡献功能或翻译

---

感谢选择 Easyland，祝你和玩家在领地里玩得开心！

