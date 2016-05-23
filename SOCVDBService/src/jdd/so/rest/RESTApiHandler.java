package jdd.so.rest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.log4j.Logger;

import jdd.so.CloseVoteFinder;
import jdd.so.api.CherryPickResult;

/**
 * Class the send the the json to server and get the
 * the link to page back as result
 * @author Petter Friberg
 *
 */
public class RESTApiHandler {
	/**
	 * Logger for this class
	 */
	private static final Logger logger = Logger.getLogger(RESTApiHandler.class);

	public RESTApiHandler() {
		super();
	}

	public String getRemoteURL(CherryPickResult result) throws IOException {

		HttpURLConnection conn=null;
		GZIPOutputStream gos=null;
		GZIPInputStream gis=null;
		ByteArrayOutputStream bos=null;
		try {
			String url = CloseVoteFinder.getInstance().getRestApi();
			if (logger.isDebugEnabled()) {
				logger.debug("getRemoteURL(CherryPickResult) - Connecting to: " + url);
			}
			conn = getConnection(url);

			String output = result.getJSONObject().toString();
			
			gos = new GZIPOutputStream(conn.getOutputStream());
			
			if (logger.isInfoEnabled()) {
				logger.info("getRemoteURL(CherryPickResult) - Sending gzipped json: " + output);
			}
			gos.write(output.getBytes("UTF-8"));
			gos.flush();
			
			//close the outstream to get response
			closeStream(gos);

			if (conn.getResponseCode() != 200) {
				throw new IOException("Incorrect response code, HTTP error code : " + conn.getResponseCode());
			}
			gis = new GZIPInputStream(conn.getInputStream());
			
			if (logger.isDebugEnabled()) {
				logger.debug("getRemoteURL(CherryPickResult) - Reading gzipped json result");
			}
			
			bos = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			int len;
			while ((len = gis.read(buffer)) != -1) {
				bos.write(buffer, 0, len);
			}

			String response = bos.toString("UTF-8");
			if (logger.isDebugEnabled()) {
				logger.debug("getRemoteURL(CherryPickResult) - Incomming response: " + response);
			}
			return response;

		} catch (IOException e) {
			logger.error("getRemoteURL(CherryPickResult)", e);
			throw e;
		} finally {
			closeStream(bos);
			closeStream(gis);
			closeStream(conn);

		}

	}

	private HttpURLConnection getConnection(String url) throws IOException, MalformedURLException, ProtocolException {
		HttpURLConnection conn;
		conn = (HttpURLConnection) new URL(url).openConnection();
		conn.setConnectTimeout(10*1000);
		conn.setReadTimeout(10*1000);
		conn.setDoOutput(true);
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setRequestProperty("Content-Encoding", "gzip");
		conn.setRequestProperty("Accept-Encoding", "gzip");
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
