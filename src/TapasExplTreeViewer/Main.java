package TapasExplTreeViewer;

import TapasDataReader.Flight;
import TapasUtilities.*;
import TapasExplTreeViewer.data.CountMatrix;
import TapasExplTreeViewer.data.ExTreeReconstructor;
import TapasExplTreeViewer.ui.ExTreePanel;
import TapasExplTreeViewer.ui.TableOfIntegersModel;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.io.*;
import java.util.Hashtable;
import java.util.Map;
import java.util.TreeSet;

public class Main {

    public static void main(String[] args) {
      String parFileName=(args!=null && args.length>0)?args[0]:"params.txt";
      
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
      //System.out.println(flights.get("EDDK-LEPA-EWG598-20190801083100").delays[2]);
      /*
      fName=fNames.get("flight_plans");
      if (fName==null) {
        System.out.println("No flight plans file name in the parameters!");
        return;
      }
      System.out.println("Flight plans file name = "+fName);
      Hashtable<String, Vector<Record>> records=TapasDataReader.Readers.readFlightPlans(fName,flights);
      */
      TapasDataReader.Readers.readExplanations(path,steps,flights);
      /**/
  
      ExTreeReconstructor exTreeReconstructor=new ExTreeReconstructor();
      if (!exTreeReconstructor.reconstructExTree(flights)) {
        System.out.println("Failed to reconstruct the explanation tree!");
        return;
      }
      System.out.println("Reconstructed explanation tree has "+exTreeReconstructor.topNodes.size()+" top nodes");

      Dimension size=Toolkit.getDefaultToolkit().getScreenSize();
     
      if (exTreeReconstructor.attributes!=null && !exTreeReconstructor.attributes.isEmpty()) {
        CountMatrix matrix=exTreeReconstructor.countActionsPerAttributes();
        if (matrix!=null) {
          JTable table = new JTable(new TableOfIntegersModel(matrix));
          table.setPreferredScrollableViewportSize(new Dimension(Math.round(size.width * 0.4f), Math.round(size.height * 0.4f)));
          table.setFillsViewportHeight(true);
          table.setAutoCreateRowSorter(true);
          DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
          centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
          table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
          for (int i=1; i<matrix.colNames.length; i++)
            table.getColumnModel().getColumn(i).setCellRenderer(new RenderLabelBarChart(0, matrix.getColumnMax(i)));
          JScrollPane scrollPane = new JScrollPane(table);
  
          JFrame fr = new JFrame("Attributes (" + matrix.rowNames.length + ")");
          fr.getContentPane().add(scrollPane, BorderLayout.CENTER);
          //Display the window.
          fr.pack();
          fr.setLocation(50, 50);
          fr.setVisible(true);
        }
      }
      if (exTreeReconstructor.sectors!=null && !exTreeReconstructor.sectors.isEmpty()) {
        CountMatrix matrix=exTreeReconstructor.countActionsPerSectors();
        if (matrix!=null) {
          JTable table = new JTable(new TableOfIntegersModel(matrix));
          table.setPreferredScrollableViewportSize(new Dimension(Math.round(size.width * 0.4f), Math.round(size.height * 0.4f)));
          table.setFillsViewportHeight(true);
          table.setAutoCreateRowSorter(true);
          DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
          centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
          table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
          for (int i=1; i<matrix.colNames.length; i++)
            table.getColumnModel().getColumn(i).setCellRenderer(new RenderLabelBarChart(0, matrix.getColumnMax(i)));
          JScrollPane scrollPane = new JScrollPane(table);
    
          JFrame fr = new JFrame("Sectors (" + matrix.rowNames.length + ")");
          fr.getContentPane().add(scrollPane, BorderLayout.CENTER);
          //Display the window.
          fr.pack();
          fr.setLocation(60+Math.round(size.width * 0.4f), 50);
          fr.setVisible(true);
        }
        matrix=exTreeReconstructor.countAttributesPerSectors();
        if (matrix!=null) {
          JTable table = new JTable(new TableOfIntegersModel(matrix));
          table.setPreferredScrollableViewportSize(new Dimension(Math.round(size.width * 0.4f), Math.round(size.height * 0.4f)));
          table.setFillsViewportHeight(true);
          table.setAutoCreateRowSorter(true);
          DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
          centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
          table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
          for (int i=1; i<matrix.colNames.length; i++)
            table.getColumnModel().getColumn(i).setCellRenderer(new RenderLabelBarChart(0, matrix.getColumnMax(i)));
          JScrollPane scrollPane = new JScrollPane(table);
    
          JFrame fr = new JFrame("Sectors (" + matrix.rowNames.length + ")");
          fr.getContentPane().add(scrollPane, BorderLayout.CENTER);
          //Display the window.
          fr.pack();
          fr.setLocation(60+Math.round(size.width * 0.4f), 50+Math.round(size.height * 0.4f));
          fr.setVisible(true);
        }
      }
  
      ExTreePanel exTreePanel=new ExTreePanel(exTreeReconstructor.topNodes);
      ExTreePanel exTreePanel1=(exTreeReconstructor.topNodesExCombined==null)?null:
                                   new ExTreePanel(exTreeReconstructor.topNodesExCombined);
      JSplitPane spl=(exTreePanel1==null)?null:new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,exTreePanel,exTreePanel1);
  
      JFrame frame = new JFrame("TAPAS Explanations Logic Explorer");
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.getContentPane().add((spl==null)?exTreePanel:spl, BorderLayout.CENTER);
      //Display the window.
      frame.pack();
      frame.setLocation(size.width-frame.getWidth()-50,50);
      frame.setVisible(true);
    }
}
