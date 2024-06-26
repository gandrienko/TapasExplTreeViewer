package TapasExplTreeViewer;
import TapasExplTreeViewer.alt_rules.Condition;
import TapasExplTreeViewer.alt_rules.Rule;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class SeeRules {
  public static void main(String[] args) {
    String csvFile = args[0];
    List<Rule> rules = new ArrayList<>();
    int maxNConditions=0;

    try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
      String line=null;
      String fieldNames[]=null;
      boolean realValued=false;

      while ((line = br.readLine()) != null) {
        if (line==null || line.trim().length()<3)
          continue;
        String[] fields = line.split(","); // Split line into 3 parts: RuleID, Rule, Class
        if (fields==null || fields.length<3)
          continue;
        if (fieldNames==null) {
          fieldNames=fields;
          String predictionType=fieldNames[2].toLowerCase();
          realValued=predictionType.contains("num") || predictionType.contains("value");
        }
        else {
          int ruleId=Integer.parseInt(fields[0]);
          String ruleText=fields[1];
          int predictedClass=(realValued)?-1:Integer.parseInt(fields[2]);
          double predictedValue=(realValued)?Double.parseDouble(fields[2]):Double.NaN;
  
          List<Condition> conditions=parseConditions(ruleText);
          if (conditions==null || conditions.isEmpty())
            continue;
          if (maxNConditions<conditions.size())
            maxNConditions=conditions.size();
          if (realValued)
            rules.add(new Rule(ruleId, conditions, predictedValue));
          else
            rules.add(new Rule(ruleId, conditions, predictedClass));
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (rules.isEmpty()) {
      System.out.println("No rules found!");
      return;
    }

    /*
    // Output the rules for verification
    for (Rule rule : rules) {
      System.out.println(rule);
    }
    */
    Hashtable<String,Integer> features=new Hashtable<String,Integer>(100);
    for (Rule rule:rules) {
      for (Condition cnd:rule.getConditions()) {
        String feature=cnd.getFeature();
        Integer count=features.get(feature);
        if (count==null) count=0;
        features.put(feature,count+1);
      }
    }
    System.out.println("Got "+rules.size()+" rules with "+features.size()+
                           " distinct features; max N of conditions in a rule = "+maxNConditions);
    ArrayList<Map.Entry<String,Integer>> fList=new ArrayList<Map.Entry<String,Integer>>(features.size());
    fList.addAll(features.entrySet());
    // Sort the list in decreasing order of frequency
    fList.sort((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()));
  
    // Print sorted feature frequencies
    for (Map.Entry<String, Integer> entry : fList) {
      System.out.println(entry.getKey() + ": " + entry.getValue());
    }
  }

  private static List<Condition> parseConditions(String ruleText) {
    if (ruleText==null)
      return null;
    String[] parts = ruleText.split(";");
    if (parts==null || parts.length<1)
      return null;
    List<Condition> conditions = new ArrayList<>();

    for (String part : parts) {
      String[] conditionParts = part.trim().split(":");
      String feature = conditionParts[0].trim();
      String minStr=conditionParts[1].replace("{", "").trim();
      String maxStr=conditionParts[2].replace("}", "").trim();

      double minValue = minStr.equals("-inf") ? Double.NEGATIVE_INFINITY : Double.parseDouble(minStr);
      double maxValue = maxStr.equals("inf") ? Double.POSITIVE_INFINITY : Double.parseDouble(maxStr);

      conditions.add(new Condition(feature, minValue, maxValue));
    }

    return conditions;
  }
}
