package la.il.comedykeyboard;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

/**
 * Taken from: https://android.googlesource.com/platform/development/+/master/samples/SoftKeyboard/src/com/example/android/softkeyboard/CandidateView.java
 *
 * Customized for simplified usage.
 */
public class CandidateView extends View {
    private static final int OUT_OF_BOUNDS = -1;
    private KeyboardService mService;
    private List<String> mSuggestions;
    private int mSelectedIndex;
    private int mTouchX = OUT_OF_BOUNDS;
    private Drawable mSelectionHighlight;
    Rect padding = new Rect();

    private Rect mBgPadding;
    private static final int SCROLL_PIXELS = 20;
    private static final int X_GAP = 10;

    private static final List<String> EMPTY_LIST = new ArrayList<String>();
    private int mColorNormal;
    private int mColorOther;
    private int mVerticalPadding;
    private Paint mPaint;
    private boolean mScrolled;
    private int mTargetScrollX;

    private int mTotalWidth;

    private GestureDetector mGestureDetector;
    /**
     * Construct a CandidateView for showing suggested words for completion.
     * @param context
     */
    public CandidateView(Context context) {
        super(context);
        mSelectionHighlight = context.getResources().getDrawable(
                R.drawable.candidate_selector_background);
        mSelectionHighlight.setState(new int[] {
                android.R.attr.state_enabled,
                android.R.attr.state_focused,
                android.R.attr.state_window_focused,
                android.R.attr.state_pressed
        });
        Resources r = context.getResources();

        setBackgroundColor(r.getColor(R.color.grey_100));

        mColorNormal = r.getColor(R.color.grey_600);
        mColorOther = r.getColor(R.color.grey_600);
        mVerticalPadding = r.getDimensionPixelSize(R.dimen.candidate_vertical_padding);

        mPaint = new Paint();
        mPaint.setColor(mColorNormal);
        mPaint.setAntiAlias(true);
        mPaint.setTextSize(r.getDimensionPixelSize(R.dimen.candidate_font_height));
        mPaint.setStrokeWidth(0);

        mGestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2,
                                    float distanceX, float distanceY) {
                mScrolled = true;
                int sx = getScrollX();
                sx += distanceX;
                if (sx < 0) {
                    sx = 0;
                }
                if (sx + getWidth() > mTotalWidth) {
                    sx -= distanceX;
                }
                mTargetScrollX = sx;
                scrollTo(sx, getScrollY());
                invalidate();
                return true;
            }
        });
        setHorizontalFadingEdgeEnabled(true);
        setWillNotDraw(false);
        setHorizontalScrollBarEnabled(false);
        setVerticalScrollBarEnabled(false);
    }

    /**
     * A connection back to the service to communicate with the text field
     * @param listener
     */
    public void setService(KeyboardService listener) {
        mService = listener;
    }

    @Override
    public int computeHorizontalScrollRange() {
        return mTotalWidth;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int measuredWidth = resolveSize(50, widthMeasureSpec);

        // Get the desired height of the icon menu view (last row of items does
        // not have a divider below)
        mSelectionHighlight.getPadding(padding);
        final int desiredHeight = ((int)mPaint.getTextSize()) + mVerticalPadding
                + padding.top + padding.bottom;

        // Maximum possible width and desired height
        setMeasuredDimension(measuredWidth,
                resolveSize(desiredHeight, heightMeasureSpec));
    }
    /**
     * If the canvas is null, then only touch calculations are performed to pick the target
     * candidate.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        if (canvas != null) {
            super.onDraw(canvas);
        }
        mTotalWidth = 0;
        if (mSuggestions == null) return;

        if (mBgPadding == null) {
            mBgPadding = new Rect(0, 0, 0, 0);
            if (getBackground() != null) {
                getBackground().getPadding(mBgPadding);
            }
        }
        int x = 0;
        final int count = mSuggestions.size();
        final int height = getHeight();
        final Rect bgPadding = mBgPadding;
        final Paint paint = mPaint;
        final int touchX = mTouchX;
        final int scrollX = getScrollX();
        final boolean scrolled = mScrolled;
        final int y = (int) (((height - mPaint.getTextSize()) / 2) - mPaint.ascent());
        for (int i = 0; i < count; i++) {
            String suggestion = mSuggestions.get(i);
            float textWidth = paint.measureText(suggestion);
            final int wordWidth = (int) textWidth + X_GAP * 2;
            paint.setColor(mColorNormal);
            if (touchX + scrollX >= x && touchX + scrollX < x + wordWidth && !scrolled) {
                if (canvas != null) {
                    canvas.translate(x, 0);
                    mSelectionHighlight.setBounds(0, bgPadding.top, wordWidth, height);
                    mSelectionHighlight.draw(canvas);
                    canvas.translate(-x, 0);
                }
                mSelectedIndex = i;
            }
            if (canvas != null) {
                paint.setColor(mColorOther);
                canvas.drawText(suggestion, x + X_GAP, y, paint);
                paint.setColor(mColorOther);
                canvas.drawLine(x + wordWidth + 0.5f, bgPadding.top,
                        x + wordWidth + 2.5f, height + 1, paint);
            }

            if (i == 0) {
                x += 3f;
            }

            x += wordWidth;
        }
        mTotalWidth = x;
        if (mTargetScrollX != getScrollX()) {
            scrollToTarget();
        }
    }

    private void scrollToTarget() {
        int sx = getScrollX();
        if (mTargetScrollX > sx) {
            sx += SCROLL_PIXELS;
            if (sx >= mTargetScrollX) {
                sx = mTargetScrollX;
                requestLayout();
            }
        } else {
            sx -= SCROLL_PIXELS;
            if (sx <= mTargetScrollX) {
                sx = mTargetScrollX;
                requestLayout();
            }
        }
        scrollTo(sx, getScrollY());
        invalidate();
    }

    public void setSuggestions(List<String> suggestions) {
        clear();
        if (suggestions != null) {
            mSuggestions = new ArrayList<>(suggestions);
        }
        scrollTo(0, 0);
        mTargetScrollX = 0;
        // Compute the total width
        onDraw(null);
        invalidate();
        requestLayout();
    }
    public void clear() {
        mSuggestions = EMPTY_LIST;
        mTouchX = OUT_OF_BOUNDS;
        mSelectedIndex = -1;
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent me) {
        if (mGestureDetector.onTouchEvent(me)) {
            return true;
        }
        int action = me.getAction();
        int x = (int) me.getX();
        int y = (int) me.getY();
        mTouchX = x;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mScrolled = false;
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                if (y <= 0) {
                    // Fling up!?
                    if (mSelectedIndex >= 0) {
                        mService.pickSuggestionManually(mSuggestions.get(mSelectedIndex));
                        mSelectedIndex = -1;
                    }
                }
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                if (!mScrolled) {
                    if (mSelectedIndex >= 0) {
                        mService.pickSuggestionManually(mSuggestions.get(mSelectedIndex));
                    }
                }
                mSelectedIndex = -1;
                removeHighlight();
                requestLayout();
                break;
        }
        return true;
    }

    /**
     * For flick through from keyboard, call this method with the x coordinate of the flick
     * gesture.
     * @param x
     */
    public void takeSuggestionAt(float x) {
        mTouchX = (int) x;
        // To detect candidate
        onDraw(null);
        if (mSelectedIndex >= 0) {
            mService.pickSuggestionManually(mSuggestions.get(mSelectedIndex));
        }
        invalidate();
    }
    private void removeHighlight() {
        mTouchX = OUT_OF_BOUNDS;
        invalidate();
    }
}