package TapasExplTreeViewer.ui;

import TapasDataReader.CommonExplanation;
import TapasDataReader.Explanation;
import TapasExplTreeViewer.rules.RuleMaster;
import TapasExplTreeViewer.rules.UnitedRule;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Hashtable;

public class AggregationRunner implements ChangeListener {
  public ArrayList<CommonExplanation> origRules =null;
  public Hashtable<String,float[]> attrMinMax=null;
  public ArrayList<UnitedRule> rules=null;
  public AbstractList<Explanation> data=null;
  
  public boolean aggregateByQ=false;
  public double minAccuracy=0, initAccuracy=Double.NaN, accStep=Double.NaN;
  public double maxQDiff=Double.NaN;
  
  public ArrayList<UnitedRule> aggRules=null;
  public ChangeListener owner=null;
  public boolean finished=false;
  
  public JDialog progressDialog=null;
  JLabel lMessage=null;
  
  public AggregationRunner(ArrayList<UnitedRule> rules, ArrayList<CommonExplanation> origRules,
                           Hashtable<String,float[]> attrMinMax, AbstractList<Explanation> data) {
    this.rules=rules; this.origRules=origRules;
    this.attrMinMax=attrMinMax; this.data=data;
  }
  
  public void setOwner(ChangeListener owner) {
    this.owner=owner;
  }
  
  public void setMinAccuracy(double minAccuracy) {
    this.minAccuracy=minAccuracy;
  }
  
  public void setMaxQDiff(double maxQDiff) {
    this.maxQDiff=maxQDiff;
  }
  
  public void setAggregateByQ(boolean aggregateByQ) {
    this.aggregateByQ=aggregateByQ;
  }
  
  public void setIterationParameters (double initAccuracy, double accStep) {
    this.initAccuracy=initAccuracy; this.accStep=accStep;
  }
  
  public void aggregate (Window win) {
    progressDialog = new JDialog(win, "Processing", Dialog.ModalityType.MODELESS);
    progressDialog.setLayout(new BorderLayout());
    lMessage=new JLabel("Aggregating the set of "+rules.size()+
        " rules in background mode. The process may take some time ...",JLabel.CENTER);
    JPanel p=new JPanel(new FlowLayout(FlowLayout.CENTER,10,20));
    p.add(lMessage);
    JPanel pp=new JPanel(new GridLayout(0,1));
    pp.add(p);
    pp.add(new JLabel(""));
    progressDialog.getContentPane().add(pp, BorderLayout.CENTER);
    progressDialog.pack();
    progressDialog.setLocationRelativeTo(win);
    progressDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
    progressDialog.setVisible(true);

    Object source=this;
    SwingWorker worker=new SwingWorker() {
      @Override
      public Boolean doInBackground() {
        runAggregation();
        return true;
      }
    
      @Override
      protected void done() {
        if (aggRules!=null && ! aggRules.isEmpty())
          owner.stateChanged(new ChangeEvent(source));
      }
    };
    System.out.println("Running rule aggregation in background");
    worker.execute();
  }
  
  public void runAggregation() {
    if (!Double.isNaN(initAccuracy) && !Double.isNaN(accStep)) {
      for (double acc=initAccuracy; acc>=minAccuracy; acc-=accStep) {
        aggRules=(aggregateByQ)?
                     RuleMaster.aggregateByQ(rules,maxQDiff,origRules,data,acc,attrMinMax):
                     RuleMaster.aggregate(rules, origRules,data,acc,attrMinMax,this);
        if (aggRules!=null && aggRules.size()<rules.size()) {
          rules=aggRules;
          if (acc-accStep>=minAccuracy)
            owner.stateChanged(new ChangeEvent(this)); //notify about intermediate result
        }
      }
      aggRules=rules;
    }
    else {
      aggRules=(aggregateByQ)?
                   RuleMaster.aggregateByQ(rules,maxQDiff,origRules,data,minAccuracy,attrMinMax):
                   RuleMaster.aggregate(rules, origRules,data,minAccuracy,attrMinMax,this);
    }
    finished=aggregateByQ;
    if (finished && progressDialog!=null) {
      progressDialog.dispose();
      progressDialog=null;
    }
  }

  public void stateChanged(ChangeEvent e) {
    if (e.getSource() instanceof ArrayList) {
      aggRules=(ArrayList<UnitedRule>)e.getSource();
      if (progressDialog!=null) {
        lMessage.setText("Rule aggregation: received an intermediate result; current number of rules = "+aggRules.size());
        JPanel pp=(JPanel)progressDialog.getContentPane().getComponent(0);
        JPanel p=new JPanel(new FlowLayout(FlowLayout.CENTER,5,5));
        Button b=new Button("Show in a new window");
        Object eventSource=this;
        b.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            b.setEnabled(false);
            owner.stateChanged(new ChangeEvent(eventSource)); //notify about intermediate result
            progressDialog.toFront();
          }
        });
        p.add(b);
        pp.remove(1);
        pp.add(p);
        progressDialog.pack();
        progressDialog.toFront();
      }
      else
        owner.stateChanged(new ChangeEvent(this)); //notify about intermediate result
    }
    else
      if (e.getSource() instanceof String) {
        String msg=(String)e.getSource();
        if (msg.equals("aggregation_finished")) {
          if (progressDialog!=null) {
            progressDialog.dispose();
            progressDialog=null;
          }
          finished=true;
          owner.stateChanged(new ChangeEvent(this)); //notify about final result
        }
      }
  }
}
