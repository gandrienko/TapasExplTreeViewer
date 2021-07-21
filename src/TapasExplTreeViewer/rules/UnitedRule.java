package TapasExplTreeViewer.rules;

import TapasDataReader.CommonExplanation;

import java.util.ArrayList;

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
    if (exList==null)
      return;
    nOrigRight=nOrigWrong=0;
    for (int i=0; i<exList.size(); i++)
      if (subsumes(exList.get(i),false))
        if (exList.get(i).action==this.action)
          ++nOrigRight;
        else
          ++nOrigWrong;
  }
}
