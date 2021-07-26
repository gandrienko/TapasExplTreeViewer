package TapasExplTreeViewer.rules;

import TapasDataReader.CommonExplanation;
import TapasDataReader.Explanation;
import TapasExplTreeViewer.clustering.ObjectWithMeasure;

import java.util.ArrayList;
import java.util.Collections;
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
  
    rule.minQ=Math.min(ex1.minQ,ex2.minQ);
    rule.maxQ=Math.max(ex1.maxQ,ex2.maxQ);
    rule.sumQ=ex1.sumQ+ex2.sumQ;
    rule.meanQ=rule.sumQ/rule.nUses;

    return rule;
  }
  
  /**
   * Removes explanations (or rules) covered by other rules with the same actions whose conditians are more general.
   * @return reduced set of explanations (rules).
   */
  public static ArrayList<CommonExplanation> removeLessGeneral(ArrayList<CommonExplanation> rules,
                                                               ArrayList<CommonExplanation> origRules,
                                                               Hashtable<String,float[]> attrMinMax) {
    if (rules==null || rules.size()<2)
      return rules;
    if (attrMinMax!=null) {
      ArrayList<CommonExplanation> rules2=null;
      for (int i=0; i<rules.size(); i++) {
        CommonExplanation ex= rules.get(i), ex2=UnitedRule.adjustToFeatureRanges(ex,attrMinMax);
        if (!ex2.equals(ex)) {
          if (rules2==null) {
            rules2=new ArrayList<CommonExplanation>(rules.size());
            for (int j=0; j<i; j++)
              rules2.add(rules.get(j));
          }
          rules2.add(ex2);
        }
        else
          if (rules2!=null)
            rules2.add(ex);
      }
      if (rules2!=null)
        rules=rules2;
    }
    ArrayList<CommonExplanation> moreGeneral=new ArrayList<CommonExplanation>(rules.size());
    boolean removed[]=new boolean[rules.size()];
    for (int i=0; i<removed.length; i++)
      removed[i]=false;
    for (int i=0; i<rules.size()-1; i++) {
      CommonExplanation ex= rules.get(i);
      for (int j = i + 1; j < rules.size(); j++)
        if (!removed[j]) {
          UnitedRule gEx = selectMoreGeneral(ex, rules.get(j));
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
      return rules; //nothing reduced
    boolean changed;
    do {
      changed=false;
      for (int i = 0; i < moreGeneral.size(); i++)
        for (int j = 0; j < rules.size(); j++)
          if (!removed[j]){
            UnitedRule gEx=selectMoreGeneral(moreGeneral.get(i),rules.get(j));
            if (gEx!=null) {
              moreGeneral.add(i,gEx);
              moreGeneral.remove(i+1);
              removed[j]=true;
              changed=true;
            }
          }
    } while (changed);
    for (int i=0; i<rules.size(); i++)
      if (!removed[i])
        moreGeneral.add(UnitedRule.getRule(rules.get(i)));
    for (int i=0; i<moreGeneral.size(); i++)
      ((UnitedRule)moreGeneral.get(i)).countRightAndWrongCoverages((origRules!=null)?origRules:rules);
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
                                                double minAccuracy,
                                                Hashtable<String,float[]> attrMinMax) {
    if (rules==null || rules.size()<2)
      return rules;
    ArrayList<ArrayList<UnitedRule>> ruleGroups=new ArrayList<ArrayList<UnitedRule>>(rules.size()/2);
    ArrayList<UnitedRule> agRules=new ArrayList<UnitedRule>(ruleGroups.size()*2);
    for (int i=0; i<rules.size(); i++) {
      UnitedRule rule=rules.get(i);
      boolean added=false;
      for (int j=0; j<ruleGroups.size() && !added; j++) {
        UnitedRule rule2=ruleGroups.get(j).get(0);
        if (rule.action==rule2.action) {
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
    boolean noActions=ruleGroups.size()<2;
    //todo: correctly handle the case of no actions
    //...
    
    for (int ig=0; ig<ruleGroups.size(); ig++) {
      ArrayList<UnitedRule> group=ruleGroups.get(ig);
      if (group.size()==1) {
        agRules.add(group.get(0));
        continue;
      }
      aggregateGroup(group,origRules,minAccuracy,attrMinMax);
      for (int i=0; i<group.size(); i++)
        agRules.add(group.get(i));
    }
    if (agRules.size()<rules.size())
      return agRules;
    return rules;
  }
  
  public static void aggregateGroup(ArrayList<UnitedRule> group,
                                    ArrayList<CommonExplanation> origRules,
                                    double minAccuracy,
                                    Hashtable<String,float[]> attrMinMax) {
    if (group==null || group.size()<2)
      return;
    boolean united;
    ArrayList<ObjectWithMeasure> pairs=new ArrayList<ObjectWithMeasure>(group.size());
    do {
      united=false;
      pairs.clear();
      for (int i=0; i<group.size()-1; i++)
        if (minAccuracy<=0 || getAccuracy(group.get(i),origRules)>=minAccuracy)
          for (int j=i+1; j<group.size(); j++)
            if ((minAccuracy<=0 || getAccuracy(group.get(j),origRules)>=minAccuracy) &&
                UnitedRule.sameFeatures(group.get(i),group.get(j))) {
              double d=UnitedRule.distance(group.get(i),group.get(j),attrMinMax);
              int pair[]={i,j};
              ObjectWithMeasure om=new ObjectWithMeasure(pair,d,false);
              pairs.add(om);
            }
      Collections.sort(pairs);
      for (int i=0; i<pairs.size() && !united; i++) {
        ObjectWithMeasure om=pairs.get(i);
        int pair[]=(int[])om.obj;
        int i1=pair[0], i2=pair[1];
        UnitedRule union=UnitedRule.unite(group.get(i1),group.get(i2),attrMinMax);
        if (union!=null) {
          union.countRightAndWrongCoverages(origRules);
          if (minAccuracy>0 && getAccuracy(union,origRules)<minAccuracy)
            continue;
          group.remove(i2);
          group.remove(i1);
          for (int j=group.size()-1; j>=0; j--)
            if (union.subsumes(group.get(j)))
              group.remove(j);
          group.add(0,union);
          united=true;
        }
      }
    } while (united && group.size()>1);
  }
  
  public static double getAccuracy(UnitedRule rule, ArrayList<CommonExplanation> origRules) {
    if (rule==null || origRules==null)
      return Double.NaN;
    if (rule.nOrigRight<1)
      rule.countRightAndWrongCoverages(origRules);
    if (rule.nOrigRight<1)
      return Double.NaN; //must not happen; it means that this rule does not cover any of origRules!
    return 1.0*rule.nOrigRight/(rule.nOrigRight+rule.nOrigWrong);
  }
}
