/*
 * Die.java
 *
 * Created on May 29, 2005, 5:13 PM
 *
 */

package unrealyahtzee;

import java.util.Random;

public class Die
{
  
  public static final int face_unknown = -1;
  
  private int topSideValue;
  
  Die() 
  {
    topSideValue = face_unknown;
  }

  int currentTopSideValue() 
  { 
    return topSideValue; 
  }
  
  public void roll()
  {
    Random random = new Random();
    topSideValue = random.nextInt(6) + 1;
  }
  
}
