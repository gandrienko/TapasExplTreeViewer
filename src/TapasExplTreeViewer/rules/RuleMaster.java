package TapasExplTreeViewer.rules;

import TapasDataReader.CommonExplanation;
import TapasDataReader.Explanation;

import java.util.ArrayList;
import java.util.Hashtable;

/**
 * Intended to contain methods for minimisation and aggregation of a set of rules (or explanations)
 */
public class RuleMaster {
  /**
   * Checks if one rule (explanation) subsumes another. If so, returns the more general rule;
   * otherwise, returns null.
   * If two rules differ in the action (decision, prediction), returns null without checking the conditions.
   */
  public static UnitedRule selectMoreGeneral(CommonExplanation ex1, CommonExplanation ex2) {
    if (ex1==null || ex2==null)
      return null;
    if (ex1.action!=ex2.action)
      return null;
    if (ex1.subsumes(ex2))
      return putTogether(ex1,ex2);
    if (ex2.subsumes(ex1))
      return putTogether(ex2,ex1);
    return null;
  }
  
  /**
   * Creates a new explanation with the same conditions as in ex1 (which is treated as more general)
   * summarizing information about the uses of ex1 and ex2.
   * @param ex1 - more general explanation
   * @param ex2 - less general information
   * @return more general explanation with summarized uses
   */
  public static UnitedRule putTogether(CommonExplanation ex1, CommonExplanation ex2) {
    if (ex1==null || ex2==null)
      return null;
    UnitedRule gEx= new UnitedRule();
    gEx.action=ex1.action;
    gEx.nUses=ex1.nUses+ex2.nUses;
    if (ex1.uses!=null) {
      gEx.uses = new Hashtable<String, ArrayList<Explanation>>(ex1.uses.size(), ex2.uses.size());
      gEx.uses.putAll(ex1.uses);
      gEx.uses.putAll(ex2.uses);
    }
    gEx.eItems=CommonExplanation.makeCopy(ex1.eItems);
    gEx.fromRules=new ArrayList<UnitedRule>(5);
    gEx.fromRules.add(UnitedRule.getRule(ex1));
    gEx.fromRules.add(UnitedRule.getRule(ex2));
    gEx.nOrigRight=2;
    return gEx;
  }
  
  public static ArrayList<CommonExplanation> removeLessGeneral(ArrayList<CommonExplanation> exList) {
    if (exList==null || exList.size()<2)
      return exList;
    ArrayList<CommonExplanation> moreGeneral=new ArrayList<CommonExplanation>(exList.size());
    boolean removed[]=new boolean[exList.size()];
    for (int i=0; i<removed.length; i++)
      removed[i]=false;
    for (int i=0; i<exList.size()-1; i++) {
      CommonExplanation ex=exList.get(i);
      for (int j = i + 1; j < exList.size(); j++)
        if (!removed[j]) {
          UnitedRule gEx = selectMoreGeneral(ex, exList.get(j));
          if (gEx != null) {
            removed[i]=removed[j]=true;
            ex=gEx;
          }
        }
      if (removed[i]) {
        for (int j=moreGeneral.size()-1; j>=0; j--) {
          UnitedRule gEx = selectMoreGeneral(ex, moreGeneral.get(j));
          if (gEx!=null) {
            moreGeneral.remove(j);
            ex=gEx;
          }
        }
        moreGeneral.add(UnitedRule.getRule(ex));
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
            UnitedRule gEx=selectMoreGeneral(moreGeneral.get(i),exList.get(j));
            if (gEx!=null) {
              moreGeneral.add(i,gEx);
              moreGeneral.remove(i+1);
              removed[j]=true;
              changed=true;
            }
          }
    } while (changed);
    for (int i=0; i<exList.size(); i++)
      if (!removed[i])
        moreGeneral.add(UnitedRule.getRule(exList.get(i)));
    for (int i=0; i<moreGeneral.size(); i++)
      ((UnitedRule)moreGeneral.get(i)).countRightAndWrongCoverages(exList);
    return  moreGeneral;
  }
}
