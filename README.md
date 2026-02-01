# plugins

Minecraft 服务器插件集合仓库，按模块划分，每个插件单独维护与构建。

## 目录结构

```
modules/
  AcShop/
  Acore/
  AtTackCraft-Core/
  Gem/
  GemShop/
  GuildManager/
  ItemManager/
  MedalCabinet/
```

## 模块简介

- AcShop：基于 Vault 的商店插件（支持商品与管理指令）。
- Acore：核心工具插件（通用工具/基础能力）。
- AtTackCraft-Core：服务器核心功能集合（Bukkit/Bungee，含保护、公告、白名单等）。
- Gem：宝石/货币系统（MySQL，支持 PAPI）。
- GemShop：宝石商店（依赖 Gem）。
- GuildManager：公会系统（Vault/PlaceholderAPI/MySQL 等）。
- ItemManager：物品存储与管理工具（供其他插件调用）。
- MedalCabinet：勋章展示与管理（支持 PAPI，含 GUI）。

## 构建与使用

每个插件为独立 Maven 工程：

1. 进入对应模块目录
2. 执行 `mvn package`
3. 将产物放入服务器 `plugins/`

具体指令与配置请查看各模块内的 `README.md` 与 `plugin.yml`。

## 运行依赖

| 模块 | 硬依赖 | 软依赖/可选 |
| --- | --- | --- |
| AcShop | Vault | ItemManager（作为内部工具库使用） |
| Acore | 无 | 无 |
| AtTackCraft-Core | 无 | Multiverse-Core，PlaceholderAPI |
| Gem | 无 | MySQL（功能所需），PlaceholderAPI |
| GemShop | Gem | 无 |
| GuildManager | 无 | Vault，PlaceholderAPI，AuthMe，MySQL（功能所需） |
| ItemManager | 无 | 无 |
| MedalCabinet | 无 | PlaceholderAPI，MySQL（功能所需） |
