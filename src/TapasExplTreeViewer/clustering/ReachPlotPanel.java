package TapasExplTreeViewer.clustering;

import it.unipi.di.sax.optics.ClusterObject;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

public class ReachPlotPanel extends JPanel implements ChangeListener, ActionListener {
  /**
   * Objects ordered by the clustering algorithm
   */
  protected ArrayList<ClusterObject> objOrdered = null;
  public ReachabilityPlot rPlot=null;
  
  protected JLabel labTop=null;
  protected JSlider thresholdSlider=null;
  protected JTextField tfThreshold=null;
  
  public ReachPlotPanel(ArrayList<ClusterObject> objOrdered) {
    this.objOrdered=objOrdered;
    rPlot=new ReachabilityPlot(objOrdered);
    
    setLayout(new BorderLayout());
    JScrollPane scp=new JScrollPane(rPlot);
    scp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
    add(scp,BorderLayout.CENTER);
    
    labTop=new JLabel("Maximal reachability distance: "+String.format("%.5f",rPlot.getMaxDistance()),JLabel.CENTER);
    add(labTop,BorderLayout.NORTH);
    
    tfThreshold=new JTextField(10);
    JPanel p=new JPanel(new FlowLayout(FlowLayout.CENTER,5,0));
    p.add(new JLabel("Distance threshold for clustering:"));
    p.add(tfThreshold);
    tfThreshold.addActionListener(this);
    
    thresholdSlider=new JSlider(JSlider.HORIZONTAL,1,1000,1000);
    JPanel bp=new JPanel(new BorderLayout());
    bp.add(p,BorderLayout.WEST);
    bp.add(thresholdSlider,BorderLayout.CENTER);
    bp.add(new JLabel("max = "+String.format("%.5f",rPlot.getMaxDistance())),BorderLayout.EAST);
    add(bp,BorderLayout.SOUTH);
    thresholdSlider.addChangeListener(this);
  }
  
  public void stateChanged(ChangeEvent e) {
    if (e.getSource().equals(thresholdSlider)) {
      double th=rPlot.getMaxDistance()*thresholdSlider.getValue()/thresholdSlider.getMaximum();
      rPlot.setThreshold(th);
      makeAndShowClusters();
      String str=String.format("%.5f",th);
      tfThreshold.setText(str);
    }
  }
  
  public void actionPerformed(ActionEvent e){
    if (e.getSource().equals(tfThreshold)) {
      double th=Double.NaN;
      try {
        th=Double.parseDouble(tfThreshold.getText());
      } catch (Exception ex) {}
      if (Double.isNaN(th) || th<=0 || th>=rPlot.getMaxDistance()) {
        String str=String.format("%.5f",rPlot.getThreshold());
        tfThreshold.setText(str);
      }
      else {
        rPlot.setThreshold(th);
        makeAndShowClusters();
      }
    }
  }
  
  public void makeAndShowClusters(){
    ClustersAssignments clAss=ClustererByOPTICS.makeClusters(objOrdered,rPlot.getThreshold());
    rPlot.setCusterAssignments(clAss);
    if (clAss!=null)
      labTop.setText(clAss.nClusters + " clusters; " + clAss.nNoise + " objects in noise; max cluster size = " +
                         clAss.maxSize + ", min cluster size = " + clAss.minSize);
    else
      labTop.setText("No clusters!");
    labTop.setSize(labTop.getPreferredSize());
    notifyChange(clAss);
  }
  
  protected ArrayList<ChangeListener> changeListeners=null;
  
  public void addChangeListener(ChangeListener l) {
    if (changeListeners==null)
      changeListeners=new ArrayList(5);
    if (!changeListeners.contains(l))
      changeListeners.add(l);
  }
  
  public void removeChangeListener(ChangeListener l) {
    if (l!=null && changeListeners!=null)
      changeListeners.remove(l);
  }
  
  public void notifyChange(ClustersAssignments clAss){
    if (changeListeners==null || changeListeners.isEmpty())
      return;
    ChangeEvent e=new ChangeEvent(clAss);
    for (ChangeListener l:changeListeners)
      l.stateChanged(e);
  }
}