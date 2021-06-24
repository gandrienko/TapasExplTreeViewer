package TapasExplTreeViewer;

import TapasDataReader.CommonExplanation;
import TapasDataReader.Flight;
import TapasExplTreeViewer.ui.ExListTableModel;
import TapasExplTreeViewer.ui.JLabel_Subinterval;
import TapasUtilities.MySammonsProjection;
import TapasUtilities.RenderLabelBarChart;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;
import java.util.TreeSet;

public class SeeExList {
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
    if (exList.isEmpty())
      System.out.println("Failed to reconstruct the list of common explanations!");
    else
      System.out.println("Made a list of "+exList.size()+" common explanations!");
  
    Dimension size=Toolkit.getDefaultToolkit().getScreenSize();

    ExListTableModel eTblModel=new ExListTableModel(exList,attrMinMax);
  
    SwingWorker worker=new SwingWorker() {
      public MySammonsProjection sam=null;
      @Override
      public Boolean doInBackground(){
        double d[][]=CommonExplanation.computeDistances(exList,attrMinMax);
        if (d==null)
          return false;
        sam=new MySammonsProjection(d,1,200,true);
        sam.runProjection(5,eTblModel,0.005);
        return true;
      }
      @Override
      protected void done() {
      }
    };
    worker.execute();

    JTable table=new JTable(eTblModel);
    table.setPreferredScrollableViewportSize(new Dimension(Math.round(size.width * 0.7f), Math.round(size.height * 0.8f)));
    table.setFillsViewportHeight(true);
    table.setAutoCreateRowSorter(true);
    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    table.setRowSelectionAllowed(true);
    table.setColumnSelectionAllowed(false);
    for (int i=0; i<eTblModel.columnNames.length; i++)
      if (eTblModel.getColumnClass(i).equals(Integer.class))
        table.getColumnModel().getColumn(i).setCellRenderer(
            new RenderLabelBarChart(0, eTblModel.getColumnMax(i)));
    for (int i=eTblModel.columnNames.length; i<eTblModel.getColumnCount(); i++)
      table.getColumnModel().getColumn(i).setCellRenderer(new JLabel_Subinterval());
  
    JScrollPane scrollPane = new JScrollPane(table);
    
    JFrame fr = new JFrame("Explanations (" + exList.size() + ")");
    fr.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    fr.getContentPane().add(scrollPane, BorderLayout.CENTER);
    //Display the window.
    fr.pack();
    fr.setLocation(30, 30);
    fr.setVisible(true);
  }
}
