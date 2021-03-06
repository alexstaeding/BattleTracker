package org.battleplugins.tracker.executor;

import lombok.AllArgsConstructor;

import mc.alk.battlecore.executor.CustomCommandExecutor;
import mc.alk.battlecore.message.MessageController;

import org.battleplugins.api.command.CommandSender;
import org.battleplugins.api.entity.living.player.OfflinePlayer;
import org.battleplugins.api.entity.living.player.Player;
import org.battleplugins.tracker.BattleTracker;
import org.battleplugins.tracker.tracking.TrackerInterface;
import org.battleplugins.tracker.message.MessageManager;
import org.battleplugins.tracker.tracking.recap.Recap;
import org.battleplugins.tracker.tracking.recap.RecapManager;
import org.battleplugins.tracker.tracking.stat.StatType;
import org.battleplugins.tracker.tracking.stat.StatTypes;
import org.battleplugins.tracker.tracking.stat.record.Record;
import org.battleplugins.tracker.tracking.stat.tally.VersusTally;
import org.battleplugins.tracker.util.TrackerUtil;

import java.text.DecimalFormat;
import java.util.Map;
import java.util.Optional;

/**
 * Main executor for trackers.
 *
 * @author Redned
 */
@AllArgsConstructor
public class TrackerExecutor extends CustomCommandExecutor {

    private BattleTracker plugin;
    private TrackerInterface tracker;

    @MCCommand(cmds = "top")
    public void topCommand(CommandSender sender) {
        topCommand(sender, 5);
    }

    @MCCommand(cmds = "top")
    public void topCommand(CommandSender sender, int amount) {
        MessageManager messageManager = plugin.getMessageManager();
        sender.sendMessage(MessageController.colorChat(messageManager.getMessage("leaderboardHeader").replace("%tracker%", tracker.getName())));
        Map<Record, Float> sortedRecords = TrackerUtil.getSortedRecords(tracker, amount);

        int i = 1;
        for (Map.Entry<Record, Float> recordEntry : sortedRecords.entrySet()) {
            String message = messageManager.getMessage("leaderboardText");
            message = message.replace("%ranking%", String.valueOf(i));
            message = message.replace("%rating%", String.valueOf((int) recordEntry.getKey().getRating()));
            message = message.replace("%kills%", String.valueOf((int) recordEntry.getKey().getStat(StatTypes.KILLS)));
            message = message.replace("%deaths%", String.valueOf((int) recordEntry.getKey().getStat(StatTypes.DEATHS)));
            message = message.replace("%player_name%", recordEntry.getKey().getName());
            message = message.replace("%tracker%", tracker.getName());
            sender.sendMessage(MessageController.colorChat(message));

            // limit at 100 to prevent lag and spam
            if (i >= amount || i >= 100)
                break;

            i++;
        }
    }

    @MCCommand(cmds = "rank")
    public void rankCommand(CommandSender sender, OfflinePlayer player) {
        MessageManager messageManager = plugin.getMessageManager();
        Optional<Record> opRecord = tracker.getRecord(player);
        if (!opRecord.isPresent()) {
            sender.sendMessage(messageManager.getFormattedMessage(player, "recordNotFound"));
            return;
        }

        Record record = opRecord.get();
        DecimalFormat format = new DecimalFormat("0.##");
        String message = messageManager.getFormattedMessage(player, "rankingText");
        message = message.replace("%kd_ratio%", format.format(record.getStat(StatTypes.KD_RATIO)));

        for (StatType type : StatTypes.values()) {
            message = message.replace("%" + type.getInternalName() + "%", format.format(record.getStat(type)));
        }

        sender.sendMessage(message);
    }

    @MCCommand(cmds = "reset", perm = "battletracker.reset")
    public void resetCommand(CommandSender sender, OfflinePlayer player) {
        MessageManager messageManager = plugin.getMessageManager();
        if (!tracker.hasRecord(player)) {
            sender.sendMessage(messageManager.getFormattedMessage(player, "recordNotFound"));
            return;
        }

        tracker.createNewRecord(player);
        sender.sendMessage(messageManager.getFormattedMessage(player, "recordsReset").replace("%tracker%", tracker.getName()));
    }

    @MCCommand(cmds = "set", perm = "battletracker.set")
    public void setCommand(CommandSender sender, OfflinePlayer player, String statType, float value) {
        MessageManager messageManager = plugin.getMessageManager();
        Optional<Record> opRecord = tracker.getRecord(player);
        if (!opRecord.isPresent()) {
            sender.sendMessage(messageManager.getFormattedMessage(player, "recordNotFound"));
            return;
        }

        Record record = opRecord.get();
        if (!record.getStats().containsKey(statType.toLowerCase())) {
            sender.sendMessage(messageManager.getFormattedMessage("statNotInTracker"));
            return;
        }

        tracker.setValue(statType, value, player);
        sender.sendMessage(messageManager.getFormattedMessage(player, "setStatValue").replace("%stat%", statType.toLowerCase()).replace("%value%", String.valueOf(value)));
    }

    @MCCommand(cmds = "recap", perm = "battletracker.recap")
    public void recapCommand(Player player, String name) {
        MessageManager messageManager = plugin.getMessageManager();
        RecapManager recapManager = tracker.getRecapManager();

        if (!recapManager.getDeathRecaps().containsKey(name)) {
            player.sendMessage(messageManager.getFormattedMessage("noRecapForPlayer"));
            return;
        }

        Recap recap = recapManager.getDeathRecaps().get(name);
        if (!recap.isVisible()) {
            player.sendMessage(messageManager.getFormattedMessage("noRecapForPlayer"));
            return;
        }

        switch (tracker.getDeathMessageManager().getClickContent()) {
            case "armor":
                tracker.getRecapManager().sendArmorRecap(player, recap);
                break;
            case "inventory":
                tracker.getRecapManager().sendInventoryRecap(player, recap);
        }
    }

    @MCCommand(cmds = {"vs", "versus"}, perm = "battletracker.versus")
    public void versusCommandSelf(Player sender, OfflinePlayer player2) {
        versusCommand(sender, sender, player2);
    }

    @MCCommand(cmds = {"vs", "versus"}, perm = "battletracker.versus")
    public void versusCommand(CommandSender sender, OfflinePlayer player1, OfflinePlayer player2) {
        MessageManager messageManager = plugin.getMessageManager();
        Optional<VersusTally> opTally = tracker.getVersusTally(player1, player2);
        if (!opTally.isPresent()) {
            sender.sendMessage(messageManager.getFormattedMessage("tallyNotFound"));
            return;
        }

        DecimalFormat format = new DecimalFormat("0.##");
        Record record1 = tracker.getOrCreateRecord(player1);
        Record record2 = tracker.getOrCreateRecord(player2);

        sender.sendMessage(MessageController.colorChat(messageManager.getMessage("versusHeader")));
        String versusMessage = MessageController.colorChat(messageManager.getMessage("versusText"));
        versusMessage = versusMessage.replace("%player_name_1%", player1.getName());
        versusMessage = versusMessage.replace("%player_name_2%", player2.getName());

        for (Map.Entry<String, Float> statEntry : record1.getStats().entrySet()) {
            versusMessage = versusMessage.replace("%" + statEntry.getKey() + "_1%", format.format(statEntry.getValue()));
        }

        for (Map.Entry<String, Float> statEntry : record2.getStats().entrySet()) {
            versusMessage = versusMessage.replace("%" + statEntry.getKey() + "_2%", format.format(statEntry.getValue()));
        }

        VersusTally tally = opTally.get();
        String versusCompare = MessageController.colorChat(messageManager.getMessage("versusCompare"));
        int kills = tally.getStats().get(StatTypes.KILLS.getInternalName()).intValue();
        int deaths = tally.getStats().get(StatTypes.DEATHS.getInternalName()).intValue();

        // Since versus tallies are only stored one way, we need to flip the value
        // in the scenario that the "1st" player instead the 2nd player
        if (tally.getId2().equals(player1.getUniqueId().toString())) {
            versusCompare = versusCompare.replace("%player_name_1%", player2.getName());
            versusCompare = versusCompare.replace("%player_name_2%", player1.getName());
        } else {
            versusCompare = versusCompare.replace("%player_name_1%", player1.getName());
            versusCompare = versusCompare.replace("%player_name_2%", player2.getName());
        }

        versusCompare = versusCompare.replace("%kills%", String.valueOf(kills));
        versusCompare = versusCompare.replace("%deaths%", String.valueOf(deaths));

        sender.sendMessage(versusMessage);
        sender.sendMessage(versusCompare);
    }
}
