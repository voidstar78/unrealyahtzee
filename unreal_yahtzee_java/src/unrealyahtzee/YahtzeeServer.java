/*
 * YahteeServer.java
 *
 * Created on May 31, 2005, 9:50 PM
 *
 */

package unrealyahtzee;

import java.net.*;
import java.util.*;
import java.io.*;

public class YahtzeeServer extends Thread
{
  
  private YahtzeeUI yahtzeeUI;
  private YahtzeeClientHandler[] yahtzeeClientHandler = new YahtzeeClientHandler[5];
  private int numConnectedClients = 0;
  private ServerSocket server = null;
  private int gameTurns = 0;  
  private boolean acceptingConnections = true;
  private boolean gameInProgress = false;
  private int currentPlayerIndex = 0;
  
  public static final int maxClients = 3;

  /** Creates a new instance of YahteeServer */
  public YahtzeeServer(YahtzeeUI yahtzeeUI, int port, String ipAddress) 
  {
    this.yahtzeeUI = yahtzeeUI;
    try 
    {
      InetAddress i_addr = InetAddress.getByName(ipAddress);
      server = new ServerSocket(port, 1024, i_addr);
      //System.out.println("Server initialized to port " + port);
//      yahtzeeUI.setConnectStatus("");
      InetAddress address = server.getInetAddress();      
      byte[] ia_b = InetAddress.getLocalHost().getAddress();
      // Convert the signed bytes into unsigned integers...
      int[] ia = new int[ia_b.length];
      for (int i = 0; i < ia.length; ++i)
      {
        ia[i] = ia_b[i] & 0xFF;
      }
      String iaStr = Integer.toString(ia[0]) + "." + Integer.toString(ia[1]) + "." + Integer.toString(ia[2]) + "." + Integer.toString(ia[3]);
      int localPort = server.getLocalPort();
      yahtzeeUI.setConnectStatus("Server listening (" + address.toString() + ", " + iaStr + ", port " + Integer.toString(localPort));      
    }
    catch (Exception e)
    {
      yahtzeeUI.setConnectStatus("Unable to start server (already running?).");
      System.out.println("Server socket error: " + e);
      server = null;
    }
  }
  
  public void broadcastCategoryAssignments(SelectableLabel[][][] assignedDieLabel, SelectableLabel[][] score)
  {
    for (int i = 0; i < yahtzeeClientHandler.length; ++i)
    {
      if (yahtzeeClientHandler[i] != null)
      {
        yahtzeeClientHandler[i].broadcastCategoryAssignments(assignedDieLabel, score);
      }
    }
  }
  
  public void broadcastStartOfGame(String[] playerName)
  {
    for (int i = 0; i < yahtzeeClientHandler.length; ++i)
    {
      if (yahtzeeClientHandler[i] != null)
      {
        yahtzeeClientHandler[i].broadcastStartOfGame(playerName);
      }
    }
  }
  
  public void broadcastWinner(int winnerNumber)
  {
    for (int i = 0; i < yahtzeeClientHandler.length; ++i)
    {
      if (yahtzeeClientHandler[i] != null)
      {
        yahtzeeClientHandler[i].broadcastWinner(winnerNumber);
      }
    }
  }  
  
  public int currentPlayerNumber()
  {
    return currentPlayerIndex+1;
  }
  
  public int getGameTurns() 
  {
    return gameTurns;
  }

  public int getNumConnectedClients()
  {
    return numConnectedClients;
  }

  public void nextPlayer()
  {
    ++gameTurns;
  
    //System.out.println("NEXT PLAYER");
    ++currentPlayerIndex;
    if (currentPlayerIndex > numConnectedClients)
    {
      //System.out.println("rollover");
      currentPlayerIndex = 0;
    }
    //System.out.println("next player -> curr plr index = " + currentPlayerIndex + " (clients: " + numConnectedClients + ")");
    for (int i = 0; i < yahtzeeClientHandler.length; ++i)
    {
      if (yahtzeeClientHandler[i] != null)
      {
        yahtzeeClientHandler[i].broadcastNextPlayer(currentPlayerIndex+1);
      }
    }
    if (currentPlayerIndex == 0)
    {
       yahtzeeUI.yourTurn();
    }
  }

  public void setAcceptingConnections(boolean nowAccepting)
  {
    acceptingConnections = nowAccepting;
  }
  
  public void setGameInProgress(boolean isGameRunning)
  {
    gameInProgress = isGameRunning;
  }    
      
  public void run()
  {
    while (server != null)
    {
      try 
      {
        gameInProgress = true;  // auto-start game in progress        
        while (gameInProgress && acceptingConnections)
        {
          //System.out.println("Waiting for connection...");
          Socket clientSocket = server.accept();
          if (acceptingConnections)
          {
            //System.out.println("Incoming connection - potential player #" + (numConnectedClients+2));
            yahtzeeClientHandler[numConnectedClients] = new YahtzeeClientHandler(this, yahtzeeUI, clientSocket, numConnectedClients + 2);
            yahtzeeClientHandler[numConnectedClients].start();
            ++numConnectedClients;
          }
          
          if (numConnectedClients >= maxClients)
          {
            acceptingConnections = false;
          }
        }

        while (gameInProgress)
        {
          for (int i = 0; i < yahtzeeClientHandler.length; ++i)
          {
            if ( (yahtzeeClientHandler[i] != null) && (!yahtzeeClientHandler[i].isAlive()) )
            {
              yahtzeeClientHandler[i] = null;  
            }
          }
          // busy wait
          sleep(20);
        }
        server.close();
        server = null;
        return;
      }
      catch (Exception e)
      {       
        System.out.println(e);
        e.printStackTrace();
        break;
      }
    }
  }

}
