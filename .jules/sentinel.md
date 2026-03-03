## 2023-10-27 - Fix unclosed PreparedStatement leaks in AtTackCraft-Core
**Vulnerability:** Found unclosed `PreparedStatement` and `ResultSet` variables in `WhiteList.java` and `PlayerListLogger.java` which are instantiated repeatedly, especially on a timer (`PlayerListLogger.run()`).
**Learning:** Legacy codebase or hastily written Bungee plugins might neglect proper lifecycle management of JDBC resources, relying on connection drops or garbage collection. This is a severe DoS risk (CWE-400 Resource Exhaustion) leading to "Too many open files" or MySQL connection starvation.
**Prevention:** Always use `try-with-resources` introduced in Java 7 for any `AutoCloseable` SQL objects.
