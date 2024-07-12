package TapasExplTreeViewer.alt_rules;

import java.util.List;

public class Rule {
  private int id=0;
  private List<Condition> conditions=null;
  private int predictedClass=-1;
  private double predictedValue=Double.NaN;
  /**
   * Number of duplicates of this rule (may occur in a decision forest)
   */
  public int nSame=1;

  public Rule(int id, List<Condition> conditions, int predictedClass) {
    this.id = id;
    this.conditions = conditions;
    this.predictedClass = predictedClass;
  }
  
  public Rule(int id, List<Condition> conditions, double predictedValue) {
    this.id = id;
    this.conditions = conditions;
    this.predictedValue = predictedValue;
  }

  // Getters and toString() method for better readability
  public int getId() {
    return id;
  }

  public List<Condition> getConditions() {
    return conditions;
  }

  public int getConditionCount(){ return conditions.size(); }

  public int getPredictedClass() {
    return predictedClass;
  }

  public double getPredictedValue() {
    return predictedValue;
  }

  @Override
  public String toString() {
    return "RuleID: " + id + ", Conditions: " + conditions +
               ((Double.isNaN(predictedValue))?", Class: " + predictedClass:", Value: "+predictedValue);
  }

  public boolean hasCondition(Condition c) {
    if (c==null || conditions==null || conditions.isEmpty())
      return false;
    return conditions.contains(c);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj==null || !(obj instanceof Rule))
      return false;
    Rule r=(Rule)obj;
    if (r.predictedClass!=predictedClass)
      return false;
    if (Double.isNaN(predictedValue))
      if (!Double.isNaN(r.getPredictedValue()))
        return false;
      else;
    else
    if (r.predictedValue!=predictedValue)
      return false;
    if (r.getConditionCount()!=getConditionCount())
      return false;
    for (Condition c:conditions)
      if (!r.hasCondition(c))
        return false;
    return true;
  }
}
