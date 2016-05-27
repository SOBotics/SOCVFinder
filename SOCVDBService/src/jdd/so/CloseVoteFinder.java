package jdd.so;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceConfigurationError;
import java.util.zip.GZIPInputStream;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.json.JSONException;
import org.json.JSONObject;

import jdd.so.api.ApiHandler;
import jdd.so.api.model.ApiResult;
import jdd.so.api.model.Question;
import jdd.so.dao.BatchDAO;
import jdd.so.dao.ConnectionHandler;
import jdd.so.dao.DuplicateNotificationsDAO;
import jdd.so.dao.TagsDao;
import jdd.so.dao.UserDAO;
import jdd.so.dao.model.DuplicateNotifications;
import jdd.so.dao.model.User;
import jdd.so.swing.NotifyMe;

/**
 * Instance class, to keep properties synchronize calls to SO Api, with
 * throttling and hold other application things in memory
 * 
 * @author Petter Friberg
 *
 */
public class CloseVoteFinder {
	/**
	 * Logger for this class
	 */
	private static final Logger logger = Logger.getLogger(CloseVoteFinder.class);

	public static final String API_URL = "http://api.stackexchange.com/2.2/";
	public static final String API_FILTER = "!-MObZ6A82KZGZ3WvblLvUKz1bWU5_K147";
	public static final int MAX_PAGES = 300;// Even if application try it will
											// never do any more then this

	public static final String API_KEY_PROPERTY = "API_KEY";
	public static final String THROTTLE_PROPERTY = "THROTTLE";
	private static final String REST_API_PROPERTY = "REST_API";

	private long throttle = 1L * 1000L; // ms
	private String apiKey = null;
	private String restApi = "http://socvr.org:222/api/socv-finder/dump-report";

	private static CloseVoteFinder instance;
	private long lastCall;
	private int apiCallNrPages = 10;
	private int apiQuota = -1;
	private int defaultNumberOfQuestion = 20;
	private Connection dbConnection;

	// DB Data cached
	private Map<Long, User> users;
	private Map<Long, Integer> batchNumbers;
	private List<DuplicateNotifications> dupHunters;
	private List<String> tagsMonitored;

	private ConnectionHandler connectionHandler;

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
			String ra = properties.getProperty(REST_API_PROPERTY, null);
			if (ra != null && ra.trim().length() > 0) {
				restApi = ra;
			}
			connectionHandler = new ConnectionHandler(properties.getProperty("db_driver"), properties.getProperty("db_url"), properties.getProperty("db_user"),
					properties.getProperty("db_password"));

			try {
				dbConnection = connectionHandler.getConnection();
				loadTagsMonitored();
				loadUsers();
				loadDupeHunters();
				loadBatchNumbers();
			} catch (SQLException e) {
				logger.error("CloseVoteFinder(Properties)", e);
			}
		}
	}

	/**
	 * Init the instance
	 * 
	 * @param properties
	 */
	public static void initInstance(Properties properties) {
		if (instance == null) {
			instance = new CloseVoteFinder(properties);
		}
	}

	/**
	 * Get the instance
	 * 
	 * @return
	 */
	public static CloseVoteFinder getInstance() {
		if (instance == null) {
			throw new ServiceConfigurationError("You need to init the instance before using it");
		}
		return instance;
	}

	/**
	 * Get the database connection, we should use a pool if the bot get some
	 * heave use, for now a single connection is used with a isValid command
	 * 
	 * @return
	 * @throws SQLException
	 */

	public Connection getConnection() throws SQLException {
		try {
			if (!dbConnection.isValid(1000)) {
				dbConnection.close();
				dbConnection = null;
			}
		} catch (SQLException e) {
			logger.error("getConnection() - Connection is not valid", e);
			dbConnection = null;
		}
		if (dbConnection == null) {
			dbConnection = connectionHandler.getConnection();
		}
		return this.dbConnection;
	}

	private void loadTagsMonitored() throws SQLException {
		this.tagsMonitored = new TagsDao(this.dbConnection).getTags();
	}

	public void loadUsers() throws SQLException {
		users = new UserDAO().getUsers(this.dbConnection);
	}

	private void loadDupeHunters() throws SQLException {
		dupHunters = new DuplicateNotificationsDAO().getDupeHunters(this.dbConnection);
	}

	public Map<Long, List<DuplicateNotifications>> getHunterInRooms() {
		Map<Long, List<DuplicateNotifications>> retMap = new HashMap<>();
		if (dupHunters != null) {
			for (DuplicateNotifications dn : dupHunters) {
				long r = dn.getRoomId();
				List<DuplicateNotifications> dnl = retMap.get(r);
				if (dnl == null) {
					dnl = new ArrayList<>();
					retMap.put(r, dnl);
				}
				dnl.add(dn);
			}
		}
		return retMap;
	}

	public List<String> getHunterTags() {
		List<String> tags = new ArrayList<>();
		if (dupHunters != null) {
			for (DuplicateNotifications dn : dupHunters) {
				String tag = dn.getTag().toLowerCase();
				if (!tags.contains(tag)) {
					tags.add(tag);
				}
			}
		}
		return tags;
	}

	public DuplicateNotifications getHunter(long roomId, long userId, String tag) {
		if (dupHunters == null) {
			return null;
		}
		for (DuplicateNotifications dn : dupHunters) {
			if (dn.getRoomId() == roomId && dn.getUserId() == userId && dn.getTag().equals(tag)) {
				return dn;
			}
		}
		return null;
	}

	private void loadBatchNumbers() throws SQLException {
		batchNumbers = new BatchDAO().getBatchNumbers(this.dbConnection);
	}

	public void shutDown() {
		if (dbConnection != null) {
			try {
				dbConnection.close();
			} catch (SQLException e) {
				logger.error("shutDown()", e);
			}
		}
	}

	public synchronized boolean isTagMonitored(String tag) {
		return tagsMonitored.contains(tag);
	}

	public String getApiUrl(String questions, int page, String tag) throws UnsupportedEncodingException {
		return getApiUrl(questions, page, 0, 0, tag);
	}

	/**
	 * Get the url to call
	 * 
	 * @param questions,
	 *            question1;question2 <code>null</code> no filter
	 * @param page,
	 *            page to view
	 * @param fromDate,
	 *            unixtimestamp date, set 0 to not filter
	 * @param toDate,
	 *            unixtimestamp date, set 0 to not filter
	 * @param tag,
	 *            unixtimestamp date, set <code>null</code> to not filter
	 * @return the url
	 * @throws UnsupportedEncodingException
	 */
	public String getApiUrl(String questions, int page, long fromDate, long toDate, String tag) throws UnsupportedEncodingException {

		StringBuilder url = new StringBuilder(API_URL);
		url.append("questions");
		if (questions != null) {
			url.append("/" + questions);
		}
		url.append("?");
		url.append("page=" + page + "&pagesize=100");
		if (fromDate > 0) {
			url.append("&fromdate=" + fromDate);
		}
		if (toDate > 0) {
			url.append("&todate=" + toDate);
		}

		// url.append("&order=desc&sort=activity");
		url.append("&order=desc&sort=creation");

		if (tag != null) {
			String tagEncoded = URLEncoder.encode(tag, "UTF-8");
			url.append("&tagged=" + tagEncoded);
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

	public String getRestApi() {
		return restApi;
	}

	public int getApiCallNrPages() {
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

	public int getBatchNumber(long roomId) {
		if (batchNumbers != null && batchNumbers.containsKey(roomId)) {
			return batchNumbers.get(roomId);
		}
		return 0;
	}

	public static void main(String[] args) throws Exception {

		PropertyConfigurator.configure("ini/log4j.properties");

		// Load properties file an instance the CloseVoteFinder
		Properties properties = new Properties();
		properties.load(new FileInputStream("ini/SOCVService.properties"));
		CloseVoteFinder.initInstance(properties);

		ApiHandler api = new ApiHandler();

		Calendar calStart = new GregorianCalendar();
		long ed = calStart.getTimeInMillis() / 1000L;
		calStart.add(Calendar.DATE, -20);
		long sd = calStart.getTimeInMillis() / 1000L;

		ApiResult rs = api.getQuestions(sd, ed, 200, "php", false);

		List<Question> questions = rs.getQuestions();
		Map<Integer, List<Question>> cMap = new HashMap<>();
		long now = System.currentTimeMillis() / 1000L;
		int maxDif = 0;
		for (Question q : questions) {
			int dif = (int) (now - q.getCreationDate()) / (60 * 60 * 24);
			if (dif > maxDif) {
				maxDif = dif;
			}
			List<Question> qd = cMap.get(dif);
			if (qd == null) {
				qd = new ArrayList<>();
				cMap.put(dif, qd);
			}
			qd.add(q);
		}

		for (int i = 0; i <= maxDif; i++) {
			List<Question> qd = cMap.get(i);
			if (qd == null) {
				continue;
			}
			int[] cvCnt = new int[] { 0, 0, 0, 0, 0 };
			for (Question question : qd) {
				int cvs = question.getCloseVoteCount();
				cvCnt[cvs] += 1;
			}
			String res = "Day " + i + " ";
			for (int n = 1; n < cvCnt.length; n++) {
				res += " CV" + n + "=" + cvCnt[n];
			}
			if (logger.isDebugEnabled()) {
				logger.debug("main(String[]) - " + res);
			}
		}

		CloseVoteFinder.getInstance().shutDown();
	}

	public List<String> getTagsMonitored() {
		return tagsMonitored;
	}

	public List<DuplicateNotifications> getDupHunters() {
		return dupHunters;
	}
}
