package jdd.so.nlp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import jdd.so.CloseVoteFinder;

public class Perspective {

	private static final Logger logger = Logger.getLogger(CommentHeatCategory.class);

	public PerspectiveResult getResult(JSONObject response,String comment) throws JSONException {
		if (response == null){
			return null;
		}
		String type = null;
		double maxScore = Double.NaN;
		double scoreToxiCity = Double.NaN;
		double scoreObscene = Double.NaN;
		List<String> languages = null;
		if (response.has("attributeScores")) {
			//String[] attributes = new String[]{"TOXICITY","OBSCENE","INFLAMMATORY","ATTACK_ON_AUTHOR","ATTACK_ON_COMMENTER","UNSUBSTANTIAL"}; 
			
			String[] attributes = new String[]{"TOXICITY","OBSCENE"}; 
			
			
			JSONObject attr = (JSONObject) response.get("attributeScores");
			for (String a : attributes) {
				double s = getScore(attr,a);
				if (Double.isNaN(s)){
					continue;
				}
				if (Double.isNaN(maxScore)||s>maxScore){
					scoreToxiCity = s;
					type = a;
				}
				if ("TOXICITY".equals(s)){
					scoreToxiCity = s;
				}
				else if ("OBSCENE".equals(s)){
					scoreObscene = s;
				}
				
			}
		}
		if (response.has("languages")) {
			JSONArray lang = response.getJSONArray("languages");
			languages = new ArrayList<>();
			for (int i = 0; i < lang.length(); i++) {
				languages.add(lang.getString(i));
			}
			System.out.println(lang);
		}
		if (!Double.isNaN(scoreToxiCity)) {
			return new PerspectiveResult(comment,type, scoreToxiCity,scoreObscene,languages);
		}
		return null;
	}

	private double getScore(JSONObject attr, String attribute) throws JSONException {
		double score = Double.NaN;
		if (attr.has(attribute)) {
			JSONObject tc = (JSONObject) attr.get(attribute);
			if (tc.has("summaryScore")) {
				JSONObject sum = (JSONObject) tc.get("summaryScore");
				score = sum.getDouble("value");
			}
		}
		return score;
	}

	public JSONObject getResponse(JSONObject request,String comment) {
		String url = "https://commentanalyzer.googleapis.com/v1alpha1/comments:analyze?key=" + CloseVoteFinder.getInstance().getPerspectiveKey();
		if (logger.isDebugEnabled()) {
			logger.debug("Perspective - Connecting");
		}
		HttpURLConnection conn = null;
		OutputStream os = null;
		InputStream is = null;
		ByteArrayOutputStream bos = null;
		try {
			conn = getConnection(url);

			String output = request.toString();

			os = conn.getOutputStream();

			
			os.write(output.getBytes("UTF-8"));
			os.flush();

			// close the outstream to get response
			closeStream(os);

			if (conn.getResponseCode() != 200) {
				throw new IOException("Incorrect response code, HTTP error code : " + conn.getResponseCode());
			}
			is = conn.getInputStream();

		
			bos = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			int len;
			while ((len = is.read(buffer)) != -1) {
				bos.write(buffer, 0, len);
			}

			String response = bos.toString("UTF-8");
			if (logger.isDebugEnabled()) {
				logger.debug("Perspective - Incomming response");
			}
			//System.out.println(response);
			return new JSONObject(response);
		} catch (Exception ex) {
			logger.error("getResponse(JSONObject) comment: " + comment);
			logger.error("getResponse(JSONObject)", ex);
		} finally {
			closeStream(bos);
			closeStream(is);
			closeStream(conn);
		}
		return null;
	}

	private HttpURLConnection getConnection(String url) throws IOException, MalformedURLException, ProtocolException {
		HttpURLConnection conn;
		conn = (HttpURLConnection) new URL(url).openConnection();
		conn.setConnectTimeout(10 * 1000);
		conn.setReadTimeout(10 * 1000);
		conn.setDoOutput(true);
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type", "application/json");
		return conn;
	}

	private void closeStream(InputStream stream) {
		if (stream != null) {
			try {
				stream.close();
			} catch (IOException e) {
				// Who cares
			}
		}
	}

	private void closeStream(OutputStream stream) {
		if (stream != null) {
			try {
				stream.close();
			} catch (IOException e) {
				// Who cares
			}
		}
	}

	private void closeStream(HttpURLConnection conn) {
		if (conn != null) {
			conn.disconnect();
		}
	}
}
