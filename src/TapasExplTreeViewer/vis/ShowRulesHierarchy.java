package TapasExplTreeViewer.vis;

import TapasExplTreeViewer.rules.UnitedRule;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class ShowRulesHierarchy extends JPanel {
  /**
   * The aggregated rules to visualise
   */
  public ArrayList<UnitedRule> rules=null;
  /**
   * Number of rules in the hierarchy
   */
  public int nRules=0;
  /**
   * Maximal depth of the hierarchy
   */
  public int hierDepth=0;
  /**
   * The vertical size, in pixels, given to one rule
   */
  public int ruleHeight=10;
  /**
   * The horizontal indentation for each next level
   */
  public int indent=20;
  public int margin=10;
  
  public ShowRulesHierarchy(ArrayList<UnitedRule> rules) {
    this.rules=rules;
    if (rules!=null && !rules.isEmpty()) {
      nRules=UnitedRule.countRulesInHierarchy(rules);
      hierDepth=UnitedRule.getMaxHierarchyDepth(rules);
      setPreferredSize(new Dimension(indent*hierDepth+margin, ruleHeight * nRules));
    }
  }
  
  public int getRuleHeight() {
    return ruleHeight;
  }
  
  public void setRuleHeight(int ruleHeight) {
    this.ruleHeight = ruleHeight;
    if (nRules>0)
      setPreferredSize(new Dimension(indent*hierDepth+margin, ruleHeight * nRules));
  }
  
  public Point drawRuleHierarchy(UnitedRule rule, Graphics g, Point p0) {
    if (rule==null)
      return p0;
    int y0=p0.y+ruleHeight/2;
    g.drawOval(p0.x-2,y0-2,4,4);
    g.fillOval(p0.x-2,y0-2,4,4);
    int x=p0.x+indent, y=p0.y+ruleHeight;
    if (rule.fromRules!=null)
      for (int i=0; i<rule.fromRules.size(); i++) {
        Point p=drawRuleHierarchy(rule.fromRules.get(i),g,new Point(x,y));
        int y2=y-ruleHeight/2;
        g.drawLine(p0.x,y0,p0.x,y2);
        g.drawLine(p0.x,y2,p.x,y2);
        y=p.y;
      }
    return new Point(p0.x,y);
  }
  
  public void paintComponent(Graphics g){
    if (rules==null || rules.isEmpty())
      return;;
    int w=getWidth(), h=getHeight();
    if (w<10 || h<10)
      return;
    g.setColor(Color.black);
    //for (int i=0; i<rules.size(); i++)
  }
}
