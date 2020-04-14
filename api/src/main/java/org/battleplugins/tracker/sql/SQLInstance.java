package org.battleplugins.tracker.sql;

import mc.alk.battlecore.serializers.SQLSerializer;
import mc.alk.battlecore.util.Log;

import org.battleplugins.api.scheduler.Scheduler;
import org.battleplugins.tracker.member.repository.MemberRepository;
import org.battleplugins.tracker.model.member.Member;
import org.battleplugins.tracker.tracking.stat.StatType;
import org.battleplugins.tracker.tracking.stat.StatTypes;
import org.battleplugins.tracker.tracking.stat.record.PlayerMember;
import org.battleplugins.tracker.tracking.stat.tally.VersusTally;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Main SQL instance for Trackers.
 *
 * @author alkarinv, Redned
 */
public class SQLInstance extends SQLSerializer {

    public static String TABLE_PREFIX;

    public static String DATABASE;
    public static String URL;
    public static String PORT;
    public static String USERNAME;
    public static String PASSWORD;

    public static String TYPE;

    private static final int MAX_LENGTH = 100;

    private String overallTable;
    private String tallyTable;
    private String versusTable;

    private MemberRepository tracker;

    private List<String> overallColumns;
    private List<String> versusColumns;

    public SQLInstance(MemberRepository tracker) {
        this(tracker, Stream.of(StatTypes.values().toArray(new StatType[0])).filter(StatType::isTracked).map(StatType::getInternalName).collect(Collectors.toList()),
                Arrays.asList(StatTypes.KILLS.getInternalName(), StatTypes.DEATHS.getInternalName(), StatTypes.TIES.getInternalName()));
    }

    public SQLInstance(MemberRepository tracker, List<String> overallColumns, List<String> versusColumns) {
        this.overallColumns = overallColumns;
        this.versusColumns = versusColumns;

        setTables(tracker);
    }

    public void setTables(MemberRepository tracker) {
       this.tracker = tracker;

       this.overallTable = TABLE_PREFIX + tracker.getName().toLowerCase() + "_overall";
       this.tallyTable = TABLE_PREFIX + tracker.getName().toLowerCase() + "_tally";
       this.versusTable = TABLE_PREFIX + tracker.getName().toLowerCase() + "_versus";

       init();
    }

    @Override
    protected boolean init() {
        setDB(DATABASE);
        setType(SQLType.valueOf(TYPE.toUpperCase()));
        setURL(URL);
        setPort(PORT);
        setUsername(USERNAME);
        setPassword(PASSWORD);

        super.init();

        setupOverallTable();
        setupVersusTable();

        try {
            // TODO: Don't put all records in cache and add a way to flush
            RSCon overallRsCon = executeQuery("SELECT * FROM " + overallTable);
            createRecords(overallRsCon).whenComplete((records, exception) -> Scheduler.scheduleAsynchrounousTask(() ->
                    records.forEach(record -> tracker.getRecords().put(UUID.fromString(record.getId()), record))));

            // TODO: Don't put all records in cache and add a way to flush
            RSCon versusRsCon = executeQuery("SELECT * FROM " + versusTable);
            createVersusTallies(versusRsCon).whenComplete((tallies, exception ) -> Scheduler.scheduleAsynchrounousTask(() ->
                    tallies.forEach(tally -> tracker.getVersusTallies().add(tally))));
        } catch (Exception ex) {
            Log.err("Failed to generate info from tables!");
            ex.printStackTrace();
        }
        return true;
    }

    public CompletableFuture<List<Member>> createRecords(RSCon rsCon) {
        CompletableFuture<List<Member>> future = new CompletableFuture<>();
        List<Member> members = new ArrayList<>();
        if (rsCon == null) {
            future.complete(members);
            return future;
        }

        try {
            ResultSet resultSet = rsCon.rs;
            while (resultSet.next()) {
                members.add(createRecord(rsCon).get());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            closeConnection(rsCon);
        }

        future.complete(members);
        return future;
    }

    public CompletableFuture<Member> createRecord(RSCon rsCon) throws SQLException {
        CompletableFuture<Member> future = new CompletableFuture<>();

        ResultSet resultSet = rsCon.rs;
        Map<String, Float> columns = new HashMap<>();
        for (String column : overallColumns) {
            columns.put(column, Float.parseFloat(resultSet.getString(column)));
        }

        future.complete(new PlayerMember(tracker, resultSet.getString("id"), resultSet.getString("name"), columns));
        return future;
    }

    public CompletableFuture<Member> getRecord(UUID id) {
        CompletableFuture<Member> future = new CompletableFuture<>();
        RSCon rsCon = executeQuery("SELECT * FROM " + overallTable + " WHERE id = ?", id.toString());
        try {
            ResultSet resultSet = rsCon.rs;
            while (resultSet.next()){
                return createRecord(rsCon);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            closeConnection(rsCon);
        }
        return future;
    }

    public CompletableFuture<List<VersusTally>> createVersusTallies(RSCon rsCon) {
        CompletableFuture<List<VersusTally>> future = new CompletableFuture<>();
        List<VersusTally> tallies = new ArrayList<>();
        if (rsCon == null) {
            future.complete(tallies);
            return future;
        }

        try {
            ResultSet resultSet = rsCon.rs;
            while (resultSet.next()) {
                tallies.add(createVersusTally(rsCon).get());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            closeConnection(rsCon);
        }

        future.complete(tallies);
        return future;
    }

    public CompletableFuture<VersusTally> createVersusTally(RSCon rsCon) throws SQLException {
        CompletableFuture<VersusTally> future = new CompletableFuture<>();

        ResultSet resultSet = rsCon.rs;
        Map<String, Float> columns = new HashMap<>();
        for (String column : versusColumns) {
            if (column.equalsIgnoreCase("infinity")) { // sometimes kdr gets saved as 'infinity'
                columns.put(column, Float.POSITIVE_INFINITY);
                continue;
            }

            columns.put(column, Float.parseFloat(resultSet.getString(column)));
        }

        future.complete(new VersusTally(tracker,
                resultSet.getString("id1"),
                resultSet.getString("id2"),
                resultSet.getString("name1"),
                resultSet.getString("name2"),
                columns));

        System.out.println("Created vs tally with " + resultSet.getString("name1") + " with columns " + columns);
        return future;
    }

    public CompletableFuture<Member> getVersusTally(UUID id1, UUID id2) {
        CompletableFuture<Member> future = new CompletableFuture<>();
        // We need to check if id1 is in place of id2 and vice versa
        RSCon rsCon = executeQuery("SELECT * FROM " + versusTable + " WHERE (id1 = ? AND id2 = ?) OR (id1 = ? AND id2 = ?)", id1.toString(), id2.toString(), id2.toString(), id1.toString());
        try {
            ResultSet resultSet = rsCon.rs;
            while (resultSet.next()){
                return createRecord(rsCon);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            closeConnection(rsCon);
        }
        return future;
    }

    public void save(UUID uuid) {
        saveTotals(new UUID[]{uuid});
    }

    public void saveAll() {
        saveTotals(tracker.getRecords().keySet().toArray(new UUID[0]));
    }

    public void saveTotals(UUID[] uuids) {
        if (uuids == null || uuids.length == 0)
            return;

        List<List<Object>> overallBatch = new ArrayList<>();
        List<List<Object>> versusBatch = new ArrayList<>();
        for (UUID uuid : uuids) {
            Member member = tracker.getRecords().get(uuid);
            if (member.getRating() < 0 || member.getRating() > 200000)
                Log.err("ELO out of range: " + member.getRating() + " with record " + member);

            // +2 in array for name and id
            String[] overallObjectArray = new String[overallColumns.size() + 2];
            overallObjectArray[0] = member.getName();
            overallObjectArray[1] = member.getId();
            for (int i = 0; i < overallColumns.size(); i++) {
                String overallColumn = overallColumns.get(i);
                overallObjectArray[i + 2] = member.getStats().get(overallColumn).toString();
            }

            overallBatch.add(Arrays.asList(overallObjectArray));
            executeBatch(true, constructInsertOverallStatement(), overallBatch);

            for (VersusTally versusTally : tracker.getVersusTallies()) {
                if (!versusTally.getId1().equals(uuid.toString()) && !versusTally.getId2().equals(uuid.toString()))
                    continue;

                // +4 in array for double name and id
                String[] versusObjectArray = new String[versusColumns.size() + 4];
                versusObjectArray[0] = versusTally.getId1();
                versusObjectArray[1] = versusTally.getName1();
                versusObjectArray[2] = versusTally.getId2();
                versusObjectArray[3] = versusTally.getName2();

                for (int i = 0; i < versusColumns.size(); i++) {
                    String versusColumn = versusColumns.get(i);
                    versusObjectArray[i + 4] = Optional.ofNullable(versusTally.getStats().get(versusColumn)).orElse(0f).toString();
                }

                versusBatch.add(Arrays.asList(versusObjectArray));
                executeBatch(true, constructInsertVersusStatement(), versusBatch);
            }
        }
    }

    public List<String> getOverallColumns() {
        return overallColumns;
    }

    private String constructInsertOverallStatement() {
        StringBuilder builder = new StringBuilder();
        switch (getType()) {
            case MYSQL:
                String insertOverall = "INSERT INTO " + overallTable + " VALUES (?, ?, ";
                builder.append(insertOverall);
                for (int i = 0; i < overallColumns.size(); i++) {
                    if ((i + 1) < overallColumns.size())
                        builder.append("?, ");
                    else
                        builder.append("?)");
                }

                builder.append(" ON DUPLICATE KEY UPDATE ");
                builder.append("name = VALUES(name), ");
                builder.append("id = VALUES(id), ");
                for (int i = 0; i < overallColumns.size(); i++) {
                    if ((i + 1) < overallColumns.size())
                        builder.append(overallColumns.get(i)).append(" = VALUES(").append(overallColumns.get(i)).append("), ");
                    else
                        builder.append(overallColumns.get(i)).append(" = VALUES(").append(overallColumns.get(i)).append(")");
                }
                break;
            case SQLITE:
                builder.append("INSERT OR REPLACE INTO ").append(overallTable).append(" VALUES (");
                builder.append("?, ");
                builder.append("?, ");
                for (int i = 0; i < overallColumns.size(); i++) {
                    if ((i + 1) < overallColumns.size())
                        builder.append("?, ");
                    else
                        builder.append("?)");
                }
                break;
        }

        return builder.toString();
    }

    private String constructInsertVersusStatement() {
        StringBuilder builder = new StringBuilder();
        switch (getType()) {
            case MYSQL:
                String insertOverall = "INSERT INTO " + versusTable + " VALUES (?, ?, ?, ?, ";
                builder.append(insertOverall);
                for (int i = 0; i < versusColumns.size(); i++) {
                    if ((i + 1) < versusColumns.size())
                        builder.append("?, ");
                    else
                        builder.append("?)");
                }

                builder.append(" ON DUPLICATE KEY UPDATE ");
                builder.append("id1 = VALUES(id1), ");
                builder.append("name1 = VALUES(name1), ");
                builder.append("id2 = VALUES(id2), ");
                builder.append("name2 = VALUES(name2), ");
                for (int i = 0; i < versusColumns.size(); i++) {
                    if ((i + 1) < versusColumns.size())
                        builder.append(versusColumns.get(i)).append(" = VALUES(").append(versusColumns.get(i)).append("), ");
                    else
                        builder.append(versusColumns.get(i)).append(" = VALUES(").append(versusColumns.get(i)).append(")");
                }
                break;
            case SQLITE:
                builder.append("INSERT OR REPLACE INTO ").append(versusTable).append(" VALUES (");
                builder.append("?, ");
                builder.append("?, ");
                builder.append("?, ");
                builder.append("?, ");
                for (int i = 0; i < versusColumns.size(); i++) {
                    if ((i + 1) < versusColumns.size())
                        builder.append("?, ");
                    else
                        builder.append("?)");
                }
                break;
        }

        return builder.toString();
    }

    private void setupOverallTable() {
        String createOverall = "CREATE TABLE IF NOT EXISTS " + overallTable + " ("
                + "name VARCHAR(" + MAX_LENGTH + "), id VARCHAR(" + MAX_LENGTH + "), ";

        StringBuilder createStringBuilder = new StringBuilder();
        createStringBuilder.append(createOverall);
        for (int i = 0; i < overallColumns.size(); i++) {
            String column = overallColumns.get(i);
            createStringBuilder.append(column).append(" VARCHAR(").append(MAX_LENGTH).append("), ");
        }

        createStringBuilder.append(" PRIMARY KEY (id))");

        String insertOverall = "INSERT INTO " + overallTable + " VALUES (";
        StringBuilder insertStringBuilder = new StringBuilder();
        insertStringBuilder.append(insertOverall);
        for (int i = 0; i < overallColumns.size(); i++) {
            String column = overallColumns.get(i);

            insertStringBuilder.append(column);
            if ((i + 1) < overallColumns.size())
                insertStringBuilder.append("?, ");
            else
                insertStringBuilder.append("?)");
        }

        try {
            createTable(overallTable, createStringBuilder.toString());
        } catch (Exception ex) {
            Log.err("Failed to create tables!");
            ex.printStackTrace();
        }
    }

    private void setupVersusTable() {
        String createVersus = "CREATE TABLE IF NOT EXISTS " + versusTable + "(" +
                "id1 VARCHAR (" + MAX_LENGTH + ") NOT NULL," +
                "name1 VARCHAR (" + MAX_LENGTH + ") NOT NULL," +
                "id2 VARCHAR (" + MAX_LENGTH + ") NOT NULL, " +
                "name2 VARCHAR (" + MAX_LENGTH + ") NOT NULL,";

        StringBuilder createStringBuilder = new StringBuilder();
        createStringBuilder.append(createVersus);
        for (int i = 0; i < versusColumns.size(); i++) {
            String column = versusColumns.get(i);
            createStringBuilder.append(column).append(" VARCHAR(").append(MAX_LENGTH).append("), ");
        }

        createStringBuilder.append(" PRIMARY KEY (id1))");

        String insertOverall = "INSERT INTO " + versusTable + " VALUES (";
        StringBuilder insertStringBuilder = new StringBuilder();
        insertStringBuilder.append(insertOverall);
        for (int i = 0; i < versusColumns.size(); i++) {
            String column = versusColumns.get(i);

            insertStringBuilder.append(column);
            if ((i + 1) < versusColumns.size())
                insertStringBuilder.append("?, ");
            else
                insertStringBuilder.append("?)");
        }

        try {
            createTable(versusTable, createStringBuilder.toString());
        } catch (Exception ex) {
            Log.err("Failed to create tables!");
            ex.printStackTrace();
        }
    }
}
