package com.mcatk.guildmanager.core.repository;

import com.mcatk.guildmanager.models.Guild;
import com.mcatk.guildmanager.models.Member;

import java.util.HashMap;

public interface GuildRepository {
    void createGuildWithChairman(String id, String chairman);

    void donate(String playerId, String guildId, int cash, int contribution);

    void validateTransactionalTables();

    void createGuild(String id, String chairman);

    void updateGuild(Guild guild);

    String getPlayerGuildId(String playerId);

    Guild getGuild(String guildId);

    Member getMember(String playerId);

    void updateMember(Member member);

    void addMember(String playerId, String guildId);

    boolean removeMember(String playerId, String guildId);

    HashMap<String, Guild> getAllGuilds();
}
