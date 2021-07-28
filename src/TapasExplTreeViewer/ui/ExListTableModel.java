package TapasExplTreeViewer.ui;

import TapasDataReader.CommonExplanation;
import TapasDataReader.Explanation;
import TapasExplTreeViewer.clustering.ClustersAssignments;
import TapasExplTreeViewer.rules.UnitedRule;
import TapasUtilities.MySammonsProjection;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

public class ExListTableModel extends AbstractTableModel implements ChangeListener {
  /**
   * The explanations to show
   */
  public ArrayList<CommonExplanation> exList=null;
  public Hashtable<String,float[]> attrMinMax =null;
  public ArrayList<String> listOfFeatures=null;
  public int order[]=null, clusters[]=null;
  public String columnNames[] = {"Action", "(mean) Q", "min Q", "max Q", "N uses", "N data items", "Order", "Cluster", "N conditions"};
  
  public ExListTableModel(ArrayList<CommonExplanation> exList, Hashtable<String,float[]> attrMinMax) {
    this.exList=exList;
    this.attrMinMax =attrMinMax;
    Hashtable<String,Integer> attrUses=new Hashtable<String,Integer>(50);
    boolean hasUnitedRules=false;
    for (int i=0; i<exList.size(); i++) {
      CommonExplanation cEx = exList.get(i);
      hasUnitedRules=hasUnitedRules || (cEx instanceof UnitedRule);
      for (int j = 0; j < cEx.eItems.length; j++) {
        Integer count=attrUses.get(cEx.eItems[j].attr);
        if (count==null)
          attrUses.put(cEx.eItems[j].attr,1);
        else
          attrUses.put(cEx.eItems[j].attr,count+1);
      }
    }
    listOfFeatures=new ArrayList<String>(attrUses.size());
    for (Map.Entry<String,Integer> entry:attrUses.entrySet()) {
      String aName=entry.getKey();
      if (listOfFeatures.isEmpty())
        listOfFeatures.add(aName);
      else {
        int count=entry.getValue(), idx=-1;
        for (int i=0; i<listOfFeatures.size() && idx<0; i++)
          if (count>attrUses.get(listOfFeatures.get(i)))
            idx=i;
        if (idx<0)
          listOfFeatures.add(aName);
        else
          listOfFeatures.add(idx,aName);
      }
    }
    if (hasUnitedRules) {
      String moreColNames[]=new String[columnNames.length+3];
      for (int i=0; i<columnNames.length; i++)
        moreColNames[i]=columnNames[i];
      int i=columnNames.length;
      moreColNames[i++]="N same action";
      moreColNames[i++]="N other actions";
      moreColNames[i++]="Accuracy";
      columnNames=moreColNames;
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
  
  public void setCusterAssignments(ClustersAssignments clAss) {
    if (clAss==null || clAss.objIndexes==null)
      return;
    if (order==null || order.length!=exList.size())
      order = new int[exList.size()];
    if (clusters==null || clusters.length!=exList.size())
      clusters = new int[exList.size()];
    for (int i=0; i<order.length; i++) {
      order[i] = i; clusters[i]=-1;
    }
    for (int i=0; i<clAss.objIndexes.length; i++) {
      int idx=clAss.objIndexes[i];
      if (idx>=0 && idx<exList.size()) {
        if (order[idx]!=idx) {
          System.out.println("Repeated order assignment to item #"+idx+
                                ": previous value = "+order[idx]+", new value = "+i);
        }
        order[idx]=i;
        clusters[idx]=clAss.clusters[i];
      }
      else
        System.out.println("Trying to assign the ordinal value "+i+" to item #"+idx+
                               " while there are "+exList.size()+" items");
    }
    fireTableDataChanged();
  }
  
  public Object getValueAt(int row, int col) {
    CommonExplanation cEx=exList.get(row);
    if (col<columnNames.length)
      switch (col) {
        case 0: return new Integer(cEx.action);
        case 1: return new Float(cEx.meanQ);
        case 2: return new Float(cEx.minQ);
        case 3: return new Float(cEx.maxQ);
        case 4: return new Integer(cEx.nUses);
        case 5: return new Integer(cEx.uses.size());
        case 6: return (order==null)?new Integer(row):new Integer(order[row]);
        case 7: return (clusters==null)?new Integer(-1):new Integer(clusters[row]);
        case 8: return new Integer(cEx.eItems.length);
        case 9: return (cEx instanceof UnitedRule)?((UnitedRule)cEx).nOrigRight:1;
        case 10: return (cEx instanceof UnitedRule)?((UnitedRule)cEx).nOrigWrong:0;
        case 11:
          if (cEx instanceof UnitedRule) {
            UnitedRule r=(UnitedRule)cEx;
            return 100f*r.nOrigRight/(r.nOrigRight+r.nOrigWrong);
          }
          else
            return 100f;
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
      float minmax[]= attrMinMax.get(attrName);
      if (minmax!=null) {
        values[2]=minmax[0];
        values[3]=minmax[1];
        if (Double.isNaN(values[0]) || Double.isInfinite(values[0]))
          values[0]=values[2];
        if (Double.isNaN(values[1]) || Double.isInfinite(values[1]))
          values[1]=values[3];
      }
    }
    int N=0;
    for (String s:cEx.uses.keySet()) {
      ArrayList<Explanation> aex = cEx.uses.get(s);
      N += aex.size();
    }
    double v[]=new double[values.length+N];
    for (int j=0; j<values.length; j++)
      v[j]=values[j];
    int j=0;
    for (String s:cEx.uses.keySet()) {
      ArrayList<Explanation> aex=cEx.uses.get(s);
      for (Explanation ex:aex) {
        int attrIdx=-1;
        for (int i=0; i<ex.eItems.length && attrIdx==-1; i++)
          if (attrName.equals(ex.eItems[i].attr))
            attrIdx=i;
        if (attrIdx!=-1) {
          float vv=ex.eItems[attrIdx].value;
          v[values.length+j]=vv;
        }
        j++;
      }
    }
    return v;
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
      if (v instanceof double[]) {
        double d[]=(double[])v, dv=d[d.length-1];
        if (Double.isNaN(max) || max<dv)
          max=(float)dv;
      }
      else
        return Float.NaN;
  }
    return max;
  }

  public float getColumnMin(int col) {
    float min=Float.NaN;
    for (int i=0; i<getRowCount(); i++) {
      Object v=getValueAt(i,col);
      if (v==null)
        continue;
      if (v instanceof Integer) {
        Integer iv=(Integer)v;
        if (Double.isNaN(min) || min>iv)
          min=iv;
      }
      else
      if (v instanceof Float) {
        Float fv=(Float)v;
        if (Double.isNaN(min) || min>fv)
          min=fv;
      }
      else
      if (v instanceof Double) {
        Double dv=(Double)v;
        if (Double.isNaN(min) || min>dv)
          min=dv.floatValue();
      }
      else
      if (v instanceof double[]) {
        double d[]=(double[])v, dv=d[d.length-1];
        if (Double.isNaN(min) || min>dv)
          min=(float)dv;
      }
      else
        return Float.NaN;
    }
    return min;
  }

  public void stateChanged(ChangeEvent e) {
    if (e.getSource() instanceof MySammonsProjection) {
      MySammonsProjection sam=(MySammonsProjection)e.getSource();
      double proj[][]=(sam.done)?sam.getProjection():sam.bestProjection;
      if (proj==null)
        return;
      if (proj[0].length==1) { // 1D projection
        System.out.println("Table: update 1D projection coordinates (column X)");
        for (int i=0; i<proj.length && i<exList.size(); i++)
          exList.get(i).x1D=proj[i][0];
      }
      fireTableDataChanged();
    }
    else
      if (e.getSource() instanceof ClustersAssignments)
        setCusterAssignments((ClustersAssignments)e.getSource());
  }
}
