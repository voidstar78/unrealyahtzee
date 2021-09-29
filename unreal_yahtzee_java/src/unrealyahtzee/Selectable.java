/*
 * Selectable.java
 *
 * Created on May 31, 2005, 1:22 AM
 *
 */

package unrealyahtzee;

interface Selectable 
{

  // If the object state is "selected", invert the state to "unselected", and vice versa.
  public void invertSelection();
  
  // Return true if the object state is "selected", otherwise return "false"
  public boolean isSelected();
  
  // Set the object state to "selected"
  public void select();
  
  // Set the object state to "unselected"
  public void unselect();
  
}
