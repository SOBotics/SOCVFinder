package jdd.so.higgs;

import io.swagger.client.ApiException;
import io.swagger.client.Configuration;
import io.swagger.client.api.BotApi;
import io.swagger.client.model.AquireTokenRequest;
import io.swagger.client.model.AquireTokenResponse;
import io.swagger.client.model.FeedbackType;
import io.swagger.client.model.RegisterFeedbackTypesRequest;

public class TestHiggs {
	
	private final static String basePath = "http://45.77.238.226";

		
	public static void main(String[] args)  {
		
		
		
		try {
			BotApi botApi = new BotApi(Configuration.getDefaultApiClient().setBasePath(basePath));
			
			AquireTokenRequest atr = new AquireTokenRequest();
			atr.dashboardId(1);
			atr.setSecret("");
			AquireTokenResponse token = botApi.botAquireTokenPost(atr);
			
			Configuration.getDefaultApiClient().setAccessToken(token.getToken());
			
			RegisterFeedbackTypesRequest rft = new RegisterFeedbackTypesRequest();
			FeedbackType fb1 = new FeedbackType();
			fb1.setName(FeedBack.TP.toString());
			fb1.setColour("green");
			fb1.setIcon("&#10003;"); //https://www.fileformat.info/info/unicode/char/2713/index.htm
			fb1.setIsActionable(true);
			fb1.setRequiredActions(0);
			rft.addFeedbackTypesItem(fb1);
			
			FeedbackType fb2 = new FeedbackType();
			fb2.setName(FeedBack.FP.toString());
			fb2.setColour("red");
			fb2.setIcon("&#10060;"); //http://www.fileformat.info/info/unicode/char/274c/index.htm
			fb2.setIsActionable(true);
			fb2.setRequiredActions(0);
			rft.addFeedbackTypesItem(fb2);
			
			FeedbackType fb3 = new FeedbackType();
			fb3.setName(FeedBack.FN.toString());
			fb3.setColour("orange");
			fb3.setIcon("&#9888;"); //https://www.fileformat.info/info/unicode/char/26a0/index.htm
			fb3.setIsActionable(true);
			fb3.setRequiredActions(0);
			rft.addFeedbackTypesItem(fb3);
			
			
			FeedbackType fb4 = new FeedbackType();
			fb4.setName(FeedBack.NC.toString());
			fb4.setColour("yellow");
			fb4.setIcon("&#128465;"); //http://www.fileformat.info/info/unicode/char/1f5d1/index.htm
			fb4.setIsActionable(true);
			fb4.setRequiredActions(0);
			rft.addFeedbackTypesItem(fb4);
			
			
			FeedbackType fb5 = new FeedbackType();
			fb5.setName(FeedBack.SK.toString());
			fb5.setColour("gray");
			fb5.setIcon("&#9193;"); //http://www.fileformat.info/info/unicode/char/23e9/index.htm
			fb5.setIsActionable(true);
			fb5.setRequiredActions(0);
			rft.addFeedbackTypesItem(fb5);
			
			//botApi.botRegisterFeedbackTypesPost(rft);
			
			
		} catch (ApiException e) {
			e.printStackTrace();
			System.out.println(e.getCode() + ":" + e.getMessage() + ", " + e.getResponseBody());
		}
		

		System.out.println("ok");
		
		
	}

}
