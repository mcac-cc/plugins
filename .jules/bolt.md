## 2026-02-26 - [Redundant DB Query in GemExecutor]
**Learning:** Found a redundant database query in `GemExecutor.addGems` where `MySQLManager.getInstance().getGems(name)` was called twice, resulting in an extra SELECT query per gem addition.
**Action:** Removed the redundant call by using the local variable `gems` which already held the result. This optimization saves one database roundtrip per call.

## 2026-03-06 - [GuildManager LoginListener Notification Bottleneck]
**Learning:** Sending notifications to online guild members inside `LoginListener` previously iterated over all online players on the server (O(N)), inside of which it fetched the list of guild members and performed a linear search string comparison (O(M)). In highly-populated servers, iterating `Bukkit.getOnlinePlayers()` inside a listener can cause main-thread latency, especially when doing memory list queries inside the loop.
**Action:** Replaced O(N*M) check with an O(M) check by iterating strictly over the guild members list and resolving their `Player` objects with constant-time hashmap lookups via `Bukkit.getPlayerExact()`.
