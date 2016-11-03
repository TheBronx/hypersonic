import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.charset.Charset;

import org.junit.Test;

public class MapTest {

	@Test
	public void parseMapCells() throws IOException {
		Map map = new Map(13, 11, 0);
		
		String inputs = TestUtils.readFile("inputs/mapWithPlayers.txt", Charset.defaultCharset());
		map.parse(inputs);
		
		assertThat(map.get(0, 1).type(), equalTo(CellType.EMPTY));
		assertThat(map.get(4, 0).type(), equalTo(CellType.BOX));
		assertThat(map.get(1, 1).type(), equalTo(CellType.WALL));
	}
	
	@Test
	public void parsePlayers() throws IOException {
		Map map = new Map(13, 11, 0);
		
		String inputs = TestUtils.readFile("inputs/mapWithPlayers.txt", Charset.defaultCharset());
		map.parse(inputs);
		
		assertTrue(map.player(0).x == 0);
		assertTrue(map.player(0).y == 0);
		
		assertTrue(map.player(1).x == 12);
		assertTrue(map.player(1).y == 10);
		
		assertThat(map.player(0).bombs(), equalTo(1));
		assertThat(map.player(1).bombs(), equalTo(1));
		
		assertThat(map.player(0).bombRange(), equalTo(3));
		assertThat(map.player(1).bombRange(), equalTo(3));
	}
	
	@Test
	public void parseItems() throws IOException {
		Map map = new Map(13, 11, 0);
		
		String inputs = TestUtils.readFile("inputs/mapWithItems.txt", Charset.defaultCharset());
		map.parse(inputs);
		
		assertThat(map.get(12, 7).type(), equalTo(CellType.ITEM));
		assertThat(map.get(11, 0).type(), equalTo(CellType.ITEM));
		
		assertThat(map.get(12, 7).itemType(), equalTo(ItemType.RANGE));
		assertThat(map.get(11, 0).itemType(), equalTo(ItemType.BOMB));
	}
	
	@Test
	public void parseBombs() throws IOException {
		Map map = new Map(13, 11, 0);
		
		String inputs = TestUtils.readFile("inputs/mapWithBombs.txt", Charset.defaultCharset());
		map.parse(inputs);
		
		assertThat(map.get(0, 0).type(), equalTo(CellType.BOMB));
		assertThat(map.get(11, 10).type(), equalTo(CellType.BOMB));
		
		assertThat(map.get(0, 0).turnsLeft(), equalTo(0));
		assertThat(map.get(11, 10).turnsLeft(), equalTo(2));
		
		assertThat(map.get(0, 0).bombRange(), equalTo(3));
		assertThat(map.get(11, 10).bombRange(), equalTo(4));
	}

	@Test
	public void parseMe() throws IOException {
		Map map = new Map(13, 11, 1);
		
		String inputs = TestUtils.readFile("inputs/mapWithPlayers.txt", Charset.defaultCharset());
		map.parse(inputs);
		
		assertThat(map.me().id, equalTo(1));
		assertThat(map.me().x, equalTo(12));
		assertThat(map.me().y, equalTo(10));
	}
}