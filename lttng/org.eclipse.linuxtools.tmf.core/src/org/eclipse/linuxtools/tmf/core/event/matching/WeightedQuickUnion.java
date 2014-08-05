package org.eclipse.linuxtools.tmf.core.event.matching;


/**
 * Source: Algorithms: part I (4th edition)
 *
 * @author Francis Giraldeau <francis.giraldeau@gmail.com>
 * @since 4.0
 *
 */
public class WeightedQuickUnion {

    private int id[];
    private int sz[];
    private int count;

    public WeightedQuickUnion(int n) {
        count = n;
        id = new int[n];
        sz = new int[n];
        for (int i = 0; i < n; i++) {
            id[i] = i;
            sz[i] = 1;
        }
    }

    public int count() {
        return count;
    }

    public boolean connected(int p, int q) {
        return find(p) == find(q);
    }

    public int find(int p) {
        int k = p;
        while(k != id[k]) {
            k = id[k];
        }
        return k;
    }

    public void union(int p, int q) {
        int i = find(p);
        int j = find(q);
        if (i == j) {
            return;
        }

        if (sz[i] < sz[j]) {
            id[i] = j;
            sz[j] += sz[i];
        } else {
            id[j] = i;
            sz[i] += sz[j];
        }
        count--;
    }
}
