package TapasExplTreeViewer;
import TapasDataReader.CommonExplanation;
import TapasDataReader.ExplanationItem;
import TapasExplTreeViewer.alt_rules.Condition;
import TapasExplTreeViewer.alt_rules.Rule;
import TapasExplTreeViewer.ui.ShowRules;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class SeeRules {
  public static String pathToRules="c:\\CommonGISprojects\\Lamarr\\model_rules\\";
  public static String lastUsedDirectory = null;

  public static void main(String[] args) {
    File file=null;
    if (args!=null && args.length>0) {
      String csvFile = args[0];
      file =new File(csvFile);
    }
    else {
      // Select file with rules
      JFileChooser fileChooser = new JFileChooser();
      fileChooser.setDialogTitle("Specify a file with rules");
      if (pathToRules!=null)
        fileChooser.setCurrentDirectory(new File(pathToRules));
      else {
        String workingDirectory = System.getProperty("user.dir");
        if (workingDirectory != null)
          fileChooser.setCurrentDirectory(new File(workingDirectory));
      }
      FileNameExtensionFilter filter = new FileNameExtensionFilter("CSV Files", "csv");
      fileChooser.setFileFilter(filter);
      int userSelection = fileChooser.showOpenDialog(null);

      if (userSelection == JFileChooser.APPROVE_OPTION) {
        file = fileChooser.getSelectedFile();
        lastUsedDirectory = file.getParent();
      }
    }
    if (file==null)
      return;

    List<Rule> rules = new ArrayList<>();
    int maxNConditions=0;

    String featureType=null;
    if (args.length>1 && args[1].startsWith("feature_type="))
      featureType=args[1].substring(13).toLowerCase();

    if (featureType==null)
      featureType=askFeatureType();

    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
      String line=null;
      String fieldNames[]=null;
      boolean realValued=false;
      int fNRuleId=-1, fNTreeId=-1, fNCluster=-1, fNResult=-1, fNWeight=-1, fNRuleBody=-1;

      while ((line = br.readLine()) != null) {
        if (line==null || line.trim().length()<2)
          continue;
        String[] fields = line.split(",");
        if (fields==null || fields.length<2)
          continue;
        if (fieldNames==null) {
          fieldNames=fields;
          for (int i=0; i<fieldNames.length; i++) {
            fieldNames[i] = fieldNames[i].toLowerCase();
            if (fieldNames[i].contains("tree"))
              fNTreeId=i;
            else
            if (fieldNames[i].contains("cluster"))
              fNCluster=i;
            else
            if (fieldNames[i].contains("weight"))
              fNWeight=i;
            else
            if (fieldNames[i].contains("rule"))
              if (fieldNames[i].contains("id"))
                fNRuleId=i;
              else
                fNRuleBody=i;
          }
          if (fNRuleBody<0) {
            System.out.println("No field containing the rule body has been found!");
            return;
          }

          fNResult=fieldNames.length-1; //we assume that the predicted value or class is in the last field
          String predictionType=fieldNames[fNResult];
          realValued=predictionType.contains("num") || predictionType.contains("value");
        }
        else {
          int ruleId=1+rules.size();
          if (fNRuleId>=0)
            ruleId=Integer.parseInt(fields[fNRuleId]);
          String ruleText=fields[fNRuleBody];
          int predictedClass=(realValued)?-1:Integer.parseInt(fields[fNResult]);
          double predictedValue=(realValued)?Double.parseDouble(fields[fNResult]):Double.NaN;
  
          List<Condition> conditions=parseConditions(ruleText);
          if (conditions==null || conditions.isEmpty())
            continue;
          if (maxNConditions<conditions.size())
            maxNConditions=conditions.size();
          Rule r=(realValued)?new Rule(ruleId, conditions, predictedValue):
                              new Rule(ruleId, conditions, predictedClass);
          if (fNTreeId>=0)
            r.treeId=Integer.parseInt(fields[fNTreeId]);
          if (fNCluster>=0)
            r.treeCluster =Integer.parseInt(fields[fNCluster]);
          if (fNWeight>=0)
            r.weight=Integer.parseInt(fields[fNWeight]);
          int idx=rules.indexOf(r);
          if (idx<0)
            rules.add(r);
          else {
            ++rules.get(idx).nSame;
            ++rules.get(idx).weight;
          }
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (rules.isEmpty()) {
      System.out.println("No rules found!");
      return;
    }

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
          else
          if (featureType.startsWith("bin")) {
            if (Float.isInfinite(min)) {
              min=0; cnd.setMinValue(0); cnd.setMaxValue(0);
            }
            else
            if (Float.isInfinite(max)) {
              max=1; cnd.setMinValue(1); cnd.setMaxValue(1);
            }
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
          if (!Float.isInfinite(min) && (Float.isInfinite(minmax[0]) || minmax[0]>min))
            minmax[0]=min;
          if (!Float.isInfinite(max) && (Float.isInfinite(minmax[1]) || minmax[1]<max))
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
      ex.treeId=rule.treeId;
      ex.treeCluster=rule.treeCluster;
      ex.nSame=rule.nSame;
      ex.weight=Math.max(rule.nSame,rule.weight);
      if (!Double.isNaN(rule.getPredictedValue())) {
        ex.minQ = ex.maxQ = ex.meanQ = (float) rule.getPredictedValue();
        ex.sumQ=rule.getPredictedValue();
      }
      ex.action=rule.getPredictedClass();
      ex.eItems=new ExplanationItem[rule.getConditionCount()];
      int i=0;
      for (Condition cnd:rule.getConditions()) {
        ex.eItems[i]=new ExplanationItem();
        ex.eItems[i].attr=cnd.getFeature();
        ex.eItems[i].interval[0]=cnd.getMinValue();
        ex.eItems[i].interval[1]=cnd.getMaxValue();
        ex.eItems[i].isInteger=intOrBin;
        if (intOrBin) {
          if (!Double.isInfinite(ex.eItems[i].interval[0]))
            ex.eItems[i].interval[0]=Math.ceil(ex.eItems[i].interval[0]);
          if (!Double.isInfinite(ex.eItems[i].interval[1]))
            ex.eItems[i].interval[1]=Math.floor(ex.eItems[i].interval[1]);
        }
        ++i;
      }
      ex.nUses=1;
      exList.add(ex);
    }

    ShowRules.RULES_FOLDER=file.getParent();
    ArrayList<String> orderedFeatureNames=loadFeatureOrder(ShowRules.RULES_FOLDER);
  
    ShowRules showRules=new ShowRules(exList,attrMinMax);
    showRules.setOrigRules(exList);
    if (orderedFeatureNames!=null)
      showRules.setOrderedFeatureNames(orderedFeatureNames);
    showRules.dataFolder=ShowRules.RULES_FOLDER;
    JFrame fr=showRules.showRulesInTable(null);
    if (fr==null) {
      System.out.println("Failed to visualize the rules!");
      System.exit(1);
    }
  }

  public static String askFeatureType () {
    JRadioButton binaryButton = new JRadioButton("binary");
    JRadioButton integerButton = new JRadioButton("integer");
    JRadioButton realValuedButton = new JRadioButton("real-valued");

    // Group the radio buttons
    ButtonGroup group = new ButtonGroup();
    group.add(binaryButton);
    group.add(integerButton);
    group.add(realValuedButton);

    // Set default selection
    integerButton.setSelected(true);

    // Create a panel to hold the radio buttons
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.add(new JLabel("Select a feature type:"));
    panel.add(binaryButton);
    panel.add(integerButton);
    panel.add(realValuedButton);

    // Show the dialog with the radio buttons
    int result = JOptionPane.showConfirmDialog(null, panel, "Feature Type Selection",
        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

    // Handle the user's selection
    if (result == JOptionPane.OK_OPTION) {
      String selectedFeatureType = null;
      if (binaryButton.isSelected()) {
        selectedFeatureType = "binary";
      } else if (integerButton.isSelected()) {
        selectedFeatureType = "integer";
      } else if (realValuedButton.isSelected()) {
        selectedFeatureType = "real-valued";
      }
      return selectedFeatureType;
    }
    return null;
  }
  /**
   * Searches for the file "features_order.txt" in the given folder and reads the feature names.
   *
   * @param folderPath The path to the folder where the file is searched.
   * @return A list of feature names if the file is found and successfully read; otherwise, an empty list.
   */
  public static ArrayList<String> loadFeatureOrder(String folderPath) {
    ArrayList<String> featureNames = new ArrayList<String>(50);
    File file = new File(folderPath, "features_order.txt");
    
    if (file.exists() && file.isFile()) {
      try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
        String line;
        while ((line = reader.readLine()) != null) {
          line = line.trim();
          if (!line.isEmpty()) {
            featureNames.add(line);
          }
        }
      } catch (IOException e) {
        System.err.println("Error reading the file: " + e.getMessage());
      }
    } else {
      System.out.println("File 'features_order.txt' not found in the specified folder.");
    }
    
    return featureNames;
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

      float minValue = minStr.equals("-inf") ? Float.NEGATIVE_INFINITY : Float.parseFloat(minStr);
      float maxValue = maxStr.equals("inf") ? Float.POSITIVE_INFINITY : Float.parseFloat(maxStr);
      if (Float.isInfinite(minValue) && Float.isInfinite(maxValue))
        continue; //this condition is excessive

      Condition c=new Condition(feature, minValue, maxValue);
      if (!conditions.contains(c))
        conditions.add(c);
    }

    return conditions;
  }
}
