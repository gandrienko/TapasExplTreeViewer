package TapasExplTreeViewer.vis;

import TapasDataReader.CommonExplanation;
import TapasExplTreeViewer.rules.RuleMaster;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;

public class EnsembleExplorer {
  public ArrayList<CommonExplanation> rules=null;
  public String rulesInfoText=null;
  public Hashtable<String,float[]> attrMinMaxValues=null;
  public HashSet<String> featuresToUse=null;
  public int treeIds[]=null;
  public double treeDistances[][]=null;
  public JPanel uiPanel=null, mainPanel=null;
  
  public JPanel startEnsembleExplorer (ArrayList<CommonExplanation> rules, String rulesInfoText,
                           Hashtable<String,float[]> attrMinMaxValues, HashSet<String> featuresToUse) {
    if (rules==null || rules.size()<5)
      return null;
    this.rules=rules; this.rulesInfoText=rulesInfoText;
    this.attrMinMaxValues=attrMinMaxValues; this.featuresToUse=featuresToUse;
    
    treeIds=RuleMaster.getAllTreeIds(rules);
    
    if (treeIds==null || treeIds.length<2) {
      JOptionPane.showMessageDialog(FocusManager.getCurrentManager().getActiveWindow(),
          "There are no distinct tree identifiers in the rule set!","No distinct trees!",
          JOptionPane.INFORMATION_MESSAGE);
      return null;
    }
    if (treeIds.length<5) {
      JOptionPane.showMessageDialog(FocusManager.getCurrentManager().getActiveWindow(),
          "There are only "+treeIds.length+" distinct tree identifiers in the rule set!",
          "Too few distinct trees!",
          JOptionPane.INFORMATION_MESSAGE);
      return null;
    }
    uiPanel=new JPanel();
    uiPanel.setLayout(new BorderLayout());
    JTextArea textArea=new JTextArea((rulesInfoText!=null && rulesInfoText.length()>5)?rulesInfoText:
                                         "Set of "+rules.size()+" rules");
    textArea.setLineWrap(true);
    textArea.setWrapStyleWord(true);
    textArea.append("\nNumber of distinct trees: "+treeIds.length);
    if (featuresToUse!=null && !featuresToUse.isEmpty()) {
      textArea.append("\n"+featuresToUse.size()+" features are used in computing distances: ");
      int n=0;
      for (String featureName:featuresToUse) {
        textArea.append((n==0)?featureName:"; "+featureName);
        ++n;
      }
    }
    uiPanel.add(textArea,BorderLayout.NORTH);
    mainPanel=new JPanel();
    mainPanel.setLayout(new BorderLayout());
    mainPanel.add(new JLabel("Computing distances between the trees ...",JLabel.CENTER),BorderLayout.CENTER);
    System.out.println("Computing distances between the trees in background mode...");
    uiPanel.add(mainPanel,BorderLayout.CENTER);
    
    SwingWorker worker=new SwingWorker() {
      @Override
      protected Object doInBackground() throws Exception {
        treeDistances=RuleMaster.computeDistancesBetweenTrees(rules,treeIds,featuresToUse,attrMinMaxValues);
        return null;
      }
      @Override
      protected void done() {
        mainPanel.removeAll();
        if (treeDistances==null) {
          mainPanel.add(new JLabel("Failed to compute distances between the trees!!!",JLabel.CENTER),
              BorderLayout.CENTER);
          System.out.println("Failed to compute distances between the trees!!!");
        }
        else {
          //mainPanel.add(new JLabel("Successfully computed the distances between the trees!!!",JLabel.CENTER),
              //BorderLayout.CENTER);
          System.out.println("Successfully computed the distances between the trees!!!");
          MatrixPainter matrixPainter=new MatrixPainter(treeDistances);
          mainPanel.add(matrixPainter,BorderLayout.CENTER);
        }
        mainPanel.invalidate();
        mainPanel.validate();
      };
    };
    worker.execute();
    
    return uiPanel;
  }
}
