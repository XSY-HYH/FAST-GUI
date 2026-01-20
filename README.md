# FastGUI | 快速界面生成器

**FAST GUI是开源的：https://github.com/XSY-HYH/FAST-GUI**

**FAST GUI is open source: https://github.com/XSY-HYH/FAST-GUI**

## 📋 项目简介 | Project Introduction

FastGUI是由XSY Team开发的Minecraft服务器插件，基于原版箱子提供自定义用户界面解决方案。管理员可以快速创建和管理复杂界面，无需客户端修改，同时保持与原生Minecraft的完全兼容性。

FastGUI is a Minecraft server plugin developed by XSY Team that provides custom user interface solutions based on vanilla chests. Administrators can quickly create and manage complex interfaces without requiring client modifications, while maintaining full compatibility with native Minecraft.

## ✨ 核心功能 | Core Features

### 基础功能 | Basic Features
- **原版箱子界面**：基于原版箱子创建自定义界面，支持单箱和双箱扩展
- **Vanilla Chest Interface**: Create custom interfaces based on vanilla chests, supporting both single and double chest extensions

- **按钮系统**：通过NBT标签设置按钮命令和点击行为
- **Button System**: Set button commands and click behaviors through NBT tags

- **数据持久化**：界面数据自动保存到本地文件
- **Data Persistence**: Interface data automatically saves to local files

- **变量替换**：支持{player}、{x}、{y}、{z}等动态变量
- **Variable Substitution**: Support for dynamic variables like {player}, {x}, {y}, {z}

- **多语言支持**：内置中文和英文语言包，支持自定义语言文件
- **Multi-language Support**: Built-in Chinese and English language packs, with support for custom language files

### 高级功能 | Advanced Features
- **权限控制**：为按钮、NPC和UI设置细粒度权限，支持op/np权限节点
- **Permission Control**: Set fine-grained permissions for buttons, NPCs, and UIs, supporting op/np permission nodes

- **NPC交互**：绑定NPC到界面进行点击交互
- **NPC Interaction**: Bind NPCs to interfaces for click interactions

- **书本界面**：基于书与笔的界面编辑器
- **Book Interface**: Interface editor based on book and quill

- **命令事件系统**：通过FGCD命令创建和管理命令事件
- **Command Event System**: Create and manage command events through FGCD commands

- **交互物品**：支持在空中点击的物品按钮
- **Interactive Items**: Support for item buttons that can be clicked in air

## 🚀 安装方法 | Installation

1. **下载插件**: 获取最新版本的FastGUI.jar文件
2. **Upload File**: Upload the plugin file to the server's plugins folder

3. **重启服务器**: 重启Minecraft服务器以加载插件
4. **Restart Server**: Restart the Minecraft server to load the plugin

5. **验证安装**: 使用`/fg help`命令检查插件是否正常工作
6. **Verify Installation**: Use `/fg help` to check if the plugin is working correctly

## 📖 基本使用 | Basic Usage

### 创建界面 | Create Interface
1. 放置一个大箱子
2. 在箱子中设计你的界面
3. 使用命令`/fg add <权限节点> <x> <y> <z> <界面名称>`创建界面（权限节点：op/np）

### 设置按钮 | Set Button
1. 手持要设置为按钮的物品
2. 使用命令`/fgnbt Button command "command"`设置按钮命令
3. 将物品放入界面箱子中

### 打开界面 | Open Interface
1. 使用命令`/fg open <界面名称>`打开界面
2. 点击按钮触发命令

## 📜 命令系统 | Command System

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

### NBT命令 (/fgnbt) | NBT Command (/fgnbt)
- `/fgnbt Border` - 设置边框物品
- `/fgnbt Border` - Set border item

- `/fgnbt Button command "command"` - 设置按钮命令
- `/fgnbt Button command "command"` - Set button command

- `/fgnbt ButtonItem <权限节点> <执行人> "命令"` - 设置可在空中点击的物品按钮
- `/fgnbt ButtonItem <permission_node> <executor> "command"` - Set item button that can be clicked in air

### FGCD命令 (/fgcd) | FGCD Command (/fgcd)
- `/fgcd add <世界名> <命令> <执行体>` - 添加命令事件
- `/fgcd add <world_name> <command> <executor>` - Add command event

- `/fgcd delete <世界名> <命令>` - 删除命令事件
- `/fgcd delete <world_name> <command>` - Delete command event

- `/fgcd list [世界名]` - 列出命令事件
- `/fgcd list [world_name]` - List command events

## 📦 系统要求 | System Requirements

### 服务器 | Server
- **PaperMC**: 1.21.x版本（所有1.21.x版本均已测试通过）
- **PaperMC**: 1.21.x versions (All 1.21.x versions have been tested)

### Java
- **版本要求**: Java 17或更高版本
- **Version**: Java 17 or higher

### 内存 | Memory
- **最低要求**: 512MB
- **Minimum**: 512MB

- **推荐配置**: 2GB或更多
- **Recommended**: 2GB or more

## 📊 版本信息 | Version Information

### 当前版本 | Current Version
- **版本号**: 6.9.8
- **Version**: 6.9.8

### 版本更新日志 | Update Changelog
- **新增FGCD命令系统**：支持创建、删除和列出命令事件
- **Added FGCD Command System**: Support for creating, deleting, and listing command events

- **优化NPC和交互物品**：修复执行问题，更新NBT结构
- **Optimized NPC and Interactive Items**: Fixed execution issues, updated NBT structure

- **UI权限系统**：支持通过np/op设置权限节点
- **UI Permission System**: Support for setting np/op permission nodes

- **命令系统优化**：改进tab补全，修复命令格式
- **Command System Optimization**: Improved tab completion, fixed command formats

## 🤝 贡献 | Contributing

欢迎提交Issue和Pull Request来帮助改进FastGUI！

Contributions are welcome! Feel free to submit Issues and Pull Requests to help improve FastGUI.

## 📄 许可证 | License

FastGUI插件遵循MIT许可证，允许自由使用、修改和分发。

FastGUI plugin is licensed under the MIT License, allowing free use, modification, and distribution.

## 📧 联系方式 | Contact

如有问题或建议，欢迎通过GitHub Issues联系我们。

For questions or suggestions, please contact us through GitHub Issues.

---

© 2026 XSY Team. All rights reserved.
