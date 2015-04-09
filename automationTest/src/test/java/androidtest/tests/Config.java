package androidtest.tests;

public final class Config {
	
	public static final String server = "owncloudServerVar";
	public static final Boolean hasResource = false;
	public static String URL = GetURl(server, hasResource, "resourceServerVar");
	public static boolean isTrusted = true;
	
	public static final String server2 = "owncloudServer2Var";
	public static final Boolean hasResource2 = false;
	public static String URL2 = GetURl(server2, hasResource2, "resourceServerVar");
	public static boolean isTrusted2 = true;
	
	public static final String user = "owncloudUserVar";
	public static final String password = "owncloudPasswordVar";
	public static final String user2 = "owncloudUser2Var";
	public static final String password2 = "owncloudPassword2Var";
	public static final String userAccount = user + "@"+ server;
	public static final String userAccount2 = user2 + "@"+ server2;
	
	public static String GetURl(String server, Boolean hasSubdirectory, String serverResource){
		if(hasSubdirectory){
			return server + serverResource;
		}else{
			return server;
		}
	}
}
