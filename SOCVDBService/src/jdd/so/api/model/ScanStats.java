package jdd.so.api.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ScanStats {
	
	private CVStats totalStats;
	private Map<String,CVStats> dayStats;
	
	private SimpleDateFormat keyFormat;
	private long lastQuestionIdScanned; //only to avoid same question in next api called
	private long firstDate;
	private long lastDate;
	
	public ScanStats(){
		totalStats = new CVStats();
		dayStats = new HashMap<>();
		keyFormat = new SimpleDateFormat("yyMMdd");
	}
	
	public void addToStats(Question q){
		if (q.getQuestionId()==lastQuestionIdScanned){
			return;
		}
		lastQuestionIdScanned = q.getQuestionId();
		totalStats.addToStats(q);
		String key = getDayKey(q.getCreationDate());
		CVStats ds = dayStats.get(key);
		if (ds == null){
			ds = new CVStats(key);
			dayStats.put(key, ds);
		}
		ds.addToStats(q);
		//store some local data also
		if (q.getCreationDate()<=firstDate||firstDate==0){
			firstDate = q.getCreationDate();
		}
		if (q.getCreationDate()>=lastDate||lastDate==0){
			lastDate = q.getCreationDate();
		}
	}

	private String getDayKey(long creationDate) {
		return keyFormat.format(new Date(creationDate*1000L));
	}

	public int getNumberOfQuestions() {
		return totalStats.getNumberOfQuestions();
	}

	public Map<String, CVStats> getDayStats() {
		return dayStats;
	}

	public SimpleDateFormat getKeyFormat() {
		return keyFormat;
	}

	public long getFirstDate() {
		return firstDate;
	}

	public long getLastDate() {
		return lastDate;
	}

	public CVStats getTotalStats() {
		return totalStats;
	}

	public void setTotalStats(CVStats totalStats) {
		this.totalStats = totalStats;
	}
}
