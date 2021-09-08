package TapasExplTreeViewer.ui;

import TapasDataReader.CommonExplanation;
import TapasExplTreeViewer.clustering.ClusterContent;
import TapasUtilities.RenderLabelBarChart;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Vector;

public class ClustersTable extends JPanel {

  protected ClusterContent clusters[]=null;
  protected double distanceMatrix[][];
  public JScrollPane scrollPane=null;

  public ClustersTable (ClusterContent clusters[], double distanceMatrix[][], ArrayList<CommonExplanation> exList, JLabel_Rule ruleRenderer, Hashtable<String,float[]> attrMinMax) {
    super();
    this.clusters=clusters;
    this.distanceMatrix=distanceMatrix;
    JTable table=new JTable(new ClustersTableModel(clusters,distanceMatrix,exList)){
      public String getToolTipText(MouseEvent e) {
        java.awt.Point p = e.getPoint();
        int rowIndex = rowAtPoint(p);
        if (rowIndex>=0) {
          int realRowIndex = convertRowIndexToModel(rowIndex);
          //highlighter.highlight(new Integer(realRowIndex));
          int colIndex=columnAtPoint(p);
          String s="";
/*
          if (colIndex>=0) {
            int realColIndex=convertColumnIndexToModel(colIndex);
            s=eTblModel.getColumnName(realColIndex);
          }
*/
          CommonExplanation ce=(CommonExplanation)exList.get(clusters[realRowIndex].medoidIdx);
          Vector<CommonExplanation> vce=null;
          //if (table!=null && table.getSelectedRow()>=0) {
            vce=new Vector<>();
            for (int i=0; i<clusters[realRowIndex].member.length; i++)
              if (clusters[realRowIndex].member[i]) {
                //int idx=clusters[realRowIndex].objIds[i]; // objIds==null
                CommonExplanation ce1=(CommonExplanation)exList.get(i);
                vce.add(ce1);
              }
            //vce.add((CommonExplanation)exList.get(clusters[table.getSelectedRow()].medoidIdx));
          //}
/*
          ArrayList selected=selector.getSelected();
          if (selected!=null && selected.size()>0) {
            vce=new Vector<>(selected.size());
            for (int i = 0; i < selected.size(); i++)
              vce.add(exList.get((Integer)selected.get(i)));
          }
*/
          try {
            BufferedImage bi = ShowSingleRule.getImageForRule(300,100, ce, vce, ruleRenderer.attrs, ruleRenderer.minmax);
            File outputfile = new File("img.png");
            ImageIO.write(bi, "png", outputfile);
            //System.out.println("img"+ce.numId+".png");
          } catch (IOException ex) { System.out.println("* error while writing image to file: "+ex.toString()); }
          String out=ce.toHTML(attrMinMax,s,"img.png");
          //System.out.println(out);
          return out;
        }
        //highlighter.clearHighlighting();
        return "";
      }

    };
    table.setFillsViewportHeight(true);
    table.setAutoCreateRowSorter(true);
    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    table.setRowSelectionAllowed(true);
    table.setColumnSelectionAllowed(false);
    table.setRowHeight((int)(1.5*table.getRowHeight()));
    TableColumnModel tableColumnModel=table.getColumnModel();
    tableColumnModel.getColumn(0).setCellRenderer(new RenderLabelBarChart(0,clusters.length-1));
    int maxSize=clusters[0].getMemberCount();
    float maxD=0;
    for (int i=1; i<clusters.length; i++) {
      maxSize = Math.max(maxSize, clusters[i].getMemberCount());
      maxD = Math.max(maxD,(float)clusters[i].getDiameter(distanceMatrix));
    }
    tableColumnModel.getColumn(1).setCellRenderer(new RenderLabelBarChart(0,maxSize));
    for (int i=2; i<=3; i++)
      tableColumnModel.getColumn(i).setCellRenderer(new RenderLabelBarChart(0,maxD));
    tableColumnModel.getColumn(4).setCellRenderer(ruleRenderer);
    for (int i=0; i<4; i++)
      tableColumnModel.getColumn(i).setPreferredWidth((i<2)?30:50);

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

  private String columnNames[] = {"Cluster","Size","m-Radius","Diameter","Rule","Action","Q"};
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