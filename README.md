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

## 交易与异步数据访问

更新时请同时部署匹配版本的 Gem 与 GemShop。GemShop 不再打包一份 Gem 实现，运行时使用 Gem 插件提供的 API。

- 商店先检查容量；后台扣宝石后再次检查。无法完整交付时恢复背包并退回余额，退款不增加累计宝石。
- 公会创建先检查 ID 和成员状态，再收费；公会及会长成员关系在同一事务提交。捐款同时更新公会资金和成员贡献（最高 100）。收费前要求 `guild`、`player_guild` 均为 InnoDB，不自动修改现有表结构。
- SQL 在后台执行，背包、Vault、权限命令和消息在主线程执行。公会读取使用整批替换的缓存（每分钟刷新）；宝石占位符使用缓存（约 5 秒刷新），勋章标题在后台预热。
- 正常失败会补偿；如果扣款、提交或退款的结果不明，不自动重放，也不盲目退款。GemShop 的 `pending-purchases/`、GuildManager 的 `pending-payments/` 保留交易凭据，相关玩家暂停新的收费操作。
- 不明凭据需结合日志、余额、物品或公会记录核对后处理。`PREPARED`/`DEBIT_PENDING` 仅表示操作已准备，不能证明是否已扣款；进程异常退出也可能留下已完成交易的凭据。不要仅凭文件存在就退款，或未核对就删除。核对并处理凭据后，需正常重启以解除该玩家的内存暂停状态。Vault 与 MySQL 属于不同存储，不能承诺跨进程崩溃时自动恰好执行一次。

本次没有恢复仓库此前主动移除的 `/gmgs` 会长指令。
