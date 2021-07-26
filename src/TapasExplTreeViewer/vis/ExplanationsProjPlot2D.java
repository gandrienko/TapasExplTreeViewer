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
      for (int i = 0; i < explanations.size(); i++)
        if (maxNUses < explanations.get(i).nUses)
          maxNUses = explanations.get(i).nUses;
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
    int dotRadius=minDotRadius+Math.round(1f*ex.nUses/maxNUses*(maxDotRadius-minDotRadius)),
      dotDiameter=2*dotRadius;
    Color color=dotColor;
    if (!highlighted && !selected) {
      color=getColorForAction(ex.action);
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
  
  public static Color getColorForAction(int action) {
    float actionRatio = (10 - ((float) action)) / 10;
    Color color = Color.getHSBColor(actionRatio * (hsbBlue[0] - hsbRed[0]),1,1);
    return new Color(color.getRed(),color.getGreen(),color.getBlue(),100);
  }
}
