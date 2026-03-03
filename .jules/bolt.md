## 2024-03-03 - O(N) Main Thread Database Read on Entity Updates (GuildService)
**Learning:** `refresh()` methods in single-threaded components (like Spigot plugins) that hit the DB sequentially for O(N) rows can cause severe main-thread lag when invoked on frequent events (like saving user stats).
**Action:** Replace `refresh()` calls with precise, O(1) in-memory cache updates (e.g., `map.put(id, entity)`) inside `saveEntity()` operations to maintain coherence without hitting the database.
