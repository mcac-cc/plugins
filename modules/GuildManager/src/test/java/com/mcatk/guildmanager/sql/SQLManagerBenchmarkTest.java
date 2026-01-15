package com.mcatk.guildmanager.sql;

import com.mcatk.guildmanager.models.Guild;
import com.mcatk.guildmanager.models.Member;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SQLManagerBenchmarkTest {

    @Test
    public void testBenchmark() throws Exception {
        // Setup
        HashMap<String, Guild> guilds = new HashMap<>();
        String guildId = "test_guild";
        Guild guild = new Guild();
        guild.setId(guildId);

        ArrayList<Member> members = new ArrayList<>();
        int memberCount = 1000;
        for (int i = 0; i < memberCount; i++) {
            members.add(new Member("player_" + i, guildId, 0, false));
        }
        guild.setMembers(members);
        guilds.put(guildId, guild);

        // Inject into SQLManager
        SQLManager instance = new SQLManager(guilds);
        Field f = SQLManager.class.getDeclaredField("instance");
        f.setAccessible(true);
        f.set(null, instance);

        // Warmup
        for (int i = 0; i < 10000; i++) {
            SQLManager.getInstance().getGuildMembers(guildId);
        }

        // Benchmark
        long start = System.nanoTime();
        int iterations = 100000;
        for (int i = 0; i < iterations; i++) {
            List<String> result = SQLManager.getInstance().getGuildMembers(guildId);
            if (result.size() != memberCount) {
                throw new RuntimeException("Size mismatch");
            }
        }
        long end = System.nanoTime();

        double durationMs = (end - start) / 1_000_000.0;
        System.out.println("Benchmark finished in " + durationMs + " ms for " + iterations + " iterations.");
        System.out.println("Average time per call: " + (durationMs / iterations) + " ms");
    }
}
