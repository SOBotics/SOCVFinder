package jdd.so.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jdd.so.dao.model.DuplicateNotifications;

public class DuplicateNotificationsDAO {

	public List<DuplicateNotifications> getDupeHunters(Connection connection) throws SQLException {
		List<DuplicateNotifications> hunters = Collections.synchronizedList(new ArrayList<>());
		String sql = "SELECT `room_id`, `user_id`,`tag`,`opt_in` FROM `dup_notifications` where `opt_in` = 1";
		Statement std = null;
		ResultSet rs = null;
		try {
			std = connection.createStatement();
			rs = std.executeQuery(sql);
			while (rs.next()) {
				hunters.add(new DuplicateNotifications(rs.getInt(1),rs.getInt(2),rs.getString(3),rs.getBoolean(4)));
			}
		} finally {
			ConnectionHandler.cleanUpQuery(std, rs);
		}
		
		return hunters;
	}

	public int insertOrUpdate(Connection conn, DuplicateNotifications dn) throws SQLException{
		String sql = "INSERT INTO `dup_notifications` (room_id,user_id,tag,opt_in) VALUES (?,?,?,?) " +
				 "ON DUPLICATE KEY UPDATE opt_in=?";
	
	PreparedStatement ps  = null;
	int retVal = 0;
	try {
		ps = conn.prepareStatement(sql);
		ps.setLong(1, dn.getRoomId());
		ps.setLong(2, dn.getUserId());
		ps.setString(3, dn.getTag());
		ps.setBoolean(4, dn.isOptIn());
		ps.setBoolean(5, dn.isOptIn());
		retVal = ps.executeUpdate();
	}finally {
		if (ps!=null){
			ps.close();
		}
	}
	return retVal;
	}
	
}
