package TapasExplTreeViewer.ui;

import TapasExplTreeViewer.rules.DataElement;
import TapasExplTreeViewer.rules.DataRecord;
import TapasExplTreeViewer.rules.DataSet;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;

public class DataTableModel extends AbstractTableModel {
  public static int nStandardColumns=4;
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
    columnNames = new String[nStandardColumns + featureNames.length];
    columnNames[0] = "Record ID";
    columnNames[1] = "Original Class/Value";
    columnNames[2] = "Predicted Class/Value";
    columnNames[3] = "Match?";
    System.arraycopy(featureNames, 0, columnNames, nStandardColumns, featureNames.length);
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
      return (record.origClassIdx >=0) ? record.origClassIdx : record.origValue;
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
    } else if (columnIndex == 3) {
      return (checkPrediction(record))?"yes":"no";
    } else {
      // Handle feature columns
      int featureIndex = columnIndex - nStandardColumns;
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
  
  public boolean checkPrediction(DataRecord r) {
    if (r.origClassIdx>=0)
      return r.origClassIdx==r.predictedClassIdx;
    if (!Double.isNaN(r.origValue)) {
      if (!Double.isNaN(r.predictedValue))
        return r.predictedValue==r.origValue;
      if (r.predictedValueRange!=null)
        return r.origValue>=r.predictedValueRange[0] &&
            r.origValue<=r.predictedValueRange[1];
      return false;
    }
    if (r.origValueRange!=null) {
      if (!Double.isNaN(r.predictedValue))
        return r.predictedValue>=r.origValueRange[0] &&
            r.predictedValue<=r.origValueRange[1];
      if (r.predictedValueRange!=null)
        return r.predictedValue==r.origValueRange[0] &&
            r.predictedValue==r.origValueRange[1];
    }
    return false;
  }

  public boolean isNumericColumn(int columnIndex) {
    if (columnIndex==0 || columnIndex==3)
      return false;
    if (columnIndex==1)
      return true;
    if (columnIndex==2)
      return dataSet.determinePredictionType()!=DataRecord.RANGE_TARGET;
    int featureIndex = columnIndex - nStandardColumns;
    byte type=dataSet.determineFeatureType(featureNames[featureIndex]);
    return type==DataElement.INTEGER || type==DataElement.REAL;
  }

  public double[] getColumnMinMax(int columnIndex) {
    if (columnIndex==0 || columnIndex==3)
      return null;
    if (columnIndex==1)
      return dataSet.getTargetMinMax();
    if (columnIndex==2)
      return dataSet.getPredictionMinMax();
    int featureIndex = columnIndex - nStandardColumns;
    return dataSet.findMinMax(featureNames[featureIndex]);
  }

  @Override
  public boolean isCellEditable(int rowIndex, int columnIndex) {
    return false; // All cells are non-editable for this viewer
  }
}
