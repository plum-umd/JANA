class Trails {
  public static boolean slides(char pw[], char guess[]) {
    int i = 0;
    boolean stop = false;
    // assume same length
    while (i < guess.length) {
      if (stop)
        break;
      if (guess[i] != pw[i])
        stop = true;
      else
        i++;
    }

    return !stop; // true if right password
  }

  public static void main(String args[]) {
    char pw[] = {'h', 'e', 'l', 'l', 'o'};
    char guess1[] = {'h', 'e', 'l', 'l', 'o'};
    char guess2[] = {'h', 'e', 'r', 'l', 'd'};
    System.out.println(slides(pw, guess1));
    // System.out.println(slides(pw, guess2));
  }
}
