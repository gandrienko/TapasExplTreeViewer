package TapasExplTreeViewer.ui;

import TapasExplTreeViewer.data.ExTreeNode;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.util.Hashtable;
import java.util.Map;

/**
 * Visualizes a collection of explanation trees
 */
public class ExTreePanel extends JPanel {
  /**
   * The collection of trees, each explains one type of decision
   */
  public Hashtable<Integer,ExTreeNode> topNodes=null;
  /**
   * The visual representation of the tree collection
   */
  protected JTree exTree=null;
  
  public ExTreePanel(Hashtable<Integer,ExTreeNode> topNodes) {
    super();
    this.topNodes=topNodes;
    if (topNodes==null || topNodes.isEmpty())
      return;
    int nUses=0;
    for (Map.Entry<Integer,ExTreeNode> e:topNodes.entrySet())
      nUses+=e.getValue().nUses;
    DefaultMutableTreeNode root=new DefaultMutableTreeNode("All ("+nUses+")");
    for (Map.Entry<Integer,ExTreeNode> e:topNodes.entrySet())
      attachNode(root, e.getValue());
    exTree=new JTree(root);
    setLayout(new BorderLayout());
    add(new JScrollPane(exTree),BorderLayout.CENTER);
  
    Dimension size=Toolkit.getDefaultToolkit().getScreenSize();
    setPreferredSize(new Dimension(Math.round(0.25f*size.width), Math.round(0.4f*size.height)));
  }
  
  protected void attachNode(DefaultMutableTreeNode parent, ExTreeNode exNode) {
    if (exNode==null)
      return;
    DefaultMutableTreeNode node=new DefaultMutableTreeNode(exNode.getLabel()+" ("+exNode.nUses+")");
    parent.add(node);
    if (exNode.children!=null)
      for (ExTreeNode child:exNode.children)
        attachNode(node,child);
  }
}
