package TapasExplTreeViewer;

import TapasDataReader.Flight;
import TapasDataReader.Record;
import TapasExplTreeViewer.data.ExTreeReconstructor;
import TapasExplTreeViewer.ui.ExTreePanel;
import TapasExplTreeViewer.ui.TableOfIntegersModel;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.io.*;
import java.util.Hashtable;
import java.util.Map;
import java.util.TreeSet;
import java.util.Vector;

class RenderBar extends JProgressBar implements TableCellRenderer {
  public RenderBar(int min, int max) {
    super(min,max);
    setStringPainted(true);
    setOpaque(false);
    // https://stackoverflow.com/questions/25385700/how-to-set-position-of-string-painted-in-jprogressbar
  }
  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
    int v=((Integer)value).intValue();
    setValue(v);
    setString(""+v);
    return this;
  }
}

public class Main {

    public static void main(String[] args) {
      String path=null;
      Hashtable<String,String> fNames=new Hashtable<String,String>(10);
      try {
        BufferedReader br = new BufferedReader(
            new InputStreamReader(
                new FileInputStream(new File("params.txt")))) ;
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
  
      ExTreePanel exTreePanel=new ExTreePanel(exTreeReconstructor.topNodes);
      
      JFrame frame = new JFrame("TAPAS Explanations Logic Explorer");
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.getContentPane().add(exTreePanel, BorderLayout.CENTER);
      //Display the window.
      frame.pack();
      Dimension size=Toolkit.getDefaultToolkit().getScreenSize();
      frame.setLocation(size.width-frame.getWidth()-50,50);
      frame.setVisible(true);
      
      if (exTreeReconstructor.attributes!=null && !exTreeReconstructor.attributes.isEmpty()) {
        String colNames[]={"Attribute","Count"};
        String rowNames[]=new String[exTreeReconstructor.attributes.size()];
        Integer counts[][]=new Integer[exTreeReconstructor.attributes.size()][1];
        int idx=0, max=0;
        for (Map.Entry<String,Integer> e:exTreeReconstructor.attributes.entrySet()) {
          rowNames[idx]=e.getKey();
          counts[idx][0]=e.getValue();
          if (counts[idx][0]>max)
            max=counts[idx][0];
          idx++;
        }
        JTable table = new JTable(new TableOfIntegersModel(colNames,rowNames,counts));
        table.setPreferredScrollableViewportSize(new Dimension(Math.round(size.width*0.2f), Math.round(size.height*0.35f)));
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
        DefaultTableCellRenderer centerRenderer=new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(1).setCellRenderer(new RenderBar(0,max));
        JScrollPane scrollPane = new JScrollPane(table);

        JFrame fr=new JFrame("Attributes ("+rowNames.length+")");
        fr.getContentPane().add(scrollPane, BorderLayout.CENTER);
        //Display the window.
        fr.pack();
        fr.setLocation(50,50);
        fr.setVisible(true);
      }
      if (exTreeReconstructor.sectors!=null && !exTreeReconstructor.sectors.isEmpty()) {
        String colNames[]={"Sector","Count"};
        String rowNames[]=new String[exTreeReconstructor.sectors.size()];
        Integer counts[][]=new Integer[exTreeReconstructor.sectors.size()][1];
        int idx=0, max=0;
        for (Map.Entry<String,Integer> e:exTreeReconstructor.sectors.entrySet()) {
          rowNames[idx]=e.getKey();
          counts[idx][0]=e.getValue();
          if (counts[idx][0]>max)
            max=counts[idx][0];
          idx++;
        }
  
        JTable table = new JTable(new TableOfIntegersModel(colNames,rowNames,counts));
        table.setPreferredScrollableViewportSize(new Dimension(Math.round(size.width*0.2f), Math.round(size.height*0.5f)));
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
        DefaultTableCellRenderer centerRenderer=new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(1).setCellRenderer(new RenderBar(0,max));
        JScrollPane scrollPane = new JScrollPane(table);
  
        JFrame fr=new JFrame("Sectors ("+rowNames.length+")");
        fr.getContentPane().add(scrollPane, BorderLayout.CENTER);
        //Display the window.
        fr.pack();
        fr.setLocation(60+Math.round(size.width*0.2f),50);
        fr.setVisible(true);
      }
   }
}
