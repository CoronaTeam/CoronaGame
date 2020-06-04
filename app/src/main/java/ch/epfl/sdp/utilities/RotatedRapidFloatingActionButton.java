package ch.epfl.sdp.utilities;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.animation.OvershootInterpolator;

import com.wangjie.rapidfloatingactionbutton.RapidFloatingActionButton;

public class RotatedRapidFloatingActionButton extends RapidFloatingActionButton {

    private float maxRotation = 0f;
    private ObjectAnimator mDrawableAnimator;
    private OvershootInterpolator mOvershootInterpolator;

    public RotatedRapidFloatingActionButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setMaxRotation(float maxRotation) {
        this.maxRotation = maxRotation;
    }

    @Override
    public void onExpandAnimator(AnimatorSet animatorSet) {
        ensureDrawableAnimator();
        ensureDrawableInterpolator();
        mDrawableAnimator.cancel();
        mDrawableAnimator.setTarget(getCenterDrawableIv());
        mDrawableAnimator.setFloatValues(0, -maxRotation);
        mDrawableAnimator.setPropertyName("rotation");
        mDrawableAnimator.setInterpolator(mOvershootInterpolator);
        animatorSet.playTogether(mDrawableAnimator);
    }

    @Override
    public void onCollapseAnimator(AnimatorSet animatorSet) {
        ensureDrawableAnimator();
        ensureDrawableInterpolator();
        mDrawableAnimator.cancel();
        mDrawableAnimator.setTarget(getCenterDrawableIv());
        mDrawableAnimator.setFloatValues(-maxRotation, 0);
        mDrawableAnimator.setPropertyName("rotation");
        mDrawableAnimator.setInterpolator(mOvershootInterpolator);
        animatorSet.playTogether(mDrawableAnimator);
    }

    private void ensureDrawableAnimator() {
        if (null == mDrawableAnimator) {
            mDrawableAnimator = new ObjectAnimator();
        }
    }

    private void ensureDrawableInterpolator() {
        if (null == mOvershootInterpolator) {
            mOvershootInterpolator = new OvershootInterpolator();
        }
    }

}
