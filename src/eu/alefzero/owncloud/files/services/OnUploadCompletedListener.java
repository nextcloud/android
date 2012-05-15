package eu.alefzero.owncloud.files.services;

public interface OnUploadCompletedListener extends Runnable {
  
  public boolean getUploadResult();
  
  public void setUploadResult(boolean result);
}
