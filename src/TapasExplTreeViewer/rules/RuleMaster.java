package TapasExplTreeViewer.rules;

import TapasDataReader.CommonExplanation;
import TapasDataReader.ExplanationItem;

import java.util.ArrayList;

/**
 * Intended to contain methods for minimisation and aggregation of a set of rules (or explanations)
 */
public class RuleMaster {
  /**
   * Checks if one rule (explanation) subsumes another. If so, returns the more general rule;
   * otherwise, returns null.
   * If two rules differ in the action (decision, prediction), returns null without checking the conditions.
   */
  public static CommonExplanation selectMoreGeneral(CommonExplanation ex1, CommonExplanation ex2) {
    if (ex1==null || ex2==null)
      return null;
    if (ex1.action!=ex2.action)
      return null;
    ExplanationItem e1[]=ex1.eItems, e2[]=ex2.eItems;
    if (e1==null || e1.length<1 || e2==null || e2.length<1)
      return null;
    if (e1.length>e2.length) {
      CommonExplanation ex=ex1; ex1=ex2; ex2=ex;
      e1=ex1.eItems; e2=ex2.eItems;
    }
    boolean subsumes=true;
    for (int i=0; i<e1.length && subsumes; i++) {
      int i2 = -1;
      for (int j = 0; j < e2.length && i2 < 0; j++)
        if (e1[i].attr.equals(e2[j].attr))
          i2 = j;
      if (i2 < 0)
        continue;
      subsumes=includes(e1[i].interval,e2[i2].interval);
    }
    if (subsumes)
      return ex1;
    
    if (e2.length>e1.length)
      return null;
    
    CommonExplanation ex=ex1; ex1=ex2; ex2=ex;
    e1=ex1.eItems; e2=ex2.eItems;
    subsumes=true;
    for (int i=0; i<e1.length && subsumes; i++) {
      int i2 = -1;
      for (int j = 0; j < e2.length && i2 < 0; j++)
        if (e1[i].attr.equals(e2[j].attr))
          i2 = j;
      if (i2 < 0)
        return null;
      subsumes=includes(e1[i].interval,e2[i2].interval);
    }
    if (subsumes)
      return ex1;
    return null;
  }
  
  public static boolean includes(double interval1[], double interval2[]) {
    if (interval1==null || interval2==null)
      return false;
    return interval1[0]<=interval2[0] && interval1[1]>=interval2[1];
  }
  
  public static ArrayList<CommonExplanation> removeLessGeneral(ArrayList<CommonExplanation> exList) {
    if (exList==null || exList.size()<2)
      return exList;
    ArrayList<CommonExplanation> moreGeneral=new ArrayList<CommonExplanation>(exList.size());
    boolean removed[]=new boolean[exList.size()];
    for (int i=0; i<removed.length; i++)
      removed[i]=false;
    for (int i=0; i<exList.size()-1; i++) {
      for (int j = i + 1; j < exList.size() && !removed[i]; j++) {
        CommonExplanation gEx = selectMoreGeneral(exList.get(i), exList.get(j));
        if (gEx != null) {
          moreGeneral.add(gEx);
          removed[i]=removed[j]=true;
        }
      }
    }
    if (moreGeneral.isEmpty())
      return exList; //nothing reduced
    boolean changed;
    do {
      changed=false;
      for (int i = 0; i < moreGeneral.size(); i++)
        for (int j = 0; j < exList.size(); j++)
          if (!removed[j]){
            CommonExplanation gEx=selectMoreGeneral(moreGeneral.get(i),exList.get(j));
            if (gEx!=null) {
              if (!gEx.equals(moreGeneral.get(i))) {
                moreGeneral.add(i,gEx);
                moreGeneral.remove(i+1);
              }
              removed[j]=true;
              changed=true;
            }
          }
    } while (changed);
    for (int i=0; i<exList.size(); i++)
      if (!removed[i])
        moreGeneral.add(exList.get(i));
    return  moreGeneral;
  }
}
