package TapasExplTreeViewer.ui;

import TapasDataReader.CommonExplanation;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

public class DataForRuleTableModel extends AbstractTableModel {

  public CommonExplanation cEx=null;
  public Hashtable<String,float[]> attrMinMax=null;
  public ArrayList<String> listOfFeatures=null;


  public DataForRuleTableModel (CommonExplanation cEx, ArrayList<String> listOfFeatures, Hashtable<String,float[]> attrMinMax) {
    this.cEx=cEx;
    this.attrMinMax=attrMinMax;
    this.listOfFeatures=listOfFeatures;
    Hashtable<String,Integer> attrUses=new Hashtable<String,Integer>(50);
    for (int j = 0; j < cEx.eItems.length; j++) {
      Integer count=attrUses.get(cEx.eItems[j].attr);
      if (count==null)
        attrUses.put(cEx.eItems[j].attr,1);
      else
        attrUses.put(cEx.eItems[j].attr,count+1);
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

  public String columnNames[] = {"Id","Action"};

  public int getColumnCount() {
    return columnNames.length+listOfFeatures.size();
  }
  public String getColumnName(int col) {
    return ((col<columnNames.length) ? columnNames[col] :
            listOfFeatures.get(col-columnNames.length));
  }
  public int getRowCount() {
    return cEx.applications.length; //exList.size();
  }

  public Class getColumnClass(int c) {
    return (getValueAt(0, c) == null) ? null : getValueAt(0, c).getClass();
  }

  public Object getValueAt(int row, int col) {
    switch (col) {
      case 0:
        return cEx.applications[row].data.FlightID;
      case 1:
        return cEx.applications[row].data.action;
    }
    return 0;
  }

}
