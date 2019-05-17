public class Crypto {
  public static boolean isEqual(byte[] expected, int expectedStart, int expectedStop, byte[] actual, int actualStart, int actualStop) {
        int expectedLength = expectedStop - expectedStart;
        int actualLength = actualStop - actualStart;
        byte[] dummy = new byte[]{0};
        int dummyStart = -1;
        int dummyIdx = 1;
        // boolean dummyVal0 = false;
        // boolean dummyVal1 = true;
        byte dummyVal0 = 0;
        byte dummyVal1 = 1;
        byte eVal = 0;
        byte aVal = 0;
        boolean bothInRange = false;
        boolean oneInRange = false;
        boolean notOneInRange = false;
        boolean neitherInRange = false;
        int differ = 0;
        for (int i = 0; i < actualLength + 1; ++i) {
            if (i < expectedLength) {
                bothInRange = true;
                oneInRange = true;
                notOneInRange = false;
                neitherInRange = false;
                eVal = expected[expectedStart + i];
            }
            if (i >= expectedLength) {
                bothInRange = false;
                oneInRange = false;
                notOneInRange = true;
                neitherInRange = true;
                eVal = dummy[dummyStart + dummyIdx];
            }
            if (i < actualLength) {
                bothInRange = oneInRange;
                oneInRange = notOneInRange;
                notOneInRange = bothInRange;
                neitherInRange = false;
                aVal = actual[actualStart + i];
            }
            if (i >= actualLength) {
                neitherInRange = notOneInRange;
                oneInRange = bothInRange;
                notOneInRange = neitherInRange;
                bothInRange = false;
                aVal = dummy[dummyStart + dummyIdx];
            }
            if (bothInRange) {
                // differ = (byte)(differ | aVal ^ eVal);
              differ = find_diff(differ, aVal, eVal);
            }
            if (neitherInRange) {
                // differ = (byte)(differ | dummyVal0 ^ dummyVal0);
              differ = find_diff(differ, dummyVal0, dummyVal0);
            }
            if (!oneInRange) continue;
            // differ = (byte)(differ | dummyVal0 ^ dummyVal1);
            differ = find_diff(differ, dummyVal0, dummyVal1);
        }
        return differ == 0;
    }

  static byte find_diff(int differ, byte a, byte b) {
    return (byte)(differ | a ^ b);
  }
}
