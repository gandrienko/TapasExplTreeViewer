package TapasExplTreeViewer.ui;

import TapasDataReader.CommonExplanation;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;

public class TMrulesDefineSettings {

  public TMrulesDefineSettings (ArrayList<CommonExplanation> exList, Hashtable<String,float[]> attrMinMax,
                                Map<String, TreeSet<Float>> uniqueSortedValues,
                                Map<String, List<Float>> allValues)
  {
    SwingUtilities.invokeLater(() -> {
      AttributeRangeDialog dialog = new AttributeRangeDialog(attrMinMax, uniqueSortedValues, allValues);
      dialog.setVisible(true);
    });
  }

}

class AttributeRangeDialog extends JFrame {

  public AttributeRangeDialog(
          Map<String, float[]> attrMinMax,
          Map<String, TreeSet<Float>> uniqueSortedValues,
          Map<String, List<Float>> allValues) {

    setTitle("Attribute Range Definition");
    setSize(800, 600);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    AttributeTableModel model = new AttributeTableModel(attrMinMax, uniqueSortedValues, allValues);
    JTable table = new JTable(model);

    // Set up JComboBox for mode selection
    JComboBox<String> modeComboBox = new JComboBox<>(new String[]{"Distinct", "Quantiles"});
    table.getColumnModel().getColumn(5).setCellEditor(new DefaultCellEditor(modeComboBox));

    // Set up JTextField for quantile number input
    JTextField quantileField = new JTextField();
    table.getColumnModel().getColumn(6).setCellEditor(new DefaultCellEditor(quantileField));

    // Set up JScrollPane
    JScrollPane scrollPane = new JScrollPane(table);
    add(scrollPane, BorderLayout.CENTER);
  }

  /*
  public static void main(String[] args) {
    // Example data
    Map<String, float[]> attrMinMax = new HashMap<>();
    attrMinMax.put("Attr1", new float[]{1.0f, 10.0f});
    attrMinMax.put("Attr2", new float[]{0.5f, 5.5f});

    Map<String, TreeSet<Float>> uniqueSortedValues = new HashMap<>();
    uniqueSortedValues.put("Attr1", new TreeSet<>(Arrays.asList(1.0f, 2.0f, 3.0f, 10.0f)));
    uniqueSortedValues.put("Attr2", new TreeSet<>(Arrays.asList(0.5f, 1.0f, 3.0f, 5.5f)));

    Map<String, List<Float>> allValues = new HashMap<>();
    allValues.put("Attr1", Arrays.asList(1.0f, 2.0f, 3.0f, 10.0f, 2.0f, 3.0f));
    allValues.put("Attr2", Arrays.asList(0.5f, 1.0f, 3.0f, 5.5f, 1.0f, 3.0f));

    SwingUtilities.invokeLater(() -> {
      AttributeRangeDialog dialog = new AttributeRangeDialog(attrMinMax, uniqueSortedValues, allValues);
      dialog.setVisible(true);
    });
  }
  */
}

class AttributeTableModel extends AbstractTableModel {
  private final String[] columnNames = {
          "Attribute", "Min", "Max", "Count of Distinct Values",
          "Count of All Values", "Mode", "Number of Quantiles", "Class Intervals"
  };
  private final List<String> attributes;
  private final Map<String, float[]> attrMinMax;
  private final Map<String, TreeSet<Float>> uniqueSortedValues;
  private final Map<String, List<Float>> allValues;
  private final Map<String, Boolean> modeMap;
  private final Map<String, Integer> quantileMap;
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
    return columnIndex == 5 || columnIndex == 6;
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
