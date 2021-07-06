package TapasExplTreeViewer.clustering;

import TapasUtilities.ItemSelectionManager;
import TapasUtilities.SingleHighlightManager;
import it.unipi.di.sax.optics.ClusterObject;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.ArrayList;

public class ClustererByOPTICS extends OPTICS_Runner {
  /**
   * Shows the reachability plot
   */
  protected ReachPlotPanel plotPanel =null;
  
  protected ArrayList<ChangeListener> changeListeners=null;
  /**
   * Highlighting and selection
   */
  protected SingleHighlightManager highlighter=null;
  protected ItemSelectionManager selector=null;
  
  public void addChangeListener(ChangeListener l) {
    if (changeListeners==null)
      changeListeners=new ArrayList(5);
    if (!changeListeners.contains(l)) {
      changeListeners.add(l);
      if (plotPanel!=null)
        plotPanel.addChangeListener(l);
    }
  }
  
  public void removeChangeListener(ChangeListener l) {
    if (l!=null && changeListeners!=null) {
      changeListeners.remove(l);
      if (plotPanel!=null)
        plotPanel.removeChangeListener(l);
    }
  }
  
  public void setDistanceMatrix(double distances[][]) {
    this.distances = distances;
    if (distances!=null) {
      objToCluster=new ArrayList<ClusterObject<Integer>>(distances.length);
      for (int i=0; i<distances.length; i++) {
        ClusterObject<Integer> clObj=new ClusterObject<Integer>(new Integer(i));
        objToCluster.add(clObj);
      }
    }
  }
  
  public void setHighlighter(SingleHighlightManager highlighter) {
    this.highlighter = highlighter;
    if (plotPanel!=null)
      plotPanel.setHighlighter(highlighter);
  }
  
  public void setSelector(ItemSelectionManager selector) {
    this.selector = selector;
    if (plotPanel!=null)
      plotPanel.setSelector(selector);
  }
  
  protected void clusteringDone(){
    if (objOrdered==null || objOrdered.isEmpty()) {
      System.out.println("OPTICS clustering failed!");
      return;
    }
    System.out.println("OPTICS clustering finished!");
    
    if (plotPanel==null) {
      plotPanel = new ReachPlotPanel(objOrdered, this);
      JFrame fr = new JFrame("OPTICS reachability plot");
      fr.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
      fr.getContentPane().add(plotPanel, BorderLayout.CENTER);
      //Display the window.
      fr.pack();
      Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
      if (fr.getWidth() > 0.8 * size.width)
        fr.setSize(Math.round(0.8f * size.width), fr.getHeight());
      fr.setLocation((size.width - fr.getWidth()) / 2, size.height - fr.getHeight() - 40);
      fr.setVisible(true);
  
      plotPanel.setHighlighter(highlighter);
      plotPanel.setSelector(selector);
  
      if (changeListeners != null) {
        for (int i = 0; i < changeListeners.size(); i++)
          plotPanel.addChangeListener(changeListeners.get(i));
      }
    }
    else {
      //update the reachability plot
      plotPanel.updateObjectsOrder(objOrdered);
    }
  
    if (changeListeners != null) {
      ClustersAssignments clAss = makeClusters(objOrdered, Double.NaN);
      ChangeEvent e = new ChangeEvent(clAss);
      for (int i = 0; i < changeListeners.size(); i++)
        changeListeners.get(i).stateChanged(e);
    }
  }
}
