package jdd.so.nlp;

import java.util.List;

public class PerspectiveResult {
	
	private String type;
	private double scoreToxiCity;
	private double scoreObscene;
	private List<String> language;
	private String comment;
	
	
	public PerspectiveResult(String comment,String type, double scoreToxiCity, double scoreObscene, List<String> languages) {
		this.comment = comment;
		this.type = type;
		this.scoreToxiCity = scoreToxiCity;
		this.scoreObscene = scoreObscene;
		language = languages;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public double getScore() {
		double s = Double.NaN;
		if (!Double.isNaN(scoreToxiCity)){
			s = scoreToxiCity;
		}
		if (!Double.isNaN(scoreObscene)){
			if (Double.isNaN(s)||scoreObscene>s){
				s = scoreObscene;
			}
		}
		return s;
	}
	
	public List<String> getLanguage() {
		return language;
	}
	public void setLanguage(List<String> language) {
		this.language = language;
	}
	public boolean isEnglish(){
		return language==null||language.contains("en");
	}
	
	public boolean isHit(){
		return getScore()>0.8 || !isEnglish();
	}
	
	@Override
	public String toString(){
		return comment + "\n\tType: " + type + " Score:" + getScore() + " language: " + language;
	}
	public String getComment() {
		return comment;
	}
	public void setComment(String comment) {
		this.comment = comment;
	}
	public double getScoreToxiCity() {
		return scoreToxiCity;
	}
	public void setScoreToxiCity(double scoreToxiCity) {
		this.scoreToxiCity = scoreToxiCity;
	}
	public double getScoreObscene() {
		return scoreObscene;
	}
	public void setScoreObscene(double scoreObscene) {
		this.scoreObscene = scoreObscene;
	}

}
