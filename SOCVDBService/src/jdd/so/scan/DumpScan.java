package jdd.so.scan;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;

import org.json.JSONArray;
import org.jsoup.Jsoup;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;

public class DumpScan {
	
	public static void main(String[] args) throws JsonIOException, JsonSyntaxException, FileNotFoundException, UnsupportedEncodingException {
		PrintWriter writer = new PrintWriter("dev/sd.txt", "UTF-8");
		 
		
		JsonParser parser = new JsonParser();
		 JsonElement res = parser.parse(new FileReader("dev/dump.json"));
		 
		 if (res instanceof JsonArray){
			 JsonArray posts = (JsonArray) res;
			 HashSet<String> set = new HashSet<String>();
			 for (int i = 0; i < posts.size(); i++) {
				 JsonObject el = (JsonObject) posts.get(i);
				 String title = ((JsonPrimitive) el.get("title")).getAsString();
				 String body = ((JsonPrimitive) el.get("body")).getAsString();
				 String[] stA = sanitize(title);
//				 for (String st : stA) {
//					 if (st.trim().length()>2 && !set.contains(st)){
//						 writer.println(st);
//						 set.add(st);
//					 }	
//				}
//				 stA = sanitize(body);
//				 for (String st : stA) {
//					 if (st.trim().length()>2 && !set.contains(st)){
//						 writer.println(st);
//						 set.add(st);
//					 }	
//				}
				 
				 String st = sanitizeLine(body);
				 if (st.trim().length()>2 && !set.contains(st)){
					 writer.println(st);
					 set.add(st);
				 }
				 
			 }
		 }
		 writer.close();
		 
	}
	
	private static String sanitizeLine(String text) {
		String line = Jsoup.parse(text).text(); //remove html
			line = line.replaceAll("((https?|ftp|gopher|telnet|file|Unsure|http):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)", "");
			line = line.replaceAll("@(\\S+)?", "").trim(); //remove username
			//line = line.replaceAll("[^a-zA-Z\r\n]", " ").trim(); //remove strange chars
			line = line.replaceAll("[ ]{2,}", " "); //remove spaces	
		
		
		return line;
	}

	private static String[] sanitize(String text) {
		String lines[] = text.split("\\n");
		String ret[] = new String[lines.length];
		int i = 0;
		for (String s : lines) {
			String line = Jsoup.parse(s).text(); //remove html
			line = line.replaceAll("((https?|ftp|gopher|telnet|file|Unsure|http):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)", "");
			line = line.replaceAll("@(\\S+)?", "").trim(); //remove username
			line = line.replaceAll("[^a-zA-Z\r\n]", " ").trim(); //remove strange chars
			line = line.replaceAll("[ ]{2,}", " "); //remove spaces	
			ret[i]=line;
			i++;
		}
		
		return ret;
	}

}
