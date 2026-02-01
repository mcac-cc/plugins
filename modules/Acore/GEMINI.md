# Project Overview

This is a Java project for a Velocity Minecraft proxy server plugin named "Acore". The plugin's main purpose is to collect and store player data from the proxy to a MySQL database. It also appears to have integration with a QQ bot using MiraiMC.

**Key Technologies:**

*   **Java 17**
*   **Gradle** build tool
*   **Velocity API**: For Minecraft proxy plugin development
*   **MySQL**: For database storage
*   **HikariCP**: For database connection pooling
*   **MiraiMC**: For QQ bot integration

**Architecture:**

The project follows a standard Java plugin structure. The main class, `Acore.java`, handles initialization, configuration loading, database connection, event listener registration, and task scheduling. The configuration is managed through a `config.yml` file. A scheduled task, `PlayerListTask`, periodically updates the player list to the database. The `MessageListener` class suggests integration with a messaging platform, likely a QQ bot, to interact with the server.

# Building and Running

**Building the project:**

The project can be built using the included Gradle wrapper. The following command will compile the code and create a JAR file in the `build/libs` directory:

```bash
./gradlew build
```

**Running the project:**

This is a plugin for the Velocity Minecraft proxy. To run it, you need to:

1.  Build the plugin JAR file using the command above.
2.  Install a Velocity proxy server.
3.  Place the generated JAR file into the `plugins` directory of your Velocity server.
4.  Configure the `config.yml` file in the plugin's data directory (`plugins/Acore/config.yml`) with your database credentials.
5.  Start the Velocity server.

# Development Conventions

*   **Configuration:** The plugin uses a `config.yml` file for configuration. The `ConfigManager` class is responsible for loading and managing the configuration.
*   **Database:** The plugin uses a MySQL database to store data. The `DatabaseManager` class handles the database connection using HikariCP.
*   **Tasks:** The plugin uses scheduled tasks for periodic operations, such as updating the player list. The `PlayerListTask` is an example of such a task.
*   **Dependencies:** Dependencies are managed using Gradle. The `build.gradle` file lists all the project dependencies.
