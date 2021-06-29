package TapasExplTreeViewer.clustering;

import TapasUtilities.ItemSelectionManager;
import TapasUtilities.SingleHighlightManager;
import it.unipi.di.sax.optics.ClusterObject;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class ReachabilityPlot extends JPanel {
  public static int minBarW=2;
  /**
   * Objects ordered by the clustering algorithm
   */
  protected ArrayList<ClusterObject<Integer>> objOrdered = null;
  public double maxDistance=Double.NaN;
  public double threshold=Double.NaN;
  
  protected int barW=minBarW;
  /**
   * Highlighting and selection
   */
  protected SingleHighlightManager highlighter=null;
  protected ItemSelectionManager selector=null;
  public int hlIdx=-1;
  public ArrayList<Integer> selected=null;
  /**
   * Used to speed up redrawing
   */
  protected BufferedImage off_Image=null, off_selected=null;
  protected boolean off_Valid=false, off_selected_Valid =false;
  
  public ReachabilityPlot (ArrayList<ClusterObject<Integer>> objOrdered) {
    this.objOrdered=objOrdered;
    if (objOrdered!=null && !objOrdered.isEmpty()) {
      setPreferredSize(new Dimension(Math.max(1200, minBarW * objOrdered.size()+10), 300));
      for (int i=0; i<objOrdered.size(); i++) {
        double rd=objOrdered.get(i).getReachabilityDistance(), cd=objOrdered.get(i).getCoreDistance();
        if (!Double.isNaN(rd) && !Double.isInfinite(rd) && (Double.isNaN(maxDistance) || maxDistance<rd))
          maxDistance=rd;
        if (!Double.isNaN(cd) && !Double.isInfinite(cd) && (Double.isNaN(maxDistance) || maxDistance<cd))
          maxDistance=cd;
      }
    }
  }
  
  public double getMaxDistance() {
    return maxDistance;
  }
  
  public void setThreshold(double threshold) {
    this.threshold = threshold;
    if (isShowing())
      redraw();
  }
  
  public void drawSelected(Graphics gr) {
    if (selected==null || selected.isEmpty() || objOrdered==null || Double.isNaN(maxDistance))
      return;
    int w=getWidth(), h=getHeight();
    if (w<1 || h<1)
      return;
    if (off_selected!=null && off_selected_Valid) {
      if (off_selected.getWidth()!=w || off_selected.getHeight()!=h) {
        off_selected = null; off_selected_Valid =false;
      }
      else {
        gr.drawImage(off_selected,0,0,null);
        return;
      }
    }
    
    off_selected=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = off_selected.createGraphics();
    
    for (int j=0; j<selected.size(); j++) {
      int i=selected.get(j);
      //todo: draw the bar
    }
    
    gr.drawImage(off_selected,0,0,null);
    off_selected_Valid=true;
  }
  
  public void drawHighlighted(Graphics gr) {
    if (hlIdx<0 || objOrdered==null || Double.isNaN(maxDistance))
      return;
    //todo: draw the bar
  }
  
  public void paintComponent(Graphics gr) {
    if (gr==null)
      return;
    int w=getWidth(), h=getHeight();
    if (w<1 || h<1)
      return;
    double scale=(h-10)/maxDistance;
    if (off_Image!=null && off_Valid) {
      if (off_Image.getWidth()!=w || off_Image.getHeight()!=h) {
        off_Image = null; off_Valid=false;
        off_selected_Valid=false;
      }
      else {
        gr.drawImage(off_Image,0,0,null);
        drawSelected(gr);
        drawHighlighted(gr);
  
        if (!Double.isNaN(threshold) && threshold>0 && threshold<maxDistance){
          int th=(int)Math.round(scale*threshold);
          gr.setColor(Color.red);
          gr.drawLine(0,h-5-th,w,h-5-th);
        }
        return;
      }
    }
  
    if (off_Image==null || off_Image.getWidth()!=w || off_Image.getHeight()!=h)
      off_Image=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = off_Image.createGraphics();
  
    g.setColor(getBackground());
    g.fillRect(0,0,w+1,h+1);
    
    if (objOrdered==null || Double.isNaN(maxDistance))
      return;
    
    barW=Math.max(minBarW,(w-10)/objOrdered.size());
    
    int x=5;
    g.setColor(Color.gray);
    for (int i=0; i<objOrdered.size(); i++) {
      double rd = objOrdered.get(i).getReachabilityDistance(), cd = objOrdered.get(i).getCoreDistance();
      int barH=(int)Math.round((Double.isNaN(rd) || Double.isInfinite(rd))?
                                   (Double.isNaN(cd) || Double.isInfinite(cd))?0:scale*cd:scale*rd);
      if (barH>0)
        g.fillRect(x,h-5-barH,barW,barH);
      x+=barW;
    }
  
    gr.drawImage(off_Image,0,0,null);
    off_Valid=true;
    drawSelected(gr);
    drawHighlighted(gr);
    
    if (!Double.isNaN(threshold) && threshold>0 && threshold<maxDistance){
      int th=(int)Math.round(scale*threshold);
      g.setColor(Color.red);
      g.drawLine(0,h-5-th,w,h-5-th);
    }
  }
  
  public void redraw(){
    if (isShowing())
      paintComponent(getGraphics());
  }
}

