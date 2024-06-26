package TapasExplTreeViewer.alt_rules;

import java.util.List;

public class Rule {
  private int id=0;
  private List<Condition> conditions=null;
  private String predictedClass=null;
  private double predictedValue=Double.NaN;

  public Rule(int id, List<Condition> conditions, String predictedClass) {
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

  public String getPredictedClass() {
    return predictedClass;
  }

  @Override
  public String toString() {
    return "RuleID: " + id + ", Conditions: " + conditions +
               ((Double.isNaN(predictedValue))?", Class: " + predictedClass:", Value: "+predictedValue);
  }
}
