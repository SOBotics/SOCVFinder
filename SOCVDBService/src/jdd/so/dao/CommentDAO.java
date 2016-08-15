package jdd.so.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import jdd.so.api.model.Comment;

public class CommentDAO {
	
	
	public int[] insertComment(Connection connection,Comment comment) throws SQLException {
		List<Comment> cs = new ArrayList<>();
		cs.add(comment);
		return insertComment(connection, cs);
	}
	
	public int[] insertComment(Connection connection,List<Comment> comments) throws SQLException {
		int[] retVal=null;

		if (!comments.isEmpty()) {
			String sql = "INSERT INTO `comments` (`id_comment`,`creation_date`,`body`,`user_id`,`rep`,`score`,`regex`,`nativeBayesBad`,`nativeBayesGood`,`openNLPBad`,`openNLPGood`,`j48Bad`,`j48Good`,`reported`,`link` ) " 
					+ "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) "
					+ "ON DUPLICATE KEY UPDATE body=?";

			PreparedStatement ps = null;
			try {
				ps = connection.prepareStatement(sql);
				for (Comment c : comments) {
					ps.setLong(1, c.getCommentId());
					ps.setLong(2, c.getCreationDate());
					ps.setString(3, c.getBody());
					ps.setLong(4, c.getUserId());
					ps.setLong(5, c.getReputation());
					ps.setInt(6, c.getScore());
					ps.setString(7, c.getRegExHit());
					ps.setDouble(8, c.getNaiveBayesBad());
					ps.setDouble(9, c.getNaiveBayesGood());
					ps.setDouble(10, c.getOpenNlpBad());
					ps.setDouble(11, c.getOpenNlpGood());
					ps.setDouble(12, c.getJ48Bad());
					ps.setDouble(13, c.getJ48Good());
					ps.setBoolean(14, c.isReported());
					ps.setString(15, c.getLink());
					ps.setString(16, c.getBody());
					ps.addBatch();
				}

				retVal = ps.executeBatch();
			} finally {
				if (ps != null) {
					ps.close();
				}
			}
		}

		return retVal;

	}

	public void tpComment(Connection connection, long idComment) throws SQLException{
		String sql = "UPDATE comments set tp=1 where id_comment=" + idComment;
		Statement std=null;
		try {
			std = connection.createStatement();
			std.executeUpdate(sql);
		}  finally {
			if (std!=null){
				std.close();
			}
		}
	}
	
	public void fpComment(Connection connection, long idComment) throws SQLException{
		String sql = "UPDATE comments set fp=1 where id_comment=" + idComment;
		Statement std=null;
		try {
			std = connection.createStatement();
			std.executeUpdate(sql);
		}  finally {
			if (std!=null){
				std.close();
			}
		}
	}


}
