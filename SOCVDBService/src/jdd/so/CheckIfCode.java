package jdd.so;

public class CheckIfCode {
	
	public static void main(String[] args) {

		System.out.println(checkIfUnformatted("up............................................................"));
	}

    public static boolean checkIfUnformatted(String strippedBody){
        strippedBody = strippedBody.replace("\n"," ");
        long totLength = strippedBody.length();
        long whitespaceCount = strippedBody.chars().filter(c -> c == ' ').count();
        long alphaCount = strippedBody.chars().filter(c -> c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9').count();
        long puncCount = totLength-whitespaceCount-alphaCount;
        if (totLength!=0 && alphaCount!=0)
            return whitespaceCount/(double)totLength<0.08 && puncCount/(double)alphaCount>0.125;
        return false;
    }

}
