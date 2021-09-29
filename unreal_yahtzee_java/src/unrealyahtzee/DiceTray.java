/*
 * Dice.java
 *
 * Created on May 29, 2005, 5:15 PM
 *
 */

package unrealyahtzee;

import java.util.*;

public class DiceTray 
{

  private Vector<Die> diceCollection;
  
  public DiceTray(int numberOfDice) 
  {
    diceCollection = new Vector<Die>();
    while (numberOfDice > 0)
    { 
      if ( !diceCollection.add(new Die()) )
      {
        // Unable to add the die for some reason.
        return;
      }
      --numberOfDice;
    }
  }

  public Collection<Die> dice() 
  { 
    return diceCollection; 
  }

  public Die handleDie(int n) 
  { 
    return diceCollection.elementAt(n); 
  }
  
  public void reroll(Selectable[] rerollFlag)
  {
    for (int i = 0; i < rerollFlag.length; ++i)
    {
      if ( rerollFlag[i].isSelected() )
      {
        diceCollection.elementAt(i).roll();
      }
    }
  }
  
  public void reroll(Vector<Integer> selectedDieToReroll)
  {
    for(Integer I: selectedDieToReroll)
    {
      diceCollection.elementAt( I.intValue() ).roll();
    }
  }
    
  void rerollAll()
  {
    for (Die die: diceCollection)
    {
      die.roll();
    }
  }
    
  public void rerollDie(int n) 
  { 
    diceCollection.elementAt(n).roll(); 
  }

  public int size() 
  { 
    return diceCollection.size(); 
  }
  
}
