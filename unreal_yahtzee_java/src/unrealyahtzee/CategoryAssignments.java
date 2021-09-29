/*
 * CategoryTable.java
 *
 * Created on May 29, 2005, 5:30 PM
 *
 */

package unrealyahtzee;

import java.util.Vector;

public class CategoryAssignments
{

  private Vector[] categoryRoll;
  
  CategoryAssignments(int numberOfCategories)
  {
    categoryRoll = new Vector[numberOfCategories];
  }
  
  void assignRoll(int category, Vector dieRoll)
  {
    categoryRoll[category] = dieRoll;
  }
  
  void clearAssignment(int category)
  {
    categoryRoll[category] = null;
  }
  
  public Vector[] categoryRoll() 
  { 
    return categoryRoll; 
  }
  
}
