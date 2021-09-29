/*
 * GameCoordinator.java
 *
 * Created on May 29, 2005, 5:30 PM
 *
 */

package unrealyahtzee;

import java.util.*;

public class GameCoordinator
{
  
  private CategoryAssignments categoryAssignments;
  private DiceTray dice;
  private int rollsRemaining;
  private int turnCount;
  
  public GameCoordinator(int numberOfDiee)
  {
    categoryAssignments = new CategoryAssignments(13);  // specific to Yahtzee rules, 13 categories
    this.dice = new DiceTray(numberOfDiee);  //dice;
    turnCount = 0;
    rollsRemaining = 3;
  }
  
  public boolean assignRollToCategory(int category)
  {    
    if (categoryAssignments.categoryRoll()[category] == null)
    {
      // Must make a copy of the scores to be associated with this category
      Vector<Integer> roll = dieValues();
      categoryAssignments.assignRoll(category, roll);
      return true;
    }
    // The category is already assigned
    return false;
  }
  
  public Vector[] categoryAssignments() 
  { 
    Vector[] categoryRoll = categoryAssignments.categoryRoll(); 
    Vector[] result = new Vector[categoryRoll.length];
    int index = 0;
    for (Vector roll: categoryRoll)
    {
      ++index;
      if (roll != null)
      {
        result[index] = new Vector();
        for (Object i: roll)
        {
          result[index].add(i);
        }
      }
    }
    return result;
  }
    
  public Vector dieValues()
  {
    // Return a copy of the current die sets face values.  Can't
    // just return the die set itself to the user, since they might
    // cheat and change the die values without the consent of the
    // game coordinator!    
    Vector result = new Vector();
    for (Die die: dice.dice())
    {
      result.addElement( die.currentTopSideValue() );
    } 
    return result;
  }
  
  public void endTurn()
  {
    rollsRemaining = 3;  
  }
  
  public boolean isJokerRoll(Vector roll)
  {
    // TBD:
    // is a roll a Yahtzee?
    // if if it is, has Yahtzee category already been assigned?
    // if yes, then this roll can be used as a joker
    return false;
  }
  
  public void nextTurn()
  {
    rollsRemaining = 3;
    ++turnCount;
    rerollAll();
  }  
    
  public boolean reroll(Selectable[] rerollFlag)
  {
    if (rollsRemaining > 0)
    {
      dice.reroll(rerollFlag);
      --rollsRemaining;
      return true;
    }
    return false;  // no more rolls left for this turn  
  }
  
  public boolean reroll(Vector<Integer> v)
  {
    if (rollsRemaining > 0)
    {
      dice.reroll(v);
      --rollsRemaining;
      return true;
    }
    return false;  // no more rolls left for this turn
  }

  public boolean rerollAll()
  {
    if (rollsRemaining > 0)
    {
      dice.rerollAll();
      --rollsRemaining;
      return true;
    }
    return false;  // no more rolls left for this turn
  }
  
  public int rollsRemaining() 
  { 
    return rollsRemaining; 
  }
  
  public static int score(int categoryIndex, Vector roll)
  {
    for (Object i: roll)
    {
      int dieValue = ((Integer)i).intValue();
      if ((dieValue < 1) || (dieValue > 6))
      {
        // At least die face values are not within 1 - 6.
        return 0;
      }
    }
    
    // Calculate the score of the specified category with the given roll.
    int result = 0;
    int category = (categoryIndex+1);  // categoryIndex -> categoryNumber
    if (category < 7)
    {
      // UPPER SCORES
      for (Object i: roll)
      {
        int dieValue = ((Integer)i).intValue();
        if (dieValue == category)
        {
          result += dieValue;
        }
      }
    }
    else
    {
      // LOWER SCORES
      switch (category)
      {
        case 7:  // 3 of a Kind
          // For a 3 of a Kind, you must have at least three of the same die faces.  If so,
          // you total all the die faces and score that total.
          int sumDieValues3 = 0;
          int[] matches3 = new int[6];  // 6 == number of die faces
          for (Object I: roll)
          {
            int dieValue = ((Integer)I).intValue();
            sumDieValues3 += dieValue;                            
            ++matches3[dieValue-1];
          }
          for (int i = 0; i < matches3.length; ++i)
          {
            if (matches3[i] >= 3)
            {
              result = sumDieValues3;
              break;
            }
          }
          break;
          
        case 8:  // 4 of a Kind
          // For a 4 of a Kind, you must have at least four of the same die faces.  If so,
          // you total all the die faces and score that total.
          int sumDieValues4 = 0;
          int[] matches4 = new int[6];  // 6 == number of die faces
          for (Object I: roll)
          {
            int dieValue = ((Integer)I).intValue();
            sumDieValues4 += dieValue;                            
            ++matches4[dieValue-1];
          }
          for (int i = 0; i < matches4.length; ++i)
          {
            if (matches4[i] >= 4)
            {
              result = sumDieValues4;
              break;
            }
          }
          break;
              
        case 9:  // Full House
          // Again as in poker, a Full House is a roll where you have both a 3 of a kind, and a pair.
          // Full houses score 25 points.
          int[] matchesFH = new int[6];
          for (int i = 1; i <= 6; ++i)
          {
            for (Object I: roll)
            {
              int dieValue = ((Integer)I).intValue();
              if (dieValue == i)
              {
                ++matchesFH[i-1];
              }
            }         
          }
          for (int i = 0; i < matchesFH.length; ++i)
          {
            if (matchesFH[i] == 5)
            {
              result = 25;
              break;
            }
            else if (matchesFH[i] == 3)
            {
              // The 3 of a Kind face value is (i+1)
              int pairValue = -1;  // no pair found yet
              for (Object I: roll)
              {
                int dieValue = ((Integer)I).intValue();
                if (dieValue != (i+1))
                {
                  if (pairValue == -1)
                  {
                    pairValue = dieValue;
                  }
                  else
                  {
                    if (dieValue == pairValue)
                    {
                      result = 25;
                      break;
                    }
                  }
                }
              }
              break;
            }
          }
          break;
              
        case 10:  // Straights (x4)
          // Like in poker, a straight is a sequence of consecutive die faces; a small straight
          // is 4 consecutive faces (30pts), and a large straight if 5 consecutive faces (40pts).
          //Arrays rollArray = new Arrays();//roll.toArray());
          Object[] rI = new Integer[5];
          roll.toArray( rI );
          Arrays.sort( rI );
          int[] r = new int[5];
          for (int i = 0; i < r.length; ++i)
          {
            Integer I = (Integer)(rI[i]);
            r[i] = I.intValue();
          }
          if ( r[0] <= 3 )
          {
            // remove duplicates
            for (int i = 0; i < r.length-2; ++i)
            {
              if (r[i] == r[i+1])
              {
                for (int j = i; j < r.length-1; ++j)
                {
                  r[j] = r[j+1];  
                }
              }
            }
          
            // combos w/o duplicates removed
          // 1 2 3 4 X (1)
          // 2 3 4 5 X (2)
          // 3 4 5 6 X (3)
          // X 1 2 3 4 (4)
          // X 2 3 4 5 (5)
          // X 3 4 5 6 (6)
          // X 2 3 4 5 (7)
          // 2 3 4 5 X (8)
          // X 3 4 5 6 (9)
          // 3 4 5 6 X (10)
          
          // with duplicates removed:
          // 1 2 3 4 X (1)
          // 2 3 4 5 X (2)
          // 3 4 5 6 X (3)
          // X 3 4 5 6 (4)

            if (
              ( (r[0] == 1) && (r[1] == 2) && (r[2] == 3) && (r[3] == 4)  ) ||
              ( (r[0] == 2) && (r[1] == 3) && (r[2] == 4) && (r[3] == 5)  ) ||
              ( (r[0] == 3) && (r[1] == 4) && (r[2] == 5) && (r[3] == 6)  ) ||
              ( (r[1] == 3) && (r[2] == 4) && (r[3] == 5) && (r[4] == 6)  )               
            )
            {
              result = 30;
            }
          }
          break;
          
        case 11:  // Straight (x5)
          Object[] rIa = new Integer[5];
          roll.toArray( rIa );
          Arrays.sort( rIa );
          int[] q = new int[5];
          for (int i = 0; i < q.length; ++i)
          {
            Integer I = (Integer)(rIa[i]);
            q[i] = I.intValue();
          }
          
          // 1 2 3 4 5 
          // 2 3 4 5 6
          
          if (
            ( (q[0] == 1) && (q[1] == 2) && (q[2] == 3) && (q[3] == 4) && (q[4] == 5) ) ||
            ( (q[0] == 2) && (q[1] == 3) && (q[2] == 4) && (q[3] == 5) && (q[4] == 6) )
          ) 
          {
            result = 40;
          }            
          break;
          
        case 12:  // Yahtzee
          // A Yahtzee is a 5 of a Kind, and scores 50 pts.  If you roll more than one Yahtzee in 
          // a single game, you will earn a 100 ptr bonus for each additional Yahtzee roll, provided
          // that you have already scored a 50 in the Yahtzee category. 
          // tbd more rules
          int prev = -1;
          boolean gotYahtzee = true;
          for (Object i: roll)
          {
            int dieValue = ((Integer)i).intValue();
            if (prev == -1)
            {
              prev = dieValue;
            }
            else
            {
              if (dieValue != prev)
              {
                gotYahtzee = false;
                break;
              }
              prev = dieValue;
            }
          }
          if (gotYahtzee)
          {
            result = 50;
          }
          break;
          
        case 13:  // Chance
          // Chance is a catch-all roll.  You can roll anything and you simply total all the die face values.
          for (Object i: roll)
          {
            int dieValue = ((Integer)i).intValue();
            result += dieValue;
          }
          break;
      }
    }
    return result;
  }
  
  public int turnCount()
  {
    return turnCount;
  }
  
/*
 * For testing die roll scores...
 */
  public static void main(String[] args)
  {
    Vector roll = new Vector();
    roll.add(new Integer(2));
    roll.add(new Integer(2));
    roll.add(new Integer(2));
    roll.add(new Integer(2));
    roll.add(new Integer(2));
    System.out.println("score = " + GameCoordinator.score(8, roll));
  }
 
}
