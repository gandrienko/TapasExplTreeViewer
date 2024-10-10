package TapasExplTreeViewer.rules;

import java.util.HashMap;

public class DataRecord {
  public static final byte NO_PREDICTION=0, CLASS_PREDICTION=1, VALUE_PREDICTION=2, RANGE_PREDICTION=3;

  public String id=null, name=null;
  public int idx=-1; //index in a data set or table
  public String trueClassLabel=null;
  public int trueClassIdx=-1, predictedClassIdx=-1;
  public double trueValue=Double.NaN, predictedValue=Double.NaN;
  public double predictedValueRange[]=null;
  public byte predictionType=NO_PREDICTION;
  public HashMap<String,DataElement> items=null;

  public DataRecord(){}

  public DataRecord(int idx, String id, String name) {
    this.idx=idx; this.id=id; this.name=name;
  }

  public void addDataElement(DataElement item) {
    if (item==null || item.feature==null)
      return;
    if (items==null)
      items=new HashMap<String, DataElement>(50);
    items.put(item.feature,item);
  }

  public void addDataElement(String feature, String value, byte dataType) {
    if (feature==null)
      return;
    DataElement item=new DataElement();
    item.feature=feature;
    if (dataType>=0)
      item.setDataType(dataType);
    item.setStringValue(value);
    addDataElement(item);
  }

  public DataElement getDataElement(String feature) {
    if (items==null)
      return null;
    return items.get(feature);
  }

  public void erasePrediction(){
    predictedClassIdx=-1;
    predictedValue=Double.NaN;
    predictedValueRange=null;
    predictionType=NO_PREDICTION;
  }

  public byte getPredictionType() {
    if (predictionType>NO_PREDICTION)
      return predictionType;
    predictionType=(predictedClassIdx>=0)?CLASS_PREDICTION:
                    (!Double.isNaN(predictedValue))?VALUE_PREDICTION:
                    (predictedValueRange!=null)?RANGE_PREDICTION:NO_PREDICTION;
    return predictionType;
  }
}
