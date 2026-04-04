package de.gaffga.betterlist;

import android.view.View;

public interface IBetterListElementHandler<T> {
    int getListItemResourceId();

    void setupView(View view, T t);
}
