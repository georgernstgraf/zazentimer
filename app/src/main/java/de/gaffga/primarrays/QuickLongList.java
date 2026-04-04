package de.gaffga.primarrays;

public class QuickLongList {
    private int count;
    private long[] elements;

    public QuickLongList() {
        this(10);
    }

    public QuickLongList(int i) {
        this.elements = new long[i];
        this.count = 0;
    }

    public long pop() {
        this.count--;
        return this.elements[this.count];
    }

    public void push(long j) {
        long[] jArr = this.elements;
        int i = this.count;
        this.count = i + 1;
        jArr[i] = j;
    }

    public void add(long j) {
        long[] jArr = this.elements;
        int i = this.count;
        this.count = i + 1;
        jArr[i] = j;
    }

    public void add(int i, long j) {
        System.arraycopy(this.elements, i, this.elements, i + 1, this.count - i);
        this.elements[i] = j;
        this.count++;
    }

    public void set(int i, long j) {
        this.elements[i] = j;
    }

    public long get(int i) {
        return this.elements[i];
    }

    public void addLastUnique(long j) {
        if (this.count == 0 || this.elements[this.count - 1] != j) {
            add(j);
        }
    }

    public int size() {
        return this.count;
    }

    public void clear() {
        this.count = 0;
    }

    public boolean isEmpty() {
        return this.count == 0;
    }

    public void addAll(QuickLongList quickLongList) {
        System.arraycopy(quickLongList.elements, 0, this.elements, this.count, quickLongList.count);
        this.count += quickLongList.count;
    }

    public long removeIndex(int i) {
        long j = this.elements[i];
        if (i >= 0 && i < this.count) {
            System.arraycopy(this.elements, i + 1, this.elements, i, (this.count - i) - 1);
            this.count--;
        }
        return j;
    }

    public long peek() {
        return this.elements[this.count - 1];
    }

    public int removeValue(long j) {
        for (int i = 0; i < this.count; i++) {
            if (this.elements[i] == j) {
                removeIndex(i);
                return i;
            }
        }
        return -1;
    }

    public int removeReverse(long j) {
        for (int i = this.count - 1; i >= 0; i--) {
            if (this.elements[i] == j) {
                removeIndex(i);
                return i;
            }
        }
        return -1;
    }

    public int find(long j) {
        for (int i = 0; i < this.count; i++) {
            if (this.elements[i] == j) {
                return i;
            }
        }
        return -1;
    }

    public boolean contains(long j) {
        return find(j) != -1;
    }

    public long last() {
        return this.elements[this.count - 1];
    }

    public void copyFrom(QuickLongList quickLongList) {
        clear();
        for (int i = 0; i < quickLongList.size(); i++) {
            add(quickLongList.get(i));
        }
    }
}
