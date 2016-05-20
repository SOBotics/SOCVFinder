package jdd.so.dao;

import java.sql.Connection;
import java.util.List;

import jdd.so.api.model.Question;

public class QuestionDAO {
	
	
	private Connection connection;

	public QuestionDAO(Connection connection){
		this.connection = connection;
	}
	
	public List<Question> getQuestions(){
		return null;
	}
	
	public Question getNotification(){
		return null;
	}
	
	
	public void insertOrUpdate(List<Question> questions){
		
	}
	
	public void update(List<Question> questions){
		
	}

}
