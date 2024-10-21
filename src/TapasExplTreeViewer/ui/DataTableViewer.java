package TapasExplTreeViewer.ui;

import TapasExplTreeViewer.rules.DataSet;
import TapasExplTreeViewer.util.CSVDataLoader;

import javax.swing.*;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class DataTableViewer extends JPanel {
  private DataSet data=null;
  private JTable dataTable=null;
  private DataTableModel tableModel=null;
  private JTextArea infoArea=null;
  private JPopupMenu popup=null;
  private int shownRow=-1;
  
  public DataTableViewer (DataSet dataSet, String featureNames[], String dataInfo) {
    if (dataSet==null || dataSet.records==null || dataSet.records.isEmpty())
      return;
    data=dataSet;
    tableModel=new DataTableModel(dataSet,featureNames);
    dataTable = new JTable(tableModel);
    for (int i = 1; i < tableModel.getColumnCount(); i++)
      if (tableModel.isNumericColumn(i)) {
        double minmax[]=tableModel.getColumnMinMax(i);
        if (minmax!=null)
          dataTable.getColumnModel().getColumn(i).setCellRenderer(new NumericCellRenderer(minmax[0], minmax[1]));
      }
  
    TableRowSorter<DataTableModel> sorter = new TableRowSorter<>(tableModel);
    dataTable.setRowSorter(sorter);
  
    JPopupMenu menu=new JPopupMenu();
    JMenuItem mit=new JMenuItem("Export the data to a CSV file");
    menu.add(mit);
    mit.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        exportDataToCSVFile();
      }
    });

    // Add mouse listener to show popup when mouse points to a row
    dataTable.addMouseMotionListener(new MouseAdapter() {
      @Override
      public void mouseMoved(MouseEvent e) {
        int row = dataTable.rowAtPoint(e.getPoint());
        if (row >= 0) {
          dataTable.setRowSelectionInterval(row, row); // Select the row under the mouse cursor
          showHtmlPopup(e, dataTable.convertRowIndexToModel(row));
        }
        else
          hidePopup();
      }
    });
    dataTable.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseExited(MouseEvent e) {
        hidePopup();
        super.mouseExited(e);
      }
      @Override
      public void mousePressed(MouseEvent e) {
        hidePopup();
        if (e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) {
          menu.show(e.getComponent(), e.getX(), e.getY());
        }
      }
    });
    
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
  
  private void showHtmlPopup(MouseEvent e, int row) {
    if (row<0)
      return;
    if (popup!=null) {
      if (shownRow==row) {
        // Show the popup at the mouse cursor position
        popup.show(e.getComponent(), e.getX(), e.getY());
        return;
      }
      popup.setVisible(false);
      popup.removeAll();
    }
    else {
      // Create the popup menu
      popup=new JPopupMenu();
      popup.setBackground(new Color(255,255,202));
    }
    
    // Create a JLabel to display the HTML content in the popup
    JLabel label = new JLabel(getPopupContent(row));
    popup.add(label);
    
    // Show the popup at the mouse cursor position
    popup.show(e.getComponent(), e.getX(), e.getY());
  }
  
  public String getPopupContent(int row) {
    if (row<0)
      return null;
    // Get the record ID and row number
    String recordId = dataTable.getValueAt(row, 0).toString();
    String rowNumber = String.valueOf(row + 1);
  
    // Build the HTML content
    StringBuilder htmlContent = new StringBuilder("<html><body>");
    htmlContent.append("<div style='text-align: center;'>")
        .append("<b>Record ID:</b> ").append(recordId)
        .append("<br><b>Row:</b> ").append(rowNumber)
        .append("</div>");
    htmlContent.append("<br><table border='1' cellspacing='0' cellpadding='2'>");
    htmlContent.append("<tr><th>Column</th><th>Value</th></tr>");
  
    for (int col = 0; col < dataTable.getColumnCount(); col++) {
      String columnName = dataTable.getColumnName(col);
      if (columnName.equalsIgnoreCase("Record id"))
        continue;
      String value = dataTable.getValueAt(row, col) != null ? dataTable.getValueAt(row, col).toString() : "";
      htmlContent.append("<tr><td>").append(columnName).append("</td><td>").append(value).append("</td></tr>");
    }
  
    htmlContent.append("</table></body></html>");
    return htmlContent.toString();
  }
  
  protected void hidePopup() {
    if (popup!=null) {
      popup.setVisible(false);
      popup=null;
      shownRow=-1;
    }
  }
  
  public void exportDataToCSVFile() {
    if (data==null || data.records==null || data.records.isEmpty()) {
      JOptionPane.showMessageDialog(FocusManager.getCurrentManager().getActiveWindow(),
          "No data have been loaded in the system!","No data",
          JOptionPane.INFORMATION_MESSAGE);
      return;
    }
    String pathName=CSVDataLoader.selectFilePathThroughDialog(false);
    if (pathName==null)
      return;
    if (data.exportToCSV(pathName))
      JOptionPane.showMessageDialog(FocusManager.getCurrentManager().getActiveWindow(),
          "Successfully exported the data to file "+pathName,"Data exported",
          JOptionPane.INFORMATION_MESSAGE);
    else
      JOptionPane.showMessageDialog(FocusManager.getCurrentManager().getActiveWindow(),
          "Failed to export the data!","Export failed",
          JOptionPane.ERROR_MESSAGE);
  }
  
  public void showRulesApplicableToData(){
    //
  }
}
