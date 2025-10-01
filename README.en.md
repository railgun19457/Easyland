# Easyland Land Management Plugin

**Language:** [English](README.en.md) · [简体中文](README.md)

![:Easyland](https://count.getloli.com/@Easyland?name=RailgunEasyland&theme=minecraft&padding=6&offset=0&align=top&scale=1&pixelated=1&darkmode=auto)

![Version](https://img.shields.io/badge/version-1.4.1-blue)
![Minecraft](https://img.shields.io/badge/minecraft-1.21.8-green)
![Java](https://img.shields.io/badge/java-21-orange)
![Paper](https://img.shields.io/badge/Paper-1.21.x-brightgreen)

A high-performance PaperMC land management plugin that covers land creation, access control, protection rules, and particle-based visualization.

## 🧭 Table of Contents

- [Easyland Land Management Plugin](#easyland-land-management-plugin)
  - [🧭 Table of Contents](#-table-of-contents)
  - [✨ Highlights](#-highlights)
  - [🛠️ Command Overview](#️-command-overview)
  - [🔐 Permission Nodes](#-permission-nodes)
  - [⚙️ Core Configuration](#️-core-configuration)
    - [Protection Rules](#protection-rules)
  - [🌍 Localization](#-localization)
  - [🚀 Quick Start](#-quick-start)
  - [📦 Deployment](#-deployment)
    - [Environment](#environment)
    - [Installation](#installation)
    - [Build from Source (Optional)](#build-from-source-optional)
  - [📝 Changelog](#-changelog)
    - [v1.4.1 · Localization Upgrade](#v141--localization-upgrade)
    - [v1.4 · Paper 1.21.8 Compatibility](#v14--paper-1218-compatibility)
    - [v1.3.1](#v131)
    - [v1.3](#v13)
    - [v1.2](#v12)
    - [v1.1](#v11)
    - [v1.0](#v10)
  - [📄 License](#-license)
  - [🤝 Support](#-support)

## ✨ Highlights

- 🏗️ **Land management**: Create, claim, delete, and list lands with ease.
- 👥 **Trust system**: Flexible member management with pre-trust support.
- 🛡️ **Protection policies**: Block, explosion, container, and player protection in one place.
- 🌈 **Visualized boundaries**: Particle effects render land borders directly in game.
- 🌍 **Built-in localization**: Chinese, English, and Japanese translations included.

## 🛠️ Command Overview

Main command: `/easyland` (aliases: `/el`, `/land`)

| Subcommand              | Description                 | Permission         |
| ----------------------- | --------------------------- | ------------------ |
| `select`                | Give the selection tool     | easyland.select    |
| `create [id]`           | Create a new land           | easyland.create    |
| `claim [id]`            | Claim an unowned land       | easyland.claim     |
| `unclaim [id]`          | Release your land           | easyland.unclaim   |
| `trust <player>`        | Trust a player              | easyland.trust     |
| `untrust <player>`      | Remove a trusted player     | easyland.untrust   |
| `trustlist`             | View trusted players        | easyland.trust     |
| `show [id] [seconds]`   | Visualize land boundary     | easyland.show      |
| `list`                  | List all lands              | easyland.list      |
| `remove <id>`           | Delete a land               | easyland.remove    |
| `rule [key] [on/off]`   | Manage protection rules     | easyland.rule      |
| `reload`                | Reload configuration        | easyland.reload    |
| `help`                  | Display help information    | easyland.help      |

## 🔐 Permission Nodes

| Permission        | Description               | Default |
| ----------------- | ------------------------- | ------- |
| easyland.select   | Access to the selection tool | op    |
| easyland.create   | Create lands               | op      |
| easyland.claim    | Claim lands                | true    |
| easyland.unclaim  | Unclaim lands              | true    |
| easyland.trust    | Manage trusted players     | true    |
| easyland.untrust  | Remove trusted players     | true    |
| easyland.show     | Visualize land boundary    | true    |
| easyland.list     | View land list             | true    |
| easyland.rule     | Toggle protection rules    | true    |
| easyland.remove   | Delete lands               | op      |
| easyland.reload   | Reload configuration       | op      |
| easyland.help     | Display help               | true    |
| easyland.bypass   | Bypass protection checks   | op      |
| easyland.admin    | Administrator bundle       | op      |

`easyland.admin` inherits every sub-permission above—no extra assignment required.

## ⚙️ Core Configuration

| Key                        | Purpose                     | Default |
| -------------------------- | --------------------------- | ------- |
| `max-lands-per-player`     | Maximum lands per player    | 5       |
| `max-chunks-per-land`      | Maximum chunks per land     | 256     |
| `show-duration-seconds`    | Default visualization time  | 10      |
| `max-show-duration-seconds`| Maximum visualization time  | 30      |
| `message-cooldown-seconds` | Global message cooldown     | 3       |
| `land-boundary-particle`   | Particle type for boundary  | firework |

### Protection Rules

Each rule provides two flags:

- `enable`: Whether the rule is available on the server
- `default`: Default state for newly created lands

| Path                               | Rule            | Description                      |
| ---------------------------------- | --------------- | -------------------------------- |
| `protection.block-protection`      | Block protection| Prevent block breaking/placing   |
| `protection.explosion-protection`  | Explosion guard | Stop explosions from doing damage |
| `protection.container-protection`  | Container guard | Block access to chests & storage |
| `protection.player-protection`     | Player guard    | Protect players from PvP damage  |

## 🌍 Localization

Set the `language` value in `config.yml` to switch the UI language:

```yaml
# Supported values: zh_cn, en_us, ja_jp
language: en_us
```

| Code    | Language    |
| ------- | ----------- |
| `zh_cn` | Simplified Chinese |
| `en_us` | English     |
| `ja_jp` | Japanese    |

Community translations are welcome—send a PR to add your language.

## 🚀 Quick Start

1. `/easyland select` to obtain the selection wand and mark two chunk corners.
2. `/easyland create [ID]` to create an unclaimed land (custom ID optional).
3. `/easyland claim [ID]` to claim an unowned land or the one you stand in.
4. `/easyland trust <player>` to share access with friends.
5. `/easyland rule <key> on/off` to configure protection behavior.

## 📦 Deployment

### Environment

- Server: Paper 1.21.x
- Java: 21 or newer
- Compatibility: backward compatible with Paper 1.20.4+

### Installation

1. Download the latest `easyland-1.4.1.jar` build.
2. Drop the plugin into your server's `plugins` directory.
3. Start or restart the server to generate configuration files.
4. Adjust `config.yml` and permission nodes as needed.

### Build from Source (Optional)

1. Clone the repository: `git clone https://github.com/railgun19457/Easyland.git`.
2. Run `mvn clean package` in the project root to produce artifacts.
3. Deploy `target/easyland-1.4.1.jar` to your Paper server.

## 📝 Changelog

### v1.4.1 · Localization Upgrade

- 🌍 Introduced a full i18n system
- 🐛 Fixed infinite resource duplication caused by vein-miner style plugins
- ➕ Added `reload` and `help` commands

### v1.4 · Paper 1.21.8 Compatibility

- 🚀 Updated to support Paper 1.21.8 and Java 21
- 🐛 Fixed `LandEnterListener` NullPointerException
- ⚡ Streamlined explosion protection checks
- 🛡️ Player protection no longer includes natural damage (falling, etc.)
- ♻️ Remains compatible with Paper 1.20.4+

### v1.3.1

- 📈 Spatial index optimization improves lookup performance by 60–80%
- 🔒 Adopted `ConcurrentHashMap` throughout for thread safety
- 💾 Reduced memory usage by 20–30%
- 🧹 Refactored configuration to remove hard-coded values

### v1.3

- 🔁 Completely overhauled the land protection system
- 🆕 Added the `/easyland rule` command flow
- 🗂️ Stored protection rules per land instead of globally
- 🧱 Upgraded configuration structure

### v1.2

- 🗃️ Introduced the configuration management system
- 🛠️ Added an administrator permission group
- 🎯 `show` command supports custom durations

### v1.1

- 👥 Trust system now supports pre-trusting players
- 📋 Added the `trustlist` command

### v1.0

- 🎉 Delivered the core land management feature set

## 📄 License

Released under the [MIT License](LICENSE). Please retain copyright and license notices when redistributing.

## 🤝 Support

- ⭐ Star the repository to support the project
- 🐛 Report issues via GitHub Issues
- 💬 Contribute features or translations through Pull Requests

---

Thanks for choosing Easyland—have fun building thriving communities!
