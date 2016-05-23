package jdd.so.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import jdd.so.CloseVoteFinder;
import jdd.so.api.model.Question;

public class QuestionIndexDao {

	private Connection connection;

	public QuestionIndexDao() {
		this.connection = CloseVoteFinder.getInstance().getConnection();
	}
	
	public String getQueryString(String tag) throws SQLException{
		StringBuilder retVal = new StringBuilder();
		String sql = "SELECT question_id FROM question_index WHERE tag='" + tag + "' ORDER BY cv_count DESC LIMIT 99"; 
		Statement std = null;
		ResultSet rs = null;
		try {
			std = connection.createStatement();
			rs = std.executeQuery(sql);
			while (rs.next()) {
				if (retVal.length()>0){
					retVal.append(";");	
				}
				retVal.append(rs.getLong(1));
			}
		} finally {
			ConnectionHandler.cleanUpQuery(std, rs);
		}
		return retVal.toString();
	}

	public int[] updateIndex(List<Question> questions, String tag) throws SQLException {
		long scanDate = System.currentTimeMillis() / 1000L;
		int[] retVal=null;

		if (!questions.isEmpty()) {
			String sql = "INSERT INTO question_index (question_id,creation_date,cv_count,tag,scan_date) VALUES (?,?,?,?,?) "
					+ "ON DUPLICATE KEY UPDATE cv_count=?,scan_date=?";

			PreparedStatement ps = null;
			try {
				ps = connection.prepareStatement(sql);
				for (Question q : questions) {
					ps.setLong(1, q.getQuestionId());
					ps.setLong(2, q.getCreationDate());
					ps.setInt(3, q.getCloseVoteCount());
					ps.setString(4, tag);
					ps.setLong(5, scanDate);
					ps.setInt(6, q.getCloseVoteCount());
					ps.setLong(7, scanDate);
					ps.addBatch();
				}

				retVal = ps.executeBatch();
			} finally {
				if (ps != null) {
					ps.close();
				}
			}
		}

		// 1. Delete old
		String sqlDelete = "DELETE FROM question_index where tag='" + tag + "' AND scan_date<" + scanDate;
		Statement std = null;
		try {
			std = connection.createStatement();
			std.execute(sqlDelete);
		} finally {
			if (std != null) {
				std.close();
			}
		}

		return retVal;

	}

}
