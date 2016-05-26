package jdd.so.bot.actions.cmd;

import java.io.FileInputStream;
import java.util.Calendar;
import java.util.Formatter;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;

import com.sun.java_cup.internal.runtime.virtual_parse_stack;

import jdd.so.CloseVoteFinder;
import jdd.so.bot.actions.BotCommand;
import jdd.so.dao.BatchDAO;
import jdd.so.dao.model.Stats;

public abstract class StatsCommandAbstract extends BotCommand {

	public String getStats(List<Stats> stats,boolean isRoom){
		StringBuilder sb = new StringBuilder();
		sb.append("\n");
		Formatter formatter = new Formatter(sb, Locale.US);
		String header = "[tag]";
		if (isRoom){
			header = "Room";
		}
		formatter.format("%10s%-15s%10s%10s%10s%10s", "    nr ", header, "Reviews", "CV virt.","CV count", "Closed");
		sb.append("\n    " + new String(new char[65]).replace("\0", "-"));
		int n=1;
		int totaleVirtualCvCount = 0;
		int totaleCvCount = 0;
		int totaleCloseCount = 0;
		int totaleQuestions  =0;
		for (Stats s : stats) {
			sb.append("\n");
			formatter.format("%10s%-15s%10d%10d%10d%10d","    " +n+". ", s.getDescription(), s.getNrQuestions(),s.getVirtualCvCount(),s.getCvCount(), s.getClosedCount());
			totaleCvCount +=s.getCvCount();
			totaleVirtualCvCount +=s.getVirtualCvCount();
			totaleCloseCount +=s.getClosedCount();
			totaleQuestions +=s.getNrQuestions();
			n++;
		}
		sb.append("\n    " + new String(new char[65]).replace("\0", "-"));
		sb.append("\n");
		formatter.format("%10s%-15s%10d%10d%10d%10d","    ", "TOTAL", totaleQuestions, totaleVirtualCvCount, totaleCvCount, totaleCloseCount);
		formatter.close();
		return sb.toString();
	}
	
	public long getFromDate(String message){
		if (message.toLowerCase().contains("today")){
			Calendar cal = new GregorianCalendar(Locale.ENGLISH);
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			return cal.getTimeInMillis()/1000L;
		}
		if (message.toLowerCase().contains("week")){
			Calendar cal = new GregorianCalendar(Locale.ENGLISH);
			cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			return cal.getTimeInMillis()/1000L;
		}
		
		if (message.toLowerCase().contains("month")){
			Calendar cal = new GregorianCalendar(Locale.ENGLISH);
			cal.set(Calendar.DATE, 1);
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			return cal.getTimeInMillis()/1000L;
		}
		
		return 0L;
	}
	
	public String getFilteredTitle(String message){
		if (message.toLowerCase().contains("today")){
			return " today";
		}
		if (message.toLowerCase().contains("week")){
			return " this week";
		}
		
		if (message.toLowerCase().contains("month")){
			return " this month";
		}
		return " all time";
	}
	
	public static void main(String[] args) throws Exception {
		PropertyConfigurator.configure("ini/log4j.properties");
		Properties properties = new Properties();
		properties.load(new FileInputStream("ini/SOCVService.properties"));
		CloseVoteFinder.initInstance(properties);
		
		List<Stats> stats = new  BatchDAO().getTagsStats(CloseVoteFinder.getInstance().getConnection(),0);
		String ret = new StatsMeCommand().getStats(stats, false);
		System.out.println(ret);
		CloseVoteFinder.getInstance().shutDown();
	}

}
