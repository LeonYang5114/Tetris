///////////////////////////////////////////////////////////////////////////////
// Title:            Tetris
// Files:            TetrisGame.java                
//                   TetrisBlock.java
//                   TetrisMain.java
//
// Author:           Guohong Yang
// Email:            gyang48@wisc.edu
//////////////////////////// 80 columns wide //////////////////////////////////

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * a class that represent a Tetris block
 * 
 * @author Administrator
 *
 */
public class TetrisBlock {

	//use 4 numbers ranging from 0 to 15 to represent a block in a specific 
	//orientation, 4 groups of such number to represent the 4 orientations
	public final static int[][][] TETRIS_BLOCK_POSITIONS = 
		{
			{
				{1, 5, 9, 13},
				{8, 9, 10, 11},
				{2, 6, 10, 14},
				{4, 5, 6, 7}
			}, {
				{1, 5, 8, 9},
				{4, 5, 6, 10},
				{1, 2, 5, 9},
				{0, 4, 5, 6}
			}, {
				{1, 5, 9, 10},
				{2, 4, 5, 6},
				{0, 1, 5, 9},
				{4, 5, 6, 8}
			}, {
				{4, 5, 6, 9},
				{1, 5, 6, 9},
				{1, 4, 5, 6},
				{1, 4, 5, 9}
			}, {
				{1, 2, 4, 5},
				{0, 4, 5, 9},
				{5, 6, 8, 9},
				{1, 5, 6, 10}
			}, {
				{0, 1, 5, 6},
				{1, 4, 5, 8},
				{4, 5, 9, 10},
				{2, 5, 6, 9}
			}, {
				{0, 1, 4, 5},
				{0, 1, 4, 5},
				{0, 1, 4, 5},
				{0, 1, 4, 5}
			}
		};
	//define the color of the 7 blocks
	public final static Color[] TETRIS_COLORS =
		{
			//deep blue
			new Color(20, 30, 166), 
			Color.cyan,
			Color.green,
			//purple
			new Color(134, 20, 166), 
			Color.yellow,
			Color.red,
			//orange
			new Color(255, 64, 0) 
		};
	private TetrisGame game;
	//each block is positioned relative to a reference point. and to move a block, 
	//we move the reference point
	private int[] reference = new int[2];
	private int orientation;
	private int[] bricks;
	private Color color;
	private int type;
	//to construct a specific block, these index should be known
	public final static int I_BLOCK = 0;
	public final static int J_BLOCK = 1;
	public final static int L_BLOCK = 2;
	public final static int T_BLOCK = 3;
	public final static int S_BLOCK = 4;
	public final static int Z_BLOCK = 5;
	public final static int O_BLOCK = 6;
	
	//if no index is given, a block is constructed with random shape, position and orientation
	public TetrisBlock(TetrisGame game) {
		this(game, (int) (Math.random()*7), (int) (Math.random()*4), null);
	}
	
	public TetrisBlock(TetrisGame game, int type, int orientation, int[] ref) {
		if(type < 0 || type > 6 || orientation < 0 || orientation > 3)
			return;
		this.game = game;
		this.type = type;
		this.orientation = orientation;
		color = TETRIS_COLORS[type];
		bricks = TETRIS_BLOCK_POSITIONS[type][orientation];
		if(ref == null) {
			int lastRow = 0;
			int firstCol = 3;
			int lastCol = 0;
			//get the first column/row that contains a brick
			for(int i: bricks) {
				if(i / 4 > lastRow)
					lastRow = i / 4;
				if(i % 4 < firstCol)
					firstCol = i % 4;
				if(i % 4 > lastCol)
					lastCol = i % 4;
			}
			//shift the block according to the reference so that all of the
			//block is in the visible area
			reference[0] = -(lastRow + 1);
			reference[1] = (int) (Math.random()*(10-lastCol+firstCol)-firstCol);
		} else
			reference = ref;
	}
	
	public int getType() {
		return type;
	}
	
	public int[] getReference() {
		return reference;
	}
	
	public void setReference(int[] ref) {
		if(ref.length == 2)
			reference = ref;
	}
	
	/**
	 * get the bricks of the block that are within the visible area and return 
	 * them in a list. if the list is empty, usually it means game is over.
	 * 
	 * @return the bricks of the block that are within the visible area
	 */
	public List<int[]> getBricksWithin() {
		List<int[]> bricksWithin = new ArrayList<int[]>();
		for(int b: bricks) {
			int dRow = b / 4;
			int dCol = b % 4;
			if(reference[0] + dRow >= 0)
				bricksWithin.add(new int[]{reference[0] + dRow, reference[1] + dCol});
		}
		return bricksWithin;
	}
	
	public Color getColor() {
		return color;
	}
	
	/**
	 * spin the block counter-clockwise. if the block has parts outside the 
	 * visible area, it is shifted until all of it is within visible area.
	 */
	public void spinCCW() {
		orientation = (orientation + 1) % 4;
		bricks = TETRIS_BLOCK_POSITIONS[type][orientation];
		int oldRow = reference[0];
		int oldCol = reference[1];
		while(isOutOfBottom())
			reference[0]--;
		while(outOfBoundIndex() != 0)
			reference[1] -= outOfBoundIndex();
		if(isOverLapped()) {
			reference[0]--;
			if(isOverLapped()) {
				reference[0] = oldRow;
				reference[1] = oldCol;
				orientation = (orientation + 3) % 4;
				bricks = TETRIS_BLOCK_POSITIONS[type][orientation];
			}
		} 
	}
	
	/**
	 * similar with the spinCCW
	 */
	public void spinCW() {
		orientation = (orientation + 3) % 4;
		bricks = TETRIS_BLOCK_POSITIONS[type][orientation];
		int oldRow = reference[0];
		int oldCol = reference[1];
		while(isOutOfBottom())
			reference[0]--;
		while(outOfBoundIndex() != 0)
			reference[1] -= outOfBoundIndex();
		if(isOverLapped()) {
			reference[0]--;
			if(isOverLapped()) {
				reference[0] = oldRow;
				reference[1] = oldCol;
				orientation = (orientation + 1) % 4;
				bricks = TETRIS_BLOCK_POSITIONS[type][orientation];
			}
		} 
	}

	/**
	 * try to move the reference left one unit. if block is out of bound or 
	 * overlapped with solidified block, reference is reset.
	 */
	public void left() {
		reference[1]--;
		if(outOfBoundIndex() == -1 || isOverLapped())
			reference[1]++;
	}

	/**
	 * similar with left
	 */
	public void right() {
		reference[1]++;
		if(outOfBoundIndex() == 1 || isOverLapped())
			reference[1]--;
	}
	
	/**
	 * similar with left, but will return whether the attempt is successful, 
	 * because when the block cannot go down anymore, it should be solidified.
	 * 
	 * @return  whether the attempt is successful
	 */
	public boolean down() {
		reference[0]++;
		if(isOverLapped() || isOutOfBottom()) {
			reference[0]--;
			return false;
		}
		return true;
	}
	
	/**
	 * drop the block to the bottom by repeatedly calling down.
	 */
	public void drop() {
		while(down());
	}
	
	/**
	 * solidify the block by adding every brick of the block to the map of the game
	 */
	public void solidify() {
		for(int[] b: getBricksWithin())
			game.getGameCore().getMap()[b[0]][b[1]] = color;
	}
	
	/**
	 * return an index indicating whether the block is out of bound and which
	 * bound is crossed if any.
	 * 
	 * @return an index indicating whether the block is out of bound and which
	 * 			bound is crossed if any.
	 */
	private int outOfBoundIndex() {
		for(int b: bricks) {
			int dCol = b % 4;
			//return -1 if the block is out of the left bound
			if(reference[1] + dCol < 0)
				return -1;
			//return 1 if the block is out of the right bound
			if(reference[1] + dCol >= 10)
				return 1;
		}
		//return 0 if the block is within bounds
		return 0;
	}
	
	/**
	 * help down method check whether the block can go down further
	 * 
	 * @return whether the block can go down further
	 */
	private boolean isOutOfBottom() {
		for(int b: bricks) {
			int dRow = b / 4;
			if(reference[0] + dRow >= 20)
				return true;
		}
		return false;
	}
	
	/**
	 * check whether any brick of the block overlaps with the solidified blocks
	 * 
	 * @return whether any brick of the block overlaps with the solidified blocks
	 */
	private boolean isOverLapped() {
		for(int b: bricks) {
			int dRow = b / 4;
			int dCol = b % 4;
			if(reference[0] + dRow >= 0 && reference[0] + dRow < 20 && reference[1] + dCol >= 0 && reference[1] + dCol < 10)
				if(game.getGameCore().getMap()[reference[0]+dRow][reference[1]+dCol] != null)
					return true;
		}
		return false;
	}
	
}
