package TapasExplTreeViewer.ui;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class JLabel_Subinterval extends JLabel implements TableCellRenderer {
  public double min=Double.NaN, max=Double.NaN, absMin=Double.NaN, absMax=Double.NaN;
  public double v[]=null;
  
  public JLabel_Subinterval() {
    setHorizontalAlignment(SwingConstants.RIGHT);
    setOpaque(false);
  }
  public void setValues (double v[]) {
    if (v!=null && v.length>=4) {
      min=v[0]; max=v[1];
      absMin=v[2]; absMax=v[3];
    }
    this.v=v;
    //setText("");

    if (Double.isNaN(min) || Double.isNaN(max))
      setText("");
    else
      setText(Math.round(min)+".."+Math.round(max));

  }
  public void paint (Graphics g) {
    if (Double.isNaN(min) || Double.isNaN(max) || Double.isNaN(absMin) || Double.isNaN(absMax)) {
      super.paint(g);
      return;
    }
    int w=getWidth(), h=getHeight();
    g.setColor(getBackground());
    g.fillRect(0, 0, w, h);
    int x1 = (int) Math.round((min - absMin) * w / (absMax - absMin)),
        x2 = (int) Math.round((max - absMin) * w / (absMax - absMin));
    g.setColor(Color.lightGray);
    g.fillRect(x1, h / 2, x2 - x1, h / 2);
    g.setColor(Color.gray.darker());
    for (int i=4; i<v.length; i++) {
      int x=(int) Math.round((v[i] - absMin) * (w-1) / (absMax - absMin));
      g.drawLine(x,h/4, x, 3*h/4);
    }
    super.paint(g);
  }
  public Component getTableCellRendererComponent(JTable table,
                                                 Object value,
                                                 boolean isSelected,
                                                 boolean hasFocus,
                                                 int row, int column) {
    if (isSelected)
      setBackground(table.getSelectionBackground());
    else
      setBackground(table.getBackground());
    setValues((double[])value);
    return this;
  }
}
