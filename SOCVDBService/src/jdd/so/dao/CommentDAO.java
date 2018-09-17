package jdd.so.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
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
			String sql = "INSERT INTO `comments` (`id_comment`,`creation_date`,`body`,`user_id`,`rep`,`score`,`regex`,`nativeBayesBad`,`nativeBayesGood`,`openNLPBad`,`openNLPGood`,`TOXICITY`,`OBSCENE`,`reported`,`link`,`higgsId` ) " 
					+ "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) "
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
					double sT = Double.NaN;
					double sO = Double.NaN;
					if (c.getPerspectiveResult()!=null){
						sT = c.getPerspectiveResult().getScoreToxiCity();
						sO = c.getPerspectiveResult().getScoreObscene();
						
					}
					if (Double.isNaN(sT)){
						ps.setNull(12, Types.DOUBLE);
					}else{
						ps.setDouble(12, sT);
					}
					if (Double.isNaN(sO)){
						ps.setNull(13, Types.DOUBLE);
					}else{
						ps.setDouble(13, sO);
					}
					ps.setBoolean(14, c.isReported());
					ps.setString(15, c.getLink());
					ps.setInt(16, c.getHiggsReportId());
					ps.setString(17, c.getBody());
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
	
	/**
	 * 
	 * @param connection
	 * @param idComment
	 * @return 0 if not found
	 * @throws SQLException
	 */
	public int getHiggsReportId(Connection connection, long idComment) throws SQLException{
		int id = 0;
		String sql = "SELECT higgsId from comments where id_comment=" + idComment;
		Statement std = null;
		ResultSet rs = null;
		try {
			std = connection.createStatement();
			rs = std.executeQuery(sql);
			if (rs.next()) {
				id = rs.getInt(1);
			}
		} finally {
			if (rs!=null){
				rs.close();
			}
			if (std!=null){
				std.close();
			}
		}
		return id;
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
