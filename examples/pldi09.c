#include <assert.h>

int nondet;

void PLDI09_cyclic(int id, int maxId) {
   assert(0 <= id && id < maxId);
   int tmp = id + 1;
   while(tmp != id && nondet)
      if (tmp <= maxId)
         tmp = tmp + 1;
      else tmp = 0;
}

void PLDI09_Example2(int n, int m) {
  assert(n > 0 && m > 0);
  int v1 = n; int v2 = 0;
  while (v1 > 0 && nondet) {
    if (v2 < m) { v2++; v1--; }
    else { v2 = 0; }
  }
}

void PLDI09_Example3(int n, int m) {
  assert(0 < m && m < n);
  int i = 0; int j = 0;
  while (i < n && nondet) {
    if (j < m) {j++;}
    else {j = 0; i++;}
  }
}

void PLDI09_Example4(int n, int m) {
  assert(0 < m && m < n);
  int i = n;
  while (i > 0 && nondet) {
    if (i < m) {i--;}
    else {i = i - m;}
  }
}

int fwd;

void PLDI09_Example5(int n, int m, int dir) {
  assert(0 < m && m < n);
  int i = m;
  while (0 < i && i < n) {
    if (dir==fwd) i++; else i--;
  }
}
