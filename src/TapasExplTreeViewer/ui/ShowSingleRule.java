package TapasExplTreeViewer.ui;

import TapasDataReader.CommonExplanation;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Vector;

public class ShowSingleRule {

  public static BufferedImage getImageForRule (int w, int h, CommonExplanation ex,
                                               Vector<CommonExplanation> vex,
                                               Vector<String> attrs,
                                               Vector<float[]> minmax) {
    BufferedImage image=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
    int offsetX=3, offsetY=2;
    int dx=(w-2*offsetX) / attrs.size(),
        dy=h-2*offsetY;
    offsetX=(w-attrs.size()*dx)/2;
    boolean useThickerLines=(w/ex.eItems.length>15) && h>=40;
    int barW=(useThickerLines)?6:4;
    Graphics2D g = image.createGraphics();
    Stroke s=g.getStroke();
    for (int i=0; i<attrs.size(); i++) {
      g.setColor(Color.lightGray);
      int x=offsetX+i*dx+dx/2;
      g.drawLine(x,offsetY,x,offsetY+dy);
      boolean found=false;
      for (int j=0; j<ex.eItems.length && !found; j++)
        if (attrs.elementAt(i).equals(ex.eItems[j].attr)) {
          g.setColor(Color.darkGray);
          int y[]=new int[2];
          for (int k=0; k<y.length; k++) {
            double v=ex.eItems[j].interval[k];
            if (v==Double.NEGATIVE_INFINITY)
              v=minmax.elementAt(i)[0];
            if (v==Double.POSITIVE_INFINITY)
              v=minmax.elementAt(i)[1];
            y[k]=(int)Math.round(dy*(v-minmax.elementAt(i)[0])/(minmax.elementAt(i)[1]-minmax.elementAt(i)[0]));
          }
          if (useThickerLines)
            g.setStroke(new BasicStroke(2));
          g.drawRect(x-2,offsetY+dy-y[1],barW,y[1]-y[0]-1);
          if (useThickerLines)
            g.setStroke(s);
          found=true;
        }
      Color cblue=new Color(0,127,127, 63);
      if (vex!=null)
        for (int idx=0; idx<vex.size(); idx++) {
          found=false;
          for (int j=0; j<vex.elementAt(idx).eItems.length && !found; j++)
            if (attrs.elementAt(i).equals(vex.elementAt(idx).eItems[j].attr)) {
              g.setColor(cblue);
              int y[]=new int[2];
              for (int k=0; k<y.length; k++) {
                double v=vex.elementAt(idx).eItems[j].interval[k];
                if (v==Double.NEGATIVE_INFINITY)
                  v=minmax.elementAt(i)[0];
                if (v==Double.POSITIVE_INFINITY)
                  v=minmax.elementAt(i)[1];
                y[k]=(int)Math.round(dy*(v-minmax.elementAt(i)[0])/(minmax.elementAt(i)[1]-minmax.elementAt(i)[0]));
              }
              g.fillRect(x+2,offsetY+dy-y[1],barW+1,y[1]-y[0]);
              found=true;
            }
        }
    }
    return image;
  }

}
