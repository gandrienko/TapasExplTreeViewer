package TapasExplTreeViewer.vis;

import TapasUtilities.MySammonsProjection;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

public class ProjectionPlot2D extends JPanel implements ChangeListener {
  public static Color dotColor=new Color(50,50,50,50);
  public static int dotRadius=5, dotDiameter=dotRadius*2;
  /**
   * Matrix of distances between the objects to project and show
   */
  public double distances[][]=null;
  /**
   * Creates a projection based on the distance matrix
   */
  protected MySammonsProjection sam=null;
  /**
   * The projection obtained (updated iteratively)
   */
  protected double proj[][]=null;
  
  public void setDistanceMatrix(double distances[][]) {
    this.distances=distances;
    if (distances!=null) {
      ChangeListener listener=this;
      SwingWorker worker=new SwingWorker() {
        @Override
        public Boolean doInBackground(){
          sam=new MySammonsProjection(distances,2,300,true);
          sam.runProjection(5,50,listener);
          return true;
        }
        @Override
        protected void done() {
        }
      };
      worker.execute();
    }
  }
  
  public void stateChanged(ChangeEvent e) {
    if (e.getSource() instanceof MySammonsProjection) {
      MySammonsProjection sam = (MySammonsProjection) e.getSource();
      proj =(sam.done)?sam.getProjection():sam.bestProjection;
      if (proj != null && proj[0].length==2) {
        System.out.println("Projection plot: updating the 2D projection");
        repaint();
      }
    }
  }
  
  public void paintComponent(Graphics gr) {
    if (gr==null)
      return;
    int w=getWidth(), h=getHeight();
    if (w<1 || h<1)
      return;
  
    gr.setColor(getBackground());
    gr.fillRect(0,0,w+1,h+1);
    
    if (proj==null || proj[0].length!=2)
      return;
    
    double xMin=proj[0][0], xMax=xMin, yMin=proj[0][1], yMax=yMin;
    for (int i=1; i<proj.length; i++) {
      if (xMin>proj[i][0])
        xMin=proj[i][0];
      else
        if (xMax<proj[i][0])
          xMax=proj[i][0];
      if (yMin>proj[i][1])
        yMin=proj[i][1];
      else
        if (yMax<proj[i][1])
          yMax=proj[i][1];
    }
    double xDiff=xMax-xMin, yDiff=yMax-yMin;
    
    int xMarg=10, yMarg=10, plotW=w-(2*xMarg), plotH=h-(2*yMarg);
    double scale=Math.min(plotW/xDiff,plotH/yDiff);
    xMarg=(int)Math.round((w-scale*xDiff)/2);
    yMarg=(int)Math.round((h-scale*yDiff)/2);
  
    Graphics2D g=(Graphics2D)gr;
    RenderingHints rh = new RenderingHints(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON);
    g.setRenderingHints(rh);
    
    g.setColor(dotColor);
    for (int i=0; i<proj.length; i++) {
      int x=xMarg+(int)Math.round((proj[i][0]-xMin)*scale);
      int y=yMarg+(int)Math.round((proj[i][1]-yMin)*scale);
      g.drawOval(x-dotRadius,y-dotRadius,dotDiameter,dotDiameter);
    }
  }
}
