import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.nio.charset.Charset;

import org.junit.Test;

public class MapCopyTest {

	@Test
	public void copyPlayer() throws IOException {
		Map original = new Map(13, 11, 0);
		
		String inputs = TestUtils.readFile("inputs/mapWithPlayers.txt", Charset.defaultCharset());
		original.parse(inputs);
		
		Map copy = original.copy();
		
		assertThat(copy.me().id, equalTo(original.me().id));
		assertThat(copy.me().x, equalTo(original.me().x));
		assertThat(copy.me().y, equalTo(original.me().y));
	}
	
	@Test
	public void copyCells() throws IOException {
		Map original = new Map(13, 11, 0);
		
		String inputs = TestUtils.readFile("inputs/mapWithPlayers.txt", Charset.defaultCharset());
		original.parse(inputs);
		
		Map copy = original.copy();
		
		assertThat(copy.get(0, 0).type(), equalTo(original.get(0, 0).type()));
		assertThat(copy.get(3, 0).type(), equalTo(original.get(3, 0).type()));
	}

	@Test
	public void copyPlayers() throws IOException {
		Map original = new Map(13, 11, 0);
		
		String inputs = TestUtils.readFile("inputs/mapWithPlayers.txt", Charset.defaultCharset());
		original.parse(inputs);
		
		Map copy = original.copy();
		
		assertThat(copy.me().x, equalTo(0));
		assertThat(copy.me().y, equalTo(0));

		assertThat(copy.player(1).x, equalTo(12));
		assertThat(copy.player(1).y, equalTo(10));
	}
}
