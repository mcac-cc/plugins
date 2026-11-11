package com.mcatk.guildmanager.core.service;

import com.mcatk.guildmanager.core.repository.GuildRepository;
import com.mcatk.guildmanager.models.Guild;
import com.mcatk.guildmanager.models.Member;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

/** SQL mutations/refresh run on a worker. Main-thread readers never acquire the SQL monitor. */
public class GuildService {
    private final GuildRepository repository;
    private volatile Snapshot snapshot = new Snapshot(new HashMap<>());
    private volatile boolean ready;
    private static final class Snapshot {
        final HashMap<String, Guild> guilds;
        final HashMap<String, Guild> byPlayer = new HashMap<>();
        final HashMap<String, Member> members = new HashMap<>();
        Snapshot(HashMap<String, Guild> guilds) {
            this.guilds = guilds;
            for (Guild guild : guilds.values()) for (Member member : guild.getMembers()) {
                byPlayer.put(key(member.getId()), guild); members.put(key(member.getId()), member);
            }
        }
    }
    private static String key(String name) { return name == null ? "" : name.toLowerCase(Locale.ROOT); }
    public GuildService(GuildRepository repository) { this.repository = repository; }
    public boolean isReady() { return ready; }

    public synchronized void refresh() {
        HashMap<String, Guild> loaded = repository.getAllGuilds();
        snapshot = new Snapshot(loaded);
        ready = true;
    }
    public void validatePayments() { repository.validateTransactionalTables(); }
    public boolean canCreateGuild(String id, String player) {
        return repository.getGuild(id) == null && repository.getMember(player) == null;
    }
    public void createGuild(String id, String chairman) { repository.createGuildWithChairman(id, chairman); }
    public boolean canDonate(String player, String guild) {
        Member member = repository.getMember(player);
        return member != null && guild.equals(member.getGuildID());
    }
    public void donate(String player, String guild, int amount) { repository.donate(player, guild, amount / 10000, amount / 10000); }

    public Guild getPlayerGuild(String player) { return copy(snapshot.byPlayer.get(key(player))); }
    public Guild getGuild(String id) { return copy(snapshot.guilds.get(id)); }
    public Member getMember(String player) {
        Member member = snapshot.members.get(key(player));
        return member == null ? null : copy(member);
    }
    public void saveGuild(Guild guild) { repository.updateGuild(guild); }
    public void saveMember(Member member) { repository.updateMember(member); }
    public void addMember(String player, String guild) { repository.addMember(player, guild); }
    public boolean removeMember(String player, String guild) { return repository.removeMember(player, guild); }
    public HashMap<String, Guild> getGuilds() {
        HashMap<String, Guild> result = new HashMap<>();
        snapshot.guilds.forEach((id, guild) -> result.put(id, copy(guild)));
        return result;
    }
    public ArrayList<String> getGuildMembers(String id) {
        ArrayList<String> result = new ArrayList<>();
        Guild guild = snapshot.guilds.get(id);
        if (guild != null) for (Member member : guild.getMembers()) result.add(member.getId());
        return result;
    }
    public ArrayList<String> getGuildAdvancedMembers(String id) {
        ArrayList<String> result = new ArrayList<>();
        Guild guild = snapshot.guilds.get(id);
        if (guild != null) for (Member member : guild.getMembers()) if (member.isAdvanced()) result.add(member.getId());
        return result;
    }
    private static Member copy(Member member) {
        return new Member(member.getId(), member.getGuildID(), member.getContribution(), member.isAdvanced());
    }
    private static Guild copy(Guild original) {
        if (original == null) return null;
        Guild guild = new Guild(); guild.setId(original.getId()); guild.setGuildName(original.getGuildName());
        guild.setChairman(original.getChairman()); guild.setViceChairman1(original.getViceChairman1());
        guild.setViceChairman2(original.getViceChairman2()); guild.setLevel(original.getLevel());
        guild.setPoints(original.getPoints()); guild.setCash(original.getCash());
        guild.setResidenceFLag(original.getResidenceFLag()); guild.setHasChangedName(original.getHasChangedName());
        ArrayList<Member> members = new ArrayList<>();
        for (Member member : original.getMembers()) members.add(copy(member));
        guild.setMembers(members); return guild;
    }
}
