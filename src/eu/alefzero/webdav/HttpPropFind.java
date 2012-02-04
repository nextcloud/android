/* ownCloud Android client application
 *   Copyright (C) 2011  Bartek Przybylski
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
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
