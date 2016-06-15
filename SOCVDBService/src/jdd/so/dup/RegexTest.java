package jdd.so.dup;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexTest {

	public static void main(String[] args) {
		String regExTest = "(?is)\\b((yo)?u suck|8={3,}D|nigg(a|er)|ass ?hole|slut|gay|kiss my ass|fag(got)?|daf[au][qk]|(mother)?fuc?k+(ing?|e?(r|d)| off+| y(ou|e)(rself)?| u+|tard)?|shit(t?er|head)|dickhead|pedo|whore|(is a )?cunt|cocksucker|ejaculated?|butthurt|(private|pussy) show|lesbo|bitches|suck\\b.{0,20}\\bdick|dee[sz]e? nut[sz])s?\\b|^.{0,250}\\b(shit)\\b.{0,100}$";
		
		String message = "I messed up this fucking code";
		List<String> list = new ArrayList<String>();
		Pattern p = Pattern.compile(regExTest);
		Matcher m = p.matcher(message);
		while (m.find()) {
		    list.add(m.group());
		}
		System.out.println(list);

	}

}
