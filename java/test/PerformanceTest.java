import java.io.IOException;
import java.nio.charset.Charset;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class PerformanceTest {
	
	private String complexMapInputs;
	private String veryComplexMapInputs;
	private String openMapInputs;

	@Before
	public void setUp() throws IOException {
		complexMapInputs = TestUtils.readFile("inputs/complexMap.txt", Charset.defaultCharset());
		veryComplexMapInputs = TestUtils.readFile("inputs/veryComplexMap.txt", Charset.defaultCharset());
		openMapInputs = TestUtils.readFile("inputs/openMap.txt", Charset.defaultCharset());
	}

	@Test
	public void complexMove() {
		Map map = new Map(13, 11, 0);
		
		map.parse(complexMapInputs);
		
		long before = System.nanoTime();
		Move move = map.move(8);
		double elapsed = (System.nanoTime() - before)/1000000f;
		
		System.out.println(elapsed);
		System.out.println(move.toString());
	}

	@Test
	public void veryComplexMove() {
		Map map = new Map(13, 11, 0);
		
		map.parse(veryComplexMapInputs); //first level has 10 moves, and branching is huge
		
		long before = System.nanoTime();
		Move move = map.move(12);
		double elapsed = (System.nanoTime() - before)/1000000f;
		
		System.out.println(elapsed);
		System.out.println(move.toString());
	}
	
	@Test
	public void openMove() {
		Map map = new Map(13, 11, 0);
		
		map.parse(openMapInputs);
		
		long before = System.nanoTime();
		Move move = map.move(12);
		double elapsed = (System.nanoTime() - before)/1000000f;
		
		System.out.println(elapsed);
		System.out.println(move.toString());
	}
	
	@Ignore
	@Test
	public void loop() {
		Map map = new Map(13, 11, 0);
		
		map.parse(veryComplexMapInputs); //first level has 10 moves, and branching is huge
		
		for(int i=0; i<10000; i++) {
			long before = System.nanoTime();
			Move move = map.move(12);
			double elapsed = (System.nanoTime() - before)/1000000f;
			
			System.out.println(elapsed);
			System.out.println(move.toString());
		}
	}
}
