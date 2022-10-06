import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class TestNGTest {
    @Test
    public void test() {
        int a = 0;
        assertEquals(a, 0);
    }

    @Test
    public void test1() {
        int a = 1;
        assertEquals(a, 1);
    }
}
