package TapasExplTreeViewer.rules;

import TapasDataReader.CommonExplanation;
import TapasDataReader.Explanation;
import TapasDataReader.ExplanationItem;

import java.util.ArrayList;
import java.util.Hashtable;

/**
 * Represents a union of two or more rules or explanations
 */

public class UnitedRule extends CommonExplanation {
  /**
   * The rules from which this rule was produced by uniting conditions
   */
  public ArrayList<UnitedRule> fromRules=null;
  /**
   * The number of original rules with the same result (decision, action) covered by this united rule.
   */
  public int nOrigRight=0;
  /**
   * The number of original rules covered by this united rule where the result (decision, action)
   * differs from the result of this rule.
   */
  public int nOrigWrong=0;
  
  public void attachAsFromRule(CommonExplanation ex) {
    if (ex==null)
      return;
    if (fromRules==null)
      fromRules=new ArrayList<UnitedRule>(10);
    UnitedRule rule=getRule(ex);
    fromRules.add(rule);
    if (rule.uses!=null && !rule.uses.isEmpty()) {
      if (uses==null)
        uses = new Hashtable<String, ArrayList<Explanation>>(rule.uses.size()+10);
      uses.putAll(rule.uses);
    }
    nUses+=rule.nUses;
    nOrigRight+=rule.nOrigRight;
    nOrigWrong+=rule.nOrigWrong;
    minQ=Math.min(minQ,rule.minQ);
    maxQ=Math.max(maxQ,rule.maxQ);
    sumQ+=rule.sumQ;
    meanQ=(float)sumQ/nUses;
  }
  
  public static UnitedRule getRule(CommonExplanation ex) {
    if (ex==null)
      return null;
    if (ex instanceof UnitedRule)
      return (UnitedRule)ex;
    UnitedRule rule=new UnitedRule();
    rule.action=ex.action;
    rule.eItems=ex.eItems;
    rule.uses=ex.uses;
    rule.nUses=ex.nUses;
    rule.minQ=ex.minQ;
    rule.maxQ=ex.maxQ;
    rule.meanQ=ex.meanQ;
    rule.sumQ=ex.sumQ;
    return rule;
  }
  
  public static ArrayList<UnitedRule> getRules(ArrayList<CommonExplanation> exList) {
    if (exList==null)
      return null;
    ArrayList<UnitedRule> rules=new ArrayList<UnitedRule>(exList.size());
    for (int i=0; i<exList.size(); i++)
      rules.add(getRule(exList.get(i)));
    return rules;
  }
  
  public void countRightAndWrongCoverages(ArrayList<CommonExplanation> exList) {
    nOrigRight=nOrigWrong=0;
    if (exList==null)
      return;
    for (int i=0; i<exList.size(); i++)
      if (subsumes(exList.get(i),false))
        if (exList.get(i).action==this.action)
          ++nOrigRight;
        else
          ++nOrigWrong;
  }
  
  public void countRightAndWrongCoveragesByQ(ArrayList<CommonExplanation> exList) {
    nOrigRight=nOrigWrong=0;
    if (exList==null)
      return;
    for (int i=0; i<exList.size(); i++)
      if (subsumes(exList.get(i),false)) {
        CommonExplanation ex=exList.get(i);
        if (ex.minQ>=minQ && ex.maxQ<=maxQ)
          ++nOrigRight;
        else
          ++nOrigWrong;
      }
  }
  
  
  public static CommonExplanation adjustToFeatureRanges(CommonExplanation r, Hashtable<String,float[]> attrMinMax) {
    if (attrMinMax==null || attrMinMax.isEmpty() || r.eItems==null || r.eItems.length<1)
      return r;
    ArrayList<ExplanationItem> items=null;
    for (int i=0; i<r.eItems.length; i++) {
      boolean lessThanMin=false, moreThanMax=false;
      float minMax[]=attrMinMax.get(r.eItems[i].attr);
      if (minMax!=null) {
        lessThanMin=r.eItems[i].interval[0]<minMax[0];
        moreThanMax=r.eItems[i].interval[1]>minMax[1];
      }
      if (lessThanMin || moreThanMax) {
        if (items==null) {
          items = new ArrayList<ExplanationItem>(r.eItems.length);
          for (int j=0; j<i; j++)
            items.add(r.eItems[j]);
        }
        if (lessThanMin && moreThanMax) //useless condition
          continue;
        ExplanationItem e=new ExplanationItem();
        e.attr=r.eItems[i].attr;
        double interval[]={(lessThanMin)?Double.NEGATIVE_INFINITY:r.eItems[i].interval[0],
            (moreThanMax)?Double.POSITIVE_INFINITY:r.eItems[i].interval[1]};
        e.interval=interval;
        e.isInteger=r.eItems[i].isInteger;
        e.sector=r.eItems[i].sector;
        e.value=r.eItems[i].value;
        e.attr_core=r.eItems[i].attr_core;
        e.level=r.eItems[i].level;
        e.attr_N=r.eItems[i].attr_N;
        items.add(e);
      }
      else
        if (items!=null)
          items.add(r.eItems[i]);
    }
    if (items==null || items.isEmpty())
      return r;
    CommonExplanation rule=(r instanceof UnitedRule)?new UnitedRule():new CommonExplanation();
    rule.action=r.action;
    rule.eItems=items.toArray(new ExplanationItem[items.size()]);
    rule.uses=r.uses;
    rule.nUses=r.nUses;
    rule.minQ=r.minQ;
    rule.maxQ=r.maxQ;
    rule.meanQ=r.meanQ;
    rule.sumQ=r.sumQ;
    if (r instanceof UnitedRule) {
      UnitedRule r0=(UnitedRule)r, r1=(UnitedRule)rule;
      r1.fromRules=r0.fromRules;
      r1.nOrigRight=r0.nOrigRight;
      r1.nOrigWrong=r0.nOrigWrong;
    }
    return rule;
  }
  
  public static UnitedRule unite(UnitedRule r1, UnitedRule r2) {
    return unite(r1,r2,null);
  }
  
  public static UnitedRule unite(UnitedRule r1, UnitedRule r2, Hashtable<String,float[]> attrMinMax) {
    if (r1==null || r2==null)
      return null;
    if (attrMinMax!=null) {
      r1=(UnitedRule)adjustToFeatureRanges(r1,attrMinMax);
      r2=(UnitedRule)adjustToFeatureRanges(r2,attrMinMax);
    }
    if (!sameFeatures(r1,r2))
      return null;
    ExplanationItem e1[]=r1.eItems, e2[]=r2.eItems, e[]=new ExplanationItem[Math.min(e1.length,e2.length)];
    int k=0;
    for (int i=0; i<e1.length; i++)
      for (int j=0; j<e2.length; j++)
        if (e1[i].attr.equals(e2[j].attr)) {
          double interval[]=uniteIntervals(e1[i].interval,e2[i].interval);
          if (interval!=null && (!Double.isInfinite(interval[0]) || !Double.isInfinite(interval[1]))) {
            e[k]=new ExplanationItem();
            e[k].attr=e1[i].attr;
            e[k].interval=interval;
            ++k;
          }
        }
    if (k<1)
      return null;
    if (k<e.length) {
      ExplanationItem ee[]=new ExplanationItem[k];
      for (int i=0; i<k; i++)
        ee[i]=e[i];
      e=ee;
    }
    UnitedRule rule=new UnitedRule();
    rule.action=r1.action;
    rule.eItems=e;
    if (r1.uses!=null || r1.uses!=null) {
      int n1=(r1.uses!=null)?r1.uses.size():0, n2=(r2.uses!=null)?r2.uses.size():0;
      rule.uses = new Hashtable<String, ArrayList<Explanation>>(n1+n2);
      if (n1>0)
        rule.uses.putAll(r1.uses);
      if (n2>0)
        rule.uses.putAll(r2.uses);
      if (rule.uses.size()<n1+n2)  //there are common uses of the original rules
        rule.nUses-=n1+n2-rule.uses.size();
    }
    rule.nUses=r1.nUses+r2.nUses;
    
    rule.minQ=Math.min(r1.minQ,r2.minQ);
    rule.maxQ=Math.max(r1.maxQ,r2.maxQ);
    rule.sumQ=r1.sumQ+r2.sumQ;
    rule.meanQ=(float)rule.sumQ/rule.nUses;
    
    rule.fromRules=new ArrayList<UnitedRule>(10);
    rule.fromRules.add(r1);
    rule.fromRules.add(r2);
    return rule;
  }
  
  public static double[] uniteIntervals(double i1[], double i2[]) {
    if (i1==null)
      return i2;
    if (i2==null)
      return i1;
    double i[]={Math.min(i1[0],i2[0]),Math.max(i1[1],i2[1])};
    return i;
  }
  
  public static double intervalDistance(double a1, double a2, double b1, double b2,
                                        float minMax[]) {
    if (Double.isNaN(a1) || Double.isInfinite(a1))
      a1=(minMax==null)?Integer.MIN_VALUE:minMax[0];
    if (Double.isNaN(a2) || Double.isInfinite(a2))
      a2=(minMax==null)?Integer.MAX_VALUE:minMax[1];
    if (Double.isNaN(b1) || Double.isInfinite(b1))
      b1=(minMax==null)?Integer.MIN_VALUE:minMax[0];
    if (Double.isNaN(b2) || Double.isInfinite(b2))
      b2=(minMax==null)?Integer.MAX_VALUE:minMax[1];
    double da1b1=Math.abs(a1-b1),
        da2b2=Math.abs(a2-b2),
        da1b2=Math.max(a2,b2)-Math.min(a1,b1);
    return (da1b1+da2b2)/da1b2;
  }
  
  public static double distance(ExplanationItem e1[], ExplanationItem e2[],
                                Hashtable<String,float[]> attrMinMax) {
    if (e1==null || e1.length<1)
      if (e2==null) return 0; else return e2.length;
    if (e2==null || e2.length<1)
      return e1.length;
    double d=e1.length+e2.length;
    for (int i=0; i<e1.length; i++) {
      float aMinMax[]=(attrMinMax==null)?null:attrMinMax.get(e1[i].attr);
      int i2 = -1;
      for (int j = 0; j < e2.length && i2 < 0; j++)
        if (e1[i].attr.equals(e2[j].attr))
          i2 = j;
      if (i2 < 0)
        continue;
      d -= 2; //corresponding items found
      d+=intervalDistance(e1[i].interval[0],e1[i].interval[1],
          e2[i2].interval[0],e2[i2].interval[1],aMinMax);
    }
    return d;
  }
  
  public static double distance(UnitedRule r1, UnitedRule r2,
                                Hashtable<String,float[]> attrMinMax) {
    if (r1==null)
      return (r2==null)?0:r2.eItems.length;
    if (r2==null)
      return r1.eItems.length;
    return distance(r1.eItems,r2.eItems,attrMinMax);
  }
  
  public int countFromRules() {
    if (fromRules==null || fromRules.isEmpty())
      return 0;
    int n=0;
    for (int i=0; i<fromRules.size(); i++)
      n+=1+fromRules.get(i).countFromRules();
    return n;
  }
  
  public static int countRulesInHierarchy(ArrayList<UnitedRule> rules) {
    if (rules==null || rules.isEmpty())
      return 0;
    int n=0;
    for (int i=0; i<rules.size(); i++)
      n+=1+rules.get(i).countFromRules();
    return n;
  }
  
  public int getHierarchyDepth(){
    if (fromRules==null || fromRules.isEmpty())
      return 1;
    int maxD=1;
    for (int i=0; i<fromRules.size(); i++) {
      int d=fromRules.get(i).getHierarchyDepth();
      if (maxD<d)
        maxD=d;
    }
    return 1+maxD;
  }
  
  public static int getMaxHierarchyDepth(ArrayList<UnitedRule> rules) {
    if (rules==null || rules.isEmpty())
      return 0;
    int maxD=1;
    for (int i=0; i<rules.size(); i++) {
      int d=rules.get(i).getHierarchyDepth();
      if (maxD<d)
        maxD=d;
    }
    return maxD;
  }
}
