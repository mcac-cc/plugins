package com.mcatk.guildmanager.core.repository;

import com.mcatk.guildmanager.models.Guild;
import com.mcatk.guildmanager.models.Member;

import java.util.HashMap;

public interface GuildRepository {
    void createGuild(String id, String chairman);

    void updateGuild(Guild guild);

    String getPlayerGuildId(String playerId);

    Guild getGuild(String guildId);

    Member getMember(String playerId);

    void updateMember(Member member);

    void addMember(String playerId, String guildId);

    void removeMember(String playerId, String guildId);

    HashMap<String, Guild> getAllGuilds();
}
