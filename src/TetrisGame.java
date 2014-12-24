///////////////////////////////////////////////////////////////////////////////
// Title:            Tetris
// Files:            TetrisGame.java                
//                   TetrisBlock.java
//                   TetrisMain.java
//
// Author:           Guohong Yang
// Email:            gyang48@wisc.edu
//////////////////////////// 80 columns wide //////////////////////////////////

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class TetrisGame {

	private final JFrame WINDOW = new JFrame();
	private final GameCore GAME_CORE = new GameCore();
	private final GameMenu GAME_MENU = new GameMenu();
	private final RightPanel RIGHT_PANEL = new RightPanel();
	private String gameStatus;
	private double updateRate;
	private int renderRate = 60;
	private Thread gameLoop;
	private String difficulty;
	private boolean hasBoosted;
	private Sequencer sequencer = null;
	private boolean isUpdating;

	public TetrisGame(String difficulty) {
		gameStatus = "NEW";
		setDifficulty(difficulty);
		hasBoosted = false;
		GAME_CORE.setPreferredSize(new Dimension(150, 300));
		RIGHT_PANEL.setPreferredSize(new Dimension(90, 250));

		WINDOW.setLayout(new BorderLayout());
		WINDOW.setJMenuBar(GAME_MENU);
		WINDOW.add(GAME_CORE, BorderLayout.WEST);
		WINDOW.add(RIGHT_PANEL, BorderLayout.EAST);
		WINDOW.setResizable(false);
		WINDOW.pack();
		WINDOW.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		WINDOW.setFocusable(true);
		WINDOW.setLocationRelativeTo(null);
		WINDOW.setTitle("Tetris");
		WINDOW.setVisible(true);
		WINDOW.addKeyListener(GAME_CORE);
		renewSequencer();
	}

	public JFrame getWindow() {
		return WINDOW;
	}

	public GameCore getGameCore() {
		return GAME_CORE;
	}

	public String getGameStatus() {
		return gameStatus;
	}

	/**
	 * Setting the difficulty of the game by setting the update rate.
	 * If difficulty is extreme, level will be set to 10 at beginning. 
	 * 
	 * @param difficulty Difficulty of the game
	 */
	public void setDifficulty(String difficulty) {
		this.difficulty = difficulty;
		switch(difficulty) {
		case "Easy":
			updateRate = 2;
			break;
		case "Medium":
			updateRate = 2.5;
			break;
		case "Hard":
			updateRate = 3;
			break;
		case "Extreme":
			updateRate = 3.5;
			GAME_CORE.setLevel(10);
		default:
			break;			
		}
	}

	/**
	 * increase update rate when down key is pressed to boost
	 */
	public void boost() {
		updateRate *= 8;
	}

	/**
	 * decrease update to normal after down key is release
	 */
	public void deboost() {
		updateRate /= 8;
	}

	/**
	 * method for playing BGM in a loop
	 */
	private void renewSequencer() {
		try {
			sequencer = MidiSystem.getSequencer();
			sequencer.open();
			BufferedInputStream midiStream = new BufferedInputStream(this.getClass().getResourceAsStream("/TetrisTheme.mid"));
			Sequence supersequence = MidiSystem.getSequence(midiStream);
			sequencer.setSequence(supersequence);
			sequencer.setLoopStartPoint(7680);
			sequencer.setLoopEndPoint(48000);
			sequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
		} catch (FileNotFoundException e) {}
		catch (MidiUnavailableException e) {}
		catch (IOException e) {}
		catch (InvalidMidiDataException e) {}
	}

	/**
	 * start the game by using a thread. When the run method is invoked, the
	 * thread will get into a while loop. In the while loop, update rate specifies
	 * the interval between the updates of game, and each time the game is updated
	 * the game will also be rendered. If the game hasn't been render for a
	 * certain interval, it will render itself. If the game is neither running
	 * nor pausing, the loop ends.
	 */
	public void start() {
		if(sequencer != null && sequencer.isOpen())
			sequencer.start();
		gameStatus = "RUNNING";
		gameLoop = new Thread() {
			public void run() {
				while(gameStatus.equals("RUNNING") || gameStatus.equals("PAUSED")) {
					double now = System.nanoTime();
					double lastUpdate = now;
					double lastRender = now;
					double secondMark = now;
					while(gameStatus.equals("RUNNING")) {
						while(now - lastUpdate < (1000000000/updateRate)) {
							if(!gameStatus.equals("RUNNING"))
								break;
							if(now - lastRender < (1000000000/renderRate)) {
								GAME_CORE.repaint();
								lastRender = now;
							}
							if(now - secondMark >= 1000000000) {
								GAME_CORE.doEachSecond();
								secondMark = now;
							}
							Thread.yield();
							try {Thread.sleep(1);} catch(Exception e) {};
							now = System.nanoTime();
						}
						while(now - lastUpdate > (1000000000/updateRate)) {
							isUpdating = true;
							GAME_CORE.updateGame();
							isUpdating = false;
							GAME_CORE.repaint();
							lastUpdate = now;
							lastRender = now;
						}
					}
					while(gameStatus.equals("PAUSED")) {
						Thread.yield();
						try {Thread.sleep(1);} catch(Exception e) {};
					}
				}
			}
		};
		gameLoop.start();

	}

	public void pause() {
		if(sequencer != null && sequencer.isRunning())
			sequencer.stop();
		gameStatus = "PAUSED";
		System.out.println("Paused");
	}

	public void unpause() {
		if(sequencer != null && sequencer.isOpen())
			sequencer.start();
		gameStatus = "RUNNING";
		System.out.println("Unpaused");
	}

	public void stop() {
		if(sequencer != null && sequencer.isOpen())
			sequencer.close();
		System.out.println("Stopped");
		gameStatus = "STOPPED";
	}

	/**
	 * renew the game. turn the game to a status of first start.
	 */
	public void renew() {
		renewSequencer();
		System.out.println("Renewed");
		gameStatus = "NEW";
		GAME_CORE.resetGame();
		GAME_CORE.repaint();
		RIGHT_PANEL.repaint();
	}

	public void exit() {
		System.out.println("Exit");
		stop();
		WINDOW.removeKeyListener(GAME_CORE);
		WINDOW.dispose();
	}


	/**
	 * the game menu class
	 * 
	 * @author Administrator
	 *
	 */
	class GameMenu extends JMenuBar implements ActionListener {

		/**
		 * 
		 */
		private static final long serialVersionUID = -4734011750688987954L;
		private final JMenu M_GAME, M_DIFFICULTY, M_SCORE, M_ABOUT;
		private final JMenuItem MI_RESTART, MI_EXIT, MI_PAUSE_UNPAUSE, MI_EASY, 
		MI_MEDIUM, MI_HARD, MI_EXTREME, MI_SCOREBOARD, MI_CREDITS, MI_HELP;
		public GameMenu() {
			M_GAME = new JMenu("Game");
			M_DIFFICULTY = new JMenu("Difficulty");
			M_SCORE = new JMenu("Score");
			M_ABOUT = new JMenu("About");

			MI_RESTART = new JMenuItem("Restart");
			MI_PAUSE_UNPAUSE = new JMenuItem("Pause/Unpause");
			MI_EXIT = new JMenuItem("Exit");

			MI_SCOREBOARD = new JMenuItem("Scoreboard");

			MI_EASY = new JMenuItem("Easy");
			MI_MEDIUM = new JMenuItem("Medium");
			MI_HARD = new JMenuItem("Hard");
			MI_EXTREME = new JMenuItem("Extreme");

			MI_HELP = new JMenuItem("Help");
			MI_CREDITS = new JMenuItem("Credits");

			add(M_GAME);
			add(M_DIFFICULTY);
			add(M_SCORE);
			add(M_ABOUT);
			M_GAME.add(MI_RESTART);
			M_GAME.add(MI_PAUSE_UNPAUSE);
			M_GAME.add(MI_EXIT);
			M_DIFFICULTY.add(MI_HARD);
			M_DIFFICULTY.add(MI_MEDIUM);
			M_DIFFICULTY.add(MI_EASY);
			M_DIFFICULTY.add(MI_EXTREME);
			M_SCORE.add(MI_SCOREBOARD);
			M_ABOUT.add(MI_HELP);
			M_ABOUT.add(MI_CREDITS);

			MI_RESTART.addActionListener(this);
			MI_EXIT.addActionListener(this);
			MI_PAUSE_UNPAUSE.addActionListener(this);
			MI_EASY.addActionListener(this);
			MI_MEDIUM.addActionListener(this);
			MI_HARD.addActionListener(this);
			MI_EXTREME.addActionListener(this);
			MI_SCOREBOARD.addActionListener(this);
			MI_CREDITS.addActionListener(this);
			MI_HELP.addActionListener(this);
		}

		public void actionPerformed(ActionEvent e) {
			if(e.getSource().equals(MI_RESTART)) {
				stop();
				renew();
			}
			if(e.getSource().equals(MI_EXIT)) {
				exit();
			}
			if(e.getSource().equals(MI_PAUSE_UNPAUSE)) {
				if(gameStatus.equals("RUNNING"))
					pause();
				else if(gameStatus.equals("PAUSED"))
					unpause();
			}
			if(e.getSource().equals(MI_EASY)) {
				stop();
				renew();
				setDifficulty("Easy");
			}
			if(e.getSource().equals(MI_MEDIUM)) {
				stop();
				renew();
				setDifficulty("Medium");
			}
			if(e.getSource().equals(MI_HARD)) {
				stop();
				renew();
				setDifficulty("Hard");
			}
			if(e.getSource().equals(MI_EXTREME)) {
				stop();
				renew();
				setDifficulty("Extreme");
			}
			if(e.getSource().equals(MI_SCOREBOARD)) {
				JOptionPane.showMessageDialog(WINDOW, "Difficulty: " + difficulty + "\n"
						+ "First Place: " + GAME_CORE.bestScore[0] + "\n"
						+ "Second Place: " + GAME_CORE.bestScore[1] + "\n"
						+ "Third Place: " + GAME_CORE.bestScore[2] + "\n", "Scoreboard", JOptionPane.INFORMATION_MESSAGE);
			}
			if(e.getSource().equals(MI_CREDITS)) {
				JOptionPane.showMessageDialog(WINDOW, "by Leon Yang" + "\n" + "leonyang1994@gmail.com"
						+ "\n" + "BGM from internet", "Credits", JOptionPane.INFORMATION_MESSAGE);
			}
			if(e.getSource().equals(MI_HELP)) {
				JOptionPane.showMessageDialog(WINDOW, 
						"Enter               ==>    start the game" + "\n"
								+ "Left/Right       ==>    move Tetris block to the left/right" + "\n" 
								+ "Down              ==>    get down faster (with bonus points!!)" + "\n"
								+ "White Space ==>    fast drop the block, or reset the game if it's ended" + "\n"
								+ "Up/Z                ==>    spin the block counter-clockwise" + "\n" 
								+ "X                      ==>    spin the block clockwise" + "\n"
								+ "C                      ==>    hold the current block (can only use once for each trial)" + "\n"
								+ "ESC                 ==>    exit", "Help", JOptionPane.INFORMATION_MESSAGE);
			}
			GAME_CORE.repaint();

		}
	}

	/**
	 * the core of the game. It listens to the key events, calculates the positions
	 * of the blocks, keeps track of the score, draws out the game and other
	 * things.
	 * 
	 * @author Administrator
	 *
	 */
	class GameCore extends JPanel implements KeyListener {

		/**
		 * 
		 */
		private static final long serialVersionUID = 4856036013843213765L;
		private boolean[] keys = new boolean[256];
		private TetrisBlock curr;			//the type of the current block
		private TetrisBlock next;			//the type of the next block
		private Color[][] map;				//use a 2D array to represent the Tetris blocks
		private ArrayList<Integer> needClearLines;				
					//sometimes more than one lines need to be cleared.
		private boolean blocksHasUpdated;		//status variable to tell whether the dropping block has been "solidified"
		private boolean hasResetBoost;			//status variable to tell the boost has been reset
		private BufferedImage baseBlocks;		//store the "solidified" blocks as an image to save calculation
		private int score;
		private int[] bestScore;
		private int level;
		private int numClearedLines;			//the number of cleared lines in one drop. Used to calculate score
		private int secondCounter;
		private boolean hasBoom;				//if 4 lines are cleared in one time, a "boom" is granted
		private int boomCounter;				//after 4 drops since a boom is granted, every block on the screen will be cleared, and with a bonus
		private boolean hasHeld;				//player can hold once each drop. keep track whether hold has been used
		private boolean hasChangedMusicMode;	//music will be changed into another mode after certain levels

		public GameCore() {
			resetGame();
		}

		public Color[][] getMap() {
			return map;
		}

		public int getScore() {
			return score;
		}

		public TetrisBlock getNext() {
			return next;
		}

		public int getLevel() {
			return level;
		}

		public void setLevel(int newLevel) {
			level = newLevel;
		}

		public void setNext() {
			next = new TetrisBlock(TetrisGame.this);
			RIGHT_PANEL.repaint();
		}

		public boolean hasBoom() {
			return hasBoom;
		}

		public void keyPressed(KeyEvent e) {
			if(isUpdating) return;
			
			if(e.getKeyCode() > 255) return;

			if(e.getKeyCode() == KeyEvent.VK_ESCAPE)
				exit();

			if(gameStatus.equals("NEW") && e.getKeyCode() == KeyEvent.VK_ENTER) {
				start();
				setNext();
			}

			if(gameStatus.equals("RUNNING")) {
				switch(e.getKeyCode()) {
				case KeyEvent.VK_LEFT:
					curr.left();
					break;
				case KeyEvent.VK_RIGHT:
					curr.right();
					break;
				case KeyEvent.VK_DOWN:
					if(keys[KeyEvent.VK_DOWN]) {
						if(!hasResetBoost && !hasBoosted) {
							boost();
							hasBoosted = true;
						}
					} else {
						curr.down();
						hasResetBoost = false;
					}
					break;
				case KeyEvent.VK_UP:
					if(!keys[KeyEvent.VK_UP])
						curr.spinCCW();
					break;
				case KeyEvent.VK_Z:
					if(!keys[KeyEvent.VK_Z])
						curr.spinCCW();
					break;
				case KeyEvent.VK_X:
					if(!keys[KeyEvent.VK_X])
						curr.spinCW();
					break;
				case KeyEvent.VK_C:
					hold();
					break;
				case KeyEvent.VK_SPACE:
					if(!keys[KeyEvent.VK_SPACE])
						curr.drop();
					break;
				default :
					break;
				}
			}

			if(e.getKeyCode() == KeyEvent.VK_P && !keys[KeyEvent.VK_P]) {
				if(gameStatus.equals("RUNNING")) {
					pause();
					keys[KeyEvent.VK_P] = true;
				} else			
					if(gameStatus.equals("PAUSED")) {
						unpause();
						keys[KeyEvent.VK_P] = true;
					}
			}

			if(gameStatus.equals("STOPPED"))
				if(e.getKeyCode() == KeyEvent.VK_SPACE) {
					renew();
				}

			keys[e.getKeyCode()] = true;

		}

		public void keyReleased(KeyEvent e) {
			if(e.getKeyCode() > 255) return;

			if(e.getKeyCode() == KeyEvent.VK_DOWN && hasBoosted) {
				deboost();
				hasBoosted = false;
			}

			keys[e.getKeyCode()] = false;

		}

		public void keyTyped(KeyEvent arg0) {}

		/**
		 * If level is over 5, blocks in random position of a line will appear
		 * for a certain interval, and the interval will be shorter as level
		 * goes up.
		 */
		public void doEachSecond() {
			if(level > 5) {
				secondCounter++;
				if(secondCounter >= 35-1.5*level) {
					for(int j = 0; j < 10; j++) {
						for(int i = 1; i < 20; i++)
							map[i-1][j] = map[i][j];
						int colorIndex = (int) (Math.random()*(level+3));
						if(colorIndex >= 7)
							map[19][j] = null;
						else
							map[19][j] = TetrisBlock.TETRIS_COLORS[colorIndex];
					}
					secondCounter = 0;
					blocksHasUpdated = true;
					repaint();
				}
			}
		}

		/**
		 * if the solidified blocks has been updated (such as, a new block is
		 * solidified), update the background image; otherwise, paint the back-
		 * ground image which contains all the solidified blocks.
		 */
		public void paintComponent(Graphics g) {
			g.setColor(Color.darkGray);
			g.fillRect(0, 0, 150, 300);
			if(blocksHasUpdated) {
				Color[][] map = this.map.clone();
				Graphics ig = baseBlocks.getGraphics();
				ig.setColor(Color.darkGray);
				ig.fillRect(0, 0, 150, 300);
				for(int i = 0; i < 20; i++)
					for(int j = 0; j < 10; j++)
						if(map[i][j] != null) {
							drawBrick(15*j, 15*i, map[i][j], ig);
						}
				blocksHasUpdated = false;
			}
			g.drawImage(baseBlocks, 0, 0, this);
			for(int[] b: curr.getBricksWithin()) {
				drawBrick(15*b[1], 15*b[0], curr.getColor(), g);
			}
		}

		/**
		 * change the music, add score, check whether game is
		 * ended, check whether the player has a boom, check whether the game
		 * is boosted. update the game according to the status
		 */
		public void updateGame() {
			//change the music after level 10
			if(level >= 10 && !hasChangedMusicMode) {
				sequencer.setLoopEndPoint(65280);
				hasChangedMusicMode = true;
			}
			//if any number of lines is cleared, add score accordingly
			//the lines that are cleared are from last update, so that player
			//could see a step by step process of how the lines are cleared 
			if(!needClearLines.isEmpty()) {
				switch(needClearLines.size()) {
				case 1:	score += 100;
				break;
				case 2:	score += 400;
				break;
				case 3:	score += 900;
				break;
				case 4:	
					score += 2500;
					hasBoom = true;
					break;
				default:
					break;
				}
				numClearedLines += needClearLines.size();
				
				//only happen if a boom exists. after a boom, game becomes harder
				if(numClearedLines >= 4) { 
					numClearedLines -= 4;
					level++;
					updateRate *= 1.04;
				}
				RIGHT_PANEL.repaint();
				
				//remove the cleared lines and shift any hanging line down
				int shift = 0;
				while(!needClearLines.isEmpty()) {
					for(int i = needClearLines.remove(0) + shift; i > 0; i--) {
						for(int j = 0; j < 10; j++)
							map[i][j] = map[i-1][j];
					}
					for(int j = 0; j < 10; j++)
						map[0][j] = null;
					shift++;
				}
				blocksHasUpdated = true;
				return;
			}
			//if a block cannot go down anymore, block solidifies or game ends
			if(!curr.down()) {
				//if the current block has no part within the visible panel, game ends
				if(curr.getBricksWithin().isEmpty()) {
					//end game
					stop();
					//record the best scores
					if(score > bestScore[0]) {
						JOptionPane.showMessageDialog(WINDOW, "New Best Score!" + "\n" + score);
						bestScore[2] = bestScore[1];
						bestScore[1] = bestScore[0];
						bestScore[0] = score;
						System.out.println("New Best Score: " + score);
					} else if(score > bestScore[1]) {
						bestScore[2] = bestScore[1];
						bestScore[1] = score;
					} else if(score > bestScore[2]) {
						bestScore[2] = score;
					}
					return;
				}
				//if the game does not end, the block is solidified
				curr.solidify();
				//if boom exists, it clears the screen after 4 drops
				if(hasBoom) {
					boomCounter++;
					if(boomCounter >= 3) {
						//the boom works by filling all the lines first, then clearing them
						int firstLineWithBricks = -1;
						for(int i = 0; i < 20 && firstLineWithBricks == -1; i++) {
							boolean isEmpty = true;
							for(int j = 0; j < 10 && isEmpty; j++)
								if(map[i][j] != null)
									isEmpty = false;
							if(!isEmpty) {
								firstLineWithBricks = i;
							}
						}
						for(int i = 19; i >= firstLineWithBricks; i--)
							for(int j = 0; j < 10; j ++) {
								if(map[i][j] == null) {
									map[i][j] = TetrisBlock.TETRIS_COLORS[(int) (Math.random()*7)];
									blocksHasUpdated = true;
									try {Thread.sleep(50);} catch (InterruptedException e) {}
									repaint();
								}
							}
						try {Thread.sleep((long) (1000/updateRate));} catch (InterruptedException e) {}
						map = new Color[20][10];
						score += (20-firstLineWithBricks)*100;
						blocksHasUpdated = true;
						repaint();
						numClearedLines += (20-firstLineWithBricks);
						while(numClearedLines >= 4) {
							numClearedLines -= 4;
							level++;
							updateRate *= 1.04;
						}
						//boom is used after a full screen clear
						boomCounter = 0;
						hasBoom = false;
					}
				}
				curr = next;
				hasHeld = false;
				blocksHasUpdated = true;
				hasResetBoost = true;
				//boost only works for one drop, so that player has to release
				//and press the down key again to boost again
				if(hasBoosted) {
					deboost();
					hasBoosted = false;
				}
				//check whether any line is all filled so that they need to be cleared
				//these lines are cleared in next update
				for(int i = 19; i >= 0; i--) {
					boolean hasNull = false;
					for(int j = 0; j < 10 && !hasNull; j++)
						if(map[i][j] == null)
							hasNull = true;
					if(hasNull == false) {
						needClearLines.add(i);
					}
				}
				next = new TetrisBlock(TetrisGame.this);
			}
			if(hasBoosted)
				score += 5;
			RIGHT_PANEL.repaint();
		}

		/**
		 * reset all game status
		 */
		public void resetGame() {
			curr = new TetrisBlock(TetrisGame.this);
			next = null;
			map = new Color[20][10];
			needClearLines = new ArrayList<Integer>();
			blocksHasUpdated = false;
			hasResetBoost = false;
			baseBlocks = new BufferedImage(150, 300, BufferedImage.TYPE_INT_ARGB);
			score = 0;
			bestScore = new int[]{0, 0, 0};
			level = 1;
			numClearedLines = 0;
			hasBoom = false;
			boomCounter = 0;
			hasHeld = false;
			hasChangedMusicMode = false;
		}

		/**
		 * swap the current block with the next. can only use once each drop
		 */
		private void hold() {
			if(!hasHeld) {
				int currType = curr.getType();
				int[] currRef = curr.getReference();
				curr = next;
				//set the reference as the same
				curr.setReference(currRef);
				//spin the block so that they will not go outside of the visible area
				curr.spinCCW();
				curr.spinCW();
				next = new TetrisBlock(TetrisGame.this, currType, (int) (Math.random()*4), null);
				hasHeld = true;
			}
		}

	}

	/**
	 * A panel that displays the score, the next block, level, and start button.
	 * 
	 * @author Administrator
	 *
	 */
	class RightPanel extends JPanel implements ActionListener {
		/**
		 * 
		 */
		private static final long serialVersionUID = 4290784483041002295L;
		private final int[] nextBlockRef = new int[]{2, 1};
		private JButton startButton;

		public RightPanel() {
			this.setLayout(null);
			startButton = new JButton("Start");
			startButton.setBounds(0, 250, 90, 50);
			add(startButton);
			startButton.addActionListener(this);
			startButton.setFocusable(false);
		}

		public void paintComponent(Graphics g) {
			g.setColor(Color.gray);
			g.fillRect(0, 0, 90, 300);
			g.setColor(Color.white);
			g.drawString("Next Block:", 10, 20);
			g.setColor(Color.green);
			g.drawString("Score:", 10, 120);
			g.drawString(Integer.toString(GAME_CORE.getScore()), 10, 140);
			g.setColor(Color.yellow);
			g.drawString("Level:", 10, 170);
			g.drawString(Integer.toString(GAME_CORE.getLevel()), 10, 190);
			if(GAME_CORE.hasBoom()) {
				g.setColor(Color.red.darker());
				g.drawString("YOU HAS", 10, 220);
				g.drawString("A BOOM!!!", 10, 235);
			}
			if(GAME_CORE.getNext() != null) {
				Color c = GAME_CORE.getNext().getColor();
				TetrisBlock next = new TetrisBlock(TetrisGame.this, GAME_CORE.getNext().getType(), 0, nextBlockRef);
				for(int[] b: next.getBricksWithin())
					drawBrick(15*b[1], 15*b[0], c, g);
			}
		}

		public void actionPerformed(ActionEvent e) {
			if(e.getSource().equals(startButton)) {
				if(gameStatus.equals("NEW")) {
					start();
					GAME_CORE.setNext();
				}
			}
		}
	}

	private void drawBrick(int x, int y, Color color, Graphics g) {
		g.setColor(color.brighter());
		g.fillPolygon(new int[]{x, x+15, x}, new int[]{y, y, y+15}, 3);
		g.setColor(color.darker());
		g.fillPolygon(new int[]{x+15, x+15, x}, new int[]{y, y+15, y+15}, 3);
		g.setColor(color);
		g.fillRect(x+2, y+2, 10, 10);
	}
}
