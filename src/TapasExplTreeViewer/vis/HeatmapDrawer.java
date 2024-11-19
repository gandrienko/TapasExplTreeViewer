package TapasExplTreeViewer.vis;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class HeatmapDrawer extends JPanel {

  private String xAxisLabel;
  private String yAxisLabel;
  private double[][] frequencies; // 2D array of frequencies
  private String[] yLabels; // Labels for y-axis (e.g., features or classes)
  private Color minColor = new Color(255, 255, 200); // Light yellow
  private Color maxColor = new Color(150, 0, 0); // Dark red

  /**
   * Constructor for the heatmap drawer.
   *
   * @param xAxisLabel Label for the x-axis.
   * @param yAxisLabel Label for the y-axis.
   * @param counts 2D array of counts, to be transformed to frequencies from 0 to 1.
   * @param yLabels Labels for y-axis (e.g., features or classes).
   */
  public HeatmapDrawer(int[][] counts,
                       String xAxisLabel,
                       String yAxisLabel,
                       String[] yLabels) {
    this.xAxisLabel = xAxisLabel;
    this.yAxisLabel = yAxisLabel;
    int maxCount=0;
    for (int i=0; i<counts.length; i++)
      for (int j=0; j<counts[i].length; j++)
        if (counts[i][j]>maxCount)
          maxCount=counts[i][j];
    this.frequencies = new double[counts.length][counts[0].length];
    for (int i=0; i<counts.length; i++)
      for (int j=0; j<counts[i].length; j++)
        frequencies[i][j]=(maxCount==0)?0:1.0*counts[i][j]/maxCount;
    this.yLabels = yLabels;
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2 = (Graphics2D) g;

    int panelWidth = getWidth();
    int panelHeight = getHeight();

    int xMargin = 50, yMargin=30; // Margins for labels and axes
    int heatmapWidth = panelWidth - 2 * xMargin;
    int heatmapHeight = panelHeight - 2 * yMargin;

    int cellWidth = heatmapWidth / frequencies[0].length;
    int cellHeight = heatmapHeight / frequencies.length;

    // Draw heatmap cells
    for (int row = 0; row < frequencies.length; row++) {
      for (int col = 0; col < frequencies[0].length; col++) {
        double value = frequencies[row][col];
        Color cellColor = (value==0)?Color.white:interpolateColor(minColor, maxColor, value);
        g2.setColor(cellColor);
        int x = xMargin + col * cellWidth;
        int y = yMargin + row * cellHeight;
        g2.fillRect(x, y, cellWidth, cellHeight);
      }
    }

    g2.setColor(new Color(192,192,192,128));

    // Draw grid lines
    for (int row = 0; row <= frequencies.length; row++) {
      int y = yMargin + row * cellHeight;
      g2.drawLine(xMargin, y, xMargin + heatmapWidth, y);
    }

    for (int col = 0; col <= frequencies[0].length; col++) {
      int x = xMargin + col * cellWidth;
      g2.drawLine(x, yMargin, x, yMargin + heatmapHeight);
    }

    g2.setColor(Color.black);

    // Draw y-axis labels
    for (int row = 0; row < yLabels.length; row++) {
      int x = xMargin - 10;
      int y = yMargin + row * cellHeight + cellHeight / 2 + g2.getFontMetrics().getAscent() / 2;
      g2.drawString(yLabels[row], x - g2.getFontMetrics().stringWidth(yLabels[row]), y);
    }

    // Draw axis titles
    g2.drawString(xAxisLabel, xMargin + heatmapWidth / 2 - g2.getFontMetrics().stringWidth(xAxisLabel) / 2,
        panelHeight - 10);
    g2.rotate(-Math.PI / 2);
    g2.drawString(yAxisLabel, -panelHeight / 2 - g2.getFontMetrics().stringWidth(yAxisLabel) / 2, 20);
    g2.rotate(Math.PI / 2);
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
