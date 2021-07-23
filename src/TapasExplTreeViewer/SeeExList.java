package TapasExplTreeViewer;

import TapasDataReader.CommonExplanation;
import TapasDataReader.Explanation;
import TapasDataReader.ExplanationItem;
import TapasDataReader.Flight;
import TapasExplTreeViewer.clustering.ReachabilityPlot;
import TapasExplTreeViewer.ui.ExListTableModel;
import TapasExplTreeViewer.ui.JLabel_Subinterval;
import TapasExplTreeViewer.ui.ShowRules;
import TapasExplTreeViewer.util.MatrixWriter;
import TapasExplTreeViewer.vis.ExplanationsProjPlot2D;
import TapasExplTreeViewer.vis.TSNE_Runner;
import TapasUtilities.TableRowsSelectionManager;
import TapasExplTreeViewer.vis.ProjectionPlot2D;
import TapasUtilities.ItemSelectionManager;
import TapasUtilities.RenderLabelBarChart;
import TapasUtilities.SingleHighlightManager;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

public class SeeExList {
  
  public static Border highlightBorder=new LineBorder(ProjectionPlot2D.highlightColor,1);

  public static void main(String[] args) {
    String parFileName = (args != null && args.length > 0) ? args[0] : "params.txt";

    if (!parFileName.startsWith("params")) {
      mainSingleFile(parFileName);
      return;
    }
  
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
      System.exit(1);
    }
  
    System.out.println("Decisions file name = "+fName);
    /**/
    TreeSet<Integer> steps=TapasDataReader.Readers.readStepsFromDecisions(fName);
    //System.out.println(steps);
    Hashtable<String, Flight> flights=
        TapasDataReader.Readers.readFlightDelaysFromDecisions(fName,steps);
    if (flights==null || flights.isEmpty()) {
      System.out.println("Failed to get original data!");
      System.exit(1);
    }
    Hashtable<String,float[]> attrMinMax=new Hashtable<String, float[]>();
    TapasDataReader.Readers.readExplanations(path,steps,flights,attrMinMax);
    /**/
  
    ArrayList<CommonExplanation> exList=new ArrayList<CommonExplanation>(10000);
    for (Map.Entry<String,Flight> entry:flights.entrySet()) {
      Flight f=entry.getValue();
      if (f.expl!=null)
        for (int i=0; i<f.expl.length; i++)
          CommonExplanation.addExplanation(exList,f.expl[i],
              false,attrMinMax,true);
    }
    if (exList.isEmpty()) {
      System.out.println("Failed to reconstruct the list of common explanations!");
      System.exit(1);
    }
    else
      System.out.println("Made a list of "+exList.size()+" common explanations!");
  
    //MainBody(attrMinMax,exList);
    ShowRules showRules=new ShowRules(exList,attrMinMax);
    JFrame fr=showRules.showRulesInTable();
    if (fr==null) {
      System.out.println("Failed to visualize the rules!");
      System.exit(1);
    }
  }

  public static void mainSingleFile (String fname) {
    System.out.println("* loading rules and data from "+fname);
    Hashtable<String,float[]> attrMinMax=new Hashtable<String, float[]>();
    Vector<String> attrs=new Vector<>();
    Vector<Explanation> vex=new Vector<>();
    ArrayList<CommonExplanation> exList=new ArrayList<CommonExplanation>(10000);
    boolean bAllInts=true;
    try {
      BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(fname))));
      String strLine=br.readLine();
      int line=0;
      String s[]=strLine.split(",");
      for (int i=0; i<s.length; i++) {
        float minmax[]=new float[2];
        minmax[0]=Integer.MAX_VALUE;
        minmax[1]=Integer.MIN_VALUE;
        attrMinMax.put(s[i],minmax);
        attrs.add(s[i]);
      }
      while ((strLine = br.readLine()) != null) {
        line++;
        s=strLine.split(",");
        String srule[]=s[1].split("&");
        Explanation ex=new Explanation();
        ex.eItems=new ExplanationItem[srule.length];
        ex.FlightID=""+(line-1);
        ex.step=0;
        ex.Q=Float.valueOf(s[2]);
        bAllInts&=ex.Q==Math.round(ex.Q);
        //ex.action=(int)ex.Q; // ToDo
        vex.add(ex);
        for (int i=0; i<srule.length; i++) {
          int p=srule[i].indexOf("=");
          int attrIdx=-1;
          try {
            attrIdx=Integer.valueOf(srule[i].substring(0,p)).intValue();
          } catch (NumberFormatException nfe) {
            System.out.println("Error in line "+line+": extracting attr name from rule item # "+i+" "+srule[i]);
          }
          ExplanationItem ei=new ExplanationItem();
          ei.attr=attrs.elementAt(attrIdx);
          float minmax[]=attrMinMax.get(ei.attr);
          String ss=srule[i].substring(p+1);
          int p1=ss.indexOf("<="), p2=ss.indexOf(">");
          ei.value=Float.MIN_VALUE;
          try {
            ei.value=Float.valueOf(ss.substring(0,Math.max(p1,p2))).floatValue();
          } catch (NumberFormatException nfe) {
            System.out.println("Error in line "+line+": extracting attr value from rule item # "+i+" "+srule[i]);
          }
          boolean changed=false;
          if (ei.value<minmax[0]) {
            minmax[0]=ei.value;
            changed=true;
          }
          if (ei.value>minmax[1]) {
            minmax[1]=ei.value;
            changed=true;
          }
          if (changed)
            attrMinMax.put(ei.attr,minmax);
          String sss=ss.substring((p1>=0)?p1+2:p2+1);
          double d=Double.NaN;
          try {
            d=Double.valueOf(sss).doubleValue();
          } catch (NumberFormatException nfe) {
            System.out.println("Error in line "+line+": extracting condition from rule item # "+i+" "+srule[i]);
          }
          if (p1>=0) // condition <=
            ei.interval=new double[]{Double.NEGATIVE_INFINITY,d};
          else  // condition >
            ei.interval=new double[]{d,Double.POSITIVE_INFINITY};
          ei.attr_core=ei.attr;
          ei.sector="None";
          ex.eItems[i]=ei;
        }
      }
      br.close();
    } catch (IOException io) {
      System.out.println(io);
    }
    if (bAllInts)
      for (Explanation ex:vex)
        ex.action=(int)ex.Q;
    for (Explanation ex:vex)
      CommonExplanation.addExplanation(exList,ex,false,attrMinMax,true);
    //MainBody(attrMinMax,exList);

    ShowRules showRules=new ShowRules(exList,attrMinMax);
    JFrame fr=showRules.showRulesInTable();
    if (fr==null) {
      System.out.println("Failed to visualize the rules!");
      System.exit(1);
    }
  }
  
  public static void runTSNE(TSNE_Runner tsne) {
    String value=JOptionPane.showInputDialog("Enter an integer from 5 to 100:",
        tsne.getPerplexity());
    if (value==null)
      return;
    try {
      int p=Integer.parseInt(value);
      if (p<5 || p>100) {
        System.out.println("Illegal perplexity: "+p);
        return;
      }
      tsne.setPerplexity(p);
      tsne.runAlgorithm();
    } catch (Exception ex) {
      System.out.println(ex);
    }
  }
  
  public static void extractSubset(ArrayList<CommonExplanation> exList,
                                   double distanceMatrix[][],
                                   ItemSelectionManager selector,
                                   Hashtable<String,float[]> attrMinMax,
                                   int origOPTICSOrder[],
                                   int origClusters[],
                                   ArrayList<File> createdFiles) {
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
        TSNE_Runner tsne=new TSNE_Runner();
        tsne.setFileRegister(createdFiles);
        subPP.setProjectionProvider(tsne);
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
          bkColor= ReachabilityPlot.getColorForCluster((Integer)subModel.getValueAt(rowIdx,colIdx));
        boolean isAction=!isCluster && colName.equalsIgnoreCase("action");
        if (isAction)
          bkColor=ExplanationsProjPlot2D.getColorForAction((Integer)subModel.getValueAt(rowIdx,colIdx));
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
  
    if (subPP.getProjectionProvider() instanceof TSNE_Runner) {
      mit=new JMenuItem("Re-run t-SNE with another perplexity setting");
      menu.add(mit);
      mit.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          runTSNE((TSNE_Runner)subPP.getProjectionProvider());
        }
      });
    }
    
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
