package TapasExplTreeViewer.vis;

import TapasExplTreeViewer.util.CoordinatesReader;
import TapasExplTreeViewer.util.MatrixWriter;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.util.ArrayList;

public class TSNE_Runner implements ProjectionProvider{
  public static int nInstancesCreated=0;
  
  public int currInstanceN=-1;
  /**
   * Matrix of distances between the objects to project and show
   */
  public double distances[][]=null;
  /**
   * The value of the "perplexity" parameter
   */
  public int perplexity=30;
  /**
   * The projection obtained (updated iteratively)
   */
  protected double proj[][]=null;
  
  protected ArrayList<ChangeListener> changeListeners=null;
  
  public TSNE_Runner() {
    currInstanceN=++nInstancesCreated;
  }
  
  public void addChangeListener(ChangeListener l) {
    if (changeListeners==null)
      changeListeners=new ArrayList(5);
    if (!changeListeners.contains(l))
      changeListeners.add(l);
  }
  
  public void removeChangeListener(ChangeListener l) {
    if (l!=null && changeListeners!=null)
      changeListeners.remove(l);
  }
  
  public void notifyChange(){
    if (changeListeners==null || changeListeners.isEmpty())
      return;
    ChangeEvent e=new ChangeEvent(this);
    for (ChangeListener l:changeListeners)
      l.stateChanged(e);
  }
  
  public int getPerplexity() {
    return perplexity;
  }
  
  public void setPerplexity(int perplexity) {
    this.perplexity = perplexity;
  }
  
  public void setDistanceMatrix(double distances[][]) {
    this.distances = distances;
    runAlgorithm();
  }
  
  public void runAlgorithm() {
    if (distances != null) {
      System.out.println("Running the t-SNE algorithm");
      String numSt=String.format("%02d",currInstanceN), distFName="distances_"+numSt;
      SwingWorker worker=new SwingWorker() {
        @Override
        public Boolean doInBackground(){
          //todo: run t-SNE; get projection to proj
          MatrixWriter.writeMatrixToFile(distances,
              distFName+".csv",true);
          String command="cmd.exe /C TSNE-precomputed.bat "+distFName+" "+perplexity;
          try {
            Process p = Runtime.getRuntime().exec(command);
            int exit_value = p.waitFor();
            System.out.println("TSNE: finished, code="+exit_value);
            proj= CoordinatesReader.readCoordinatesFromFile(distFName+"_out_p"+perplexity+".csv");
            notifyChange();
          } catch (Exception e) {
            e.printStackTrace();
          }
          return true;
        }
        @Override
        protected void done() {
          //notifyChange();
        }
      };
      worker.execute();
    }
    
  }
  
  public double[][] getProjection(){
    return proj;
  }
  
  public String getProjectionTitle() {
    return "t-SNE projection; perplexity = "+perplexity;
  }
}
