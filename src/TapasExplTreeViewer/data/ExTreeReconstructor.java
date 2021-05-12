package TapasExplTreeViewer.data;

import TapasDataReader.ExplanationItem;
import TapasDataReader.Flight;
import TapasDataReader.Explanation;

import java.util.Hashtable;
import java.util.Map;

public class ExTreeReconstructor {
  public Hashtable<Integer,ExTreeNode> topNodes=null;
  public boolean reconstructExTree (Hashtable<String, Flight> flights) {
    if (flights==null || flights.isEmpty())
      return false;
    for (Map.Entry<String,Flight> e:flights.entrySet()) {
      Flight f=e.getValue();
      if (f.expl==null || f.expl.length<1)
        continue;
      for (int i=0; i<f.expl.length; i++) {
        if (topNodes==null)
          topNodes=new Hashtable<Integer,ExTreeNode>(20);
        ExTreeNode currNode=topNodes.get(f.delays[i]);
        if (currNode==null) {
          currNode=new ExTreeNode();
          topNodes.put(f.delays[i],currNode);
        }
        if (f.expl[i] != null && f.expl[i].eItems != null)
          for (int j = 0; j < f.expl[i].eItems.length; j++) {
            ExplanationItem eIt = f.expl[i].eItems[j];
            ExTreeNode child=null;
            if (currNode.children!=null)
              child=currNode.children.get(eIt.attr);
            if (child==null) {
              child = new ExTreeNode();
              child.attrName = eIt.attr;
              child.level = eIt.level;
              child.interval = eIt.interval;
            }
          }
      }
    }
    return topNodes!=null && !topNodes.isEmpty();
  }
}
