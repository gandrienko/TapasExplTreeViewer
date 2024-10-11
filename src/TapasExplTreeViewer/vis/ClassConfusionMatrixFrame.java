package TapasExplTreeViewer.vis;

import TapasExplTreeViewer.rules.ClassConfusionMatrix;

import javax.swing.*;
import java.awt.*;

public class ClassConfusionMatrixFrame extends JFrame {
  public ClassConfusionMatrixFrame(ClassConfusionMatrix cMatrix, String rulesInfoText) {
    setTitle("Class confusion matrix");
    setSize(800, 700);
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    setLocationRelativeTo(null);

    if (cMatrix==null || cMatrix.classNs==null || cMatrix.counts==null || cMatrix.nDataTotal<1)
      return;

    String labels[]=new String[cMatrix.classNs.size()];
    for (int i=0; i<cMatrix.classNs.size(); i++)
      labels[i]=cMatrix.classNs.get(i).toString();

    // Create the tabbed pane
    JTabbedPane tabbedPane = new JTabbedPane();

    // Create the MatrixPainter instance for the count matrix
    MatrixPainter countMatrixPainter = new MatrixPainter(cMatrix.counts);
    countMatrixPainter.setShowValues(true);
    countMatrixPainter.setShowColumnTotals(true);
    countMatrixPainter.setMinValue(0);
    countMatrixPainter.setLabels(labels);
    JScrollPane countScrollPane = new JScrollPane(countMatrixPainter);
    tabbedPane.addTab("Counts", countScrollPane);

    // Create the MatrixPainter instance for the percentage matrix
    MatrixPainter percentageMatrixPainter = new MatrixPainter(cMatrix.percents,false);
    percentageMatrixPainter.setShowValues(true);
    percentageMatrixPainter.setMinValue(0);
    percentageMatrixPainter.setLabels(labels);
    JScrollPane percentageScrollPane = new JScrollPane(percentageMatrixPainter);
    tabbedPane.addTab("Percentages", percentageScrollPane);

    // Add the tabbed pane to the frame
    add(tabbedPane, BorderLayout.CENTER);

    JTextArea infoArea=new JTextArea("Rule set: "+rulesInfoText);
    infoArea.setLineWrap(true);
    infoArea.setWrapStyleWord(true);
    infoArea.append("\nResults of applying the rules to "+cMatrix.nDataTotal+" data records. ");
    infoArea.append("Number of classes: "+cMatrix.counts.length+
        "; number of correct class assignments: "+cMatrix.nSame+
        String.format(" (%.2f %%)",100.0*cMatrix.nSame/cMatrix.nDataTotal));
    add(infoArea,BorderLayout.SOUTH);
  }
}
