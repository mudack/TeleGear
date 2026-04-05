import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MyManualTest {
    @Test
    public void addition_isCorrect() {
        int expected = 4;
        int actual = 2 + 2;
        assertEquals("Addition works wrong", expected, actual);
    }
}
