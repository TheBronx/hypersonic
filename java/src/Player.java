import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

class Player {
	private static final int DEPTH = 6;

    public static void main(String args[]) {
        Scanner in = new Scanner(System.in);
        int width = in.nextInt();
        int height = in.nextInt();
        int myId = in.nextInt();
        Map map = new Map(width, height, myId);

        while (true) {
        	StringBuilder inputs = new StringBuilder();
        	for (int i = 0; i < height; i++) {
        		inputs.append(in.next() + "\n");
            }
            int entities = in.nextInt();
            for (int i = 0; i < entities; i++) {
            	inputs.append(in.nextInt()+" "+in.nextInt()+" "+in.nextInt()+" "+in.nextInt()+" "+in.nextInt()+" "+in.nextInt());
            	if (i<(entities-1)) inputs.append("\n");
            }
            //System.err.println(inputs.toString());
            map.parse(inputs.toString());

            long before = System.nanoTime();
            Move move = map.move(DEPTH);
            double elapsed = (System.nanoTime() - before)/1000000f;
			System.out.println(move.output() + " Move: " + Math.floor(elapsed) + "ms");
			System.err.println("PLAN: " + move.toString());
        }
    }
}

class Map {
	private int w;
	private int h;
	private Cell[][] map;
	private Robot me;
	private int playerId;

	public Map(int w, int h, int playerId) {
        this.w = w;
        this.h = h;
        this.playerId = playerId;
        this.map = new Cell[w][h];
    }
	
	public void parse(String inputs) {
		String[] lines = inputs.split("\n");
		for (int i=0; i<h; i++) {
			String[] chars = lines[i].split("");
			for (int j=0; j<chars.length; j++) {
				map[j][i] = CellFactory.createFromMap(chars[j]);
				map[j][i].setCoordinates(j, i);
			}
		}
		for (int i=h; i<lines.length; i++) {
			String[] chars = lines[i].split(" ");
			int owner = Integer.parseInt(chars[1]);
			int x = Integer.parseInt(chars[2]);
			int y = Integer.parseInt(chars[3]);
			int param1 = Integer.parseInt(chars[4]);
			int param2 = Integer.parseInt(chars[5]);
			map[x][y] = CellFactory.createFromEntity(chars[0], owner, param1, param2);
			map[x][y].setCoordinates(x, y);
			
			if (chars[0].equals("0")) {
				Robot player = new Robot(owner, x, y);
				player.setRemainingBombs(param1);
				player.setBombRange(param2);
				
				if (player.id == playerId) me = player;
				map[x][y].addPlayer(player);
			}
		}
		
		fixBombTurns();
	}
	
	public void fixBombTurns() {
		//TODO performance: sacar las bombas una vez para cada mapa
		List<Cell> bombs = new ArrayList<Cell>();
	    for (int x=0; x<w; x++) {
	        for (int y=0; y<h; y++) {
	            if (map[x][y].type() == CellType.BOMB) {
	                bombs.add(map[x][y]);
	            }
	        }
	    }

	    Collections.sort(bombs);

	    for (int i=0; i<bombs.size(); i++) {
	        Cell bomb = map[bombs.get(i).x][bombs.get(i).y];
	        Integer min = minTurnsLeftOfBombsInRange(bombs.get(i).x, bombs.get(i).y);
	        if (min != null && min < bomb.turnsLeft()) {
	            bomb.setTurnsLeft(min); //adjust this bomb turns
	            //now adjust affected bombs too
	            List<Position> bombsAffected = bombsInRange(bombs.get(i).x, bombs.get(i).y);
	            for (int j=0; j<bombsAffected.size(); j++) {
	                Cell affectedBombCell = map[bombsAffected.get(j).x][bombsAffected.get(j).y];
	                if (affectedBombCell.turnsLeft()>min) affectedBombCell.setTurnsLeft(min);
	            }
	        }
	    }
	}

	private Integer minTurnsLeftOfBombsInRange(int bombX, int bombY) {
		List<Position> bombs = bombsInRange(bombX, bombY);
	    if (bombs==null || bombs.size() == 0) return null;

	    Integer min = null;
	    int x,y;
	    for (int i=0; i<bombs.size(); i++) {
	        x = bombs.get(i).x;
	        y = bombs.get(i).y;
	        if (min==null || map[x][y].turnsLeft() < min) min = map[x][y].turnsLeft();
	    }
	    return min;
	}
	
	private List<Position> bombsInRange(int x, int y) {
		List<Position> bombs = new ArrayList<Position>();

	    for (int i=x+1; i<w; i++) {
	        if (map[i][y].isBox() || map[i][y].isWall() || map[i][y].isItem()) break;
	        if (map[i][y].isBomb() && map[i][y].bombRange() > Math.abs(x - i)) {
	            bombs.add(new Position(i, y));
	        }
	    }

	    for (int i=x-1; i>=0; i--) {
	        if (map[i][y].isBox() || map[i][y].isWall() || map[i][y].isItem()) break;
	        if (map[i][y].isBomb() && map[i][y].bombRange() > Math.abs(x - i)) {
	            bombs.add(new Position(i, y));
	        }
	    }

	    for (int i=y+1; i<h; i++) {
	        if (map[x][i].isBox() || map[x][i].isWall() || map[x][i].isItem()) break;
	        if (map[x][i].isBomb() && map[x][i].bombRange() > Math.abs(y - i)) {
	            bombs.add(new Position(x, i));
	        }
	    }

	    for (int i=y-1; i>=0; i--) {
	        if (map[x][i].isBox() || map[x][i].isWall() || map[x][i].isItem()) break;
	        if (map[x][i].isBomb() && map[x][i].bombRange() > Math.abs(y - i)) {
	        	bombs.add(new Position(x, i));
	        }
	    }

	    return bombs;
	}

	private void set(Cell cell) {
		map[cell.x][cell.y] = cell;
	}
	
	public Cell get(int x, int y) {
		return map[x][y];
	}

	public Robot me() {
		return me;
	}

	private void setMe(Robot me) {
		this.me = me;
	}

	private void setCells(Cell[][] cells) {
		this.map = cells;
	}
	
	public Map copy() {
		Map copy = new Map(w, h, playerId);
		copy.setMe(me.copy());
		Cell[][] cellsCopy = new Cell[w][h];
		for (Cell[] cells : map) {
			for (Cell cell : cells) {
				cellsCopy[cell.x][cell.y] = cell.copy();
			}
		}
		copy.setCells(cellsCopy);
		return copy;
	}

	public Move move(int depth) {
		long startTime = System.nanoTime();
		ExecutorService es;
		
		Move root = new Move(-1,-1,false);
		root.setResult(this);
		List<Move> moves = new ArrayList<Move>();
		moves.add(root);
		
		List<Move> nextMoves;
		int level = 0;
		long elapsedTime = 0;
		while (level<depth && elapsedTime<90) {
			nextMoves = new ArrayList<Move>();
			es = Executors.newWorkStealingPool();
			elapsedTime = (System.nanoTime()-startTime)/1000000;
			long maxTime = 90-elapsedTime;
			for (Move move : moves) {
				es.submit(new BranchingTask(move, maxTime));
			}
			
			es.shutdown();
			try {
				if (maxTime<=0) maxTime=1;
				boolean finished = es.awaitTermination(maxTime, TimeUnit.MILLISECONDS);
				if (!finished) {
					System.out.println("TIMEOUT!!");
					es.shutdownNow();
					break;
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
				break;
			}
			
			//once branching finishes, get all childs for next level
			for (Move move : moves) {
				nextMoves.addAll(move.childs());
			}
			
			moves = nextMoves;
			level++;
		}
		
		return root.bestPath().firstChild();
	}
	
	public int boxesInRange(Cell bomb) {
		List<Cell> things = explosiveThingsInRangeOfBomb(bomb);
		int boxes = 0;
		for (Cell cell : things) {
			if (cell.isBox()) boxes++;
		}
		return boxes;
	}

	public List<Move> possibleMoves() {
		List<Move> moves = new ArrayList<Move>();
		int x = me.x;
		int y = me.y;
		
		moves.add(new Move(x, y, false));//stay
		if (me.hasBombs() && !map[me.x][me.y].isBomb()) moves.add(new Move(x, y, true));//stay and place bomb
		
		if (me.y-1 >= 0) {
			x = me.x;
			y = me.y-1;
			if (!map[x][y].isAnObstacle()) {
				moves.add(new Move(x, y, false));//top
				if (me.hasBombs() && !map[me.x][me.y].isBomb()) moves.add(new Move(x, y, true));
			}
		}
		
		if (me.y+1 < h) {
			x = me.x;
			y = me.y+1;
			if (!map[x][y].isAnObstacle()) {
				moves.add(new Move(x, y, false));//bottom
				if (me.hasBombs() && !map[me.x][me.y].isBomb()) moves.add(new Move(x, y, true));
			}
		}
		
		if (me.x-1 >= 0) {
			x = me.x-1;
			y = me.y;
			if (!map[x][y].isAnObstacle()) {
				moves.add(new Move(x, y, false));//left
				if (me.hasBombs() && !map[me.x][me.y].isBomb()) moves.add(new Move(x, y, true));
			}
		}
		
		if (me.x+1 < w) {
			x = me.x+1;
			y = me.y;
			if (!map[x][y].isAnObstacle()) {
				moves.add(new Move(x, y, false));//right
				if (me.hasBombs() && !map[me.x][me.y].isBomb()) moves.add(new Move(x, y, true));
			}
		}
		
		return moves;
	}

	public Map simulate(Move move) {
		Map copy = copy();
		Robot player = copy.me();
		
		//first, things explode
		copy.fixBombTurns();
		copy.explodeBombs();
		
		//then we place a bomb in our current position
		if (move.placeBomb) {
			Cell bomb = new Cell(CellType.BOMB);
			bomb.setTurnsLeft(7);
			bomb.setBombRange(player.bombRange());
			bomb.setCoordinates(player.x, player.y);
			bomb.setOwner(player.id);
			player.decreaseBombs();
			copy.set(bomb);
		}
		
		//finally we move player to move.x and move.y
		copy.get(player.x, player.y).removePlayer(player);
		player.x = move.x;
		player.y = move.y;
		copy.get(player.x, player.y).addPlayer(player);
		
		//if there is an item there, we take it
		if (copy.get(player.x, player.y).isItem()) {
			Cell empty = new Cell(CellType.EMPTY);
			empty.setCoordinates(player.x, player.y);
			empty.addPlayer(player);
			copy.set(empty);
		}
		
		return copy;
	}

	private void explodeBombs() {
		for (Cell[] cells : map) {
			for (Cell cell : cells) {
				if (cell.isBomb()) {
					if (cell.turnsLeft() > 0) {
						cell.setTurnsLeft(cell.turnsLeft() - 1);
					} else {
						List<Cell> things = explosiveThingsInRangeOfBomb(cell);
						for (Cell thing : things) {
							if (thing.containsPlayer(playerId)) me().die();
							map[thing.x][thing.y] = thing.blowUp(); //thing explodes, cell is now empty or an item
						}
						if (cell.containsPlayer(playerId)) me().die();
						map[cell.x][cell.y] = cell.blowUp(); //bomb explodes, now this cell is empty
						//TODO increase bombs for player if bomb is mine
					}
				}
			}
		}
	}

	private List<Cell> explosiveThingsInRangeOfBomb(Cell cell) {
		List<Cell> objects = new ArrayList<Cell>();

		int bombRange = cell.bombRange();
		int x = cell.x;
		int y = cell.y;
		
	    for (int i=x+1; i<w; i++) {
	    	if (bombRange > Math.abs(x - i)) {
	    		if (map[i][y].isBomb()) break;
	    		if (map[i][y].isPlayer()) objects.add(map[i][y]);
	    		if (map[i][y].isBox() || map[i][y].isWall() || map[i][y].isItem()) {
	    			objects.add(map[i][y]);
	    			break;
	    		}
	        }
	    }

	    for (int i=x-1; i>=0; i--) {
	    	if (bombRange > Math.abs(x - i)) {
	    		if (map[i][y].isBomb()) break;
	    		if (map[i][y].isPlayer()) objects.add(map[i][y]);
	    		if (map[i][y].isBox() || map[i][y].isWall() || map[i][y].isItem()) {
	    			objects.add(map[i][y]);
	    			break;
	    		}
	        }
	    }

	    for (int i=y+1; i<h; i++) {
	    	if (bombRange > Math.abs(y - i)) {
	    		if (map[x][i].isBomb()) break;
	    		if (map[x][i].isPlayer()) objects.add(map[x][i]);
	    		if (map[x][i].isBox() || map[x][i].isWall() || map[x][i].isItem()) {
	    			objects.add(map[x][i]);
	    			break;
	    		}
	        }
	    }

	    for (int i=y-1; i>=0; i--) {
	    	if (bombRange > Math.abs(y - i)) {
	    		if (map[x][i].isBomb()) break;
	    		if (map[x][i].isPlayer()) objects.add(map[x][i]);
	    		if (map[x][i].isBox() || map[x][i].isWall() || map[x][i].isItem()) {
	    			objects.add(map[x][i]);
	    			break;
	    		}
	        }
	    }

	    return objects;
	}

	public boolean playerIsDead() {
		return me.isDead();
	}
}

class Cell implements Comparable<Cell> {
	
	private CellType type;

	public int x;
	public int y;
	
	private int remainingBombs;
	private int bombRange;
	private int owner;
	private int turnsLeft;
	private ItemType itemType;
	private boolean reachable;
	private int steps;
	private List<Robot> players;

	public Cell(CellType type) {
		this.type = type;
		players = new ArrayList<Robot>();
	}

	public boolean containsPlayer(int playerId) {
		if (players.size() == 0) return false;
		for (Robot robot : players) {
			if (robot.id == playerId) return true;
		}
		return false;
	}

	public void addPlayer(Robot player) {
		this.players.add(player);
	}
	
	public void removePlayer(Robot me) {
		int i = 0;
		for (Robot robot : players) {
			if (me.id == robot.id) {
				players.remove(i);
				break;
			}
			i++;
		}
	}
	
	public List<Robot> players() {
		return players;
	}

	public boolean isBomb() { return type == CellType.BOMB; }
	public boolean isWall() { return type == CellType.WALL; }
	public boolean isBox() { return type == CellType.BOX; }
	public boolean isItem() { return type == CellType.ITEM; }
	public boolean isPlayer() { return players.size() > 0; }

	public void setCoordinates(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public boolean isAnObstacle() {
		return type == CellType.WALL || type == CellType.BOX || type == CellType.BOMB;
	}

	public boolean isEmpty() {
		return type == CellType.EMPTY;
	}

	public CellType type() {
		return type;
	}
	
	public void setRemainingBombs(int bombs) {
		this.remainingBombs = bombs;
	}

	public int remainingBombs() {
		return remainingBombs;
	}
	
	public void setBombRange(int range) {
		this.bombRange = range;
	}

	public int bombRange() {
		return bombRange;
	}

	public void setTurnsLeft(int turnsLeft) {
		this.turnsLeft = turnsLeft;
	}

	public int turnsLeft() {
		return turnsLeft;
	}
	
	public void setOwner(int owner) {
		this.owner = owner;
	}
	
	public int owner() {
		return owner;
	}

	public void setItemType(ItemType itemType) {
		this.itemType = itemType;
	}
	
	public ItemType itemType() {
		return itemType;
	}
	
	public void steps(int steps) {
		this.steps = steps;
	}
	
	public int steps() {
		return steps;
	}

	public void reachable(boolean isReachable) {
		this.reachable = isReachable;
	}

	public boolean reachable() {
		return reachable;
	}

	public Cell blowUp() {
		if (type == CellType.WALL) return this; //walls never explode
		
		Cell explodedCell = new Cell(CellType.EMPTY);
		
		if (type == CellType.BOX && itemType != null) {
			explodedCell = new Cell(CellType.ITEM);
			if (itemType == ItemType.BOMB) explodedCell.setItemType(ItemType.BOMB);
			if (itemType == ItemType.RANGE) explodedCell.setItemType(ItemType.RANGE);
		}
		
		explodedCell.setCoordinates(x, y);
		return explodedCell;
	}
	
	@Override
	public int compareTo(Cell o) {
		return this.turnsLeft() - o.turnsLeft();
	}
	
	public String toString() {
		return type.toString();
	}
	
	/**
	 * Does not copy reachability nor steps
	 */
	public Cell copy() {
		Cell cell = new Cell(type);
		cell.setCoordinates(x, y);
		cell.setBombRange(this.bombRange());
		cell.setTurnsLeft(this.turnsLeft());
		cell.setOwner(this.owner());
		cell.setItemType(this.itemType());
		cell.setRemainingBombs(this.remainingBombs());
		for (Robot player : players) {
			cell.addPlayer(player.copy());
		}
		return cell;
	}
	
}

class Move {

	private static final int PENALTY = 1; //chaining moves has a penalty (because for the same score, closer is better)
	
	public int x;
	public int y;
	public boolean placeBomb;
	private Move parent;
	private List<Move> childs;
	private int score = 0;
	private Map result;

	public Move(int x, int y, boolean placeBomb) {
		this.x = x;
		this.y = y;
		this.placeBomb = placeBomb;
		this.childs = new ArrayList<Move>();
	}
	
	public boolean shouldContinue() {
		if (result.playerIsDead()) return false; //we are dead, no need to simulate more moves...
		if (parent!=null && parent.x == x && parent.y == y) return false; //we are not going very far...
		return true;
	}

	public Map result() {
		return result;
	}

	public void setResult(Map result) {
		this.result = result;
	}

	public Collection<Move> childs() {
		return childs;
	}

	public Move firstChild() {
		return childs.get(0);
	}

	public Move bestPath() {
		Move thisMove = new Move(x, y, placeBomb);
		if (!hasChilds()) {
			thisMove.setScore(score);
			return thisMove;
		}
		
		Move selectedPath = null;
		for (Move move : childs) {
			Move bestPath = move.bestPath();
			if (selectedPath == null || selectedPath.score()<bestPath.score())
				selectedPath = bestPath;
		}
		Move union = thisMove;
		union.addChild(selectedPath);
		int childScore = selectedPath.score();
		if (childScore > 0)
			childScore -= PENALTY;
		union.setScore(score + childScore);
		return union;
	}

	public Move parent() {
		return parent;
	}

	public void setParent(Move move) {
		this.parent = move;
	}
	
	public boolean hasChilds() {
		return this.childs.size() > 0;
	}

	public void addChild(Move move) {
		this.childs.add(move);
	}
	
	public int score() {
		return score;
	}
	
	public void setScore(int score) {
		this.score = score;
	}
	
	public String output() {
		String move = "";
		if (placeBomb) move += "BOMB ";
		else move += "MOVE ";
		return move + x + " " + y;
	}

	@Override
	public String toString() {
		String move = "(" + score + ") " + output();
		if (this.hasChilds()) {
			move += " -> ";
			move += "[";
			for (Move child : childs) {
				move += child.toString() + ", ";
			}
			move = move.substring(0, move.length()-2);
			move += "]";
		}
		
		return move;
	}
	
}

class Robot {
	public int id;
	public int x;
	public int y;
	private int bombRange;
	private int bombs;
	private boolean dead;

	public Robot(int id, int x, int y) {
		this.id = id;
		this.x = x;
		this.y = y;
		this.dead = false;
	}
	
	public void die() {
		dead = true;
	}

	public boolean isDead() {
		return dead;
	}

	public void setBombRange(int bombRange) {
		this.bombRange = bombRange;
	}

	public int bombRange() {
		return bombRange;
	}

	public void setRemainingBombs(int bombs) {
		this.bombs = bombs;
	}

	public boolean hasBombs() {
		return bombs > 0;
	}
	
	public void decreaseBombs() {
		bombs--;
	}
	
	public void increaseBombs() {
		bombs++;
	}

	public int bombs() {
		return bombs;
	}
	
	public Robot copy() {
		Robot copy = new Robot(id, x, y);
		copy.setBombRange(bombRange);
		copy.setRemainingBombs(bombs);
		return copy;
	}

}

class CellFactory {
	public static Cell createFromMap(String character) {
		Cell cell = new Cell(CellType.EMPTY);
		switch (character) {
		case ".":
			cell = new Cell(CellType.EMPTY);
			break;
		case "X":
			cell = new Cell(CellType.WALL);
			break;
		case "0":
			cell = new Cell(CellType.BOX);
			break;
		case "1":
			cell = new Cell(CellType.BOX);
			cell.setItemType(ItemType.RANGE);
			break;
		case "2":
			cell = new Cell(CellType.BOX);
			cell.setItemType(ItemType.BOMB);
			break;
		}
		return cell;
	}
	
	public static Cell createFromEntity(String character, int owner, int param1, int param2) {
		Cell cell = new Cell(CellType.EMPTY);
		switch (character) {
		case "1":
			cell = new Cell(CellType.BOMB);
			cell.setOwner(owner);
			cell.setTurnsLeft(param1 - 1); //current turn should not count
			cell.setBombRange(param2);
			break;
		case "2":
			cell = new Cell(CellType.ITEM);
			if (param1 == 1) cell.setItemType(ItemType.RANGE);
			if (param1 == 2) cell.setItemType(ItemType.BOMB);
			break;
		}
		return cell;
	}
}

class Position {
	public int y;
	public int x;

	public Position(int x, int y) {
		this.x = x;
		this.y = y;
	}
}

enum CellType {EMPTY, BOX, WALL, BOMB, ITEM}
enum ItemType {BOMB, RANGE}

class BranchingTask implements Runnable {

	private Move move;
	private long startTime;
	private long maxTimeMs;

	public BranchingTask(Move move, long maxTimeMs) {
		this.move = move;
		this.startTime = System.nanoTime();
		this.maxTimeMs = maxTimeMs;
	}
	
	@Override
	public void run() {
		if (noMoreTime()) {
			Thread.currentThread().interrupt();
			return;
		}
		
		if (move.shouldContinue()) {
			branchFromNode(move.result(), move);
		} else {
			move.setScore(move.score() - 10); //penalizamos el quedarse quieto... psa
		}
	}
	
	private void branchFromNode(Map map, Move node) {
		Robot me = map.me();
		List<Move> moves = map.possibleMoves();
		for (Move move : moves) {
			if (noMoreTime()) {
				Thread.currentThread().interrupt();
				break;
			}
			Map result = map.simulate(move);
			
			//evaluate result
			//System.err.println("Evaluate move " + move.toString() + " at depth="+level);
			int score = 0;
			if (result.playerIsDead()) score = score - 100;
			if (move.placeBomb) {
				Cell hypotheticalBomb = new Cell(CellType.BOMB);
				hypotheticalBomb.setCoordinates(me.x, me.y);
				hypotheticalBomb.setBombRange(me.bombRange());
				hypotheticalBomb.setTurnsLeft(7);
				score = score + 5*map.boxesInRange(hypotheticalBomb);
			}
			//if this cell is safe for n turns, score + n
			//if this cell contains an item, score + 3
			move.setScore(score);
			move.setResult(result);
			
			move.setParent(node);
			node.addChild(move);
		}
	}
	
	private boolean noMoreTime() {
		if (maxTimeMs<=0) return true;
		long elapsedTime = (System.nanoTime()-startTime)/1000000;
		return elapsedTime>=maxTimeMs;
	}
	
}