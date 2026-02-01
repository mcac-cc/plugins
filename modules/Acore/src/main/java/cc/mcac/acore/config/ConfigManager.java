package cc.mcac.acore.config;

import com.google.common.io.ByteStreams;
import org.slf4j.Logger;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class ConfigManager {

    private final Path dataDir;
    private final Path configPath;
    private final YamlConfigurationLoader loader;
    private final Logger logger;

    private PluginConfig cached;

    public ConfigManager(Logger logger, Path dataDir) {
        this.logger = logger;
        this.dataDir = dataDir;
        this.configPath = dataDir.resolve("config.yml");
        this.loader = YamlConfigurationLoader.builder()
                .path(this.configPath)
                .build();
        try {
            initIfNeeded();
            load();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to initialize configuration", ex);
        }
    }

    public void initIfNeeded() throws IOException {
        if (Files.notExists(dataDir)) {
            Files.createDirectories(dataDir);
        }
        if (Files.notExists(configPath)) {
            try (InputStream in = getClass().getResourceAsStream("/config.yml")) {
                if (in == null) {
                    throw new IOException("Missing default /config.yml in resources");
                }
                Files.write(configPath, ByteStreams.toByteArray(in), StandardOpenOption.CREATE_NEW);
            }
            logger.info("Successfully initiated configuration at {}", configPath);
        }
    }

    public void load() throws IOException {
        ConfigurationNode node = loader.load();
        this.cached = node.get(PluginConfig.class);
        if (this.cached == null) {
            this.cached = new PluginConfig(); // fallback to defaults
        }
        logger.info("Successfully loaded configuration at {}", configPath);
    }

    public PluginConfig getPluginConfig() {
        return this.cached;
    }

}
