import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.charset.Charset;

import org.junit.Before;
import org.junit.Test;

public class PerformanceTest {
	
	private String inputs;
	private String inputs2;

	@Before
	public void setUp() throws IOException {
		inputs = TestUtils.readFile("inputs/complexMap.txt", Charset.defaultCharset());
		inputs2 = TestUtils.readFile("inputs/veryComplexMap.txt", Charset.defaultCharset());
	}

	@Test
	public void complexMove() {
		Map map = new Map(13, 11, 0);
		
		map.parse(inputs);
		
		long before = System.nanoTime();
		Move move = map.move(8);
		double elapsed = (System.nanoTime() - before)/1000000f;
		
		System.out.println(elapsed);
		System.out.println(move.toString());
	}

	@Test
	public void veryComplexMove() {
		Map map = new Map(13, 11, 0);
		
		map.parse(inputs2); //first level has 10 moves, and branching is huge
		
		long before = System.nanoTime();
		Move move = map.move(12);
		double elapsed = (System.nanoTime() - before)/1000000f;
		
		System.out.println(elapsed);
		System.out.println(move.toString());
	}
}
