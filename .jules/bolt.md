## 2024-05-15 - [Async Config Saving in Spigot]
**Learning:** In Spigot plugin development, saving YAML configurations to disk using `saveConfig()` is a synchronous operation that blocks the main server thread, potentially causing lag spikes. A highly effective optimization is to serialize the configuration to a string synchronously using `getConfig().saveToString()`, and then write that string to disk asynchronously via an `ExecutorService` or `BukkitRunnable`.
**Action:** When working on Bukkit/Spigot plugins, use an asynchronous write method for configuration saves, especially in performance-sensitive areas or when config changes are frequent (e.g. on every item transaction).

## 2024-05-20 - Optimize player iteration for targeted notifications
**Learning:** Iterating through all online players (`Bukkit.getOnlinePlayers()`) and checking against a target list (like guild members) has O(N*M) complexity. Even worse if the target list is fetched inside the loop.
**Action:** For targeted notifications, iterate through the target list directly and use `Bukkit.getPlayerExact(name)` to resolve online players. This changes the complexity to O(M) and avoids unnecessary list fetches.
