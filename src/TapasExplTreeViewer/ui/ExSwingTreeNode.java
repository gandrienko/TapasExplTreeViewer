package TapasExplTreeViewer.ui;

import TapasDataReader.ExTreeNode;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.ArrayList;

public class ExSwingTreeNode extends DefaultMutableTreeNode {
  public String name=null;
  public ExTreeNode dataNode=null;
  
  public ExSwingTreeNode(String name, ExTreeNode dataNode) {
    super(name);
    this.dataNode=dataNode;
  }
  
  public boolean hasAttribute(String attrName) {
    if (dataNode==null || attrName==null)
      return false;
    return attrName.equals(dataNode.attrName);
  }
  
  public boolean hasLevel(int level){
    if (dataNode==null)
      return false;
    return dataNode.level==level;
  }
  
  public boolean hasLevelAbove(int level){
    if (dataNode==null)
      return false;
    return dataNode.level>level;
  }
  
  public void getNodesWithAttribute(String attrName,ArrayList<ExSwingTreeNode> resultList) {
    if (attrName==null || resultList==null)
      return;
    if (hasAttribute(attrName))
      resultList.add(this);
    for (int i=0; i<getChildCount(); i++)
      ((ExSwingTreeNode)getChildAt(i)).getNodesWithAttribute(attrName,resultList);
  }
  
  public void getNodesUpToLevel(int level, ArrayList<ExSwingTreeNode> resultList) {
    if (resultList==null || hasLevelAbove(level))
      return;
    if (hasLevel(level) || getChildCount()<1)
      resultList.add(this);
    else
    for (int i=0; i<getChildCount(); i++)
      ((ExSwingTreeNode)getChildAt(i)).getNodesUpToLevel(level,resultList);
  }
}
