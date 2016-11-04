import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import org.junit.Test;

public class MoveTest {
	
	@Test
	public void possibleMovesOnTopLeftCorner() throws IOException {
		Map map = new Map(13, 11, 0);
		
		String inputs = TestUtils.readFile("inputs/corner.txt", Charset.defaultCharset());
		map.parse(inputs);
		
		List<Move> moves = map.possibleMoves();
		
		assertThat(moves.size(), equalTo(6));
		assertThat(moves.get(0).output(), startsWith("MOVE 0 0"));
		assertThat(moves.get(1).output(), startsWith("BOMB 0 0"));
		
		assertThat(moves.get(2).output(), startsWith("MOVE 0 1"));
		assertThat(moves.get(3).output(), startsWith("BOMB 0 1"));
		
		assertThat(moves.get(4).output(), startsWith("MOVE 1 0"));
		assertThat(moves.get(5).output(), startsWith("BOMB 1 0"));
	}
	
	@Test
	public void possibleMovesOnBottomRightCorner() throws IOException {
		Map map = new Map(13, 11, 0);
		
		String inputs = TestUtils.readFile("inputs/bottomRightCorner.txt", Charset.defaultCharset());
		map.parse(inputs);
		
		List<Move> moves = map.possibleMoves();
		
		assertThat(moves.size(), equalTo(6));
		assertThat(moves.get(0).output(), startsWith("MOVE 12 10"));
		assertThat(moves.get(1).output(), startsWith("BOMB 12 10"));
		
		assertThat(moves.get(2).output(), startsWith("MOVE 12 9"));
		assertThat(moves.get(3).output(), startsWith("BOMB 12 9"));
		
		assertThat(moves.get(4).output(), startsWith("MOVE 11 10"));
		assertThat(moves.get(5).output(), startsWith("BOMB 11 10"));
	}
	
	@Test
	public void moveAwayFromBomb() throws IOException {
		Map map = new Map(13, 11, 0);
		
		String inputs = TestUtils.readFile("inputs/playerOnTopOfBomb.txt", Charset.defaultCharset());
		map.parse(inputs);
		
		Move move = map.move(4);
		
		System.out.println(move.toString());
		assertThat(move.output(), startsWith("MOVE 0 1"));
		assertThat(move.firstChild().output(), startsWith("MOVE 1 1"));
	}

	@Test
	public void dontSuicideOnACorner() throws IOException {
		Map map = new Map(13, 11, 0);
		
		String inputs = TestUtils.readFile("inputs/corner.txt", Charset.defaultCharset());
		map.parse(inputs);
		
		Move move = map.move(8);
		
		System.out.println(move.toString());
		assertThat(move.output(), startsWith("MOVE 0 1"));
	}
	
	@Test
	public void placeBomb() throws IOException {
		Map map = new Map(13, 11, 0);
		
		String inputs = TestUtils.readFile("inputs/placeBombHere.txt", Charset.defaultCharset());
		map.parse(inputs);
		
		Move move = map.move(8);
		
		System.out.println(move.toString());
		assertThat(move.output(), startsWith("BOMB"));
	}
	
	@Test
	public void keepMoving() throws IOException {
		Map map = new Map(13, 11, 0);
		
		String inputs = TestUtils.readFile("inputs/realTestCase1.txt", Charset.defaultCharset());
		map.parse(inputs);
		
		Move move = map.move(8);
		
		System.out.println(move.toString());
		assertThat(move.output(), not(startsWith("MOVE 0 1")));
	}

}
