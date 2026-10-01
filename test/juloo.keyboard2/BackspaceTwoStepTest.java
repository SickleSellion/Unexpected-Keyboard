package juloo.keyboard2;

import org.junit.Test;
import static org.junit.Assert.*;

public class BackspaceTwoStepTest
{
  @Test
  public void first_step_is_a_diagonal_up_left()
  {
    assertTrue(Pointers.is_up_left(-10, -10));
    assertTrue(Pointers.is_up_left(-10, -5));
    assertFalse(Pointers.is_up_left(-10, -2)); // Mostly left
    assertFalse(Pointers.is_up_left(-2, -10)); // Mostly up
    assertFalse(Pointers.is_up_left(10, -10));
    assertFalse(Pointers.is_up_left(-10, 10));
  }

  @Test
  public void second_step_picks_the_corner()
  {
    float d = 10;
    assertEquals(2, Pointers.two_step_corner(12, -12, d)); // NE: delete
    assertEquals(4, Pointers.two_step_corner(12, 12, d)); // SE: word forward
    assertEquals(3, Pointers.two_step_corner(-12, 12, d)); // SW: word backward
    assertEquals(-1, Pointers.two_step_corner(-12, -12, d)); // On up-left
    assertEquals(-1, Pointers.two_step_corner(3, 3, d)); // Too short
  }
}
