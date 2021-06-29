package TapasExplTreeViewer.clustering;

import it.unipi.di.sax.optics.ClusterObject;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.ArrayList;

public class ReachPlotPanel extends JPanel implements ChangeListener {
  /**
   * Objects ordered by the clustering algorithm
   */
  protected ArrayList<ClusterObject<Integer>> objOrdered = null;
  public ReachabilityPlot rPlot=null;
  
  protected JLabel labTop=null;
  protected JSlider thresholdSlider=null;
  protected JTextField tfThreshold=null;
  
  public ReachPlotPanel(ArrayList<ClusterObject<Integer>> objOrdered) {
    this.objOrdered=objOrdered;
    rPlot=new ReachabilityPlot(objOrdered);
    
    setLayout(new BorderLayout());
    JScrollPane scp=new JScrollPane(rPlot);
    scp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
    add(scp,BorderLayout.CENTER);
    
    labTop=new JLabel("Maximal reachability distance: "+rPlot.getMaxDistance(),JLabel.CENTER);
    add(labTop,BorderLayout.NORTH);
    
    tfThreshold=new JTextField(10);
    JPanel p=new JPanel(new FlowLayout(FlowLayout.CENTER,5,0));
    p.add(new JLabel("Distance threshold for clustering:"));
    p.add(tfThreshold);
    
    thresholdSlider=new JSlider(JSlider.HORIZONTAL,1,1000,1000);
    JPanel bp=new JPanel(new BorderLayout());
    bp.add(p,BorderLayout.WEST);
    bp.add(thresholdSlider,BorderLayout.CENTER);
    add(bp,BorderLayout.SOUTH);
    thresholdSlider.addChangeListener(this);
  }
  
  public void stateChanged(ChangeEvent e) {
    if (e.getSource().equals(thresholdSlider)) {
      double th=rPlot.getMaxDistance()*thresholdSlider.getValue()/thresholdSlider.getMaximum();
      rPlot.setThreshold(th);
    }
  }
}
