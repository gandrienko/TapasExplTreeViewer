package TapasExplTreeViewer.rules;

import java.util.HashMap;

public class DataRecord {
  public static final byte NO_TARGET =0, CLASS_TARGET =1, VALUE_TARGET =2, RANGE_TARGET =3;

  public String id=null, name=null;
  public int idx=-1; //index in a data set or table
  public String trueClassLabel=null;
  public int trueClassIdx=-1, predictedClassIdx=-1;
  public double trueValue=Double.NaN, predictedValue=Double.NaN;
  public double predictedValueRange[]=null;
  public byte predictionType= NO_TARGET;
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
    predictionType= NO_TARGET;
  }

  public byte getPredictionType() {
    if (predictionType> NO_TARGET)
      return predictionType;
    predictionType=(predictedClassIdx>=0)? CLASS_TARGET :
                    (!Double.isNaN(predictedValue))? VALUE_TARGET :
                    (predictedValueRange!=null)? RANGE_TARGET : NO_TARGET;
    return predictionType;
  }

  public byte getTargetType() {
    if (trueClassLabel!=null || trueClassIdx>=0)
      return CLASS_TARGET;
    if (!Double.isNaN(trueValue))
      return VALUE_TARGET;
    return NO_TARGET;
  }

  public boolean hasCorrectPrediction(){
    if (getPredictionType()==NO_TARGET)
      return false;
    if (predictionType==CLASS_TARGET)
      return predictedClassIdx==trueClassIdx;
    if (predictionType==VALUE_TARGET)
      return !Double.isNaN(predictedValue) && !Double.isNaN(trueValue) && predictedValue==trueValue;
    if (predictionType==RANGE_TARGET)
      return predictedValueRange!=null && !Double.isNaN(trueValue) &&
          trueValue>=predictedValueRange[0] && trueValue<=predictedValueRange[1];
    return false;
  }
}
