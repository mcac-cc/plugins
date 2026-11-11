package com.mcatk.gemshop;

import java.util.Properties;

/** What was promised at checkout, independent of subsequent catalogue changes. */
public final class Product {
    private final String category;
    private final String id;
    private final String shop;
    private final int quantity;
    private final String duration;

    private Product(String category, String id, String shop, int quantity, String duration) {
        this.category = category;
        this.id = id;
        this.shop = shop;
        this.quantity = quantity;
        this.duration = duration;
    }

    public static Product item(String shop, String item, int quantity) {
        return new Product("item", item, shop, quantity, null);
    }

    public static Product vip(String group, String duration) {
        return new Product("vip", group, null, 1, duration);
    }

    public static Product permission(String node) {
        return new Product("permission", node, null, 1, "permanent");
    }

    void writeTo(Properties record) {
        record.setProperty("category", category);
        record.setProperty("product", id);
        record.setProperty("quantity", Integer.toString(quantity));
        if (shop != null) record.setProperty("shop", shop);
        if (duration != null) record.setProperty("duration", duration);
    }
}
