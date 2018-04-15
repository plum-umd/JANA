class TreeNode<K, V> {
  TreeNode<K, V> left;
  TreeNode<K, V> right;
  int hash;
  Object key;

  boolean doComparison(Class<?> kc, Object k, Object pk) {
    // dummy function
    if (k.hashCode() % 2 == 0) {
      return true;
    } else {
      return false;
    }
  }

  int initDir(Object k) {
    // dummy function
    if (k.hashCode() % 2 == 0) {
      return -1;
    } else {
      return 1;
    }
  }
  
  final TreeNode<K, V> find(int h, Object k, Class<?> kc) {
        TreeNode<K, V> p = this;
        do {
            int dir = initDir(k);
            TreeNode<K, V> pl = p.left;
            TreeNode<K, V> pr = p.right;
            int ph = p.hash;
            if (ph > h) {
                p = pl;
                continue;
            }
            if (ph < h) {
                p = pr;
                continue;
            }
            Object pk = p.key;
            if (pk == k || k != null && k.equals(pk)) {
                return p;
            }
            if (pl == null) {
                p = pr;
                continue;
            }
            if (pr == null) {
                p = pl;
                continue;
            }
            if (kc != null || doComparison(kc, k, pk)) {
                p = dir < 0 ? pl : pr;
                continue;
            }
            TreeNode<K, V> q = pr.find(h, k, kc);
            if (q != null) {
                return q;
            }
            p = pl;
        } while (p != null);
        return null;
    }

// final TreeNode<K, V> find(int h, Object k, Class<?> kc) {
//         TreeNode<K, V> p = this;
//         do {
//             int dir;
//             TreeNode<K, V> pl = p.left;
//             TreeNode<K, V> pr = p.right;
//             int ph = p.hash;
//             if (ph > h) {
//                 p = pl;
//                 continue;
//             }
//             if (ph < h) {
//                 p = pr;
//                 continue;
//             }
//             Object pk = p.key;
//             if (pk == k || k != null && k.equals(pk)) {
//                 return p;
//             }
//             if (pl == null) {
//                 p = pr;
//                 continue;
//             }
//             if (pr == null) {
//                 p = pl;
//                 continue;
//             }
//             if ((kc != null || (kc = this.comparableClassFor(k)) != null) && (dir = this.compareComparables(kc, k, pk)) != 0) {
//                 p = dir < 0 ? pl : pr;
//                 continue;
//             }
//             TreeNode<K, V> q = pr.find(h, k, kc);
//             if (q != null) {
//                 return q;
//             }
//             p = pl;
//         } while (p != null);
//         return null;
//     }
}
