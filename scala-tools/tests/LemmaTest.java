/*

NAMING LEGEND:

[additive lemma tests]

each function tests a combination of init and trans types:
Z means zero
C means non-zero constant
V means variable or expression

e.g., ZC means an init of zero, and a step of a constant value

[multiplicative lemma tests]

O means one
C means non-one constant
V means variable or expression

*/

// TODO validate negative increments
// TODO distinguish multiplicative lemmas, 2 + 2 vs 2 * 2, etc
// TODO test more constant increments and variable expression
// TODO test additive starting at 1

public class LemmaTest {
  public static void ZZinf(int max) {
    // this is an infinite loop
    int x = 0;
    while (x < max) {
      x = x + 0;
    }
  }
  public static void ZC(int max) {
    int x = 0;
    while (x < max) {
      x = x + 2;
    }
  }
  public static void ZV(int max, int input) {
    int x = 0;
    while (x < max) {
      x = x + 2 * input;
    }
  }
  public static void CZ(int max) {
    int x = 2;
    while (x < max) {
      x = x - 2;
    }
  }
  public static void CC(int max) {
    int x = 2;
    while (x < max) {
      x = x + 2;
    }
  }
  public static void CCinf(int max) {
    // if init and trans are the same, then infinite loop
    int x = 1;
    while (x < max) {
      x = x + 0;
    }
  }
  public static void CV(int max, int input) {
    int x = 2;
    while (x < max) {
      x = x + 2 * input;
    }
  }
  public static void VZ(int max, int init) {
    int x = 5 * init;
    while (x < max) {
      x = x - 2;
    }
  }
  public static void VC(int max, int init) {
    // infinite loop
    int x = 5 * init;
    while (x < max) {
      x = 2;
    }
  }
  public static void VV(int max, int init, int input) {
    int x = 5 * init;
    while (x < max) {
      x = x + 2 * input;
    }
  }
}
