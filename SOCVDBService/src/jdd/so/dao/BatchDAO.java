package jdd.so.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import jdd.so.dao.model.Batch;

/**
 * Manually mapping table users, to avoid hibernate and other crap
 * 
 * @author Petter Friberg
 *
 */
public class BatchDAO {
	
	public Map<Long, Integer> getBatchNumbers(Connection connection) throws SQLException{
		Map<Long,Integer> batchNumbers = new HashMap<Long, Integer>();
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
	
	public String getLastQuestionsReviewed(Connection connection, long idUser) throws SQLException{
		String sql = "SELECT GROUP_CONCAT(questions SEPARATOR '') from (select distinct questions " +
				"FROM batch " +
				"WHERE user_id = " + idUser +" AND batch_date_end > 0 " +
				"ORDER BY batch_date_end desc " +
				"LIMIT 5) as br";
		
		String questions = null;
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

		String sql = "INSERT INTO batch (`room_id`,`message_id`, `batch_date_start`,`batch_nr`,`user_id`,`searchTags`,`questions`,`cv_count_before`) "
				+ " VALUES (?,?,?,?,?,?,?,?)";

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
			ps.setString(7, b.getQuestions());
			ps.setInt(8, b.getCvCountBefore());
			ret = ps.executeUpdate();
		} finally {
			if (ps != null) {
				ps.close();
			}
		}
		return ret;
	}

	public int update(Connection conn, Batch b) throws SQLException {

		String sql = "UPDATE `batch` SET `batch_date_end` = ?, `cv_count_after` = ?,`closed_count` = ? " 
		+ "WHERE `room_id` = ? AND `message_id` = ?";

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

}
