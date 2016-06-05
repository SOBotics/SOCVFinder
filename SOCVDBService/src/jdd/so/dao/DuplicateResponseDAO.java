package jdd.so.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import jdd.so.dao.model.DuplicateResponse;

public class DuplicateResponseDAO {
	
	public String getFalseDupeQuestions(Connection connection, long idUser) throws SQLException {
		String sql = "SELECT GROUP_CONCAT(question_id SEPARATOR ';') from (select distinct question_id FROM dup_response" 
				+ " WHERE user_id = " + idUser + " AND confirmed = 0 "
				+ " ORDER BY response_date desc " + "LIMIT 40) as br";

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


	
	public int insertOrUpdate(Connection conn, DuplicateResponse dr) throws SQLException{
		String sql = "INSERT INTO `dup_response` (question_id, user_id,room_id, confirmed, tag, response_date) VALUES (?,?,?,?,?,?) " +
				 "ON DUPLICATE KEY UPDATE confirmed=?";
	
	PreparedStatement ps  = null;
	int retVal = 0;
	try {
		ps = conn.prepareStatement(sql);
		ps.setLong(1, dr.getQuestionId());
		ps.setLong(2, dr.getUserId());
		ps.setLong(3, dr.getRoomId());
		ps.setBoolean(4, dr.isConfirmed());
		ps.setString(5, dr.getTag());
		ps.setLong(6, dr.getResponseDate());
		ps.setBoolean(7, dr.isConfirmed());
		retVal = ps.executeUpdate();
	}finally {
		if (ps!=null){
			ps.close();
		}
	}
	return retVal;
	}
	
}
