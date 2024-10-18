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
    for (int i = 1; i < tableModel.getColumnCount(); i++)
      if (tableModel.isNumericColumn(i)) {
        double minmax[]=tableModel.getColumnMinMax(i);
        if (minmax!=null)
          dataTable.getColumnModel().getColumn(i).setCellRenderer(new NumericCellRenderer(minmax[0], minmax[1]));
      }

    JScrollPane scrollPane = new JScrollPane(dataTable);
    setLayout(new BorderLayout());

    if (dataInfo!=null) {
      infoArea=new JTextArea(dataInfo);
      infoArea.setLineWrap(true);
      infoArea.setWrapStyleWord(true);
      JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollPane, infoArea);
      add(splitPane,BorderLayout.CENTER);
    }
    else
      add(scrollPane, BorderLayout.CENTER);
  }
}
