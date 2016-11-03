import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;

import org.junit.Test;

public class PlayerCopyTest {

	@Test
	public void copyPlayer() {
		Robot player = new Robot(0, 0, 1);
		player.setBombRange(5);
		player.setRemainingBombs(4);
		
		Robot copy = player.copy();
		
		assertThat(copy.id, equalTo(player.id));
		assertThat(copy.x, equalTo(player.x));
		assertThat(copy.y, equalTo(player.y));
		assertThat(copy.bombRange(), equalTo(player.bombRange()));
		assertThat(copy.bombs(), equalTo(player.bombs()));
	}

}
