package TapasExplTreeViewer.rules;

import java.util.ArrayList;
import java.util.Collections;

public class ClassConfusionMatrix {
  public ArrayList<Integer> classNs=null;
  public int counts[][]=null;
  public int classTotals[]=null;
  public double percents[][]=null;
  public int nSame=0;

  public boolean makeConfusionMatrix(DataSet data) {
    if (data==null || data.records==null || data.records.isEmpty())
      return false;
    classNs=new ArrayList<Integer>(10);
    for (DataRecord record:data.records) {
      if (!classNs.contains(record.trueClassIdx))
        classNs.add(record.trueClassIdx);
      if (!classNs.contains(record.predictedClassIdx))
        classNs.add(record.predictedClassIdx);
    }
    if (classNs.size()<2)
      return false;
    Collections.sort(classNs);
    int nClasses=classNs.size();
    counts=new int[nClasses][nClasses];
    classTotals=new int[nClasses];
    for (int i=0; i<nClasses; i++) {
      classTotals[i]=0;
      for (int j = 0; j < nClasses; j++)
        counts[i][j] = 0;
    }
    nSame=0;
    for (DataRecord record:data.records)  {
      int i1=classNs.indexOf(record.trueClassIdx),
          i2=(record.trueClassIdx==record.predictedClassIdx)?i1:classNs.indexOf(record.predictedClassIdx);
      if (i1==i2) ++nSame;
      ++classTotals[i1];
      ++counts[i1][i2];
    }
    percents=new double[nClasses][nClasses];
    for (int i=0; i<nClasses; i++)
      for (int j=0; j<nClasses; j++)
        percents[i][j]=100.0*counts[i][j]/classTotals[i];
    return true;
  }
}
