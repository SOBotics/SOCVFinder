package jdd.so.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jdd.so.dao.model.Batch;
import jdd.so.dao.model.Stats;

/**
 * Manually mapping table users, to avoid hibernate and other crap
 * 
 * @author Petter Friberg
 *
 */
public class BatchDAO {

	public Map<Long, Integer> getBatchNumbers(Connection connection) throws SQLException {
		Map<Long, Integer> batchNumbers = new HashMap<Long, Integer>();
		String sql = "SELECT room_id,max(batch_nr) as mb FROM batch GROUP BY room_id";
		Statement std = null;
		ResultSet rs = null;
		try {
			std = connection.createStatement();
			rs = std.executeQuery(sql);
			while (rs.next()) {
				long roomNr = rs.getLong(1);
				int batchNr = rs.getInt(2);
				batchNumbers.put(roomNr, batchNr);
			}
		} finally {
			ConnectionHandler.cleanUpQuery(std, rs);
		}

		return batchNumbers;
	}
	
	public String getQuestionsInOpenBatches(Connection connection, String tag) throws SQLException{
		int timeOffSetMin = 10; //Within 10 minutes
		long batchStart = System.currentTimeMillis()/1000L - (timeOffSetMin*60);
		
		String sql = "SELECT GROUP_CONCAT(questions SEPARATOR '') from batch " +
					"WHERE batch_date_end=? and batch_date_start>=? and searchTags=?";

		
		
		String questions = "";
		PreparedStatement std = null;
		ResultSet rs = null;
		try {
			std = connection.prepareStatement(sql);
			std.setLong(1, 0L);
			std.setLong(2, batchStart);
			std.setString(3, tag);
			rs = std.executeQuery();
			if (rs.next()) {
				questions = rs.getString(1);
			}
		} finally {
			ConnectionHandler.cleanUpQuery(std, rs);
		}
		return questions;
		
	}

	public String getLastQuestionsReviewed(Connection connection, long idUser) throws SQLException {
		String sql = "SELECT GROUP_CONCAT(questions SEPARATOR '') from (select distinct questions " + "FROM batch " + "WHERE user_id = " + idUser
				+ " AND batch_date_end > 0 " + "ORDER BY batch_date_end desc " + "LIMIT 20) as br";

		String questions = "";
		Statement std = null;
		ResultSet rs = null;
		try {
			std = connection.createStatement();
			rs = std.executeQuery(sql);
			if (rs.next()) {
				questions = rs.getString(1);
			}
		} finally {
			ConnectionHandler.cleanUpQuery(std, rs);
		}
		return questions;
	}

	public int insert(Connection conn, Batch b) throws SQLException {

		String sql = "INSERT INTO batch (`room_id`,`message_id`, `batch_date_start`,`batch_nr`,`user_id`,`searchTags`,`nr_questions`,`questions`,`cv_count_before`) "
				+ " VALUES (?,?,?,?,?,?,?,?,?)";

		PreparedStatement ps = null;
		int ret;
		try {
			ps = conn.prepareStatement(sql);
			ps.setLong(1, b.getRoomId());
			ps.setLong(2, b.getMessageId());
			ps.setLong(3, b.getBatchDateStart());
			ps.setInt(4, b.getBatchNr());
			ps.setLong(5, b.getUserId());
			ps.setString(6, b.getSearchTags());
			ps.setInt(7,  b.getNumberOfQuestions());
			ps.setString(8, b.getQuestions());
			ps.setInt(9, b.getCvCountBefore());
			ret = ps.executeUpdate();
		} finally {
			if (ps != null) {
				ps.close();
			}
		}
		return ret;
	}

	public int update(Connection conn, Batch b) throws SQLException {

		String sql = "UPDATE `batch` SET `batch_date_end` = ?, `cv_count_after` = ?,`closed_count` = ? " + "WHERE `room_id` = ? AND `message_id` = ?";

		PreparedStatement ps = null;
		int ret;
		try {
			ps = conn.prepareStatement(sql);
			ps.setLong(1, b.getBatchDateEnd());
			ps.setInt(2, b.getCvCountAfter());
			ps.setInt(3, b.getClosedCount());
			ps.setLong(4, b.getRoomId());
			ps.setLong(5, b.getMessageId());
			ret = ps.executeUpdate();
		} finally {
			if (ps != null) {
				ps.close();
			}
		}
		return ret;
	}

	public long getLastMessageId(Connection connection, long userId) throws SQLException {
		long messageId = 0L;
		String sql = "SELECT message_id FROM batch WHERE user_id = " + userId + " ORDER BY batch_date_start DESC LIMIT 1";
		Statement std = null;
		ResultSet rs = null;
		try {
			std = connection.createStatement();
			rs = std.executeQuery(sql);
			while (rs.next()) {
				messageId = rs.getLong(1);
			}
		} finally {
			ConnectionHandler.cleanUpQuery(std, rs);
		}
		return messageId;
	}

	public Batch getBatch(Connection connection, long pmId) throws SQLException {
		Batch b = null;

		String sql = "SELECT * FROM batch WHERE message_id=" + pmId;
		Statement std = null;
		ResultSet rs = null;
		try {
			std = connection.createStatement();
			rs = std.executeQuery(sql);
			if (rs.next()) {
				b = new Batch();
				b.setRoomId(rs.getLong("room_id"));
				b.setMessageId(rs.getLong("message_id"));
				b.setBatchNr(rs.getInt("batch_nr"));
				b.setUserId(rs.getLong("user_id"));
				b.setBatchDateStart(rs.getLong("batch_date_start"));
				b.setSearchTags(rs.getString("searchTags"));
				b.setNumberOfQuestions(rs.getInt("nr_questions"));
				b.setQuestions(rs.getString("questions"));
				b.setCvCountBefore(rs.getInt("cv_count_before"));
				b.setBatchDateEnd(rs.getLong("batch_date_end"));
				b.setCvCountAfter(rs.getInt("cv_count_after"));
				b.setClosedCount(rs.getInt("closed_count"));

			}
		} finally {
			ConnectionHandler.cleanUpQuery(std, rs);
		}
		return b;
	}
	
	public List<Stats> getTagsStats(Connection connection, long fromDate) throws SQLException {
		return getTagStats(connection, 0, fromDate);
	}

	public List<Stats> getTagStats(Connection connection, long userId, long fromDate) throws SQLException {
		List<Stats> retList = new ArrayList<>();
		String sql = "SELECT searchTags, sum(nr_questions) as nrQuestions,sum((cv_count_after-cv_count_before)) as virtual_cv_count, sum(LEAST((cv_count_after-cv_count_before),nr_questions)) as cv_count, sum(closed_count) as closed " + "FROM batch "
				+ "WHERE batch_date_end>" + fromDate + " ";
		if (userId > 0) {
			sql += "AND user_id = " + userId + " ";
		}

		sql += "GROUP BY searchTags ORDER BY cv_count desc";
		Statement std = null;
		ResultSet rs = null;
		try {
			std = connection.createStatement();
			rs = std.executeQuery(sql);
			while (rs.next()) {
				retList.add(new Stats(rs.getString(1), rs.getInt(2),rs.getInt(3),rs.getInt(4), rs.getInt(5)));
			}
		} finally {
			ConnectionHandler.cleanUpQuery(std, rs);
		}
		return retList;
	}

	public List<Stats> getRoomStats(Connection connection, long fromDate) throws SQLException {
		List<Stats> retList = new ArrayList<>();
		String sql = "SELECT room_id, sum(nr_questions) as nrQuestions, sum((cv_count_after-cv_count_before)) as virtual_cv_count, sum(LEAST((cv_count_after-cv_count_before),nr_questions)) as cv_count, sum(closed_count) as closed " 
		+ "FROM batch WHERE batch_date_end>" +  fromDate + " " 
	    + "GROUP BY room_id order by cv_count desc";
		Statement std = null;
		ResultSet rs = null;
		try {
			std = connection.createStatement();
			rs = std.executeQuery(sql);
			while (rs.next()) {
				retList.add(new Stats(rs.getLong(1), rs.getInt(2), rs.getInt(3),rs.getInt(4), rs.getInt(5)));
			}
		} finally {
			ConnectionHandler.cleanUpQuery(std, rs);
		}
		return retList;
	}

}
