import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.charset.Charset;

import org.junit.Before;
import org.junit.Test;

public class PerformanceTest {
	
	private String inputs;

	@Before
	public void setUp() throws IOException {
		inputs = TestUtils.readFile("inputs/complexMap.txt", Charset.defaultCharset());
	}

	@Test
	public void moveTest() {
		Map map = new Map(13, 11, 0);
		
		map.parse(inputs);
		
		Move move = map.move(8);
		
		System.out.println(move.toString());
	}

}
