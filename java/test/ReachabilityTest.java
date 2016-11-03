

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.Charset;

import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class ReachabilityTest {

	@Test
	public void wallsAndBoxesBlockAllMap() throws IOException {
		Map map = new Map(13, 11, 0);
		
		String inputs = TestUtils.readFile("inputs/blockedMap.txt", Charset.defaultCharset());
		map.parse(inputs);
		
		assertTrue(map.get(0, 0).reachable());
		assertFalse(map.get(0, 1).reachable());
		assertFalse(map.get(1, 0).reachable());
	}
	
	@Test
	public void pathAroundMap() throws IOException {
		Map map = new Map(13, 11, 0);
		
		String inputs = TestUtils.readFile("inputs/mapWithPathAllAround.txt", Charset.defaultCharset());
		map.parse(inputs);
		
		assertTrue(map.get(0, 0).reachable());
		assertTrue(map.get(12, 10).reachable());
		assertTrue(map.get(0, 10).reachable());
		assertTrue(map.get(12, 0).reachable());
		
		assertFalse(map.get(4, 4).reachable());
		assertFalse(map.get(1, 1).reachable());
	}
	
	@Test
	public void pathAroundMapSteps() throws IOException {
		Map map = new Map(13, 11, 0);
		
		String inputs = TestUtils.readFile("inputs/mapWithPathAllAround.txt", Charset.defaultCharset());
		map.parse(inputs);
		
		assertTrue(map.get(12, 10).reachable());
		assertThat(map.get(12, 10).steps(), equalTo(22));
	}

}
