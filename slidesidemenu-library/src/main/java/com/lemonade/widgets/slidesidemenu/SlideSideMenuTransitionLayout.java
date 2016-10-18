package com.lemonade.widgets.slidesidemenu;

import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

/**
 * A view group layout to use in the xml. There should be 2 children:
 *   1) The Activity Menu container element.
 *   2) The Activity Content container element.
 * Wire up as needed:
 *   - Close the menu when back is pressed (if is currently open)
 *   - Toggle when user clicks on the menu button in the action bar
 * Please see sample activity for examples.
 * Created by eyalbiran on 2/23/16.
 */
public class SlideSideMenuTransitionLayout extends FrameLayout {

    public static final float SWIPE_DISTANCE_FACTOR = 1.2f;

    private static final float DEFAULT_CONTENT_PEEK_DISTANCE_PERCENT = 0.4f;
    private static final float DEFAULT_CONTENT_PEEK_SIZE_PERCENT = 0.85f;
    private static final float DEFAULT_MENU_START_SIZE_PERCENT = 1.1f;
    private static final long  DEFAULT_ANIMATION_DURATION = 300;

    private static final long  TOUCH_TAP_DURATION_MAX = 150;
    private static final int   TOUCH_TAP_DISTANCE_MAX_DP = 13;
    private static final int   TOUCH_SWIPE_DISTANCE_MIN = 10;
    private static final int   TOUCH_SWIPE_DISTANCE_INVALID_MAX_DP = 17;
    private static final int   TOUCH_AREA_SIZE = 33;
    private static final long  TOUCH_FLING_MAX_DURATION = 200;
    private static final int TOUCH_FLING_MIN_DISTANCE = 33;

    private static final TimeInterpolator DECELERATE = new DecelerateInterpolator();
    private static final TimeInterpolator ACCELERATE_DECELERATE = new AccelerateDecelerateInterpolator();

    private View mMenuContainer;
    private View mContentContainer;

    private boolean mIsLocked;

    // Animation Settings
    private float mAnimationDuration = DEFAULT_ANIMATION_DURATION;
    private float mContentContainerPeekPercent = DEFAULT_CONTENT_PEEK_DISTANCE_PERCENT;
    private Integer mContentContainerPeekWidth;
    private float mContentContainerPeekSizePercent = DEFAULT_CONTENT_PEEK_SIZE_PERCENT;
    private float mMenuContainerStartSizePercent = DEFAULT_MENU_START_SIZE_PERCENT;

    // Touch Settings
    private long  mTouchTapDurationMax = TOUCH_TAP_DURATION_MAX;
    private int   mTouchTapDistanceMax;
    private int   mTouchSwipeDistanceMin;
    private int   mTouchSwipeDistanceInvalidMax;
    private int   mTouchAreaSize;
    private long  mTouchFlingMaxDuration = TOUCH_FLING_MAX_DURATION;
    private int   mTouchFlingMinDistance;

    // Cached values for animation
    private int mContentContainerTranslationX;
    private float mContentContainerScaleDiff;
    private float mMenuContainerScaleDiff;

    private SlideSideMenuStateListener mSideMenuStateListener;

    private boolean mSideMenuOpen;
    private float mSideMenuAnimation;
    private ObjectAnimator mAnimation;

    private float mTouchDownX;
    private float mTouchDownY;
    private long mTouchDownTimestamp;
    private boolean mTouchSwipeValid;
    private boolean mTouchSwipeActive;
    private boolean mTouchFlingActive;

    private int[] mHelpArrayInt2 = new int[2];

    public interface SlideSideMenuStateListener {

        void onSideMenuOpened();

        void onSideMenuClosed();

        void onSideMenuFirstReveal();
    }

    public interface SlideSideMenuUpdateListener {
        /**
         * This will be called whenever the slide factor changes
         * @param factor the current menu factor, where the menu is fully close at 0 and fully opened at 1
         */
        void onSlideSideMenuFactorUpdate(float factor);
    }

    public SlideSideMenuTransitionLayout(Context context) {
        super(context);
        init(context);
    }

    public SlideSideMenuTransitionLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SlideSideMenuTransitionLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mTouchTapDistanceMax = Utils.convertDPtoPixels(context, TOUCH_TAP_DISTANCE_MAX_DP);
        mTouchSwipeDistanceMin = Utils.convertDPtoPixels(context, TOUCH_SWIPE_DISTANCE_MIN);
        mTouchSwipeDistanceInvalidMax = Utils.convertDPtoPixels(context, TOUCH_SWIPE_DISTANCE_INVALID_MAX_DP);
        mTouchAreaSize = Utils.convertDPtoPixels(context, TOUCH_AREA_SIZE);
        mTouchFlingMinDistance = Utils.convertDPtoPixels(context, TOUCH_FLING_MIN_DISTANCE);
    }

    private void calculateValues(int width) {

        // Calculate Content Translation X
        if (mContentContainerPeekWidth != null) {
            // Based on fixed target width
            mContentContainerTranslationX = width - mContentContainerPeekWidth;
        } else {
            // Based on percentage of the total width
            mContentContainerTranslationX = (int) (width - (width * mContentContainerPeekPercent));
        }

        // Calculate Content Target Scale
        mContentContainerScaleDiff = 1 - mContentContainerPeekSizePercent;

        // Calculate Side Menu Start Scale
        mMenuContainerScaleDiff = mMenuContainerStartSizePercent - 1;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        int count = getChildCount();
        if (count == 0) {
            // We don't have any children, it's ok - user needs to add manually
            return;
        }

        if (count != 2) {
            throw new RuntimeException("Must contain two children: 1) Menu 2) Content");
        }

        mMenuContainer = getChildAt(0);
        mContentContainer = getChildAt(1);

        syncContentContainerState();
    }

    /**
     * Locks the side menu
     * @param isLocked true if the user may not open/close the side menu. false otherwise
     */
    public void setLocked(boolean isLocked) {
        mIsLocked = isLocked;
    }

    private void syncContentContainerState() {
        if (mSideMenuAnimation == 0) {
            mMenuContainer.setVisibility(GONE);
        } else {
            mMenuContainer.setVisibility(VISIBLE);
        }

        if (mContentContainer instanceof SlideSideMenuUpdateListener) {
            ((SlideSideMenuUpdateListener) mContentContainer).onSlideSideMenuFactorUpdate(mSideMenuAnimation);
        }
        if (mMenuContainer instanceof SlideSideMenuUpdateListener) {
            ((SlideSideMenuUpdateListener) mMenuContainer).onSlideSideMenuFactorUpdate(mSideMenuAnimation);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        calculateValues(w);
    }

    public void setContentPeekDistanceWidthResource(int widthRes) {
        setContentPeekDistanceWidth(getResources().getDimensionPixelSize(widthRes));
    }

    public void setContentPeekDistanceWidth(Integer width) {
        mContentContainerPeekWidth = width;
        calculateValues(getWidth());
    }

    public void setContentPeekDistancePercent(float percent) {
        mContentContainerPeekWidth = null;
        mContentContainerPeekPercent = percent;
        calculateValues(getWidth());
    }

    public void setContentPeekSizePercent(float percent) {
        mContentContainerPeekSizePercent = percent;
        calculateValues(getWidth());
    }

    public void setMenuSizePercent(float percent) {
        mMenuContainerStartSizePercent = percent;
        calculateValues(getWidth());
    }

    public void setMenuLayout(View view) {
        if (mMenuContainer != null) {
            removeView(mMenuContainer);
            mMenuContainer = null;
        }

        // Add it as the background view
        addView(view, 0);
        mMenuContainer = view;
    }

    public void setContentLayout(View view) {
        if (mContentContainer != null) {
            removeView(mContentContainer);
            mContentContainer = null;
        }

        // Add it as the foreground view
        addView(view, getChildCount());
        mContentContainer = view;
    }

    public void setSideMenuStateListener(SlideSideMenuStateListener listener) {
        mSideMenuStateListener = listener;
    }

    public void setAnimationDuration(long duration) {
        mAnimationDuration = duration;
    }

    public void toggle() {
        if (mSideMenuOpen) {
            closeSideMenu();
        } else {
            openSideMenu();
        }
    }

    public boolean openSideMenu() {
        if (mSideMenuOpen) {
            // we are already open
            // We are not open
            if (mSideMenuAnimation < 1 && mAnimation == null) {
                openSideMenuAnimate();
            }
            return false;
        }

        if (mIsLocked) {
            return false;
        }

        if (mAnimation != null) {
            mAnimation.cancel();
            mAnimation = null;
        }

        mSideMenuOpen = true;
        openSideMenuAnimate();
        return true;
    }

    private void openSideMenuAnimate() {
        mAnimation = ObjectAnimator.ofFloat(this, "sideMenuAnimation", mSideMenuAnimation, 1);
        mAnimation.setInterpolator(mTouchFlingActive ? DECELERATE : ACCELERATE_DECELERATE);
        mAnimation.setDuration((long) ((1 - mSideMenuAnimation) * mAnimationDuration));
        mAnimation.start();

        if (mSideMenuStateListener != null) {
            mSideMenuStateListener.onSideMenuOpened();
        }
    }

    public boolean closeSideMenu() {
        if (!mSideMenuOpen) {
            // We are not open
            if (mSideMenuAnimation > 0 && mAnimation == null) {
                closeSideMenuAnimate();
            }
            return false;
        }

        if (mIsLocked) {
            return false;
        }

        if (mAnimation != null) {
            mAnimation.cancel();
            mAnimation = null;
        }

        mSideMenuOpen = false;

        closeSideMenuAnimate();
        return true;
    }

    private void closeSideMenuAnimate() {
        mAnimation = ObjectAnimator.ofFloat(this, "sideMenuAnimation", mSideMenuAnimation, 0);
        mAnimation.setInterpolator(mTouchFlingActive ? DECELERATE : ACCELERATE_DECELERATE);
        mAnimation.setDuration((long) ((mSideMenuAnimation) * mAnimationDuration));
        mAnimation.start();

        if (mSideMenuStateListener != null) {
            mSideMenuStateListener.onSideMenuClosed();
        }
    }

    /**
     * Help function for animation. Used to place current state between (includes) open and closed states.
     * @param factor a value between 0 and 1, where the menu is fully closed at 0 and fully opened at 1.
     */
    public void setSideMenuAnimation(float factor) {
        if (factor < 0) {
            factor = 0;
        } else if (factor > 1) {
            factor = 1;
        }

        if (mSideMenuAnimation == 0 && factor > 0) {
            // Just starting to show
            if (mSideMenuStateListener != null) {
                mSideMenuStateListener.onSideMenuFirstReveal();
            }
        }

        mSideMenuAnimation = factor;

        // Animate Content
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mContentContainer.setPivotX(0);
        }
        mContentContainer.setTranslationX(mContentContainerTranslationX * factor);

        float contentScale = 1 - (mContentContainerScaleDiff * factor);
        mContentContainer.setScaleY(contentScale);
        mContentContainer.setScaleX(contentScale);

        // Animate Side Menu
        float menuScale = 1 + (mMenuContainerScaleDiff * (1-factor));
        mMenuContainer.setScaleY(menuScale);
        mMenuContainer.setScaleX(menuScale);

        syncContentContainerState();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {

        if (mIsLocked) {
            return super.onInterceptTouchEvent(ev);
        }

        // We want to intercept when:
        //    1) Menu is open and user click on the content view (close menu)
        //    2) Swipe left/right to close/open the menu
        boolean shouldIntercept = false;

        float x = ev.getRawX();
        float y = ev.getRawY();

        int actionMasked = ev.getActionMasked();
        switch (actionMasked) {
            case MotionEvent.ACTION_DOWN:
                mTouchDownTimestamp = System.currentTimeMillis();
                mTouchDownX = x;
                mTouchDownY = y;
                mTouchSwipeValid = (mSideMenuOpen && isViewContains(mContentContainer, (int)x, (int)y, mHelpArrayInt2))
                                        || mTouchDownX <= mTouchAreaSize;
                mTouchSwipeActive = false;
                mTouchFlingActive = false;
                shouldIntercept = mSideMenuOpen && inContentContainer(x, y);
                break;
            case MotionEvent.ACTION_MOVE:
                if (!mTouchSwipeValid) {
                    break;
                }

                if (Math.abs(y - mTouchDownY) > mTouchSwipeDistanceInvalidMax) {
                    mTouchSwipeValid = false;
                } else {
                    shouldIntercept = (Math.abs(x - mTouchDownX) > mTouchSwipeDistanceMin);
                }

                break;
        }

        return shouldIntercept || super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (mIsLocked) {
            return super.onTouchEvent(event);
        }

        float x = event.getRawX();
        float y = event.getRawY();

        int actionMasked = event.getActionMasked();
        switch (actionMasked) {
            case MotionEvent.ACTION_DOWN:
                mTouchDownTimestamp = System.currentTimeMillis();
                mTouchDownX = x;
                mTouchDownY = y;
                break;

            case MotionEvent.ACTION_MOVE:
                if (!mTouchSwipeActive) {
                    if (!mTouchSwipeValid) {
                        break;
                    }

                    if (Math.abs(y - mTouchDownY) > mTouchSwipeDistanceInvalidMax) {
                        mTouchSwipeValid = false;
                        break;
                    } else {
                        mTouchSwipeActive = Math.abs(x - mTouchDownX) > mTouchSwipeDistanceMin;
                    }
                }

                if (mTouchSwipeActive) {
                    setSideMenuAnimation(getFactorForTouch(x));

                }

                break;

            case MotionEvent.ACTION_UP:
                long currentTimestamp = System.currentTimeMillis();

                // Support fling
                if (currentTimestamp - mTouchDownTimestamp < mTouchFlingMaxDuration) {

                    if (mSideMenuOpen) {
                        // Fling right?
                        if (mTouchDownX - x > mTouchFlingMinDistance) {
                            mTouchFlingActive = true;
                            closeSideMenu();
                            break;
                        }
                    } else {
                        // Fling left?
                        if (x - mTouchDownX > mTouchFlingMinDistance) {
                            mTouchFlingActive = true;
                            openSideMenu();
                            break;
                        }
                    }

                }

                // Support swipe
                if (mTouchSwipeActive) {
                    // We were swiping
                    if (getFactorForTouch(x) < 0.5f) {
                        if (mSideMenuOpen) {
                            closeSideMenu();
                        } else {
                            closeSideMenuAnimate();
                        }
                    } else {
                        if (!mSideMenuOpen) {
                            openSideMenu();
                        } else {
                            openSideMenuAnimate();
                        }

                    }
                    break;
                }

                if ( (currentTimestamp - mTouchDownTimestamp) < mTouchTapDurationMax &&
                     Math.abs(mTouchDownX - x) < mTouchTapDistanceMax                &&
                     Math.abs(mTouchDownY - y) < mTouchTapDistanceMax) {
                    // Touch event is a TAP!
                    if (mSideMenuOpen && inContentContainer(x,y)) {
                        // Tap in the content area while we are open, close!
                        closeSideMenu();
                        break;
                    }
                }
                break;
        }

        // Do we always want to catch the event?
        return true;
    }

    private float getFactorForTouch(float x) {
        int viewWidth = getWidth();
        float swipeDistance = (mSideMenuOpen ? mTouchDownX - x : x - mTouchDownX) * SWIPE_DISTANCE_FACTOR;
        float factor = (swipeDistance / viewWidth);
        return mSideMenuOpen ? 1 - factor : factor;
    }

    private boolean inContentContainer(float x, float y) {
        return isViewContains(mContentContainer, (int)x, (int)y, mHelpArrayInt2);
    }

    private static boolean isViewContains(View view, int rx, int ry, int[] helpArray) {
        if (view == null) {
            return false;
        }

        view.getLocationOnScreen(helpArray);

        int x = helpArray[0];
        int y = helpArray[1];
        int w = view.getWidth();
        int h = view.getHeight();

        return !(rx < x || rx > x + w || ry < y || ry > y + h);
    }
}
