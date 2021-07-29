package TapasExplTreeViewer.ui;

import TapasDataReader.CommonExplanation;
import TapasExplTreeViewer.MST.Edge;
import TapasExplTreeViewer.MST.Prim;
import TapasExplTreeViewer.MST.Vertex;
import TapasExplTreeViewer.clustering.ClustererByOPTICS;
import TapasExplTreeViewer.clustering.ReachabilityPlot;
import TapasExplTreeViewer.rules.RuleMaster;
import TapasExplTreeViewer.rules.UnitedRule;
import TapasExplTreeViewer.util.CoordinatesReader;
import TapasExplTreeViewer.util.MatrixWriter;
import TapasExplTreeViewer.vis.ExplanationsProjPlot2D;
import TapasExplTreeViewer.vis.ProjectionPlot2D;
import TapasExplTreeViewer.vis.TSNE_Runner;
import TapasUtilities.ItemSelectionManager;
import TapasUtilities.RenderLabelBarChart;
import TapasUtilities.SingleHighlightManager;
import TapasUtilities.TableRowsSelectionManager;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;

public class ShowRules {
  
  public static Border highlightBorder=new LineBorder(ProjectionPlot2D.highlightColor,1);
  /**
   * The very original rule set (before any transformations have been applied)
   */
  public ArrayList<CommonExplanation> origRules =null;
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
   * Whether the rule set has been previously reduced by removing rules
   * subsumed in more general rules.
   */
  public boolean nonSubsumed =false;
  /**
   * Whether the rule set consists of generalized rules obtained by aggregation.
   */
  public boolean aggregated=false;
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
  
  protected ArrayList<JFrame> frames=null;
  protected ArrayList<File> createdFiles=null;
  /**
   * A top frame is created once for each instance of ShowRules.
   * When a top frame is closed, all other frames created by this instance of ShowRules
   * are also closed.
   * When the last top frame is closed, the temporary files that have been created
   * are esased.
   */
  protected static ArrayList<JFrame> topFrames=null;
  /**
   * Whether this instance of ShowRules has already created its top frame
   */
  protected boolean topFrameCreated=false;
  
  
  public ShowRules(ArrayList<CommonExplanation> exList,
                   Hashtable<String,float[]> attrMinMax,
                   double distances[][]) {
    this.exList=exList; this.attrMinMax=attrMinMax;
    this.distanceMatrix=distances;
    if (distanceMatrix==null) {
      System.out.println("Computing distance matrix...");
      distanceMatrix = CommonExplanation.computeDistances(exList, attrMinMax);
      if (distanceMatrix == null)
        System.out.println("Failed to compute a matrix of distances between the rules (explanations)!");
      else
        System.out.println("Distance matrix ready!");
    }
  }
  
  public ShowRules(ArrayList<CommonExplanation> exList, Hashtable<String,float[]> attrMinMax) {
    this(exList,attrMinMax,null);
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
  
  public JFrame showRulesInTable() {
    return showRulesInTable(exList,distanceMatrix,attrMinMax);
  }
  
  public JFrame showRulesInTable(ArrayList rules,
                                 double distanceMatrix[][],
                                 Hashtable<String,float[]> attrMinMax) {
    boolean showOriginalRules=rules.equals(origRules);
    if (showOriginalRules && origHighlighter==null) {
      origHighlighter=new SingleHighlightManager();
      origSelector=new ItemSelectionManager();
    }
    
    SingleHighlightManager highlighter=(showOriginalRules)?origHighlighter:new SingleHighlightManager();
    ItemSelectionManager selector=(showOriginalRules)?origSelector:new ItemSelectionManager();
    
    ClustererByOPTICS clOptics=(distanceMatrix!=null && distanceMatrix.length>5)?new ClustererByOPTICS():null;
    if (clOptics!=null) {
      clOptics.setDistanceMatrix(distanceMatrix);
      clOptics.setHighlighter(highlighter);
      clOptics.setSelector(selector);
      clOptics.doClustering();
    }
    
    int minAction=Integer.MAX_VALUE, maxAction=Integer.MIN_VALUE;
    double minQValue=Double.NaN, maxQValue=Double.NaN;
    for (int i=0; i<rules.size(); i++) {
      CommonExplanation ex = (CommonExplanation) rules.get(i);
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
    if (clOptics!=null)
      clOptics.addChangeListener(eTblModel);
    
    JTable table=new JTable(eTblModel){
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
          return ((CommonExplanation)rules.get(realRowIndex)).toHTML(attrMinMax,s);
        }
        highlighter.clearHighlighting();
        return "";
      }
    
      /**/
      public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component c = super.prepareRenderer(renderer, row, column);
        Color bkColor=(isRowSelected(row))?getSelectionBackground():getBackground();
        int rowIdx=convertRowIndexToModel(row), colIdx=convertColumnIndexToModel(column);
        String colName=eTblModel.getColumnName(colIdx);
        boolean isCluster=colName.equalsIgnoreCase("cluster");
        if (isCluster)
          bkColor= ReachabilityPlot.getColorForCluster((Integer)eTblModel.getValueAt(rowIdx,colIdx));
        boolean isAction=false, isQ=false;
        if (!isCluster && minA<maxA && colName.equalsIgnoreCase("action")) {
          bkColor = ExplanationsProjPlot2D.getColorForAction((Integer) eTblModel.getValueAt(rowIdx, colIdx),
              minA, maxA);
          isAction=true;
        }
        if (!isCluster && minQ<maxQ && colName.toUpperCase().endsWith(" Q")) {
          bkColor = ExplanationsProjPlot2D.getColorForQ(new Double((Float)eTblModel.getValueAt(rowIdx, colIdx)),
              minQ, maxQ);
          isQ=true;
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
    for (int i=0; i<eTblModel.columnNames.length; i++)
      if ((eTblModel.getColumnClass(i).equals(Integer.class) || eTblModel.getColumnClass(i).equals(Float.class) || eTblModel.getColumnClass(i).equals(Double.class)) &&
              !eTblModel.getColumnName(i).equalsIgnoreCase("cluster"))
        table.getColumnModel().getColumn(i).setCellRenderer(
            new RenderLabelBarChart(eTblModel.getColumnMin(i),eTblModel.getColumnMax(i)));
    for (int i=eTblModel.columnNames.length; i<eTblModel.getColumnCount(); i++)
      table.getColumnModel().getColumn(i).setCellRenderer(new JLabel_Subinterval());
  
    /**/
    TableRowsSelectionManager rowSelMan=new TableRowsSelectionManager();
    rowSelMan.setTable(table);
    rowSelMan.setHighlighter(highlighter);
    rowSelMan.setSelector(selector);
    rowSelMan.setMayScrollTable(false);
    /**/
    
    JPopupMenu menu=new JPopupMenu();

    JCheckBoxMenuItem cbmit=new JCheckBoxMenuItem("Show texts for intervals",false);
    menu.add(cbmit);
    cbmit.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        for (int i=eTblModel.columnNames.length; i<eTblModel.getColumnCount(); i++) {
          TableCellRenderer tcr=table.getColumnModel().getColumn(i).getCellRenderer();
          if (tcr instanceof JLabel_Subinterval) {
            ((JLabel_Subinterval)tcr).setDrawTexts(cbmit.getState());
          }
        }
        eTblModel.fireTableDataChanged();
      }
    });
    menu.addSeparator();

    JMenuItem mit=new JMenuItem("Show the OPTICS reachability plot");
    menu.add(mit);
    mit.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        JFrame fr=clOptics.showPlot();
        if (fr!=null)
          fr.toFront();;
      }
    });
    
    mit=new JMenuItem("Show the t-SNE projection");
    menu.add(mit);
    mit.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        showProjection(rules,distanceMatrix,highlighter,selector);
      }
    });
    
    if (aggregated) {
      mit=new JMenuItem("Show the links between the original rules in a t-SNE projection");
      menu.add(mit);
      mit.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
         showAggregationInProjection();
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
    
    menu.addSeparator();
    menu.add(mit=new JMenuItem("Extract the non-subsumed rules to a separate view"));
    mit.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        getNonSubsumed(exList,attrMinMax);
      }
    });
  
    menu.addSeparator();
    menu.add(mit=new JMenuItem("Otain generalized rules through aggregation"));
    mit.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        String value=JOptionPane.showInputDialog(FocusManager.getCurrentManager().getActiveWindow(),
            "Accuracy threshold from 0 to 1 :",
            String.format("%.3f",accThreshold));
        if (value==null)
          return;
        try {
          double d=Double.parseDouble(value);
          if (d<0 || d>1) {
            JOptionPane.showMessageDialog(FocusManager.getCurrentManager().getActiveWindow(),
                "Illegal threshold value for the accuracy; must be from 0 to 1!",
                "Error",JOptionPane.ERROR_MESSAGE);
            return;
          }
          aggregate(exList,attrMinMax,d);
        } catch (Exception ex) {
          JOptionPane.showMessageDialog(FocusManager.getCurrentManager().getActiveWindow(),
              "Illegal threshold value for the accuracy!",
              "Error",JOptionPane.ERROR_MESSAGE);
          return;
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
    });

    JScrollPane scrollPane = new JScrollPane(table);
    
    String title=((aggregated)?"Aggregated rules":
                      (nonSubsumed)?"Extracted non-subsumed rules":
                          "Original distinct rules or explanations")+
                     " (" + rules.size() + ")"+
                     ((aggregated)?"; obtained with accuracy threshold "+
                                       String.format("%.3f",accThreshold):"");
    if (maxQDiff>0)
      title+=" and max Q difference "+String.format("%.5f",maxQDiff);
  
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
    return fr;
  }
  
  public void eraseCreatedFiles () {
    if (createdFiles!=null && !createdFiles.isEmpty())
      for (File f : createdFiles)
        f.delete();
    createdFiles.clear();
  }
  
  public JFrame showProjection(ArrayList<CommonExplanation> exList,
                               double distanceMatrix[][],
                               SingleHighlightManager highlighter,
                               ItemSelectionManager selector){
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
    
    ExplanationsProjPlot2D pp=new ExplanationsProjPlot2D();
    pp.setExplanations(exList);
    pp.setDistanceMatrix(distanceMatrix);
    pp.setProjectionProvider(tsne);
    pp.setHighlighter(highlighter);
    pp.setSelector(selector);
    pp.setPreferredSize(new Dimension(800,800));
  
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
        ExplanationsProjPlot2D anotherPlot=new ExplanationsProjPlot2D(exList,coords);
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
    double distances[][] = new double[nEx][nEx];
    for (int i = 0; i < nEx; i++) {
      distances[i][i] = 0;
      int ii = idx[i];
      for (int j = i + 1; j < nEx; j++) {
        int jj = idx[j];
        distances[i][j] = distances[j][i] = distanceMatrix[ii][jj];
      }
    }
    ShowRules showRules=new ShowRules(exSubset,attrMinMax,distances);
    showRules.setOrigRules(exList.equals(origRules)?exSubset: origRules);
    if (showRules.getOrigRules().equals(origRules)) {
      showRules.setOrigHighlighter(origHighlighter);
      showRules.setOrigSelector(origSelector);
    }
    showRules.setNonSubsumed(this.nonSubsumed);
    showRules.setAggregated(this.aggregated);
    showRules.setAccThreshold(accThreshold);
    showRules.setMaxQDiff(maxQDiff);
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
    System.out.println("Trying to reduce the explanation set by removing less general explanations...");
    ArrayList<CommonExplanation> exList2= RuleMaster.removeLessGeneral(exList,origRules,attrMinMax);
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
    ShowRules showRules=new ShowRules(exList2,attrMinMax);
    showRules.setOrigRules(origRules);
    showRules.setOrigHighlighter(origHighlighter);
    showRules.setOrigSelector(origSelector);
    showRules.setNonSubsumed(true);
    showRules.setCreatedFileRegister(createdFiles);
    showRules.showRulesInTable();
  }
  
  public void aggregate(ArrayList<CommonExplanation> exList,
                        Hashtable<String,float[]> attrMinMax,
                        double minAccuracy) {
    if (exList==null || exList.size()<2)
      return;
    if (!nonSubsumed) {
      System.out.println("Prior to aggregation, trying to remove less general explanations...");
      ArrayList<CommonExplanation> exList2= RuleMaster.removeLessGeneral(exList, origRules,attrMinMax);
      if  (exList2!=null && exList2.size()<exList.size()) {
        JOptionPane.showMessageDialog(FocusManager.getCurrentManager().getActiveWindow(),
            "Removal of subsumed rules has reduced the number of explanations from " +
                               exList.size() + " to " + exList2.size(),
            "Reduced rule set",JOptionPane.INFORMATION_MESSAGE);
        exList = exList2;
      }
    }
    boolean noActions=RuleMaster.noActionDifference(exList);
    double maxQDiff=Double.NaN;
    if (noActions) {
      String value=JOptionPane.showInputDialog(FocusManager.getCurrentManager().getActiveWindow(),
          "The rules do not differ in actions (decisions) and can be aggregated by closeness of " +
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
    System.out.println("Trying to aggregate the rules...");
    ArrayList<UnitedRule> aggRules=(noActions)?RuleMaster.aggregateByQ(UnitedRule.getRules(exList),maxQDiff,
        origRules,minAccuracy,attrMinMax):
        RuleMaster.aggregate(UnitedRule.getRules(exList), origRules,minAccuracy,attrMinMax);
    if (aggRules==null || aggRules.size()>=exList.size()) {
      JOptionPane.showMessageDialog(FocusManager.getCurrentManager().getActiveWindow(),
          "Failed to aggregate!",
          "Fail",JOptionPane.WARNING_MESSAGE);
      return;
    }
    JOptionPane.showMessageDialog(FocusManager.getCurrentManager().getActiveWindow(),
        "Reduced the number of explanations from " +
                           exList.size() + " to " + aggRules.size(),
        "Aggregated rule set",JOptionPane.INFORMATION_MESSAGE);
    ArrayList<CommonExplanation> aggEx=new ArrayList<CommonExplanation>(aggRules.size());
    aggEx.addAll(aggRules);
    ShowRules showRules=new ShowRules(aggEx,attrMinMax);
    showRules.setOrigRules(origRules);
    showRules.setOrigHighlighter(origHighlighter);
    showRules.setOrigSelector(origSelector);
    showRules.setNonSubsumed(true);
    showRules.setAggregated(true);
    showRules.setAccThreshold(minAccuracy);
    if (!Double.isNaN(maxQDiff))
      showRules.setMaxQDiff(maxQDiff);
    showRules.setCreatedFileRegister(createdFiles);
    showRules.showRulesInTable();
  }
  
  public void showAggregationInProjection() {
    if (exList==null || exList.isEmpty() || !(exList.get(0) instanceof UnitedRule))
      return;
    ArrayList<CommonExplanation> origList=new ArrayList<CommonExplanation>(exList.size());
    HashSet<ArrayList<Vertex>> graphs=null;
    for (int i=0; i<exList.size(); i++) {
      UnitedRule rule=(UnitedRule)exList.get(i);
      if (rule.fromRules==null || rule.fromRules.isEmpty()) {
        origList.add(rule);
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
            Edge e=new Edge((float)d);
            Vertex v2=graph.get(j2);
            v.addEdge(v2,e);
            v2.addEdge(v,e);
          }
        }
        Prim prim=new Prim(graph);
        prim.run();
        if (graphs==null)
          graphs=new HashSet<ArrayList<Vertex>>(exList.size());
        graphs.add(graph);
      }
      for (int j=0; j<ruleGroup.size(); j++)
        origList.add(ruleGroup.get(j));
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
    
    double d[][]=CommonExplanation.computeDistances(origList,attrMinMax);
  
    ExplanationsProjPlot2D pp=new ExplanationsProjPlot2D();
    pp.setExplanations(origList);
    pp.setDistanceMatrix(d);
    pp.setGraphs(graphs);
    pp.setProjectionProvider(tsne);
    pp.setPreferredSize(new Dimension(800,800));
  
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
}
