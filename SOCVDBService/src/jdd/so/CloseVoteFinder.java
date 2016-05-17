package jdd.so;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceConfigurationError;
import java.util.zip.GZIPInputStream;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.json.JSONException;
import org.json.JSONObject;

import jdd.so.api.ApiHandler;
import jdd.so.db.ConnectionHandler;
import jdd.so.db.UserDAO;
import jdd.so.model.ApiResult;
import jdd.so.model.User;

/**
 * Test application to get questions with close votes from SO Api
 * 
 * @author Petter Friberg
 * 
 *
 */
public class CloseVoteFinder {
	/**
	 * Logger for this class
	 */
	private static final Logger logger = Logger.getLogger(CloseVoteFinder.class);

	public static final String API_URL = "http://api.stackexchange.com/2.2/";
	public static final String API_FILTER = "!-MObZ6A82KZGZ3WvblLvUKz1bWU5_K147";
	public static final int MAX_PAGES = 100;//Even if application  try it will never do any more then this

	public static final String API_KEY_PROPERTY = "API_KEY";
	public static final String THROTTLE_PROPERTY = "THROTTLE";
	private static final String REST_API_PROPERTY = "REST_API";

	
	private long throttle = 1L * 1000L; // ms
	private String apiKey = null;
	private String restApi = "http://socvr.org:222/api/socv-finder/dump-report"; 

	private static CloseVoteFinder instance;
	private static long lastCall;
	private static int apiCallNrPages = 10;
	private int apiQuota;
	private int defaultNumberOfQuestion=20;
	private Connection dbConnection;
	
	
	//User
	private Map<Long,User> users;

	private CloseVoteFinder(Properties properties) {
		if (properties != null) {
			apiKey = properties.getProperty(API_KEY_PROPERTY, null);
			String ts = properties.getProperty(THROTTLE_PROPERTY, null);
			if (ts != null) {
				try {
					throttle = Long.parseLong(ts);
				} catch (NumberFormatException e) {
					logger.error("CloseVoteFinder(Properties)", e);
				}
			}
			String ra =properties.getProperty(REST_API_PROPERTY, null);
			if (ra!=null && ra.trim().length()>0){
				restApi = ra;
			}
			ConnectionHandler connectionHandler = new ConnectionHandler(properties.getProperty("db_driver"),properties.getProperty("db_url"), properties.getProperty("db_user"), properties.getProperty("db_password"));
			
			try {
				dbConnection = connectionHandler.getConnection();
				loadUsers();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	
	public Connection getConnection(){
		return this.dbConnection;
	}
	
	public void loadUsers() throws SQLException {
		users = new UserDAO().getUsers(this.dbConnection);
	}

	public static void initInstance(Properties properties) {
		if (instance == null) {
			instance = new CloseVoteFinder(properties);
		}
	}
	
	public void shutDown(){
		if (dbConnection!=null){
			try {
				dbConnection.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	public static CloseVoteFinder getInstance() {
		if (instance == null) {
			throw new ServiceConfigurationError("You need to init the instance before using it");
		}
		return instance;
	}

	public synchronized boolean isTagMonitored(String tag) {
		//TODO, load on start up from db and check if its monitored
		return false;
	}
	
	public String getApiUrl(String questions, int page, String tag) throws UnsupportedEncodingException{
		return getApiUrl(questions, page, 0, 0, tag);
	}
	
	/**
	 * Get the url to call
	 * @param questions, question1;question2 <code>null</code> no filter
	 * @param page, page to view
	 * @param fromDate, unixtimestamp date, set 0 to not filter 
	 * @param toDate, unixtimestamp date, set 0 to not filter
	 * @param tag, unixtimestamp date, set <code>null</code> to not filter
	 * @return the url
	 * @throws UnsupportedEncodingException
	 */
	public String getApiUrl(String questions, int page, long fromDate, long toDate, String tag) throws UnsupportedEncodingException{
		
		StringBuilder url = new StringBuilder(API_URL);
		url.append("questions");
		if (questions!=null){
			url.append("/" + questions);
		}
		url.append("?");
		url.append("page=" + page + "&pagesize=100");
		if (fromDate>0){
			url.append("&fromdate=" + fromDate);
		}
		if (toDate>0){
			url.append("&todate=" + toDate);
		}
		
		//url.append("&order=desc&sort=activity");
		url.append("&order=desc&sort=creation");
		
		if (tag!=null){
			String tagEncoded = URLEncoder.encode(tag, "UTF-8");
			url.append("&tagged=" +tagEncoded);
		}
		url.append("&site=stackoverflow&filter=" + API_FILTER);
		if (apiKey != null) {
			url.append("&key=" + apiKey);
		}
		return url.toString();
	}

	/**
	 * Get the data from url as a JSON object with trottle implementation to
	 * avoid calling to often SO
	 * 
	 * @param url
	 * @param notifyMe
	 *            can be null
	 * @return
	 * @throws IOException
	 * @throws JSONException
	 */
	public synchronized JSONObject getJSONObject(String url, NotifyMe notifyMe) throws IOException, JSONException {
		long curTime = System.currentTimeMillis();
		long timeToWait = throttle - (curTime - lastCall);
		if (logger.isDebugEnabled()) {
			logger.debug("getJSONObject - Throttle time: " + timeToWait + " ms");
		}
		if (timeToWait > 0) {
			if (notifyMe != null) {
				notifyMe.message("Throttle for " + timeToWait + " ms to not upset SO");
			}
			try {
				// TODO: Trottle, replace with monitor.wait
				Thread.sleep(timeToWait);
			} catch (InterruptedException e) {
				logger.error("getJSONObject(String, NotifyMe)", e);
			}
		}
		lastCall = System.currentTimeMillis();
		if (logger.isInfoEnabled()) {
			logger.info("getJSONObject - Calling url: " + url);
		}
		
		URLConnection connection = new URL(url).openConnection();
		connection.setRequestProperty("Accept-Encoding", "gzip");
		GZIPInputStream gis = new GZIPInputStream(connection.getInputStream());
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			byte[] buffer = new byte[1024];
			int len;
			while ((len = gis.read(buffer)) != -1) {
				bos.write(buffer, 0, len);
			}

			String jsonText = bos.toString("UTF-8");
			// uncompress
			if (logger.isDebugEnabled()) {
				logger.debug("getJSONObject - JSON response\n" + jsonText);
			}
			return new JSONObject(jsonText);
		} finally {
			gis.close();
			bos.close();
		}
	}



	public static void main(String[] args) throws IOException, ParseException {
		PropertyConfigurator.configure("ini/log4j.properties");

		if (logger.isDebugEnabled()) {
			logger.debug("main(String[]) - start");
		}

		// load properties file
		Properties properties = new Properties();
		properties.load(new FileInputStream("ini/SOCVService.properties"));
		CloseVoteFinder.initInstance(properties);

		String date = "20160506";
		String tag = "java";

		Calendar cal = new GregorianCalendar();
		cal.setTime(new SimpleDateFormat("yyyyMMdd").parse(date));
		long fromDate = cal.getTimeInMillis() / 1000;
		cal.add(Calendar.HOUR_OF_DAY, 24);
		long toDate = cal.getTimeInMillis() / 1000;

		ApiHandler api = new ApiHandler();
		ApiResult qr = api.getQuestions(fromDate, toDate, 20, tag,true);

		File f = new File("output/" + tag + "_" + date + ".html");
		// lets just overwrite
		PrintWriter out = null;
		try {
			out = new PrintWriter(f, "UTF-8");
			out.write(qr.getHTML(tag + " test (6h)"));
		} catch (IOException e) {
			logger.error("main(String[])", e);
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}

	public String getRestApi() {
		return restApi;
	}

	public static int getApiCallNrPages() {
		return apiCallNrPages;
	}

	public int getApiQuota() {
		return apiQuota;
	}

	public void setApiQuota(int apiQuota) {
		this.apiQuota = apiQuota;
	}

	public int getDefaultNumberOfQuestion() {
		return this.defaultNumberOfQuestion;
	}

	public void setDefaultNumberOfQuestion(int defaultNumberOfQuestion) {
		this.defaultNumberOfQuestion = defaultNumberOfQuestion;
	}

	public Map<Long, User> getUsers() {
		return users;
	}

}
