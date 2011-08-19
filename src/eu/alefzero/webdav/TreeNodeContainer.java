package eu.alefzero.webdav;

import java.util.List;
import java.util.ListIterator;

import org.w3c.dom.Document;

import android.util.Xml;

public class TreeNodeContainer extends TreeNode {
  
  @Override
  void refreshData(Document document) {
    ListIterator<TreeNode> iterator = children_.listIterator();
    while (iterator.hasNext()) {
      iterator.next().refreshData(document);
    }
  }
  
  private List<TreeNode> children_;
}
