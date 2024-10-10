package TapasExplTreeViewer.vis;

import TapasExplTreeViewer.rules.ClassConfusionMatrix;

import javax.swing.*;
import java.awt.*;

public class ClassConfusionMatrixFrame extends JFrame {
  public ClassConfusionMatrixFrame(ClassConfusionMatrix cMatrix) {
    setTitle("Class confusion matrix");
    setSize(800, 700);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setLocationRelativeTo(null);

    // Create the tabbed pane
    JTabbedPane tabbedPane = new JTabbedPane();

    // Create the MatrixPainter instance for the count matrix
    MatrixPainter countMatrixPainter = new MatrixPainter(cMatrix.counts);
    countMatrixPainter.setShowValues(true);
    countMatrixPainter.setMinValue(0);
    JScrollPane countScrollPane = new JScrollPane(countMatrixPainter);
    tabbedPane.addTab("Counts", countScrollPane);

    // Create the MatrixPainter instance for the percentage matrix
    MatrixPainter percentageMatrixPainter = new MatrixPainter(cMatrix.percents,false);
    percentageMatrixPainter.setShowValues(true);
    percentageMatrixPainter.setMinValue(0);
    JScrollPane percentageScrollPane = new JScrollPane(percentageMatrixPainter);
    tabbedPane.addTab("Percentages", percentageScrollPane);

    // Add the tabbed pane to the frame
    add(tabbedPane, BorderLayout.CENTER);
  }
}
