package jdd.so.dao;

import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jdd.so.dao.model.RoomTag;

public class RoomTagDAO {
	/**
	 * Logger for this class
	 */
	private static final Logger logger = Logger.getLogger(RoomTagDAO.class);
	
	public Map<Long,List<String>> getRoomTags(Connection conn) throws SQLException{
		Map<Long,List<String>> retMap = new HashMap<>();
		String sql = "SELECT room_id, tag FROM room_tags ORDER BY room_id";
		Statement std = null;
		ResultSet rs = null;
		try {
			std = conn.createStatement();
			rs = std.executeQuery(sql);
			while (rs.next()) {
				long roomId = rs.getLong(1);
				List<String> tags = retMap.get(roomId);
				if (tags==null){
					tags = new ArrayList<>();
					retMap.put(roomId, tags);
				}
				tags.add(rs.getString(2));
			}
		} catch (SQLException e) {
			logger.error("getRoomTags(Connection)", e);
		} finally {
			ConnectionHandler.cleanUpQuery(std, rs);
		}
		
		return retMap;
	}

	public int insertOrUpdate(Connection conn, RoomTag rt) throws SQLException {

		String sql = "INSERT INTO room_tags (room_id,tag) VALUES (?,?) " + "ON DUPLICATE KEY UPDATE tag=?";

		PreparedStatement ps = null;
		int retVal = 0;
		try {
			ps = conn.prepareStatement(sql);
			ps.setLong(1, rt.getRoomId());
			ps.setString(2, rt.getTag());
			ps.setString(3, rt.getTag());
			retVal = ps.executeUpdate();
		} finally {
			if (ps != null) {
				ps.close();
			}
		}
		return retVal;
	}

	public int delete(Connection conn, RoomTag rt) throws SQLException {

		String sql = "DELETE from room_tags WHERE room_id=? and tag=?";

		PreparedStatement ps = null;
		int retVal = 0;
		try {
			ps = conn.prepareStatement(sql);
			ps.setLong(1, rt.getRoomId());
			ps.setString(2, rt.getTag());
			retVal = ps.executeUpdate();
		} finally {
			if (ps != null) {
				ps.close();
			}
		}
		return retVal;
	}

}
