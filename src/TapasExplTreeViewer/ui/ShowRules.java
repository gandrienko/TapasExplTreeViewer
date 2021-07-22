package TapasExplTreeViewer.ui;

import TapasDataReader.CommonExplanation;
import TapasExplTreeViewer.clustering.ClustererByOPTICS;
import TapasExplTreeViewer.clustering.ReachabilityPlot;
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
import java.util.Hashtable;

public class ShowRules {
  
  public static Border highlightBorder=new LineBorder(ProjectionPlot2D.highlightColor,1);
  /**
   * The original rules or explanations to be visualized
   */
  public ArrayList<CommonExplanation> exList=null;
  /**
   * The ranges of feature values
   */
  public Hashtable<String,int[]> attrMinMax=null;
  /**
   * The distances between the rules
   */
  protected double distanceMatrix[][]=null;
  
  protected ArrayList<JFrame> frames=null;
  protected ArrayList<File> createdFiles=null;
  
  
  public ShowRules(ArrayList<CommonExplanation> exList,
                   Hashtable<String,int[]> attrMinMax,
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
  
  public ShowRules(ArrayList<CommonExplanation> exList, Hashtable<String,int[]> attrMinMax) {
    this(exList,attrMinMax,null);
  }
  
  public JFrame showRulesInTable() {
    return showRulesInTable(exList,distanceMatrix,attrMinMax);
  }
  
  public JFrame showRulesInTable(ArrayList rules,
                                 double distanceMatrix[][],
                                 Hashtable<String,int[]> attrMinMax) {
  
    SingleHighlightManager highlighter=new SingleHighlightManager();
    ItemSelectionManager selector=new ItemSelectionManager();

    ClustererByOPTICS clOptics=(distanceMatrix!=null)?new ClustererByOPTICS():null;
    if (clOptics!=null) {
      clOptics.setDistanceMatrix(distanceMatrix);
      clOptics.setHighlighter(highlighter);
      clOptics.setSelector(selector);
      clOptics.doClustering();
    }
  
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
          return ((CommonExplanation)rules.get(realRowIndex)).toHTML();
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
        boolean isAction=!isCluster && colName.equalsIgnoreCase("action");
        if (isAction)
          bkColor= ExplanationsProjPlot2D.getColorForAction((Integer)eTblModel.getValueAt(rowIdx,colIdx));
        c.setBackground(bkColor);
        if (highlighter==null || highlighter.getHighlighted()==null ||
                ((Integer)highlighter.getHighlighted())!=rowIdx) {
          ((JComponent) c).setBorder(null);
          return c;
        }
        ((JComponent) c).setBorder(highlightBorder);
        if (!isCluster && !isAction)
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
      if (eTblModel.getColumnClass(i).equals(Integer.class) &&
              !eTblModel.getColumnName(i).equalsIgnoreCase("cluster"))
        table.getColumnModel().getColumn(i).setCellRenderer(
            new RenderLabelBarChart(0,eTblModel.getColumnMax(i)));
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
    JMenuItem mit=new JMenuItem("Show the OPTICS reachability plot");
    menu.add(mit);
    mit.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        JFrame fr=clOptics.showPlot();
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
    
    JMenuItem mitExtract=new JMenuItem("Extract the selected subset to a separate view");
    menu.add(mitExtract);
    mitExtract.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        extractSubset(selector,rules,distanceMatrix,attrMinMax);
      }
    });
  
    table.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
      super.mousePressed(e);
      if (e.getButton()>MouseEvent.BUTTON1) {
        ArrayList selected=selector.getSelected();
        mitExtract.setEnabled(selected!=null && selected.size()>5);
        menu.show(table,e.getX(),e.getY());
      }
      }
    });

    JScrollPane scrollPane = new JScrollPane(table);
  
    JFrame fr = new JFrame("Explanations (" + rules.size() + ")");
    fr.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    fr.getContentPane().add(scrollPane, BorderLayout.CENTER);
    //Display the window.
    fr.pack();
    fr.setLocation(30, 30);
    fr.setVisible(true);
    
    if (frames==null)
      frames=new ArrayList<JFrame>(20);
    frames.add(fr);
    
    fr.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        super.windowClosing(e);
        frames.remove(fr);
        if (frames.isEmpty() && !createdFiles.isEmpty())
          for (File f:createdFiles)
            f.delete();
      }
    });
    return fr;
  }
  
  public JFrame showProjection(ArrayList<CommonExplanation> exList,
                               double distanceMatrix[][],
                               SingleHighlightManager highlighter,
                               ItemSelectionManager selector){
    ExplanationsProjPlot2D pp=new ExplanationsProjPlot2D();
    pp.setExplanations(exList);
    pp.setDistanceMatrix(distanceMatrix);
    TSNE_Runner tsne=new TSNE_Runner();
    tsne.setFileRegister(createdFiles);
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
    return plotFrame;
  }
  
  
  public JFrame extractSubset(ItemSelectionManager selector,
                              ArrayList<CommonExplanation> exList,
                              double distanceMatrix[][],
                              Hashtable<String,int[]> attrMinMax) {
    ArrayList selected = selector.getSelected();
    if (selected.size() < 5)
      return null;
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
    return showRulesInTable(exSubset,distances,attrMinMax);
  }
}
