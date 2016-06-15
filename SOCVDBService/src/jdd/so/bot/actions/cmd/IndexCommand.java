package jdd.so.bot.actions.cmd;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;

import org.json.JSONException;

import fr.tunaki.stackoverflow.chat.event.PingMessageEvent;
import jdd.so.CloseVoteFinder;
import jdd.so.api.model.ApiResult;
import jdd.so.api.model.CVStats;
import jdd.so.api.model.ScanStats;
import jdd.so.bot.ChatRoom;
import jdd.so.bot.actions.BotCommand;
import jdd.so.scan.QuestionScanner;

public class IndexCommand extends BotCommand {
	/**
	 * Logger for this class
	 */
	private static final Logger logger = Logger.getLogger(IndexCommand.class);

	@Override
	public String getMatchCommandRegex() {
		return "(?i)(index)";
	}

	@Override
	public int getRequiredAccessLevel() {
		return BotCommand.ACCESS_LEVEL_TAG_OWNER;
	}

	@Override
	public String getCommandName() {
		return "Index tag";
	}

	@Override
	public String getCommandDescription() {
		return "Update the index for tag executing api calls on 20 days (200) and save to db";
	}

	@Override
	public String getCommandUsage() {
		return "index [tag]";
	}

	@Override
	public void runCommand(ChatRoom room, PingMessageEvent event) {
		String message = event.getContent();
		String tag = getTags(message);
		if (tag == null || tag.contains(";") || tag.trim().length() == 0) {
			room.send("You should index only on single tag");
			return;
		}
		boolean tagMonitored = CloseVoteFinder.getInstance().isRoomTag(room.getRoomId(),tag);
		if (!tagMonitored) {
			room.replyTo(event.getUserId(),"This tag is not monitored in this room, contact RO for more info");
			return;
		}

		room.send("Starting to index tag: " + tag);
		try {
			ApiResult ar = new QuestionScanner().scan(tag, 20, 3);
			ScanStats ss = ar.getScanStatistics();
			if (ar.getBackoff() > 0) {
				room.replyTo(event.getMessageId(), "Index of tag " + tag + " incomplete backoff message received " + ar.getBackoff() + " s");
			} else {
				room.replyTo(event.getMessageId(), "Index of tag " + tag + " completed");
			}
			room.send(getScanStats(tag, ss));
		} catch (JSONException | IOException | SQLException e) {
			logger.error("runCommand(ChatRoom, PingMessageEvent)", e);
			room.replyTo(event.getMessageId(), "Error occured while indexing, tell @Petter to check stacktrace");
			return;
		}

	}

	private String getScanStats(String tag, ScanStats ss) {
		StringBuilder sb = new StringBuilder("    Scan statistics tag [" + tag + "]\n");
		Formatter formatter = new Formatter(sb, Locale.US);
		formatter.format("%10s%15s%10s%10s%10s%10s%10s%10s", "    Date", "# questions", "# closed", "CV1", "CV2", "CV3", "CV4", "PD");
		sb.append("\n    " + new String(new char[85]).replace("\0", "-"));
		List<String> dates = new ArrayList<>();
		dates.addAll(ss.getDayStats().keySet());
		Collections.sort(dates);

		for (String d : dates) {
			CVStats ds = ss.getDayStats().get(d);
			sb.append("\n");
			// Change format of date
			SimpleDateFormat df = new SimpleDateFormat("dd MMM", Locale.ENGLISH);
			String date = "Nd";
			try {
				date = df.format(ss.getKeyFormat().parse(d));
			} catch (ParseException e) {
				// Will not happen... I hope
			}
			formatter.format("%10s%15d%10d%10s%10s%10s%10s%10s", "    " + date, ds.getNumberOfQuestions(), ds.getNumberOfClosed(), ds.getCVCountAt(1),
					ds.getCVCountAt(2), ds.getCVCountAt(3), ds.getCVCountAt(4), ds.getCVPossibleDupeCount());
		}
		sb.append("\n    " + new String(new char[85]).replace("\0", "-"));
		CVStats ts = ss.getTotalStats();
		sb.append("\n");
		formatter.format("%10s%15d%10d%10s%10s%10s%10s%10s", "    TOTAL", ts.getNumberOfQuestions(), ts.getNumberOfClosed(), ts.getCVCountAt(1),
				ts.getCVCountAt(2), ts.getCVCountAt(3), ts.getCVCountAt(4), ts.getCVPossibleDupeCount());
		formatter.close();
		return sb.toString();

	}

}
