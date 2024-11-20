package TapasExplTreeViewer.vis;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class HeatmapDrawer extends JPanel {

  private String xAxisLabel=null;
  private String yAxisLabel=null;
  private double[][] frequencies=null; // 2D array of frequencies
  private int counts[][]=null;
  private int absMax=0;
  private String[] yLabels=null; // Labels for y-axis (e.g., features or classes)
  public static Color minColor = new Color(255, 255, 200); // Light yellow
  public static Color maxColor = new Color(150, 0, 0); // Dark red

  /**
   * Constructor for the heatmap drawer.
   *
   * @param xAxisLabel Label for the x-axis.
   * @param yAxisLabel Label for the y-axis.
   * @param counts 2D array of counts, to be transformed to frequencies from 0 to 1.
   * @param yLabels Labels for y-axis (e.g., features or classes).
   */
  public HeatmapDrawer(int[][] counts, int absMaxCount,
                       String xAxisLabel,
                       String yAxisLabel,
                       String[] yLabels) {
    this.xAxisLabel = xAxisLabel;
    this.yAxisLabel = yAxisLabel;
    this.counts=counts;
    if (absMaxCount<=0)
      for (int i=0; i<counts.length; i++)
        for (int j=0; j<counts[i].length; j++)
          if (counts[i][j]>absMaxCount)
            absMaxCount=counts[i][j];
    this.absMax=absMaxCount;

    this.frequencies = new double[counts.length][counts[0].length];
    for (int i=0; i<counts.length; i++)
      for (int j=0; j<counts[i].length; j++)
        frequencies[i][j]=(absMaxCount==0)?0:1.0*counts[i][j]/absMaxCount;
    this.yLabels = yLabels;
  }

  public Dimension getMinumumSize() {
    return new Dimension(100+frequencies[0].length*5,15+frequencies.length*10);
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2 = (Graphics2D) g;

    int panelWidth = getWidth();
    int panelHeight = getHeight();

    int leftMargin = 100, rightMargin=0, topMargin=0, bottomMargin=15; // Margins for labels and axes
    int heatmapWidth = panelWidth - leftMargin-rightMargin;
    int heatmapHeight = panelHeight - topMargin  - bottomMargin;

    int cellWidth = heatmapWidth / frequencies[0].length;
    int cellHeight = heatmapHeight / frequencies.length;

    if (cellWidth>2 && cellHeight>2) {
      // Draw heatmap cells
      for (int row = 0; row < frequencies.length; row++) {
        for (int col = 0; col < frequencies[0].length; col++) {
          double value = frequencies[row][col];
          Color cellColor = (value == 0) ? Color.white : interpolateColor(minColor, maxColor, value);
          g2.setColor(cellColor);
          int x = leftMargin + col * cellWidth;
          int y = topMargin + row * cellHeight;
          g2.fillRect(x, y, cellWidth, cellHeight);
        }
      }

      g2.setColor(new Color(192, 192, 192, 128));

      // Draw grid lines
      for (int row = 0; row <= frequencies.length; row++) {
        int y = topMargin + row * cellHeight;
        g2.drawLine(leftMargin, y, leftMargin + frequencies[0].length * cellWidth, y);
      }
      /*
      for (int col = 0; col <= frequencies[0].length; col++) {
        int x = leftMargin + col * cellWidth;
        g2.drawLine(x, topMargin, x, topMargin + heatmapHeight);
      }
      */
    }

    g2.setColor(Color.black);
    FontMetrics fm = g2.getFontMetrics();

    // Draw y-axis labels
    for (int row = 0; row < yLabels.length; row++) {
      int x = leftMargin - 10;
      int y = topMargin + row * cellHeight + cellHeight / 2 + fm.getAscent() / 2;
      g2.drawString(yLabels[row], x - fm.stringWidth(yLabels[row]), y);
    }

    // Draw axis titles
    if (xAxisLabel!=null)
      g2.drawString(xAxisLabel, leftMargin + heatmapWidth / 2 - fm.stringWidth(xAxisLabel) / 2,
          panelHeight - bottomMargin+fm.getAscent());
    if (yAxisLabel!=null) {
      g2.rotate(-Math.PI / 2);
      g2.drawString(yAxisLabel, -panelHeight / 2 - fm.stringWidth(yAxisLabel) / 2, 20);
      g2.rotate(Math.PI / 2);
    }
  }

  /**
   * Interpolates between two colors based on a value between 0 and 1.
   *
   * @param minColor The color representing the minimum value.
   * @param maxColor The color representing the maximum value.
   * @param value A value between 0 and 1.
   * @return The interpolated color.
   */
  private Color interpolateColor(Color minColor, Color maxColor, double value) {
    value = Math.max(0, Math.min(1, value)); // Clamp value to [0, 1]
    int red = (int) (minColor.getRed() + value * (maxColor.getRed() - minColor.getRed()));
    int green = (int) (minColor.getGreen() + value * (maxColor.getGreen() - minColor.getGreen()));
    int blue = (int) (minColor.getBlue() + value * (maxColor.getBlue() - minColor.getBlue()));
    return new Color(red, green, blue);
  }
}
