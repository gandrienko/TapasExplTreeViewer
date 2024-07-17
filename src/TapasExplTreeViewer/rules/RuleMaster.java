package TapasExplTreeViewer.rules;

import TapasDataReader.CommonExplanation;
import TapasDataReader.Explanation;
import TapasExplTreeViewer.clustering.ObjectWithMeasure;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.util.*;

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
      //if (rule.uses.size()<n1+n2)  //there are common uses of the original rules
        //rule.nUses-=n1+n2-rule.uses.size();
    }
    rule.eItems=CommonExplanation.makeCopy(ex1.eItems);
    /*
    rule.fromRules=new ArrayList<UnitedRule>(10);
    rule.fromRules.add(UnitedRule.getRule(ex1));
    rule.fromRules.add(UnitedRule.getRule(ex2));
    for (int i=0; i<rule.fromRules.size(); i++) {
      rule.nOrigRight+=rule.fromRules.get(i).nOrigRight;
      rule.nOrigWrong+=rule.fromRules.get(i).nOrigWrong;
    }
    */
  
    rule.minQ=Math.min(ex1.minQ,ex2.minQ);
    rule.maxQ=Math.max(ex1.maxQ,ex2.maxQ);
    rule.sumQ=ex1.sumQ+ex2.sumQ;
    rule.meanQ=(float)(rule.sumQ/rule.nUses);

    return rule;
  }
  
  /**
   * Removes explanations (or rules) covered by other rules with the same actions
   * whose conditions are more general.
   * @return reduced set of explanations (rules).
   */
  public static ArrayList<CommonExplanation> removeLessGeneral(ArrayList<CommonExplanation> rules,
                                                               ArrayList<CommonExplanation> origRules,
                                                               Hashtable<String,float[]> attrMinMax,
                                                               boolean useQ, double maxQDiff,
                                                               boolean listSubsumedRules) {
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
    for (int i=0; i<rules.size(); i++) {
      CommonExplanation ex= rules.get(i);
      UnitedRule r=UnitedRule.getRule(ex);
      if (useQ)
        r.countRightAndWrongCoveragesByQ(origRules);
      else
        r.countRightAndWrongCoverages(origRules);
      if (!(ex instanceof UnitedRule)) {
        rules.remove(i);
        rules.add(i,r);
      }
    }
    
    ArrayList<CommonExplanation> moreGeneral=new ArrayList<CommonExplanation>(rules.size());
    boolean removed[]=new boolean[rules.size()];
    for (int i=0; i<removed.length; i++)
      removed[i]=false;
    for (int i=0; i<rules.size()-1; i++)
      if (!removed[i]) {
        CommonExplanation ex= rules.get(i);
        for (int j = i + 1; j < rules.size(); j++)
          if (!removed[j]) {
            UnitedRule gEx = selectMoreGeneral(ex, rules.get(j));
            if (gEx != null) {
              if (useQ && gEx.maxQ-gEx.minQ>maxQDiff);
              else {
                removed[i] = removed[j] = true;
                ex = gEx;
              }
            }
          }
        if (removed[i]) {
          for (int j=moreGeneral.size()-1; j>=0; j--) {
            UnitedRule gEx = selectMoreGeneral(ex, moreGeneral.get(j));
            if (gEx!=null) {
              if (useQ && gEx.maxQ-gEx.minQ>maxQDiff);
              else {
                moreGeneral.remove(j);
                ex = gEx;
              }
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
              if (useQ && gEx.maxQ-gEx.minQ>maxQDiff);
              else {
                moreGeneral.add(i, gEx);
                moreGeneral.remove(i + 1);
                removed[j] = true;
                changed = true;
              }
            }
          }
    } while (changed);
    for (int i=0; i<rules.size(); i++)
      if (!removed[i])
        moreGeneral.add(UnitedRule.getRule(rules.get(i)));
      else
        for (CommonExplanation gen:moreGeneral)
          if (gen.subsumes(rules.get(i))) {
            UnitedRule genR=(UnitedRule.getRule(gen));
            if (genR.equals(rules.get(i)))
              break;
            if (genR.fromRules==null)
              genR.fromRules=new ArrayList<UnitedRule>(50);
            genR.fromRules.add(UnitedRule.getRule(rules.get(i)));
            break;
          }
  
    boolean noActions=noActionDifference(rules);
    for (int i=0; i<moreGeneral.size(); i++)
      if (noActions)
        ((UnitedRule)moreGeneral.get(i)).countRightAndWrongCoveragesByQ((origRules!=null)?origRules:rules);
      else
        ((UnitedRule)moreGeneral.get(i)).countRightAndWrongCoverages((origRules!=null)?origRules:rules);
    return  moreGeneral;
  }
  
  public static boolean noActionDifference(ArrayList rules) {
    if (rules==null || rules.size()<2 || !(rules.get(0) instanceof CommonExplanation))
      return true;
    CommonExplanation ex=(CommonExplanation)rules.get(0);
    for (int i=1; i<rules.size(); i++)
      if (ex.action!=((CommonExplanation)rules.get(i)).action)
        return false;
    return true;
  }
  
  public static double suggestMaxQDiff(ArrayList rules) {
    if (rules==null || rules.size()<3 || !(rules.get(0) instanceof CommonExplanation))
      return Double.NaN;
    ArrayList<Double> qList=new ArrayList<Double>(rules.size());
    for (int i=0; i<rules.size(); i++) {
      CommonExplanation ex=(CommonExplanation)rules.get(i);
      if (!Double.isNaN(ex.meanQ))
        qList.add(new Double(ex.meanQ));
    }
    if (qList.size()<3)
      return Double.NaN;
    Collections.sort(qList);
    ArrayList<Double> qDiffList=new ArrayList<Double>(qList.size()-1);
    for (int i=0; i<qList.size()-1; i++)
      qDiffList.add(qList.get(i+1)-qList.get(i));
    Collections.sort(qDiffList);
    int idx=Math.round(0.025f*qDiffList.size());
    if (idx<5)
      idx=Math.round(0.05f*qDiffList.size());
    if (idx<5)
      idx=Math.max(Math.round(0.1f*qDiffList.size()),3);
    double maxQDiff=qDiffList.get(idx);
    if (maxQDiff==0)
      maxQDiff=qDiffList.get(qDiffList.size()-1)/10;
    return maxQDiff;
  }
  
  /**
   * Aggregates rules with coinciding actions by bottom-up
   * hierarchical joining of the closest rules. The accuracy of the united rules
   * is checked against the set of original explanations (rules). If the accuracy
   * (a number from 0 to 1) is less than minAccuracy, the united rule is discarded
   * and further aggregation is stopped.
   */
  public static ArrayList<UnitedRule> aggregate(ArrayList<UnitedRule> rules,
                                                ArrayList<CommonExplanation> origRules,
                                                double minAccuracy,
                                                Hashtable<String,float[]> attrMinMax) {
    return aggregate(rules,origRules,null,minAccuracy,attrMinMax,null);
  }
  
  /**
   * Aggregates rules with coinciding actions by bottom-up
   * hierarchical joining of the closest rules.
   * The accuracy of the united rules is checked against the set of original explanations (rules).
   * If the set of data instances exData is not null, the accuracy is also checked against the data.
   * From two accuracy estimates, the smaller one is taken.
   * If the accuracy (a number from 0 to 1) is less than minAccuracy, the united rule is discarded
   * and further aggregation is stopped.
   */
  public static ArrayList<UnitedRule> aggregate(ArrayList<UnitedRule> rules,
                                                ArrayList<CommonExplanation> origRules,
                                                AbstractList<Explanation> exData,
                                                double minAccuracy,
                                                Hashtable<String,float[]> attrMinMax,
                                                ChangeListener listener) {
    if (rules==null || rules.size()<2)
      return rules;
    ArrayList<ArrayList<UnitedRule>> ruleGroups=new ArrayList<ArrayList<UnitedRule>>(rules.size()/2);
    ArrayList<UnitedRule> agRules=new ArrayList<UnitedRule>(ruleGroups.size()*2);
    for (int i=0; i<rules.size(); i++) {
      UnitedRule rule=rules.get(i);
      rule.countRightAndWrongCoverages(origRules);
      if (minAccuracy>0 && exData != null && rule.nCasesRight + rule.nCasesWrong < 1)
        rule.countRightAndWrongApplications(exData, true);
      
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
    
    if (ruleGroups.size()<2)  //handle the case of no actions
      agRules=aggregateByQ(ruleGroups.get(0), suggestMaxQDiff(rules),origRules,exData,minAccuracy,attrMinMax);
    else {
      SwingWorker workers[]=(listener==null)?null:new SwingWorker[ruleGroups.size()];
      for (int ig=0; ig<ruleGroups.size(); ig++) {
        if (workers!=null)
          workers[ig]=null;
        ArrayList<UnitedRule> group=ruleGroups.get(ig);
        if (group.size()==1) {
          agRules.add(group.get(0));
          continue;
        }
        if (workers!=null) {
          final ArrayList<UnitedRule> finAgRules = agRules;
          workers[ig] = new SwingWorker() {
            @Override
            public Boolean doInBackground() {
              aggregateGroup(group, origRules, exData, minAccuracy, attrMinMax);
              return true;
            }

            @Override
            protected void done() {
              finAgRules.addAll(group);
              if (listener != null)
                listener.stateChanged(new ChangeEvent(finAgRules));
            }
          };
          workers[ig].execute();
        }
        else {
          aggregateGroup(group, origRules, exData, minAccuracy, attrMinMax);
          agRules.addAll(group);
          if (listener != null)
            listener.stateChanged(new ChangeEvent(agRules));
        }
      }
      if (workers!=null) {
        // Wait for all workers to finish
        SwingWorker resultAggregator = new SwingWorker() {
          @Override
          protected Void doInBackground() throws Exception {
            for (SwingWorker worker : workers) {
              if (worker!=null)
                worker.get(); // Wait for each worker to finish
            }
            return null;
          }

          @Override
          protected void done() {
            if (listener != null)
              listener.stateChanged(new ChangeEvent("aggregation_finished"));
          }
        };
        resultAggregator.execute();
      }
    }
    return (agRules.size()<rules.size())?agRules:rules;
  }
  
  public static void aggregateGroup(ArrayList<UnitedRule> group,
                                    ArrayList<CommonExplanation> origRules,
                                    AbstractList<Explanation> exData,
                                    double minAccuracy,
                                    Hashtable<String,float[]> attrMinMax) {
    if (group==null || group.size()<2)
      return;
    int origSize=group.size();
    System.out.println("Aggregating group with "+origSize+" rules; action or class = "+group.get(0).action);
    ArrayList<UnitedRule> notAccurate=(minAccuracy>0)?new ArrayList<UnitedRule>(group.size()):null;
    if (minAccuracy>0)
      for (int i=group.size()-1; i>=0; i--) {
        UnitedRule r=group.get(i);
        double dataAcc=(exData==null)?1:(1.0*r.nCasesRight/(r.nCasesRight+r.nCasesWrong));
        if (dataAcc<minAccuracy || getAccuracy(r,origRules,false)<minAccuracy) {
          group.remove(i);
          notAccurate.add(r);
        }
      }
    boolean united;
    ArrayList<ObjectWithMeasure> pairs=new ArrayList<ObjectWithMeasure>(group.size()*2);
    System.out.println("Computing pairwise distances");
    for (int i=0; i<group.size()-1; i++)
      for (int j=i+1; j<group.size(); j++)
        if (/*UnitedRule.sameFeatures(group.get(i),group.get(j))*/true) {
          double d=UnitedRule.distance(group.get(i),group.get(j),attrMinMax);
          int pair[]={i,j};
          ObjectWithMeasure om=new ObjectWithMeasure(pair,d,false);
          pairs.add(om);
        }
    System.out.println("Sorting "+pairs.size()+" pairs by distances");
    Collections.sort(pairs);
    System.out.println("Sorted "+pairs.size()+" pairs!");

    HashSet<Integer> excluded=new HashSet<Integer>(group.size()*10);

    int nUnions=0, nExcluded=0;
    Hashtable<Integer,HashSet<Integer>> failedPairs=new Hashtable<Integer,HashSet<Integer>>(group.size()*5);
    do {
      united=false;
      UnitedRule union=null;
      //pairs.clear();
      for (int i=0; i<pairs.size() && !united; i++) {
        ObjectWithMeasure om=pairs.get(i);
        int pair[]=(int[])om.obj;
        int i1=pair[0], i2=pair[1];
        if (excluded.contains(i1) || excluded.contains(i2))
          continue;
        HashSet<Integer> failed=failedPairs.get(i1);
        if (failed!=null && failed.contains(i2))
          continue;
        union=UnitedRule.unite(group.get(i1),group.get(i2),attrMinMax);
        boolean success=union!=null;
        if (success) {
          union.countRightAndWrongCoverages(origRules);
          if (union.nOrigRight<1)
            System.out.println("Zero coverage!");
          if (minAccuracy>0) {
            if (getAccuracy(union, origRules, false)<minAccuracy)
              success=false;
            else
              if (exData!=null) { //check the accuracy based on the data
                union.countRightAndWrongApplications(exData, true);
                if (1.0*union.nCasesRight/(union.nCasesRight+union.nCasesWrong)<minAccuracy)
                  success=false;
              }
          }
          if (!success) {
            if (failed==null) {
              failed=new HashSet<Integer>(origSize*5);
              failedPairs.put(i1,failed);
            }
            failed.add(i2);
            continue;
          }
          //group.remove(i2);
          //group.remove(i1);
          excluded.add(i1);
          excluded.add(i2);
          nExcluded+=2;
          failedPairs.remove(i1); //no more needed
          failedPairs.remove(i2); //no more needed
          for (int j=group.size()-1; j>=0; j--)
            if (!excluded.contains(j) && union.subsumes(group.get(j),true)) {
              union.attachAsFromRule(group.get(j));
              if (minAccuracy>0 && exData!=null)  //check the accuracy based on the data
                union.countRightAndWrongApplications(exData,true);
              //group.remove(j);
              excluded.add(j);
              ++nExcluded;
            }
          group.add(union);
          united=true;
          ++nUnions;
        }
      }
      if (united) {
        for (int i = pairs.size() - 1; i > 0; i--) {
          ObjectWithMeasure om = pairs.get(i);
          int pair[] = (int[]) om.obj;
          int i1 = pair[0], i2 = pair[1];
          if (excluded.contains(i1) || excluded.contains(i2))
            pairs.remove(i);
        }
        int nRemain=1;
        for (int i=0; i<group.size()-1; i++)
          if (!excluded.contains(i)) {
            ++nRemain;
            double d=UnitedRule.distance(group.get(i),union,attrMinMax);
            int pair[]={i,group.size()-1};
            ObjectWithMeasure om=new ObjectWithMeasure(pair,d,false);
            pairs.add(om);
          }
        //System.out.println("Sorting "+pairs.size()+" pairs by distances");
        Collections.sort(pairs);
        //System.out.println("Sorted "+pairs.size()+" pairs!");
        if (nUnions%10==0) {
          System.out.println("Aggregation: made "+nUnions+" unions; excluded "+nExcluded+" rules; "+
              nRemain+" rules remain; group size = "+group.size());
        }
      }
    } while (united /*&& group.size()>1*/ && pairs.size()>0);

    for (int i=group.size()-1; i>=0; i--)
      if (excluded.contains(i))
        group.remove(i);

    if (notAccurate!=null && !notAccurate.isEmpty())
      group.addAll(notAccurate);

    System.out.println("Finished group aggregation attempt; action or class = "+group.get(0).action+
        "; original size = "+origSize+"; final size = "+group.size());
  }
  /**
   * Aggregates rules with close real-valued outcomes (represented by variable Q) by bottom-up
   * hierarchical joining of the closest rules. Rule results are considered close if their difference
   * does not exceed maxQDiff.
   * The accuracy of the united rules is checked against the set of original explanations (rules).
   * If the accuracy (a number from 0 to 1) is less than minAccuracy, the united rule is discarded
   * and further aggregation is stopped.
   */
  public static ArrayList<UnitedRule> aggregateByQ(ArrayList<UnitedRule> rules,
                                                   double maxQDiff,
                                                   ArrayList<CommonExplanation> origRules,
                                                   double minAccuracy,
                                                   Hashtable<String,float[]> attrMinMax) {
    return aggregateByQ(rules,maxQDiff,origRules,null,minAccuracy,attrMinMax);
  }
  
  /**
   * Aggregates rules with close real-valued outcomes (represented by variable Q) by bottom-up
   * hierarchical joining of the closest rules. Rule results are considered close if their difference
   * does not exceed maxQDiff.
   * The accuracy of the united rules is checked against the set of original explanations (rules).
   * If the set of data instances exData is not null, the accuracy is also checked against the data.
   * From two accuracy estimates, the smaller one is taken.
   * If the accuracy (a number from 0 to 1) is less than minAccuracy, the united rule is discarded
   * and further aggregation is stopped.
   */
  public static ArrayList<UnitedRule> aggregateByQ(ArrayList<UnitedRule> rules,
                                            double maxQDiff,
                                            ArrayList<CommonExplanation> origRules,
                                            AbstractList<Explanation> exData,
                                            double minAccuracy,
                                            Hashtable<String,float[]> attrMinMax) {
    if (rules==null || rules.size()<2)
      return rules;

    System.out.println("Aggregating rules by Q; max difference = "+maxQDiff);
    
    ArrayList<UnitedRule> result=new ArrayList<UnitedRule>(rules.size());
    result.addAll(rules);
    ArrayList<UnitedRule> notAccurate=(minAccuracy>0)?new ArrayList<UnitedRule>(result.size()):null;
    if (minAccuracy>0)
      for (int i=result.size()-1; i>=0; i--) {
        UnitedRule r=result.get(i);
        r.countRightAndWrongCoveragesByQ(origRules);
        if (minAccuracy>0 && exData!=null && r.nCasesRight+r.nCasesWrong<1)
          r.countRightAndWrongApplications(exData,false);
        double dataAcc=(exData==null)?1:(1.0*r.nCasesRight/(r.nCasesRight+r.nCasesWrong));
        if (dataAcc<minAccuracy || getAccuracy(r,origRules,true)<minAccuracy) {
          result.remove(i);
          notAccurate.add(r);
        }
      }

    ArrayList<ObjectWithMeasure> pairs=new ArrayList<ObjectWithMeasure>(result.size());
    boolean united;
    do {
      united=false;
      pairs.clear();
      for (int i=0; i<result.size()-1; i++) {
        UnitedRule r1=result.get(i);
        for (int j = i + 1; j < result.size(); j++) {
          UnitedRule r2=result.get(j);
          if (Math.max(r1.maxQ,r2.maxQ)-Math.min(r1.minQ,r2.minQ)>maxQDiff)
            continue;
          if (/*UnitedRule.sameFeatures(r1, r2)*/true) {
            double d = UnitedRule.distance(r1, r2, attrMinMax);
            int pair[] = {i, j};
            ObjectWithMeasure om = new ObjectWithMeasure(pair, d, false);
            pairs.add(om);
          }
        }
      }
      Collections.sort(pairs);
      for (int i=0; i<pairs.size() && !united; i++) {
        ObjectWithMeasure om=pairs.get(i);
        int pair[]=(int[])om.obj;
        int i1=pair[0], i2=pair[1];
        UnitedRule union=UnitedRule.unite(result.get(i1),result.get(i2),attrMinMax);
        if (union!=null) {
          if (union.maxQ-union.minQ>maxQDiff)
            continue;
          union.countRightAndWrongCoveragesByQ(origRules);
          if (union.nOrigRight<1)
            System.out.println("Zero coverage!");
          if (minAccuracy>0 && getAccuracy(union,origRules,true)<minAccuracy)
            continue;
          if (minAccuracy>0 && exData!=null) { //check the accuracy based on the data
            union.countRightAndWrongApplications(exData,false);
            if (1.0*union.nCasesRight/(union.nCasesRight+union.nCasesWrong)<minAccuracy)
              continue;
          }
          result.remove(i2);
          result.remove(i1);
          for (int j=result.size()-1; j>=0; j--) {
            UnitedRule r2 = result.get(j);
            if (r2.minQ >= union.minQ && r2.maxQ <= union.maxQ &&
                    union.subsumes(r2)) {
              union.attachAsFromRule(r2);
              if (minAccuracy>0 && exData!=null)  //check the accuracy based on the data
                union.countRightAndWrongApplications(exData,false);
              result.remove(j);
            }
          }
          result.add(0,union);
          united=true;
        }
      }
    } while (united && result.size()>1);
    
    int nResult=result.size();
    if (notAccurate!=null)
      nResult+=notAccurate.size();
    
    if (nResult>=rules.size()) //no aggregation was done
      return rules;
    ArrayList<CommonExplanation> aEx=new ArrayList<CommonExplanation>(result.size());
    aEx.addAll(result);
    aEx = removeLessGeneral(aEx, origRules, attrMinMax, true, maxQDiff, false);
    if (aEx.size()<result.size()) {
      result.clear();
      for (int i=0; i<aEx.size(); i++)
        result.add((UnitedRule)aEx.get(i));
    }
    if (notAccurate!=null && !notAccurate.isEmpty())
      for (int i=0; i<notAccurate.size(); i++)
        result.add(notAccurate.get(i));
    return result;
  }
  
  public static double getAccuracy(UnitedRule rule, ArrayList<CommonExplanation> origRules, boolean byQ) {
    if (rule==null || origRules==null)
      return Double.NaN;
    if (rule.nOrigRight<1)
      if (byQ)
        rule.countRightAndWrongCoveragesByQ(origRules);
      else
        rule.countRightAndWrongCoverages(origRules);
    if (rule.nOrigRight<1) {
      System.out.println("Zero coverage!");
      return Double.NaN; //must not happen; it means that this rule does not cover any of origRules!
    }
    return 1.0*rule.nOrigRight/(rule.nOrigRight+rule.nOrigWrong);
  }
  
  public static boolean hasRuleHierarchies(ArrayList<UnitedRule> rules) {
    if (rules==null || rules.isEmpty())
      return false;
    for (int i=0; i<rules.size(); i++) {
      ArrayList<UnitedRule> from=rules.get(i).fromRules;
      if (from!=null && !from.isEmpty())
        return true;
    }
    return false;
  }
  
  public static ArrayList<UnitedRule> expandRuleHierarchies(ArrayList<CommonExplanation> rules) {
    if (rules==null || rules.isEmpty() || !(rules.get(0) instanceof UnitedRule))
      return null;
    int lastUpId=rules.get(0).upperId;
    boolean wasExpanded=false;
    for (int i=1; i<rules.size() && !wasExpanded; i++)
      wasExpanded=lastUpId!=rules.get(i).upperId;
    if (wasExpanded) {
      ArrayList<CommonExplanation> rCopy = new ArrayList<CommonExplanation>(rules.size());
      for (int i=0; i<rules.size(); i++)
        rCopy.add(((UnitedRule)rules.get(i)).makeRuleCopy(false,false));
    }
    
    ArrayList<UnitedRule> result=new ArrayList<UnitedRule>(rules.size()*5);
    for (int i=0; i<rules.size(); i++)
      expandOneRuleHierarchy((UnitedRule)rules.get(i),result);
    return result;
  }
  
  public static void expandOneRuleHierarchy(UnitedRule rule, ArrayList<UnitedRule> result) {
    if (rule==null || result==null)
      return;
    rule.numId=result.size();
    result.add(rule);
    if (rule.nOrigRight<1)
      System.out.println("Zero coverage!");
    if (rule.fromRules!=null)
      for (int i=0; i<rule.fromRules.size(); i++) {
        rule.fromRules.get(i).upperId=rule.numId;
        expandOneRuleHierarchy(rule.fromRules.get(i),result);
      }
  }
  
  public static int findRuleInList(ArrayList rules, int ruleId){
    if (rules==null || rules.isEmpty())
      return -1;
    if (!(rules.get(0) instanceof CommonExplanation))
      return -1;
    for (int i=0; i<rules.size(); i++)
      if (((CommonExplanation)rules.get(i)).numId==ruleId)
        return i;
    return -1;
  }
}
