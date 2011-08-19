package eu.alefzero.webdav;

import java.net.URI;

import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;

public class HttpPropPatch extends HttpEntityEnclosingRequestBase {

  public static final String METHOD_NAME = "PROPPATCH";
  
  public HttpPropPatch(URI uri) {
    super();
    setURI(uri);
  }
  
  public HttpPropPatch(final String uri) {
    super();
    setURI(URI.create(uri));
  }
  
  @Override
  public String getMethod() {
    return METHOD_NAME;
  }
  
}
