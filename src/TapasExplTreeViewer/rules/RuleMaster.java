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
    UnitedRule rule= new UnitedRule();
    rule.action=ex1.action;
    rule.nUses=ex1.nUses+ex2.nUses;
    if (ex1.uses!=null || ex2.uses!=null) {
      int n1=(ex1.uses!=null)?ex1.uses.size():0, n2=(ex2.uses!=null)?ex2.uses.size():0;
      rule.uses = new Hashtable<String, ArrayList<Explanation>>(n1+n2);
      if (n1>0)
        rule.uses.putAll(ex1.uses);
      if (n2>0)
        rule.uses.putAll(ex2.uses);
    }
    rule.eItems=CommonExplanation.makeCopy(ex1.eItems);
    rule.fromRules=new ArrayList<UnitedRule>(10);
    if (ex1 instanceof UnitedRule) {
      UnitedRule r=(UnitedRule)ex1;
      if (r.fromRules!=null && !r.fromRules.isEmpty())
        rule.fromRules.addAll(r.fromRules);
      else
        rule.fromRules.add(r);
    }
    else
      rule.fromRules.add(UnitedRule.getRule(ex1));
    if (ex2 instanceof UnitedRule) {
      UnitedRule r=(UnitedRule)ex2;
      if (r.fromRules!=null && !r.fromRules.isEmpty())
        rule.fromRules.addAll(r.fromRules);
      else
        rule.fromRules.add(r);
    }
    else
      rule.fromRules.add(UnitedRule.getRule(ex2));
    for (int i=0; i<rule.fromRules.size(); i++) {
      rule.nOrigRight+=rule.fromRules.get(i).nOrigRight;
      rule.nOrigWrong+=rule.fromRules.get(i).nOrigWrong;
    }
    return rule;
  }
  
  /**
   * Removes explanations (or rules) covered by other rules with the same actions whose conditians are more general.
   * @return reduced set of explanations (rules).
   */
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
  
  /**
   * Aggregates rules with coinciding actions and the same features by bottom-up
   * hierarchical uniting of the closest rules. The accuracy of the united rules
   * is checked against the set of original explanations (rules). If the accuracy
   * (a number from 0 to 1) is less than minAccuracy, the united rule is discarded
   * and further aggregation is stopped.
   */
  public static ArrayList<UnitedRule> aggregate(ArrayList<UnitedRule> rules,
                                                ArrayList<CommonExplanation> origRules,
                                                double minAccuracy) {
    if (rules==null || rules.size()<2)
      return rules;
    ArrayList<ArrayList<UnitedRule>> ruleGroups=new ArrayList<ArrayList<UnitedRule>>(rules.size()/2);
    ArrayList<UnitedRule> agRules=new ArrayList<UnitedRule>(ruleGroups.size()*2);
    for (int i=0; i<rules.size(); i++) {
      UnitedRule rule=rules.get(i);
      if (minAccuracy>0) {
        if (rule.nOrigRight<1) {
          rule.countRightAndWrongCoverages(origRules);
          if (rule.nOrigRight<1)
            continue; //must not happen; it means that this rule does not cover any of origRules!
        }
        double accuracy=1.0*rule.nOrigRight/(rule.nOrigRight+rule.nOrigWrong);
        if (accuracy<minAccuracy) {
          agRules.add(rule);
          continue;
        }
      }
      boolean added=false;
      for (int j=0; j<ruleGroups.size() && !added; j++) {
        UnitedRule rule2=ruleGroups.get(j).get(0);
        if (rule.action==rule2.action && UnitedRule.sameFeatures(rule,rule2)) {
          ruleGroups.get(j).add(rule);
          added=true;
        }
      }
      if (!added) {
        ArrayList<UnitedRule> group=new ArrayList<UnitedRule>(100);
        group.add(rule);
        ruleGroups.add(group);
      }
    }
    if (ruleGroups.size()==rules.size())
      return rules;
    for (int ig=0; ig<ruleGroups.size(); ig++) {
      ArrayList<UnitedRule> group=ruleGroups.get(ig);
      if (group.size()==1) {
        agRules.add(group.get(0));
        continue;
      }
      aggregateGroup(group,origRules,minAccuracy);
      for (int i=0; i<group.size(); i++)
        agRules.add(group.get(i));
    }
    if (agRules.size()<rules.size())
      return agRules;
    return rules;
  }
  
  public static void aggregateGroup(ArrayList<UnitedRule> group,
                                    ArrayList<CommonExplanation> origRules,
                                    double minAccuracy) {
    if (group==null || group.size()<2)
      return;
    boolean united;
    do {
      united=false;
      double minDistance=Double.NaN;
      int i1=-1, i2=-1;
      for (int i=0; i<group.size()-1; i++)
        for (int j=i+1; j<group.size(); j++) {
          double d=UnitedRule.distance(group.get(i),group.get(j));
          if (Double.isNaN(minDistance) || minDistance>d) {
            minDistance=d;
            i1=i; i2=j;
          }
        }
      if (i1>=0 && i2>=0)  {
        UnitedRule union=UnitedRule.unite(group.get(i1),group.get(i2));
        if (union!=null) {
          if (minAccuracy>0) {
            if (union.nOrigRight<1) {
              union.countRightAndWrongCoverages(origRules);
              if (union.nOrigRight<1)
                continue; //must not happen; it means that this rule does not cover any of origRules!
            }
            double accuracy=1.0*union.nOrigRight/(union.nOrigRight+union.nOrigWrong);
            if (accuracy>=minAccuracy) {
              group.remove(i2);
              group.remove(i1);
              group.add(0,union);
              united=true;
            }
          }
        }
      }
    } while (united);
  }
}