package jdd.so.scan;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;
import org.json.JSONException;

import jdd.so.CloseVoteFinder;
import jdd.so.api.ApiHandler;
import jdd.so.api.CherryPickResult;
import jdd.so.api.model.ApiResult;
import jdd.so.bot.actions.filter.NumberFilter;
import jdd.so.bot.actions.filter.QuestionsFilter;
import jdd.so.dao.QuestionIndexDao;

public class QuestionScanner {
	
	public void scan(String tag, int nrDays , int minCvCount) throws JSONException, IOException, SQLException{
		Calendar cal = new GregorianCalendar();
		cal.add(Calendar.DATE, -1); //lets move back one day...
		long ed = cal.getTimeInMillis()/1000L;
		cal.add(Calendar.DATE, -nrDays);
		long sd = cal.getTimeInMillis()/1000L;
		
		SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");
		System.out.println("Scanning from " + df.format(new Date(sd*1000L)) + " to " +  df.format(new Date(ed*1000L)));
		
		ApiResult rs = new ApiHandler().getQuestions(sd,ed, 200,tag, false);
		
		System.out.println("Number of question scanned: " + rs.getNrOfQuestionScanned());
		
		QuestionsFilter filter = new QuestionsFilter();
		filter.setNumberOfQuestions(200);
		filter.setCloseVotes(new NumberFilter(">="+minCvCount));
		
		CherryPickResult cpr = new CherryPickResult(rs, 0L, tag, 0);
		cpr.filter(filter);
		
		
		QuestionIndexDao qid = new QuestionIndexDao();
		System.out.println("Number of question found: " + cpr.getFilterdQuestions().size());
		qid.updateIndex(cpr.getFilterdQuestions(), tag);
	}
	
	
	public static void main(String[] args) throws JSONException, IOException, SQLException {
		//PropertyConfigurator.configure("ini/log4j.properties");
		
		// Load properties file an instance the CloseVoteFinder
		Properties properties = new Properties();
		properties.load(new FileInputStream("ini/SOCVService.properties"));
		CloseVoteFinder.initInstance(properties);
		new QuestionScanner().scan("java", 20, 3);
		//new QuestionScanner().scan("c", 20, 3);
		CloseVoteFinder.getInstance().shutDown();
	
	}
	
}
