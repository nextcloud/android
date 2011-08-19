package eu.alefzero.owncloud;

public class OwnCloudSession {
  private String mSessionName;
  private String mSessionUrl;
  private int mEntryId;
  
  public OwnCloudSession(String name, String url, int entryId) {
    mSessionName = name;
    mSessionUrl = url;
    mEntryId = entryId;
  }
  
  public void setName(String name) {
    mSessionName = name;
  }
  
  public String getName() {
    return mSessionName;
  }
  
  public void setUrl(String url) {
    mSessionUrl = url;
  }
  
  public String getUrl() {
    return mSessionUrl;
  }
  
  public int getEntryId() {
    return mEntryId;
  }
}
