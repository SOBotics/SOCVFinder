package jdd.so.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jdd.so.dao.model.CommentsNotify;

public class CommentsNotifyDAO {

	public List<CommentsNotify> getCommentsNotify(Connection connection) throws SQLException {
		List<CommentsNotify> commentsUser = Collections.synchronizedList(new ArrayList<>());
		String sql = "SELECT `user_id`,`notify`,`score` FROM `comments_notify`";
		Statement std = null;
		ResultSet rs = null;
		try {
			std = connection.createStatement();
			rs = std.executeQuery(sql);
			while (rs.next()) {
				commentsUser.add(new CommentsNotify(rs.getInt(1),rs.getBoolean(2),rs.getInt(3)));
			}
		} finally {
			ConnectionHandler.cleanUpQuery(std, rs);
		}
		
		return commentsUser;
	}

	public int insertOrUpdate(Connection conn, CommentsNotify dn) throws SQLException{
		String sql = "INSERT INTO `comments_notify` (user_id,notify,score) VALUES (?,?,?) " +
				 "ON DUPLICATE KEY UPDATE notify=?, score=?";
	
	PreparedStatement ps  = null;
	int retVal = 0;
	try {
		ps = conn.prepareStatement(sql);
		ps.setLong(1, dn.getUserId());
		ps.setBoolean(2, dn.isNotify());
		ps.setInt(3, dn.getScore());
		ps.setBoolean(4, dn.isNotify());
		ps.setInt(5, dn.getScore());
		retVal = ps.executeUpdate();
	}finally {
		if (ps!=null){
			ps.close();
		}
	}
	return retVal;
	}
	
}
