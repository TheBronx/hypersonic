import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

class Player {
	private static final int DEPTH = 7;

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
        	long before = System.nanoTime();
            int entities = in.nextInt();
            for (int i = 0; i < entities; i++) {
            	inputs.append(in.nextInt()+" "+in.nextInt()+" "+in.nextInt()+" "+in.nextInt()+" "+in.nextInt()+" "+in.nextInt());
            	if (i<(entities-1)) inputs.append("\n");
            }
            //System.err.println(inputs.toString());
            map.parse(inputs.toString());

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
	private int playerId;
	private List<Robot> players;

	public Map(int w, int h, int playerId) {
        this.w = w;
        this.h = h;
        this.playerId = playerId;
        this.map = new Cell[w][h];
        this.players = new ArrayList<Robot>();
    }
	
	public void parse(String inputs) {
		this.players.clear();
		
		String[] lines = inputs.split("\n");
		for (int i=0; i<h; i++) {
			String[] chars = lines[i].split("");
			for (int j=0; j<chars.length; j++) {
				map[j][i] = CellFactory.createFromMap(chars[j]);
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
			
			if (chars[0].equals("0")) {
				Robot player = new Robot(owner, x, y);
				player.setRemainingBombs(param1);
				player.setBombRange(param2);
				
				addPlayer(player);
			}
		}
		
		fixBombTurns();
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
	
	public boolean playerOnCoords(int x, int y) {
		if (players.size() == 0) return false;
		for (Robot robot : players) {
			if (robot.x == x && robot.y == y) return true;
		}
		return false;
	}
	
	public Robot player(int id) {
		for (Robot robot : players) {
			if (robot.id == id) return robot;
		}
		return null;
	}
	
	public void fixBombTurns() {
		//TODO performance: sacar las bombas una vez para cada mapa
		List<BombPosition> bombs = new ArrayList<BombPosition>();
	    for (int x=0; x<w; x++) {
	        for (int y=0; y<h; y++) {
	            if (map[x][y].isBomb()) {
	                bombs.add(new BombPosition(map[x][y], x, y));
	            }
	        }
	    }

	    Collections.sort(bombs);

	    for (int i=0; i<bombs.size(); i++) {
	        int x = bombs.get(i).position.x;
			int y = bombs.get(i).position.y;
			Cell bomb = map[x][y];
	        Integer min = minTurnsLeftOfBombsInRange(x, y);
	        if (min != null && min < bomb.turnsLeft()) {
	        	map[x][y] = CellFactory.createBomb(bomb.owner(), bomb.bombRange(), min);
	            //now adjust affected bombs too
	            List<Position> bombsAffected = bombsInRange(x, y);
	            for (int j=0; j<bombsAffected.size(); j++) {
	                int x2 = bombsAffected.get(j).x;
					int y2 = bombsAffected.get(j).y;
					Cell affectedBombCell = map[x2][y2];
	                if (affectedBombCell.turnsLeft()>min) {
	                	map[x2][y2] = CellFactory.createBomb(affectedBombCell.owner(), affectedBombCell.bombRange(), min);
	                }
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

	private void set(Cell cell, int x, int y) {
		map[x][y] = cell;
	}
	
	public Cell get(int x, int y) {
		return map[x][y];
	}

	public Robot me() {
		for (Robot robot : players) {
			if (robot.id == playerId) return robot;
		}
		return null;
	}

	private void setCells(Cell[][] cells) {
		this.map = cells;
	}
	
	public Map copy() {
		Map copy = new Map(w, h, playerId);
		Cell[][] cellsCopy = new Cell[w][h];
		for (int x=0; x<w; x++) 
			for (int y=0; y<h; y++)
				cellsCopy[x][y] = map[x][y];
		copy.setCells(cellsCopy);
		for (Robot player : players) {
			copy.addPlayer(player.copy());
		}
		return copy;
	}

	public Move move(int depth) {
		long startTime = System.nanoTime();
		ExecutorService es;
		
		Move root = new Move(me().x, me().y, false);
		root.setLevel(0);
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
			
			if (maxTime<=5) {
				//not enough time to start and wait for all the threads, stop now!
				es.shutdownNow();
				break;
			}
			es.shutdown();
			try {
				boolean finished = es.awaitTermination(maxTime, TimeUnit.MILLISECONDS);
				if (!finished) {
					System.err.println("TIMEOUT!!");
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
			if (nextMoves.size()>2000) break;//this is fucking crazy. stop!
			moves = nextMoves;
			level++;
		}
		
		return root.bestPath().firstChild();
	}
	
	public int boxesInRange(Cell bomb, int x, int y) {
		List<Cell> things = explosiveThingsInRangeOfBomb(bomb, x, y);
		int boxes = 0;
		for (Cell cell : things) {
			if (cell.isBox()) boxes++;
		}
		return boxes;
	}

	public List<Move> possibleMoves() {
		Robot me = me();
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
			Cell bomb = CellFactory.createBomb(player.id, player.bombRange(), 7);
			player.decreaseBombs();
			copy.set(bomb, player.x, player.y);
		}
		
		//finally we move player to move.x and move.y
		player.x = move.x;
		player.y = move.y;
		
		//if there is an item there, we take it
		if (copy.get(player.x, player.y).isItem()) {
			Cell empty = CellFactory.createEmpty();
			copy.set(empty, player.x, player.y);
		}
		
		return copy;
	}

	private void explodeBombs() {
		for (int x=0; x<w; x++) 
			for (int y=0; y<h; y++) {
				Cell cell = map[x][y];
				if (cell.isBomb()) {
					if (cell.turnsLeft() > 0) {
						map[x][y] = cell.decreaseTurnsLeft();
					} else {
						explodeThingsInRangeOf(cell, x, y);
						
						if (me().x == x && me().y == y) me().die();
						map[x][y] = cell.blowUp(); //bomb explodes, now this cell is empty
						//TODO increase bombs for player if bomb is mine
					}
				}
			}
	}

	private void explodeThingsInRangeOf(Cell cell, int x, int y) {
		int bombRange = cell.bombRange();
		
		Robot me = me();
	    for (int i=x+1; i<w; i++) {
	    	if (bombRange > Math.abs(x - i)) {
	    		if (map[i][y].isBomb()) break;
	    		if (playerOnCoords(i, y)) me.die();
	    		if (map[i][y].isBox() || map[i][y].isWall() || map[i][y].isItem()) {
	    			map[i][y] = map[i][y].blowUp();
	    			break;
	    		}
	        }
	    }

	    for (int i=x-1; i>=0; i--) {
	    	if (bombRange > Math.abs(x - i)) {
	    		if (map[i][y].isBomb()) break;
	    		if (playerOnCoords(i, y)) me.die();
	    		if (map[i][y].isBox() || map[i][y].isWall() || map[i][y].isItem()) {
	    			map[i][y] = map[i][y].blowUp();
	    			break;
	    		}
	        }
	    }

	    for (int i=y+1; i<h; i++) {
	    	if (bombRange > Math.abs(y - i)) {
	    		if (map[x][i].isBomb()) break;
	    		if (playerOnCoords(x, i)) me.die();
	    		if (map[x][i].isBox() || map[x][i].isWall() || map[x][i].isItem()) {
	    			map[x][i] = map[x][i].blowUp();
	    			break;
	    		}
	        }
	    }

	    for (int i=y-1; i>=0; i--) {
	    	if (bombRange > Math.abs(y - i)) {
	    		if (map[x][i].isBomb()) break;
	    		if (playerOnCoords(x, i)) me.die();
	    		if (map[x][i].isBox() || map[x][i].isWall() || map[x][i].isItem()) {
	    			map[x][i] = map[x][i].blowUp();
	    			break;
	    		}
	        }
	    }
	}

	private List<Cell> explosiveThingsInRangeOfBomb(Cell cell, int x, int y) {
		List<Cell> objects = new ArrayList<Cell>();

		int bombRange = cell.bombRange();
		
	    for (int i=x+1; i<w; i++) {
	    	if (bombRange > Math.abs(x - i)) {
	    		if (map[i][y].isBomb()) break;
	    		if (playerOnCoords(i, y)) objects.add(map[i][y]);
	    		if (map[i][y].isBox() || map[i][y].isWall() || map[i][y].isItem()) {
	    			objects.add(map[i][y]);
	    			break;
	    		}
	        }
	    }

	    for (int i=x-1; i>=0; i--) {
	    	if (bombRange > Math.abs(x - i)) {
	    		if (map[i][y].isBomb()) break;
	    		if (playerOnCoords(i, y)) objects.add(map[i][y]);
	    		if (map[i][y].isBox() || map[i][y].isWall() || map[i][y].isItem()) {
	    			objects.add(map[i][y]);
	    			break;
	    		}
	        }
	    }

	    for (int i=y+1; i<h; i++) {
	    	if (bombRange > Math.abs(y - i)) {
	    		if (map[x][i].isBomb()) break;
	    		if (playerOnCoords(x, i)) objects.add(map[x][i]);
	    		if (map[x][i].isBox() || map[x][i].isWall() || map[x][i].isItem()) {
	    			objects.add(map[x][i]);
	    			break;
	    		}
	        }
	    }

	    for (int i=y-1; i>=0; i--) {
	    	if (bombRange > Math.abs(y - i)) {
	    		if (map[x][i].isBomb()) break;
	    		if (playerOnCoords(x, i)) objects.add(map[x][i]);
	    		if (map[x][i].isBox() || map[x][i].isWall() || map[x][i].isItem()) {
	    			objects.add(map[x][i]);
	    			break;
	    		}
	        }
	    }

	    return objects;
	}

	public boolean playerIsDead() {
		return me().isDead();
	}

}

class Cell {
	
	private final CellType type;
	private final int bombRange;
	private final int owner;
	private final int turnsLeft;
	private final ItemType itemType;

	public Cell(CellType type, int owner, int bombRange, int turnsLeft, ItemType itemType) {
		this.type = type;
		this.owner = owner;
		this.bombRange = bombRange;
		this.turnsLeft = turnsLeft;
		this.itemType = itemType;
	}

	public Cell decreaseTurnsLeft() {
		return CellFactory.createBomb(owner, bombRange, turnsLeft - 1);
	}

	public boolean isBomb() { return type == CellType.BOMB; }
	public boolean isWall() { return type == CellType.WALL; }
	public boolean isBox() { return type == CellType.BOX; }
	public boolean isItem() { return type == CellType.ITEM; }
	public boolean isEmpty() { return type == CellType.EMPTY; }

	public boolean isAnObstacle() {
		return type == CellType.WALL || type == CellType.BOX || type == CellType.BOMB;
	}

	public CellType type() {
		return type;
	}
	
	public int bombRange() {
		return bombRange;
	}

	public int turnsLeft() {
		return turnsLeft;
	}
	
	public int owner() {
		return owner;
	}

	public ItemType itemType() {
		return itemType;
	}
	
	public Cell blowUp() {
		if (type == CellType.WALL) return this; //walls never explode
		
		Cell explodedCell = CellFactory.createEmpty();
		
		if (type == CellType.BOX && itemType != null) {
			explodedCell = CellFactory.createItem(itemType);
		}
		
		return explodedCell;
	}
	
	public String toString() {
		return type.toString();
	}
	
}

class Move {

	private static final int PENALTY = 2; //chaining moves has a penalty (because for the same score, closer is better)
	private static final int DEPTH_BONUS = 1; //bonus for long moves (long=safe)
	
	public int x;
	public int y;
	public boolean placeBomb;
	private Move parent;
	private List<Move> childs;
	private int score = 0;
	private int level = 0;
	private Map result;

	public Move(int x, int y, boolean placeBomb) {
		this.x = x;
		this.y = y;
		this.placeBomb = placeBomb;
		this.childs = new ArrayList<Move>();
	}
	
	public boolean shouldContinue() {
		if (result.playerIsDead()) return false; //we are dead, no need to simulate more moves...
		//if we havent moved since the last turn, stop branching this shit. Unless we have placed a bomb. We care about future when bombs are placed :D
		if (!placeBomb && parent!=null && parent.x == x && parent.y == y) return false; //we are not going very far...
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
			thisMove.setScore(score + levelBonus());
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

	private int levelBonus() {
		if (score == 0) return 0;
		return (level/2)*DEPTH_BONUS;
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

	public int level() {
		return level;
	}
	
	public void setLevel(int level) {
		this.level = level;
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
	private static final Cell EMPTY = new Cell(CellType.EMPTY, 0, 0, 0, null);
	private static final Cell WALL = new Cell(CellType.WALL, 0, 0, 0, null);
	private static final Cell EMPTY_BOX = new Cell(CellType.BOX, 0, 0, 0, null);
	private static final Cell BOX_BOMB = new Cell(CellType.BOX, 0, 0, 0, ItemType.BOMB);
	private static final Cell BOX_RANGE = new Cell(CellType.BOX, 0, 0, 0, ItemType.RANGE);
	private static final Cell ITEM_BOMB = new Cell(CellType.ITEM, 0, 0, 0, ItemType.BOMB);
	private static final Cell ITEM_RANGE = new Cell(CellType.ITEM, 0, 0, 0, ItemType.RANGE);
	
	private static java.util.Map<String, Cell> bombPool = new HashMap<String, Cell>();

	public static Cell createFromMap(String character) {
		Cell cell;
		switch (character) {
		case ".":
			cell = createEmpty();
			break;
		case "X":
			cell = createWall();
			break;
		case "0":
			cell = createBox(null);
			break;
		case "1":
			cell = createBox(ItemType.RANGE);
			break;
		case "2":
			cell = createBox(ItemType.BOMB);
			break;
		default:
			cell = createEmpty();
			break;
		}
		return cell;
	}
	
	public static Cell createFromEntity(String character, int owner, int param1, int param2) {
		Cell cell;
		switch (character) {
		case "1":
			cell = createBomb(owner, param2, param1 - 1);
			break;
		case "2":
			ItemType itemType = ItemType.RANGE;
			if (param1 == 2) itemType = ItemType.BOMB;
			cell = createItem(itemType);
			break;
		default:
			cell = createEmpty();
			break;
		}
		return cell;
	}
	
	private static Cell createWall() {
		return WALL;
	}
	
	public static Cell createEmpty() {
		return EMPTY;
	}
	
	public static Cell createBox(ItemType itemType) {
		if (itemType == null) return EMPTY_BOX;
		if (itemType == ItemType.BOMB) return BOX_BOMB;
		return BOX_RANGE;
	}
	
	public static Cell createItem(ItemType itemType) {
		if (itemType == ItemType.BOMB) return ITEM_BOMB;
		return ITEM_RANGE;
	}
	
	public static Cell createBomb(int owner, int bombRange, int turnsLeft) {
		String key = bombKey(owner, bombRange, turnsLeft);
		Cell bomb = bombPool.get(key);
		if (bomb == null) {
			bomb = new Cell(CellType.BOMB, owner, bombRange, turnsLeft, null);
			bombPool.put(key, bomb);
		}
		return bomb;
	}
	
	public static String bombKey(int owner, int range, int turns) {
		return owner + " " + range + " " + turns;
	}
}

class Position {
	public final int y;
	public final int x;

	public Position(int x, int y) {
		this.x = x;
		this.y = y;
	}
}

class BombPosition implements Comparable<BombPosition> {
	public final Position position;
	public final Cell bomb;
	
	public BombPosition(Cell cell, int x, int y) {
		this.bomb = cell;
		this.position = new Position(x, y);
	}

	@Override
	public int compareTo(BombPosition o) {
		return this.bomb.turnsLeft() - o.bomb.turnsLeft();
	}
}

enum CellType {EMPTY, BOX, WALL, BOMB, ITEM}
enum ItemType {BOMB, RANGE}

class BranchingTask implements Runnable {

	private Move move;
	private final long startTime;
	private final long maxTimeMs;

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
			move.setScore(move.score() - 5); //penalizamos el quedarse quieto... psa
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
				Cell hypotheticalBomb = CellFactory.createBomb(me.id, me.bombRange(), 7);
				score = score + 10*map.boxesInRange(hypotheticalBomb, me.x, me.y);
			}
			//if this cell is safe for n turns, score + n
			//if this cell contains an item, score + 3
			move.setLevel(node.level() + 1);
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