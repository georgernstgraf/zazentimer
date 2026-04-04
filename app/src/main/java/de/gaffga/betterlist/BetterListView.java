package de.gaffga.betterlist;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.Nullable;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import de.gaffga.android.zazentimer.R;
import de.gaffga.primarrays.QuickLongList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

/* loaded from: classes.dex */
public class BetterListView<T> extends ListView implements ListAdapter, AdapterView.OnItemLongClickListener {
    private static final String TAG = "ZMT_BetterListView";
    private int animSpeed;
    private int backgroundColor;
    private boolean currentlyScrolling;
    private HashSet<DataSetObserver> dataSetObservers;
    private BetterListView<T>.DeleteAnimator deleteAnimator;
    private BitmapView deleteDisplay;
    private int deleteIconSpeed;
    private boolean deleteItem;
    private int deleteSpeed;
    private BitmapView deletedBitmap;
    private long deletedId;
    private T deletedItem;
    private int deletedPos;
    private float deltaX;
    private float deltaY;
    private long downId;
    private int downPos;
    private View downView;
    private float downX;
    private float downY;
    private BitmapView dragDisplay;
    private long dragId;
    private QuickLongList elementIds;
    private ArrayList<T> elements;
    private Handler handler;
    private HashMap<Long, Integer> hiddenIds;
    private Paint hidePaint;
    private LayoutInflater inflater;
    private AccelerateDecelerateInterpolator interpolator;
    private long lastCall;
    private long lastFrame;
    private IBetterListElementHandler<T> listElementHandler;
    private BetterListListener<T> listener;
    private float minSlideDist;
    private ArrayList<BetterListView<T>.MoveSession> moveSessions;
    private long nextId;
    private boolean pendingActionDetection;
    private float scroll;
    private float scrollSpeed;
    private int shadowSize;
    private BetterListView<T>.ShiftAnimator shiftAnimator;
    private int shiftSpeed;
    private long slideId;
    private ETouchState touchState;
    private float touchX;
    private float touchY;
    private BetterListView<T>.UndoDeleteAnimator undoDeleteAnimator;

    /* loaded from: classes.dex */
    public interface BetterListListener<T> {
        void onDeleteItem(T t);

        void onItemClick(T t);

        void onReorder();

        void onUndoDelete(T t);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public enum ETouchState {
        TOUCHSTATE_DOWN,
        TOUCHSTATE_UP
    }

    @Override // android.widget.ListAdapter
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override // android.widget.Adapter
    public int getItemViewType(int i) {
        return 0;
    }

    @Override // android.widget.Adapter
    public int getViewTypeCount() {
        return 1;
    }

    @Override // android.widget.Adapter
    public boolean hasStableIds() {
        return true;
    }

    @Override // android.widget.ListAdapter
    public boolean isEnabled(int i) {
        return true;
    }

    /* loaded from: classes.dex */
    private class UndoDeleteAnimator {
        private UndoDeleteAnimator() {
        }

        public void undo() {
            Log.d(BetterListView.TAG, "undo animator");
            MoveSession moveSession = new MoveSession(new Runnable() { // from class: de.gaffga.betterlist.BetterListView.UndoDeleteAnimator.1
                /* JADX WARN: Multi-variable type inference failed */
                @Override // java.lang.Runnable
                public void run() {
                    Log.d(BetterListView.TAG, "finish undo animator");
                    BetterListView.this.unhideId(BetterListView.this.deletedId);
                    BetterListView.this.deletedBitmap.getBmp().recycle();
                    BetterListView.this.onUndoDelete(BetterListView.this.deletedItem);
                    BetterListView.this.sendDataChanged();
                }
            });
            BetterListView.this.elements.add(BetterListView.this.deletedPos, BetterListView.this.deletedItem);
            BetterListView.this.elementIds.add(BetterListView.this.deletedPos, BetterListView.this.deletedId);
            BetterListView.this.sendDataChanged();
            if (BetterListView.this.deletedPos < BetterListView.this.elements.size() - 1) {
                int i = BetterListView.this.deletedPos;
                while (i < BetterListView.this.elements.size()) {
                    int i2 = i + 1;
                    moveSession.moveItemBitmap(i, 0.0f, i2, BetterListView.this.animSpeed);
                    i = i2;
                }
            } else {
                BetterListView.this.smoothScrollToPosition(BetterListView.this.deletedPos);
            }
            BetterListView.this.moveSessions.add(moveSession);
            moveSession.moveItemBitmap(BetterListView.this.deletedBitmap.getBmp(), BetterListView.this.deletedPos, BetterListView.this.deletedBitmap.getWidth(), 0.0f, BetterListView.this.deletedPos, 500);
            moveSession.start();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class MoveSession {
        private Runnable runOnFinish;
        private BetterListView<T>.MoveSession.DrawThread drawThread = null;
        private ArrayList<BetterListView<T>.MoveProcess> runningMoveProcesses = new ArrayList<>();

        public MoveSession() {
        }

        public int getItemCount() {
            return this.runningMoveProcesses.size();
        }

        public BetterListView<T>.MoveProcess getItem(int i) {
            return this.runningMoveProcesses.get(i);
        }

        public void stopNow() {
            for (int i = 0; i < this.runningMoveProcesses.size(); i++) {
                this.runningMoveProcesses.get(i).stopNow();
                BetterListView.this.unhideId(((MoveProcess) this.runningMoveProcesses.get(i)).id);
            }
            this.runningMoveProcesses.clear();
            if (this.runOnFinish != null) {
                this.runOnFinish.run();
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public class DrawThread extends AsyncTask<Void, Void, Void> {
            private boolean running;

            private DrawThread() {
                this.running = false;
            }

            /* JADX INFO: Access modifiers changed from: protected */
            @Override // android.os.AsyncTask
            public Void doInBackground(Void... voidArr) {
                this.running = true;
                while (this.running) {
                    BetterListView.this.postInvalidate();
                    try {
                        Thread.sleep(15L);
                    } catch (InterruptedException unused) {
                    }
                }
                return null;
            }

            public void stopNow() {
                this.running = false;
                MoveSession.this.drawThread = null;
            }
        }

        /* loaded from: classes.dex */
        private class DrawThread2 extends Thread {
            private boolean running = false;

            private DrawThread2() {
            }

            @Override // java.lang.Thread, java.lang.Runnable
            public void run() {
                this.running = true;
                while (this.running) {
                    BetterListView.this.postInvalidate();
                    try {
                        Thread.sleep(15L);
                    } catch (InterruptedException unused) {
                    }
                }
            }

            public void stopNow() {
                this.running = false;
                MoveSession.this.drawThread = null;
            }
        }

        public MoveSession(Runnable runnable) {
            this.runOnFinish = runnable;
        }

        public void moveItemBitmap(int i, float f, int i2, int i3) {
            View viewFromPos = BetterListView.this.getViewFromPos(i);
            if (viewFromPos == null) {
                return;
            }
            long itemId = BetterListView.this.getItemId(i);
            float yPosForPos = BetterListView.this.getYPosForPos(i);
            float x = viewFromPos.getX();
            addMoveProcess(new MoveProcess(BetterListView.this.getBitmapForView(viewFromPos), itemId, x, yPosForPos, f, i2, i3, true));
        }

        public void moveItemBitmap(int i, float f, float f2, int i2, int i3) {
            View viewFromPos = BetterListView.this.getViewFromPos(i);
            if (viewFromPos == null) {
                return;
            }
            long itemId = BetterListView.this.getItemId(i);
            float yPosForPos = BetterListView.this.getYPosForPos(i);
            addMoveProcess(new MoveProcess(BetterListView.this.getBitmapForView(viewFromPos), itemId, f, yPosForPos, f2, i2, i3, true));
        }

        public void moveItemBitmap(Bitmap bitmap, int i, float f, float f2, int i2, int i3) {
            addMoveProcess(new MoveProcess(bitmap, BetterListView.this.getItemId(i), f, BetterListView.this.getYPosForPos(i), f2, i2, i3, false));
        }

        private void addMoveProcess(BetterListView<T>.MoveProcess moveProcess) {
            this.runningMoveProcesses.add(moveProcess);
            moveProcess.start();
        }

        public void start() {
            if (this.drawThread != null) {
                Log.e(BetterListView.TAG, "drawThread already running");
                return;
            }
            for (int i = 0; i < this.runningMoveProcesses.size(); i++) {
                this.runningMoveProcesses.get(i).start();
                BetterListView.this.hideId(((MoveProcess) this.runningMoveProcesses.get(i)).id);
                this.runningMoveProcesses.get(i).step();
            }
            this.drawThread = new DrawThread();
            this.drawThread.execute(null, null, null);
        }

        public boolean step() {
            for (int size = this.runningMoveProcesses.size() - 1; size >= 0; size--) {
                BetterListView<T>.MoveProcess moveProcess = this.runningMoveProcesses.get(size);
                if (!moveProcess.step()) {
                    moveProcess.cleanUp();
                    this.runningMoveProcesses.remove(size);
                    BetterListView.this.unhideId(((MoveProcess) moveProcess).id);
                }
            }
            if (this.runningMoveProcesses.size() != 0) {
                return true;
            }
            if (this.runOnFinish != null) {
                this.runOnFinish.run();
                this.runOnFinish = null;
            }
            if (this.drawThread == null) {
                return false;
            }
            this.drawThread.stopNow();
            this.drawThread = null;
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class MoveProcess {
        private Bitmap bitmap;
        private final float destXPx;
        private final int destYPos;
        private final int duration;
        private final long id;
        private float px;
        private float py;
        private final boolean recycle;
        private long startTime;
        private final float startX;
        private final float startY;

        public MoveProcess(Bitmap bitmap, long j, float f, float f2, float f3, int i, int i2, boolean z) {
            this.recycle = z;
            this.bitmap = bitmap;
            this.id = j;
            this.startX = f;
            this.startY = f2;
            this.destXPx = f3;
            this.destYPos = i;
            this.duration = i2;
        }

        public void start() {
            this.startTime = System.currentTimeMillis();
        }

        public boolean step() {
            float currentTimeMillis = ((float) (System.currentTimeMillis() - this.startTime)) / this.duration;
            if (currentTimeMillis > 1.0f) {
                currentTimeMillis = 1.0f;
            }
            if (currentTimeMillis < 0.0f) {
                currentTimeMillis = 0.0f;
            }
            float yPosForPos = BetterListView.this.getYPosForPos(this.destYPos);
            this.px = this.startX + ((this.destXPx - this.startX) * currentTimeMillis);
            this.py = this.startY + ((yPosForPos - this.startY) * currentTimeMillis);
            return currentTimeMillis < 1.0f;
        }

        public void stopNow() {
            float yPosForPos = BetterListView.this.getYPosForPos(this.destYPos);
            this.px = this.destXPx;
            this.py = yPosForPos;
        }

        public void cleanUp() {
            if (this.recycle) {
                this.bitmap.recycle();
                this.bitmap = null;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class DeleteAnimator {
        private Handler handler = new Handler(Looper.getMainLooper());

        public DeleteAnimator() {
        }

        public void delete(final int i) {
            Log.d(BetterListView.TAG, "deleting pos " + i);
            MoveSession moveSession = new MoveSession(new Runnable() { // from class: de.gaffga.betterlist.BetterListView.DeleteAnimator.1
                @Override // java.lang.Runnable
                public void run() {
                    DeleteAnimator.this.handler.post(new Runnable() { // from class: de.gaffga.betterlist.BetterListView.DeleteAnimator.1.1
                        /* JADX WARN: Multi-variable type inference failed */
                        @Override // java.lang.Runnable
                        public void run() {
                            BetterListView.this.unhideId(BetterListView.this.getItemId(i));
                            BetterListView.this.elements.remove(i);
                            BetterListView.this.elementIds.removeIndex(i);
                            BetterListView.this.onDeleteItem(BetterListView.this.deletedItem);
                            BetterListView.this.sendDataChanged();
                        }
                    });
                }
            });
            BetterListView.this.hideId(BetterListView.this.getItemId(i));
            int currentIdx = i;
            while (true) {
                currentIdx++;
                if (currentIdx < BetterListView.this.elements.size()) {
                    moveSession.moveItemBitmap(currentIdx, 0.0f, currentIdx - 1, BetterListView.this.animSpeed);
                } else {
                    BetterListView.this.moveSessions.add(moveSession);
                    moveSession.start();
                    BetterListView.this.sendDataChanged();
                    return;
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class ShiftAnimator {
        private int curDirection;
        private int curP0;
        private int curP1;
        private int curPos;
        private int curTo;
        private BetterListView<T>.MoveSession shiftMoveSession = null;

        public ShiftAnimator() {
        }

        public void shift(int i, int i2, int i3, int i4, int i5) {
            if (i == this.curPos && i2 == this.curTo && i3 == this.curP0 && i4 == this.curP1 && this.curDirection == i5) {
                return;
            }
            this.curPos = i;
            this.curTo = i2;
            this.curP0 = i3;
            this.curP1 = i4;
            this.curDirection = i5;
            Log.d(BetterListView.TAG, "shift pos=" + i + " to=" + i2 + " p0=" + i3 + " p1=" + i4 + " direction=" + i5);
            if (this.shiftMoveSession != null) {
                this.shiftMoveSession.stopNow();
                this.shiftMoveSession = null;
            }
            this.shiftMoveSession = new MoveSession(new Runnable() { // from class: de.gaffga.betterlist.BetterListView.ShiftAnimator.1
                @Override // java.lang.Runnable
                public void run() {
                    ShiftAnimator.this.shiftMoveSession = null;
                    BetterListView.this.sendDataChanged();
                }
            });
            for (int i6 = i3; i6 <= i4; i6++) {
                if (BetterListView.this.getChildAt(i6 - BetterListView.this.getFirstVisiblePosition()) != null) {
                    this.shiftMoveSession.moveItemBitmap(i3, 0.0f, i3 + i5, BetterListView.this.shiftSpeed);
                }
            }
            BetterListView.this.moveSessions.add(this.shiftMoveSession);
            this.shiftMoveSession.start();
            Object obj = BetterListView.this.elements.get(i);
            long j = BetterListView.this.elementIds.get(i);
            BetterListView.this.elements.remove(i);
            BetterListView.this.elementIds.removeIndex(i);
            BetterListView.this.elements.add(i2, (T) obj);
            BetterListView.this.elementIds.add(i2, j);
            BetterListView.this.onReorder();
            BetterListView.this.sendDataChanged();
        }
    }

    public BetterListView(Context context) {
        this(context, null);
    }

    public BetterListView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, R.attr.betterListView);
    }

    public BetterListView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.dataSetObservers = new HashSet<>();
        this.elements = new ArrayList<>();
        this.elementIds = new QuickLongList(ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION);
        this.nextId = 1L;
        this.animSpeed = ItemTouchHelper.Callback.DEFAULT_SWIPE_ANIMATION_DURATION;
        this.shiftSpeed = 100;
        this.deleteSpeed = ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION;
        this.deleteIconSpeed = ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION;
        this.dragId = -1L;
        this.currentlyScrolling = false;
        this.pendingActionDetection = false;
        this.slideId = -1L;
        this.hidePaint = new Paint();
        this.deleteAnimator = new DeleteAnimator();
        this.shiftAnimator = new ShiftAnimator();
        this.undoDeleteAnimator = new UndoDeleteAnimator();
        this.moveSessions = new ArrayList<>();
        this.hiddenIds = new HashMap<>();
        this.interpolator = new AccelerateDecelerateInterpolator();
        this.deleteDisplay = null;
        this.dragDisplay = null;
        this.listener = null;
        this.lastCall = 0L;
        this.lastFrame = 0L;
        init(attributeSet, i, 0);
    }

    @TargetApi(21)
    public BetterListView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.dataSetObservers = new HashSet<>();
        this.elements = new ArrayList<>();
        this.elementIds = new QuickLongList(ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION);
        this.nextId = 1L;
        this.animSpeed = ItemTouchHelper.Callback.DEFAULT_SWIPE_ANIMATION_DURATION;
        this.shiftSpeed = 100;
        this.deleteSpeed = ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION;
        this.deleteIconSpeed = ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION;
        this.dragId = -1L;
        this.currentlyScrolling = false;
        this.pendingActionDetection = false;
        this.slideId = -1L;
        this.hidePaint = new Paint();
        this.deleteAnimator = new DeleteAnimator();
        this.shiftAnimator = new ShiftAnimator();
        this.undoDeleteAnimator = new UndoDeleteAnimator();
        this.moveSessions = new ArrayList<>();
        this.hiddenIds = new HashMap<>();
        this.interpolator = new AccelerateDecelerateInterpolator();
        this.deleteDisplay = null;
        this.dragDisplay = null;
        this.listener = null;
        this.lastCall = 0L;
        this.lastFrame = 0L;
        init(attributeSet, i, i2);
    }

    private void init(AttributeSet attributeSet, int i, int i2) {
        Log.d(TAG, "init()");
        TypedArray obtainStyledAttributes = getContext().obtainStyledAttributes(attributeSet, new int[]{R.attr.colorPrimary});
        this.backgroundColor = obtainStyledAttributes.getColor(0, 0);
        obtainStyledAttributes.recycle();
        setLongClickable(true);
        setOnItemLongClickListener(this);
        setAdapter((ListAdapter) this);
        this.hidePaint.setStyle(Paint.Style.FILL);
        this.hidePaint.setColor(this.backgroundColor);
        this.inflater = (LayoutInflater) getContext().getSystemService("layout_inflater");
        this.minSlideDist = (int) ((10.0f * getResources().getDisplayMetrics().density) + 0.5d);
        this.shadowSize = (int) TypedValue.applyDimension(1, 20.0f, getResources().getDisplayMetrics());
        this.handler = new Handler(Looper.getMainLooper());
    }

    public void setListElementHandler(IBetterListElementHandler<T> iBetterListElementHandler) {
        this.listElementHandler = iBetterListElementHandler;
    }

    @Override // android.widget.ListView, android.widget.AbsListView, android.view.ViewGroup, android.view.View
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        for (int firstVisiblePosition = getFirstVisiblePosition(); firstVisiblePosition <= getLastVisiblePosition(); firstVisiblePosition++) {
            long itemId = getItemId(firstVisiblePosition);
            View viewFromPos = getViewFromPos(firstVisiblePosition);
            if (viewFromPos != null && (itemId == this.dragId || isIdHidden(itemId) || itemId == this.slideId)) {
                canvas.drawRect(viewFromPos.getX(), viewFromPos.getY(), viewFromPos.getX() + viewFromPos.getWidth(), viewFromPos.getY() + viewFromPos.getHeight(), this.hidePaint);
            }
        }
        for (int size = this.moveSessions.size() - 1; size >= 0; size--) {
            BetterListView<T>.MoveSession moveSession = this.moveSessions.get(size);
            for (int i = 0; i < moveSession.getItemCount(); i++) {
                BetterListView<T>.MoveProcess item = moveSession.getItem(i);
                canvas.drawBitmap(((MoveProcess) item).bitmap, ((MoveProcess) item).px, ((MoveProcess) item).py, (Paint) null);
            }
        }
        if (this.deleteDisplay != null) {
            this.deleteDisplay.draw(canvas);
        }
        if ((this.slideId != -1 || this.dragId != -1) && this.dragDisplay != null) {
            this.dragDisplay.draw(canvas);
        }
        for (int size2 = this.moveSessions.size() - 1; size2 >= 0; size2--) {
            if (!this.moveSessions.get(size2).step()) {
                this.moveSessions.remove(size2);
            }
        }
    }

    @Override // android.widget.AdapterView.OnItemLongClickListener
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long j) {
        Log.d(TAG, "onItemLongClick()");
        if (this.slideId != -1) {
            return true;
        }
        this.dragId = j;
        this.scrollSpeed = 0.0f;
        this.dragDisplay.setDrawShadow(true);
        this.pendingActionDetection = false;
        this.deltaX = this.touchX - view.getX();
        this.deltaY = this.touchY - view.getY();
        sendDataChanged();
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public Bitmap getBitmapForView(View view) {
        Bitmap createBitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        view.draw(new Canvas(createBitmap));
        return createBitmap;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public float getYPosForPos(int i) {
        View childAt = getChildAt(0);
        if (childAt == null) {
            return 0.0f;
        }
        return childAt.getY() + ((i - getFirstVisiblePosition()) * (childAt.getHeight() + getDividerHeight()));
    }

    public float getScrollPos() {
        int childCount = getChildCount();
        int firstVisiblePosition = getFirstVisiblePosition();
        float f = 0.0f;
        for (int i = 0; i < childCount; i++) {
            View childAt = getChildAt(i);
            if (i < firstVisiblePosition) {
                f += childAt.getHeight() + getDividerHeight();
            }
            if (i == firstVisiblePosition) {
                f += childAt.getY();
            }
        }
        return f;
    }

    @Override // android.widget.AbsListView, android.view.View
    public boolean onTouchEvent(MotionEvent motionEvent) {
        int action = motionEvent.getAction() & 255;
        if (action == 0 || action == 1 || action == 2) {
            this.touchX = motionEvent.getX();
            this.touchY = motionEvent.getY();
        }
        if (action == 0) {
            this.touchState = ETouchState.TOUCHSTATE_DOWN;
            this.downPos = getPosFromCoord(this.touchX, this.touchY);
            if (this.downPos != -1) {
                this.pendingActionDetection = true;
                this.downX = this.touchX;
                this.downY = this.touchY;
                this.downId = this.elementIds.get(this.downPos);
                this.downView = getViewFromPos(this.downPos);
                this.dragDisplay = new BitmapView(this, getBitmapForView(this.downView), this.downView.getX(), this.downView.getY(), this.backgroundColor);
                if (this.deleteDisplay == null) {
                    this.deleteDisplay = new BitmapView(this, ((BitmapDrawable) ContextCompat.getDrawable(getContext(), R.drawable.ic_delete)).getBitmap(), 0.0f, this.dragDisplay.getY(), this.backgroundColor);
                    this.deleteDisplay.setAlpha(0.0f);
                } else {
                    this.deleteDisplay.setY(this.dragDisplay.getY());
                }
            }
        }
        if (this.downPos != -1) {
            switch (action) {
                case 1:
                    this.touchState = ETouchState.TOUCHSTATE_UP;
                    this.currentlyScrolling = false;
                    if (this.slideId == -1) {
                        if (this.dragId != -1) {
                            this.dragId = -1L;
                            this.downPos = -1;
                            this.downId = -1L;
                            sendDataChanged();
                            break;
                        } else {
                            int abs = (int) Math.abs(this.touchX - this.downX);
                            int abs2 = (int) Math.abs(this.touchY - this.downY);
                            if (abs <= this.minSlideDist && abs2 <= this.minSlideDist) {
                                onItemClick(getElementAt(this.downPos));
                            }
                            this.downPos = -1;
                            this.downId = -1L;
                            break;
                        }
                    } else {
                        this.deleteDisplay.animateAlphaTo(0.0f, this.deleteSpeed);
                        if (this.deleteItem) {
                            BetterListView<T>.MoveSession moveSession = new MoveSession(new Runnable() { // from class: de.gaffga.betterlist.BetterListView.1
                                @Override // java.lang.Runnable
                                public void run() {
                                    BetterListView.this.deleteElement(BetterListView.this.downPos);
                                    BetterListView.this.downPos = -1;
                                    BetterListView.this.sendDataChanged();
                                }
                            });
                            moveSession.moveItemBitmap(this.dragDisplay.getBmp(), this.downPos, this.dragDisplay.getX(), this.dragDisplay.getWidth(), this.downPos, ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION);
                            this.moveSessions.add(moveSession);
                            moveSession.start();
                            if (this.deletedBitmap != null) {
                                this.deletedBitmap.getBmp().recycle();
                            } else {
                                this.deletedBitmap = new BitmapView(this, this.backgroundColor);
                            }
                            this.deletedBitmap.setBmp(this.dragDisplay.getBmp().copy(Bitmap.Config.ARGB_8888, false));
                            this.deletedBitmap.setX(getWidth());
                            this.deletedBitmap.setY(this.dragDisplay.getY());
                            this.slideId = -1L;
                            this.downId = -1L;
                            break;
                        } else {
                            BetterListView<T>.MoveSession moveSession2 = new MoveSession(new Runnable() { // from class: de.gaffga.betterlist.BetterListView.2
                                @Override // java.lang.Runnable
                                public void run() {
                                    BetterListView.this.invalidate();
                                    BetterListView.this.sendDataChanged();
                                }
                            });
                            moveSession2.moveItemBitmap(this.dragDisplay.getBmp(), this.downPos, this.dragDisplay.getX(), 0.0f, this.downPos, this.deleteSpeed);
                            this.moveSessions.add(moveSession2);
                            moveSession2.start();
                            this.downId = -1L;
                            this.slideId = -1L;
                            this.downPos = -1;
                            break;
                        }
                    }
                case 2:
                    if (this.dragId != -1) {
                        this.dragDisplay.setX(this.touchX - this.deltaX);
                        this.dragDisplay.setY(this.touchY - this.deltaY);
                        if (!this.currentlyScrolling) {
                            checkDragScroll();
                        }
                        checkDragToPlace();
                        return true;
                    }
                    if (this.slideId != -1) {
                        this.dragDisplay.setX(Math.max(this.touchX - this.downX, 0.0f));
                        if (this.dragDisplay.getX() > this.dragDisplay.getWidth() / 3) {
                            this.deleteDisplay.fitToHeight(this.dragDisplay.getHeight());
                            this.deleteDisplay.setInsertFactor(0.2f);
                            if (!this.deleteItem) {
                                this.deleteDisplay.animateAlphaTo(1.0f, this.deleteIconSpeed);
                            }
                            this.deleteItem = true;
                        } else {
                            if (this.deleteItem) {
                                this.deleteDisplay.animateAlphaTo(0.0f, this.deleteIconSpeed);
                            }
                            this.deleteItem = false;
                        }
                        return true;
                    }
                    float f = this.touchX - this.downX;
                    float f2 = this.touchY - this.downY;
                    if (f < this.minSlideDist && Math.abs(f2) > this.minSlideDist) {
                        this.pendingActionDetection = false;
                    }
                    if (this.slideId == -1 && this.pendingActionDetection && f > this.minSlideDist && Math.abs(f2) < this.minSlideDist) {
                        this.slideId = this.downId;
                        this.pendingActionDetection = false;
                        this.dragDisplay.setDrawShadow(false);
                        setPressed(false);
                        this.downView.setPressed(false);
                        sendDataChanged();
                        return true;
                    }
                    break;
            }
        }
        return super.onTouchEvent(motionEvent);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void hideId(long j) {
        int valueOf;
        Integer num = this.hiddenIds.get(Long.valueOf(j));
        if (num == null) {
            valueOf = 1;
        } else {
            valueOf = Integer.valueOf(num.intValue() + 1);
        }
        this.hiddenIds.put(Long.valueOf(j), valueOf);
    }

    private boolean isIdHidden(long j) {
        Integer num = this.hiddenIds.get(Long.valueOf(j));
        return num != null && num.intValue() > 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void unhideId(long j) {
        Integer num = this.hiddenIds.get(Long.valueOf(j));
        if (num != null) {
            int val = num.intValue() - 1;
            if (val == 0) {
                this.hiddenIds.remove(Long.valueOf(j));
            } else {
                this.hiddenIds.put(Long.valueOf(j), Integer.valueOf(val));
            }
        }
    }

    private void checkDragToPlace() {
        int itemPosition = getItemPosition(this.dragId);
        float y = this.dragDisplay.getY();
        float height = this.dragDisplay.getHeight() + y;
        int i = 0;
        View child0 = getChildAt(0);
        if (child0 != null && y < child0.getY() + (child0.getHeight() / 2) && itemPosition != 0) {
            moveElementTo(itemPosition, getFirstVisiblePosition());
        }
        int lastIdx = getChildCount() - 1;
        View childLast = getChildAt(lastIdx);
        if (childLast != null && height > childLast.getY() + (childLast.getHeight() / 2) && itemPosition != this.elements.size() - 1) {
            moveElementTo(itemPosition, getLastVisiblePosition());
        }
        int childCount = getChildCount();
        while (i < childCount - 1) {
            View childAt = getChildAt(i);
            i++;
            View childAt2 = getChildAt(i);
            if ((height > childAt.getY() + (childAt.getHeight() / 2) && height < childAt2.getY()) || (y < childAt2.getY() + (childAt2.getHeight() / 2) && y > childAt.getY() + childAt.getHeight())) {
                int positionForView = getPositionForView(childAt);
                int positionForView2 = getPositionForView(childAt2);
                long j = this.elementIds.get(positionForView);
                long j2 = this.elementIds.get(positionForView2);
                if (j != this.dragId && j2 != this.dragId) {
                    if (itemPosition < positionForView) {
                        moveElementTo(itemPosition, positionForView);
                    } else {
                        moveElementTo(itemPosition, positionForView2);
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkDragScroll() {
        if (this.touchState != ETouchState.TOUCHSTATE_DOWN) {
            return;
        }
        if (!this.currentlyScrolling) {
            this.lastFrame = 15L;
        } else {
            this.lastFrame = System.currentTimeMillis() - this.lastCall;
        }
        this.lastCall = System.currentTimeMillis();
        int i = 1;
        this.currentlyScrolling = true;
        if (this.dragId == -1) {
            return;
        }
        postInvalidate();
        if (this.touchY - this.deltaY < 0.0f) {
            i = -1;
        } else if ((this.touchY - this.deltaY) + this.dragDisplay.getHeight() <= getHeight()) {
            i = 0;
        }
        if (i != 0) {
            int height = this.dragDisplay.getHeight();
            float f = 1000.0f / ((float) this.lastFrame);
            float f2 = (height * 3.0f) / f;
            this.scrollSpeed = Math.min(this.scrollSpeed + (f2 / (0.5f * f)), f2);
            this.scroll += this.scrollSpeed;
            if (this.scroll >= 1.0f) {
                if (Build.VERSION.SDK_INT >= 19) {
                    scrollListBy((int) (i * this.scroll));
                } else {
                    setSelectionFromTop(getFirstVisiblePosition(), ((int) getChildAt(0).getY()) - ((int) (i * this.scroll)));
                    invalidate();
                }
                this.scroll -= (int) this.scroll;
            }
            getHandler().postDelayed(new Runnable() { // from class: de.gaffga.betterlist.BetterListView.3
                @Override // java.lang.Runnable
                public void run() {
                    BetterListView.this.checkDragScroll();
                }
            }, 15L);
            return;
        }
        this.currentlyScrolling = false;
        this.scroll = 0.0f;
        this.scrollSpeed = 0.0f;
    }

    private void moveElementTo(int i, int i2) {
        Log.d(TAG, "moveElementTo pos=" + i + " p1=" + i2);
        if (i < i2) {
            this.shiftAnimator.shift(i, i2, i + 1, i2, -1);
        } else {
            this.shiftAnimator.shift(i, i2, i2, i - 1, 1);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void deleteElement(int i) {
        this.deletedPos = i;
        this.deletedItem = this.elements.get(i);
        this.deletedId = this.elementIds.get(i);
        this.deleteAnimator.delete(i);
        Snackbar make = Snackbar.make(this, "Deleted '" + this.deletedItem.toString() + "'", 0);
        make.setAction("UNDO", new View.OnClickListener() { // from class: de.gaffga.betterlist.BetterListView.4
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                BetterListView.this.undoDeleteAnimator.undo();
            }
        });
        TypedArray obtainStyledAttributes = getContext().obtainStyledAttributes(new int[]{R.attr.colorText, R.attr.colorZenAccentHighlight});
        make.getView().setBackgroundColor(obtainStyledAttributes.getColor(1, -10461088));
        make.setActionTextColor(obtainStyledAttributes.getColor(0, -4144960));
        obtainStyledAttributes.recycle();
        make.setCallback(new Snackbar.Callback() { // from class: de.gaffga.betterlist.BetterListView.5
            @Override // com.google.android.material.snackbar.Snackbar.Callback
            public void onDismissed(Snackbar snackbar, int i2) {
                if (i2 != 1) {
                    BetterListView.this.deletedBitmap.getBmp().recycle();
                }
            }
        });
        make.show();
    }

    @Override // android.widget.Adapter
    public void registerDataSetObserver(DataSetObserver dataSetObserver) {
        Log.d(TAG, "registerDataSetObserver()");
        this.dataSetObservers.add(dataSetObserver);
    }

    @Override // android.widget.Adapter
    public void unregisterDataSetObserver(DataSetObserver dataSetObserver) {
        Log.d(TAG, "unregisterDataSetObserver()");
        this.dataSetObservers.remove(dataSetObserver);
    }

    @Override // android.widget.Adapter
    public Object getItem(int i) {
        return this.elements.get(i);
    }

    public T getElementAt(int i) {
        return this.elements.get(i);
    }

    @Override // android.widget.Adapter
    public long getItemId(int i) {
        return this.elementIds.get(i);
    }

    @Override // android.widget.Adapter
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = this.inflater.inflate(this.listElementHandler.getListItemResourceId(), viewGroup, false);
        }
        long j = this.elementIds.get(i);
        if (j != this.dragId && !isIdHidden(j)) {
            long j2 = this.slideId;
        }
        this.listElementHandler.setupView(view, this.elements.get(i));
        return view;
    }

    @Override // android.widget.Adapter
    public boolean isEmpty() {
        return this.elements.size() == 0;
    }

    public void clear() {
        this.elements.clear();
        this.elementIds.clear();
    }

    @Override // android.widget.AdapterView, android.widget.Adapter
    public int getCount() {
        return this.elements.size();
    }

    public void add(T t) {
        this.elements.add(t);
        QuickLongList quickLongList = this.elementIds;
        long j = this.nextId;
        this.nextId = j + 1;
        quickLongList.add(j);
        sendDataChanged();
    }

    public int getItemPosition(long j) {
        for (int i = 0; i < this.elementIds.size(); i++) {
            if (this.elementIds.get(i) == j) {
                return i;
            }
        }
        return -1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendDataChanged() {
        Iterator<DataSetObserver> it = this.dataSetObservers.iterator();
        while (it.hasNext()) {
            it.next().onChanged();
        }
    }

    private void postSendDataChanged() {
        this.handler.post(new Runnable() { // from class: de.gaffga.betterlist.BetterListView.6
            @Override // java.lang.Runnable
            public void run() {
                BetterListView.this.sendDataChanged();
            }
        });
    }

    private int getPosFromCoord(float f, float f2) {
        Rect rect = new Rect();
        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).getHitRect(rect);
            if (rect.contains((int) f, (int) f2)) {
                return i + getFirstVisiblePosition();
            }
        }
        return -1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    @Nullable
    public View getViewFromPos(int i) {
        for (int i2 = 0; i2 < getChildCount(); i2++) {
            View childAt = getChildAt(i2);
            if (i == getFirstVisiblePosition() + i2) {
                return childAt;
            }
        }
        return null;
    }

    public void setListener(BetterListListener<T> betterListListener) {
        this.listener = betterListListener;
    }

    public void onDeleteItem(T t) {
        if (this.listener != null) {
            this.listener.onDeleteItem(t);
        }
    }

    public void onReorder() {
        if (this.listener != null) {
            this.listener.onReorder();
        }
    }

    public void onUndoDelete(T t) {
        if (this.listener != null) {
            this.listener.onUndoDelete(t);
        }
    }

    public void onItemClick(T t) {
        if (this.listener != null) {
            this.listener.onItemClick(t);
        }
    }
}
