package eu.alefzero.webdav;

import java.net.URI;

import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.protocol.HTTP;

public class HttpPropFind extends HttpEntityEnclosingRequestBase {

  public final static String METHOD_NAME = "PROPFIND";
  
  public HttpPropFind(final URI uri) {
    super();
    setURI(uri);
  }

  public HttpPropFind(final String uri) {
    this.setDepth("1");
    setURI(URI.create(uri));
    this.setHeader(HTTP.CONTENT_TYPE, "text/xml" + HTTP.CHARSET_PARAM + HTTP.UTF_8.toLowerCase());
  }
  
  @Override
  public String getMethod() {
    return METHOD_NAME;
  }
  
  public void setDepth(String depth) {
    this.setHeader("Depth", depth);
  }
  
}
