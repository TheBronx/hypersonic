import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.charset.Charset;

import org.junit.Test;

public class SimulateTest {

	@Test
	public void simulateBoxDestroyed() throws IOException {
		Map map = new Map(13, 11, 0);
		
		String inputs = TestUtils.readFile("inputs/bombsDestroyBoxes.txt", Charset.defaultCharset());
		map.parse(inputs);
		
		Move stay = new Move(map.me().x, map.me().y, false);
		Map next = map.simulate(stay);
		
		assertThat(map.get(0, 2).type(), equalTo(CellType.BOX));
		
		assertThat(map.get(0, 1).type(), equalTo(CellType.BOMB));
		assertThat(map.get(0, 1).turnsLeft(), equalTo(0));
		assertThat(map.get(0, 1).bombRange(), equalTo(3));
		
		assertThat(next.get(0, 2).type(), equalTo(CellType.ITEM));
	}

	@Test
	public void simulatePlayerKilled() throws IOException {
		Map map = new Map(13, 11, 0);
		
		String inputs = TestUtils.readFile("inputs/bombKillsPlayer.txt", Charset.defaultCharset());
		map.parse(inputs);
		
		Move stay = new Move(map.me().x, map.me().y, false);
		Map next = map.simulate(stay);
		
		assertThat(map.player(0).x, equalTo(1));
		assertThat(map.player(0).y, equalTo(0));
		
		assertThat(map.get(0, 0).type(), equalTo(CellType.BOMB));
		assertThat(map.get(0, 0).turnsLeft(), equalTo(0));
		assertThat(map.get(0, 0).bombRange(), equalTo(3));
		
		assertThat(next.get(1, 0).type(), equalTo(CellType.EMPTY));
		assertTrue(next.playerIsDead());
	}
	
	@Test
	public void simulatePlayerKilledAtMaxRange() throws IOException {
		Map map = new Map(13, 11, 0);
		
		String inputs = TestUtils.readFile("inputs/playerAtMaxBombRange.txt", Charset.defaultCharset());
		map.parse(inputs);
		
		Move stay = new Move(map.me().x, map.me().y, false);
		Map next = map.simulate(stay);
		
		assertThat(map.player(0).x, equalTo(2));
		assertThat(map.player(0).y, equalTo(0));
		
		assertThat(map.get(0, 0).type(), equalTo(CellType.BOMB));
		assertThat(map.get(0, 0).turnsLeft(), equalTo(0));
		assertThat(map.get(0, 0).bombRange(), equalTo(3));
		
		assertThat(next.get(2, 0).type(), equalTo(CellType.EMPTY));
		assertTrue(next.playerIsDead());
	}
	
	@Test
	public void simulatePlayerOutOfBombs() throws IOException {
		Map map = new Map(13, 11, 0);
		
		String inputs = TestUtils.readFile("inputs/corner.txt", Charset.defaultCharset());
		map.parse(inputs);
		
		Move placeBomb = new Move(map.me().x, map.me().y, true);
		Map next = map.simulate(placeBomb);
		
		assertTrue(next.get(0, 0).isBomb());
		assertThat(next.me().bombs(), equalTo(0));
	}
}
