package TapasExplTreeViewer.vis;

import TapasDataReader.CommonExplanation;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

public class ExplanationsProjPlot2D extends ProjectionPlot2D {
  public static int minDotRadius=4, maxDotRadius=20;
  public static float hsbRed[]=Color.RGBtoHSB(255,0,0,null);
  public static float hsbBlue[]=Color.RGBtoHSB(0,0,255,null);
  
  public ArrayList<CommonExplanation> explanations = null;
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
