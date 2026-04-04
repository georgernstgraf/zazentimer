package de.gaffga.primarrays;

public class QuickIntList {
    private int count;
    private int[] elements;

    public QuickIntList() {
        this(10);
    }

    public QuickIntList(int i) {
        this.elements = new int[i];
        this.count = 0;
    }

    public int pop() {
        this.count--;
        return this.elements[this.count];
    }

    public void push(int i) {
        int[] iArr = this.elements;
        int i2 = this.count;
        this.count = i2 + 1;
        iArr[i2] = i;
    }

    public void add(int i) {
        int[] iArr = this.elements;
        int i2 = this.count;
        this.count = i2 + 1;
        iArr[i2] = i;
    }

    public void set(int i, int i2) {
        this.elements[i] = i2;
    }

    public int get(int i) {
        return this.elements[i];
    }

    public void addLastUnique(int i) {
        if (this.count == 0 || this.elements[this.count - 1] != i) {
            add(i);
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

    public void addAll(QuickIntList quickIntList) {
        System.arraycopy(quickIntList.elements, 0, this.elements, this.count, quickIntList.count);
        this.count += quickIntList.count;
    }

    public int removeIndex(int i) {
        int i2 = this.elements[i];
        if (i >= 0 && i < this.count) {
            System.arraycopy(this.elements, i + 1, this.elements, i, (this.count - i) - 1);
            this.count--;
        }
        return i2;
    }

    public int peek() {
        return this.elements[this.count - 1];
    }

    public int removeValue(int i) {
        for (int i2 = 0; i2 < this.count; i2++) {
            if (this.elements[i2] == i) {
                removeIndex(i2);
                return i2;
            }
        }
        return -1;
    }

    public int removeReverse(int i) {
        for (int i2 = this.count - 1; i2 >= 0; i2--) {
            if (this.elements[i2] == i) {
                removeIndex(i2);
                return i2;
            }
        }
        return -1;
    }

    public int find(int i) {
        for (int i2 = 0; i2 < this.count; i2++) {
            if (this.elements[i2] == i) {
                return i2;
            }
        }
        return -1;
    }

    public boolean contains(int i) {
        return find(i) != -1;
    }

    public int last() {
        return this.elements[this.count - 1];
    }

    public void copyFrom(QuickIntList quickIntList) {
        clear();
        for (int i = 0; i < quickIntList.size(); i++) {
            add(quickIntList.get(i));
        }
    }
}
