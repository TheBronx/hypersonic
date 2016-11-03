import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.charset.Charset;

import org.junit.Test;

public class UnchainedBombTest {

	@Test
	public void bombsSeparatedByAWall() throws IOException {
		Map map = new Map(13, 11, 0);
		
		String inputs = TestUtils.readFile("inputs/unchainedBombs.txt", Charset.defaultCharset());
		map.parse(inputs);
		
		assertThat(map.get(3, 0).type(), equalTo(CellType.BOMB));
		assertThat(map.get(4, 0).type(), equalTo(CellType.WALL));
		assertThat(map.get(5, 0).type(), equalTo(CellType.BOMB));
		
		assertThat(map.get(3, 0).turnsLeft(), equalTo(0));
		assertThat(map.get(5, 0).turnsLeft(), equalTo(2));
	}
	
	@Test
	public void bombsSeparatedByABox() throws IOException {
		Map map = new Map(13, 11, 0);
		
		String inputs = TestUtils.readFile("inputs/unchainedBombs.txt", Charset.defaultCharset());
		map.parse(inputs);
		
		assertThat(map.get(4, 1).type(), equalTo(CellType.BOMB));
		assertThat(map.get(5, 1).type(), equalTo(CellType.BOX));
		assertThat(map.get(6, 1).type(), equalTo(CellType.BOMB));
		
		assertThat(map.get(4, 1).turnsLeft(), equalTo(0));
		assertThat(map.get(6, 1).turnsLeft(), equalTo(2));
	}
	
	@Test
	public void bombsSeparatedByAnItem() throws IOException {
		Map map = new Map(13, 11, 0);
		
		String inputs = TestUtils.readFile("inputs/unchainedBombs.txt", Charset.defaultCharset());
		map.parse(inputs);
		
		assertThat(map.get(0, 10).type(), equalTo(CellType.BOMB));
		assertThat(map.get(1, 10).type(), equalTo(CellType.ITEM));
		assertThat(map.get(2, 10).type(), equalTo(CellType.BOMB));
		
		assertThat(map.get(0, 10).turnsLeft(), equalTo(0));
		assertThat(map.get(2, 10).turnsLeft(), equalTo(2));
	}

}
