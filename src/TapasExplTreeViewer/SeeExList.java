package TapasExplTreeViewer;

import TapasDataReader.CommonExplanation;
import TapasDataReader.Flight;
import TapasExplTreeViewer.clustering.ClustererByOPTICS;
import TapasExplTreeViewer.clustering.ReachabilityPlot;
import TapasExplTreeViewer.ui.ExListTableModel;
import TapasExplTreeViewer.ui.JLabel_Subinterval;
import TapasExplTreeViewer.util.CoordinatesReader;
import TapasExplTreeViewer.util.MatrixWriter;
import TapasExplTreeViewer.vis.ExplanationsProjPlot2D;
import TapasUtilities.TableRowsSelectionManager;
import TapasExplTreeViewer.vis.ProjectionPlot2D;
import TapasUtilities.ItemSelectionManager;
import TapasUtilities.MySammonsProjection;
import TapasUtilities.RenderLabelBarChart;
import TapasUtilities.SingleHighlightManager;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;
import java.util.TreeSet;

public class SeeExList {
  
  public static Border highlightBorder=new LineBorder(ProjectionPlot2D.highlightColor,1);

  public static void main(String[] args) {
    String parFileName = (args != null && args.length > 0) ? args[0] : "params.txt";
  
    String path=null;
    Hashtable<String,String> fNames=new Hashtable<String,String>(10);
    try {
      BufferedReader br = new BufferedReader(
          new InputStreamReader(
              new FileInputStream(new File(parFileName)))) ;
      String strLine;
      try {
        while ((strLine = br.readLine()) != null) {
          String str=strLine.replaceAll("\"","").replaceAll(" ","");
          String[] tokens=str.split("=");
          if (tokens==null || tokens.length<2)
            continue;
          String parName=tokens[0].trim().toLowerCase();
          if (parName.equals("path") || parName.equals("data_path"))
            path=tokens[1].trim();
          else
            fNames.put(parName,tokens[1].trim());
        }
      } catch (IOException io) {
        System.out.println(io);
      }
    } catch (IOException io) {
      System.out.println(io);
    }
    if (path!=null) {
      for (Map.Entry<String,String> e:fNames.entrySet()) {
        String fName=e.getValue();
        if (!fName.startsWith("\\") && !fName.contains(":\\")) {
          fName=path+fName;
          fNames.put(e.getKey(),fName);
        }
      }
    }
    else
      path="";
  
    String fName=fNames.get("decisions");
    if (fName==null) {
      System.out.println("No decisions file name in the parameters!");
      return;
    }
  
    System.out.println("Decisions file name = "+fName);
    /**/
    TreeSet<Integer> steps=TapasDataReader.Readers.readStepsFromDecisions(fName);
    //System.out.println(steps);
    Hashtable<String, Flight> flights=
        TapasDataReader.Readers.readFlightDelaysFromDecisions(fName,steps);
    if (flights==null || flights.isEmpty()) {
      System.out.println("Failed to get flight data!");
      return;
    }
    Hashtable<String,int[]> attrMinMax=new Hashtable<String, int[]>();
    TapasDataReader.Readers.readExplanations(path,steps,flights,attrMinMax);
    /**/
  
    ArrayList<CommonExplanation> exList=new ArrayList<CommonExplanation>(10000);
    for (Map.Entry<String,Flight> entry:flights.entrySet()) {
      Flight f=entry.getValue();
      if (f.expl!=null)
        for (int i=0; i<f.expl.length; i++)
          CommonExplanation.addExplanation(exList,f.expl[i],
              true,attrMinMax,true);
    }
    if (exList.isEmpty()) {
      System.out.println("Failed to reconstruct the list of common explanations!");
      return;
    }
    else
      System.out.println("Made a list of "+exList.size()+" common explanations!");
    
    System.out.println("Computing distance matrix...");
    double distanceMatrix[][]=CommonExplanation.computeDistances(exList,attrMinMax);
    if (distanceMatrix==null) {
      System.out.println("Failed to compute a matrix of distances between the explanations!");
      return;
    }
    System.out.println("Distance matrix ready!");
  
    ClustererByOPTICS clOptics=new ClustererByOPTICS();
    clOptics.setDistanceMatrix(distanceMatrix);
    clOptics.doClustering();

    ExListTableModel eTblModel=new ExListTableModel(exList,attrMinMax);
    clOptics.addChangeListener(eTblModel);
    
    SwingWorker worker=new SwingWorker() {
      @Override
      public Boolean doInBackground(){
        MySammonsProjection sam=new MySammonsProjection(distanceMatrix,1,300,true);
        sam.runProjection(5,50,eTblModel);
        return true;
      }
      @Override
      protected void done() {
        //pp.setDistanceMatrix(d);
      }
    };
    worker.execute();
  
    Dimension size=Toolkit.getDefaultToolkit().getScreenSize();
  
    /**/
    ExplanationsProjPlot2D pp=new ExplanationsProjPlot2D();
    pp.setExplanations(exList);
    pp.setDistanceMatrix(distanceMatrix);
    pp.setPreferredSize(new Dimension(800,800));

    JFrame plotFrame=new JFrame("Projection plot");
    plotFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    plotFrame.getContentPane().add(pp);
    plotFrame.pack();
    plotFrame.setLocation(size.width-plotFrame.getWidth()-30, size.height-plotFrame.getHeight()-50);
    plotFrame.setVisible(true);
  
    SingleHighlightManager highlighter=pp.getHighlighter();
    ItemSelectionManager selector=pp.getSelector();
    /**/
    
    JTable table=new JTable(eTblModel){
      public String getToolTipText(MouseEvent e) {
        java.awt.Point p = e.getPoint();
        int rowIndex = rowAtPoint(p);
        if (rowIndex>=0) {
          int realRowIndex = convertRowIndexToModel(rowIndex);
          highlighter.highlight(new Integer(realRowIndex));
          return exList.get(realRowIndex).toHTML();
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
          bkColor= ReachabilityPlot.getColorForCluster((Integer)eTblModel.getValueAt(rowIdx,column));
        boolean isAction=!isCluster && colName.equalsIgnoreCase("action");
        if (isAction)
          bkColor=ExplanationsProjPlot2D.getColorForAction((Integer)eTblModel.getValueAt(rowIdx,column));
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
    /**/
  
    JScrollPane scrollPane = new JScrollPane(table);
    
    JFrame fr = new JFrame("Explanations (" + exList.size() + ")");
    fr.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    fr.getContentPane().add(scrollPane, BorderLayout.CENTER);
    //Display the window.
    fr.pack();
    fr.setLocation(30, 30);
    fr.setVisible(true);
  
    /**/
    JPopupMenu menu=new JPopupMenu();
    JMenuItem mitExtract=new JMenuItem("Extract the selected subset to a separate view");
    menu.add(mitExtract);
    mitExtract.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        extractSubset(exList,distanceMatrix,selector,attrMinMax,eTblModel.order,eTblModel.clusters);
      }
    });
  
    JMenuItem mit=new JMenuItem("Read point coordinates from a file");
    menu.add(mit);
    mit.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        double coords[][]= CoordinatesReader.readCoordinatesFromChosenFile();
        if (coords==null) {
          System.out.println("No coordinates could be read!");
          return;
        }
        if (coords.length!=exList.size()) {
          System.out.println("The new coordinates are for "+coords.length+
                                 " points but must be for "+exList.size()+" points!");
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
              mitExtract.setEnabled(selected!=null && selected.size()>5);
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
  
    pp.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        super.mousePressed(e);
        if (e.getButton()>MouseEvent.BUTTON1) {
          ArrayList selected=selector.getSelected();
          mitExtract.setEnabled(selected!=null && selected.size()>5);
          menu.show(pp,e.getX(),e.getY());
        }
      }
    });
    /**/
  }
  
  public static void extractSubset(ArrayList<CommonExplanation> exList,
                                   double distanceMatrix[][],
                                   ItemSelectionManager selector,
                                   Hashtable<String,int[]> attrMinMax,
                                   int origOPTICSOrder[],
                                   int origClusters[]) {
    ArrayList selected=selector.getSelected();
    if (selected.size()<5)
      return;
    ArrayList<CommonExplanation> exSubset=new ArrayList<CommonExplanation>(selected.size());
    int idx[]=new int[selected.size()];
    int nEx=0;
    for (int i=0; i<selected.size(); i++)
      if (selected.get(i) instanceof Integer) {
        idx[nEx]=(Integer)selected.get(i);
        exSubset.add(exList.get(idx[nEx]));
        ++nEx;
      }
    double distances[][]=new double[nEx][nEx];
    for (int i=0; i<nEx; i++) {
      distances[i][i]=0;
      int ii=idx[i];
      for (int j=i+1; j<nEx; j++) {
        int jj=idx[j];
        distances[i][j]=distances[j][i]=distanceMatrix[ii][jj];
      }
    }
  
    ExplanationsProjPlot2D subPP=new ExplanationsProjPlot2D();
    subPP.setExplanations(exSubset);
  
    ExListTableModel subModel=new ExListTableModel(exSubset,attrMinMax);
  
    SwingWorker worker=new SwingWorker() {
      @Override
      public Boolean doInBackground(){
        subPP.setDistanceMatrix(distances);
        MySammonsProjection sam=new MySammonsProjection(distances,1,300,true);
        sam.runProjection(5,50,subModel);
        return true;
      }
      @Override
      protected void done() {
        //pp.setDistanceMatrix(distances);
      }
    };
    worker.execute();
  
    subModel.clusters=(origClusters==null)?null:new int[nEx];
    subModel.order=(origOPTICSOrder==null)?null:new int[nEx];
    if (subModel.clusters!=null || subModel.order!=null) {
      for (int i = 0; i < nEx; i++) {
        if (subModel.clusters != null)
          subModel.clusters[i]=origClusters[idx[i]];
        if (subModel.order!=null)
          subModel.order[i]=origOPTICSOrder[i];
      }
    }
  
    subPP.setPreferredSize(new Dimension(500,500));
    JFrame pf=new JFrame("Subset projection plot ("+exSubset.size()+" points)");
    pf.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    pf.getContentPane().add(subPP);
    pf.pack();
    Dimension size=Toolkit.getDefaultToolkit().getScreenSize();
    pf.setLocation(size.width-pf.getWidth()-20, size.height-pf.getHeight()-40);
    pf.setVisible(true);
  
    SingleHighlightManager hlSub=subPP.getHighlighter();
    ItemSelectionManager selSub=subPP.getSelector();
  
    JTable subTbl=new JTable(subModel){
      public String getToolTipText(MouseEvent e) {
        java.awt.Point p = e.getPoint();
        int rowIndex = rowAtPoint(p);
        if (rowIndex>=0) {
          int realRowIndex = convertRowIndexToModel(rowIndex);
          hlSub.highlight(new Integer(realRowIndex));
          return exSubset.get(realRowIndex).toHTML();
        }
        hlSub.clearHighlighting();
        return "";
      }
    
      public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component c = super.prepareRenderer(renderer, row, column);
        Color bkColor=(isRowSelected(row))?getSelectionBackground():getBackground();
        int rowIdx=convertRowIndexToModel(row), colIdx=convertColumnIndexToModel(column);
        String colName=subModel.getColumnName(colIdx);
        boolean isCluster=colName.equalsIgnoreCase("cluster");
        if (isCluster)
          bkColor= ReachabilityPlot.getColorForCluster((Integer)subModel.getValueAt(rowIdx,column));
        boolean isAction=!isCluster && colName.equalsIgnoreCase("action");
        if (isAction)
          bkColor=ExplanationsProjPlot2D.getColorForAction((Integer)subModel.getValueAt(rowIdx,column));
        c.setBackground(bkColor);
        if (hlSub==null || hlSub.getHighlighted()==null ||
                ((Integer)hlSub.getHighlighted())!=convertRowIndexToModel(row)) {
          ((JComponent) c).setBorder(null);
          return c;
        }
        ((JComponent) c).setBorder(highlightBorder);
        if (!isCluster && !isAction)
          c.setBackground(ProjectionPlot2D.highlightFillColor);
        return c;
      }
    };
    subTbl.addMouseListener(new MouseAdapter() {
      private void reactToMousePosition(MouseEvent e) {
        int rowIndex=subTbl.rowAtPoint(e.getPoint());
        if (rowIndex<0)
          hlSub.clearHighlighting();
        else {
          int realRowIndex = subTbl.convertRowIndexToModel(rowIndex);
          hlSub.highlight(new Integer(realRowIndex));
        }
      }
      @Override
      public void mouseEntered(MouseEvent e) {
        reactToMousePosition(e);
        super.mouseEntered(e);
      }
    
      @Override
      public void mouseExited(MouseEvent e) {
        hlSub.clearHighlighting();
        super.mouseExited(e);
      }
    
      @Override
      public void mouseMoved(MouseEvent e) {
        reactToMousePosition(e);
        super.mouseMoved(e);
      }
    });
  
    subTbl.setPreferredScrollableViewportSize(
        new Dimension(Math.round(size.width * 0.7f), Math.round(size.height * 0.8f)));
    subTbl.setFillsViewportHeight(true);
    subTbl.setAutoCreateRowSorter(true);
    subTbl.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    subTbl.setRowSelectionAllowed(true);
    subTbl.setColumnSelectionAllowed(false);
    for (int i=0; i<subModel.columnNames.length; i++)
      if (subModel.getColumnClass(i).equals(Integer.class))
        subTbl.getColumnModel().getColumn(i).setCellRenderer(
            new RenderLabelBarChart(0, subModel.getColumnMax(i)));
    for (int i=subModel.columnNames.length; i<subModel.getColumnCount(); i++)
      subTbl.getColumnModel().getColumn(i).setCellRenderer(new JLabel_Subinterval());
  
    TableRowsSelectionManager subSelMan=new TableRowsSelectionManager();
    subSelMan.setTable(subTbl);
    subSelMan.setHighlighter(hlSub);
    subSelMan.setSelector(selSub);
  
    JScrollPane scrollPane = new JScrollPane(subTbl);
  
    JFrame fr = new JFrame("Subset of explanations (" + exSubset.size() + ")");
    fr.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    fr.getContentPane().add(scrollPane, BorderLayout.CENTER);
    //Display the window.
    fr.pack();
    fr.setLocation(size.width-fr.getWidth()-10, 40);
    fr.setVisible(true);
  
    JPopupMenu menu=new JPopupMenu();
    JMenuItem mit=new JMenuItem("Export the distance matrix to a file");
    menu.add(mit);
    mit.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        MatrixWriter.writeMatrixToFile(distances,"distances.csv",true);
      }
    });
    
    subPP.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        super.mousePressed(e);
        if (selector.hasSelection() && e.getButton()>MouseEvent.BUTTON1) {
          ArrayList selected=selector.getSelected();
          if (selected.size()>5)
            menu.show(subPP,e.getX(),e.getY());
        }
      }
    });
  }
}
