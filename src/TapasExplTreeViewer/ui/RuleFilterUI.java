package TapasExplTreeViewer.ui;

import TapasExplTreeViewer.rules.RuleSet;
import TapasExplTreeViewer.util.DualSlider;
import TapasExplTreeViewer.util.SliderWithBounds;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class RuleFilterUI extends JPanel {
  public RuleSet ruleSet=null;

  private Map<String, SliderWithBounds> sliders=null;
  private Map<String, JCheckBox> checkBoxes=null;
  private JButton applyFilterButton=null;

  public RuleFilterUI(RuleSet ruleSet) {
    this.ruleSet=ruleSet;

    setLayout(new BorderLayout());

    JPanel filtersPanel = new JPanel();
    filtersPanel.setLayout(new GridLayout(0, 1, 5, 5));
    sliders = new HashMap<>();
    checkBoxes = new HashMap<>();

    ArrayList<String> featureNames=(ruleSet.orderedFeatureNames!=null)?
        ruleSet.orderedFeatureNames:ruleSet.listOfFeatures;

    for (int i=0; i<featureNames.size(); i++) {
      String featureName = featureNames.get(i);
      float minmax[]=ruleSet.attrMinMax.get(featureName);
      if (minmax==null)
        continue;

      JPanel featurePanel = new JPanel(new BorderLayout());
      featurePanel.setBorder(BorderFactory.createTitledBorder(featureName));

      // Checkbox for excluding rules not involving this feature
      JCheckBox checkBox = new JCheckBox("Select rules not involving this feature");
      featurePanel.add(checkBox, BorderLayout.NORTH);
      checkBoxes.put(featureName, checkBox);

      // RangeSlider for selecting feature value range
      SliderWithBounds rangeSlider = new SliderWithBounds(0,1000,0,1000);
      rangeSlider.setRealMinMax(minmax[0],minmax[1]);
      featurePanel.add(rangeSlider, BorderLayout.CENTER);
      sliders.put(featureName, rangeSlider);

      // Disable RangeSlider when checkbox is selected
      checkBox.addItemListener(new ItemListener() {
        @Override
        public void itemStateChanged(ItemEvent e) {
          rangeSlider.setEnabled(!checkBox.isSelected());
        }
      });

      filtersPanel.add(featurePanel);
    }

    add(new JScrollPane(filtersPanel), BorderLayout.CENTER);

    applyFilterButton = new JButton("Apply Filter");
    add(applyFilterButton, BorderLayout.SOUTH);
  }

  public Map<String, Object> getSelectedFilters() {
    Map<String, Object> filters = new HashMap<>();
    for (String feature : sliders.keySet()) {
      if (checkBoxes.get(feature).isSelected()) {
        filters.put(feature, "exclude");
      } else {
        SliderWithBounds slider = sliders.get(feature);
        filters.put(feature, new double[]{slider.getLowerValue(), slider.getUpperValue()});
      }
    }
    return filters;
  }

  public JButton getApplyFilterButton() {
    return applyFilterButton;
  }
}
