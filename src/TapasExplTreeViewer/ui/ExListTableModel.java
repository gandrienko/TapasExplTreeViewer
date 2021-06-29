package TapasExplTreeViewer.ui;

import TapasDataReader.CommonExplanation;
import TapasExplTreeViewer.clustering.ClustersAssignments;
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
  public Hashtable<String,int[]> attrMinMax =null;
  public ArrayList<String> listOfFeatures=null;
  public int order[]=null, clusters[]=null;
  public String columnNames[] = {"Action", "N uses", "N flights", "X", "Order", "Cluster", "N conditions"};
  
  public ExListTableModel(ArrayList<CommonExplanation> exList, Hashtable<String,int[]> attrMinMax) {
    this.exList=exList;
    this.attrMinMax =attrMinMax;
    Hashtable<String,Integer> attrUses=new Hashtable<String,Integer>(50);
    for (int i=0; i<exList.size(); i++) {
      CommonExplanation cEx = exList.get(i);
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
        order[idx]=i;
        clusters[idx]=clAss.clusters[i];
      }
    }
    fireTableDataChanged();
  }
  
  public Object getValueAt(int row, int col) {
    CommonExplanation cEx=exList.get(row);
    switch (col) {
      case 0: return new Integer(cEx.action);
      case 1: return new Integer(cEx.nUses);
      case 2: return new Integer(cEx.uses.size());
      case 3: return (Double.isNaN(cEx.x1D))?new Double(row):new Double(cEx.x1D);
      case 4: return (order==null)?new Integer(row):new Integer(order[row]);
      case 5: return (clusters==null)?new Integer(-1):new Integer(clusters[row]);
      case 6: return new Integer(cEx.eItems.length);
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
