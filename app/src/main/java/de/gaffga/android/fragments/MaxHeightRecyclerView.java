package de.gaffga.android.fragments;

import android.content.Context;
import android.util.AttributeSet;
import androidx.recyclerview.widget.RecyclerView;

public class MaxHeightRecyclerView extends RecyclerView {

    private int maxHeightPx = Integer.MAX_VALUE;

    public MaxHeightRecyclerView(Context context) {
        super(context);
    }

    public MaxHeightRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MaxHeightRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setMaxHeight(int maxHeightPx) {
        this.maxHeightPx = maxHeightPx;
        requestLayout();
    }

    public int getMaxHeight() {
        return maxHeightPx;
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        super.onMeasure(widthSpec, heightSpec);
        if (getMeasuredHeight() > maxHeightPx) {
            setMeasuredDimension(getMeasuredWidth(), maxHeightPx);
        }
    }
}
