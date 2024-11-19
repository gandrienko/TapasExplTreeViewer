package TapasExplTreeViewer.vis;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class MultiHeatmapPanel extends JPanel {
  private List<HeatmapDrawer> heatmaps = new ArrayList<HeatmapDrawer>();
  private List<String> titles = new ArrayList<String>();
  private int padding = 10; // Space between heatmaps
  private int minHeatmapWidth = 200; // Minimum width for a heatmap

  public MultiHeatmapPanel() {
    setLayout(null); // Use absolute positioning to allow custom layout
  }

  /**
   * Adds a heatmap to the panel.
   *
   * @param heatmap The HeatmapDrawer instance.
   * @param title   The title to display above the heatmap.
   */
  public void addHeatmap(HeatmapDrawer heatmap, String title) {
    heatmaps.add(heatmap);
    titles.add(title);
    add(heatmap);
    repaint();
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);

    if (heatmaps.isEmpty()) return;

    int panelWidth = getWidth();
    int panelHeight = getHeight();

    // Calculate the number of columns
    int numCols = Math.max(1, panelWidth / (minHeatmapWidth + padding));
    int numRows = (int) Math.ceil((double) heatmaps.size() / numCols);

    int cellWidth = (panelWidth - (numCols + 1) * padding) / numCols;
    int cellHeight = (panelHeight - (numRows + 1) * padding) / numRows;

    // Position heatmaps
    for (int i = 0; i < heatmaps.size(); i++) {
      int row = i / numCols;
      int col = i % numCols;

      int x = padding + col * (cellWidth + padding);
      int y = padding + row * (cellHeight + padding);

      HeatmapDrawer heatmap = heatmaps.get(i);
      heatmap.setBounds(x, y + 20, cellWidth, cellHeight - 20); // Leave space for title

      // Draw the title above the heatmap
      String title = titles.get(i);
      g.setColor(Color.BLACK);
      g.setFont(new Font("SansSerif", Font.BOLD, 12));
      FontMetrics fm = g.getFontMetrics();
      int titleWidth = fm.stringWidth(title);
      g.drawString(title, x + (cellWidth - titleWidth) / 2, y + 15);
    }
  }

  @Override
  public void doLayout() {
    // Trigger layout when panel is resized
    revalidate();
    repaint();
  }
}
