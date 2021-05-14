package TapasExplTreeViewer.data;

import TapasDataReader.ExplanationItem;
import TapasDataReader.Flight;

import java.util.Hashtable;
import java.util.Map;

public class ExTreeReconstructor {
  public Hashtable<Integer,ExTreeNode> topNodes=null;
  public Hashtable<String,Integer> attributes=null;
  public Hashtable<String,Integer> sectors=null;
  
  public boolean reconstructExTree (Hashtable<String, Flight> flights) {
    if (flights==null || flights.isEmpty())
      return false;
    for (Map.Entry<String,Flight> e:flights.entrySet()) {
      Flight f=e.getValue();
      if (f.expl==null || f.expl.length<1)
        continue;
      for (int i=0; i<f.expl.length; i++) {
        if (f.expl[i] != null && f.expl[i].eItems != null) {
          if (topNodes==null)
            topNodes=new Hashtable<Integer,ExTreeNode>(20);
          ExTreeNode currNode=topNodes.get(f.expl[i].action);
          if (currNode==null) {
            currNode=new ExTreeNode();
            topNodes.put(f.expl[i].action,currNode);
            currNode.attrName="Action = "+f.expl[i].action;
          }
          currNode.addUse();
          for (int j = 0; j < f.expl[i].eItems.length; j++) {
            ExplanationItem eIt = f.expl[i].eItems[j];
            if (eIt == null)
              continue;
            ExTreeNode child = currNode.findChild(eIt.attr, eIt.interval);
            if (child == null) {
              child = new ExTreeNode();
              child.attrName = eIt.attr;
              child.level = eIt.level;
              child.condition = eIt.interval;
              currNode.addChild(child);
            }
            child.addUse();
            currNode = child;
  
            if (attributes==null)
              attributes=new Hashtable<String,Integer>(100);
            Integer n=attributes.get(child.attrName);
            if (n==null) n=0;
            attributes.put(child.attrName,n+1);
            
            if (eIt.sector!=null && !eIt.sector.equalsIgnoreCase("null")) {
              if (sectors==null)
                sectors=new Hashtable<String,Integer>(100);
              n=sectors.get(eIt.sector);
              if (n==null) n=0;
              sectors.put(eIt.sector,n+1);
            }
          }
        }
      }
    }
    return topNodes!=null && !topNodes.isEmpty();
  }
}
