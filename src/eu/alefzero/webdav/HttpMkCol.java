package eu.alefzero.webdav;

import java.net.URI;

import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;

public class HttpMkCol extends HttpEntityEnclosingRequestBase {

  public final static String METHOD_NAME = "MKCOL";
  
  public HttpMkCol(final String uri) {
    setURI(URI.create(uri));
  }

  @Override
  public String getMethod() {
    return METHOD_NAME;
  }
}
