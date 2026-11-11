package com.mcatk.gem;

import com.mcatk.gem.sql.MySQLManager;

import java.sql.SQLException;

/** Blocking database API. Invoke from a worker; Bukkit callers use Gem.queryAsync. */
public class GemExecutor {
    public boolean isReady() { return MySQLManager.getInstance().isReady(); }
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
        MySQLManager.getInstance().upsertGems(name, gems);
        Gem.getPlugin().invalidateBalance(name);
        Gem.getPlugin().log(name + "宝石设置为" + gems);
    }


    public boolean takeGems(String name, int gems) {
        if (gems <= 0) {
            return false;
        }
        if (MySQLManager.getInstance().reduceGems(name, gems)) {
            Gem.getPlugin().invalidateBalance(name);
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
        MySQLManager.getInstance().addGems(name, addGems);
        Gem.getPlugin().invalidateBalance(name);
        Gem.getPlugin().log(name + "获得宝石" + addGems);
    }

    public boolean refundGems(String name, int gems) {
        requirePositiveAmount(gems);
        boolean refunded = MySQLManager.getInstance().refundGems(name, gems);
        if (refunded) {
            Gem.getPlugin().invalidateBalance(name);
            Gem.getPlugin().log(name + "退回宝石" + gems);
        }
        return refunded;
    }

    public boolean deleteData(String name) {
        if (MySQLManager.getInstance().deleteData(name)) {
            Gem.getPlugin().invalidateBalance(name);
            Gem.getPlugin().log(name + "宝石数据已删除");
            return true;
        }
        return false;
    }
}
