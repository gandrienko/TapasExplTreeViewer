package TapasExplTreeViewer.data;

import java.util.ArrayList;
import java.util.Hashtable;

/**
 * Represents a node in an explanation tree (decision tree)
 */
public class ExTreeNode {
  public int level=0;
  public String attrName=null;
  /**
   * min..max; either min or max is +-inf
   */
  public double condition[]={Double.NEGATIVE_INFINITY,Double.POSITIVE_INFINITY};
  /**
   * How many times this node was used (i.e., occurs in explanations)
   */
  public int nUses=0;
  /**
   * How many times different sectors appear in combination with this attribute and this condition
   */
  public Hashtable<String,Integer> sectors=null;
  
  public ExTreeNode parent=null;
  public ArrayList<ExTreeNode> children=null;
  
  public String getLabel() {
    if (attrName==null)
      return "null";
    if (condition==null ||
            (condition[0]==Double.NEGATIVE_INFINITY &&
                 condition[1]==Double.POSITIVE_INFINITY))
      return attrName;
    if (condition[0]==condition[1])
      return attrName+" = "+condition[0];
    if (condition[0]==Double.NEGATIVE_INFINITY)
      return attrName+" < "+condition[1];
    if (condition[1]==Double.POSITIVE_INFINITY)
      return attrName+" >= "+condition[0];
    return condition[0]+" <= "+attrName+" < "+condition[1];
  }
  
  public boolean sameCondition(String attrName, double condition[]) {
    if (attrName==null)
      return this.attrName==null;
    if (!attrName.equals(this.attrName))
      return false;
    if (condition==null)
      return this.condition==null;
    if (this.condition==null)
      return false;
    return condition[0]==this.condition[0] && condition[1]==this.condition[1];
  }
  
  static public int compareConditions(double cnd1[], double cnd2[]) {
    if (cnd1==null)
      return (cnd2==null)?0:1;
    if (cnd2==null) return -1;
    if (cnd1[0]==cnd2[0])
      return (cnd1[1]<cnd2[1])?-1:(cnd1[1]==cnd2[1])?0:1;
    return (cnd1[0]<cnd2[1])?-1:1;
  }
  
  public ExTreeNode findChild(String attrName, double condition[]) {
    if (children==null)
      return null;
    for (ExTreeNode child:children)
      if (child.sameCondition(attrName,condition))
        return child;
    return null;
  }
  
  public void addUse(){
    ++nUses;
  }
  
  public void addSectorUse(String sector){
    if (sectors==null)
      sectors=new Hashtable<String,Integer>(100);
    Integer n=sectors.get(sector);
    if (n==null) n=0;
    sectors.put(sector,n+1);
  }
  
  public void addChild(ExTreeNode child) {
    if (child==null)
      return;
    if (children==null)
      children=new ArrayList<ExTreeNode>(10);
    int idx=-1;
    if (!children.isEmpty()) {
      for (int i = 0; i < children.size() && idx < 0; i++)
        if (child.attrName.equals(children.get(i).attrName))
          idx = (compareConditions(child.condition, children.get(i).condition) < 0) ? i : i + 1;
      if (idx<0)
        for (int i = 0; i < children.size() && idx < 0; i++)
          if (child.attrName.compareTo(children.get(i).attrName)<0)
            idx=i;
    }
    if (idx>=0)
      children.add(idx,child);
    else
      children.add(child);
    child.parent=this;
  }
}
