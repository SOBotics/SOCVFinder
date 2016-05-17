package jdd.so.rest;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import fr.tunaki.stackoverflow.chat.Room;
import fr.tunaki.stackoverflow.chat.StackExchangeClient;
import jdd.so.CloseVoteFinder;
import jdd.so.SwingAppCommands;
import jdd.so.api.CherryPickResult;


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
			conn = (HttpURLConnection) new URL(url).openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setRequestProperty("Content-Encoding", "gzip");
			conn.setRequestProperty("Accept-Encoding", "gzip");

			String output = result.getJSONObject().toString();
			
			if (logger.isDebugEnabled()) {
				logger.debug("getRemoteURL(CherryPickResult) - " + output);
			}
			gos = new GZIPOutputStream(conn.getOutputStream());
			
			if (logger.isDebugEnabled()) {
				logger.debug("getRemoteURL(CherryPickResult) - Sending gzipped json: " + output);
			}
			gos.write(output.getBytes("UTF-8"));
			gos.flush();
			
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
			//closeStream(gos);
			closeStream(conn);

		}

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
