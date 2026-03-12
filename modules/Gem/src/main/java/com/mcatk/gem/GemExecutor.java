package com.mcatk.gem;

import com.mcatk.gem.sql.MySQLManager;

import java.sql.SQLException;

public class GemExecutor {
    private static void requirePositiveAmount(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("gems must be positive");
        }
    }

    public Integer getGems(String name) {
        Integer gems = MySQLManager.getInstance().getGems(name);
        if (gems == null) {
            return 0;
        }
        return gems;
    }

    public void setGems(String name, int gems) {
        requirePositiveAmount(gems);
        if (MySQLManager.getInstance().getGems(name) == null) {
            MySQLManager.getInstance().insertData(name);
        }
        MySQLManager.getInstance().setGems(name, gems);
        Gem.getPlugin().log(name + "宝石设置为" + gems);
    }


    public boolean takeGems(String name, int gems) {
        if (gems <= 0) {
            return false;
        }
        if (MySQLManager.getInstance().reduceGems(name, gems)) {
            Gem.getPlugin().log(name + "花费宝石" + gems);
            return true;
        }
        return false;
    }

    public Integer getTotalGems(String name) {
        Integer gems = MySQLManager.getInstance().getTotal(name);
        if (gems == null) {
            return 0;
        }
        return gems;
    }

    public void addGems(String name, int addGems) {
        requirePositiveAmount(addGems);
        int[] data = MySQLManager.getInstance().getData(name);
        if (data == null) {
            MySQLManager.getInstance().insertData(name);
            data = MySQLManager.getInstance().getData(name);
            if (data == null) {
                data = new int[]{0, 0};
            }
        }
        int gems = data[0] + addGems;
        int total = data[1] + addGems;
        MySQLManager.getInstance().updateData(name, gems, total);
        Gem.getPlugin().log(name + "获得宝石" + addGems);
    }
}
