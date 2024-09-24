package TapasExplTreeViewer.ui;

import TapasDataReader.CommonExplanation;
import TapasExplTreeViewer.rules.RuleMaster;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;

public class RuleSelector {
  protected ArrayList<CommonExplanation> origRules =null, selectedRules=null;
  protected ChangeListener changeListener=null;
  protected JDialog queryDialog=null;
  protected boolean isRunning=false, queryExecuting=false;
  public String queryStr=null;

  public boolean makeQueryInterface(ArrayList<CommonExplanation> rules,
                                    ChangeListener changeListener) {
    if (rules==null || rules.isEmpty())
      return false;
    this.origRules =rules; this.changeListener=changeListener;
    
    int minClass=-1, maxClass=-1, minTreeId=-1, maxTreeId=-1, minTreeCluster=-1, maxTreeCluster=-1;
    double minValue=Double.NaN, maxValue=Double.NaN;
    
    for (CommonExplanation r: rules) {
      if (r.action>=0) {
        if (minClass<0 || minClass>r.action) minClass=r.action;
        if (maxClass<r.action) maxClass=r.action;
      }
      if (!Double.isNaN(r.minQ)) {
        if (Double.isNaN(minValue) || minValue>r.minQ)
          minValue=r.minQ;
        if (Double.isNaN(maxValue) || maxValue<r.minQ)
          maxValue=r.minQ;
      }
      if (!Double.isNaN(r.maxQ) && r.maxQ>r.minQ) {
        if (Double.isNaN(maxValue) || maxValue<r.maxQ)
          maxValue=r.maxQ;
      }
      if (r.treeId>=0) {
        if (minTreeId<0 || minTreeId>r.treeId)
          minTreeId=r.treeId;
        if (maxTreeId<0 || maxTreeId<r.treeId)
          maxTreeId=r.treeId;
      }
      if (r.treeCluster>=0) {
        if (minTreeCluster<0 || minTreeCluster>r.treeCluster)
          minTreeCluster=r.treeCluster;
        if (maxTreeCluster<0 || maxTreeCluster<r.treeCluster)
          maxTreeCluster=r.treeCluster;
      }
    }
    JPanel mainP=new JPanel();
    mainP.setLayout(new GridLayout(0,1));
    JCheckBox cb[]=new JCheckBox[4];
    JTextField tfMin[]=new JTextField[4], tfMax[]=new JTextField[4];
    for (int i=0; i<4; i++) {
      cb[i]=null; tfMin[i]=tfMax[i]=null;
    }

    if (minClass<maxClass) {
      JPanel p=new JPanel();
      p.setLayout(new FlowLayout(FlowLayout.LEFT));
      cb[0]=new JCheckBox("Predicted class: from");
      p.add(cb[0]);
      tfMin[0]=new JTextField(Integer.toString(minClass),2);
      p.add(tfMin[0]);
      p.add(new JLabel("to",JLabel.RIGHT));
      tfMax[0]=new JTextField(Integer.toString(maxClass),2);
      p.add(tfMax[0]);
      mainP.add(p);
    }
    if (minValue<maxValue) {
      JPanel p=new JPanel();
      p.setLayout(new FlowLayout(FlowLayout.LEFT));
      cb[1]=new JCheckBox("Predicted value: from");
      p.add(cb[1]);
      tfMin[1]=new JTextField(String.format("%.5f",minValue),10);
      p.add(tfMin[1]);
      p.add(new JLabel("to",JLabel.RIGHT));
      tfMax[1]=new JTextField(String.format("%.5f",maxValue),10);
      p.add(tfMax[1]);
      mainP.add(p);
    }
    if (minTreeId<maxTreeId) {
      JPanel p=new JPanel();
      p.setLayout(new FlowLayout(FlowLayout.LEFT));
      cb[2]=new JCheckBox("Tree identifier: from");
      p.add(cb[2]);
      tfMin[2]=new JTextField(Integer.toString(minTreeId),2);
      p.add(tfMin[2]);
      p.add(new JLabel("to",JLabel.RIGHT));
      tfMax[2]=new JTextField(Integer.toString(maxTreeId),2);
      p.add(tfMax[2]);
      mainP.add(p);
    }
    if (minTreeCluster<maxTreeCluster) {
      JPanel p=new JPanel();
      p.setLayout(new FlowLayout(FlowLayout.LEFT));
      cb[3]=new JCheckBox("Tree cluster: from");
      p.add(cb[3]);
      tfMin[3]=new JTextField(Integer.toString(minTreeCluster),2);
      p.add(tfMin[3]);
      p.add(new JLabel("to",JLabel.RIGHT));
      tfMax[3]=new JTextField(Integer.toString(maxTreeCluster),2);
      p.add(tfMax[3]);
      mainP.add(p);
    }

    if (mainP.getComponentCount()<1)
      return false;

    JPanel bp=new JPanel();
    bp.setLayout(new FlowLayout(FlowLayout.CENTER,10,5));
    JButton b=new JButton("Select");
    bp.add(b);

    Window owner=FocusManager.getCurrentManager().getActiveWindow();
    queryDialog=new JDialog(owner, "Select rules", Dialog.ModalityType.MODELESS);
    queryDialog.setLayout(new BorderLayout());
    queryDialog.getContentPane().add(mainP, BorderLayout.CENTER);
    queryDialog.getContentPane().add(new JLabel("Set conditions for selecting rules:",JLabel.CENTER),
        BorderLayout.NORTH);
    queryDialog.getContentPane().add(bp,BorderLayout.SOUTH);
    queryDialog.pack();
    queryDialog.setLocationRelativeTo(owner);
    queryDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    queryDialog.setVisible(true);
    isRunning=true;

    b.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (queryExecuting) {
          JOptionPane.showMessageDialog(FocusManager.getCurrentManager().getActiveWindow(),
              "The previous query is still being executed!",
              "Wait...",JOptionPane.WARNING_MESSAGE);
        }
        int minClass=-1, maxClass=-1, minTreeId=-1, maxTreeId=-1, minTreeCluster=-1, maxTreeCluster=-1;
        double minValue=Double.NaN, maxValue=Double.NaN;
        queryStr="";
        if (cb[0]!=null && cb[0].isSelected()) {
          try {
            minClass=Integer.parseInt(tfMin[0].getText());
          } catch (Exception ex) {}
          try {
            maxClass=Integer.parseInt(tfMax[0].getText());
          } catch (Exception ex) {}
          if (minClass>=0 && maxClass>=minClass)
            queryStr+="Class in ["+minClass+","+maxClass+"]; ";
        }
        if (cb[1]!=null && cb[1].isSelected()) {
          try {
            minValue=Double.parseDouble(tfMin[1].getText());
          } catch (Exception ex) {}
          try {
            maxValue=Double.parseDouble(tfMax[1].getText());
          } catch (Exception ex) {}
          if (!Double.isNaN(minValue) && maxValue>=minValue)
            queryStr+="Value in ["+minValue+","+maxValue+"]; ";
        }
        if (cb[2]!=null && cb[2].isSelected()) {
          try {
            minTreeId=Integer.parseInt(tfMin[2].getText());
          } catch (Exception ex) {}
          try {
            maxTreeId=Integer.parseInt(tfMax[2].getText());
          } catch (Exception ex) {}
          if (minTreeId>=0 && maxTreeId>=minTreeId)
            queryStr+="Tree id in ["+minTreeId+","+maxTreeId+"]; ";
        }
        if (cb[3]!=null && cb[3].isSelected()) {
          try {
            minTreeCluster=Integer.parseInt(tfMin[3].getText());
          } catch (Exception ex) {}
          try {
            maxTreeCluster=Integer.parseInt(tfMax[3].getText());
          } catch (Exception ex) {}
          if (minTreeCluster>=0 && maxTreeCluster>=minTreeCluster)
            queryStr+="Tree cluster in ["+minTreeCluster+","+maxTreeCluster+"]; ";
        }
        if (queryStr.length()<3) {
          JOptionPane.showMessageDialog(FocusManager.getCurrentManager().getActiveWindow(),
              "The query conditions are not set properly!",
              "Invalid conditions!", JOptionPane.ERROR_MESSAGE);
          return;
        }
        if (queryStr.endsWith("; "))
          queryStr=queryStr.substring(0,queryStr.length()-2);

        selectRulesByQuery(minClass,maxClass,minValue,maxValue,
            minTreeId,maxTreeId,minTreeCluster,maxTreeCluster);
      }
    });

    Object source=this;
    queryDialog.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        super.windowClosing(e);
        isRunning=false;
        changeListener.stateChanged(new ChangeEvent(source));
      }
    });

    return true;
  }

  public void selectRulesByQuery(int minClass, int maxClass,
                                 double minValue, double maxValue,
                                 int minTreeId, int maxTreeId,
                                 int minTreeCluster, int maxTreeCluster) {
    Object source=this;
    SwingWorker worker=new SwingWorker() {
      @Override
      public Boolean doInBackground() {
        queryExecuting=true;
        selectedRules=null;
        selectedRules= RuleMaster.selectByQuery(origRules,minClass,maxClass,minValue,maxValue,
            minTreeId,maxTreeId,minTreeCluster,maxTreeCluster);
        return true;
      }

      @Override
      protected void done() {
        queryExecuting=false;
        if (selectedRules==null) {
          JOptionPane.showMessageDialog(FocusManager.getCurrentManager().getActiveWindow(),
              "The query result is null!",
              "Query failed!",JOptionPane.WARNING_MESSAGE);
          return;
        }
        if (changeListener!=null)
          changeListener.stateChanged(new ChangeEvent(source));
      }
    };
    System.out.println("Running rule selection in background");
    worker.execute();
  }

  public void toFront() {
    queryDialog.toFront();
  }

  public ArrayList<CommonExplanation> getSelectedRules() {
    return selectedRules;
  }
}
