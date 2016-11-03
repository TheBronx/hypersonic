import static org.junit.Assert.*;

import org.junit.Test;
import static org.hamcrest.CoreMatchers.*;

public class CellCopyTest {

	@Test
	public void copyCellTypeEmpty() {
		Cell original = new Cell(CellType.EMPTY);
		
		Cell copy = original.copy();
		
		assertThat(copy.type(), equalTo(CellType.EMPTY));
	}
	
	@Test
	public void copyCellTypeBomb() {
		Cell original = new Cell(CellType.BOMB);
		
		Cell copy = original.copy();
		
		assertThat(copy.type(), equalTo(CellType.BOMB));
	}
	
	@Test
	public void copyCellCoordinates() {
		int x = 5;
		int y = 0;

		Cell original = new Cell(CellType.EMPTY);
		original.setCoordinates(x, y);
		
		Cell copy = original.copy();
		
		assertThat(copy.x, equalTo(x));
		assertThat(copy.y, equalTo(y));
	}

	@Test
	public void copyBombRange() {
		int range = 4;

		Cell original = new Cell(CellType.BOMB);
		original.setBombRange(range);
		
		Cell copy = original.copy();
		
		assertThat(copy.bombRange(), equalTo(range));
	}
	
	@Test
	public void copyBombTurnsLeft() {
		int turnsLeft = 7;

		Cell original = new Cell(CellType.BOMB);
		original.setTurnsLeft(turnsLeft);
		
		Cell copy = original.copy();
		
		assertThat(copy.turnsLeft(), equalTo(turnsLeft));
	}
	
	@Test
	public void copyOwner() {
		int owner = 1;

		Cell original = new Cell(CellType.BOMB);
		original.setOwner(owner);
		
		Cell copy = original.copy();
		
		assertThat(copy.owner(), equalTo(owner));
	}
	
	@Test
	public void copyItemType() {
		Cell original = new Cell(CellType.ITEM);
		original.setItemType(ItemType.RANGE);
		
		Cell copy = original.copy();
		
		assertThat(copy.itemType(), equalTo(ItemType.RANGE));
	}
	
}
