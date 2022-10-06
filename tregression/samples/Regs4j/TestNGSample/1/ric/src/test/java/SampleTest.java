import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class SampleTest {
    @Test
    public void test() {
        Sample sample = new Sample();
        int actualResult = sample.sampleMethod();
        assertEquals(actualResult, 100);
    }
}
