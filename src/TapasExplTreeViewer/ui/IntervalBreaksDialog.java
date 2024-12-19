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

    JPanel mainPanel = new JPanel(new GridLayout(0, 1));
    mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

    // Label and text field for breaks
    mainPanel.add(new JLabel("Enter break values (space-separated):"));
    breaksField = new JTextField(getBreaksAsString());
    mainPanel.add(breaksField);
    mainPanel.add(new JSeparator(JSeparator.HORIZONTAL));

    // Controls for automatic division
    mainPanel.add(new JLabel("Automatic division",JLabel.CENTER));
    JPanel p=new JPanel(new BorderLayout(10,0));
    mainPanel.add(p);
    p.add(new JLabel("Number of intervals:"),BorderLayout.WEST);
    numIntervalsField = new JTextField(5);
    numIntervalsField.setText("5"); // Default to 5 intervals
    p.add(numIntervalsField,BorderLayout.CENTER);
    JButton autoDivideButton = new JButton("Divide");
    autoDivideButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        divideIntoEqualIntervals();
      }
    });
    p.add(autoDivideButton,BorderLayout.EAST);


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

    cancelButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dispose();
      }
    });

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
