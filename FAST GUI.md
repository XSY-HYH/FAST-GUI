# FastGUI | 快速界面生成器

**FAST GUI是开源的：https://github.com/XSY-HYH/FAST-GUI**

**FAST GUI is open source: https://github.com/XSY-HYH/FAST-GUI**

## 项目概述 | Project Overview
FastGUI 是由 XSY Team 开发的 Minecraft 服务器插件，基于原版箱子提供自定义用户界面解决方案。管理员可以快速创建和管理复杂界面，无需客户端修改，同时保持与原生 Minecraft 的完全兼容性。

FastGUI is a Minecraft server plugin developed by XSY Team that provides custom user interface solutions based on vanilla chests. Administrators can quickly create and manage complex interfaces without requiring client modifications, while maintaining full compatibility with native Minecraft.

## 核心功能 | Core Features

### 基础功能 | Basic Features
- **原版箱子界面**：基于原版箱子创建自定义界面，支持单箱和双箱扩展
- **Vanilla Chest Interface**: Create custom interfaces based on vanilla chests, supporting both single and double chest extensions

- **按钮系统**：通过 NBT 标签设置按钮命令和点击行为
- **Button System**: Set button commands and click behaviors through NBT tags

- **数据持久化**：界面数据自动保存到本地文件
- **Data Persistence**: Interface data automatically saves to local files

- **变量替换**：支持 {player}、{x}、{y}、{z} 等动态变量
- **Variable Substitution**: Support for dynamic variables like {player}, {x}, {y}, {z}

- **多语言支持**：内置中文和英文语言包，支持自定义语言文件
- **Multi-language Support**: Built-in Chinese and English language packs, with support for custom language files

### 高级功能 | Advanced Features
- **权限控制**：为按钮、NPC和UI设置细粒度权限，支持op/np权限节点
- **Permission Control**: Set fine-grained permissions for buttons, NPCs, and UIs, supporting op/np permission nodes

- **NPC 交互**：绑定 NPC 到界面进行点击交互
- **NPC Interaction**: Bind NPCs to interfaces for click interactions

- **书本界面**：基于书与笔的界面编辑器
- **Book Interface**: Interface editor based on book and quill

- **命令事件系统**：通过 FGCD 命令创建和管理命令事件
- **Command Event System**: Create and manage command events through FGCD commands

- **交互物品**：支持在空中点击的物品按钮
- **Interactive Items**: Support for item buttons that can be clicked in air

## 命令系统 | Command System

### 主命令 (/fg) | Main Command (/fg)
- `/fg add <权限节点> <x> <y> <z> <界面名称>` - 创建新界面 (权限节点: op/np)
- `/fg add <permission_node> <x> <y> <z> <interface_name>` - Create new interface (permission node: op/np)

- `/fg open <名称>` - 打开指定界面
- `/fg open <name>` - Open specified interface

- `/fg list` - 列出所有界面
- `/fg list` - List all interfaces

- `/fg set id <源UI名> <要改的名字>` - 重命名界面
- `/fg set id <source_ui_name> <new_name>` - Rename interface

- `/fg delete <名称>` - 删除界面
- `/fg delete <name>` - Delete interface

- `/fg refresh` - 刷新界面缓存
- `/fg refresh` - Refresh interface cache

- `/fg debug` - 切换调试模式
- `/fg debug` - Toggle debug mode

- `/fg reload` - 重新加载插件配置
- `/fg reload` - Reload plugin configuration

### NBT 命令 (/fgnbt) | NBT Command (/fgnbt)
- `/fgnbt Border` - 设置边框物品
- `/fgnbt Border` - Set border item

- `/fgnbt Button command "command"` - 设置按钮命令
- `/fgnbt Button command "command"` - Set button command

- `/fgnbt Button command "command" closeOnClick:true` - 设置点击后关闭的按钮
- `/fgnbt Button command "command" closeOnClick:true` - Set button that closes on click

- `/fgnbt ButtonItem <权限节点> <执行人> "命令"` - 设置可在空中点击的物品按钮
- `/fgnbt ButtonItem <permission_node> <executor> "command"` - Set item button that can be clicked in air

- `/fgnbt npc "command"` - 设置 NPC 命令（需要手持刷怪蛋）
- `/fgnbt npc "command"` - Set NPC command (requires holding spawn egg)

### 书本命令 (/fgBook) | Book Command (/fgBook)
- `/fgBook register <名称>` - 注册书本界面（需要手持书与笔）
- `/fgBook register <name>` - Register book interface (requires holding book and quill)

- `/fgBook open <名称>` - 打开书本界面
- `/fgBook open <name>` - Open book interface

- `/fgBook list` - 列出所有书本界面
- `/fgBook list` - List all book interfaces

- `/fgBook delete <名称>` - 删除书本界面
- `/fgBook delete <name>` - Delete book interface

### FGCD 命令 (/fgcd) | FGCD Command (/fgcd)
- `/fgcd add <世界名> <命令> <执行体>` - 添加命令事件
- `/fgcd add <world_name> <command> <executor>` - Add command event

- `/fgcd delete <世界名> <命令>` - 删除命令事件
- `/fgcd delete <world_name> <command>` - Delete command event

- `/fgcd list [世界名]` - 列出命令事件
- `/fgcd list [world_name]` - List command events

### 语言命令 (/fglang) | Language Command (/fglang)
- `/fglang` - 查看当前语言和可用语言
- `/fglang` - View current language and available languages

- `/fglang <语言文件名>` - 切换到指定语言
- `/fglang <language_file>` - Switch to specified language

## 系统要求 | System Requirements

### 服务器 | Server
- **PaperMC**: 1.21.x 版本（所有 1.21.x 版本均已测试通过）
- **PaperMC**: 1.21.x versions (All 1.21.x versions have been tested)

### Java
- **版本要求**: Java 17 或更高版本
- **Version**: Java 17 or higher

### 内存 | Memory
- **最低要求**: 512MB
- **Minimum**: 512MB

- **推荐配置**: 2GB 或更多
- **Recommended**: 2GB or more

## 版本信息 | Version Information

### 当前版本 | Current Version
- **版本号**: 6.9.8
- **Version**: 6.9.8

### 版本更新日志 | Update Changelog
- **新增 FGCD 命令系统**：支持创建、删除和列出命令事件
- **Added FGCD Command System**: Support for creating, deleting, and listing command events

- **优化 NPC 和交互物品**：修复执行问题，更新 NBT 结构
- **Optimized NPC and Interactive Items**: Fixed execution issues, updated NBT structure

- **UI 权限系统**：支持通过 np/op 设置权限节点
- **UI Permission System**: Support for setting np/op permission nodes

- **命令系统优化**：改进 tab 补全，修复命令格式
- **Command System Optimization**: Improved tab completion, fixed command formats

- **全面支持 1.21.x**: 所有 1.21.x 版本均已测试通过
- **Full Support for 1.21.x**: All 1.21.x versions have been tested

## 安装方法 | Installation

1. **下载插件**: 获取最新版本的 FastGUI.jar 文件
1. **Download Plugin**: Get the latest FastGUI.jar file

2. **上传文件**: 将插件文件上传到服务器的 plugins 文件夹
2. **Upload File**: Upload the plugin file to the server's plugins folder

3. **重启服务器**: 重启 Minecraft 服务器以加载插件
3. **Restart Server**: Restart the Minecraft server to load the plugin

4. **验证安装**: 使用 `/fg help` 命令检查插件是否正常工作
4. **Verify Installation**: Use `/fg help` to check if the plugin is working correctly

## 开发团队 | Development Team

### 开发团队 | Development Team
- **XSY Team**: FastGUI 插件的主要开发团队
- **XSY Team**: Main development team for FastGUI plugin

### 联系方式 | Contact Information
- **GitHub**: 项目源代码和问题反馈
- **GitHub**: Project source code and issue feedback

## 许可证 | License

FastGUI 插件遵循 MIT 许可证，允许自由使用、修改和分发。

FastGUI plugin is licensed under the MIT License, allowing free use, modification, and distribution.

---

© 2026 XSY Team. All rights reserved.
