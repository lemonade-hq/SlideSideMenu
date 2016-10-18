package com.lemonade.widgets.slidesidemenu;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.view.ViewGroup;

/**
 * A content container for the SlideSideMenuTransitionLayout. When used as the root for the content,
 * this view will animate round corners and shadow when opening the side menu.
 * Created by eyalbiran on 2/24/16.
 */
public class SlideSideMenuContentCardView extends CardView implements SlideSideMenuTransitionLayout.SlideSideMenuUpdateListener {

    private static final int DEFAULT_MAX_ELEVATION = 7;
    private static final int DEFAULT_MAX_RADIUS = 4;

    private int mMaxElevation;
    private int mMaxRadius;
    private float mFactor = 0f;

    public SlideSideMenuContentCardView(Context context) {
        super(context);
        init(context);
    }

    public SlideSideMenuContentCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SlideSideMenuContentCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mMaxElevation = Utils.convertDPtoPixels(context, DEFAULT_MAX_ELEVATION);
        mMaxRadius = Utils.convertDPtoPixels(context, DEFAULT_MAX_RADIUS);
        // Prevent pre-L from adding inner card padding
        setPreventCornerOverlap(false);
        // Make Lollipop and above add shadow padding to match pre-L padding
        setUseCompatPadding(true);
    }

    public void setMaxElevation(int elevationPX) {
        mMaxElevation = elevationPX;
        applyFactor();
    }

    public void setMaxRadius(int radiusPX) {
        mMaxRadius = radiusPX;
        applyFactor();
    }

    @Override
    public void onSlideSideMenuFactorUpdate(float factor) {
        mFactor = factor;
        applyFactor();
    }

    private void applyFactor() {
        int cardElevation = Math.round(mMaxElevation * mFactor);
        setCardElevation(cardElevation);

        int radius = Math.round(mMaxRadius * mFactor);
        setRadius(radius);
    }

    @Override
    public void setLayoutParams(ViewGroup.LayoutParams params) {
        // FIX shadow padding: we don't want to have margin for the content view (saved for the card padding)
        // so we compensate the shadow size as a negative margin for the card. Works like a charm.
        if (params instanceof MarginLayoutParams) {
            MarginLayoutParams layoutParams = (MarginLayoutParams) params;
            layoutParams.bottomMargin -= (getPaddingBottom() - getContentPaddingBottom());
            layoutParams.leftMargin -= (getPaddingLeft() - getContentPaddingLeft());
            layoutParams.rightMargin -= (getPaddingRight() - getContentPaddingRight());
            layoutParams.topMargin -= (getPaddingTop() - getContentPaddingTop());
        }

        super.setLayoutParams(params);
    }
}
