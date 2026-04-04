#!/bin/bash
sed -i 's/android.support.v7.app.AppCompatActivity/androidx.appcompat.app.AppCompatActivity/g' $(grep -rl "android.support.v7.app.AppCompatActivity" app/src/main/java/)
sed -i 's/android.support.v7.app.AlertDialog/androidx.appcompat.app.AlertDialog/g' $(grep -rl "android.support.v7.app.AlertDialog" app/src/main/java/)
sed -i 's/android.support.v4.content.ContextCompat/androidx.core.content.ContextCompat/g' $(grep -rl "android.support.v4.content.ContextCompat" app/src/main/java/)
sed -i 's/android.support.design.widget.FloatingActionButton/com.google.android.material.floatingactionbutton.FloatingActionButton/g' $(grep -rl "android.support.design.widget.FloatingActionButton" app/src/main/java/)
sed -i 's/android.support.design.widget.Snackbar/com.google.android.material.snackbar.Snackbar/g' $(grep -rl "android.support.design.widget.Snackbar" app/src/main/java/)
sed -i 's/android.support.v7.widget.helper.ItemTouchHelper/androidx.recyclerview.widget.ItemTouchHelper/g' $(grep -rl "android.support.v7.widget.helper.ItemTouchHelper" app/src/main/java/)
sed -i 's/android.support.v7.widget.Toolbar/androidx.appcompat.widget.Toolbar/g' $(grep -rl "android.support.v7.widget.Toolbar" app/src/main/java/)
sed -i 's/android.support.annotation.Nullable/androidx.annotation.Nullable/g' $(grep -rl "android.support.annotation.Nullable" app/src/main/java/)
