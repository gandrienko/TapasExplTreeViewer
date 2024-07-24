package TapasExplTreeViewer.ui;

import TapasDataReader.CommonExplanation;
import TapasDataReader.ExplanationItem;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class TMrulesDefineSettings {

  public TMrulesDefineSettings (ArrayList<CommonExplanation> exList, Hashtable<String,float[]> attrMinMax,
                                Map<String, TreeSet<Float>> uniqueSortedValues,
                                Map<String, List<Float>> allValues,
                                String dataFolder)
  {
    SwingUtilities.invokeLater(() -> {
      AttributeRangeDialog dialog = new AttributeRangeDialog(exList, attrMinMax, uniqueSortedValues, allValues, dataFolder);
      dialog.setVisible(true);
    });
  }

}

class AttributeRangeDialog extends JFrame {

  private JLabel fileNameLabel;

  ArrayList<CommonExplanation> exList;
  Map<String, float[]> attrMinMax;
  Map<String, TreeSet<Float>> uniqueSortedValues;

  public AttributeRangeDialog(
          ArrayList<CommonExplanation> exList,
          Map<String, float[]> attrMinMax,
          Map<String, TreeSet<Float>> uniqueSortedValues,
          Map<String, List<Float>> allValues,
          String dataFolder) {

    this.exList=exList;
    this.attrMinMax=attrMinMax;
    this.uniqueSortedValues=uniqueSortedValues;

    setTitle("Attribute Range Definition");
    setSize(800, 600);
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    AttributeTableModel model = new AttributeTableModel(attrMinMax, uniqueSortedValues, allValues);
    JTable table = new JTable(model);

    // Set up JComboBox for mode selection
    JComboBox<String> modeComboBox = new JComboBox<>(new String[]{"Distinct", "Quantiles"});
    table.getColumnModel().getColumn(5).setCellEditor(new DefaultCellEditor(modeComboBox));

    // Set up JTextField for quantile number input
    JTextField quantileField = new JTextField();
    table.getColumnModel().getColumn(6).setCellEditor(new DefaultCellEditor(quantileField));

    // Disable editing quantile numbers when mode is distinct
    table.getColumnModel().getColumn(6).setCellRenderer((table1, value, isSelected, hasFocus, row, column) -> {
      JTextField textField = new JTextField(value != null ? value.toString() : "");
      if (model.modeMap.get(model.attributes.get(row))) {
        textField.setEditable(false);
        textField.setBackground(Color.LIGHT_GRAY);
      } else {
        textField.setEditable(true);
        textField.setBackground(Color.WHITE);
      }
      return textField;
    });

    // Adjust column widths proportionally initially and on resize
    table.addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent e) {
        adjustColumnWidths(table);
      }
    });

    // Set up JScrollPane
    JScrollPane scrollPane = new JScrollPane(table);
    add(scrollPane, BorderLayout.CENTER);

    // Adjust column widths after the table is shown
    addComponentListener(new ComponentAdapter() {
      @Override
      public void componentShown(ComponentEvent e) {
        adjustColumnWidths(table);
      }
    });

    // Create and set up the panel for the button and file name label
    JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    JButton goButton = new JButton("Go");
    fileNameLabel = new JLabel("");

    // Add action listener to the button
    goButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        String fname=new File(dataFolder,generateFileName()).getPath();
        fileNameLabel.setText("File: " + fname);
        saveToFile(fname, model.modeMap, model.quantileMap);
      }
    });

    bottomPanel.add(goButton);
    bottomPanel.add(fileNameLabel);

    // Add the panel to the frame
    add(bottomPanel, BorderLayout.SOUTH);
  }

/*
  private void saveToFile (ArrayList<CommonExplanation> exList, String fname) {
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(fname))) {
      // Write header
      writer.write("ruleNumber,outcome,conditions");
      writer.newLine();

      // Write each rule
      for (int i = 0; i < exList.size(); i++) {
        CommonExplanation rule = exList.get(i);
        StringBuilder sb = new StringBuilder();
        sb.append(i + 1).append(","); // ruleNumber (starting from 1)
        sb.append(rule.action).append(","); // outcome
        for (ExplanationItem item : rule.eItems) {
          sb.append("[").append(item.interval[0]).append(" - ").append(item.interval[1]).append("] ");
        }
        // Remove the last space
        sb.setLength(sb.length() - 1);
        writer.write(sb.toString());
        writer.newLine();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
*/

  private void saveToFile(String fname, Map<String,Boolean> modeMap, Map<String,Integer> quantileMap) {
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(fname))) {
      // Write header
      writer.write("ruleNumber,outcome,conditions");
      writer.newLine();

      // Prepare interval mapping for each attribute
      Map<String, List<String>> intervalMap = new HashMap<>();
      for (String attribute : attrMinMax.keySet()) {
        List<String> intervals = new ArrayList<>();
        if (modeMap.get(attribute)) {
          // Distinct mode
          List<Float> values = new ArrayList<>(uniqueSortedValues.get(attribute));
          for (int i = 0; i < values.size(); i++) {
            intervals.add(attribute + "_" + i);
          }
        } else {
          // Quantiles mode
          int n = quantileMap.get(attribute);
          float[] minMax = attrMinMax.get(attribute);
          float min = minMax[0];
          float max = minMax[1];
          for (int i = 0; i < n; i++) {
            intervals.add(attribute + "_" + i);
          }
        }
        intervalMap.put(attribute, intervals);
      }

      // Write each rule
      for (int i = 0; i < exList.size(); i++) {
        CommonExplanation rule = exList.get(i);
        StringBuilder sb = new StringBuilder();
        sb.append(i + 1).append(","); // ruleNumber (starting from 1)
        sb.append(rule.action).append(","); // outcome
        for (ExplanationItem item : rule.eItems) {
          String attribute = item.attr;
          List<String> intervals = intervalMap.get(attribute);
          double start = item.interval[0];
          double end = item.interval[1];

          for (int j = 0; j < intervals.size(); j++) {
            double intervalStart = j == 0 ? -Double.MAX_VALUE : uniqueSortedValues.get(attribute).toArray(new Float[0])[j - 1];
            double intervalEnd = j == intervals.size() - 1 ? Double.MAX_VALUE : uniqueSortedValues.get(attribute).toArray(new Float[0])[j];

            if (start < intervalEnd && end > intervalStart) {
              sb.append(intervals.get(j)).append(" ");
            }
          }
        }
        // Remove the last space
        sb.setLength(sb.length() - 1);
        writer.write(sb.toString());
        writer.newLine();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  private void adjustColumnWidths(JTable table) {
    int tableWidth = table.getWidth();
    int intervalColumnWidth = (int) (tableWidth * 0.5);
    int otherColumnWidth = (tableWidth - intervalColumnWidth) / (table.getColumnCount() - 1);

    for (int i = 0; i < table.getColumnCount(); i++) {
      if (i == 7) {
        table.getColumnModel().getColumn(i).setPreferredWidth(intervalColumnWidth);
      } else {
        table.getColumnModel().getColumn(i).setPreferredWidth(otherColumnWidth);
      }
    }
  }

  private String generateFileName() {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");
    return "TM_" + sdf.format(new Date()) + ".csv";
  }

}

class AttributeTableModel extends AbstractTableModel {
  private final String[] columnNames = {
          "Attribute", "Min", "Max", "Count of Distinct Values",
          "Count of All Values", "Mode", "Number of Quantiles", "Class Intervals"
  };
  public final List<String> attributes;
  private final Map<String, float[]> attrMinMax;
  private final Map<String, TreeSet<Float>> uniqueSortedValues;
  private final Map<String, List<Float>> allValues;
  public final Map<String, Boolean> modeMap;
  public final Map<String, Integer> quantileMap;
  private final Map<String, List<Float>> intervalsMap;

  public AttributeTableModel (Map<String, float[]> attrMinMax,
                              Map<String, TreeSet<Float>> uniqueSortedValues,
                              Map<String, List<Float>> allValues)
  {
    this.attributes = new ArrayList<>(attrMinMax.keySet());
    this.attrMinMax = attrMinMax;
    this.uniqueSortedValues = uniqueSortedValues;
    this.allValues = allValues;
    this.modeMap = new HashMap<>();
    this.quantileMap = new HashMap<>();
    this.intervalsMap = new HashMap<>();

    for (String attribute : attributes) {
      int uniqueCount = uniqueSortedValues.get(attribute).size();
      if (uniqueCount <= 5) {
        modeMap.put(attribute, true); // Distinct mode
      } else {
        modeMap.put(attribute, false); // Quantiles mode
        quantileMap.put(attribute, 4); // Default quantiles
      }
      updateClassIntervals(attribute);
    }
  }

  @Override
  public int getRowCount() {
    return attributes.size();
  }

  @Override
  public int getColumnCount() {
    return columnNames.length;
  }

  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    String attribute = attributes.get(rowIndex);
    switch (columnIndex) {
      case 0: return attribute;
      case 1: return attrMinMax.get(attribute)[0];
      case 2: return attrMinMax.get(attribute)[1];
      case 3: return uniqueSortedValues.get(attribute).size();
      case 4: return allValues.get(attribute).size();
      case 5: return modeMap.get(attribute) ? "Distinct" : "Quantiles";
      case 6: return quantileMap.get(attribute);
      case 7: return intervalsMap.get(attribute).toString();
      default: return null;
    }
  }

  @Override
  public boolean isCellEditable(int rowIndex, int columnIndex) {
    String attribute = attributes.get(rowIndex);
    if (columnIndex == 5) {
      return true;
    }
    if (columnIndex == 6) {
      return !modeMap.get(attribute);
    }
    return false;
  }

  @Override
  public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
    String attribute = attributes.get(rowIndex);
    if (columnIndex == 5) {
      modeMap.put(attribute, "Distinct".equals(aValue));
      if (modeMap.get(attribute)) {
        quantileMap.remove(attribute);
      }
      updateClassIntervals(attribute);
      fireTableCellUpdated(rowIndex, 7);
    } else if (columnIndex == 6) {
      try {
        int quantiles = Integer.parseInt(aValue.toString());
        quantileMap.put(attribute, quantiles);
        updateClassIntervals(attribute);
        fireTableCellUpdated(rowIndex, 7);
      } catch (NumberFormatException e) {
        // Handle invalid number input
      }
    }
    fireTableCellUpdated(rowIndex, columnIndex);
  }

  private void updateClassIntervals(String attribute) {
    if (modeMap.get(attribute)) {
      intervalsMap.put(attribute, new ArrayList<>(uniqueSortedValues.get(attribute)));
    } else {
      // Ensure quantileMap has an entry for the attribute
      if (!quantileMap.containsKey(attribute)) {
        quantileMap.put(attribute, 4); // Default quantiles
      }
      // Calculate quantiles
      List<Float> values = new ArrayList<>(new HashSet<>(allValues.get(attribute))); // Remove duplicates
      Collections.sort(values);
      int n = quantileMap.get(attribute);
      List<Float> quantiles = new ArrayList<>();
      for (int i = 1; i < n; i++) {
        int index = (int) Math.ceil(i * values.size() / (double) n) - 1;
        // Ensure index is within bounds
        index = Math.min(index, values.size() - 1);
        float breakPoint = (values.get(index) + values.get(Math.min(index + 1, values.size() - 1))) / 2;
        if (!quantiles.contains(breakPoint)) {
          quantiles.add(breakPoint);
        }
      }
      quantileMap.put(attribute, quantiles.size() + 1); // Update the number of quantiles
      intervalsMap.put(attribute, quantiles);
    }
  }

  @Override
  public String getColumnName(int column) {
    return columnNames[column];
  }

  @Override
  public Class<?> getColumnClass(int columnIndex) {
    switch (columnIndex) {
      case 1: case 2: return Float.class;
      case 3: case 4: case 6: return Integer.class;
      case 5: return String.class;
      default: return Object.class;
    }
  }
}
