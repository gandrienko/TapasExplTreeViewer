package TapasExplTreeViewer.vis;

import TapasDataReader.CommonExplanation;
import TapasExplTreeViewer.MST.Edge;
import TapasExplTreeViewer.MST.Vertex;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

public class ExplanationsProjPlot2D extends ProjectionPlot2D {
  public static int minDotRadius=4, maxDotRadius=20;
  public static float hsbRed[]=Color.RGBtoHSB(255,0,0,null);
  public static float hsbBlue[]=Color.RGBtoHSB(0,0,255,null);
  public static Color linkColor=new Color(0,0,0,80);
  
  public ArrayList<CommonExplanation> explanations = null;
  /**
   * The graphs represent connections between rules when they are aggregated.
   * The labels of the vertices are string representations of the rule indexes in the list.
   */
  public HashSet<ArrayList<Vertex>> graphs=null;
  
  public int maxNUses = 0;
  public int minAction=Integer.MAX_VALUE, maxAction=Integer.MIN_VALUE;
  public boolean sameAction=true;
  public double minQ=Double.NaN, maxQ=Double.NaN;
  public int maxRadius=maxDotRadius;
  
  public ExplanationsProjPlot2D(){
    ToolTipManager.sharedInstance().registerComponent(this);
    ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
  }
  
  public ExplanationsProjPlot2D(ArrayList<CommonExplanation> explanations, double coords[][]) {
    super(coords);
    ToolTipManager.sharedInstance().registerComponent(this);
    ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
    setExplanations(explanations);
  }
  
  public void setExplanations(ArrayList<CommonExplanation> explanations) {
    this.explanations = explanations;
    maxNUses = 0;
    if (explanations != null && !explanations.isEmpty())
      for (int i = 0; i < explanations.size(); i++) {
        CommonExplanation ex=explanations.get(i);
        if (maxNUses < ex.nUses)
          maxNUses = ex.nUses;
        if (minAction>ex.action)
          minAction=ex.action;
        if (maxAction<ex.action)
          maxAction=ex.action;
        if (!Double.isNaN(ex.meanQ)) {
          if (Double.isNaN(minQ) || minQ>ex.meanQ)
            minQ=ex.minQ;
          if (Double.isNaN(maxQ) || maxQ<ex.meanQ)
            maxQ=ex.meanQ;
        }
      sameAction=minAction==maxAction;
    }
    maxRadius=(maxNUses<maxDotRadius)?minDotRadius+maxNUses-1:maxDotRadius;
    off_Valid=off_selected_Valid=false;
    if (isShowing())
      repaint();
  }
  
  public HashSet<ArrayList<Vertex>> getGraphs() {
    return graphs;
  }
  
  public void setGraphs(HashSet<ArrayList<Vertex>> graphs) {
    this.graphs = graphs;
  }
  
  public void drawPoint(Graphics2D g, int pIdx, int x, int y, boolean highlighted, boolean selected) {
    if (explanations == null || explanations.isEmpty() || pIdx < 0 || pIdx >= explanations.size()) {
      super.drawPoint(g, pIdx, x, y, highlighted, selected);
      return;
    }
    CommonExplanation ex=explanations.get(pIdx);
    int dotRadius=minDotRadius+Math.round(1f*ex.nUses/maxNUses*(maxRadius-minDotRadius)),
      dotDiameter=2*dotRadius;
    Color color=dotColor;
    if (!highlighted && !selected) {
      color=(sameAction)?getColorForQ(ex.meanQ,minQ,maxQ):getColorForAction(ex.action,minAction,maxAction);
      g.setColor(color);
      g.fillOval(x-dotRadius-1,y-dotRadius-1,dotDiameter+2,dotDiameter+2);
    }
    else
      if (highlighted) {
        g.setColor(highlightFillColor);
        g.fillOval(x-dotRadius-1,y-dotRadius-1,dotDiameter+2,dotDiameter+2);
      }
    Stroke origStr=(selected || highlighted)?g.getStroke():null;
    if (selected || highlighted)
      g.setStroke(strokeSelected);
    g.setColor((highlighted)?highlightColor:(selected)?selectColor:color.darker());
    g.drawOval(x-dotRadius,y-dotRadius,dotDiameter,dotDiameter);
    if (origStr!=null)
      g.setStroke(origStr);
  }
  
  public void drawPoints(Graphics2D g) {
    super.drawPoints(g);
    drawLinks(g);
  }
  
  public void drawLinks(Graphics2D gr) {
    if (graphs==null || graphs.isEmpty())
      return;
    if (px==null || py==null)
      return;
    gr.setColor(linkColor);
    for (ArrayList<Vertex> graph:graphs)
      drawLinks(gr,graph);
  }
  
  public void drawLinks(Graphics2D gr, ArrayList<Vertex> graph) {
    if (graph==null || graph.isEmpty())
      return;
    for (Vertex v:graph) {
      int idx0=-1;
      try {
        idx0=Integer.parseInt(v.getLabel());
      } catch (Exception ex) {}
      if (idx0<0 || idx0>=px.length)
        continue;
      Iterator<Map.Entry<Vertex,Edge>> it = v.getEdges().entrySet().iterator();
      while (it.hasNext()) {
        Map.Entry<Vertex, Edge> pair = it.next();
        if (pair.getValue().isIncluded()) {
          Vertex v2=pair.getKey();
          int idx2=-1;
          try {
            idx2=Integer.parseInt(v2.getLabel());
          } catch (Exception ex) {}
          if (idx2>=0 && idx2<px.length)
            drawLinkBetweenPoints(gr,idx0,idx2);
        }
      }
    }
  }
  
  public void drawLinkBetweenPoints(Graphics gr, int idx1, int idx2) {
    gr.drawLine(px[idx1],py[idx1],px[idx2],py[idx2]);
  }
 
  public String getToolTipText(MouseEvent me) {
    if (!isShowing())
      return null;
    if (me.getButton() != MouseEvent.NOBUTTON)
      return null;
    int idx=getPointIndexAtPosition(me.getX(),me.getY(),dotRadius);
    if (idx<0)
      return null;
    return explanations.get(idx).toHTML(null);
  }
  
  public static Color getColorForAction(int action, int minAction, int maxAction) {
    if (minAction==maxAction || action < minAction || action>maxAction)
      return Color.darkGray;
    float actionRatio = ((float)maxAction-action) / (maxAction-minAction);
    Color color = Color.getHSBColor(actionRatio * (hsbBlue[0] - hsbRed[0]),1,1);
    return new Color(color.getRed(),color.getGreen(),color.getBlue(),100);
  }
  
  public static Color getColorForQ(double q, double minQ, double maxQ) {
    if (Double.isNaN(minQ) || Double.isNaN(maxQ) || Double.isNaN(q) || minQ>=maxQ || q<minQ || q>maxQ)
      return Color.darkGray;
    float ratio=(float)((maxQ-q)/(maxQ-minQ));
    Color color = Color.getHSBColor(ratio * (hsbBlue[0] - hsbRed[0]),1,1);
    return new Color(color.getRed(),color.getGreen(),color.getBlue(),100);
  }
}
