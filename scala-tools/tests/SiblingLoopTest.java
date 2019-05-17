public class SiblingLoopTest {
  public static int siblingLoops(int N, int M, int P) {
    int v1 = 0;
    for (int i = 0; i < N; i++) {
      int v2 = 0;
      int v3 = 0;
      if ((v1 % 3) == 0) {
        for (int j = 0; j < M; j++) {
          v2 = v2 + 1;
        }
      } else {
        for (int k = 0; k < P; k++) {
          v3 = v3 + 1;
        }
      }
      v1 = v1 + v2 + v3;
    }

    return v1;
  }

  public static void siblingLoops(int N, int M, int P, boolean cond) {
    int v1 = 0;
    for (int i = 0; i < N; i++) {
      int v2 = 0;
      int v3 = 0;
      if (cond) {
        for (int j = 0; j < M; j++) {
          v2 = v2 + 1;
          if (cond) {
              for (int h = 0; h < M; h++) {
                  v2 = v2 + 1;
              }
          } else if (!cond && v2 > 3) {
              for (int h = 0; h < M; h++) {
                  v2 = v2 + 1;
              }
          } else {
              for (int h = 0; h < M; h++) {
                  v2 = v2 + 1;
              }
          }
        }
      } else {
        for (int k = 0; k < P; k++) {
          v3 = v3 + 1;
        }
      }
      v1 = v1 + v2 + v3;
    }
  }

  public static void siblingLoopsIfs(int N, int M, int P, boolean cond) {
    int v1 = 0;
    int v2 = 0;
    int v3 = 0;
    for (int j = 0; j < M; j++) {
      v2 = v2 + 1;
      if (cond) {
          for (int h = 0; h < M; h++) {
              v2 = v2 + 1;
          }
      } else if (!cond && v2 > 3) {
          for (int h = 0; h < M; h++) {
              v2 = v2 + 1;
          }
      } else {
          for (int h = 0; h < M; h++) {
              v2 = v2 + 1;
          }
      }
      v1 = v1 + v2 + v3;
    }
  }

  public static void siblingLoopsMany(int N, int M, int P, boolean cond) {
    int v1 = 0;
    for (int i = 0; i < N; i++) {
      int v2 = 0;
      int v3 = 0;
      if (cond) {
        for (int j = 0; j < M; j++) {
            for (int h = 0; h < M; h++) {
                for (int g = 0; g < M; g++) {
                  v2 = v2 + 1;
                }
            }

            for (int h = 0; h < M; h++) {
              v2 = v2 + 1;
            }

            for (int h = 0; h < M; h++) {
              v2 = v2 + 1;
            }
        }
      } else {
        for (int k = 0; k < P; k++) {
          v3 = v3 + 1;
        }

        for (int j = 0; j < M; j++) {
            for (int h = 0; h < M; h++) {
                for (int g = 0; g < M; g++) {
                    for (int f = 0; f < M; f++) {
                      v2 = v2 + 1;
                    }
                }
            }
            for (int h = 0; h < M; h++) {
                for (int g = 0; g < M; g++) {
                    for (int f = 0; f < M; f++) {
                      v2 = v2 + 1;
                    }
                }
                for (int g = 0; g < M; g++) {
                    for (int f = 0; f < M; f++) {
                      v2 = v2 + 1;
                    }
                }
            }
        }
      }
      v1 = v1 + v2 + v3;
    }
  }


}
