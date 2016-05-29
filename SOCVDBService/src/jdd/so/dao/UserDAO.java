package jdd.so.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import jdd.so.dao.model.User;

/**
 * Manually mapping table users, to avoid hibernate and other crap
 * @author Petter Friberg
 *
 */
public class UserDAO {
	

	public Map<Long, User> getUsers(Connection connection) throws SQLException{
		Map<Long,User> users = Collections.synchronizedMap(new HashMap<Long, User>());
		String sql = "SELECT user_id,user_name,privilege_level FROM users"; 
		Statement std = null;
		ResultSet rs = null;
		try {
			std = connection.createStatement();
			rs = std.executeQuery(sql);
			while (rs.next()) {
				User u = new User(rs.getLong(1), rs.getString(2), rs.getInt(3));
				users.put(u.getUserId(), u);
			}
		} finally {
			ConnectionHandler.cleanUpQuery(std, rs);
		}
		
		return users;
	}
	
	public int insertOrUpdate(Connection conn, User u) throws SQLException{
		
		String sql = "INSERT INTO users (user_id,user_name,privilege_level) VALUES (?,?,?) " +
					 "ON DUPLICATE KEY UPDATE user_name=?,privilege_level=?";
		
		PreparedStatement ps  = null;
		int retVal = 0;
		try {
			ps = conn.prepareStatement(sql);
			ps.setLong(1, u.getUserId());
			ps.setString(2, u.getUserName());
			ps.setInt(3, u.getAccessLevel());
			ps.setString(4, u.getUserName());
			ps.setInt(5, u.getAccessLevel());
			retVal = ps.executeUpdate();
		}finally {
			if (ps!=null){
				ps.close();
			}
		}
		return retVal;	
	}

}
