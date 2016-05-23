package jdd.so.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class TagsDao {
	
	private Connection connection;

	public TagsDao(Connection dbConnection){
		this.connection = dbConnection;
	}
	
	public List<String> getTags() throws SQLException{
		List<String> retVal = new ArrayList<>();
		String sql = "SELECT tag_name FROM tags_tracked"; 
		Statement std = null;
		ResultSet rs = null;
		try {
			std = connection.createStatement();
			rs = std.executeQuery(sql);
			while (rs.next()) {
				retVal.add(rs.getString(1));
			}
		} finally {
			ConnectionHandler.cleanUpQuery(std, rs);
		}
		return retVal;
	}

}
