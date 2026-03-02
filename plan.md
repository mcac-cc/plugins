1. **Analyze Security Issues in `AtTackCraft-Core`:**
   - In `modules/AtTackCraft-Core/src/main/java/cc/mcac/attackcraftcore/Bungee/PlayerListLogger.java`, the `PreparedStatement` is created inside a task but never closed, leading to resource leaks.
   - In `modules/AtTackCraft-Core/src/main/java/cc/mcac/attackcraftcore/Bungee/WhiteList/WhiteList.java`, the `PreparedStatement` and `ResultSet` are created but never closed, leading to resource leaks.
2. **Implement Fix in `PlayerListLogger.java`:**
   - Modify the `run` method in `PlayerListLogger.java` to use a `try-with-resources` block for `PreparedStatement`.
3. **Implement Fix in `WhiteList.java`:**
   - Modify the `on` method in `WhiteList.java` to use `try-with-resources` blocks for `PreparedStatement` and `ResultSet`.
   - Modify the `addPlayer` method in `WhiteList.java` to use a `try-with-resources` block for `PreparedStatement`.
4. **Pre-commit step:** Follow instructions to make sure proper testing, verifications, reviews and reflections are done.
5. **Submit Changes:** Ensure standard practices are followed.
