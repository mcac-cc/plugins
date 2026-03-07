## 2026-02-26 - [Redundant DB Query in GemExecutor]
**Learning:** Found a redundant database query in `GemExecutor.addGems` where `MySQLManager.getInstance().getGems(name)` was called twice, resulting in an extra SELECT query per gem addition.
**Action:** Removed the redundant call by using the local variable `gems` which already held the result. This optimization saves one database roundtrip per call.

## 2026-03-06 - [GuildManager LoginListener Notification Bottleneck]
**Learning:** Sending notifications to online guild members inside `LoginListener` previously iterated over all online players on the server (O(N)), inside of which it fetched the list of guild members and performed a linear search string comparison (O(M)). In highly-populated servers, iterating `Bukkit.getOnlinePlayers()` inside a listener can cause main-thread latency, especially when doing memory list queries inside the loop.
**Action:** Replaced O(N*M) check with an O(M) check by iterating strictly over the guild members list and resolving their `Player` objects via `Bukkit.getPlayerExact()`.

## 2024-05-15 - [Async Config Saving in Spigot]
**Learning:** In Spigot plugin development, saving YAML configurations to disk using `saveConfig()` is a synchronous operation that blocks the main server thread, potentially causing lag spikes. A highly effective optimization is to serialize the configuration to a string synchronously using `getConfig().saveToString()`, and then write that string to disk asynchronously via an `ExecutorService` or `BukkitRunnable`.
**Action:** When working on Bukkit/Spigot plugins, use an asynchronous write method for configuration saves, especially in performance-sensitive areas or when config changes are frequent (e.g. on every item transaction).

## 2024-05-20 - Optimize player iteration for targeted notifications
**Learning:** Iterating through all online players (`Bukkit.getOnlinePlayers()`) and checking against a target list (like guild members) has O(N*M) complexity. Even worse if the target list is fetched inside the loop.
**Action:** For targeted notifications, iterate through the target list directly and use `Bukkit.getPlayerExact(name)` to resolve online players. This changes the complexity to O(M) and avoids unnecessary list fetches.
