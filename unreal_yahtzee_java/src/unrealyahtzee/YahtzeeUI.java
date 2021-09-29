/*
 * YahtzeeUI.java
 *
 * Created on June 4, 2005, 1:06 AM
 *
 */

package unrealyahtzee;

import java.util.*;

interface YahtzeeUI 
{
  
  // Used to indicate to the player that the game has been started by the server.
  public void gameCanStart();

  // Get the identity of the player running this instance of the Yahtzee game.
  public String getIdentity();   
  
  // Interface to indicate who has won the game (which player number).  The server broadcast
  // the player number of the winner to all the clients.
  public void gameOver(int winningIndex);

  // Set the Player Number of the player running this instance of the Yahtzee game.
  // In a solo game, the player is always Player Number 0.  If an online game,
  // the player might be assigned to be Player Number 2, 3, or 4.  The server
  // lets the player know which Player Number they are.
  public void setActivePlayerNumber(int playerNumber);
  
  // Used to tell the player what the rolls and category assignments are for the other
  // players of the game.  The server sends this information, and the UI can use this
  // information to update its display.
  public void setCategoryRollForPlayer(int scorePlayerIndex, int scoreCategory, Vector roll, int score);

  // Update the literal connection status of the socket connection.
  public void setConnectStatus(String status);
    
  // Set the player name of the player running this instance.  This interface is not
  // really needed, the GUI show show the players own name already.  But there
  // are other uses for this, such as Bots.
  public void setPlayerName(String newPlayerName);

  // This interface is used to inform the player the name of other players in the game.
  public void setPlayerName(int playerNumber, String newPlayerName);
        
  // Indicate which players turn it is.
  public void setPlayerToGo(int playerNumber);

  // Used to indicate to the player that it is their turn to roll and assign to a category.
  public void yourTurn();
  
}
