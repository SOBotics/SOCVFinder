package jdd.so.higgs;

public enum FeedBack {
	
	TP("tp"),FP("fp"),FN("fn"),NC("nc"),SK("sk");
	
	private final String text;
    
    private FeedBack(final String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}
