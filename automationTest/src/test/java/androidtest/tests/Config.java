package androidtest.tests;

public final class Config {

	//without http or https
	public static final String URL = "owncloudServerVar";
	public static boolean isTrusted = true;

	//without http or https
	public static final String URL2 = "owncloudServer2Var";
	public static boolean isTrusted2 = true;

	public static final String user = "owncloudUserVar";
	public static final String password = "owncloudPasswordVar";
	public static final String user2 = "owncloudUser2Var";
	public static final String password2 = "owncloudPassword2Var";
	public static final String userAccount = user + "@"+ URL;
	public static final String userAccount2 = user2 + "@"+ URL2;

}
