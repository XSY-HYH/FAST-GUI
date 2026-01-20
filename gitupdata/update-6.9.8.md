# FastGUI 6.9.8 更新日志 | FastGUI 6.9.8 Update Changelog

## 📅 更新日期 | Update Date
2026-01-20

## 🎯 更新概述 | Update Overview
FastGUI 6.9.8 版本带来了全新的命令事件系统（FGCD）、UI权限系统以及一系列功能优化和bug修复，全面支持Minecraft 1.21.x版本。

FastGUI 6.9.8 brings a new command event system (FGCD), UI permission system, and a series of feature optimizations and bug fixes, fully supporting Minecraft 1.21.x versions.

## ✨ 新增功能 | New Features

### 1. FGCD 命令事件系统 | FGCD Command Event System
- **新增 `/fgcd` 命令**：用于创建和管理命令事件
- **Added `/fgcd` command**: For creating and managing command events

- **支持三个子命令**：
  - `/fgcd add <世界名> <命令> <执行体>` - 添加命令事件
  - `/fgcd delete <世界名> <命令>` - 删除命令事件
  - `/fgcd list [世界名]` - 列出命令事件

- **Support three subcommands**:
  - `/fgcd add <world_name> <command> <executor>` - Add command event
  - `/fgcd delete <world_name> <command>` - Delete command event
  - `/fgcd list [world_name]` - List command events

### 2. UI 权限系统 | UI Permission System
- **新增权限节点**：支持通过 `op/np` 设置UI的访问权限
- **Added permission nodes**: Support for setting UI access permissions through `op/np`

- **命令更新**：`/fg add` 命令新增权限节点参数
- **Command update**: `/fg add` command adds permission node parameter

## 🛠️ 功能优化 | Feature Optimizations

### 1. 命令系统优化 | Command System Optimization
- **改进 Tab 补全**：为主命令 `/fg` 添加了Tab补全功能
- **Improved Tab Completion**: Added Tab completion for main command `/fg`

- **修复命令格式**：
  - `/fg set` 命令修复为 `/fg set id <源UI名> <要改的名字>`
  - `/fg add` 命令更新为 `/fg add <权限节点> <x> <y> <z> <界面名称>`

- **Fixed command formats**:
  - `/fg set` command fixed to `/fg set id <source_ui_name> <new_name>`
  - `/fg add` command updated to `/fg add <permission_node> <x> <y> <z> <interface_name>`

### 2. NPC 和交互物品优化 | NPC and Interactive Item Optimization
- **修复执行问题**：解决了NPC命令执行的权限检查问题
- **Fixed execution issues**: Resolved permission check issues for NPC command execution

- **更新 NBT 结构**：优化了交互物品的NBT存储格式
- **Updated NBT structure**: Optimized NBT storage format for interactive items

### 3. 语言文件更新 | Language File Updates
- **更新命令帮助**：修正了 `/fg add` 和 `/fg set` 命令的帮助信息
- **Updated command help**: Corrected help information for `/fg add` and `/fg set` commands

## 🐛 Bug 修复 | Bug Fixes

1. **解决引用命令失去空格的问题**：修复了带有引号的命令在执行时空格丢失的问题
2. **Fixed quoted command space loss**: Fixed the issue where spaces were lost in quoted commands during execution

3. **修复界面切换时的闪烁问题**：优化了界面加载机制，减少闪烁
4. **Fixed interface flickering on switch**: Optimized interface loading mechanism to reduce flickering

5. **解决权限节点检查错误**：修复了UI权限检查的逻辑错误
6. **Fixed permission node check errors**: Resolved logical errors in UI permission checks

## 🎮 版本兼容性 | Version Compatibility

- **全面支持 Paper 1.21.x**：所有1.21.x版本均已测试通过
- **Full support for Paper 1.21.x**: All 1.21.x versions have been tested

- **Java 版本要求**：Java 17 或更高版本
- **Java version requirement**: Java 17 or higher

## 📦 安装和升级 | Installation and Upgrade

1. **下载插件**：从 GitHub Release 页面下载最新版本的 FastGUI.jar
2. **Download plugin**: Download the latest FastGUI.jar from GitHub Release page

3. **替换旧版本**：将新的 FastGUI.jar 替换服务器 plugins 文件夹中的旧版本
4. **Replace old version**: Replace the old FastGUI.jar in the server's plugins folder with the new one

5. **重启服务器**：重启 Minecraft 服务器以加载新版本
6. **Restart server**: Restart the Minecraft server to load the new version

## 📝 使用说明 | Usage Instructions

### UI 权限设置示例 | UI Permission Setting Example
```
/fg add op 100 64 100 my_ui  # 只有OP可以打开该UI
/fg add np 100 64 100 public_ui  # 所有玩家都可以打开该UI
```

### FGCD 命令使用示例 | FGCD Command Usage Example
```
/fgcd add world "say Hello World" console  # 在world世界添加一个控制台命令事件
/fgcd list world  # 列出world世界的所有命令事件
/fgcd delete world "say Hello World"  # 删除world世界的指定命令事件
```

## 📧 联系方式 | Contact Information

如有问题或建议，欢迎通过以下方式联系我们：

- **GitHub Issues**: https://github.com/XSY-HYH/FAST-GUI/issues
- **GitHub Discussions**: https://github.com/XSY-HYH/FAST-GUI/discussions

---

© 2026 XSY Team. All rights reserved.
