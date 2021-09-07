package TapasExplTreeViewer.ui;

import TapasExplTreeViewer.clustering.ClusterContent;
import TapasUtilities.RenderLabelBarChart;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;

public class ClustersTable extends JPanel {

  protected ClusterContent clusters[]=null;
  public JScrollPane scrollPane=null;

  public ClustersTable (ClusterContent clusters[]) {
    super();
    this.clusters=clusters;
    JTable table=new JTable(new ClustersTableModel(clusters));
    table.setFillsViewportHeight(true);
    table.setAutoCreateRowSorter(true);
    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    table.setRowSelectionAllowed(true);
    table.setColumnSelectionAllowed(false);
    table.getColumnModel().getColumn(0).setCellRenderer(new RenderLabelBarChart(0,clusters.length-1));
    int maxSize=clusters[0].getMemberCount();
    for (int i=1; i<clusters.length; i++)
      maxSize=Math.max(maxSize,clusters[i].getMemberCount());
    table.getColumnModel().getColumn(1).setCellRenderer(new RenderLabelBarChart(0,maxSize));
    scrollPane = new JScrollPane(table);
    scrollPane.setMinimumSize(new Dimension(100,200));
    scrollPane.setOpaque(true);
  }
}

class ClustersTableModel extends AbstractTableModel {

  protected ClusterContent clusters[]=null;

  public ClustersTableModel (ClusterContent clusters[]) {
    this.clusters=clusters;
  }

  private String columnNames[] = {"Cluster","Size","m-Radius","Diameter","Rule","Outcomes"};
  public int getColumnCount() {
    return columnNames.length;
  }
  public String getColumnName(int col) {
    return columnNames[col];
  }
  public int getRowCount() {
    return clusters.length;
  }
  public Class getColumnClass(int c) {
    return (getValueAt(0, c)==null) ? null: getValueAt(0, c).getClass();
  }
  public Object getValueAt (int row, int col) {
    switch (col) {
      case 0:
        return row;
      case 1:
        return clusters[row].getMemberCount();
    }
    return 0;
  }
}