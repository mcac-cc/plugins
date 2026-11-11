package com.mcatk.gem.sql;

import org.junit.Test;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.UUID;
import java.util.concurrent.*;
import static org.junit.Assert.*;

public class GemBalanceTest {
    private String database() throws Exception {
        String url="jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1";
        try(Connection c=DriverManager.getConnection(url,"sa","");Statement s=c.createStatement()) {
            s.execute("CREATE TABLE gem(username VARCHAR(64) PRIMARY KEY,gems INT DEFAULT 0,total INT DEFAULT 0)");
        }return url;
    }
    private MySQLManager manager(String url) throws Exception {
        MySQLManager manager=new MySQLManager();
        Field enabled=MySQLManager.class.getDeclaredField("enabled"),connection=MySQLManager.class.getDeclaredField("connection");
        enabled.setAccessible(true);connection.setAccessible(true);enabled.set(manager,true);
        connection.set(manager,DriverManager.getConnection(url,"sa",""));return manager;
    }
    @Test public void refundRestoresBalanceWithoutInflatingLifetimeAwards() throws Exception {
        MySQLManager manager=manager(database());
        try {
            manager.addGems("Alice",50);assertTrue(manager.reduceGems("Alice",10));assertTrue(manager.refundGems("Alice",10));
            assertArrayEquals(new int[]{50,50},manager.getData("Alice"));
            assertFalse(manager.refundGems("missing",10));assertNull(manager.getData("missing"));
        } finally {manager.shutdown();}
    }
    @Test public void concurrentAwardersCannotOverwriteEachOther() throws Exception {
        String url=database();MySQLManager first=manager(url),second=manager(url);
        CountDownLatch start=new CountDownLatch(1);ExecutorService workers=Executors.newFixedThreadPool(2);
        try {
            Future<?> a=workers.submit(()->{try{start.await();}catch(InterruptedException e){throw new RuntimeException(e);}for(int i=0;i<25;i++)first.addGems("Alice",1);});
            Future<?> b=workers.submit(()->{try{start.await();}catch(InterruptedException e){throw new RuntimeException(e);}for(int i=0;i<25;i++)second.addGems("Alice",1);});
            start.countDown();a.get(5,TimeUnit.SECONDS);b.get(5,TimeUnit.SECONDS);
            assertArrayEquals(new int[]{50,50},first.getData("Alice"));
        } finally {workers.shutdownNow();first.shutdown();second.shutdown();}
    }
    @Test public void settingBalanceDoesNotResetLifetimeAndOverflowDoesNotWrap() throws Exception {
        MySQLManager manager=manager(database());
        try {
            manager.upsertGems("Alice",10);manager.addGems("Alice",5);manager.upsertGems("Alice",20);
            assertArrayEquals(new int[]{20,5},manager.getData("Alice"));
            assertThrows(IllegalStateException.class,()->manager.addGems("Alice",Integer.MAX_VALUE));
            assertArrayEquals(new int[]{20,5},manager.getData("Alice"));
        } finally {manager.shutdown();}
    }
    @Test public void legacyNullBalancesAreTreatedAsZeroWhenAwarding() throws Exception {
        String url=database();MySQLManager manager=manager(url);
        try {
            try(Connection c=DriverManager.getConnection(url,"sa","");Statement s=c.createStatement()) {
                s.execute("INSERT INTO gem(username,gems,total) VALUES('Alice',NULL,NULL)");
            }
            manager.addGems("Alice",10);
            assertArrayEquals(new int[]{10,10},manager.getData("Alice"));
        } finally {manager.shutdown();}
    }
}
