package androidtest.tests;

public final class Config {
	
	public static final String server = "owncloudServerVar";
	public static final Boolean hasSubdirectory = false;
	public static String URL = GetURl(hasSubdirectory);
	
	public static final String user = "owncloudUserVar";
	public static final String password = "owncloudPasswordVar";
	public static final String user2 = "owncloudUser2Var";
	public static final String password2 = "owncloudPassword2Var";
	public static final String userAccount = user + "@"+server;
	public static final String userAccount2 = user2 + "@"+server;
	
	public static String GetURl(Boolean hasSubdirectory){
		if(hasSubdirectory){
			return server + "/owncloud";
		}else{
			return server;
		}
	}

}
