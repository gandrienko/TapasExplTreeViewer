package TapasExplTreeViewer.vis;

import TapasUtilities.ItemSelectionManager;
import TapasUtilities.MySammonsProjection;
import TapasUtilities.SingleHighlightManager;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class ProjectionPlot2D extends JPanel implements ChangeListener {
  public static Color dotColor=new Color(50,50,50,90),
      highlightColor=new Color(255,0,0,160),
      highlightFillColor=new Color(255,255,0,100),
      selectColor=new Color(0,0,0,200);
  public static int dotRadius=4, dotDiameter=dotRadius*2;
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
  /**
   * Minimum and maximum values of x- and y-coordinates
   */
  public double xMin=Double.NaN, xMax=xMin, yMin=xMin, yMax=xMin,
      xDiff=Double.NaN, yDiff=Double.NaN;
  public int xMarg=10, yMarg=10;
  public double scale=Double.NaN;
  
  /**
   * x- and y-coordinates of the drawn points
   */
  protected int px[]=null, py[]=null;
  
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
  
  public ProjectionPlot2D() {
    highlighter=new SingleHighlightManager();
    highlighter.addChangeListener(this);
    selector=new ItemSelectionManager();
    selector.addChangeListener(this);
    addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        super.mouseClicked(e);
        if (e.getClickCount()>1)
          selector.deselectAll();
        else {
          ArrayList<Integer> sel=getPointIndexesAtPosition(e.getX(),e.getY(),dotRadius*2);
          if (sel!=null)
            selector.select(sel);
        }
      }
      @Override
      public void mouseExited(MouseEvent e) {
        super.mouseExited(e);
        highlighter.clearHighlighting();
      }
    });
    addMouseMotionListener(new MouseAdapter() {
      @Override
      public void mouseMoved(MouseEvent e) {
        super.mouseMoved(e);
        int idx=getPointIndexAtPosition(e.getX(),e.getY(),dotRadius);
        if (idx<0)
          highlighter.clearHighlighting();
        else
          highlighter.highlight(new Integer(idx));
      }
    });
  }
  
  public SingleHighlightManager getHighlighter(){
    return highlighter;
  }
  
  public ItemSelectionManager getSelector() {
    return selector;
  }
  
  public void setDistanceMatrix(double distances[][]) {
    this.distances=distances;
    if (distances!=null) {
      ChangeListener listener=this;
      SwingWorker worker=new SwingWorker() {
        @Override
        public Boolean doInBackground(){
          sam=new MySammonsProjection(distances,2,250,true);
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
        xMin=xMax=yMin=yMax=xDiff=yDiff=Double.NaN;
        scale=Double.NaN;
        System.out.println("Projection plot: updating the 2D projection");
        off_Valid=false; off_selected_Valid=false;
        repaint();
      }
    }
    else
    if (e.getSource().equals(highlighter)) {
      if (!off_Valid)
        return;
      int idx=(highlighter.highlighted!=null &&
                   (highlighter.highlighted instanceof Integer))?(Integer)highlighter.highlighted:-1;
      if (hlIdx!=idx) {
        hlIdx=idx;
        if (off_Valid)
          redraw();
      }
    }
    else
      if (e.getSource().equals(selector)) {
        ArrayList currSel=selector.selected;
        if (sameContent(currSel,selected))
          return;
        if (currSel==null || currSel.isEmpty())
          selected.clear();
        else {
          if (selected==null)
            selected=new ArrayList<Integer>(100);
          selected.clear();
          for (int i=0; i<currSel.size(); i++)
            if (currSel.get(i) instanceof Integer)
              selected.add((Integer)currSel.get(i));
        }
        off_selected_Valid=false;
        if (off_Valid)
          redraw();
      }
  }
  
  /**
   * Assumes that both lists contain unique elements, no duplicates
   */
  public static boolean sameContent(ArrayList a1, ArrayList a2) {
    if (a1==null || a1.isEmpty())
      return a2==null || a2.isEmpty();
    if (a2==null || a2.isEmpty())
      return false;
    if (a1.size()!=a2.size())
      return false;
    for (int i=0; i<a1.size(); i++)
      if (!a2.contains(a1))
        return false;
    return true;
  }
  
  public void drawSelected(Graphics gr) {
    if (selected==null || selected.isEmpty() || proj==null || Double.isNaN(scale))
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
    
    Stroke origStr=g.getStroke();
    g.setStroke(new BasicStroke(2));
    g.setColor(selectColor);
    for (int j=0; j<selected.size(); j++) {
      int i=selected.get(j);
      g.drawOval(px[i]-dotRadius-1,py[i]-dotRadius-1,dotDiameter+2,dotDiameter+2);
    }
    g.setStroke(origStr);
    
    gr.drawImage(off_selected,0,0,null);
    off_selected_Valid=true;
  }
  
  public void drawHighlighted(Graphics gr) {
    if (hlIdx<0 || proj==null || Double.isNaN(scale))
      return;
    Graphics2D g=(Graphics2D)gr;
    Stroke origStr=g.getStroke();
    int x=px[hlIdx];
    int y=py[hlIdx];
    g.setColor(highlightFillColor);
    g.fillOval(x-dotRadius-1,y-dotRadius-1,dotDiameter+2,dotDiameter+2);
    g.setStroke(new BasicStroke(2));
    g.setColor(highlightColor);
    g.drawOval(x-dotRadius-1,y-dotRadius-1,dotDiameter+2,dotDiameter+2);
    g.setStroke(origStr);
  }
  
  public void paintComponent(Graphics gr) {
    if (gr==null)
      return;
    int w=getWidth(), h=getHeight();
    if (w<1 || h<1)
      return;
    if (off_Image!=null && off_Valid) {
      if (off_Image.getWidth()!=w || off_Image.getHeight()!=h) {
        off_Image = null; off_Valid=false;
        off_selected_Valid=false;
      }
      else {
        gr.drawImage(off_Image,0,0,null);
        drawSelected(gr);
        drawHighlighted(gr);
        return;
      }
    }
  
    if (off_Image==null || off_Image.getWidth()!=w || off_Image.getHeight()!=h)
      off_Image=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = off_Image.createGraphics();
  
    g.setColor(getBackground());
    g.fillRect(0,0,w+1,h+1);
    g.setColor(Color.black);
    g.drawRect(0,0,w-2,h-2);
    
    if (proj==null || proj[0].length!=2)
      return;
    
    if (Double.isNaN(xMin)) {
      xMin = proj[0][0]; xMax = xMin;
      yMin = proj[0][1]; yMax = yMin;
      for (int i = 1; i < proj.length; i++) {
        if (xMin > proj[i][0])
          xMin = proj[i][0];
        else
          if (xMax < proj[i][0])
            xMax = proj[i][0];
        if (yMin > proj[i][1])
          yMin = proj[i][1];
        else
          if (yMax < proj[i][1])
            yMax = proj[i][1];
      }
      xDiff = xMax - xMin;
      yDiff = yMax - yMin;
    }
    
    scale=Math.min((w-10)/xDiff,(h-10)/yDiff);
    xMarg=(int)Math.round((w-scale*xDiff)/2);
    yMarg=(int)Math.round((h-scale*yDiff)/2);
  
    RenderingHints rh = new RenderingHints(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON);
    g.setRenderingHints(rh);
    
    g.setColor(dotColor);
    if (px==null || px.length!=proj.length)
      px=new int[proj.length];
    if (py==null || py.length!=proj.length)
      py=new int[proj.length];
    for (int i=0; i<proj.length; i++) {
      px[i]=xMarg+(int)Math.round((proj[i][0]-xMin)*scale);
      py[i]=yMarg+(int)Math.round((proj[i][1]-yMin)*scale);
      g.drawOval(px[i]-dotRadius,py[i]-dotRadius,dotDiameter,dotDiameter);
    }
    gr.drawImage(off_Image,0,0,null);
    off_Valid=true;
    drawSelected(gr);
    drawHighlighted(gr);
  }
  
  public void redraw(){
    if (isShowing())
      paintComponent(getGraphics());
  }
  
  public int getPointIndexAtPosition(int x, int y, int tolerance) {
    if (px==null || py==null || x<xMarg || y<yMarg)
      return -1;
    int idx=-1, diffX=Integer.MAX_VALUE, diffY=diffX;
    for (int i=0; i<px.length; i++)
      if (Math.abs(px[i]-x)<=tolerance && Math.abs(py[i]-y)<=tolerance) {
        int dx=Math.abs(px[i]-x), dy=Math.abs(py[i]-y);
        if (idx<0 || dx+dy<diffX+diffY) {
          idx=i; diffX=dx; diffY=dy;
        }
      }
    return idx;
  }
  
  public ArrayList<Integer> getPointIndexesAtPosition(int x,int y,int radius) {
    if (px==null || py==null || x<xMarg || y<yMarg)
      return null;
    int sqRadius=radius*radius;
    ArrayList<Integer> indexes=new ArrayList<Integer>(50);
    for (int i=0; i<px.length; i++)
      if (Math.abs(px[i]-x)<radius && Math.abs(py[i]-y)<radius) {
        int dx=px[i]-x, dy=py[i]-y;
        dx*=dx; dy*=dy;
        if (dx+dy<sqRadius)
          indexes.add(i);
      }
    if (indexes.isEmpty())
      return null;
    return indexes;
  }
}
