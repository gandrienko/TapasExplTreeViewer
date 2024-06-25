package TapasExplTreeViewer.alt_rules;

import java.util.List;

public class Rule {
  private int id;
  private List<Condition> conditions;
  private int predictedClass;

  public Rule(int id, List<Condition> conditions, int predictedClass) {
    this.id = id;
    this.conditions = conditions;
    this.predictedClass = predictedClass;
  }

  // Getters and toString() method for better readability
  public int getId() {
    return id;
  }

  public List<Condition> getConditions() {
    return conditions;
  }

  public int getPredictedClass() {
    return predictedClass;
  }

  @Override
  public String toString() {
    return "RuleID: " + id + ", Conditions: " + conditions + ", Class: " + predictedClass;
  }
}
