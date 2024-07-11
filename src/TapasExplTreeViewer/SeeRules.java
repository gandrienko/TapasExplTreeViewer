package TapasExplTreeViewer;
import TapasDataReader.CommonExplanation;
import TapasDataReader.Explanation;
import TapasDataReader.ExplanationItem;
import TapasExplTreeViewer.alt_rules.Condition;
import TapasExplTreeViewer.alt_rules.Rule;
import TapasExplTreeViewer.ui.ShowRules;

import javax.swing.*;
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

    String featureType=null;
    if (args.length>1 && args[1].startsWith("feature_type="))
      featureType=args[1].substring(13).toLowerCase();

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
  
          List<Condition> conditions=parseConditions(ruleText,featureType);
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
    Hashtable<String,float[]> attrMinMax=new Hashtable<String,float[]>(100);
    for (Rule rule:rules) {
      for (Condition cnd:rule.getConditions()) {
        String feature=cnd.getFeature();
        Integer count=features.get(feature);
        if (count==null) count=0;
        features.put(feature,count+1);
        float min=cnd.getMinValue(), max=cnd.getMaxValue();
        if (featureType!=null) {
          if (featureType.startsWith("int")) {
            if (Float.isInfinite(min))
              min=(int)Math.floor(max);
            if (Float.isInfinite(max))
              max=(int)Math.ceil(min);
          }
        }

        float minmax[]=attrMinMax.get(feature);
        if (minmax==null) {
          minmax=new float[2];
          minmax[0]=min;
          minmax[1]=max;
          attrMinMax.put(feature,minmax);
        }
        else {
          if (!Float.isInfinite(minmax[0]) && minmax[0]>min)
            minmax[0]=min;
          if (!Float.isInfinite(minmax[1]) && minmax[1]<max)
            minmax[1]=max;
        }
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
      float minmax[]=attrMinMax.get(entry.getKey());
      System.out.println(entry.getKey() + ": frequency = " + entry.getValue()+"; values = "+minmax[0]+".."+minmax[1]);
    }

    ArrayList<CommonExplanation> exList=new ArrayList<CommonExplanation>(rules.size());
    boolean intOrBin=featureType!=null && (featureType.startsWith("int") || featureType.startsWith("bin"));

    for (Rule rule:rules) {
      CommonExplanation ex=new CommonExplanation();
      ex.numId=rule.getId();
      if (!Double.isNaN(rule.getPredictedValue()))
        ex.minQ=ex.maxQ=ex.meanQ=(float)rule.getPredictedValue();
      ex.action=rule.getPredictedClass();
      ex.eItems=new ExplanationItem[rule.getConditionCount()];
      int i=0;
      for (Condition cnd:rule.getConditions()) {
        ex.eItems[i]=new ExplanationItem();
        ex.eItems[i].attr=cnd.getFeature();
        ex.eItems[i].interval[0]=cnd.getMinValue();
        ex.eItems[i].interval[1]=cnd.getMaxValue();
        ex.eItems[i].isInteger=intOrBin;
        ++i;
      }
      exList.add(ex);
    }

    ShowRules showRules=new ShowRules(exList,attrMinMax);
    showRules.setOrigRules(exList);
    JFrame fr=showRules.showRulesInTable();
    if (fr==null) {
      System.out.println("Failed to visualize the rules!");
      System.exit(1);
    }
  }

  private static List<Condition> parseConditions(String ruleText, String featureType) {
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

      float minValue = minStr.equals("-inf") ? Float.NEGATIVE_INFINITY : Float.parseFloat(minStr);
      float maxValue = maxStr.equals("inf") ? Float.POSITIVE_INFINITY : Float.parseFloat(maxStr);
      if (Float.isInfinite(minValue) && Float.isInfinite(maxValue))
        continue; //this condition is excessive

      if (featureType!=null) {
        if (featureType.startsWith("bin"))  {
          if (Float.isInfinite(minValue))
            minValue=0;
          else
            if (minValue>0) minValue=1;
          if (Float.isInfinite(maxValue))
            maxValue=1;
          else
            if (maxValue<1)
              maxValue=0;
        }
      }

      conditions.add(new Condition(feature, minValue, maxValue));
    }

    return conditions;
  }
}
