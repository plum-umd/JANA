class User {
  private boolean passwordsEqual(String a, String b) {
    int bLen;
    boolean equal = true;
    boolean shmequal = true;
    int aLen = a.length();
    if (aLen != (bLen = b.length())) {
      equal = false;
    }
    int min = Math.min(aLen, bLen);
    for (int i = 0; i < min; ++i) {
      if (a.charAt(i) != b.charAt(i)) {
        equal = false;
        continue;
      }
      shmequal = true;
    }
    return equal;
  }
}
