package com.mcatk.guildmanager.core.service;

import com.mcatk.guildmanager.core.repository.GuildRepository;
import com.mcatk.guildmanager.models.Guild;
import com.mcatk.guildmanager.models.Member;

import java.util.ArrayList;
import java.util.HashMap;

public class GuildService {
    private final GuildRepository repository;
    private HashMap<String, Guild> guilds = new HashMap<>();

    public GuildService(GuildRepository repository) {
        this.repository = repository;
        refresh();
    }

    public synchronized void refresh() {
        guilds = repository.getAllGuilds();
    }

    // ⚡ Bolt: Instead of refreshing the entire cache from the database (O(N) operation),
    // we fetch only the modified entity and update the map directly to improve performance.
    public synchronized void createGuild(String id, String chairman) {
        repository.createGuild(id, chairman);
        Guild guild = repository.getGuild(id);
        if (guild != null) {
            guilds.put(id, guild);
        } else {
            refresh();
        }
    }

    public synchronized Guild getPlayerGuild(String playerId) {
        String guildId = repository.getPlayerGuildId(playerId);
        if (guildId == null) {
            return null;
        }
        Guild guild = guilds.get(guildId);
        if (guild == null) {
            refresh();
            return guilds.get(guildId);
        }
        return guild;
    }

    public synchronized Guild getGuild(String guildId) {
        return guilds.get(guildId);
    }

    // ⚡ Bolt: Instead of refreshing the entire cache from the database (O(N) operation),
    // we fetch only the modified entity and update the map directly to improve performance.
    public synchronized void saveGuild(Guild guild) {
        repository.updateGuild(guild);
        // ⚡ Bolt: Update cache directly to avoid an O(N) refresh or extra DB read.
        guilds.put(guild.getId(), guild);
    }

    public synchronized Member getMember(String playerId) {
        return repository.getMember(playerId);
    }

    // ⚡ Bolt: Instead of refreshing the entire cache from the database (O(N) operation),
    // we fetch only the modified entity and update the map directly to improve performance.
    public synchronized void saveMember(Member member) {
        repository.updateMember(member);
        // ⚡ Bolt: Update member in cache directly instead of refreshing/fetching from DB.
        // This prevents an N+1 query bottleneck on frequent operations like donations.
        Guild guild = guilds.get(member.getGuildID());
        if (guild != null) {
            ArrayList<Member> members = guild.getMembers();
            for (int i = 0; i < members.size(); i++) {
                if (members.get(i).getId().equals(member.getId())) {
                    members.set(i, member);
                    break;
                }
            }
        }
    }

    // ⚡ Bolt: Instead of refreshing the entire cache from the database (O(N) operation),
    // we fetch only the modified entity and update the map directly to improve performance.
    public synchronized void addMember(String playerId, String guildId) {
        repository.addMember(playerId, guildId);
        Guild updatedGuild = repository.getGuild(guildId);
        if (updatedGuild != null) {
            guilds.put(guildId, updatedGuild);
        } else {
            refresh();
        }
    }

    // ⚡ Bolt: Instead of refreshing the entire cache from the database (O(N) operation),
    // we fetch only the modified entity and update the map directly to improve performance.
    public synchronized void removeMember(String playerId, String guildId) {
        repository.removeMember(playerId, guildId);
        Guild updatedGuild = repository.getGuild(guildId);
        if (updatedGuild != null) {
            guilds.put(guildId, updatedGuild);
        } else {
            refresh();
        }
    }

    public synchronized ArrayList<String> getGuildMembers(String guildId) {
        ArrayList<String> list = new ArrayList<>();
        Guild guild = guilds.get(guildId);
        if (guild == null) {
            return list;
        }
        for (Member m : guild.getMembers()) {
            list.add(m.getId());
        }
        return list;
    }

    public synchronized ArrayList<String> getGuildAdvancedMembers(String guildId) {
        ArrayList<String> list = new ArrayList<>();
        Guild guild = guilds.get(guildId);
        if (guild == null) {
            return list;
        }
        for (Member m : guild.getMembers()) {
            if (m.isAdvanced()) {
                list.add(m.getId());
            }
        }
        return list;
    }

    public synchronized HashMap<String, Guild> getGuilds() {
        return guilds;
    }
}
