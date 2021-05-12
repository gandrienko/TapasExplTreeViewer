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
  double interval[]={Double.NEGATIVE_INFINITY,Double.POSITIVE_INFINITY};
  /**
   * How many times this node was used (i.e., occurs in explanations)
   */
  int nUses=0;
  
  public ExTreeNode parent=null;
  public Hashtable<String,ExTreeNode> children=null;
}
