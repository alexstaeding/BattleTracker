package org.battleplugins.tracker.tracking;

import lombok.AccessLevel;
import lombok.Getter;

import org.battleplugins.api.configuration.Configuration;
import org.battleplugins.api.entity.living.player.OfflinePlayer;
import org.battleplugins.tracker.BattleTracker;
import org.battleplugins.tracker.member.repository.MemberRepository;
import org.battleplugins.tracker.model.member.Member;
import org.battleplugins.tracker.sql.SQLInstance;
import org.battleplugins.tracker.tracking.message.DeathMessageManager;
import org.battleplugins.tracker.tracking.recap.RecapManager;
import org.battleplugins.tracker.tracking.stat.StatTypes;
import org.battleplugins.tracker.tracking.stat.calculator.RatingCalculator;
import org.battleplugins.tracker.tracking.stat.record.PlayerMember;
import org.battleplugins.tracker.tracking.stat.tally.VersusTally;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Main implementation of a tracker instance. Any plugin
 * wanting to track data should extend this class or
 * use it as the implementation.
 *
 * @author Redned
 */
@Getter
public class Tracker implements MemberRepository {

    protected String name;

    protected DeathMessageManager deathMessageManager;
    protected RecapManager recapManager;
    protected RatingCalculator ratingCalculator;

    protected Map<UUID, Member> records;
    protected List<VersusTally> versusTallies;

    @Getter(AccessLevel.NONE)
    protected SQLInstance sql;

    public Tracker(BattleTracker plugin, String name, Configuration config, RatingCalculator calculator) {
        this(plugin, name, config, calculator, null);
    }

    public Tracker(BattleTracker plugin, String name, Configuration config, RatingCalculator calculator, SQLInstance sqlInstance) {
        this.name = name;
        this.recapManager = new RecapManager(plugin);
        this.deathMessageManager = new DeathMessageManager(plugin, this, config);
        this.ratingCalculator = calculator;
        this.records = new HashMap<>();
        this.versusTallies = new ArrayList<>();
        if (sqlInstance == null) {
            sqlInstance = new SQLInstance(this);
        }
        this.sql = sqlInstance;
    }

    @Override
    public int getRecordCount() {
        return records.size();
    }

    @Override
    public boolean hasRecord(OfflinePlayer player) {
        return records.containsKey(player.getUniqueId());
    }

    @Override
    public Optional<Member> getOne(OfflinePlayer player) {
        return Optional.ofNullable(records.get(player.getUniqueId()));
    }

    @Override
    public boolean hasVersusTally(OfflinePlayer player) {
        for (VersusTally tally : versusTallies) {
            if (tally.getId1().equals(player.getUniqueId().toString()))
                return true;

            if (tally.getId2().equals(player.getUniqueId().toString()))
                return true;
        }

        return false;
    }

    @Override
    public Optional<VersusTally> getVersusTally(OfflinePlayer player1, OfflinePlayer player2) {
        for (VersusTally tally : versusTallies) {
            if (tally.getId1().equals(player1.getUniqueId().toString()) &&
                    tally.getId2().equals(player2.getUniqueId().toString())) {

                return Optional.of(tally);
            }

            if (tally.getId2().equals(player1.getUniqueId().toString()) &&
                    tally.getId1().equals(player2.getUniqueId().toString())) {

                return Optional.of(tally);
            }
        }

        return Optional.empty();
    }

    @Override
    public VersusTally createNewVersusTally(OfflinePlayer player1, OfflinePlayer player2) {
        VersusTally versusTally = new VersusTally(this, player1, player2, new HashMap<>());
        versusTallies.add(versusTally);
        return versusTally;
    }

    @Override
    public void setValue(String statType, float value, OfflinePlayer player) {
        Member member = records.get(player.getUniqueId());
        member.setValue(statType, value);
    }

    @Override
    public void updateRating(OfflinePlayer killer, OfflinePlayer killed, boolean tie) {
        Member killerMember = getOrCreateRecord(killer);
        Member killedMember = getOrCreateRecord(killed);
        ratingCalculator.updateRating(killerMember, killedMember, tie);

        float killerRating = killerMember.getRating();
        float killerMaxRating = killerMember.getStat(StatTypes.MAX_RATING);

        setValue(StatTypes.RATING, killerMember.getRating(), killer);
        setValue(StatTypes.RATING, killedMember.getRating(), killed);

        if (killerRating > killerMaxRating)
            setValue(StatTypes.MAX_RATING, killerRating, killer);

        if (tie) {
            incrementValue(StatTypes.TIES, killer);
            incrementValue(StatTypes.TIES, killed);
        }

        setValue(StatTypes.KD_RATIO, killerMember.getStat(StatTypes.KILLS) / killerMember.getStat(StatTypes.DEATHS), killer);
        setValue(StatTypes.KD_RATIO, killedMember.getStat(StatTypes.KILLS) / killedMember.getStat(StatTypes.DEATHS), killed);

        float killerKdr = killerMember.getStat(StatTypes.KD_RATIO);
        float killerMaxKdr = killerMember.getStat(StatTypes.MAX_KD_RATIO);

        if (killerKdr > killerMaxKdr)
            setValue(StatTypes.MAX_KD_RATIO, killerKdr, killer);

        setValue(StatTypes.STREAK, 0, killed);
        incrementValue(StatTypes.STREAK, killer);

        float killerStreak = killerMember.getStat(StatTypes.STREAK);
        float killerMaxStreak = killerMember.getStat(StatTypes.MAX_STREAK);

        if (killerStreak > killerMaxStreak)
            setValue(StatTypes.MAX_STREAK, killerStreak, killer);
    }

    @Override
    public Member createNewRecord(OfflinePlayer player) {
        Map<String, Float> columns = new HashMap<>();
        for (String column : sql.getOverallColumns()) {
            columns.put(column, 0f);
        }

        Member member = new PlayerMember(this, player.getUniqueId().toString(), player.getName(), columns);
        return createNewRecord(player, member);
    }

    @Override
    public Member createNewRecord(OfflinePlayer player, Member member) {
        member.setRating(ratingCalculator.getDefaultRating());
        records.put(player.getUniqueId(), member);
        return member;
    }

    @Override
    public void removeRecord(OfflinePlayer player) {
        records.remove(player.getUniqueId());

        save(player);
    }

    @Override
    public void save(OfflinePlayer player) {
        sql.save(player.getUniqueId());
    }

    @Override
    public void saveAll() {
        sql.saveAll();
    }
}
