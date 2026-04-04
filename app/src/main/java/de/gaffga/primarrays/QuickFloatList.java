package de.gaffga.primarrays;

public class QuickFloatList {
    private int count;
    private float[] elements;

    public QuickFloatList() {
        this(10);
    }

    public QuickFloatList(int i) {
        this.elements = new float[i];
        this.count = 0;
    }

    public float pop() {
        this.count--;
        return this.elements[this.count];
    }

    public void push(float f) {
        float[] fArr = this.elements;
        int i = this.count;
        this.count = i + 1;
        fArr[i] = f;
    }

    public void add(float f) {
        float[] fArr = this.elements;
        int i = this.count;
        this.count = i + 1;
        fArr[i] = f;
    }

    public float get(int i) {
        return this.elements[i];
    }

    public void set(int i, float f) {
        this.elements[i] = f;
    }

    public void addLastUnique(float f) {
        if (this.count == 0 || this.elements[this.count - 1] != f) {
            add(f);
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

    public void addAll(QuickFloatList quickFloatList) {
        System.arraycopy(quickFloatList.elements, 0, this.elements, this.count, quickFloatList.count);
        this.count += quickFloatList.count;
    }

    public float removeIndex(int i) {
        float f = this.elements[i];
        if (i >= 0 && i < this.count) {
            while (i < this.count - 1) {
                int i2 = i + 1;
                this.elements[i] = this.elements[i2];
                i = i2;
            }
            this.count--;
        }
        return f;
    }

    public float peek() {
        return this.elements[this.count - 1];
    }

    public int removeValue(float f) {
        for (int i = 0; i < this.count; i++) {
            if (this.elements[i] == f) {
                removeIndex(i);
                return i;
            }
        }
        return -1;
    }

    public int removeReverse(float f) {
        for (int i = this.count - 1; i >= 0; i--) {
            if (this.elements[i] == f) {
                removeIndex(i);
                return i;
            }
        }
        return -1;
    }
}
