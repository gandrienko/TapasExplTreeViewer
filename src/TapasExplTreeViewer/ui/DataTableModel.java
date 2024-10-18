package TapasExplTreeViewer.ui;

import TapasExplTreeViewer.rules.DataElement;
import TapasExplTreeViewer.rules.DataRecord;
import TapasExplTreeViewer.rules.DataSet;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;

public class DataTableModel extends AbstractTableModel {
  public DataSet dataSet=null;
  public String featureNames[]=null;
  protected String[] columnNames=null;
  public ArrayList<DataRecord> records=null;

  public DataTableModel(DataSet dataSet, String featureNames[]) {
    this.dataSet=dataSet;
    if (dataSet==null || dataSet.records==null || dataSet.records.isEmpty())
      return;
    this.records = dataSet.records;
    this.featureNames = (featureNames==null)?dataSet.fieldNames:featureNames;

    // Define column names: Record ID, True Class, Predicted Class/Value, and Features
    columnNames = new String[3 + featureNames.length];
    columnNames[0] = "Record ID";
    columnNames[1] = "True Class/Value";
    columnNames[2] = "Predicted Class/Value";
    System.arraycopy(featureNames, 0, columnNames, 3, featureNames.length);
  }

  @Override
  public int getRowCount() {
    return records.size();
  }

  @Override
  public int getColumnCount() {
    return columnNames.length;
  }

  @Override
  public String getColumnName(int columnIndex) {
    return columnNames[columnIndex];
  }

  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    DataRecord record = records.get(rowIndex);

    if (columnIndex == 0) {
      return record.id;
    } else if (columnIndex == 1) {
      return (record.trueClassIdx >=0) ? record.trueClassIdx : record.trueValue;
    } else if (columnIndex == 2) {
      switch (record.getPredictionType()) {
        case DataRecord.CLASS_TARGET:
          return record.predictedClassIdx;
        case DataRecord.VALUE_TARGET:
          return record.predictedValue;
        case DataRecord.RANGE_TARGET:
          return "[" + record.predictedValueRange[0] + " - " + record.predictedValueRange[1] + "]";
        default:
          return "";
      }
    } else {
      // Handle feature columns
      int featureIndex = columnIndex - 3;
      String featureName = featureNames[featureIndex];
      DataElement dataElement = record.getDataElement(featureName);

      if (dataElement != null) {
        switch (dataElement.dataType) {
          case DataElement.CATEGORY:
            return dataElement.stringValue;
          case DataElement.INTEGER:
            return dataElement.intValue > Integer.MIN_VALUE ? dataElement.intValue : "";
          case DataElement.REAL:
            return !Double.isNaN(dataElement.doubleValue) ? dataElement.doubleValue : "";
          default:
            return "";
        }
      } else {
        return ""; // If no value, return empty string
      }
    }
  }

  public boolean isNumericColumn(int columnIndex) {
    if (columnIndex==0)
      return false;
    if (columnIndex==1)
      return true;
    if (columnIndex==2)
      return dataSet.determinePredictionType()!=DataRecord.RANGE_TARGET;
    int featureIndex = columnIndex - 3;
    byte type=dataSet.determineFeatureType(featureNames[featureIndex]);
    return type==DataElement.INTEGER || type==DataElement.REAL;
  }

  public double[] getColumnMinMax(int columnIndex) {
    if (columnIndex==0)
      return null;
    if (columnIndex==1)
      return dataSet.getTargetMinMax();
    if (columnIndex==2)
      return dataSet.getPredictionMinMax();
    int featureIndex = columnIndex - 3;
    return dataSet.findMinMax(featureNames[featureIndex]);
  }

  @Override
  public boolean isCellEditable(int rowIndex, int columnIndex) {
    return false; // All cells are non-editable for this viewer
  }
}
