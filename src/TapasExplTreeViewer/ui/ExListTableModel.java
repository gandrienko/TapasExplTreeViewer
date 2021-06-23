package TapasExplTreeViewer.ui;

import TapasDataReader.CommonExplanation;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Hashtable;

public class ExListTableModel extends AbstractTableModel {
  /**
   * The explanations to show
   */
  public ArrayList<CommonExplanation> exList=null;
  public Hashtable<String,int[]> attrMinMax =null;
  public ArrayList<String> listOfFeatures=null;
  public String columnNames[] = {"Action", "N uses", "N flights", "X", "N conditions"};
  
  public ExListTableModel(ArrayList<CommonExplanation> exList, Hashtable<String,int[]> attrMinMax) {
    this.exList=exList;
    this.attrMinMax =attrMinMax;
    if (attrMinMax!=null && !attrMinMax.isEmpty()) {
      listOfFeatures=new ArrayList<String>(attrMinMax.size());
      for (String name:attrMinMax.keySet())
        listOfFeatures.add(name);
    }
    else {
      listOfFeatures=new ArrayList<String>(50);
      for (int i=0; i<exList.size(); i++) {
        CommonExplanation cEx=exList.get(i);
        for (int j=0; j<cEx.eItems.length; j++)
          if (!listOfFeatures.contains(cEx.eItems[j].attr))
            listOfFeatures.add(cEx.eItems[j].attr);
      }
    }
  }
  public int getColumnCount() {
    return columnNames.length + listOfFeatures.size();
  }
  public String getColumnName(int col) {
    return ((col<columnNames.length) ? columnNames[col] :
                listOfFeatures.get(col-columnNames.length));
  }
  public int getRowCount() {
    return exList.size();
  }
  public Class getColumnClass(int c) {
    return (getValueAt(0, c) == null) ? null : getValueAt(0, c).getClass();
  }
  public Object getValueAt(int row, int col) {
    CommonExplanation cEx=exList.get(row);
    switch (col) {
      case 0: return new Integer(cEx.action);
      case 1: return new Integer(cEx.nUses);
      case 2: return new Integer(cEx.uses.size());
      case 3: return new Double(row);
      case 4: return new Integer(cEx.eItems.length);
    }
    String attrName=listOfFeatures.get(col-columnNames.length);
    double values[]={Double.NaN,Double.NaN,Double.NaN,Double.NaN};
    for (int i=0; i<cEx.eItems.length; i++)
      if (attrName.equals(cEx.eItems[i].attr)) {
        if (Double.isNaN(values[0]) || values[0]>cEx.eItems[i].interval[0])
          values[0]=cEx.eItems[i].interval[0];
        if (Double.isNaN(values[1]) || values[1]<cEx.eItems[i].interval[1])
          values[1]=cEx.eItems[i].interval[1];
      }
    if (attrMinMax !=null && !Double.isNaN(values[0]) || !Double.isNaN(values[1]))  {
      int minmax[]= attrMinMax.get(attrName);
      if (minmax!=null) {
        values[2]=minmax[0];
        values[3]=minmax[1];
        if (Double.isNaN(values[0]) || Double.isInfinite(values[0]))
          values[0]=values[2];
        if (Double.isNaN(values[1]) || Double.isInfinite(values[1]))
          values[1]=values[3];
      }
    }
    return values;
  }
  
  public float getColumnMax(int col) {
    float max=Float.NaN;
    for (int i=0; i<getRowCount(); i++) {
      Object v=getValueAt(i,col);
      if (v==null)
        continue;
      if (v instanceof Integer) {
        Integer iv=(Integer)v;
        if (Double.isNaN(max) || max<iv)
          max=iv;
      }
      else
      if (v instanceof Float) {
        Float fv=(Float)v;
        if (Double.isNaN(max) || max<fv)
          max=fv;
      }
      else
      if (v instanceof Double) {
        Double dv=(Double)v;
        if (Double.isNaN(max) || max<dv)
          max=dv.floatValue();
      }
      else
        return Float.NaN;
  }
    return max;
  }
}
