## 2026-02-26 - [Redundant DB Query in GemExecutor]
**Learning:** Found a redundant database query in `GemExecutor.addGems` where `MySQLManager.getInstance().getGems(name)` was called twice, resulting in an extra SELECT query per gem addition.
**Action:** Removed the redundant call by using the local variable `gems` which already held the result. This optimization saves one database roundtrip per call.
