/*
 * YahtzeeClientHandler.java
 *
 * Created on June 2, 2005, 2:15 PM
 *
 */

package unrealyahtzee;

import java.net.*;
import java.util.*;
import java.io.*;

public class YahtzeeClientHandler extends Thread 
{
  
  private String playerIdentity = null;
  private PrintWriter out = null;
  private BufferedReader in = null;
  private YahtzeeServer yahtzeeServer = null;
  private YahtzeeUI yahtzeeUI;
  private Socket clientSocket;
  private int playerNumber;  

  public void broadcastCategoryAssignments(SelectableLabel[][][] assignedDieLabel, SelectableLabel[][] score)
  {
    for (int i = 0; i < YahtzeeApplet.maxPlayersPerGame; ++i)
    {
      for (int j = 0; j < YahtzeeApplet.numberOfCategories; ++j)
      {
        StringBuffer scorecard = new StringBuffer("YAHTZEE SCORECARD " + i + " CATEGORY " + j + " ");
        for (int k = 0; k < YahtzeeApplet.numberOfDie; ++k)
        {
          scorecard.append("" + assignedDieLabel[i][k][j].getNumber() + " ");          
        }
        scorecard.append(score[i][j].getNumber());
        //System.out.println("(to client) " + scorecard.toString());
        out.println(scorecard.toString());
      }
    }
    out.flush();
  }
  
  public YahtzeeClientHandler(YahtzeeServer yahtzeeServer, YahtzeeUI yahtzeeUI, Socket clientSocket, int playerNumber)
  {
    this.yahtzeeServer = yahtzeeServer;
    this.yahtzeeUI = yahtzeeUI;
    this.clientSocket = clientSocket;
    this.playerNumber = playerNumber;
  }
    
  public void broadcastNextPlayer(int newPlayerNumber)
  { 
    String s = "YAHTZEE TURN " + newPlayerNumber;
    //System.out.println("(to client) " + s);
    out.println(s);
    out.flush();
  }
  
  public void broadcastStartOfGame(String[] playerName)
  {
    StringBuffer s = new StringBuffer("YAHTZEE START ");
    for (int i = 0; i < playerName.length; ++i)
    {
      s.append((i+1) + " ");
      if (playerName[i] == null)
      {
        s.append("(none)");
      }
      else
      {
        s.append(playerName[i]);
      }
      s.append(" ");
    }
    out.println(s.toString());
    out.flush();
  }
  
  public void broadcastWinner(int winnerNumber)
  {
    String s = "YAHTZEE WINNER " + winnerNumber;    
    //System.out.println("(to client " + s);
    out.println(s);
    out.flush();
  }

  public void run()
  {
    try 
    {
      //System.out.println("Setting up I/O streams of potential player #" + playerNumber);
      out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
      in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

      //System.out.println("Waiting for IDENTITY of connection");
      String s_in = in.readLine();  // wait for YAHTZEE IDENTIFY <Identity>
      //System.out.println("(from client) " + s_in);
      StringTokenizer st = new StringTokenizer(s_in, " ");
      if (st.nextToken().equals("YAHTZEE"))
      {
        if (st.nextToken().equals("IDENTIFY"))
        {
          playerIdentity = st.nextToken();  
          yahtzeeUI.setConnectStatus(playerIdentity + " has joined");
          String s = "YAHTZEE HELLO " + playerNumber + " " + yahtzeeUI.getIdentity();
          //System.out.println(playerIdentity + " identified.  Assigned to " + playerNumber);
          //System.out.println("(to client) " + s);
          out.println(s);
          out.flush();
          yahtzeeUI.setPlayerName(playerNumber, playerIdentity);
          yahtzeeUI.gameCanStart();
        }
      }
      
      while (true)
      {
        s_in = in.readLine();
        //System.out.println("(from client) " + s_in);
        st = new StringTokenizer(s_in, " ");
        if (st.nextToken().equals("YAHTZEE"))
        {
          String token = st.nextToken();
          if (token.equals("DONE"))
          {
            int donePlayerNumber = Integer.parseInt(st.nextToken());            
            if (donePlayerNumber != yahtzeeServer.currentPlayerNumber())
            {
              yahtzeeUI.setConnectStatus("Invalid play claimed to be done!");
            }
            else
            {
              yahtzeeServer.nextPlayer();
            }
          }
          else if (token.equals("ASSIGNMENT"))
          {
            token = st.nextToken(); // player number
            int playerNumber = Integer.parseInt(token);
            if (playerNumber != yahtzeeServer.currentPlayerNumber())
            {
              yahtzeeUI.setConnectStatus("Category assignments from wrong client!");
            }
            else 
            {
              token = st.nextToken(); // "CATEGORY"
              token = st.nextToken(); // category index
              int categoryIndex = Integer.parseInt(token);
              Vector roll = new Vector();
              token = st.nextToken(); // roll1
              roll.add(new Integer(Integer.parseInt(token)));
              token = st.nextToken(); // roll2
              roll.add(new Integer(Integer.parseInt(token)));
              token = st.nextToken(); // roll3
              roll.add(new Integer(Integer.parseInt(token)));
              token = st.nextToken(); // roll4
              roll.add(new Integer(Integer.parseInt(token)));
              token = st.nextToken(); // roll5
              roll.add(new Integer(Integer.parseInt(token)));
              //System.out.println("Set category....");
              yahtzeeUI.setCategoryRollForPlayer(playerNumber-1, categoryIndex, roll, -1);
            }  
          }
        }
        sleep(20);
      }
    }
    catch (Exception e)
    {
      yahtzeeUI.setConnectStatus("Error with client");
      return;
    }
  }
  
}
