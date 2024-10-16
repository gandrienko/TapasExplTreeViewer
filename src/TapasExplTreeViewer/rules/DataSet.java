package TapasExplTreeViewer.rules;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class DataSet {
  public String filePath=null;
  public String fieldNames[]=null;
  public int idIdx=-1, nameIdx=-1, numIdx=-1, classLabelIdx=-1, classNumIdx=-1;

  public ArrayList<DataRecord> records=null;

  /**
   * Determines the type of the target variable, i.e., class (category) or real value
   */
  public byte determineTargetType() {
    if (records == null || records.isEmpty()) return DataRecord.NO_TARGET;
    for (DataRecord data:records) {
      byte type=data.getTargetType();
      if (type>DataRecord.NO_TARGET)
        return type;
    }
    return DataRecord.NO_TARGET;
  }
  /**
   * Finds the type of model predictions made for the data records
   */
  public byte determinePredictionType() {
    if (records == null || records.isEmpty()) return DataRecord.NO_TARGET;
    for (DataRecord data:records)
      if (data.getPredictionType()>DataRecord.NO_TARGET)
        return data.getPredictionType();
    return DataRecord.NO_TARGET;
  }

  public DataSet extractWronglyPredicted(){
    if (determineTargetType()==DataRecord.NO_TARGET)
      return null; //no target to predict
    if (determinePredictionType()==DataRecord.NO_TARGET)
      return null; //no predictions available
    ArrayList<DataRecord> wrong=null;
    for (DataRecord data:records)
      if (!data.hasCorrectPrediction()) {
        if (wrong==null)
          wrong=new ArrayList<DataRecord>(records.size()/3);
        wrong.add(data);
      }
    if (wrong==null)
      return null;
    DataSet ds=new DataSet();
    ds.records=wrong;
    ds.fieldNames=fieldNames;
    ds.idIdx=idIdx; ds.nameIdx=nameIdx; ds.numIdx=numIdx;
    ds.classLabelIdx=classLabelIdx; ds.classNumIdx=classNumIdx;
    return ds;
  }

  /**
   * Exports the data records to a CSV file, including prediction results based on the prediction type.
   */
  public boolean exportToCSV(String outputFilePath) {
    if (fieldNames==null || records==null || records.isEmpty()) {
      System.out.println("No data in the dataset!");
      return false;
    }
    try (FileWriter writer = new FileWriter(outputFilePath)) {
      // Determine the prediction type for the data set
      byte predictionType = determinePredictionType();

      // Create a list of header fields, including predicted fields based on the prediction type
      ArrayList<String> headerFields = new ArrayList<String>(fieldNames.length+2);
      if (fieldNames != null) {
        for (String field : fieldNames) {
          headerFields.add(field);
        }
      }

      // Add additional fields based on the prediction type
      switch (predictionType) {
        case DataRecord.CLASS_TARGET:
          headerFields.add("PredictedClass");
          break;
        case DataRecord.VALUE_TARGET:
          headerFields.add("PredictedValue");
          break;
        case DataRecord.RANGE_TARGET:
          headerFields.add("PredictedValueLowerBound");
          headerFields.add("PredictedValueUpperBound");
          break;
        default:
          break; // No additional fields needed for NO_TARGET
      }

      // Write the header row to the CSV file
      writer.append(String.join(",", headerFields));
      writer.append("\n");

      // Write each data record to the CSV file
      for (DataRecord record : records) {
        ArrayList<String> rowValues = new ArrayList<String>(headerFields.size());
        for (int i=0; i<fieldNames.length; i++)
          if (i==idIdx) rowValues.add(record.id); else
          if (i==nameIdx) rowValues.add(record.name); else
          if (i==numIdx) rowValues.add(Integer.toString(record.idx)); else
          if (i==classLabelIdx) rowValues.add(record.trueClassLabel); else
          if (i==classNumIdx) rowValues.add(Integer.toString(record.trueClassIdx));
          else {
            DataElement item=record.getDataElement(fieldNames[i]);
            if (item==null)
              rowValues.add("");
            else {
              String sValue=item.getStringValue();
              rowValues.add((sValue==null)?"":sValue);
            }
          }
        switch (predictionType) {
          case DataRecord.CLASS_TARGET:
            rowValues.add((record.predictedClassIdx>=0)?Integer.toString(record.predictedClassIdx):"");
            break;
          case DataRecord.VALUE_TARGET:
            rowValues.add((Double.isNaN(record.predictedValue))?"":String.format("%.6f",record.predictedValue));
            break;
          case DataRecord.RANGE_TARGET:
            if (record.predictedValueRange==null) {
              rowValues.add(""); rowValues.add("");
            }
            else {
              rowValues.add(String.format("%.6f",record.predictedValueRange[0]));
              rowValues.add(String.format("%.6f",record.predictedValueRange[1]));
            }
            break;
        }

        // Write the row to the CSV file
        writer.append(String.join(",", rowValues));
        writer.append("\n");
      }

      writer.flush();
      return true; // Successful export
    } catch (IOException e) {
      e.printStackTrace();
      return false; // Failed export
    }
  }
}
