package TapasExplTreeViewer.ui;

import TapasExplTreeViewer.rules.DataSet;

import javax.swing.*;
import java.awt.*;

public class DataTableViewer extends JPanel {
  private JTable dataTable=null;
  private DataTableModel tableModel=null;
  private JTextArea infoArea=null;

  public DataTableViewer (DataSet dataSet, String featureNames[], String dataInfo) {
    if (dataSet==null || dataSet.records==null || dataSet.records.isEmpty())
      return;
    tableModel=new DataTableModel(dataSet,featureNames);
    dataTable = new JTable(tableModel);

    JScrollPane scrollPane = new JScrollPane(dataTable);
    setLayout(new BorderLayout());
    add(scrollPane, BorderLayout.CENTER);

    if (dataInfo!=null) {
      infoArea=new JTextArea(dataInfo);
      add(infoArea,BorderLayout.SOUTH);
    }
  }
}
