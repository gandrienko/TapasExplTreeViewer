package TapasExplTreeViewer;
import TapasExplTreeViewer.alt_rules.Condition;
import TapasExplTreeViewer.alt_rules.Rule;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SeeRules {
  public static void main(String[] args) {
    String csvFile = args[0];
    List<Rule> rules = new ArrayList<>();

    try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
      String line;
      br.readLine(); // Skip header line

      while ((line = br.readLine()) != null) {
        String[] fields = line.split(",", 3); // Split line into 3 parts: RuleID, Rule, Class
        int ruleId = Integer.parseInt(fields[0]);
        String ruleText = fields[1];
        int predictedClass = Integer.parseInt(fields[2]);

        List<Condition> conditions = parseConditions(ruleText);
        rules.add(new Rule(ruleId, conditions, predictedClass));
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    // Output the rules for verification
    for (Rule rule : rules) {
      System.out.println(rule);
    }
  }

  private static List<Condition> parseConditions(String ruleText) {
    List<Condition> conditions = new ArrayList<>();
    String[] parts = ruleText.split("; ");

    for (String part : parts) {
      String[] conditionParts = part.split(": ");
      String feature = conditionParts[0];
      String minStr=conditionParts[1].replace("{", "");
      String maxStr=conditionParts[2].replace("}", "");

      double minValue = minStr.equals("-inf") ? Double.NEGATIVE_INFINITY : Double.parseDouble(minStr);
      double maxValue = maxStr.equals("inf") ? Double.POSITIVE_INFINITY : Double.parseDouble(maxStr);

      conditions.add(new Condition(feature, minValue, maxValue));
    }

    return conditions;
  }
}
