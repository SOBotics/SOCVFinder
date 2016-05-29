package jdd.so.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jdd.so.dao.model.WhiteList;

public class WhitelistDAO {

	
	public Set<Long> getWhiteListedQuestions(Connection connection) throws SQLException{
		Set<Long> wlq = new HashSet<>();
		Calendar cal = new GregorianCalendar();
		cal.add(Calendar.DATE, -22);
		String sql = "SELECT question_id FROM whitelist WHERE creation_date>" + (cal.getTimeInMillis()/1000L);
		Statement std = null;
		ResultSet rs = null;
		try {
			std = connection.createStatement();
			rs = std.executeQuery(sql);
			while (rs.next()) {
				wlq.add(rs.getLong(1));
			}
		} finally {
			ConnectionHandler.cleanUpQuery(std, rs);
		}
		return wlq;
	}
	
public int insertOrUpdate(Connection conn, WhiteList w) throws SQLException{
		
		String sql = "INSERT INTO whitelist (question_id,user_id,creation_date) VALUES (?,?,?) " +
					 "ON DUPLICATE KEY UPDATE user_id=?, creation_date=?";
		
		PreparedStatement ps  = null;
		int retVal = 0;
		try {
			long now = System.currentTimeMillis()/1000L;
			ps = conn.prepareStatement(sql);
			ps.setLong(1, w.getQuestionId());
			ps.setLong(2, w.getUserId());
			ps.setLong(3, now);
			ps.setLong(4, w.getUserId());
			ps.setLong(5, now);
			retVal = ps.executeUpdate();
		}finally {
			if (ps!=null){
				ps.close();
			}
		}
		return retVal;	
	}
}
