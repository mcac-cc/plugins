package com.mcatk.acshop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mcatk.acshop.shop.Shops;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class FileOperation {
    
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File shopsFile;
    
    public FileOperation() {
        this(AcShop.getPlugin().getDataFolder());
    }

    public FileOperation(File dataFolder) {
        this.shopsFile = new File(dataFolder, "shopsFile.json");
    }
    
    public void saveShops(Shops shops) {
        try (FileWriter writer = new FileWriter(shopsFile)) {
            gson.toJson(shops, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveShopsAsync(Shops shops) {
        final String json = gson.toJson(shops);
        org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(AcShop.getPlugin(), () -> {
            try (FileWriter writer = new FileWriter(shopsFile)) {
                writer.write(json);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
    
    public Shops loadShops() {
        Shops shops = null;
        try {
            if (shopsFile.exists()) {
                FileReader reader = new FileReader(shopsFile);
                shops = gson.fromJson(reader, Shops.class);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return shops == null ? new Shops() : shops;
    }
    
}
