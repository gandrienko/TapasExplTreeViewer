package TapasExplTreeViewer.ui;

import TapasDataReader.CommonExplanation;
import TapasDataReader.Explanation;
import TapasDataReader.ExplanationItem;
import TapasExplTreeViewer.MST.Edge;
import TapasExplTreeViewer.MST.Prim;
import TapasExplTreeViewer.MST.Vertex;
import TapasExplTreeViewer.clustering.*;
import TapasExplTreeViewer.rules.RuleMaster;
import TapasExplTreeViewer.rules.UnitedRule;
import TapasExplTreeViewer.util.CoordinatesReader;
import TapasExplTreeViewer.util.MatrixWriter;
import TapasExplTreeViewer.vis.*;
import TapasUtilities.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class ShowRules implements RulesOrderer, ChangeListener {
  public static String RULES_FOLDER=null;
  
  public static Border highlightBorder=new LineBorder(ProjectionPlot2D.highlightColor,1);
  /**
   * The very original rule set (before any transformations have been applied)
   */
  public ArrayList<CommonExplanation> origRules =null;
  /**
   * Whether there are different actions (decisions, classes) in the data and in the original rules
   */
  public boolean actionsDiffer =false;
  /**
   * The highlighter and selector for the original rule set
   */
  public SingleHighlightManager origHighlighter=null;
  public ItemSelectionManager origSelector=null;
  
  /**
   * The rules or explanations to be visualized
   */
  public ArrayList<CommonExplanation> exList=null;
  /**
   * The data instances (cases) the rules apply to.
   */
  public AbstractList<Explanation> dataInstances =null;
  /**
   * Whether the rule set has been previously reduced by removing rules
   * subsumed in more general rules.
   */
  public boolean nonSubsumed =false;
  /**
   * Whether the rule set consists of generalized rules obtained by aggregation.
   */
  public boolean aggregated=false;
  /**
   * Whether the rule set consists of expanded rule hierarchies
   */
  public boolean expanded=false;
  /**
   * The accuracy threshold used in aggregation
   */
  public double accThreshold=1;
  /**
   * The threshold for the differences in Q used for the aggregation
   */
  public double maxQDiff=0;
  /**
   * The ranges of feature values
   */
  public Hashtable<String,float[]> attrMinMax=null;
  /**
   * The distances between the rules
   */
  protected double distanceMatrix[][]=null;
  protected boolean orderingInProgress=false;
  
  protected ClustererByOPTICS clOptics=null;
  
  protected JTable table=null;
  protected JLabel_Rule ruleRenderer=null;
  
  public String title=null;
  public String dataFolder="";
  
  protected JFrame mainFrame=null;
  protected ArrayList<JFrame> frames=null;
  protected ArrayList<File> createdFiles=null;
  /**
   * A top frame is created once for each instance of ShowRules.
   * When a top frame is closed, all other frames created by this instance of ShowRules
   * are also closed.
   * When the last top frame is closed, the temporary files that have been created
   * are erased.
   */
  protected static ArrayList<JFrame> topFrames=null;

  protected RuleSelector ruleSelector=null;
  /**
   * Whether this instance of ShowRules has already created its top frame
   */
  protected boolean topFrameCreated=false;

  /**
   * used for rendering rules in tooltips
   */
  protected Vector<String> attrs=null;
  protected Vector<float[]> minmax=null;

  /**
   * list of features sorted by frequency
   */
  public ArrayList<String> listOfFeatures=null;
  /**
   * Which features are to be used in computing distances between rules
   */
  public HashSet featuresInDistances=null;
  
  public ShowRules(ArrayList<CommonExplanation> exList,
                   Hashtable<String,float[]> attrMinMax,
                   double distances[][]) {
    this.exList=exList; this.attrMinMax=attrMinMax;
    this.distanceMatrix=distances;
    if (exList!=null && !exList.isEmpty() && exList.get(0).numId<0)
      for (int i=0; i<exList.size(); i++)
        exList.get(i).numId=i+1;
    if (distances==null && exList.size()<500)
      computeDistanceMatrix();
    /*
    if (distanceMatrix==null && exList.size()<=1000) {
      System.out.println("Computing distance matrix...");
      distanceMatrix = CommonExplanation.computeDistances(exList, attrMinMax);
      if (distanceMatrix == null)
        System.out.println("Failed to compute a matrix of distances between the rules (explanations)!");
      else
        System.out.println("Distance matrix ready!");
    }
    */
    ToolTipManager.sharedInstance().setDismissDelay(30000);
  }
  
  public ShowRules(ArrayList<CommonExplanation> exList, Hashtable<String,float[]> attrMinMax) {
    this(exList,attrMinMax,null);
  }
  
  public AbstractList<Explanation> getDataInstances() {
    return dataInstances;
  }
  
  public void setDataInstances(AbstractList<Explanation> exData, boolean actionsDiffer) {
    this.dataInstances = exData;
    this.actionsDiffer=actionsDiffer;
  }
  
  public void countRightAndWrongRuleApplications() {
    if (dataInstances!=null && !dataInstances.isEmpty() && exList!=null && !exList.isEmpty())
      for (CommonExplanation ex:exList)
        ex.countRightAndWrongApplications(dataInstances,actionsDiffer);
  }
  
  public ArrayList<CommonExplanation> getOrigRules() {
    return origRules;
  }
  
  public void setOrigRules(ArrayList<CommonExplanation> origRules) {
    this.origRules = origRules;
  }
  
  public void setOrigHighlighter(SingleHighlightManager origHighlighter) {
    this.origHighlighter = origHighlighter;
  }
  
  public void setOrigSelector(ItemSelectionManager origSelector) {
    this.origSelector = origSelector;
  }
  
  public void setCreatedFileRegister(ArrayList<File> createdFiles) {
    if (createdFiles!=null)
      this.createdFiles=createdFiles;
  }
  
  public void setNonSubsumed(boolean nonSubsumed) {
    this.nonSubsumed = nonSubsumed;
  }
  
  public void setAggregated(boolean aggregated) {
    this.aggregated = aggregated;
  }
  
  public double getAccThreshold() {
    return accThreshold;
  }
  
  public void setAccThreshold(double accThreshold) {
    this.accThreshold = accThreshold;
  }
  
  public double getMaxQDiff() {
    return maxQDiff;
  }
  
  public void setMaxQDiff(double maxQDiff) {
    this.maxQDiff = maxQDiff;
  }
  
  public boolean isExpanded() {
    return expanded;
  }
  
  public void setExpanded(boolean expanded) {
    this.expanded = expanded;
  }
  
  public void setTitle(String title) {
    this.title = title;
  }
  
  public JFrame showRulesInTable() {
    return showRulesInTable(exList,attrMinMax);
  }

  protected JDialog selectFeaturesDialog=null;
  protected FeatureSelector featureSelector=null;

  public void selectFeaturesToComputeDistances() {
    if (selectFeaturesDialog!=null) {
      selectFeaturesDialog.toFront();
      return;
    }
    if (exList==null || exList.size()<3)
      return;
    if (listOfFeatures==null)
      listOfFeatures=RuleMaster.getListOfFeatures(exList);
    if (listOfFeatures==null || listOfFeatures.size()<2) {
      JOptionPane.showMessageDialog(null, "Found "+
              ((listOfFeatures==null)?"0":listOfFeatures.size())+" features!",
          "Error", JOptionPane.ERROR_MESSAGE);
      return;
    }

    Window win=(mainFrame!=null)?mainFrame:FocusManager.getCurrentManager().getActiveWindow();
    selectFeaturesDialog=new JDialog(win, "Select features",
        Dialog.ModalityType.MODELESS);
    selectFeaturesDialog.setLayout(new BorderLayout());
    selectFeaturesDialog.add(new JLabel("Select features to use in computing distances:"),
        BorderLayout.NORTH);
    featureSelector=new FeatureSelector(listOfFeatures,featuresInDistances);
    selectFeaturesDialog.getContentPane().add(featureSelector, BorderLayout.CENTER);
    JPanel bp=new JPanel(new FlowLayout(FlowLayout.CENTER, 5,5));
    selectFeaturesDialog.add(bp,BorderLayout.SOUTH);
    JButton b=new JButton("Apply");
    bp.add(b);
    b.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        featuresInDistances=featureSelector.getSelection();
        distanceMatrix=null;
        clOptics=null;
        if (JOptionPane.showConfirmDialog(featureSelector,"Re-compute distances and ordering?",
            "Re-compute?",JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE)==JOptionPane.YES_OPTION)
          computeDistanceMatrix();
      }
    });
    selectFeaturesDialog.pack();
    selectFeaturesDialog.setLocationRelativeTo(win);
    selectFeaturesDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    selectFeaturesDialog.setVisible(true);
    selectFeaturesDialog.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosed(WindowEvent e) {
        super.windowClosed(e);
        selectFeaturesDialog=null;
      }
    });
  }

  public ArrayList<String> getSelectedFeatures(){
    if (featuresInDistances==null || featuresInDistances.isEmpty())
      return listOfFeatures;
    ArrayList<String> selected=new ArrayList<String>(featuresInDistances.size());
    for (int i=0; i<listOfFeatures.size(); i++)
      if (featuresInDistances.contains(listOfFeatures.get(i)))
        selected.add(listOfFeatures.get(i));
    return selected;
  }

  public void computeDistanceMatrix(){
    if (distanceMatrix==null)  {
      SwingWorker worker=new SwingWorker() {
        @Override
        public Boolean doInBackground() {
          orderingInProgress=true;
          System.out.println("Computing distance matrix in background mode ...");
          distanceMatrix = CommonExplanation.computeDistances(exList, featuresInDistances, attrMinMax);
          return true;
        }

        @Override
        protected void done() {
          orderingInProgress=false;
          if (distanceMatrix == null) {
            System.out.println("Failed to compute a matrix of distances between the rules (explanations)!");
          }
          else {
            System.out.println("Distance matrix ready!");
            if (table!=null)
              runOptics((ExListTableModel) table.getModel());
          }
        }
      };
      worker.execute();
    }
  }

  public void runOptics(ChangeListener changeListener){
    if (orderingInProgress || clOptics!=null)
      return;
    clOptics=(distanceMatrix!=null && distanceMatrix.length>5)?new ClustererByOPTICS():null;
    if (clOptics!=null) {
      boolean showOriginalRules=this.exList.equals(origRules);
      SingleHighlightManager highlighter=(showOriginalRules)?origHighlighter:new SingleHighlightManager();
      ItemSelectionManager selector=(showOriginalRules)?origSelector:new ItemSelectionManager();
      clOptics.setDistanceMatrix(distanceMatrix);
      clOptics.setHighlighter(highlighter);
      clOptics.setSelector(selector);
      clOptics.addChangeListener(changeListener);
      clOptics.addChangeListener(this);
      System.out.println("Running OPTICS clustering in background mode ...");
      orderingInProgress=true;
      clOptics.doClustering();
    }
  }
  
  public JFrame showRulesInTable(ArrayList rules,
                                 Hashtable<String,float[]> attrMinMax) {

    boolean showOriginalRules=rules.equals(origRules);
    if (showOriginalRules && origHighlighter==null) {
      origHighlighter=new SingleHighlightManager();
      origSelector=new ItemSelectionManager();
    }

    SingleHighlightManager highlighter=(showOriginalRules)?origHighlighter:new SingleHighlightManager();
    ItemSelectionManager selector=(showOriginalRules)?origSelector:new ItemSelectionManager();

    int minAction=Integer.MAX_VALUE, maxAction=Integer.MIN_VALUE;
    double minQValue=Double.NaN, maxQValue=Double.NaN;
    for (int i=0; i<origRules.size(); i++) {
      CommonExplanation ex = (CommonExplanation) origRules.get(i);
      if (minAction > ex.action)
        minAction = ex.action;
      if (maxAction < ex.action)
        maxAction = ex.action;
      if (!Double.isNaN(ex.meanQ)) {
        if (Double.isNaN(minQValue) || minQValue > ex.minQ)
          minQValue = ex.minQ;
        if (Double.isNaN(maxQValue) || maxQValue < ex.maxQ)
          maxQValue = ex.maxQ;
      }
    }
    int minA=minAction, maxA=maxAction;
    double minQ=minQValue, maxQ=maxQValue;
  
    if (createdFiles==null)
      createdFiles=new ArrayList<File>(20);

    ExListTableModel eTblModel=new ExListTableModel(rules,attrMinMax);
    if (clOptics==null)
      runOptics(eTblModel);
    else
      clOptics.addChangeListener(eTblModel);
    listOfFeatures=eTblModel.listOfFeatures;
    
    table=new JTable(eTblModel){
      public String getToolTipText(MouseEvent e) {
        java.awt.Point p = e.getPoint();
        int rowIndex = rowAtPoint(p);
        if (rowIndex>=0) {
          int realRowIndex = convertRowIndexToModel(rowIndex);
          highlighter.highlight(new Integer(realRowIndex));
          int colIndex=columnAtPoint(p);
          String s="";
          if (colIndex>=0) {
            int realColIndex=convertColumnIndexToModel(colIndex);
            s=eTblModel.getColumnName(realColIndex);
          }
          CommonExplanation ce=(CommonExplanation)rules.get(realRowIndex);
          Vector<CommonExplanation> vce=null;
          ArrayList selected=selector.getSelected();
          if (selected!=null && selected.size()>0) {
            vce=new Vector<>(selected.size());
            for (int i = 0; i < selected.size(); i++)
              vce.add(exList.get((Integer)selected.get(i)));
          }
          try {
            BufferedImage bi = ShowSingleRule.getImageForRule(300,100, ce, vce, attrs, minmax);
            File outputfile = new File("img.png");
            ImageIO.write(bi, "png", outputfile);
            //System.out.println("img"+ce.numId+".png");
          } catch (IOException ex) { System.out.println("* error while writing image to file: "+ex.toString()); }
          String out=ce.toHTML(eTblModel.listOfFeatures,attrMinMax,s,"img.png");
          //System.out.println(out);
          return out;
        }
        highlighter.clearHighlighting();
        return "";
      }
    
      /**/
      public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        if (renderer==null)
          return null;
        Component c = super.prepareRenderer(renderer, row, column);
        Color bkColor=(isRowSelected(row))?getSelectionBackground():getBackground();
        int rowIdx=convertRowIndexToModel(row), colIdx=convertColumnIndexToModel(column);
        boolean isCluster=eTblModel.isClusterColumn(colIdx);
        if (isCluster)
          bkColor= ReachabilityPlot.getColorForCluster((Integer)eTblModel.getValueAt(rowIdx,colIdx));
        boolean isAction=false, isQ=false;
        if (!isCluster && minA<maxA &&
            eTblModel.isResultClassColumn(colIdx)) {
          bkColor = ExplanationsProjPlot2D.getColorForAction((Integer) eTblModel.getValueAt(rowIdx, colIdx),
              minA, maxA);
          isAction=true;
        }
        if (!isCluster && minQ<maxQ && eTblModel.isResultValueColumn(colIdx)) {
          Object v=eTblModel.getValueAt(rowIdx, colIdx);
          if (v!=null)
          if (v instanceof Float) {
            bkColor = ExplanationsProjPlot2D.getColorForQ(new Double((Float)v),minQ, maxQ);
            isQ = true;
          }
          else
            if (v instanceof double[]) {
              double d[]=(double[])v;
              if (d.length>1) {
                Color qC=ExplanationsProjPlot2D.getColorForQ((d[0]+d[1])/2,minQ, maxQ);
                bkColor = new Color(qC.getRed(),qC.getGreen(),qC.getBlue(),30);
                isQ = true;
              }
            }
        }
        c.setBackground(bkColor);
        if (highlighter==null || highlighter.getHighlighted()==null ||
                ((Integer)highlighter.getHighlighted())!=rowIdx) {
          ((JComponent) c).setBorder(null);
          return c;
        }
        ((JComponent) c).setBorder(highlightBorder);
        if (!isCluster && !isAction && !isQ)
          c.setBackground(ProjectionPlot2D.highlightFillColor);
        return c;
      }
      /**/
    };
    /**/
    table.addMouseListener(new MouseAdapter() {
      private void reactToMousePosition(MouseEvent e) {
        int rowIndex=table.rowAtPoint(e.getPoint());
        if (rowIndex<0)
          highlighter.clearHighlighting();
        else {
          int realRowIndex = table.convertRowIndexToModel(rowIndex);
          highlighter.highlight(new Integer(realRowIndex));
        }
      }
      @Override
      public void mouseEntered(MouseEvent e) {
        reactToMousePosition(e);
        super.mouseEntered(e);
      }
    
      @Override
      public void mouseExited(MouseEvent e) {
        highlighter.clearHighlighting();
        super.mouseExited(e);
      }
    
      @Override
      public void mouseMoved(MouseEvent e) {
        reactToMousePosition(e);
        super.mouseMoved(e);
      }
    });
    /**/
  
    Dimension size=Toolkit.getDefaultToolkit().getScreenSize();

    table.setPreferredScrollableViewportSize(new Dimension(Math.round(size.width * 0.7f), Math.round(size.height * 0.8f)));
    table.setFillsViewportHeight(true);
    table.setAutoCreateRowSorter(true);
    table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    table.setRowSelectionAllowed(true);
    table.setColumnSelectionAllowed(false);

    for (int i=0; i<eTblModel.getColumnCount(); i++) {
      if (eTblModel.isMinMaxColumn(i)) {
        JLabel_Subinterval subIntRend = new JLabel_Subinterval();
        subIntRend.setDrawTexts(true);
        subIntRend.setPrecision(3);
        table.getColumnModel().getColumn(i).setCellRenderer(subIntRend);
      }
      else
      if (!eTblModel.isClusterColumn(i)) {
        Class columnClass = eTblModel.getColumnClass(i);
        if (columnClass==null)
          continue;
        if (columnClass.equals(Integer.class) ||
            columnClass.equals(Float.class) ||
            columnClass.equals(Double.class)) {
          float min = eTblModel.getColumnMin(i), max = eTblModel.getColumnMax(i);
          RenderLabelBarChart rBar = new RenderLabelBarChart(min, max);
          table.getColumnModel().getColumn(i).setCellRenderer(rBar);
        }
      }
    }

    ruleRenderer=new JLabel_Rule();
    attrs=new Vector(eTblModel.listOfFeatures.size());
    minmax=new Vector<>(eTblModel.listOfFeatures.size());
    for (int i=0; i<eTblModel.listOfFeatures.size(); i++) {
      String s=eTblModel.listOfFeatures.get(i);
      attrs.add(s);
      minmax.add(attrMinMax.get(s));
    }
    ruleRenderer.setAttrs(attrs,minmax);
    int rIdx=eTblModel.getRuleColumnIdx();
    if (rIdx>=0) {
      table.getColumnModel().getColumn(rIdx).setCellRenderer(ruleRenderer);
      table.getColumnModel().getColumn(rIdx).setPreferredWidth(200);
    }

    for (int i=0; i<eTblModel.listOfFeatures.size(); i++)
      table.getColumnModel().getColumn(eTblModel.listOfColumnNames.size()+i).
          setCellRenderer(new JLabel_Subinterval());

    /**/
    TableRowsSelectionManager rowSelMan=new TableRowsSelectionManager();
    rowSelMan.setTable(table);
    rowSelMan.setHighlighter(highlighter);
    rowSelMan.setSelector(selector);
    rowSelMan.setMayScrollTable(false);
    /**/

    TableRowSorter sorter=(TableRowSorter)table.getRowSorter();
    for (int i=0; i<eTblModel.listOfFeatures.size(); i++)
      sorter.setComparator(eTblModel.listOfColumnNames.size()+i, new Comparator<Object>() {
        public int compare (Object o1, Object o2) {
          if (o1 instanceof double[]) {
            double d1=((double[])o1)[0], d2=((double[])o2)[0], d1r=((double[])o1)[1], d2r=((double[])o2)[1];
            if (Double.isNaN(d1) && Double.isNaN(d2))
              return 0;
            if (Double.isNaN(d1))
              return 1;
            if (Double.isNaN(d2))
              return -1;
            return (d1<d2)?-1:(d1==d2)?((d1r<d2r)?-1:(d1r==d2r)?0:1):1;
          }
          else
            return 0;
        }
      });
    
    JPopupMenu menu=new JPopupMenu();

    JCheckBoxMenuItem cbmit=new JCheckBoxMenuItem("Show texts for intervals",false);
    menu.add(cbmit);
    cbmit.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        for (int i=0; i<eTblModel.listOfFeatures.size(); i++) {
          TableCellRenderer tcr=table.getColumnModel().
              getColumn(eTblModel.listOfColumnNames.size()+i).getCellRenderer();
          if (tcr instanceof JLabel_Subinterval) {
            ((JLabel_Subinterval)tcr).setDrawTexts(cbmit.getState());
          }
        }
        eTblModel.fireTableDataChanged();
      }
    });
    JCheckBoxMenuItem cbmit1=new JCheckBoxMenuItem("Mark values",false);
    JCheckBoxMenuItem cbmit2=new JCheckBoxMenuItem("Show statistics",false);
    menu.add(cbmit1);
    cbmit1.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        for (int i=0; i<eTblModel.listOfFeatures.size(); i++) {
          TableCellRenderer tcr=table.getColumnModel().getColumn(
              eTblModel.listOfColumnNames.size()+i).getCellRenderer();
          if (tcr instanceof JLabel_Subinterval) {
            ((JLabel_Subinterval)tcr).setDrawValues(cbmit1.getState());
          }
        }
        eTblModel.setDrawValuesOrStatsForIntervals(cbmit1.getState() || cbmit2.getState());
        eTblModel.fireTableDataChanged();
      }
    });
    menu.add(cbmit2);
    cbmit2.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        for (int i=0; i<eTblModel.listOfFeatures.size(); i++) {
          TableCellRenderer tcr=table.getColumnModel().
              getColumn(eTblModel.listOfColumnNames.size()+i).getCellRenderer();
          if (tcr instanceof JLabel_Subinterval) {
            ((JLabel_Subinterval)tcr).setDrawStats(cbmit2.getState());
          }
        }
        eTblModel.setDrawValuesOrStatsForIntervals(cbmit1.getState() || cbmit2.getState());
        eTblModel.fireTableDataChanged();
      }
    });
    JMenuItem mit=new JMenuItem("Table -> Clipboard");
    menu.add(mit);
    mit.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        eTblModel.putTableToClipboard();
      }
    });
    menu.addSeparator();
  
    mit=new JMenuItem("Represent rules by glyphs");
    menu.add(mit);
    mit.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        showRuleGlyphs(exList,attrs,highlighter,selector);
      }
    });

    menu.add(mit=new JMenuItem("Apply topic modelling"));
    mit.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        applyTopicModelling(rules, attrMinMax);
      }
    });

    menu.addSeparator();
    menu.add(mit=new JMenuItem("Select feature subset for computing distances"));
    mit.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        selectFeaturesToComputeDistances();
      }
    });

    menu.add(mit=new JMenuItem("Show the OPTICS reachability plot"));
    mit.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (orderingInProgress) {
          JOptionPane.showMessageDialog(null, "Ordering is in progress.",
              "Wait...", JOptionPane.WARNING_MESSAGE);
          return;
        }
        if (clOptics!=null) {
          JFrame fr = clOptics.showPlot();
          if (fr != null)
            fr.toFront();
        }
        else {
          if (distanceMatrix==null)
            computeDistanceMatrix();
          else
            runOptics((ExListTableModel) table.getModel());
          if (clOptics!=null)
            clOptics.showPlot();
        }
      }
    });
    
    menu.add(mit=new JMenuItem("Show the t-SNE projection"));
    mit.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        showProjection(rules,distanceMatrix,highlighter,selector);
      }
    });

    menu.add(mit=new JMenuItem("Apply hierarchical clustering"));
    mit.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        hierClustering(exList,distanceMatrix,minA,maxA,minQ,maxQ);
      }
    });

    if (aggregated && !expanded) {
      menu.add(mit=new JMenuItem("Expand rule hierarchies"));
      mit.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          boolean applyToSelection=
              selector.hasSelection() &&
                  JOptionPane.showConfirmDialog(FocusManager.getCurrentManager().getActiveWindow(),
                      "Apply the operation to the selected subset?",
                      "Apply to selection?",JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE)
                      ==JOptionPane.YES_OPTION;
          ArrayList rules=(applyToSelection)?getSelectedRules(exList,selector):exList;
          ArrayList<UnitedRule> expanded=RuleMaster.expandRuleHierarchies(rules);
          if (expanded!=null) {
            /*
            if (origRules!=null) {
              boolean noActions=RuleMaster.noActionDifference(origRules);
              for (UnitedRule r : expanded)
                if (noActions)
                  r.countRightAndWrongCoveragesByQ(origRules);
                else
                  r.countRightAndWrongCoverages(origRules);
            }
            */
            ArrayList<CommonExplanation> ex=new ArrayList<CommonExplanation>(expanded.size());
            ex.addAll(expanded);
            ShowRules showRules=createShowRulesInstance(ex);
            showRules.setNonSubsumed(true);
            showRules.setAggregated(true);
            showRules.setExpanded(true);
            showRules.setCreatedFileRegister(createdFiles);
            showRules.countRightAndWrongRuleApplications();
            showRules.showRulesInTable();
          }
        }
      });
      menu.add(mit=new JMenuItem("Show the links between the original rules in a t-SNE projection"));
      mit.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
         showAggregationInProjection(selector);
        }
      });
    }
    
    JMenuItem mitExtract=new JMenuItem("Extract the selected subset to a separate view");
    menu.add(mitExtract);
    mitExtract.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        extractSubset(selector,rules,distanceMatrix,attrMinMax);
      }
    });
    
    if (!expanded) {
      menu.addSeparator();
      menu.add(mit = new JMenuItem("Remove contradictory rules"));
      mit.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          removeContradictory(exList);
        }
      });
      menu.add(mit = new JMenuItem("Extract the non-subsumed rules to a separate view"));
      mit.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          getNonSubsumed(exList, attrMinMax);
        }
      });
      menu.add(mit = new JMenuItem("Extract rule subset through a query"));
      ChangeListener changeListener=this;
      mit.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          if (ruleSelector!=null && ruleSelector.isRunning)
            ruleSelector.toFront();
          else {
            ruleSelector=new RuleSelector();
            if (!ruleSelector.makeQueryInterface(exList,changeListener))
              ruleSelector=null;
          }
        }
      });

      menu.addSeparator();
      menu.add(mit = new JMenuItem("Aggregate and generalise rules"));
      mit.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          aggregate(exList, attrMinMax);
        }
      });
      /*
      if (dataInstances!=null) {
        menu.add(mit = new JMenuItem("Aggregate and generalize rules checking data-based accuracy"));
        mit.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            aggregate(exList, attrMinMax, true);
          }
        });
      }
      */
    }

    menu.addSeparator();
    menu.add(mit=new JMenuItem("Export rules to file in text format"));
    mit.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        boolean applyToSelection=
            selector.hasSelection() &&
                JOptionPane.showConfirmDialog(FocusManager.getCurrentManager().getActiveWindow(),
                    "Apply the operation to the selected subset?",
                    "Apply to selection?",JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE)
                    ==JOptionPane.YES_OPTION;
        ArrayList rules=(applyToSelection)?getSelectedRules(exList,selector):exList;
        // Select file to save
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Specify a file to save");
        fileChooser.setCurrentDirectory(new File(RULES_FOLDER));
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Text file", "txt");
        fileChooser.setFileFilter(filter);
        int userSelection = fileChooser.showSaveDialog(null);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
          File fileToSave = fileChooser.getSelectedFile();
          String fName=fileToSave.getName().toLowerCase();
          if (!fName.endsWith(".txt") && !fName.endsWith(".csv"))
            fileToSave = new File(fileToSave.getAbsolutePath() + ".txt");
          boolean ok=RuleMaster.exportRulesToFile(fileToSave, rules);
          if (ok)
            JOptionPane.showMessageDialog(null, "Rules exported successfully!");
          else
            JOptionPane.showMessageDialog(null, "Failed to export rules.",
                "Error", JOptionPane.ERROR_MESSAGE);
        }
      }
    });
    menu.add(mit=new JMenuItem("Export rules to file in structured (table) format"));
    mit.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        boolean applyToSelection=
            selector.hasSelection() &&
                JOptionPane.showConfirmDialog(FocusManager.getCurrentManager().getActiveWindow(),
                    "Apply the operation to the selected subset?",
                    "Apply to selection?",JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE)
                    ==JOptionPane.YES_OPTION;
        ArrayList rules=(applyToSelection)?getSelectedRules(exList,selector):exList;
        // Select file to save
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Specify a file to save the table");
        fileChooser.setCurrentDirectory(new File(RULES_FOLDER));
        // Set the file extension filter
        FileNameExtensionFilter filter = new FileNameExtensionFilter("CSV file", "csv");
        fileChooser.setFileFilter(filter);
        int userSelection = fileChooser.showSaveDialog(null);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
          File fileToSave = fileChooser.getSelectedFile();
          String fName=fileToSave.getName().toLowerCase();
          if (!fName.endsWith(".csv"))
            fileToSave = new File(fileToSave.getAbsolutePath() + ".csv");
          boolean ok=RuleMaster.exportRulesToTable(fileToSave, rules,attrMinMax);
          if (ok)
            JOptionPane.showMessageDialog(null, "Rules exported successfully!");
          else
            JOptionPane.showMessageDialog(null, "Failed to export rules.",
                "Error", JOptionPane.ERROR_MESSAGE);
        }
      }
    });

    menu.addSeparator();
    menu.add(mit=new JMenuItem("Quit"));
    mit.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        int result = JOptionPane.showConfirmDialog(FocusManager.getCurrentManager().getActiveWindow(),
            "Sure? Do you want to exit?",
            "Confirm quitting",JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE);
        if(result == JOptionPane.YES_OPTION) {
          eraseCreatedFiles();
          System.exit(0);
        }
      }
    });
    
    table.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        super.mousePressed(e);
        if (e.getButton()>MouseEvent.BUTTON1) {
          ArrayList selected=selector.getSelected();
          mitExtract.setEnabled(selected!=null && selected.size()>0);
          menu.show(table,e.getX(),e.getY());
        }
      }
      @Override
      public void mouseClicked(MouseEvent e) {
        super.mouseClicked(e);
        if (e.getClickCount()>1) {
          selector.deselectAll();
          return;
        }
        JPopupMenu selMenu=new JPopupMenu();;
        int rowIndex=table.rowAtPoint(e.getPoint());
        if (rowIndex<0)
          return;
        int realRowIndex = table.convertRowIndexToModel(rowIndex);
        if (exList.get(realRowIndex) instanceof UnitedRule) {
          UnitedRule rule=(UnitedRule)exList.get(realRowIndex);
          if (rule.fromRules!=null && !rule.fromRules.isEmpty()) {
            JMenuItem selItem = new JMenuItem("Show derivation hierarchy of rule " + rule.numId);
            selMenu.add(selItem);
            selItem.addActionListener(new ActionListener() {
              @Override
              public void actionPerformed(ActionEvent e) {
                showRuleHierarchy(rule,attrs);
              }
            });
          }
        }
        if (exList.get(realRowIndex) instanceof UnitedRule) {
          UnitedRule rule=(UnitedRule)exList.get(realRowIndex);
          if (origRules!=null) {
            //selMenu = new JPopupMenu();
            JMenuItem selItem = new JMenuItem("Extract all coverages of rule " + rule.numId);
            selMenu.add(selItem);
            selItem.addActionListener(new ActionListener() {
              @Override
              public void actionPerformed(ActionEvent e) {
                ArrayList<CommonExplanation> valid=rule.extractValidCoverages(origRules,
                    RuleMaster.noActionDifference(origRules));
                ArrayList<CommonExplanation> wrong=rule.extractWrongCoverages(origRules,
                    RuleMaster.noActionDifference(origRules));
                if ((valid==null || valid.isEmpty()) && (wrong==null || wrong.isEmpty())) {
                  JOptionPane.showMessageDialog(FocusManager.getCurrentManager().getActiveWindow(),
                      "Neither valid nor invalid coverages found!",
                      "Very strange!", JOptionPane.INFORMATION_MESSAGE);
                  return;
                }
                int n=1;
                if (valid!=null) n+=valid.size();
                if (wrong!=null) n+=wrong.size();
                ArrayList<CommonExplanation> all=new ArrayList<CommonExplanation>(n);
                if (valid!=null)
                  all.addAll(valid);
                if (wrong!=null)
                  all.addAll(wrong);
                all.add(0,rule);
                ShowRules showRules=createShowRulesInstance(all);
                showRules.setTitle("All coverages of rule # "+rule.numId);
                showRules.showRulesInTable();
              }
            });
            selMenu.add(selItem = new JMenuItem("Extract invalid coverages of rule " + rule.numId));
            selItem.addActionListener(new ActionListener() {
              @Override
              public void actionPerformed(ActionEvent e) {
                ArrayList<CommonExplanation> wrong=rule.extractWrongCoverages(origRules,
                    RuleMaster.noActionDifference(origRules));
                if (wrong==null || wrong.isEmpty()) {
                  JOptionPane.showMessageDialog(FocusManager.getCurrentManager().getActiveWindow(),
                      "No invalid coverages found!",
                      "No exceptions", JOptionPane.INFORMATION_MESSAGE);
                  return;
                }
                wrong.add(0,rule);
                ShowRules showRules=createShowRulesInstance(wrong);
                showRules.setTitle("Invalid coverages of rule # "+rule.numId);
                showRules.showRulesInTable();
              }
            });
            selMenu.add(selItem = new JMenuItem("Extract valid coverages of rule " + rule.numId));
            selItem.addActionListener(new ActionListener() {
              @Override
              public void actionPerformed(ActionEvent e) {
                ArrayList<CommonExplanation> valid=rule.extractValidCoverages(origRules,
                    RuleMaster.noActionDifference(origRules));
                if (valid==null || valid.isEmpty()) {
                  JOptionPane.showMessageDialog(FocusManager.getCurrentManager().getActiveWindow(),
                      "No valid coverages found!",
                      "Very strange!", JOptionPane.INFORMATION_MESSAGE);
                  return;
                }
                valid.add(0,rule);
                ShowRules showRules=createShowRulesInstance(valid);
                showRules.setTitle("Valid coverages of rule # "+rule.numId);
                showRules.showRulesInTable();
              }
            });
          }
          if (expanded) {
            if (rule.upperId >= 0) {
              //if (selMenu == null)
                //selMenu = new JPopupMenu();
              JMenuItem selItem = new JMenuItem("Select rules that include rule " + rule.numId);
              selMenu.add(selItem);
              selItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                  ArrayList<Integer> toSelect = new ArrayList<Integer>(10);
                  UnitedRule r = rule;
                  do {
                    int idx = RuleMaster.findRuleInList(exList, r.upperId);
                    if (idx >= 0) {
                      toSelect.add(new Integer(idx));
                      r = (UnitedRule) exList.get(idx);
                    }
                    else
                      r = null;
                  } while (r != null && r.upperId >= 0);
                  if (!toSelect.isEmpty())
                    selector.select(toSelect);
                }
              });
            }

            if (rule.fromRules != null && !rule.fromRules.isEmpty()) {
              //if (selMenu == null)
                //selMenu = new JPopupMenu();
              JMenuItem selItem = new JMenuItem("Select rules directly included in " + rule.numId);
              selMenu.add(selItem);
              selItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                  ArrayList<Integer> toSelect = new ArrayList<Integer>(10);
                  for (int i = 0; i < rule.fromRules.size(); i++) {
                    int idx = RuleMaster.findRuleInList(exList, rule.fromRules.get(i).numId);
                    if (idx >= 0)
                      toSelect.add(idx);
                  }
                  if (!toSelect.isEmpty())
                    selector.select(toSelect);
                }
              });
              selMenu.add(selItem = new JMenuItem("Select all rules included in " + rule.numId));
              selItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                  ArrayList<UnitedRule> included = rule.putHierarchyInList(null);
                  ArrayList<Integer> toSelect = new ArrayList<Integer>(10);
                  for (int i = 0; i < included.size(); i++) {
                    int idx = RuleMaster.findRuleInList(exList, included.get(i).numId);
                    if (idx >= 0)
                      toSelect.add(idx);
                  }
                  if (!toSelect.isEmpty())
                    selector.select(toSelect);
                }
              });
            }
          }
          //if (selMenu==null)
            //return;
        }
        String title="Select data for the rule "+
                exList.get(realRowIndex).numId+", action="+exList.get(realRowIndex).action+
                ", Nrecords="+exList.get(realRowIndex).getApplicationsCount();
        JMenuItem selItem = new JMenuItem(title);
        selItem.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            DataForRuleTableModel dataTblModel=new DataForRuleTableModel(exList.get(realRowIndex),eTblModel.listOfFeatures,attrMinMax);
            JTable dataTbl=new JTable(dataTblModel);
            Dimension size=Toolkit.getDefaultToolkit().getScreenSize();
            dataTbl.setPreferredScrollableViewportSize(new Dimension(Math.round(size.width * 0.7f), Math.round(size.height * 0.8f)));
            dataTbl.setFillsViewportHeight(true);
            dataTbl.setAutoCreateRowSorter(true);
            dataTbl.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
            dataTbl.setRowSelectionAllowed(true);
            dataTbl.setColumnSelectionAllowed(false);
            for (int i=dataTblModel.columnNames.length; i<dataTblModel.getColumnCount(); i++) {
              JLabel_Subinterval renderer=new JLabel_Subinterval();
              renderer.setDrawValues(true);
              //renderer.setDrawTexts(true);
              dataTbl.getColumnModel().getColumn(i).setCellRenderer(renderer);
            }
            JScrollPane scrollPane = new JScrollPane(dataTbl);
            JFrame fr = new JFrame(title);
            fr.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            fr.getContentPane().add(scrollPane, BorderLayout.CENTER);
            //Display the window.
            fr.pack();
            int nFrames=((topFrames==null)?0:topFrames.size())+((frames==null)?0:frames.size());
            fr.setLocation(30+nFrames*30, 30+nFrames*15);
            fr.setVisible(true);
          }
        });
        selMenu.addSeparator();
        selMenu.add(selItem);
        selMenu.addSeparator();
        selMenu.add(new JMenuItem("Cancel"));
        selMenu.show(table,e.getX(),e.getY());
      }
    });

    JScrollPane scrollPane = new JScrollPane(table);
    
    if (title==null) {
      int nUses=0, nCond=0;
      for (int i=0; i<rules.size(); i++) {
        CommonExplanation cEx=(CommonExplanation)rules.get(i);
        nUses+=cEx.nUses;
        nCond+=cEx.eItems.length;
      }
      title = ((expanded) ? "Expanded aggregated rules " :
                   (aggregated) ? "Aggregated rules" :
                       (nonSubsumed) ? "Extracted non-subsumed rules" :
                           "Original distinct rules or explanations") +
                  " (" + rules.size() + ")" +
                  ", N conditions (" +nCond + ")" +
                  ", Total uses (" +nUses + ")" +
                  ((aggregated) ? "; obtained with coherence threshold " +
                                      String.format("%.3f", accThreshold) : "");
      if (maxQDiff > 0)
        title += " and max Q difference " + String.format("%.5f", maxQDiff);
    }
  
    JFrame fr = new JFrame(title);
    fr.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    fr.getContentPane().add(scrollPane, BorderLayout.CENTER);
    //Display the window.
    fr.pack();
    int nFrames=((topFrames==null)?0:topFrames.size())+((frames==null)?0:frames.size());
    fr.setLocation(30+nFrames*30, 30+nFrames*15);
    fr.setVisible(true);

    if (topFrameCreated) { //this will not be a top frame
      if (frames==null)
        frames=new ArrayList<JFrame>(20);
      frames.add(fr);
    }
    else {
      if (topFrames==null)
        topFrames=new ArrayList<JFrame>(10);
      topFrames.add(fr);
      topFrameCreated=true;
    }
    
    fr.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        super.windowClosing(e);
        if (topFrames.contains(fr)) {
          if (frames!=null) {
            for (int i = 0; i < frames.size(); i++)
              frames.get(i).dispose();
            frames.clear();
          }
          topFrames.remove(fr);
          if (topFrames.isEmpty()) {
            eraseCreatedFiles();
            System.exit(0);
          }
        }
        else
          if (frames!=null)
            frames.remove(fr);
      }
    });
    if (fr!=null)
      fr.toFront();
    if (mainFrame==null)
      mainFrame=fr;
    return fr;
  }
  
  protected ShowRules createShowRulesInstance(ArrayList rules) {
    ShowRules showRules=new ShowRules(rules,attrMinMax,null);
    showRules.setOrigRules(origRules);
    showRules.setDataInstances(dataInstances,actionsDiffer);
    showRules.setOrigHighlighter(origHighlighter);
    showRules.setOrigSelector(origSelector);
    showRules.setCreatedFileRegister(createdFiles);
    showRules.setAccThreshold(getAccThreshold());
    showRules.setNonSubsumed(nonSubsumed);
    showRules.setAggregated(aggregated);
    if (!Double.isNaN(maxQDiff))
      showRules.setMaxQDiff(maxQDiff);
    showRules.setExpanded(expanded);
    showRules.setCreatedFileRegister(createdFiles);
    //showRules.showRulesInTable();
    return showRules;
  }
  
  public ArrayList getSelectedRules(ArrayList allRules, ItemSelectionManager selector) {
    if (allRules==null || allRules.isEmpty() || selector==null || !selector.hasSelection())
      return null;
    ArrayList selected=selector.getSelected();
    if (selected==null || selected.isEmpty())
      return null;
    ArrayList result=new ArrayList(selected.size());
    for (int i=0; i<selected.size(); i++) {
      int idx=(Integer)selected.get(i);
      result.add(allRules.get(idx));
    }
    if (result.isEmpty())
      return null;
    return result;
  }
  
  public int[] getRulesOrder(ArrayList rules) {
    if (rules==null || rules.isEmpty() || exList==null || table==null)
      return null;
    int order[]=new int[rules.size()];
    int k=0;
    for (int i=0; i<table.getRowCount(); i++) {
      int mIdx = table.convertRowIndexToModel(i);
      int idx=rules.indexOf(exList.get(mIdx));
      if (idx>=0)
        order[k++]=idx;
    }
    return order;
  }
  
  public void eraseCreatedFiles () {
    if (createdFiles!=null && !createdFiles.isEmpty())
      for (File f : createdFiles)
        f.delete();
    createdFiles.clear();
  }
  
  public JFrame showRuleGlyphs(ArrayList exList,
                               Vector<String> attributes,
                               SingleHighlightManager highlighter,
                               ItemSelectionManager selector) {
    boolean applyToSelection=
        selector.hasSelection() &&
            JOptionPane.showConfirmDialog(FocusManager.getCurrentManager().getActiveWindow(),
                "Apply the operation to the selected subset?",
                "Apply to selection?",JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE)
                ==JOptionPane.YES_OPTION;
    ArrayList rules=(applyToSelection)?getSelectedRules(exList,selector):exList;
    RuleSetVis vis=new RuleSetVis(rules,exList,attributes,attrMinMax);
    vis.setOrigExList(origRules);
    vis.setHighlighter(highlighter);
    vis.setSelector(selector);
    vis.setRulesOrderer(this);
  
    JScrollPane scrollPane = new JScrollPane(vis);
    scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

    Dimension size=Toolkit.getDefaultToolkit().getScreenSize();
    JFrame plotFrame=new JFrame(title+((applyToSelection)?" (selection)":""));
    plotFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    plotFrame.getContentPane().add(scrollPane);
    plotFrame.pack();
    plotFrame.setSize((int)Math.min(plotFrame.getWidth(),0.7*size.width),
        (int)Math.min(plotFrame.getHeight(),0.7*size.height));
    plotFrame.setLocation(size.width-plotFrame.getWidth()-30, size.height-plotFrame.getHeight()-50);
    plotFrame.setVisible(true);
    if (frames==null)
      frames=new ArrayList<JFrame>(20);
    frames.add(plotFrame);
    return plotFrame;
  }
  
  public JFrame showRuleHierarchy(UnitedRule rule,Vector<String> attributes) {
    RuleHierarchyVis vis=new RuleHierarchyVis(rule,origRules,attributes,attrMinMax);
    Dimension size=Toolkit.getDefaultToolkit().getScreenSize();
    JFrame plotFrame=new JFrame("Derivation hierarchy of rule "+rule.numId);
    plotFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    plotFrame.getContentPane().add(vis);
    plotFrame.pack();
    plotFrame.setSize((int)Math.min(plotFrame.getWidth(),0.7*size.width),
        (int)Math.min(plotFrame.getHeight(),0.7*size.height));
    plotFrame.setLocation(size.width-plotFrame.getWidth()-30, size.height-plotFrame.getHeight()-50);
    plotFrame.setVisible(true);
    if (frames==null)
      frames=new ArrayList<JFrame>(20);
    frames.add(plotFrame);
    return plotFrame;
  }

  public JFrame applyTopicModelling (ArrayList<CommonExplanation> exList, Hashtable<String,float[]> attrMinMax) {
    // TreeMap to maintain unique sorted values for each key
    Map<String, TreeSet<Float>> uniqueSortedValues = new TreeMap<>();
    // TreeMap to maintain all values for each key (for sorting later)
    Map<String, List<Float>> allValues = new TreeMap<>();
    // processing sttrMinMax
    for (String key: attrMinMax.keySet()) {
      //float minmax[]=attrMinMax.get(key);
      // Initialize the data structures for each key if not already initialized
      uniqueSortedValues.putIfAbsent(key, new TreeSet<>());
      allValues.putIfAbsent(key, new ArrayList<>());
/*
      // Add values to the data structures
      for (int i=0; i<minmax.length; i++) {
        uniqueSortedValues.get(key).add(minmax[i]);
        allValues.get(key).add(minmax[i]);
      }
*/
    }
    // processing rules
    for (CommonExplanation rule: exList)
      for (ExplanationItem cond: rule.eItems)
        for (int i=0; i<cond.interval.length; i++)
          if (Double.isFinite(cond.interval[i])) {
            uniqueSortedValues.get(cond.attr).add((float)cond.interval[i]);
            allValues.get(cond.attr).add((float)cond.interval[i]);
          }

    // Sort all values lists for each key
    for (List<Float> valueList: allValues.values())
      Collections.sort(valueList);
    // Output the results
    System.out.println("Unique Sorted Values by Key:");
    for (Map.Entry<String, TreeSet<Float>> entry : uniqueSortedValues.entrySet()) {
      System.out.println("Key: " + entry.getKey() + ", NValues: " + entry.getValue().size() + ", Values: " + entry.getValue());
    }
    System.out.println("All Sorted Values by Key:");
    for (Map.Entry<String, List<Float>> entry : allValues.entrySet()) {
      System.out.println("Key: " + entry.getKey() + ", NValues: " + entry.getValue().size() + ", Values: " + entry.getValue());
    }
    // Defining intervals for feature discretization
    TMrulesDefineSettings tmRulesSettings=new TMrulesDefineSettings(exList,listOfFeatures,attrMinMax,uniqueSortedValues,allValues,dataFolder);
    //
    return null;
  }

  public JFrame showProjection(ArrayList<CommonExplanation> exList,
                               double distanceMatrix[][],
                               SingleHighlightManager highlighter,
                               ItemSelectionManager selector){
    if (distanceMatrix==null) {
      JOptionPane.showMessageDialog(FocusManager.getCurrentManager().getActiveWindow(),
          "No distance matrix!",
          "Error",JOptionPane.ERROR_MESSAGE);
      return null;
    }
    TSNE_Runner tsne=new TSNE_Runner();
    tsne.setFileRegister(createdFiles);
    String value=JOptionPane.showInputDialog(FocusManager.getCurrentManager().getActiveWindow(),
        "Perplexity (integer; suggested range from 5 to 50) :",
        tsne.getPerplexity());
    if (value==null)
      return null;
    try {
      int p=Integer.parseInt(value);
      if (p<5 || p>100) {
        JOptionPane.showMessageDialog(FocusManager.getCurrentManager().getActiveWindow(),
            "Illegal perplexity: "+p,
            "Error",JOptionPane.ERROR_MESSAGE);
        return null;
      }
      tsne.setPerplexity(p);
    } catch (Exception ex) {
      JOptionPane.showMessageDialog(FocusManager.getCurrentManager().getActiveWindow(),
          "Illegal perplexity!"+value,
          "Error",JOptionPane.ERROR_MESSAGE);
      return null;
    }

    ExplanationsProjPlot2D pp=new ExplanationsProjPlot2D(attrMinMax,attrs,minmax);
    pp.setExplanations(exList,origRules);
    pp.setDistanceMatrix(distanceMatrix);
    pp.setFeatures(getSelectedFeatures());
    pp.setProjectionProvider(tsne);
    pp.setHighlighter(highlighter);
    pp.setSelector(selector);
    pp.setPreferredSize(new Dimension(800,800));
    
    if (expanded && (exList.get(0) instanceof UnitedRule)){
      HashSet<ArrayList<Vertex>> graphs=null;
      for (int i=0; i<exList.size(); i++) {
        UnitedRule rule=(UnitedRule)exList.get(i);
        if (rule.upperId>=0 || rule.fromRules==null || rule.fromRules.isEmpty())
          continue;
        ArrayList<UnitedRule> hList=rule.putHierarchyInList(null);
        if (hList==null || hList.size()<2)
          continue;
        ArrayList<Vertex> graph=new ArrayList<Vertex>(hList.size());
        Hashtable<String,Integer> vertIndexes=new Hashtable<String,Integer>(hList.size());
        for (int j=0; j<hList.size(); j++) {
          UnitedRule r=hList.get(j);
          String label=Integer.toString(r.numId);
          vertIndexes.put(label,j);
          graph.add(new Vertex(label));
        }
        for (int j=0; j<hList.size(); j++) {
          UnitedRule r=hList.get(j);
          if (r.upperId<0)
            continue;
          Edge e=new Edge(1);
          e.setIncluded(true);
          Vertex v1=graph.get(j), v2=graph.get(vertIndexes.get(Integer.toString(r.upperId)));
          v1.addEdge(v2,e);
          v2.addEdge(v1,e);
        }
        if (graphs==null)
          graphs=new HashSet<ArrayList<Vertex>>(exList.size()/10);
        graphs.add(graph);
      }
      if (graphs!=null)
        pp.setGraphs(graphs);
    }
  
    Dimension size=Toolkit.getDefaultToolkit().getScreenSize();
    JFrame plotFrame=new JFrame(pp.getProjectionProvider().getProjectionTitle());
    plotFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    plotFrame.getContentPane().add(pp);
    plotFrame.pack();
    plotFrame.setLocation(size.width-plotFrame.getWidth()-30, size.height-plotFrame.getHeight()-50);
    plotFrame.setVisible(true);
    if (frames==null)
      frames=new ArrayList<JFrame>(20);
    frames.add(plotFrame);
 
    JPopupMenu menu=new JPopupMenu();
    JMenuItem mitExtract=new JMenuItem("Extract the selected subset to a separate view");
    menu.add(mitExtract);
    mitExtract.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        extractSubset(selector,exList,distanceMatrix,attrMinMax);
      }
    });
  
    JMenuItem mit=new JMenuItem("Read point coordinates from a file");
    menu.add(mit);
    mit.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        double coords[][]= CoordinatesReader.readCoordinatesFromChosenFile();
        if (coords==null) {
          JOptionPane.showMessageDialog(FocusManager.getCurrentManager().getActiveWindow(),
              "No coordinates could be read!",
              "Error",JOptionPane.ERROR_MESSAGE);
          return;
        }
        if (coords.length!=exList.size()) {
          JOptionPane.showMessageDialog(FocusManager.getCurrentManager().getActiveWindow(),
              "The new coordinates are for "+coords.length+
                                 " points but must be for "+exList.size()+" points!",
              "Error",JOptionPane.ERROR_MESSAGE);
          return;
        }
        System.out.println("Trying to create another plot...");
        ExplanationsProjPlot2D anotherPlot=
            new ExplanationsProjPlot2D(attrMinMax,attrs,minmax,exList,origRules,coords);
        anotherPlot.setPreferredSize(new Dimension(800,800));
        anotherPlot.setSelector(selector);
        anotherPlot.setHighlighter(highlighter);
      
        anotherPlot.addMouseListener(new MouseAdapter() {
          @Override
          public void mousePressed(MouseEvent e) {
            super.mousePressed(e);
            if (e.getButton()>MouseEvent.BUTTON1) {
              ArrayList selected=selector.getSelected();
              mitExtract.setEnabled(selected!=null && selected.size()>0);
              menu.show(anotherPlot,e.getX(),e.getY());
            }
          }
        });
      
        JFrame fr=new JFrame(CoordinatesReader.lastFileName);
        fr.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        fr.getContentPane().add(anotherPlot);
        fr.pack();
        Point p=plotFrame.getLocationOnScreen();
        fr.setLocation(Math.max(10,p.x-30), Math.max(10,p.y-50));
        fr.setVisible(true);
        if (frames==null)
          frames=new ArrayList<JFrame>(20);
        frames.add(fr);
      }
    });
  
    mit=new JMenuItem("Export the distance matrix to a file");
    menu.add(mit);
    mit.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        MatrixWriter.writeMatrixToFile(distanceMatrix,"allDistances.csv",true);
      }
    });
  
    mit=new JMenuItem("Re-run t-SNE with another perplexity setting");
    menu.add(mit);
    mit.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        String value=JOptionPane.showInputDialog(FocusManager.getCurrentManager().getActiveWindow(),
            "Perplexity (integer; suggested range from 5 to 50) :",
            tsne.getPerplexity());
        if (value==null)
          return;
        try {
          int p=Integer.parseInt(value);
          if (p<5 || p>100) {
            JOptionPane.showMessageDialog(FocusManager.getCurrentManager().getActiveWindow(),
                "Illegal perplexity: "+p,
                "Error",JOptionPane.ERROR_MESSAGE);
            return;
          }
          tsne.setPerplexity(p);
          tsne.runAlgorithm();
        } catch (Exception ex) {
          JOptionPane.showMessageDialog(FocusManager.getCurrentManager().getActiveWindow(),
              "Illegal perplexity: "+value,
              "Error",JOptionPane.ERROR_MESSAGE);
          return;
        }
      }
    });
  
    pp.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        super.mousePressed(e);
        if (e.getButton()>MouseEvent.BUTTON1) {
          ArrayList selected=selector.getSelected();
          mitExtract.setEnabled(selected!=null && selected.size()>0);
          menu.show(pp,e.getX(),e.getY());
        }
      }
    });
    return plotFrame;
  }
  
  public JFrame hierClustering (ArrayList<CommonExplanation> exList,
                                double distanceMatrix[][],
                                int minA, int maxA,
                                double minQ, double maxQ) {
    if (distanceMatrix==null)
      return null;
  
    String options[]={"mean pairwise distance between members",
                      "distance between cluster medoids"};
    String selOption = (String)JOptionPane.showInputDialog(FocusManager.getCurrentManager().getActiveWindow(),
        "How to compute the distances between clusters?",
        "What is the distance between clusters?",JOptionPane.QUESTION_MESSAGE,
        null,options,options[0]);
    
    ClusterContent topCluster=HierarchicalClusterer.doClustering(distanceMatrix,selOption.equals(options[1]));
    if (topCluster==null) {
      JOptionPane.showMessageDialog(FocusManager.getCurrentManager().getActiveWindow(),
          "Clustering failed!",
          "Error",JOptionPane.ERROR_MESSAGE);
      return null;
    }

    int oIds[]=new int[exList.size()];
    for (int i=0; i<exList.size(); i++)
      oIds[i]=exList.get(i).numId;
    topCluster.setObjIds(oIds);

    JFrame frame = new JFrame("Cluster hierarchy; depth = "+topCluster.hierDepth+"; "+selOption);
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    Choice ch=new Choice();
    for (int i=0; i<topCluster.hierDepth+1; i++)
      ch.add("level "+i+": "+topCluster.getNClustersAtLevel(i)+" clusters");
    ch.select(2);
    putHierClustersToTable(topCluster, ch.getSelectedIndex());
    JScrollPane scpDendrogram=getHierClusteringPanel(topCluster, ch.getSelectedIndex());
    ClustersTable clTable=new ClustersTable(topCluster.getClustersAtLevel(ch.getSelectedIndex()),
        distanceMatrix,exList,ruleRenderer,attrMinMax,minA,maxA,minQ,maxQ);
    scpDendrogram.setPreferredSize(new Dimension(100,200));
    JSplitPane splitPane=new JSplitPane(JSplitPane.VERTICAL_SPLIT,clTable.scrollPane,scpDendrogram);
    splitPane.setOneTouchExpandable(true);
    splitPane.setDividerLocation(frame.getHeight()/2);
    frame.getContentPane().add(ch, BorderLayout.NORTH);
    frame.getContentPane().add(splitPane,BorderLayout.CENTER);

    ch.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        putHierClustersToTable(topCluster, ch.getSelectedIndex());
        ClustersTable clTable=new ClustersTable(topCluster.getClustersAtLevel(ch.getSelectedIndex()),
            distanceMatrix,exList,ruleRenderer,attrMinMax,minA,maxA,minQ,maxQ);
        splitPane.setTopComponent(clTable.scrollPane);
      }
    });

/*
    String vstr=JOptionPane.showInputDialog(FocusManager.getCurrentManager().getActiveWindow(),
        "Hierarchical clustering done\nHierarchy depth is "+topCluster.hierDepth+"\nSet desired N clusters here (max="+exList.size()+")", "7");
        //"Success!",JOptionPane.INFORMATION_MESSAGE);
    int v=Integer.valueOf(vstr);
    int level;
    for (level=0; level<topCluster.hierDepth && topCluster.getNClustersAtLevel(level)<v; level++);
*/
    Dimension size=Toolkit.getDefaultToolkit().getScreenSize();
    frame.pack();
    splitPane.setDividerLocation(0.5);
    frame.setSize(new Dimension(Math.min(frame.getWidth(),Math.round(0.8f*size.width)),
        Math.min(frame.getHeight(),Math.round(0.8f*size.height))));
    frame.setLocation(size.width-frame.getWidth()-30, size.height-frame.getHeight()-50);
    frame.setVisible(true);
    if (frames==null)
      frames=new ArrayList<JFrame>(20);
    frames.add(frame);
    return frame;
  }

  protected void putHierClustersToTable (ClusterContent topCluster, int level) {
    ClusterContent clusters[]=topCluster.getClustersAtLevel(level);
    if (clusters!=null && clusters.length>1) {
      ClustersAssignments clAss=new ClustersAssignments();
      clAss.objIndexes=new int[exList.size()];
      clAss.clusters=new int[exList.size()];
      for (int i=0; i<exList.size(); i++) {
        clAss.objIndexes[i]=i;
        clAss.clusters[i]=-1;
      }
      clAss.minSize=exList.size()+10;
      for (int i=0; i<clusters.length; i++) {
        int n=0;
        for (int j = 0; j < exList.size(); j++)
          if (clusters[i].member[j]) {
            clAss.clusters[j] = i;
            ++n;
          }
        clAss.minSize=Math.min(n,clAss.minSize);
        clAss.maxSize=Math.max(n,clAss.maxSize);
      }
      ExListTableModel eTblModel=(ExListTableModel)table.getModel();
      eTblModel.setCusterAssignments(clAss);
    }
  }
  protected JScrollPane getHierClusteringPanel (ClusterContent topCluster, int level) {
    JPanel pp=topCluster.makePanel();
    if (pp==null) {
      JOptionPane.showMessageDialog(FocusManager.getCurrentManager().getActiveWindow(),
              "Failed to visualize the hierarchy!",
              "Error",JOptionPane.ERROR_MESSAGE);
      return null;
    }
    JScrollPane scp=new JScrollPane(pp);
    return scp;
  }

  public void extractSubset(ItemSelectionManager selector,
                              ArrayList<CommonExplanation> exList,
                              double distanceMatrix[][],
                              Hashtable<String,float[]> attrMinMax) {
    ArrayList selected = selector.getSelected();
    if (selected.size() < 1)
      return;
    ArrayList<CommonExplanation> exSubset = new ArrayList<CommonExplanation>(selected.size());
    int idx[] = new int[selected.size()];
    int nEx = 0;
    for (int i = 0; i < selected.size(); i++)
      if (selected.get(i) instanceof Integer) {
        idx[nEx] = (Integer) selected.get(i);
        exSubset.add(exList.get(idx[nEx]));
        ++nEx;
      }
    double distances[][] = null;
    if (distanceMatrix!=null) {
      distances=new double[nEx][nEx];
      for (int i = 0; i < nEx; i++) {
        distances[i][i] = 0;
        int ii = idx[i];
        for (int j = i + 1; j < nEx; j++) {
          int jj = idx[j];
          distances[i][j] = distances[j][i] = distanceMatrix[ii][jj];
        }
      }
    }
    ShowRules showRules=new ShowRules(exSubset,attrMinMax,distances);
    showRules.setOrigRules(exList.equals(origRules)?exSubset: origRules);
    showRules.setDataInstances(dataInstances,actionsDiffer);
    if (showRules.getOrigRules().equals(origRules)) {
      showRules.setOrigHighlighter(origHighlighter);
      showRules.setOrigSelector(origSelector);
    }
    showRules.setNonSubsumed(this.nonSubsumed);
    showRules.setAggregated(this.aggregated);
    showRules.setAccThreshold(accThreshold);
    showRules.setMaxQDiff(maxQDiff);
    showRules.setExpanded(expanded);
    showRules.setCreatedFileRegister(createdFiles);
    showRules.showRulesInTable();
  }
  
  /**
   * From the given set of rules or explanations, extracts those rules that are not subsumed
   * by any other rules. Shows the resulting rule set in a table view.
   */
  public void getNonSubsumed(ArrayList<CommonExplanation> exList,
                             Hashtable<String,float[]> attrMinMax) {
    if (exList==null || exList.size()<2)
      return;
    boolean noActions=RuleMaster.noActionDifference(exList);
    double maxQDiff=Double.NaN;
    if (noActions) {
      String value=JOptionPane.showInputDialog(FocusManager.getCurrentManager().getActiveWindow(),
          "The rules do not differ in actions (decisions) and can be compared by closeness of " +
              "the Q values. Enter a threshold for the difference in Q :",
          String.format("%.5f",RuleMaster.suggestMaxQDiff(exList)));
      if (value==null)
        return;
      try {
        maxQDiff=Double.parseDouble(value);
        if (maxQDiff<0) {
          JOptionPane.showMessageDialog(FocusManager.getCurrentManager().getActiveWindow(),
              "Illegal threshold value for the Q difference; must be >=0!",
              "Error",JOptionPane.ERROR_MESSAGE);
          return;
        }
      } catch (Exception ex) {
        JOptionPane.showMessageDialog(FocusManager.getCurrentManager().getActiveWindow(),
            "Illegal threshold value for the Q difference!",
            "Error",JOptionPane.ERROR_MESSAGE);
      }
    }
    System.out.println("Trying to reduce the explanation set by removing less general explanations...");
    ArrayList<CommonExplanation> exList2= RuleMaster.removeLessGeneral(exList,origRules,attrMinMax,
        noActions,maxQDiff);
    if (exList2.size()<exList.size()) {
      JOptionPane.showMessageDialog(FocusManager.getCurrentManager().getActiveWindow(),
          "Reduced the number of explanations from " +
                                             exList.size() + " to " + exList2.size(),
          "Reduced rule set",JOptionPane.INFORMATION_MESSAGE);
    }
    else {
      JOptionPane.showMessageDialog(FocusManager.getCurrentManager().getActiveWindow(),
          "Did not manage to reduce the set of explanations!",
          "Fail",JOptionPane.WARNING_MESSAGE);
      return;
    }
    ShowRules showRules=createShowRulesInstance(exList2);
    showRules.setNonSubsumed(true);
    showRules.setAggregated(true);
    showRules.countRightAndWrongRuleApplications();
    showRules.showRulesInTable();
  }

  public void removeContradictory (ArrayList<CommonExplanation> exList) {
    if (exList==null || exList.size()<2)
      return;
    boolean noActions=RuleMaster.noActionDifference(exList);
    double maxQDiff=Double.NaN;
    if (noActions) {
      String value=JOptionPane.showInputDialog(FocusManager.getCurrentManager().getActiveWindow(),
          "The rules do not differ in actions (decisions) and can be compared by closeness of " +
              "the Q values. Enter a threshold for the difference in Q :",
          String.format("%.5f",RuleMaster.suggestMaxQDiff(exList)));
      if (value==null)
        return;
      try {
        maxQDiff=Double.parseDouble(value);
        if (maxQDiff<0) {
          JOptionPane.showMessageDialog(FocusManager.getCurrentManager().getActiveWindow(),
              "Illegal threshold value for the Q difference; must be >=0!",
              "Error",JOptionPane.ERROR_MESSAGE);
          return;
        }
      } catch (Exception ex) {
        JOptionPane.showMessageDialog(FocusManager.getCurrentManager().getActiveWindow(),
            "Illegal threshold value for the Q difference!",
            "Error",JOptionPane.ERROR_MESSAGE);
      }
    }
    System.out.println("Trying to find and remove contradictory rules...");
    ArrayList<CommonExplanation> exList2= RuleMaster.removeContradictory(exList,noActions,maxQDiff);
    if (exList2.size()<exList.size()) {
      JOptionPane.showMessageDialog(FocusManager.getCurrentManager().getActiveWindow(),
          "The removal of contradictory rules has reduced the number of explanations from " +
              exList.size() + " to " + exList2.size(),
          "Removed contradictions!",JOptionPane.INFORMATION_MESSAGE);
    }
    else {
      JOptionPane.showMessageDialog(FocusManager.getCurrentManager().getActiveWindow(),
          "Did not find any contradictory rules!",
          "No contradictions!",JOptionPane.INFORMATION_MESSAGE);
      return;
    }
    ShowRules showRules=createShowRulesInstance(exList2);
    showRules.setOrigRules(exList2);
    showRules.setNonSubsumed(false);
    showRules.setAggregated(false);
    showRules.countRightAndWrongRuleApplications();
    showRules.showRulesInTable();

    ArrayList<UnitedRule> excluded=new ArrayList<UnitedRule>(exList.size()-exList2.size());
    for (CommonExplanation ce:exList)
      if (!exList2.contains(ce)) {
        UnitedRule r=UnitedRule.getRule(ce);
        if (noActions)
          r.countRightAndWrongCoveragesByQ(exList2);
        else
          r.countRightAndWrongCoverages(exList2);
        excluded.add(r);
      }

    showRules=createShowRulesInstance(excluded);
    showRules.setOrigRules(exList2);
    showRules.setNonSubsumed(false);
    showRules.setAggregated(false);
    showRules.countRightAndWrongRuleApplications();
    JFrame frame=showRules.showRulesInTable();
    frame.setTitle("Excluded contradictory rules");
  }

  private long aggrT0=-1;
  
  public void stateChanged(ChangeEvent e) {
    if (e.getSource() instanceof ClustersAssignments) {
      if (orderingInProgress) {
        orderingInProgress=false;
        clOptics.removeChangeListener(this);
      }
    }
    else
    if (e.getSource() instanceof AggregationRunner) {
      AggregationRunner aRun=(AggregationRunner)e.getSource();
      ArrayList<UnitedRule> aggRules=aRun.aggRules;
      if (aggRules==null || aggRules.isEmpty() || aggRules.size()>=exList.size()) {
        JOptionPane.showMessageDialog(FocusManager.getCurrentManager().getActiveWindow(),
            "Failed to aggregate!",
            "Failed",JOptionPane.WARNING_MESSAGE);
        return;
      }
      
      if (aRun.finished) {
        if (aggRules.size()>=exList.size()) {
          JOptionPane.showMessageDialog(FocusManager.getCurrentManager().getActiveWindow(),
              "The given rules could not be aggregated!",
              "Failed",JOptionPane.WARNING_MESSAGE);
          return;
        }
      }

      long tDiff=System.currentTimeMillis()-aggrT0;
      aggrT0=-1;
      System.out.println("Aggregation took "+(1.0*tDiff/60000)+" minutes.");
  
      ArrayList<CommonExplanation> aggEx=new ArrayList<CommonExplanation>(aggRules.size());
      aggEx.addAll(aggRules);
      ShowRules showRules=createShowRulesInstance(aggEx);
      showRules.setNonSubsumed(true);
      showRules.setAggregated(true);
      showRules.setAccThreshold(aRun.currAccuracy);
      if (aRun.aggregateByQ)
        showRules.setMaxQDiff(aRun.maxQDiff);
      showRules.countRightAndWrongRuleApplications();
      showRules.showRulesInTable();
    }
    else
    if (e.getSource().equals(ruleSelector))  {
      if (!ruleSelector.isRunning)
        ruleSelector=null;
      else {
        ArrayList<CommonExplanation> selRules=ruleSelector.getSelectedRules();
        if (selRules!=null) {
          ShowRules showRules=createShowRulesInstance(selRules);
          showRules.setTitle("Subset by query "+ruleSelector.queryStr);
          showRules.showRulesInTable();
        }
      }
    }
  }
  
  public void aggregate(ArrayList<CommonExplanation> exList,
                        Hashtable<String,float[]> attrMinMax) {
    if (exList==null || exList.size()<2)
      return;
  
    boolean noActions=RuleMaster.noActionDifference(exList);
    
    JPanel pDialog=new JPanel();
    pDialog.setLayout(new BoxLayout(pDialog,BoxLayout.Y_AXIS));
    pDialog.add(new JLabel("Set parameters for rule aggregation and generalisation:",JLabel.CENTER));
    JPanel pp=new JPanel(new BorderLayout(10,0));
    pp.add(new JLabel("Coherence threshold from 0 to 1 :",JLabel.RIGHT),BorderLayout.CENTER);
    
    JTextField tfAcc=new JTextField(String.format("%.3f", (float)Math.max(0.6f,accThreshold-0.25f)),5);
    pp.add(tfAcc,BorderLayout.EAST);
    pDialog.add(pp);
    JCheckBox cbData=(dataInstances==null)?null:
                         new JCheckBox("Additionally check the data-based rule fidelity",true);
    if (cbData!=null)
      pDialog.add(cbData);
    
    JTextField tfQDiff=null;
    if (noActions) {
      pDialog.add(Box.createVerticalStrut(5)); // a spacer
      pDialog.add(new JLabel("The rules do not differ in resulting " +
                           "class labels / actions / decisions.",JLabel.CENTER));
      pDialog.add(new JLabel("They can be joined based on close " +
                           "real-valued results (denoted as Q).",JLabel.CENTER));
      pp=new JPanel(new BorderLayout(10,0));
      pp.add(new JLabel("Enter a threshold for the difference in Q :",JLabel.RIGHT),BorderLayout.CENTER);
      tfQDiff=new JTextField(String.format("%.4f",RuleMaster.suggestMaxQDiff(exList)),7);
      pp.add(tfQDiff,BorderLayout.EAST);
      pDialog.add(pp);
    }
    pDialog.add(Box.createVerticalStrut(5)); // a spacer
    /**/
    pDialog.add(new JLabel("The aggregation can be fulfilled in a step-wise manner:",JLabel.CENTER));
    pDialog.add(new JLabel("The process is executed several times", JLabel.CENTER));
    pDialog.add(new JLabel("with decreasing the coherence threshold in each step.", JLabel.CENTER));
    /**/
    pDialog.add(Box.createVerticalStrut(5)); // a spacer
    JCheckBox cbIterative=new JCheckBox("Do step-wise aggregation",false);
    pDialog.add(cbIterative);
    JTextField tfAcc0=new JTextField("1.00",5);
    pp=new JPanel(new BorderLayout(10,0));
    pp.add(new JLabel("Initial c threshold:",JLabel.RIGHT),BorderLayout.CENTER);
    pp.add(tfAcc0,BorderLayout.EAST);
    pDialog.add(pp);
    JTextField tfAccStep=new JTextField("0.05",5);
    pp=new JPanel(new BorderLayout(10,0));
    pp.add(new JLabel("Decrease the threshold in each run by",JLabel.RIGHT),BorderLayout.CENTER);
    pp.add(tfAccStep,BorderLayout.EAST);
    pDialog.add(pp);
  
    int result = JOptionPane.showConfirmDialog(FocusManager.getCurrentManager().getActiveWindow(), pDialog,
        "Rule aggregation parameters", JOptionPane.OK_CANCEL_OPTION);
    if (result != JOptionPane.OK_OPTION)
      return;
    
    double minAccuracy=0;
    double maxQDiff=Double.NaN;
    
    String value=tfAcc.getText();
    if (value == null)
      return;
    try {
      minAccuracy = Double.parseDouble(value);
      if (minAccuracy < 0 || minAccuracy > 1) {
        JOptionPane.showMessageDialog(FocusManager.getCurrentManager().getActiveWindow(),
            "Illegal threshold value for the coherence; must be from 0 to 1!",
            "Error", JOptionPane.ERROR_MESSAGE);
        return;
      }
    } catch (Exception ex) {
      JOptionPane.showMessageDialog(FocusManager.getCurrentManager().getActiveWindow(),
          "Illegal threshold value for the coherence!",
          "Error", JOptionPane.ERROR_MESSAGE);
      return;
    }
    
    if (noActions) {
      value=tfQDiff.getText();
      if (value==null)
        return;
      try {
        maxQDiff=Double.parseDouble(value);
        if (maxQDiff<0) {
          JOptionPane.showMessageDialog(FocusManager.getCurrentManager().getActiveWindow(),
              "Illegal threshold value for the Q difference; must be >=0!",
              "Error",JOptionPane.ERROR_MESSAGE);
          return;
        }
      } catch (Exception ex) {
        JOptionPane.showMessageDialog(FocusManager.getCurrentManager().getActiveWindow(),
            "Illegal threshold value for the Q difference!",
            "Error",JOptionPane.ERROR_MESSAGE);
      }
    }
    
    boolean checkWithData=cbData!=null && cbData.isSelected();

    Window win=FocusManager.getCurrentManager().getActiveWindow();
    System.out.println("Trying to reduce the explanation set by removing less general explanations...");
    JDialog dialog=new JDialog(win, "Trying to remove less general rules ...",
        Dialog.ModalityType.MODELESS);
    dialog.setLayout(new BorderLayout());
    JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 50, 20));
    p.add(new JLabel("Trying to reduce the explanation set " +
        "by removing less general explanations...", JLabel.CENTER));
    dialog.getContentPane().add(p, BorderLayout.CENTER);
    dialog.pack();
    dialog.setLocationRelativeTo(win);
    dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
    dialog.setVisible(true);

    ArrayList<CommonExplanation> exList2= RuleMaster.removeLessGeneral(exList,origRules,attrMinMax,
      noActions,maxQDiff);
    dialog.dispose();

    if (exList2.size()<exList.size()) {
      JOptionPane.showMessageDialog(FocusManager.getCurrentManager().getActiveWindow(),
          "After excluding the subsumed (less general) rules, the number of the rules has decreased from " +
              exList.size() + " to " + exList2.size(),
          "Reduced rule set",JOptionPane.INFORMATION_MESSAGE);
      exList=exList2;
    }

    System.out.println("Trying to aggregate the rules...");

    ArrayList<UnitedRule> rules=UnitedRule.getRules(exList);
    ArrayList<UnitedRule> aggRules=null;
    AbstractList<Explanation> data=(checkWithData)?dataInstances:null;
    
    AggregationRunner aggRunner=new AggregationRunner(rules,origRules,attrMinMax,data);
    aggRunner.setOwner(this);
    aggRunner.setAggregateByQ(noActions);
    aggRunner.setMinAccuracy(minAccuracy);
    aggRunner.setMaxQDiff(maxQDiff);
    
    if (cbIterative.isSelected()) {
      double initAccuracy=Double.NaN;
      value=tfAcc0.getText();
      if (value!=null)
        try {
          initAccuracy=Float.parseFloat(value);
        } catch (Exception ex) {}
      if (Double.isNaN(initAccuracy) || initAccuracy<0.5 || initAccuracy>1) {
        result = JOptionPane.showConfirmDialog (FocusManager.getCurrentManager().getActiveWindow(),
            "Illegal value for the initial coherence threshold! Should it be set to 1?",
            "Illegal initial threshold value!",JOptionPane.YES_NO_OPTION);
        if (result != JOptionPane.YES_OPTION)
          return;
        initAccuracy=1;
      }
      double accStep=Double.NaN;
      value=tfAccStep.getText();
      if (value!=null)
        try {
          accStep=Double.parseDouble(value);
        } catch (Exception ex) {}
      if (Double.isNaN(accStep) || accStep<=0 || initAccuracy-accStep<=0) {
        result = JOptionPane.showConfirmDialog (FocusManager.getCurrentManager().getActiveWindow(),
            "Illegal value for the threshold decrement in each step! Should it be set to 0.1?",
            "Illegal value for threshold decrement!",JOptionPane.YES_NO_OPTION);
        if (result != JOptionPane.YES_OPTION)
          return;
        accStep=0.1;
      }
      aggRunner.setIterationParameters(initAccuracy,accStep);
    }

    aggrT0=System.currentTimeMillis();

    aggRunner.aggregate(FocusManager.getCurrentManager().getActiveWindow());
    
  }
  
  public void showAggregationInProjection(ItemSelectionManager selector) {
    if (exList==null || exList.isEmpty() || !(exList.get(0) instanceof UnitedRule))
      return;
    boolean applyToSelection=
        selector.hasSelection() &&
            JOptionPane.showConfirmDialog(FocusManager.getCurrentManager().getActiveWindow(),
                "Apply the operation to the selected subset?",
                "Apply to selection?",JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE)
                ==JOptionPane.YES_OPTION;
    ArrayList rules=(applyToSelection)?getSelectedRules(exList,selector):exList;
    ArrayList<CommonExplanation> origList=new ArrayList<CommonExplanation>(rules.size());
    HashSet<ArrayList<Vertex>> graphs=null;
    ArrayList<Integer> unionIds=new ArrayList<Integer>(rules.size()*5);
    for (int i=0; i<rules.size(); i++) {
      UnitedRule rule=(UnitedRule)rules.get(i);
      if (rule.fromRules==null || rule.fromRules.isEmpty()) {
        origList.add(rule);
        unionIds.add(i);
        continue;
      }
      //create a graph
      ArrayList<UnitedRule> ruleGroup=addOrigRules(rule,null);
      if (ruleGroup.size()>1) { //create a graph
        ArrayList<Vertex> graph=new ArrayList<Vertex>(ruleGroup.size());
        for (int j=0; j<ruleGroup.size(); j++)
          graph.add(new Vertex(Integer.toString(j+origList.size())));
        for (int j1=0; j1<ruleGroup.size()-1; j1++) {
          Vertex v=graph.get(j1);
          for (int j2 = j1 + 1; j2 < ruleGroup.size(); j2++) {
            double d = UnitedRule.distance(ruleGroup.get(j1), ruleGroup.get(j2), attrMinMax);
            if (Double.isNaN(d)) {
              System.out.println("!!! NaN distance value !!!");
              System.out.println(ruleGroup.get(j1).toString());
              System.out.println(ruleGroup.get(j2).toString());
            }
            else {
              Edge e=new Edge((float) d);
              Vertex v2=graph.get(j2);
              v.addEdge(v2, e);
              v2.addEdge(v, e);
            }
          }
        }
        Prim prim=new Prim(graph);
        prim.run();
        if (graphs==null)
          graphs=new HashSet<ArrayList<Vertex>>(rules.size());
        graphs.add(graph);
      }
      for (int j=0; j<ruleGroup.size(); j++) {
        origList.add(ruleGroup.get(j));
        unionIds.add(i);
      }
    }
    
    TSNE_Runner tsne=new TSNE_Runner();
    tsne.setFileRegister(createdFiles);
    String value=JOptionPane.showInputDialog(FocusManager.getCurrentManager().getActiveWindow(),
        "Perplexity (integer; suggested range from 5 to 50) :",
        tsne.getPerplexity());
    if (value==null)
      return;
    try {
      int p=Integer.parseInt(value);
      if (p<5 || p>100) {
        JOptionPane.showMessageDialog(FocusManager.getCurrentManager().getActiveWindow(),
            "Illegal perplexity: "+p,
            "Error",JOptionPane.ERROR_MESSAGE);
        return;
      }
      tsne.setPerplexity(p);
    } catch (Exception ex) {
      JOptionPane.showMessageDialog(FocusManager.getCurrentManager().getActiveWindow(),
          "Illegal perplexity!"+value,
          "Error",JOptionPane.ERROR_MESSAGE);
      return;
    }
    
    double d[][]=CommonExplanation.computeDistances(origList,featuresInDistances,attrMinMax);
  
    ExplanationsProjPlot2D pp=new ExplanationsProjPlot2D(attrMinMax,attrs,minmax);
    pp.setExplanations(origList,origRules);
    pp.setDistanceMatrix(d);
    pp.setFeatures(getSelectedFeatures());
    pp.setGraphs(graphs);
    int uIds[]=new int[unionIds.size()];
    for (int j=0; j<unionIds.size(); j++)
      uIds[j]=unionIds.get(j);
    pp.setUnionIds(uIds);
    pp.setProjectionProvider(tsne);
    pp.setPreferredSize(new Dimension(800,800));
  
    Translator translator=(origRules!=null && (origHighlighter!=null || origSelector!=null))?
                              createTranslator(origList,origRules):null;
    if (translator!=null) {
      if (origHighlighter!=null) {
        HighlightTranslator hTrans=new HighlightTranslator();
        hTrans.setTranslator(translator);
        hTrans.setOtherHighlighter(origHighlighter);
        pp.setHighlighter(hTrans);
      }
      if (origSelector!=null) {
        SelectTranslator sTrans=new SelectTranslator();
        sTrans.setTranslator(translator);
        sTrans.setOtherSelector(origSelector);
        pp.setSelector(sTrans);
      }
    }
  
    Dimension size=Toolkit.getDefaultToolkit().getScreenSize();
    JFrame plotFrame=new JFrame(pp.getProjectionProvider().getProjectionTitle());
    plotFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    plotFrame.getContentPane().add(pp);
    plotFrame.pack();
    plotFrame.setLocation(size.width-plotFrame.getWidth()-30, size.height-plotFrame.getHeight()-50);
    plotFrame.setVisible(true);
    if (frames==null)
      frames=new ArrayList<JFrame>(20);
    frames.add(plotFrame);
  
    JPopupMenu menu=new JPopupMenu();
    
    JMenuItem mitSelectLinked=new JMenuItem("Select linked items");
    menu.add(mitSelectLinked);
    mitSelectLinked.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        pp.selectLinkedToSelected();
      }
    });
  
    JMenuItem mitSelectUnions=new JMenuItem("Select the union(s) of the linked items");
    menu.add(mitSelectUnions);
    mitSelectUnions.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        ArrayList<Integer> uIds=pp.getUnionIdsOfSelected();
        if (uIds!=null)
          selector.select(uIds);
      }
    });
    
    JMenuItem mitExtract=new JMenuItem("Extract the selected subset to a separate view");
    menu.add(mitExtract);
    mitExtract.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        extractSubset(pp.getSelector(),origList,d,attrMinMax);
      }
    });
  
    JMenuItem mit=new JMenuItem("Re-run t-SNE with another perplexity setting");
    menu.add(mit);
    mit.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        String value=JOptionPane.showInputDialog(FocusManager.getCurrentManager().getActiveWindow(),
            "Perplexity (integer; suggested range from 5 to 50) :",
            tsne.getPerplexity());
        if (value==null)
          return;
        try {
          int p=Integer.parseInt(value);
          if (p<5 || p>100) {
            JOptionPane.showMessageDialog(FocusManager.getCurrentManager().getActiveWindow(),
                "Illegal perplexity: "+p,
                "Error",JOptionPane.ERROR_MESSAGE);
            return;
          }
          tsne.setPerplexity(p);
          tsne.runAlgorithm();
        } catch (Exception ex) {
          JOptionPane.showMessageDialog(FocusManager.getCurrentManager().getActiveWindow(),
              "Illegal perplexity: "+value,
              "Error",JOptionPane.ERROR_MESSAGE);
          return;
        }
      }
    });
  
    pp.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        super.mousePressed(e);
        if (e.getButton()>MouseEvent.BUTTON1) {
          ArrayList selected=pp.getSelector().getSelected();
          boolean someSelected=selected!=null && selected.size()>0;
          mitExtract.setEnabled(someSelected);
          mitSelectLinked.setEnabled(someSelected);
          mitSelectUnions.setEnabled(someSelected);
          menu.show(pp,e.getX(),e.getY());
        }
      }
    });
  }
  
  protected static ArrayList<UnitedRule> addOrigRules(UnitedRule rule, ArrayList<UnitedRule> origList) {
    if (rule==null)
      return origList;
    if (origList==null)
      origList=new ArrayList<UnitedRule>(20);
    if (rule.fromRules==null || rule.fromRules.isEmpty())
      origList.add(rule);
    else
      for (UnitedRule r:rule.fromRules)
        addOrigRules(r,origList);
    return origList;
  }
  
  /**
   * For two lists consisting (at least partly) of same objects finds correspondences between
   * the indexes of the common objects and creates a translator that will keep the correspondences.
   */
  public static Translator createTranslator(ArrayList list1, ArrayList list2) {
    if (list1==null || list2==null || list1.isEmpty() || list2.isEmpty())
      return null;
    ArrayList<Object[]> pairs=new ArrayList<Object[]>(Math.min(list1.size(),list2.size()));
    for (int i=0; i<list1.size(); i++) {
      int idx=list2.indexOf(list1.get(i));
      if (idx>=0) {
        Object pair[]={new Integer(i),new Integer(idx)};
        pairs.add(pair);
      }
    }
    if (pairs.isEmpty())
      return null;
    Translator trans=new Translator();
    trans.setPairs(pairs);
    return trans;
  }
}
