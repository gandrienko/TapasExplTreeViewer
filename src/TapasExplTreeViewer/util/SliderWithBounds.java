package TapasExplTreeViewer.util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class SliderWithBounds extends JPanel {
  private DualSlider slider;
  private JTextField lowerBoundField, upperBoundField;
  private double realMin=Double.NaN, realMax=Double.NaN;

  public SliderWithBounds(int min, int max, int lowerValue, int upperValue) {
    setLayout(new BorderLayout(5, 5));

    // Initialize the DualSlider
    slider = new DualSlider(min, max, lowerValue, upperValue);

    // Initialize text fields for lower and upper bounds
    lowerBoundField = new JTextField(String.format("%.3f",1.0*lowerValue), 5);
    upperBoundField = new JTextField(String.format("%.3f",1.0*upperValue), 5);

    // Listen for slider changes
    slider.addChangeListener(e -> {
      lowerBoundField.setText(String.format("%.3f",translateToRealValue(slider.getLowerValue())));
      upperBoundField.setText(String.format("%.3f",translateToRealValue(slider.getUpperValue())));
    });

    // Listen for changes in the lower bound field
    lowerBoundField.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        boolean ok=false;
        try {
          double value = Double.parseDouble(lowerBoundField.getText());
          int pos=translateToPosition(value);
          ok=pos>=slider.getMin() && pos<=slider.getUpperValue();
          if (ok)
            slider.setLowerValue(pos);
        } catch (NumberFormatException ex) { }
        if (!ok) {
          JOptionPane.showMessageDialog(SliderWithBounds.this,
              "Invalid lower bound value.", "Error", JOptionPane.ERROR_MESSAGE);
          lowerBoundField.setText(String.format("%.3f",translateToRealValue(slider.getLowerValue())));
        }
      }
    });

    // Listen for changes in the upper bound field
    upperBoundField.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        boolean ok=false;
        try {
          double value = Double.parseDouble(upperBoundField.getText());
          int pos=translateToPosition(value);
          ok= pos>=slider.getLowerValue() && pos<=slider.getMax();
          if (ok)
            slider.setUpperValue(pos);
        } catch (NumberFormatException ex) { }
        if (!ok) {
          JOptionPane.showMessageDialog(SliderWithBounds.this,
              "Invalid upper bound value.", "Error", JOptionPane.ERROR_MESSAGE);
          upperBoundField.setText(String.format("%.3f",translateToRealValue(slider.getUpperValue())));
        }
      }
    });

    // Layout components
    /*
    JPanel fieldsPanel = new JPanel(new BorderLayout(5, 5));
    fieldsPanel.add(lowerBoundField, BorderLayout.WEST);
    fieldsPanel.add(upperBoundField, BorderLayout.EAST);
    */

    add(slider, BorderLayout.CENTER);
    add(lowerBoundField, BorderLayout.WEST);
    add(upperBoundField, BorderLayout.EAST);
  }

  public void setRealMinMax(double min, double max) {
    if (min<max) {
      realMin = min;
      realMax = max;
      lowerBoundField.setText(String.format("%.3f",min));
      upperBoundField.setText(String.format("%.3f",max));
    }
  }

  public double getRealMin() {
    return (Double.isNaN(realMin))?slider.getMin():realMin;
  }

  public double getRealMax() {
    return (Double.isNaN(realMax))?slider.getMax():realMax;
  }

  public double translateToRealValue(int position) {
    if (Double.isNaN(realMin) || Double.isNaN(realMax))
      return position;
    double relativePos=((double)position-slider.getMin())/(slider.getMax()-slider.getMin());
    return realMin+relativePos*(realMax-realMin);
  }

  public int translateToPosition(double value) {
    if (Double.isNaN(realMin) || Double.isNaN(realMax))
      return (int)Math.round(Math.abs(value));
    double relativePos=(value-realMin)/(realMax-realMin);
    return slider.getMin()+(int)Math.round(relativePos*(slider.getMax()-slider.getMin()));
  }

  public double getLowerValue() {
    if (lowerBoundField!=null) {
      String txt=lowerBoundField.getText();
      if (txt!=null)
        try {
          if (txt.contains(".")) {
            double d=Double.parseDouble(txt);
            if (!Double.isNaN(d))
              return  d;
          }
          else {
            int k=Integer.parseInt(txt);
            return k;
          }
        } catch( Exception ex) {}
    }
    return translateToRealValue(slider.getLowerValue());
  }

  public double getUpperValue() {
    if (upperBoundField!=null) {
      String txt=upperBoundField.getText();
      if (txt!=null)
        try {
          if (txt.contains(".")) {
            double d=Double.parseDouble(txt);
            if (!Double.isNaN(d))
              return  d;
          }
          else {
            int k=Integer.parseInt(txt);
            return k;
          }
        } catch( Exception ex) {}
    }
    return translateToRealValue(slider.getUpperValue());
  }

  public void setLowerValue(double value) {
    slider.setLowerValue(translateToPosition(value));
    lowerBoundField.setText(String.format("%.3f",value));
  }

  public void setUpperValue(double value) {
    slider.setUpperValue(translateToPosition(value));
    upperBoundField.setText(String.format("%.3f",value));
  }

  public void setEnabled(boolean enabled) {
    slider.setEnabled(enabled);
    lowerBoundField.setEnabled(enabled);
    upperBoundField.setEnabled(enabled);
  }
}
