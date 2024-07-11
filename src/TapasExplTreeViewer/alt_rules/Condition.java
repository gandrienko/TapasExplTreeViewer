package TapasExplTreeViewer.alt_rules;

public class Condition {
  private String feature=null;
  private float minValue=Float.NEGATIVE_INFINITY;
  private float maxValue=Float.POSITIVE_INFINITY;

  public Condition(String feature, float minValue, float maxValue) {
    this.feature = feature;
    this.minValue = minValue;
    this.maxValue = maxValue;
  }

  // Getters and toString() method for better readability
  public String getFeature() {
    return feature;
  }

  public float getMinValue() {
    return minValue;
  }

  public float getMaxValue() {
    return maxValue;
  }

  @Override
  public String toString() {
    return feature + ": {" + minValue + ": " + maxValue + "}";
  }
}
