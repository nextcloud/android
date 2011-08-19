/* ownCloud Android client application
 *
 * Copyright (C) 2011  Bartek Przybylski
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package eu.alefzero.webdav;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;

import org.w3c.dom.Document;

import android.text.TextUtils;
import android.util.Log;

public class TreeNode {
  public enum NodeProperty {
      NAME,
      PARENT,
      PATH,
      RESOURCE_TYPE,
      CREATE_DATE,
      LAST_MODIFIED_DATE,
      CONTENT_LENGTH
  }
  
  private LinkedList<TreeNode> mChildren;
  
  public TreeNode() {
    propertyMap_ = new HashMap<NodeProperty, String>();
    mChildren = new LinkedList<TreeNode>();
  }
  
  public void setProperty(NodeProperty propertyName, String propertyValue) {
    propertyMap_.put(propertyName, propertyValue);
  }
  
  public String getProperty(NodeProperty propertyName) {
    return propertyMap_.get(propertyName);
  }
  
  void refreshData(Document document) {
    throw new RuntimeException("Unimplemented refreshData");
  }

  public String toString() {
    String str = "TreeNode {";
    for (Entry<NodeProperty, String> e : propertyMap_.entrySet()) {
      str += e.getKey() + ": " + e.getValue() + ",";
    }
    str += "}";
    return str;
  }
  
  private HashMap<NodeProperty, String> propertyMap_;

  public String stripPathFromFilename(String oc_path) {
    if (propertyMap_.containsKey(NodeProperty.NAME)) {
      String name = propertyMap_.get(NodeProperty.NAME);
      name = name.replace(oc_path, "");
      String path = "";
      if (name.endsWith("/")) {
        name = name.substring(0, name.length()-1);
      }
      path = name.substring(0, name.lastIndexOf('/')+1);
      name = name.substring(name.lastIndexOf('/')+1);
      name = name.replace("%20", " ");
      if (TextUtils.isEmpty(name)) {
        name = "/";
      }

      propertyMap_.remove(NodeProperty.NAME);
      propertyMap_.put(NodeProperty.NAME, name);
      propertyMap_.remove(NodeProperty.PATH);
      propertyMap_.put(NodeProperty.PATH, path);
      Log.i("TreeNode", toString());
      return path;
    }
    return null;
  }
  
  public LinkedList<TreeNode> getChildList() {
    return mChildren;
  }
  
  public String[] getChildrenNames() {
    String[] names = new String[mChildren.size()];
    for (int i = 0; i < mChildren.size(); ++i) {
      names[i] = mChildren.get(i).getProperty(NodeProperty.NAME);
    }
    return names;
  }
  
  public boolean hasChildren() {
    return !mChildren.isEmpty();
  }
}
