package eu.alefzero.owncloud.authenticator;

public interface OnAuthenticationResultListener {
  
  public void onAuthenticationResult(boolean success, String message);

}
