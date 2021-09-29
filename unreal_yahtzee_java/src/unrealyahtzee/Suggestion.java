/*
 * Suggestion.java
 *
 * Created on June 1, 2005, 9:37 AM
 *
 */

package unrealyahtzee;

import java.util.*;

class Suggestion
{
  public boolean reroll[] = new boolean[YahtzeeApplet.numberOfDie];
  public boolean matchCategoryCriteria[] = new boolean[YahtzeeApplet.numberOfCategories];
  public int categoryToChoose;  // 1 - numberOfCategories, 0 if reroll suggested instead
  
  // "makeSuggestion" uses the given input to attempt to advise a Yahtzee player as to
  // what is most probably their best move, within the current context of the game state (given by
  // the input arguments).
  public static Suggestion makeSuggestion(Vector<Die> roll, Vector[] currentCategoryAssignments, int rollsRemaining)
  {
    // "roll" - a given die roll (face values, 1-6)
    // "currentCategoryAssignments" - array representing the face value of die rolls previously assigned to categories 1-13
    // "rollsRemaining" - how many re-rolls remaining for the current turn
    Suggestion suggestion = new Suggestion();
    
    for (int i = 0; i < YahtzeeApplet.numberOfCategories; ++i)
    {
      suggestion.matchCategoryCriteria[i] = (GameCoordinator.score(i, roll) > 0);
    }
    return suggestion;
  }
  
}