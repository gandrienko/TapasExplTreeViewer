package TapasExplTreeViewer.clustering;

import javax.swing.*;
import java.awt.*;

public class ClusterContent {
  /**
   * For each of N original objects contains true if the object is cluster member.
   */
  public boolean member[]=null;
  /**
   * Index of the object that is the medoid of the cluster
   */
  public int medoidIdx=-1;
  /**
   * Lower clusters in the hierarchy
   */
  public ClusterContent children[]=null;
  /**
   * Depth of the hierarchy below this cluster; 0 if no children.
   */
  public int hierDepth=0;
  
  public void initialize(int nObjects) {
    member=new boolean[nObjects];
    for (int i=0; i<nObjects; i++)
      member[i]=false;
  }
  
  public int getMemberCount() {
    if (member==null)
      return 0;
    int n=0;
    for (int i=0; i<member.length; i++)
      if (member[i])
        ++n;
    return n;
  }
  
  public void drawHierarchy(Graphics g, int x0, int y0, int stepX, int stepY, int width, int height) {
    if (g==null || getMemberCount()<1)
      return;
    if (stepX<=0)
      stepX=width/(hierDepth+1);
    if (stepY<=0)
      stepY=height/member.length;
    int n=getMemberCount();
    String txt=(n>1)?"("+n+";"+Integer.toString(medoidIdx)+")":Integer.toString(medoidIdx);
    g.drawString(txt,x0,y0+stepY-g.getFontMetrics().getDescent()-1);
    if (children!=null) {
      int h0=stepY*children[0].getMemberCount();
      int y1=y0+stepY-1, y2=y1+h0;
      g.drawLine(x0,y1,x0+stepX,y1);
      g.drawLine(x0+stepX/2,y1,x0+stepX/2,y2);
      g.drawLine(x0+stepX/2,y2,x0+stepX,y2);
      children[0].drawHierarchy(g,x0+stepX,y0,stepX,stepY,width,height);
      children[1].drawHierarchy(g,x0+stepX,y0+h0,stepX,stepY,width,height);
    }
  }
  
  public JPanel makePanel() {
    if (getMemberCount()<1)
      return null;
    JPanel p=new JPanel() {
      public Dimension getPreferredSize() {
        return new Dimension(50*(hierDepth+1),20*getMemberCount());
      }
      public void paintComponent(Graphics g) {
        g.setColor(getBackground());
        g.fillRect(0,0,getWidth()+1,getHeight()+1);
        g.setColor(Color.black);
        drawHierarchy(g,1,1,0,0,getWidth()-2,getHeight()-2);
      }
    };
    return p;
  }
  
  public static ClusterContent joinClusters(ClusterContent cc1, ClusterContent cc2) {
    if (cc1==null || cc2==null || cc1.getMemberCount()<1 || cc2.getMemberCount()<1)
      return null;
    ClusterContent cc=new ClusterContent();
    cc.member=new boolean[cc1.member.length];
    for (int i=0; i<cc.member.length; i++)
      cc.member[i]= cc1.member[i] || cc2.member[i];
    cc.hierDepth=1+Math.max(cc1.hierDepth,cc2.hierDepth);
    cc.children=new ClusterContent[2];
    cc.children[0]=cc1;
    cc.children[1]=cc2;
    return cc;
  }
}
