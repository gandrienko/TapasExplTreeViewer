package TapasExplTreeViewer.vis;

import TapasDataReader.CommonExplanation;
import TapasExplTreeViewer.ui.ShowSingleRule;
import TapasUtilities.ItemSelectionManager;
import TapasUtilities.SingleHighlightManager;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Vector;

/**
 * A panel with multiple rules represented by glyphs.
 */
public class RuleSetVis extends JPanel
  implements ChangeListener {
  
  protected static int minFeatureW=20, minGlyphH=50, space=20, margin=10;
  public static float hsbRed[]=Color.RGBtoHSB(255,0,0,null);
  public static float hsbBlue[]=Color.RGBtoHSB(0,0,255,null);
  /**
   * The rules or explanations to be visualized. The elements are instances of
   * CommonExplanation or UnitedRule.
   */
  public ArrayList exList=null;
  /**
   * The ranges of feature values
   */
  public Hashtable<String,float[]> attrMinMax=null;
  
  protected int minAction=Integer.MAX_VALUE, maxAction=Integer.MIN_VALUE;
  protected double minQValue=Double.NaN, maxQValue=Double.NaN;
  /**
   * used for rendering glyphs
   */
  protected Vector<String> attrs=null;
  protected Vector<float[]> minmax=null;
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
  protected BufferedImage off_Image=null;
  protected boolean off_Valid=false;
  
  public RuleSetVis(ArrayList exList, ArrayList fullExList,
                    Vector<String> attrNamesOrdered, Hashtable<String,float[]> attrMinMax) {
    this.exList=exList; this.attrMinMax=attrMinMax;
    attrs=attrNamesOrdered;
    if (attrs!=null && attrMinMax!=null) {
      minmax=new Vector<float[]>(attrs.size());
      for (int i=0; i<attrs.size(); i++)
        minmax.add(attrMinMax.get(attrs.elementAt(i)));
    }
    ArrayList rules=(fullExList!=null)?fullExList:exList;
    for (int i=0; i<rules.size(); i++) {
      CommonExplanation ex = (CommonExplanation) rules.get(i);
      if (minAction > ex.action)
        minAction = ex.action;
      if (maxAction < ex.action)
        maxAction = ex.action;
      if (!Double.isNaN(ex.meanQ)) {
        if (Double.isNaN(minQValue) || minQValue > ex.minQ)
          minQValue = ex.minQ;
        if (Double.isNaN(maxQValue) || maxQValue < ex.maxQ)
          maxQValue = ex.maxQ;
      }
    }
  }
  
  public SingleHighlightManager getHighlighter(){
    return highlighter;
  }
  
  public ItemSelectionManager getSelector() {
    return selector;
  }
  
  public void setHighlighter(SingleHighlightManager highlighter) {
    if (this.highlighter!=null)
      if (this.highlighter.equals(highlighter))
        return;
      else
        this.highlighter.removeChangeListener(this);
    this.highlighter = highlighter;
    if (highlighter!=null)
      highlighter.addChangeListener(this);
  }
  
  public void setSelector(ItemSelectionManager selector) {
    if (this.selector!=null)
      if (this.selector.equals(selector))
        return;
      else
        this.selector.removeChangeListener(this);
    this.selector = selector;
    if (selector!=null)
      selector.addChangeListener(this);
  }
  
  public void redraw(){
    if (isShowing())
      paintComponent(getGraphics());
  }
  
  public void drawHighlighted(Graphics gr) {
    if (hlIdx<0)
      return;
    //todo ...
  }
  
  public static Color getColorForAction(int action, int minAction, int maxAction) {
    if (minAction==maxAction || action < minAction || action>maxAction)
      return Color.gray;
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
  
  public void drawRuleGlyph(Graphics2D gr,
                            CommonExplanation ex, Vector<CommonExplanation> exSelected,
                            int x, int y, int w, int h) {
    if (gr==null || ex==null)
      return;
    BufferedImage ruleImage= ShowSingleRule.getImageForRule(w,h,ex,exSelected,attrs,minmax);
    if (ruleImage==null)
      return;
    gr.drawImage(ruleImage,x,y,null);
    Color c=(minAction<maxAction)?getColorForAction(ex.action,minAction,maxAction):
                getColorForQ(ex.meanQ,minQValue,maxQValue);
    gr.setColor(c);
    Stroke str=gr.getStroke();
    gr.setStroke(new BasicStroke(2));
    gr.drawRect(x,y,w,h);
    if (exSelected!=null && exSelected.contains(ex)) {
      gr.setColor(Color.black);
      gr.drawRect(x-2,y-2,w+4,h+4);
    }
    gr.setStroke(str);
  }
  
  public Dimension getPreferredSize() {
    if (exList==null || exList.isEmpty())
      return new Dimension(100,50);
    int minGlyphW=attrs.size()*minFeatureW;
    int nGlyphsInRow=Math.min(exList.size(),5),
        nRows=(int)Math.round(Math.ceil(1.0*exList.size()/nGlyphsInRow));
    return new Dimension(2 * margin + nGlyphsInRow * minGlyphW + (nGlyphsInRow-1) * space,
        2*margin+nRows*minGlyphH+(nRows-1)*space);
  }
  
  public void paintComponent(Graphics gr) {
    if (exList==null || exList.isEmpty() || attrs==null || attrs.isEmpty())
      return;
    if (gr==null)
      return;
    int w=getWidth(), h=getHeight();
    if (w<1 || h<1)
      return;
    if (getParent() instanceof JViewport) {
      JViewport vp=(JViewport)getParent();
      if (w>vp.getWidth())
        w=vp.getWidth();
    }
    
    if (off_Image!=null && off_Valid) {
      if (off_Image.getWidth()!=w || off_Image.getHeight()!=h) {
        off_Image = null; off_Valid=false;
      }
      else {
        gr.drawImage(off_Image,0,0,null);
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
    
    //determine glyph dimensions
    int glyphW=attrs.size()*minFeatureW;
    int nGlyphsInRow=Math.max(1,Math.min(exList.size(),(w-2*margin+space)/(glyphW+space)));
    glyphW=(w-2*margin)/nGlyphsInRow-space;
    int nRows=(int)Math.round(Math.ceil(1.0*exList.size()/nGlyphsInRow));
    int glyphH=Math.max((h-2*margin+space)/nRows-space,minGlyphH);
  
    Vector<CommonExplanation> exSelected=(selected==null || selected.isEmpty())?null:
                                             new Vector<CommonExplanation>(selected.size());
    if (exSelected!=null)
      for (int i=0; i<exList.size(); i++)
        if (selected.contains(((CommonExplanation)exList.get(i)).numId))
          exSelected.addElement((CommonExplanation)exList.get(i));
    
    int x=margin, y=margin, nInRow=0;
    for (int i=0; i<exList.size(); i++) {
      drawRuleGlyph(g,(CommonExplanation)exList.get(i),exSelected,x,y,glyphW,glyphH);
      ++nInRow;
      if (nInRow<nGlyphsInRow)
        x+=glyphW+space;
      else {
        x=margin; y+=glyphH+space; nInRow=0;
      }
    }
    gr.drawImage(off_Image,0,0,null);
    off_Valid=true;
    drawHighlighted(gr);
    
    if (w<getWidth())
      setSize(w,y+glyphH+margin);
  }
  
  public void stateChanged(ChangeEvent e) {
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
        if (ItemSelectionManager.sameContent(currSel,selected))
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
        off_Valid=false;
        redraw();
      }
  }
}
