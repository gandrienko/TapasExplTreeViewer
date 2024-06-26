package TapasExplTreeViewer.alt_rules;

public class Condition {
  private String feature=null;
  private double minValue=Double.NEGATIVE_INFINITY;
  private double maxValue=Double.POSITIVE_INFINITY;

  public Condition(String feature, double minValue, double maxValue) {
    this.feature = feature;
    this.minValue = minValue;
    this.maxValue = maxValue;
  }

  // Getters and toString() method for better readability
  public String getFeature() {
    return feature;
  }

  public double getMinValue() {
    return minValue;
  }

  public double getMaxValue() {
    return maxValue;
  }

  @Override
  public String toString() {
    return feature + ": {" + minValue + ": " + maxValue + "}";
  }
}
