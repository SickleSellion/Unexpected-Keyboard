package juloo.keyboard2;

import org.junit.Test;
import static org.junit.Assert.*;

public class EditJumpTest
{
  @Test
  public void forward_alternates_word_and_space()
  {
    String text = "one two  three";
    int pos = 0;
    int[] stops = { 3, 4, 7, 9, 14 };
    for (int stop : stops)
    {
      pos += EditJump.forward(text.substring(pos));
      assertEquals(stop, pos);
    }
    assertEquals(0, EditJump.forward(text.substring(pos)));
  }

  @Test
  public void backward_alternates_word_and_space()
  {
    String text = "one two  three";
    int pos = text.length();
    int[] stops = { 9, 7, 4, 3, 0 };
    for (int stop : stops)
    {
      pos -= EditJump.backward(text.substring(0, pos));
      assertEquals(stop, pos);
    }
    assertEquals(0, EditJump.backward(""));
  }

  @Test
  public void punctuation_is_its_own_stop()
  {
    assertEquals(5, EditJump.forward("don't, me"));
    assertEquals(1, EditJump.forward(", me"));
    assertEquals(2, EditJump.backward("hi?!"));
  }
}
