/*
 * YahtzeeClient.java
 *
 * Created on May 31, 2005, 9:54 PM
 *
 */

package unrealyahtzee;

import java.net.*;
import java.io.*;
import java.util.*;

public class YahtzeeClient extends Thread
{

  private Socket socketToServer = null;
  private PrintWriter out = null;
  private BufferedReader in = null;
  private int playerNumber = -1;
  private YahtzeeUI yahtzeeUI;
  private boolean gameInProgress = false;
  
  public YahtzeeClient(YahtzeeUI yahtzeeUI)
  {
    this.yahtzeeUI = yahtzeeUI;
  }  
  
  public void setGameInProgress(boolean isGameInProgress)
  {
    gameInProgress = isGameInProgress;
  }

  public void broadcastCategoryAssignments(SelectableLabel[][][] assignedDieLabel)
  {
    for (int i = 0; i < YahtzeeApplet.numberOfCategories; ++i)
    {
      StringBuffer scorecard_entry = new StringBuffer("YAHTZEE ASSIGNMENT " + playerNumber + " CATEGORY " + i + " ");
      for (int j = 0; j < YahtzeeApplet.numberOfDie; ++j)
      {
        scorecard_entry.append("" + assignedDieLabel[playerNumber-1][j][i].getNumber() + " ");
      }
      //System.out.println("(to server) " + scorecard_entry.toString());
      out.println(scorecard_entry.toString());
    }
    out.flush();
    // SelectableLabel assignedDieLabel[][][] = new SelectableLabel[maxPlayersPerGame][numberOfDie][numberOfCategories];
  }

  public boolean connect(String address, int port)
  {
    try
    {
      //System.out.println("Connecting to " + address + ":" + port + "...");
      socketToServer = new Socket(address, port);
      //System.out.println("OK - Connecting I/O streams");
   
      out = new PrintWriter(new OutputStreamWriter(socketToServer.getOutputStream()));
      in = new BufferedReader(new InputStreamReader(socketToServer.getInputStream()));
      
      String s = "YAHTZEE IDENTIFY " + yahtzeeUI.getIdentity(); 
      //System.out.println("(to server) " + s);
      out.println(s);
      out.flush();
      
      return true;
    } 
    catch  (Exception e) 
    {
      System.out.println(e);
      yahtzeeUI.setConnectStatus("Unable to connect to server");
    } 
    return false;
  }
  
  public void nextPlayer()
  {
    String s = "YAHTZEE DONE " + playerNumber;
    //System.out.println("(to server) " + s);
    out.println(s);
    out.flush();
  }
    
  public void run()
  {
     try 
     {
//       System.out.println("Waiting for server to say hello...");
       yahtzeeUI.setConnectStatus("Waiting for server to say hello...");
       // Read next command from server
       String line = "";
       while (true) 
       {
         line = in.readLine();
         //System.out.println("(from server) [" + line + "]");
         if ( (line != null) && (!line.equals("")) )
         {
           break;
         }
       }
       //System.out.println("(from server) " + line);
       
       StringTokenizer st = new StringTokenizer(line, " ");
       if (st.nextToken().equals("YAHTZEE"))
       {
         if (st.nextToken().equals("HELLO"))
         {
           String sPlayerNumber = st.nextToken();
           playerNumber = Integer.parseInt(sPlayerNumber);
           yahtzeeUI.setActivePlayerNumber(playerNumber);
           yahtzeeUI.setPlayerName(yahtzeeUI.getIdentity());  // updates the player name GUI
           String serverName = st.nextToken();
           yahtzeeUI.setPlayerName(1, serverName);  // set the name of the Server player
           //System.out.println("You are player " + playerNumber + ", server is named " + serverName);
           yahtzeeUI.setConnectStatus("Server says hello.  You are player #" + playerNumber);
         }
       }
       
       while (true)
       {
         line = in.readLine();
         st = new StringTokenizer(line, " ");
         if (st.nextToken().equals("YAHTZEE"))
         {
           String token = st.nextToken();
           if (token.equals("START"))
           {
             yahtzeeUI.setConnectStatus("Game started!");
             for (int i = 0; i < YahtzeeApplet.maxPlayersPerGame; ++i)
             {
               int playerNumber = Integer.parseInt(st.nextToken());
               String playerName = st.nextToken();
               yahtzeeUI.setPlayerName(playerNumber, playerName);
             }
             break;
           }           
         }
       }
       
       gameInProgress = true;
       while (gameInProgress)
       {
         line = in.readLine();
         //System.out.println("(from server) " + line);
         st = new StringTokenizer(line, " ");
         if (st.nextToken().equals("YAHTZEE"))
         {
           String token = st.nextToken();
           if (token.equals("TURN"))
           {
             int playerToGo = Integer.parseInt(st.nextToken());  // playerNumber
             yahtzeeUI.setPlayerToGo(playerToGo);             
             yahtzeeUI.setConnectStatus("Player " + playerToGo + " turn.");
           }
           else if (token.equals("SCORECARD"))
           {
             token = st.nextToken();  // player index
             int scorePlayerIndex = Integer.parseInt(token);
             token = st.nextToken();  // "CATEGORY'
             token = st.nextToken();  // category
             int scoreCategory = Integer.parseInt(token);
             Vector roll = new Vector();
             token = st.nextToken();  // die roll 1
             roll.add(new Integer(Integer.parseInt(token)));
             token = st.nextToken();  // die roll 2
             roll.add(new Integer(Integer.parseInt(token)));
             token = st.nextToken();  // die roll 3
             roll.add(new Integer(Integer.parseInt(token)));
             token = st.nextToken();  // die roll 4
             roll.add(new Integer(Integer.parseInt(token)));
             token = st.nextToken();  // die roll 5
             roll.add(new Integer(Integer.parseInt(token)));
             token = st.nextToken();  // score
             int score = Integer.parseInt(token);
             //System.out.println("Setting category " + scorePlayerIndex + " cat " + scoreCategory + " score = " + score);
             yahtzeeUI.setCategoryRollForPlayer(scorePlayerIndex, scoreCategory, roll, score);
           }
           else if (token.equals("WINNER"))
           {
             int winningPlayerIndex = Integer.parseInt(st.nextToken()) - 1;
             yahtzeeUI.gameOver(winningPlayerIndex);
           }           
         }
       }

       //socketToServer.close();
     }
     catch (Exception e)
     {
       System.out.println("Client error: " + e);                
       e.printStackTrace();       
     }
  }
  
}
