package TapasExplTreeViewer.clustering;

import it.unipi.di.sax.optics.AnotherOptics;
import it.unipi.di.sax.optics.ClusterListener;
import it.unipi.di.sax.optics.ClusterObject;
import it.unipi.di.sax.optics.DistanceMeter;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.TreeSet;

public class ClustererByOPTICS
    implements DistanceMeter<ClusterObject<Integer>>, ClusterListener {
  /**
   * Matrix of distances between the objects to cluster
   */
  public double distances[][]=null;
  /**
   * The objects prepared for clustering
   */
  protected ArrayList<ClusterObject<Integer>> objToCluster=null;
  /**
   * The objects ordered by the clustering algorithm
   */
  protected ArrayList<ClusterObject> objOrdered=null;
  /**
   * Shows the reachability plot
   */
  protected ReachPlotPanel plotPanel =null;
  
  protected ArrayList<ChangeListener> changeListeners=null;
  
  public void addChangeListener(ChangeListener l) {
    if (changeListeners==null)
      changeListeners=new ArrayList(5);
    if (!changeListeners.contains(l)) {
      changeListeners.add(l);
      if (plotPanel!=null)
        plotPanel.addChangeListener(l);
    }
  }
  
  public void removeChangeListener(ChangeListener l) {
    if (l!=null && changeListeners!=null) {
      changeListeners.remove(l);
      if (plotPanel!=null)
        plotPanel.removeChangeListener(l);
    }
  }
  
  public void setDistanceMatrix(double distances[][]) {
    this.distances = distances;
    if (distances!=null) {
      objToCluster=new ArrayList<ClusterObject<Integer>>(distances.length);
      for (int i=0; i<distances.length; i++) {
        ClusterObject<Integer> clObj=new ClusterObject<Integer>(new Integer(i));
        objToCluster.add(clObj);
      }
    }
  }
  
  /**
   * Tries to select clustering parameters based on statistics of distances
   */
  public void doClustering() {
    TreeSet<Double> distSet=new TreeSet<Double>();
    for (int i=0; i<distances.length-1; i++)
      for (int j=i+1; j<distances.length; j++)
        distSet.add(distances[i][j]);
    ArrayList<Double> distList=new ArrayList<Double>(distSet);
    int idx=Math.max(3,Math.round(0.025f*distList.size()));
    doClustering(distList.get(idx),5);
  }
  
  public void doClustering(double maxDistance, int minNeighbors) {
    if (objToCluster==null || objToCluster.size()<=minNeighbors)
      return;
    System.out.println("Starting OPTICS clustering; max distance = "+maxDistance+
                           "; min N neighbors = "+minNeighbors);
    AnotherOptics optics = new AnotherOptics(this);
    optics.addClusterListener(this);
    SwingWorker worker=new SwingWorker() {
      @Override
      public Boolean doInBackground(){
        optics.optics(objToCluster,maxDistance,minNeighbors);
        return true;
      }
      @Override
      protected void done() {
        clusteringDone();
      }
    };
    worker.execute();
  }
  
  public double distance(ClusterObject<Integer> o1, ClusterObject<Integer> o2) {
    if (o1==null || o2==null) return Double.POSITIVE_INFINITY;
    if (distances==null) return Double.POSITIVE_INFINITY;
    return distances[o1.getOriginalObject()][o2.getOriginalObject()];
  }
  
  public Collection<ClusterObject<Integer>> neighbors(ClusterObject<Integer> core,
                                                      Collection<ClusterObject<Integer>> objects,
                                                      double epsilon){
    TreeSet<ObjectWithMeasure> neighborsWithDistances=new TreeSet<ObjectWithMeasure>();
    for(Iterator<ClusterObject<Integer>> i = objects.iterator(); i.hasNext(); ){
      ClusterObject<Integer> o = i.next();
      if(!o.equals(core)){
        double dist = distance(o, core);
        if (dist <=epsilon){
          ObjectWithMeasure om=new ObjectWithMeasure(o,dist);
          neighborsWithDistances.add(om);
        }
      }
    }
    ArrayList<ClusterObject<Integer>> neiObj=
        new ArrayList<ClusterObject<Integer>>(Math.max(1,neighborsWithDistances.size()));
    for (Iterator<ObjectWithMeasure> it=neighborsWithDistances.iterator(); it.hasNext();) {
      ObjectWithMeasure om=it.next();
      neiObj.add((ClusterObject<Integer>)om.obj);
    }
    return neiObj;
  }
  
  /**
   * Receives an object from the clustering tool
   */
  public void emit(ClusterObject o) {
    if (objOrdered==null)
      objOrdered=new ArrayList<ClusterObject>(objToCluster.size());
    objOrdered.add((ClusterObject<Integer>)o);
    if (objOrdered.size()%250==0)
      System.out.println("OPTICS clustering: "+objOrdered.size()+" objects put in order");
  }
  
  protected void clusteringDone(){
    if (objOrdered==null || objOrdered.isEmpty()) {
      System.out.println("OPTICS clustering failed!");
      return;
    }
    System.out.println("OPTICS clustering finished!");
    
    plotPanel =new ReachPlotPanel(objOrdered);
    JFrame fr = new JFrame("OPTICS reachability plot");
    fr.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    fr.getContentPane().add(plotPanel, BorderLayout.CENTER);
    //Display the window.
    fr.pack();
    Dimension size=Toolkit.getDefaultToolkit().getScreenSize();
    if (fr.getWidth()>0.8*size.width)
      fr.setSize(Math.round(0.8f*size.width),fr.getHeight());
    fr.setLocation((size.width-fr.getWidth())/2, size.height-fr.getHeight()-40);
    fr.setVisible(true);
    
    if (changeListeners!=null)
      for (int i=0; i<changeListeners.size(); i++)
        plotPanel.addChangeListener(changeListeners.get(i));
  }
  
  public static ClustersAssignments makeClusters(ArrayList<ClusterObject> objOrdered,
                                                 double distThreshold) {
    if (objOrdered==null || objOrdered.isEmpty())
      return null;
    ClustersAssignments clAss=new ClustersAssignments();
    clAss.clusters=new int[objOrdered.size()];
    clAss.objIndexes=new int[objOrdered.size()];
    clAss.nClusters=0;
    int currClusterSize=0;
    for (int i=0; i<objOrdered.size(); i++) {
      clAss.clusters[i]=-1; //noise
      clAss.objIndexes[i]=-1;
      ClusterObject clObj=objOrdered.get(i);
      if (clObj.getOriginalObject() instanceof ClusterObject) {
        ClusterObject origObj=(ClusterObject)clObj.getOriginalObject();
        if (origObj.getOriginalObject() instanceof Integer)
          clAss.objIndexes[i]=(Integer)origObj.getOriginalObject();
      }
      double d=clObj.getReachabilityDistance();
      if (!Double.isNaN(d) && !Double.isInfinite(d) && d<distThreshold) {
        if (clAss.nClusters == 0 || (i > 0 && clAss.clusters[i - 1] < 0)) { //new cluster
          if (currClusterSize>0) {
            if (clAss.minSize<1 || clAss.minSize>currClusterSize)
              clAss.minSize=currClusterSize;
            if (clAss.maxSize<currClusterSize)
              clAss.maxSize=currClusterSize;
          }
          clAss.clusters[i] = clAss.nClusters++;
          currClusterSize=1;
        }
        else {
          clAss.clusters[i] = clAss.clusters[i - 1]; //continuation of the previous cluster
          ++currClusterSize;
        }
      }
      else
        ++clAss.nNoise;
    }
    return clAss;
  }
}
