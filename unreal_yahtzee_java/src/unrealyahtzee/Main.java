/*
 * Main.java
 *
 * Created on May 29, 2005, 2:50 AM
 *
 */

package unrealyahtzee;

import java.io.*;
import java.util.*;

public class Main {
    
  /** Creates a new instance of Main */
  public Main() {
  }
    
  /**
     
   * @param args the command line arguments
   */
  public static String dieValuesToString(Vector<Integer> dieValues)
  {
    String result = "";
    Iterator<Integer> i_itr = dieValues.iterator();
    while (i_itr.hasNext())
    {
      Integer I = i_itr.next();
      result += "[ " + I.intValue() + " ]\t";
    }
    return result;
  }

  public static void main(String[] args) 
  {     
    //DiceTray diceTray = new DiceTray(5);
    GameCoordinator gameCoordinator = new GameCoordinator(5);
    gameCoordinator.nextTurn();
    String dieValuesString = dieValuesToString(gameCoordinator.dieValues());
    while (true)
    {
      System.out.println("Roll = " + dieValuesString);
      System.out.print("Command [" + gameCoordinator.rollsRemaining() + "]: ");
            
      try {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String s = br.readLine();
        if (s != null && s.length() > 0)
        {
          s = s.toUpperCase();
          char c = s.charAt(0);
          switch (c)
          {
            case 'C':  // CN  (N = 1 - 13, or 01 - 13)
              int category = -1;
              try 
              {
                category = Integer.parseInt( s.substring(1) );
              }
              catch (Exception e)
              {
                System.out.println("Invalid category command.");
                break;
              }              
              if ( !gameCoordinator.assignRollToCategory(category - 1) )
              {
                System.out.println("Category has already been assigned.");
                break;
              }
              // no break, go to next turn
              
            case 'N':
              gameCoordinator.nextTurn();
              // update the die value string
              dieValuesString = dieValuesToString(gameCoordinator.dieValues());
              break;
              
            case 'S':
              System.out.print("Current: " + dieValuesString);
              System.out.println("Rolls left: " + gameCoordinator.rollsRemaining());
              
              Vector[] categoryTable = gameCoordinator.categoryAssignments();
              for (int i = 0; i < categoryTable.length; ++i)
              {
                System.out.print((i+1) + ": ");
                if (categoryTable[i] == null)
                {
                  System.out.println("category not yet assigned");
                }
                else
                {
                  Vector v = categoryTable[i];
                  System.out.print(v + "\tPts = " + gameCoordinator.score(i, v) + "\n");
                }
              }
              break;
                      
            case 'R':  // roll
              try {
                s = s.substring(1);
                c = s.charAt(0);
                if (c == 'A')
                {
                  if (!gameCoordinator.rerollAll())
                  {
                    // out of turns
                    System.out.println("No more rerolls available.");
                  }
                }
                else 
                {
                  Vector<Integer> selected_dice = new Vector<Integer>();
                  while (s.length() > 0)
                  { 
                    int i = Integer.parseInt(s.substring(0,1)) - 1;
                    selected_dice.add(i);
                    s = s.substring(1);
                  }
                  if (!gameCoordinator.reroll(selected_dice))
                  {
                    System.out.println("No more rerolls available.");
                  }
                }
              } 
              catch (Exception e) 
              {
                // out of bounds?  oh well
                System.out.println("Invalid roll command.");
              }
              dieValuesString = dieValuesToString(gameCoordinator.dieValues());
              break;
                      
            case 'Q':
              return;
                      
            default:              
              System.out.println("  ?        help");
              System.out.println("  s        game status / score");
              System.out.println("  cN       assign current roll to category N (n = 1 to 13, e.g. c13, c8 or c08)");
              System.out.println("  ra       reroll all die");
              System.out.println("  rNnnnN   reroll die N (e.g. r1, r351, r35, note: ra = r12345)");
              System.out.println("  q        quit");
              break;
          }
        }
      } 
      catch (Exception e) 
      {
        System.out.println(e);   
      }
    }
  }  
}
