/*
 * YahtzeeApplet.java
 *
 * Created on May 29, 2005, 10:51 PM
 *
 */

package unrealyahtzee;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.event.*;
import java.util.*;
import java.net.*;

public class YahtzeeApplet 
  extends JFrame implements ActionListener, YahtzeeUI  // change JFrame to JApplet to make this an Applet
{

  // --- CONSTANTS ---
  public static final String VERSION = new String("1.2");

  public static final int maxPlayersPerGame = 4;
  public static final int numberOfDie = 5;
  public static final int numberOfCategories = 13;
  public static final int numberOfDieFaces = 6;
  public static final int rollsPerTurn = 3;  // initial roll + two rerolls

  private static final int noWinner = -1;
  private static final int noCategory = -1;
  private static final String imagePath = "images";
  private static final String unknownPoints = "---";

  public static final String categoryNames[] = {
    "Aces",            // 0
    "Twos",            // 1
    "Threes",          // 2
    "Fours",           // 3
    "Fives",           // 4
    "Sixes",           // 5
    "3 of a Kind",     // 6
    "4 of a Kind",     // 7
    "Full House",      // 8
    "Small Straight",  // 9
    "Large Straight",  // 10
    "Yahtzee",         // 11
    "Chance"           // 12
  };     
    
  // --- Static Sized Arrays ---
  private ImageIcon[] dieImage = new ImageIcon[numberOfDieFaces];
  private ImageIcon[] dieSelectedImage = new ImageIcon[numberOfDieFaces];
  private ImageIcon[] categoryImage = new ImageIcon[numberOfCategories];
  private ImageIcon[] miniDieImage = new ImageIcon[numberOfDieFaces];
  private ImageIcon[] turnLeftImage = new ImageIcon[rollsPerTurn+1];  // 0 1 2 3  
  private JLabel[] identityLabel = new JLabel[maxPlayersPerGame];
  private String[] playerName = new String[maxPlayersPerGame];
  private SelectableLabel[] dieLabel = new SelectableLabel[numberOfDie];
  private SelectableLabel scoreLabel[][] = new SelectableLabel[maxPlayersPerGame][numberOfCategories];
  private SelectableLabel assignedDieLabel[][][] = new SelectableLabel[maxPlayersPerGame][numberOfDie][numberOfCategories];
  private SelectableLabel[] categoryLabel = new SelectableLabel[numberOfCategories];
  private SelectableLabel[] pointsLabel = new SelectableLabel[numberOfCategories];
  private JLabel[] upperTotalLabel = new JLabel[maxPlayersPerGame];
  private JLabel[] bonusLabel = new JLabel[maxPlayersPerGame];  
  private JLabel[] lowerTotalLabel = new JLabel[maxPlayersPerGame];
  private SelectableLabel[] grandTotalLabel = new SelectableLabel[maxPlayersPerGame]; 

  private ImageIcon yahtzeeImage = null;
  private ImageIcon blankIcon = null; 
    
  private JLabel rollsRemainingLabel = new JLabel("");    
    
  private JTextField identityText = new JTextField(10);
  private JTextField addressText = new JTextField(15);
  private JTextField portText = new JTextField("2626", 4);
  private JLabel connectStatusLabel = new JLabel();
  private JLabel gameStatusLabel = new JLabel();
  
  private JButton resetButton = new JButton("Reset");
  private JButton hostButton = new JButton("Host");
  private JButton startButton = new JButton("Start");
  private JButton rollButton = new JButton("ROLL");
  private JButton suggestButton = new JButton("SUGGEST");
  private JButton connectButton = new JButton("Connect To");
  
  private JLabel timerLabel = new JLabel("00:00");
    
  // --- Game State (must be initialized in the init() method ---
  private int assignedPlayerIndex;
  private int winnerIndex;  // index of the player who has won the game (-1 if none)
  private GameCoordinator gameCoordinator;
  private YahtzeeClient yahtzeeClient;
  private int categoryAssigned;
  private YahtzeeServer yahtzeeServer;
  private javax.swing.Timer timer;
  private Thread scrambleThread;
  
  private Action updateTimerAction = null;

  private void actionAssignedDieClicked(java.awt.event.MouseEvent evt)
  {
    SelectableLabel label = (SelectableLabel)evt.getComponent();  
    for (int i = 0; i < maxPlayersPerGame; ++i)
    {
      for (int j = 0; j < numberOfDie; ++j)
      {
        for (int k = 0; k < numberOfCategories; ++k)
        {
          if (assignedDieLabel[i][j][k] == label)
          {
              if (assignedPlayerIndex == i)
              {
                java.awt.event.MouseEvent c_evt = new java.awt.event.MouseEvent(categoryLabel[k], 0, 0, 0, 0, 0, 0, false);
                actionCategoryMouseClicked(c_evt);
              }
              break;
          }
        }
      }
    }
  }
  
  private void actionCategoryMouseClicked(java.awt.event.MouseEvent evt)
  {
    if (winnerIndex != noWinner)
    {
      setGameStatus("The game has already been won.  Restart the applet to play again!");
      return;
    }
    
    if ( (!rollButton.isEnabled()) & (gameCoordinator.rollsRemaining() > 0) ) 
    {
      // The die are rolling or it's not the players turn yet
      setGameStatus("The dice haven't finished rolling yet!");
      return;
    }
    
    if (gameCoordinator.rollsRemaining() >= 3)
    {
      setGameStatus("Roll the dice first!");  
    }
    else if (categoryAssigned != noCategory)
    {
      String s = "The roll has already been assigned to the " + categoryNames[categoryAssigned] + " category.  Roll again!";
      if ((yahtzeeClient != null) || (yahtzeeServer != null))
      {
        s += " (is it your turn?)";
      }
      setGameStatus(s);
    }
    else
    {
      SelectableLabel label = (SelectableLabel)evt.getComponent();
      int categoryIndex = label.getNumber();
      if (gameCoordinator.assignRollToCategory(categoryIndex))
      {      
        Vector dieValues = gameCoordinator.dieValues();
        for (int i = 0; i < numberOfDie; ++i)
        {
          int dieValue = (Integer)dieValues.elementAt(i);
          assignedDieLabel[assignedPlayerIndex][i][categoryIndex].setIcon( miniDieImage[ dieValue-1 ] );
          assignedDieLabel[assignedPlayerIndex][i][categoryIndex].setNumber(dieValue);
        }
        int score = gameCoordinator.score(categoryIndex,  dieValues);
        scoreLabel[assignedPlayerIndex][categoryIndex].setText( Integer.toString(score) );
        scoreLabel[assignedPlayerIndex][categoryIndex].setNumber( score );
        categoryAssigned = categoryIndex;
        setGameStatus("Roll has been assigned to category " + (categoryIndex+1) + " (" + categoryNames[categoryIndex] + ")");
        doNextTurn();
      }
      else
      {
        // category already assigned, make some noise
        setGameStatus("That category has already been assigned by a previous roll.");
      }
    }
  }
  
  private void actionDieMouseClicked(java.awt.event.MouseEvent evt) 
  {
    if (winnerIndex != noWinner)
    {
      setGameStatus("The game has already been won.  Restart the applet to play again!");
      return;
    }
    
    int rollsLeft = gameCoordinator.rollsRemaining();
    if ( (!rollButton.isEnabled()) & (rollsLeft > 0))
    {
      // The die are rolling
      setGameStatus("Wait for the die to stop rolling!");
      return;      
    }
    if (rollsLeft >= 3)
    {
      // The game hasn't started yet
      setGameStatus("Roll the dice first!");
    }
    else if (rollsLeft <= 0)
    {
      // No rolls left, no need to select
      if (categoryAssigned != noCategory)
      {
        setGameStatus("The roll has already been assigned to the " + categoryNames[categoryAssigned] + " category.  Roll again!");
      }
      else
      {
        setGameStatus("No rerolls remaining.  You must assign the roll to a category."); 
      }
    }
    else
    {
      // Highlight (or un-highlight) the selected die object
      Vector dieValues = gameCoordinator.dieValues();       
      
      SelectableLabel label = (SelectableLabel)evt.getComponent();
      int dieIndex = label.getNumber();
      label.invertSelection();
      
      int faceValueIndex = (Integer)dieValues.elementAt(dieIndex) - 1;
      
      if (label.isSelected())
      {
        label.setIcon( dieSelectedImage[faceValueIndex] );
      }
      else
      {
        label.setIcon( dieImage[faceValueIndex] );
      }
    }
  }
  
  private void actionEnteredPointsMotion(java.awt.event.MouseEvent evt)
  {
    SelectableLabel label = (SelectableLabel)evt.getSource();
    int category = label.getNumber();
    int score = gameCoordinator.score(category, gameCoordinator.dieValues());
//    System.out.println("Points label for category " + label.getNumber() + ", score = " + score);
    String scoreStr = Integer.toString(score);
    while (scoreStr.length() < unknownPoints.length())
    {
      scoreStr = "0" + scoreStr;
    }
    pointsLabel[category].setText( scoreStr );
  }
  
  private void actionExitedPointsMotion(java.awt.event.MouseEvent evt)
  {
    SelectableLabel label = (SelectableLabel)evt.getSource();
    int category = label.getNumber();
    pointsLabel[category].setText( unknownPoints );
  }
  
  public void actionPerformed(ActionEvent e)
  {
    JButton source = (JButton)e.getSource();
    if (source == rollButton)
    {
      // Disable Net player (whether single player mode or not -- no need to bother checking,
      // since we'll disable these buttons either way.  And its just a waste of time to check
      // to see if they're disabled already).
      hostButton.setEnabled(false);  
      connectButton.setEnabled(false);
      // ---------------------------------------
      
      rollButton.setEnabled(false);
      if (categoryAssigned != noCategory)
      {
        // The current roll was assigned to a category, so we can proceed to the next turn.
        categoryAssigned = noCategory;
        gameCoordinator.nextTurn();
        setGameStatus("Rolling the dice for turn " + Integer.toString(gameCoordinator.turnCount()+1) + ".");
        scrambleAllDie();
      }
      else if (gameCoordinator.rollsRemaining() <= 0)
      {
        setGameStatus("No rerolls remaining. You must assign the roll to an available category.");
      }
      else
      {
        // Check for whether this is a partial reroll or a complete reroll
        int i;
        for (i = 0; i < dieLabel.length; ++i)
        {
          if (dieLabel[i].isSelected())
          {
            break;
          }
        }
        if (i == dieLabel.length)
        {   
          // No die were selected, so automatically reroll all of them (complete reroll)
          setGameStatus("Rolling all the dice.");
          gameCoordinator.rerollAll();
          scrambleAllDie();
        }
        else
        {
          // Selected die only (partial reroll)
          setGameStatus("Rolling the selected die.");
          gameCoordinator.reroll(dieLabel);
          scrambleSelectedDie();
        }        
      }
    }  // end rollButton processing
    
    else if (source == suggestButton)
    {
      // TBD
    }
    
    else if (source == resetButton)
    {
      init();
    }
    
    else if (source == hostButton)
    {
      // SETUP SERVER
      yahtzeeServer = new YahtzeeServer(this, Integer.parseInt( portText.getText() ),  addressText.getText());
      if (yahtzeeServer != null)
      {
        yahtzeeServer.start();
        try 
        {
          Thread.sleep(20);  // give time for the server to actually start listening for clients
        } catch (Exception ex)
        {
        }
        if ((yahtzeeServer != null) && (yahtzeeServer.isAlive()))
        {
          this.setSize(1430, 720);
          this.repaint();
          startButton.setEnabled(false);
          hostButton.setEnabled(false);
          rollButton.setEnabled(false);
          connectButton.setEnabled(false);
          suggestButton.setEnabled(false);
        }
        else
        {
          yahtzeeServer = null;
        }   
        setPlayerName(1, getIdentity());
      }
    }  // end hostButton processing
    
    else if (source == startButton)
    {
      startButton.setEnabled(false);
      yahtzeeServer.setAcceptingConnections(false);
      yahtzeeServer.setGameInProgress(true);
      rollButton.setEnabled(true);
      yahtzeeServer.broadcastStartOfGame(playerName);
      setConnectStatus("Start. No more players will be accepted.");
    }
    
    else if (source == connectButton)
    {
      yahtzeeClient = new YahtzeeClient(this);
      if (yahtzeeClient.connect(addressText.getText(), Integer.parseInt(portText.getText())))
      {
        yahtzeeClient.start();
        rollButton.setEnabled(false);
        hostButton.setEnabled(false);
        connectButton.setEnabled(false);
      }
      else
      {
        yahtzeeClient = null;
        //setConnectStatus("Unable to connect to server");
      }
    }
  }
  
  private void doNextTurn()
  {
    updateUpperLowerTotals();
    
    rollButton.setEnabled(false);
    gameCoordinator.endTurn();
    
    if (yahtzeeServer != null)
    {
      setConnectStatus("Server category assigned.  Next client turn.");
      yahtzeeServer.broadcastCategoryAssignments(assignedDieLabel, scoreLabel);      
      yahtzeeServer.nextPlayer();
    }
    else if (yahtzeeClient != null)
    {
      //setConnectStatus("Client category assigned.  Next players turn.");      
      yahtzeeClient.broadcastCategoryAssignments(assignedDieLabel);
      yahtzeeClient.nextPlayer();
    }
    else
    {
      // Single player game
      yourTurn();
    }
    
    this.updateRollsRemaining();
  }
    
  public void gameCanStart()
  {
    startButton.setEnabled(true);
  }
  
  public void gameOver(int winnerIndex)
  {
    if ((yahtzeeServer != null) || (yahtzeeClient != null))
    {
      if (winnerIndex == assignedPlayerIndex)
      {
        setGameStatus("You scored the highest!");
      }
      else
      {
        setGameStatus(identityLabel[winnerIndex].getText() + " has won the game!");
      }
    }
    else
    {
      // Single Player Mode
      setGameStatus("Game Over");
    }
  }
  
  public String getIdentity() 
  {
    String s = identityText.getText();
    if (s.equals(""))
    {
      s = addressText.getText();
      if (s.equals(""))
      {
        s = "UNKOWN";
      }
    }
    return s;
  }  
  
  public void init()
  { 
    reset();

    loadImages();

    // Set the default IP address to be shown next to the "Connect" button
//    addressText.setText("0.0.0.0");
    // Attempt to default to the IP address of the host machine...
    try 
    {
      byte[] ia_b = InetAddress.getLocalHost().getAddress();
      // Convert the signed bytes into unsigned integers...
      int[] ia = new int[ia_b.length];
      for (int i = 0; i < ia.length; ++i)
      {
        ia[i] = ia_b[i] & 0xFF;
      }
      String iaStr = Integer.toString(ia[0]) + "." + Integer.toString(ia[1]) + "." + Integer.toString(ia[2]) + "." + Integer.toString(ia[3]);
      addressText.setText(iaStr);
    }
    catch (Exception e)
    {
      addressText.setText("localHost");
    } 
    
    gameCoordinator = new GameCoordinator(numberOfDie);
    
    /*
      New GUI layout:
     
     Identity ___________  [Connect] ___________  Port ____  [host] [start] [reset]
     connectStatusLabel
     --------         | category_header  points    identity1            identity2            identity3             identity4
     | die1 |-------- | category_1       sc_pL_c1  _ _ _ _ _  sc_p1_c1  _ _ _ _ _  sc_p2_c1  _ _ _ _ _   sc_p3_c1  _ _ _ _ _  sc_p4_c1
     -------|| die2 | | ...
     | die3 |-------- | upper_total                p1_ut                p2_ut                p3_ut                 p4_ut
     --------| die4 | | bonus                      p1_bon               p2_bon               p3_bon                p4_bon
     | die5 |-------- | category_N       sc_pN_cN  _ _ _ _ _  sc_p1_cN  _ _ _ _ _  sc_p2_cN  _ _ _ _ _   sc_p3_cN  _ _ _ _ _  sc_p4_cN
     --------| rleft| | lower_total                p1_lt                p2_lt                p3_lt                 p4_lt
             -------- | grand_total                p1_gt                p2_gt                p3_gt                 p4_gt
     timer | gameStatusLabel
     
     */
    
    /* (old) Approximate layout of GUI:
     
       Identity  ___________    [Connect] ______________  Port _____   connectStatusLabel  [host] [start] [reset]
       [                   die1     die2    die3    die4    die5          ]  rollRemaining
                                                   [roll] [suggest]
       category_header  points  identity1        identity2        identity3        identity4
       Category 1         ---   _ _ _ _ _  sc_p1  _ _ _ _ _ sc_p2 _ _ _ _ _ sc_p3  _ _ _ _ _ sc_p4
       ...
       upper total                p1_ut             p2_ut           p3_ut             p4_ut
       bonus                      p1_bon            p2_bon          p3_bon            p4_bon
       ...
       Category N         ---   _ _ _ _ _   sc_p1 _ _ _ _ _ sc_p2  _ _ _ _ _ sc_p3 _ _ _ _ _ sc_p4
       lower total                p1_lt             p2_lt           p3_lt             p4_lt
       grand total                p1_gt             p2_gt           p3_gt             p4_gt
       timer  gameStatusLabel
     
     */
    
/*
  OLD GUI layout:
    JPanel diceTrayPanel = new JPanel();
    diceTrayPanel.setLayout(new BoxLayout(diceTrayPanel, BoxLayout.X_AXIS));
    for (int index = 0; index < numberOfDie; ++index)
    {
      dieLabel[index] = new SelectableLabel(index, false);
      dieLabel[index].addMouseListener(
         new java.awt.event.MouseAdapter() {
           public void mouseClicked(java.awt.event.MouseEvent evt) { actionDieMouseClicked(evt); }
         }
      );
      diceTrayPanel.add(dieLabel[index]);
    }
        
    JPanel controlPanel = new JPanel();
    controlPanel.setLayout(new FlowLayout());
    controlPanel.add(new JLabel("Identity"));
    controlPanel.add(identityText);
    controlPanel.add(connectButton);        
    controlPanel.add(addressText);
    controlPanel.add(new Label("Port:"));
    controlPanel.add(portText);
    controlPanel.add(connectStatusLabel);
    controlPanel.add(resetButton);
    controlPanel.add(hostButton);
    
    startButton.setEnabled(false);
    controlPanel.add(startButton);
    
    resetButton.setEnabled(true);
    controlPanel.add(resetButton);
    
    suggestButton.setEnabled(false);
    
    JPanel trayAndCountPanel = new JPanel();
    trayAndCountPanel.setLayout(new FlowLayout());
    trayAndCountPanel.add(diceTrayPanel);
    trayAndCountPanel.add(rollsRemainingLabel);    
    
    JPanel rollControlPanel = new JPanel();
    rollControlPanel.setLayout(new FlowLayout());
    rollControlPanel.add(rollButton);
    rollControlPanel.add(suggestButton);
    
    JPanel scorecardPanel = new JPanel();
    GridBagLayout scorecardLayout = new GridBagLayout();
    scorecardPanel.setLayout(scorecardLayout); //numberOfCategories+4,9,1,1) ); 
    GridBagConstraints c = new GridBagConstraints();
    c.fill = GridBagConstraints.BOTH;

    // columns: category, points, roll_p0, score_p0, roll_p1, score_p1, roll_p3, score_p3, roll_p4, score_p4

    for (int i = 0; i < maxPlayersPerGame; ++i)
    {  
      upperTotalLabel[i] = new JLabel("0");
      bonusLabel[i] = new JLabel("0");
      lowerTotalLabel[i] = new JLabel("0");      
      identityLabel[i] = new JLabel("Player " + i);
    }
    
    c.weightx = 1.0;
    JLabel categoryHeaderLabel = new JLabel("Category");
    scorecardLayout.setConstraints(categoryHeaderLabel, c);
    scorecardPanel.add( categoryHeaderLabel );     // 1
    scorecardPanel.add( new JLabel(" "));          // 2  points
    scorecardPanel.add( identityLabel[0] );        // 3
    scorecardPanel.add( new JLabel("Score"));      // 4
    scorecardPanel.add( identityLabel[1] );        // 5
    scorecardPanel.add( new JLabel("Score"));      // 6
    scorecardPanel.add( identityLabel[2] );        // 7
    scorecardPanel.add( new JLabel("Score"));      // 8
    scorecardPanel.add( identityLabel[3] );        // 9
    JLabel last_score = new JLabel("Score");
    c.gridwidth = GridBagConstraints.REMAINDER;
    scorecardLayout.setConstraints(last_score, c);
    scorecardPanel.add( last_score );              // 10
    
    for (int categoryIndex = 0; categoryIndex < numberOfCategories; ++categoryIndex)
    {
      if (categoryIndex == 6)
      {        
        scorecardPanel.add( new JLabel("Upper Total") );  // 1
        scorecardPanel.add( new JLabel(" ") );            // 2 points
        scorecardPanel.add( new JLabel(" ") );            // 3
        scorecardPanel.add( upperTotalLabel[0] );         // 4
        scorecardPanel.add( new JLabel(" ") );            // 5
        scorecardPanel.add( upperTotalLabel[1] );         // 6
        scorecardPanel.add( new JLabel(" ") );            // 7
        scorecardPanel.add( upperTotalLabel[2] );         // 8
        scorecardPanel.add( new JLabel(" ") );            // 9
        c.gridwidth = GridBagConstraints.REMAINDER;
        scorecardLayout.setConstraints(upperTotalLabel[3], c);
        scorecardPanel.add( upperTotalLabel[3] );         // 10
        
        scorecardPanel.add( new JLabel("Bonus") );        // 1
        scorecardPanel.add( new JLabel(" ") );            // 2 points
        scorecardPanel.add( new JLabel(" ") );            // 3
        scorecardPanel.add( bonusLabel[0] );              // 4
        scorecardPanel.add( new JLabel(" ") );            // 5
        scorecardPanel.add( bonusLabel[1] );              // 6
        scorecardPanel.add( new JLabel(" ") );            // 7
        scorecardPanel.add( bonusLabel[2] );              // 8
        scorecardPanel.add( new JLabel(" ") );            // 9
        c.gridwidth = GridBagConstraints.REMAINDER;
        scorecardLayout.setConstraints(bonusLabel[3], c);
        scorecardPanel.add( bonusLabel[3] );              // 10
      }
      
      categoryLabel[categoryIndex] = new SelectableLabel(categoryIndex, false);
      categoryLabel[categoryIndex].setIcon( categoryImage[categoryIndex] );
      categoryLabel[categoryIndex].addMouseListener(
        new java.awt.event.MouseAdapter() {
          public void mouseClicked(java.awt.event.MouseEvent evt) { actionCategoryMouseClicked(evt); }
          
          public void mouseEntered(java.awt.event.MouseEvent evt) { actionEnteredPointsMotion(evt); }
          public void mouseExited(java.awt.event.MouseEvent evt) { actionExitedPointsMotion(evt); }
        }
      );
      scorecardPanel.add(categoryLabel[categoryIndex]);                     // 1
      
      pointsLabel[categoryIndex] = new SelectableLabel(categoryIndex, false);
      pointsLabel[categoryIndex].setFont(new Font("Courier New",  Font.BOLD, 18));
      pointsLabel[categoryIndex].setText(unknownPoints);
      pointsLabel[categoryIndex].setNumber(categoryIndex);
      scorecardPanel.add(pointsLabel[categoryIndex]);                       // 2
      
      JPanel[] rollPanel = new JPanel[maxPlayersPerGame];
      for (int player = 0; player < maxPlayersPerGame; ++player)
      {
        rollPanel[player] = new JPanel();
        rollPanel[player].setLayout( new BoxLayout(rollPanel[player], BoxLayout.X_AXIS) );
        for (int die = 0; die < numberOfDie; ++die)
        {
          assignedDieLabel[player][die][categoryIndex] = new SelectableLabel(0,  false);
          assignedDieLabel[player][die][categoryIndex].setIcon( blankIcon );
          rollPanel[player].add( assignedDieLabel[player][die][categoryIndex] );

          assignedDieLabel[player][die][categoryIndex].addMouseListener(
            new java.awt.event.MouseAdapter() {
              public void mouseClicked(java.awt.event.MouseEvent evt) { actionAssignedDieClicked(evt); }
            }
          );
        }
        scorecardPanel.add(rollPanel[player]);         // 3, 5, 7, 9
        
        scoreLabel[player][categoryIndex] = new SelectableLabel(0, false);
        scoreLabel[player][categoryIndex].setText("0");
        scoreLabel[player][categoryIndex].setNumber(0);
        if (player == maxPlayersPerGame-1)
        {
          c.gridwidth = GridBagConstraints.REMAINDER;
          scorecardLayout.setConstraints(scoreLabel[player][categoryIndex], c);   
        }
        scorecardPanel.add( scoreLabel[player][categoryIndex] );  // 4, 6, 8, 10
      }
    }
        
    scorecardPanel.add( new JLabel("Lower Total") );  // 1
    scorecardPanel.add( new JLabel(" ") );            // 2    
    scorecardPanel.add( new JLabel(" ") );            // 3
    scorecardPanel.add( lowerTotalLabel[0] );         // 4
    scorecardPanel.add( new JLabel(" ") );            // 5
    scorecardPanel.add( lowerTotalLabel[1] );         // 6 
    scorecardPanel.add( new JLabel(" ") );            // 7
    scorecardPanel.add( lowerTotalLabel[2] );         // 8
    scorecardPanel.add( new JLabel(" ") );            // 9
    c.gridwidth = GridBagConstraints.REMAINDER;
    scorecardLayout.setConstraints(lowerTotalLabel[3], c);
    scorecardPanel.add( lowerTotalLabel[3] );         // 10
    
    for (int i = 0; i < grandTotalLabel.length; ++i)
    {
      grandTotalLabel[i] = new SelectableLabel(0, false);
      grandTotalLabel[i].setText("" + grandTotalLabel[i].getNumber());
    }
    
    scorecardPanel.add( new JLabel("Grand Total") );  // 1
    scorecardPanel.add( new JLabel(" ") );            // 2
    scorecardPanel.add( new JLabel(" ") );            // 3
    scorecardPanel.add( grandTotalLabel[0] );         // 4
    scorecardPanel.add( new JLabel(" ") );            // 5
    scorecardPanel.add( grandTotalLabel[1] );         // 6
    scorecardPanel.add( new JLabel(" ") );            // 7
    scorecardPanel.add( grandTotalLabel[2] );         // 8
    scorecardPanel.add( new JLabel(" ") );            // 9
    c.gridwidth = GridBagConstraints.REMAINDER;
    scorecardLayout.setConstraints(grandTotalLabel[3], c);
    scorecardPanel.add( grandTotalLabel[3] );         // 10

    GridBagLayout layout = new GridBagLayout();
    Container pane = this.getContentPane();    
    pane.setLayout(layout); 
    c = new GridBagConstraints();
    c.fill = GridBagConstraints.BOTH;
    
    c.weightx = 1.0;
    c.gridwidth = GridBagConstraints.REMAINDER;
    layout.setConstraints(controlPanel, c);
    pane.add(controlPanel);
    
    layout.setConstraints(trayAndCountPanel, c);
    pane.add(trayAndCountPanel);
    
    c.weightx = 0.0;
    layout.setConstraints(rollControlPanel, c);
    pane.add(rollControlPanel);
    
    layout.setConstraints(scorecardPanel, c);
    pane.add(scorecardPanel);
    
    gameStatusLabel.setFont( new Font("Arial Bold", Font.ITALIC, 18) );
    layout.setConstraints(gameStatusLabel, c);
    pane.add(timerLabel);
    pane.add(gameStatusLabel);
 */

    Container main_pane = this.getContentPane();    
    main_pane.setLayout(null); 
    
    int rollPanelWidth = blankIcon.getIconWidth() * numberOfDie;    
    
    for (int index = 0; index < numberOfDie; ++index)
    {
      dieLabel[index] = new SelectableLabel(index, false);
      dieLabel[index].addMouseListener(
         new java.awt.event.MouseAdapter() {
           public void mouseClicked(java.awt.event.MouseEvent evt) { actionDieMouseClicked(evt); }
         }
      );
      main_pane.add(dieLabel[index]);
    }
    
    dieLabel[0].setBounds(
      5,  50,  // x, y
      dieImage[0].getIconWidth(), dieImage[0].getIconHeight()  // width, height
    );  // DIE #1
    dieLabel[1].setBounds(
      dieImage[0].getIconWidth() + 10, dieLabel[0].getY() + dieImage[0].getIconHeight() / 2,  
      dieImage[1].getIconWidth(), dieImage[1].getIconHeight()
    );  // DIE #2
    dieLabel[2].setBounds(
      dieLabel[0].getX(),  dieLabel[0].getY() + dieImage[0].getIconHeight() + 10, 
      dieImage[2].getIconWidth(), dieImage[2].getIconHeight()
    );  // DIE #3
    dieLabel[3].setBounds(
      dieLabel[1].getX(), dieLabel[1].getY() + dieImage[1].getIconHeight() + 10, 
      dieImage[3].getIconWidth(), dieImage[3].getIconHeight()
    );  // DIE #4
    dieLabel[4].setBounds(
      dieLabel[0].getX(),  dieLabel[2].getY() + dieImage[2].getIconHeight() + 10, 
      dieImage[4].getIconWidth(), dieImage[4].getIconHeight()
    );  // DIE #5

    main_pane.add(rollsRemainingLabel);
    rollsRemainingLabel.setBounds(
      dieLabel[4].getX(), dieLabel[4].getY() + dieLabel[4].getHeight() + 10, 
      turnLeftImage[0].getIconWidth(), turnLeftImage[0].getIconHeight()
    );
    
    main_pane.add(rollButton);
    rollButton.setBounds(rollsRemainingLabel.getX() + rollsRemainingLabel.getWidth() + 2, rollsRemainingLabel.getY() + rollsRemainingLabel.getHeight()/2-50, 100, 48);
    
    suggestButton.setEnabled(false);
    main_pane.add(suggestButton);
    suggestButton.setBounds(rollButton.getX(), rollButton.getY() + rollButton.getHeight() + 2, 100, 48);
        
    main_pane.add(hostButton);
    hostButton.setBounds(200, 5, 100, 24);
    
    startButton.setEnabled(false);
    main_pane.add(startButton);
    startButton.setBounds(320, 5, 100, 24);

    JLabel identity_label = new JLabel("Identity (enter your name here)");
    main_pane.add(identity_label);  
    identity_label.setBounds(200, hostButton.getY() + hostButton.getHeight() + 2, 200, 24);
    
    main_pane.add(identityText);
    identityText.setBounds(identity_label.getX(), identity_label.getY() + identity_label.getHeight() + 2,  rollPanelWidth, 24);
    
    main_pane.add(connectButton);
    connectButton.setBounds(440, 5, 122, 24);
    
    main_pane.add(addressText);
    addressText.setBounds(connectButton.getX(), connectButton.getY() + connectButton.getHeight() + 2, 122, 24);
    
    JLabel port_label = new JLabel("Port:");
    main_pane.add(port_label);
    port_label.setBounds(addressText.getX(), addressText.getY() + addressText.getHeight() + 2, 40, 24);
    
    main_pane.add(portText);
    portText.setBounds(port_label.getX() + port_label.getWidth() + 2, port_label.getY(), 80, 24);
    
    main_pane.add(connectStatusLabel);
    connectStatusLabel.setBounds(connectButton.getX() + connectButton.getWidth() + 2, connectButton.getY(), 800, 24);
    
    resetButton.setEnabled(true);
    main_pane.add(resetButton);
    resetButton.setBounds(5, 5, 100, 24);
        
    // columns: category, points, roll_p0, score_p0, roll_p1, score_p1, roll_p3, score_p3, roll_p4, score_p4

    for (int i = 0; i < maxPlayersPerGame; ++i)
    {  
      upperTotalLabel[i] = new JLabel("0");
      bonusLabel[i] = new JLabel("0");
      lowerTotalLabel[i] = new JLabel("0");      
      identityLabel[i] = new JLabel("Player " + i);
      grandTotalLabel[i] = new SelectableLabel(0, false);
      grandTotalLabel[i].setText("" + grandTotalLabel[i].getNumber());
    }
    
    // ROW 1
    int pointLabelWidth = 50;
    int column_delta = 5;
    
    JLabel categoryHeaderLabel = new JLabel("Category");
    main_pane.add(categoryHeaderLabel);
    categoryHeaderLabel.setBounds(280, 100, categoryImage[0].getIconWidth(), 24);
    
    main_pane.add(identityLabel[0]);
    identityLabel[0].setBounds(
      categoryHeaderLabel.getX() + categoryHeaderLabel.getWidth() + pointLabelWidth + column_delta, categoryHeaderLabel.getY(), 
      rollPanelWidth, 24
    );
    
    JLabel[] score_Label = new JLabel[4];
    score_Label[0] = new JLabel("Score");
    main_pane.add(score_Label[0]);
    score_Label[0].setBounds(identityLabel[0].getX() + rollPanelWidth + column_delta, categoryHeaderLabel.getY(), 50, 24);
    
    main_pane.add(identityLabel[1]);
    identityLabel[1].setBounds(score_Label[0].getX() + score_Label[0].getWidth() + column_delta, categoryHeaderLabel.getY(), rollPanelWidth, 24);
    
    score_Label[1] = new JLabel("Score");
    main_pane.add(score_Label[1]);
    score_Label[1].setBounds(identityLabel[1].getX() + rollPanelWidth + column_delta, categoryHeaderLabel.getY(), 50, 24);
    
    main_pane.add(identityLabel[2]);
    identityLabel[2].setBounds(score_Label[1].getX() + score_Label[1].getWidth() + column_delta, categoryHeaderLabel.getY(), rollPanelWidth, 24);
    
    score_Label[2] = new JLabel("Score");
    main_pane.add(score_Label[2]);
    score_Label[2].setBounds(identityLabel[2].getX() + rollPanelWidth + column_delta, categoryHeaderLabel.getY(), 50, 24);
    
    main_pane.add(identityLabel[3]);
    identityLabel[3].setBounds(score_Label[2].getX() + score_Label[2].getWidth() + column_delta, categoryHeaderLabel.getY(), rollPanelWidth, 24);
    
    score_Label[3] = new JLabel("Score");
    main_pane.add(score_Label[3]);
    score_Label[3].setBounds(identityLabel[3].getX() + rollPanelWidth + column_delta, categoryHeaderLabel.getY(), 50, 24);
    // END ROW 1
    
    for (int categoryIndex = 0; categoryIndex < numberOfCategories; ++categoryIndex)
    {
      categoryLabel[categoryIndex] = new SelectableLabel(categoryIndex, false);
      categoryLabel[categoryIndex].setIcon( categoryImage[categoryIndex] );
      categoryLabel[categoryIndex].addMouseListener(
        new java.awt.event.MouseAdapter() {
          public void mouseClicked(java.awt.event.MouseEvent evt) { actionCategoryMouseClicked(evt); }
          
          public void mouseEntered(java.awt.event.MouseEvent evt) { actionEnteredPointsMotion(evt); }
          public void mouseExited(java.awt.event.MouseEvent evt) { actionExitedPointsMotion(evt); }
        }
      );
      
      main_pane.add(categoryLabel[categoryIndex]);
      int nudge_delta = (categoryIndex == 6) ? 10 : 2;
      categoryLabel[categoryIndex].setBounds(
        categoryHeaderLabel.getX(), categoryHeaderLabel.getY() + categoryHeaderLabel.getHeight() + nudge_delta + categoryIndex * categoryImage[categoryIndex].getIconHeight(), 
        categoryImage[categoryIndex].getIconWidth(), categoryImage[categoryIndex].getIconHeight()
      );

      pointsLabel[categoryIndex] = new SelectableLabel(categoryIndex, false);
      pointsLabel[categoryIndex].setFont(new Font("Courier New",  Font.BOLD, 18));
      pointsLabel[categoryIndex].setText(unknownPoints);
      pointsLabel[categoryIndex].setNumber(categoryIndex);
      
      main_pane.add(pointsLabel[categoryIndex]);
      pointsLabel[categoryIndex].setBounds(
        categoryLabel[categoryIndex].getX() + categoryLabel[categoryIndex].getWidth() + 2, categoryLabel[categoryIndex].getY(),
        pointLabelWidth, categoryImage[categoryIndex].getIconHeight()
      );
 
      JPanel[] rollPanel = new JPanel[maxPlayersPerGame];
      for (int player = 0; player < maxPlayersPerGame; ++player)
      {
        //rollPanel[player] = new JPanel();
        for (int die = 0; die < numberOfDie; ++die)
        {
          assignedDieLabel[player][die][categoryIndex] = new SelectableLabel(0,  false);
          assignedDieLabel[player][die][categoryIndex].setIcon( blankIcon );
          //rollPanel[player].add( assignedDieLabel[player][die][categoryIndex] );
          main_pane.add(assignedDieLabel[player][die][categoryIndex]);
          assignedDieLabel[player][die][categoryIndex].setBounds(
            identityLabel[player].getX() + die * blankIcon.getIconWidth(), //score_Label[player].getX() + score_Label[player].getWidth() + 2 + die * blankIcon.getIconWidth(), 
            pointsLabel[categoryIndex].getY(), 
            blankIcon.getIconWidth(), blankIcon.getIconHeight()
          );

          assignedDieLabel[player][die][categoryIndex].addMouseListener(
            new java.awt.event.MouseAdapter() {
              public void mouseClicked(java.awt.event.MouseEvent evt) { actionAssignedDieClicked(evt); }
            }
          );
        }
        //main_pane.add(rollPanel[player]);
        //rollPanel[player].setBounds(score_Label[player].getX() + score_Label[player].getWidth(), score_Label[player].getHeight(), rollPanelWidth, blankIcon.getIconHeight());
        
        scoreLabel[player][categoryIndex] = new SelectableLabel(0, false);
        scoreLabel[player][categoryIndex].setText("0");
        scoreLabel[player][categoryIndex].setNumber(0);
        main_pane.add(scoreLabel[player][categoryIndex]);
        scoreLabel[player][categoryIndex].setBounds(
          score_Label[player].getX(), pointsLabel[categoryIndex].getY(), 
          score_Label[player].getWidth(),  pointsLabel[categoryIndex].getHeight()
        );
      }
    }
    
    JLabel upper_TotalLabel = new JLabel("Upper Total");
    main_pane.add(upper_TotalLabel);
    upper_TotalLabel.setBounds(
      categoryHeaderLabel.getX(), categoryLabel[numberOfCategories-1].getY() + categoryLabel[numberOfCategories-1].getHeight(), 
      categoryHeaderLabel.getWidth(), categoryHeaderLabel.getHeight()
    );

    JLabel bonus_Label = new JLabel("Bonus");
    main_pane.add(bonus_Label);
    bonus_Label.setBounds(
      upper_TotalLabel.getX(), upper_TotalLabel.getY() + upper_TotalLabel.getHeight(), 
      categoryHeaderLabel.getWidth(), categoryHeaderLabel.getHeight()
    );
    
    JLabel lower_TotalLabel = new JLabel("Lower Total");
    main_pane.add(lower_TotalLabel);
    lower_TotalLabel.setBounds(
      bonus_Label.getX(), bonus_Label.getY() + bonus_Label.getHeight(),
      categoryHeaderLabel.getWidth(), categoryHeaderLabel.getHeight()
    );
    
    JLabel grand_TotalLabel = new JLabel("Grand Total");
    main_pane.add(grand_TotalLabel);
    grand_TotalLabel.setBounds(
      lower_TotalLabel.getX(), lower_TotalLabel.getY() + lower_TotalLabel.getHeight(),
      categoryHeaderLabel.getWidth(), categoryHeaderLabel.getHeight()
    );

    for (int i = 0; i < maxPlayersPerGame; ++i)
    {
      main_pane.add(upperTotalLabel[i]); 
      upperTotalLabel[i].setBounds(score_Label[i].getX(), upper_TotalLabel.getY(), score_Label[i].getWidth(), categoryHeaderLabel.getHeight());
      main_pane.add(bonusLabel[i]);
      bonusLabel[i].setBounds(score_Label[i].getX(),  bonus_Label.getY(), score_Label[i].getWidth(), categoryHeaderLabel.getHeight());
      main_pane.add( lowerTotalLabel[i] );
      lowerTotalLabel[i].setBounds(score_Label[i].getX(),  lower_TotalLabel.getY(), score_Label[i].getWidth(), categoryHeaderLabel.getHeight());
      main_pane.add( grandTotalLabel[i] );       
      grandTotalLabel[i].setBounds(score_Label[i].getX(),  grand_TotalLabel.getY(), score_Label[i].getWidth(), categoryHeaderLabel.getHeight());
    }
    
    gameStatusLabel.setFont( new Font("Arial Bold", Font.ITALIC, 18) );
    // Set the game status to initially show the version of the game
    setGameStatus("Welcome to UnrealYahtzee (version " + VERSION + ")");
        
    main_pane.add(gameStatusLabel);
    gameStatusLabel.setBounds(2, grandTotalLabel[0].getY() + grandTotalLabel[0].getHeight() + 2, 900, 20);
    
    main_pane.add(timerLabel);
    timerLabel.setBounds(rollsRemainingLabel.getX(), rollsRemainingLabel.getY() + rollsRemainingLabel.getHeight(), 100, 24);
    
    // Add the action listeners for the buttons
    connectButton.addActionListener(this);
    rollButton.addActionListener(this);
    suggestButton.addActionListener(this);
    resetButton.addActionListener(this);
    hostButton.addActionListener(this);
    startButton.addActionListener(this);    
    
    updateRollsRemaining();
    updateDiceTray();
    
    timer = new javax.swing.Timer(1000, updateTimerAction);
    timer.start();
    
    this.setSize(750, 720);
  }
  
  ImageIcon loadImage(String filename)
  {
    URL url = this.getClass().getResource(filename);
    if (url != null)
    {
      return new ImageIcon(url);
    }
    setGameStatus("Image file not found - " + filename);
    return null;
  }
  
  void loadImages()
  {
    // Load the initial "Y" image to be used for the unrolled dice on startup
 
    yahtzeeImage = loadImage(imagePath + "/yahtzee.gif"); 

    // Load the images used to show the number of turns left ("3", "2", "1", "0")
    for (int i = 0; i < turnLeftImage.length; ++i)
    {
      turnLeftImage[i] = loadImage(imagePath + "/roll" + Integer.toString(i) + ".gif");
    }
    
    // Load the blank assignment image for the unassigned mini-die
    blankIcon = loadImage(imagePath + "/blank.gif");
    
    // Load the images for the category labels
    for (int index = 0; index < numberOfCategories; ++index)
    {
      categoryImage[index] = loadImage(imagePath + "/category" + (index+1) + ".gif");
    }
    
    
    // Load the images associated with the die faces (normal and selected),
    // and the mini-die images
    for (int index = 0; index < numberOfDieFaces; ++index)
    {
      int indexPlus = index+1;
      
      dieImage[index] = loadImage(imagePath + "/red" + indexPlus + ".gif");
      dieSelectedImage[index] = loadImage(imagePath + "/black" + indexPlus + ".gif");      
      miniDieImage[index] = loadImage(imagePath + "/mini" + indexPlus + ".gif");
    }    
  }
  
  void reset()
  {
    if (scrambleThread != null)
    {
      while ( scrambleThread.isAlive() )
      {
        // busy-wait
      }
      scrambleThread = null;
    }

    if (timer != null)
    {
      timer.stop();
      timer = null;
    }
    
    assignedPlayerIndex = 0;
    winnerIndex = noWinner;  // index of the player who has won the game 
    gameCoordinator = null;
    if (yahtzeeServer != null)
    {
      yahtzeeServer.setGameInProgress(false);
    }
    yahtzeeServer = null;  
    if (yahtzeeClient != null)
    {
      yahtzeeClient.setGameInProgress(false);
    }
    yahtzeeClient = null;
    timer = null;
    categoryAssigned = noCategory;
    
    Container pane = this.getContentPane();
    pane.removeAll();
            
    System.gc();
    
    resetButton.setEnabled(true);
    hostButton.setEnabled(true);
    startButton.setEnabled(true);
    rollButton.setEnabled(true);
    suggestButton.setEnabled(true);
    connectButton.setEnabled(true);
    
    setConnectStatus("");
    
    updateTimerAction = new AbstractAction() 
      {
        private long initialTime = System.currentTimeMillis();
        public void actionPerformed(ActionEvent e)
        {
          long delta = (System.currentTimeMillis() - initialTime) / 1000;  // seconds      
          long minutes = delta / 60;
          long seconds = (delta % 60);
          String time = "";
          if (minutes < 10)
          {
            time += "0";
          }
          time += minutes + ":";
          if (seconds < 10)
          {
            time += "0";
          }
          time += seconds;
          timerLabel.setText(time);
        }
      };    
  }
  
  private void scrambleAllDie()
  {
    scrambleThread = new Thread()
      {
        public void run()
        {
          Random random = new Random();
          long startTime = System.currentTimeMillis();
          long duration = random.nextInt(1200) + 800;
          while (System.currentTimeMillis() - startTime < duration)
          {
            for (int i = 0; i < dieLabel.length; ++i)
            {
              //dieLabel[i].setIcon( dieImage[ random.nextInt(6)] );               
              dieLabel[i].setIcon( dieImage[ random.nextInt(6)] );  
              dieLabel[i].repaint();
              dieLabel[i].invalidate();
            }
            
            try 
            {
              Thread.sleep( random.nextInt(200) + 50 );
            }
            catch (Exception e)
            {
            }
          }
          updateRollsRemaining();  
          updateDiceTray();
          rollButton.setEnabled(true);
        }
      };  
    scrambleThread.start();
  }
  
  private void scrambleSelectedDie()
  {
    scrambleThread = new Thread()
      {
        public void run()
        {
          Random random = new Random();
          long startTime = System.currentTimeMillis();
          long duration = random.nextInt(1200) + 800;
          while (System.currentTimeMillis() - startTime < duration)
          {
            for (int i = 0; i < dieLabel.length; ++i)
            {
              if (dieLabel[i].isSelected())
              {
                dieLabel[i].setIcon( dieImage[ random.nextInt(6)] ); 
                dieLabel[i].repaint();
                dieLabel[i].invalidate();
              }
            }
            
            try 
            {
              Thread.sleep( random.nextInt(200) + 50 );
            }
            catch (Exception e)
            {
            }
          }
          updateRollsRemaining();  
          updateDiceTray();
          
          // Check for JOKER
          boolean hasJokerRoll = false;
          if (gameCoordinator.isJokerRoll( gameCoordinator.dieValues()))
          {
            setGameStatus("You have rolled a joker!");
            hasJokerRoll = true;
          }
          
          rollButton.setEnabled(true);
        }
      };  
    scrambleThread.start();  
  }  
  
  public void setActivePlayerNumber(int playerNumber)
  {
    assignedPlayerIndex = playerNumber-1;
  }

  public void setCategoryRollForPlayer(int playerIndex, int categoryIndex, Vector roll, int score)
  {
    int true_score = score;
    if (true_score == -1)
    {
      // A category assignment from a client.  Let the servers GameCoordinator calculate the real score.
      true_score = gameCoordinator.score(categoryIndex, roll);
    }
    
    for (int i = 0; i < roll.size(); ++i)
    {
      int roll_i = (Integer)roll.elementAt(i);
      assignedDieLabel[playerIndex][i][categoryIndex].setNumber( roll_i );
      if (roll_i > 0)
      {
        assignedDieLabel[playerIndex][i][categoryIndex].setIcon( miniDieImage[ roll_i - 1 ] );
      }
      else
      {
        assignedDieLabel[playerIndex][i][categoryIndex].setIcon( blankIcon );
      }
    }
    scoreLabel[playerIndex][categoryIndex].setText(Integer.toString(true_score));
    scoreLabel[playerIndex][categoryIndex].setNumber(true_score);
    
    if (score == -1)
    {
      yahtzeeServer.broadcastCategoryAssignments(assignedDieLabel, scoreLabel);      
    }
    updateUpperLowerTotals();
  }

  public void setConnectStatus(String status)
  {
    connectStatusLabel.setText(status);
  }
  
  public void setGameStatus(String status)
  {
    gameStatusLabel.setText(status);
  }

  public void setPlayerName(int playerNumber, String playerName)
  {
    int playerIndex = playerNumber-1;
    identityLabel[playerIndex].setText(playerName);
    this.playerName[playerIndex] = playerName;
  }
  
  public void setPlayerName(String newIdentityName)
  {
    identityLabel[assignedPlayerIndex].setText(newIdentityName);
    playerName[assignedPlayerIndex] = newIdentityName;
  }  
  
  public void setPlayerToGo(int playerNumberToGo)
  {
     if (playerNumberToGo-1 == this.assignedPlayerIndex)
     {
       yourTurn();
     }
     else
     {
       setGameStatus( identityLabel[playerNumberToGo].getText() + "'s turn.");
     }
  }  
  
  private void updateDiceTray()
  {
    Vector dieValues = gameCoordinator.dieValues(); 
    for (int index = 0; index < numberOfDie; ++index)
    {
      int dieFaceValue = (Integer)dieValues.elementAt(index);
      if (dieFaceValue == Die.face_unknown)
      {
        dieLabel[index].setIcon(yahtzeeImage);
      }
      else
      {
        dieLabel[index].setIcon( dieImage[ dieFaceValue - 1 ] );
      }
      dieLabel[index].unselect();
    }    
  }
  
  private void updateRollsRemaining()
  {
    rollsRemainingLabel.setIcon( turnLeftImage[gameCoordinator.rollsRemaining()] );
  }
  
  private void updateUpperLowerTotals()
  {
    int[] lowerSum = new int[maxPlayersPerGame];
    int[] upperSum = new int[maxPlayersPerGame];

    for (int i = 0; i < maxPlayersPerGame; ++i)
    {
      lowerSum[i] = 0;
      upperSum[i] = 0;
      
      int upperBonus = 0;
      for (int j = 0; j < numberOfCategories; ++j)
      {
        if (j < 6)
        {
          upperSum[i] += scoreLabel[i][j].getNumber();  
          if (upperSum[i] >= 63)
          {
            upperBonus = 35;
          }
        }
        else
        {
          lowerSum[i] += scoreLabel[i][j].getNumber();
        }
      }
      upperTotalLabel[i].setText( Integer.toString( upperSum[i] ) );      
      bonusLabel[i].setText( Integer.toString(upperBonus) );
      lowerTotalLabel[i].setText( Integer.toString( lowerSum[i] ) );
      grandTotalLabel[i].setNumber( upperSum[i] + lowerSum[i] + upperBonus );
      grandTotalLabel[i].setText( "" + grandTotalLabel[i].getNumber() );
    }
  }  

  public void yourTurn()
  {
    updateRollsRemaining();    
    rollButton.setEnabled(true);
    if ( (yahtzeeServer != null) || (yahtzeeClient != null) )
    {
      // Single player mode.
      setConnectStatus("It is your turn!");
    }
    
    // Determine End of Game status
    if (yahtzeeServer != null)
    {
      if ( (yahtzeeServer.getGameTurns()+1) >= (numberOfCategories*(yahtzeeServer.getNumConnectedClients()+1)) )
      {
        winnerIndex = 0;  // first assume player 1 has won the game... (then check the other player scores)
        for (int i = 1; i < scoreLabel.length; ++i)
        {
          if ( grandTotalLabel[i].getNumber() > grandTotalLabel[winnerIndex].getNumber() )
          {
            winnerIndex = i;
          }
        }
        // Broadcast to all the clients who win the game
        yahtzeeServer.broadcastWinner(winnerIndex+1);
        // And report to self who won the game...
        gameOver(winnerIndex);        
      }
    }
    else if (yahtzeeClient != null)
    {
      // Single Player Mode
      if (gameCoordinator.turnCount()+1 >= numberOfCategories)
      {
        winnerIndex = 0;
        gameOver(winnerIndex);
      }
    }
  }        
/*
  private static void createAndShowGUI()  // part of main(String[])
  {
   JFrame.setDefaultLookAndFeelDecorated(true);
      
    JApplet applet = new YahtzeeApplet();
    applet.init();
    //applet.start(); 
      
    JFrame frame = new JFrame("Yahtzee");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.getContentPane().setLayout(new GridBagLayout());
    frame.getContentPane().add(applet);
    frame.pack();
      
    frame.setVisible(true);
  }
 */ 
  public static void main(String[] args)
  {
    // If running as an applet...
/*    
    javax.swing.SwingUtilities.invokeLater(
      new Runnable() 
      {
        public void run() 
        {
          createAndShowGUI();
        }
      }
    );
  */   
    // If running as an application... NOTE: YahtzeeApplet needs to derive from JFrame if you do this
    
    YahtzeeApplet yahtzee = new YahtzeeApplet();
    yahtzee.init();
    yahtzee.setVisible(true);
    yahtzee.addWindowListener(
       new WindowAdapter()
       {
         public void windowClosing(WindowEvent e)
         {
           System.exit(0);
         }
       }
    );

  }

}
