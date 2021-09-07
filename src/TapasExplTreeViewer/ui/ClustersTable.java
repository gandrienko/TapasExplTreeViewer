package TapasExplTreeViewer.ui;

import TapasDataReader.CommonExplanation;
import TapasExplTreeViewer.clustering.ClusterContent;
import TapasUtilities.RenderLabelBarChart;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Vector;

public class ClustersTable extends JPanel {

  protected ClusterContent clusters[]=null;
  protected double distanceMatrix[][];
  public JScrollPane scrollPane=null;

  public ClustersTable (ClusterContent clusters[], double distanceMatrix[][], ArrayList<CommonExplanation> exList, JLabel_Rule ruleRenderer) {
    super();
    this.clusters=clusters;
    this.distanceMatrix=distanceMatrix;
    JTable table=new JTable(new ClustersTableModel(clusters,distanceMatrix,exList));
    table.setFillsViewportHeight(true);
    table.setAutoCreateRowSorter(true);
    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    table.setRowSelectionAllowed(true);
    table.setColumnSelectionAllowed(false);
    table.getColumnModel().getColumn(0).setCellRenderer(new RenderLabelBarChart(0,clusters.length-1));
    int maxSize=clusters[0].getMemberCount();
    float maxD=0;
    for (int i=1; i<clusters.length; i++) {
      maxSize = Math.max(maxSize, clusters[i].getMemberCount());
      maxD = Math.max(maxD,(float)clusters[i].getDiameter(distanceMatrix));
    }
    table.getColumnModel().getColumn(1).setCellRenderer(new RenderLabelBarChart(0,maxSize));
    for (int i=2; i<=3; i++)
      table.getColumnModel().getColumn(i).setCellRenderer(new RenderLabelBarChart(0,maxD));

    table.getColumnModel().getColumn(4).setCellRenderer(ruleRenderer);

    scrollPane = new JScrollPane(table);
    scrollPane.setMinimumSize(new Dimension(100,200));
    scrollPane.setOpaque(true);
  }
}

class ClustersTableModel extends AbstractTableModel {

  protected ClusterContent clusters[]=null;
  protected double distanceMatrix[][]=null;
  protected ArrayList<CommonExplanation> exList=null;

  public ClustersTableModel (ClusterContent clusters[], double distanceMatrix[][], ArrayList<CommonExplanation> exList) {
    this.clusters=clusters;
    this.distanceMatrix=distanceMatrix;
    this.exList=exList;
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
      case 2:
        return clusters[row].getMRadius(distanceMatrix);
      case 3:
        return clusters[row].getDiameter(distanceMatrix);
      case 4:
        return exList.get(clusters[row].medoidIdx);
    }
    return 0;
  }
}