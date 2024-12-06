package TapasExplTreeViewer.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;

public class IntervalBreaksDialog extends JDialog {
  private ArrayList<Double> breaks;
  private JTextField breaksField;
  private JTextField numIntervalsField;
  private double minValue, maxValue;

  public IntervalBreaksDialog(Frame parent, double minValue, double maxValue) {
    super(parent, "Edit Breaks", true);
    this.minValue = minValue;
    this.maxValue = maxValue;
    this.breaks = new ArrayList<>();
    this.breaks.add(minValue);
    this.breaks.add(maxValue);

    JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
    mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

    // Label and text field for breaks
    JLabel breaksLabel = new JLabel("Enter break values (space-separated):");
    breaksField = new JTextField(getBreaksAsString());

    JPanel breaksPanel = new JPanel(new BorderLayout());
    breaksPanel.add(breaksLabel, BorderLayout.NORTH);
    breaksPanel.add(breaksField, BorderLayout.CENTER);

    // Panel for automatic division
    JLabel autoLabel = new JLabel("Number of intervals:");
    numIntervalsField = new JTextField(5);
    numIntervalsField.setText("5"); // Default to 5 intervals

    JButton autoDivideButton = new JButton("Divide");
    autoDivideButton.addActionListener(e -> divideIntoEqualIntervals());

    JPanel autoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    autoPanel.add(autoLabel);
    autoPanel.add(numIntervalsField);
    autoPanel.add(autoDivideButton);

    mainPanel.add(breaksPanel, BorderLayout.NORTH);
    mainPanel.add(autoPanel, BorderLayout.CENTER);

    // Buttons for Apply and Cancel
    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    JButton applyButton = new JButton("Apply");
    JButton cancelButton = new JButton("Cancel");

    applyButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          updateBreaksFromField();
          dispose();
        } catch (NumberFormatException ex) {
          JOptionPane.showMessageDialog(IntervalBreaksDialog.this,
              "Invalid input. Please enter numeric values separated by spaces.",
              "Error", JOptionPane.ERROR_MESSAGE);
        }
      }
    });

    cancelButton.addActionListener(e -> dispose());

    buttonPanel.add(applyButton);
    buttonPanel.add(cancelButton);
    mainPanel.add(buttonPanel, BorderLayout.SOUTH);

    add(mainPanel);
    setSize(400, 200);
    setLocationRelativeTo(parent);
  }

  private String getBreaksAsString() {
    if (breaks==null || breaks.isEmpty())
      return "";
    String str=String.valueOf(breaks.get(0));
    for (int i=1; i<breaks.size(); i++)
      str+=" "+String.valueOf(breaks.get(i));
    return str;
  }

  private void updateBreaksFromField() throws NumberFormatException {
    String[] parts = breaksField.getText().trim().split("\\s+");
    breaks.clear();
    for (String part : parts) {
      try {
        breaks.add(Double.parseDouble(part));
      } catch (NumberFormatException ex) {}
    }
    Collections.sort(breaks);
    breaksField.setText(getBreaksAsString());
  }

  private void divideIntoEqualIntervals() {
    try {
      int numIntervals = Integer.parseInt(numIntervalsField.getText().trim());
      if (numIntervals < 2) throw new NumberFormatException("Invalid interval count");

      double step = (maxValue - minValue) / numIntervals;
      breaks.clear();
      for (int i=0; i<numIntervals-1; i++)
        breaks.add(minValue + i * step);

      breaksField.setText(getBreaksAsString());
    } catch (NumberFormatException ex) {
      JOptionPane.showMessageDialog(this,
          "Please enter a valid positive integer for the number of intervals.",
          "Error", JOptionPane.ERROR_MESSAGE);
    }
  }

  public ArrayList<Double> getBreaks() {
    return breaks;
  }

}
