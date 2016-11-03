import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.charset.Charset;

import org.junit.Test;

public class BombChainingTest {

	@Test
	public void twoHorizontalBombsNextToEachOther() throws IOException {
		Map map = new Map(13, 11, 0);
		
		String inputs = TestUtils.readFile("inputs/chainedBombs.txt", Charset.defaultCharset());
		map.parse(inputs);
		
		assertThat(map.get(0, 0).type(), equalTo(CellType.BOMB));
		assertThat(map.get(1, 0).type(), equalTo(CellType.BOMB));
		
		assertThat(map.get(0, 0).turnsLeft(), equalTo(0));
		assertThat(map.get(1, 0).turnsLeft(), equalTo(0));
	}
	
	@Test
	public void twoVerticalBombsNextToEachOther() throws IOException {
		Map map = new Map(13, 11, 0);
		
		String inputs = TestUtils.readFile("inputs/chainedBombs.txt", Charset.defaultCharset());
		map.parse(inputs);
		
		assertThat(map.get(5, 5).type(), equalTo(CellType.BOMB));
		assertThat(map.get(5, 6).type(), equalTo(CellType.BOMB));
		
		assertThat(map.get(5, 5).turnsLeft(), equalTo(4));
		assertThat(map.get(5, 6).turnsLeft(), equalTo(4));
	}
	
	@Test
	public void twoHorizontalBombsSeparatedButInRange() throws IOException {
		Map map = new Map(13, 11, 0);
		
		String inputs = TestUtils.readFile("inputs/chainedBombs.txt", Charset.defaultCharset());
		map.parse(inputs);
		
		assertThat(map.get(2, 2).type(), equalTo(CellType.BOMB));
		assertThat(map.get(8, 2).type(), equalTo(CellType.BOMB));
		
		assertThat(map.get(2, 2).bombRange(), equalTo(7));
		
		assertThat(map.get(2, 2).turnsLeft(), equalTo(2));
		assertThat(map.get(8, 2).turnsLeft(), equalTo(2));
	}
	
	@Test
	public void twoHorizontalBombsOutOfRange() throws IOException {
		Map map = new Map(13, 11, 0);
		
		String inputs = TestUtils.readFile("inputs/chainedBombs.txt", Charset.defaultCharset());
		map.parse(inputs);
		
		assertThat(map.get(3, 3).type(), equalTo(CellType.BOMB));
		assertThat(map.get(9, 3).type(), equalTo(CellType.BOMB));
		
		assertThat(map.get(3, 3).bombRange(), equalTo(6));
		
		assertThat(map.get(3, 3).turnsLeft(), equalTo(2));
		assertThat(map.get(9, 3).turnsLeft(), equalTo(5));
	}

}
