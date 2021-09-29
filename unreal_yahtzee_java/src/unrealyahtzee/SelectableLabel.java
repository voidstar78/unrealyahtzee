/*
 * SelectableLabel.java
 *
 * Created on May 31, 2005, 1:30 AM
 *
 */

package unrealyahtzee;

import javax.swing.*;

class SelectableLabel extends JLabel implements Selectable
{

  // Flag used to indicate whether the label is "selected" or not.
  private boolean isSelected;
  
  // A "tag" number used to store additional information with the label.
  private int number;
  
  public SelectableLabel(int number, boolean isSelected)
  {
    this.isSelected = isSelected;
    this.number = number;
  }
  
  public int getNumber()
  {
    return number;
  }
  
  public void invertSelection()
  {
    isSelected = !isSelected;
  }
  
  public boolean isSelected()
  {
    return isSelected;
  }
  
  public void select()
  {
    isSelected = true;
  }

  public void setNumber(int newNumber)
  {
    number = newNumber;
  }
  
  public void unselect()
  {
    isSelected = false;
  }
  
}
