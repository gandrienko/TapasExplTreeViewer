package TapasExplTreeViewer;

import TapasDataReader.Flight;
import TapasDataReader.Record;
import TapasExplTreeViewer.data.ExTreeReconstructor;
import TapasExplTreeViewer.ui.ExTreePanel;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.Hashtable;
import java.util.Map;
import java.util.TreeSet;
import java.util.Vector;

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
      frame.setLocation(50,50);
      frame.setVisible(true);
      
      if (exTreeReconstructor.attributes!=null && !exTreeReconstructor.attributes.isEmpty()) {
        String items[]=new String[exTreeReconstructor.attributes.size()];
        int idx=0;
        for (Map.Entry<String,Integer> e:exTreeReconstructor.attributes.entrySet()) {
          items[idx++]= idx+") "+ e.getKey()+": "+e.getValue();
        }
        JList list=new JList(items);
        JScrollPane listScroller = new JScrollPane(list);
        listScroller.setPreferredSize(new Dimension(250, 400));
        JFrame fr=new JFrame("Attributes:");
        fr.getContentPane().add(listScroller, BorderLayout.CENTER);
        //Display the window.
        fr.pack();
        fr.setLocation(250,100);
        fr.setVisible(true);
      }
      if (exTreeReconstructor.sectors!=null && !exTreeReconstructor.sectors.isEmpty()) {
        String items[]=new String[exTreeReconstructor.sectors.size()];
        int idx=0;
        for (Map.Entry<String,Integer> e:exTreeReconstructor.sectors.entrySet()) {
          items[idx++]= idx+") "+ e.getKey()+": "+e.getValue();
        }
        JList list=new JList(items);
        JScrollPane listScroller = new JScrollPane(list);
        listScroller.setPreferredSize(new Dimension(250, 400));
        JFrame fr=new JFrame("Sectors:");
        fr.getContentPane().add(listScroller, BorderLayout.CENTER);
        //Display the window.
        fr.pack();
        fr.setLocation(450,150);
        fr.setVisible(true);
      }
   }
}
