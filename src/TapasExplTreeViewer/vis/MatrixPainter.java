package TapasExplTreeViewer.vis;

import TapasUtilities.MySammonsProjection;
import TapasUtilities.gunther_foidl.SammonsProjection;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.util.ArrayList;

public class MatrixPainter extends JPanel  {
  public static Color minColor = new Color(255, 255, 204); // Light yellow
  public static Color maxColor = new Color(102, 51, 0);    // Dark brown

  public double matrix[][]=null;
  public int order[]=null;

  public MatrixPainter(double matrix[][]) {
    this.matrix=matrix;
    setPreferredSize(new Dimension(500,500));
    if (matrix!=null) {
      SammonsProjection sam=new SammonsProjection(matrix,1, 1000,true);
      sam.CreateMapping();
      double proj[][]=sam.getProjection();
      if (proj!=null) {
        ArrayList<Integer> ord=new ArrayList<Integer>(proj.length);
        ord.add(0);
        for (int i=1; i<proj.length; i++) {
          int idx=-1;
          for (int j=0; j<ord.size() && idx<0; j++)
            if (proj[i][0]<proj[ord.get(j)][0])
              idx=j;
          if (idx>=0)
            ord.add(idx,i);
          else
            ord.add(i);
        }
        order=new int[ord.size()];
        for (int i=0; i<ord.size(); i++)
          order[i]=ord.get(i);
      }
    }
  }
  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    if (matrix == null) {
      return;
    }

    int rows = matrix.length;
    int cols = matrix[0].length;
    int cellWidth = getWidth() / cols;
    int cellHeight = getHeight() / rows;

    // Calculate the minimum and maximum values in the distance matrix
    double minValue = Double.MAX_VALUE;
    double maxValue = Double.MIN_VALUE;
    for (double[] row : matrix) {
      for (double value : row) {
        if (value < minValue) {
          minValue = value;
        }
        if (value > maxValue) {
          maxValue = value;
        }
      }
    }

    // Draw the distance matrix as a grid of colored cells
    for (int i = 0; i < rows; i++) {
      for (int j = 0; j < cols; j++) {
        double value = (order==null)?matrix[i][j]:matrix[order[i]][order[j]];
        Color cellColor = getColorForValue(value, minValue, maxValue);
        g.setColor(cellColor);
        g.fillRect(j * cellWidth, i * cellHeight, cellWidth, cellHeight);
      }
    }
  }

  // Maps a value to a color between minColor and maxColor based on the range of values
  private Color getColorForValue(double value, double minValue, double maxValue) {
    double ratio = (value - minValue) / (maxValue - minValue);
    int red = (int) (minColor.getRed() + ratio * (maxColor.getRed() - minColor.getRed()));
    int green = (int) (minColor.getGreen() + ratio * (maxColor.getGreen() - minColor.getGreen()));
    int blue = (int) (minColor.getBlue() + ratio * (maxColor.getBlue() - minColor.getBlue()));
    return new Color(red, green, blue);
  }

}
