package com.example.stripeprogressbardemo;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class StripeProgressBar extends View {

    private static final int MAX_PROGRESS = 100;
    private static final int DEFAULT_BAR_STRIPE_TRANSLATION_TIME_UNIT = 300;

    private final int BACKGROUND_COLOR;
    private final int BAR_COLOR;
    private final float BAR_RECT_RADIUS;
    private final float PADDING_PX;
    private final int BAR_STRIPE_COLOR;
    private final float BAR_STRIPE_WIDTH_PX;
    private final float BAR_STRIPE_INTERVAL_WIDTH_PX;
    private final float BAR_STRIPE_ROTATE_DEGREE;
    private final float BAR_STRIPE_TRANSLATION_OFFSET_PX;
    private final int BAR_STRIPE_TRANSLATION_TIME_UNIT;

    private Paint mBarStripesPaint;
    private Paint mBarRectBitmapPaint;
    private Paint mBarPaint;
    private Paint mBackgroundRoundRectPaint;

    private Path mBarStripesPath;
    private Path mBarRectMaskPath;
    private Path mBarRoundRectMaskPath;

    private Bitmap mBarRectBitmap;
    private Canvas mProgressBarRectCanvas;
    private Matrix mBarPaintShaderMatrix;

    private ValueAnimator mBarStripesValueAnimator;
    private float mBarRectLeftPosition;

    private int mWidth;
    private int mHeight;
    private float mBarWidth;
    private float mBarHeight;
    private float mBarRectLength;

    private int mProgress = 0;

    public StripeProgressBar(Context context) {
        this(context, null, 0);
    }

    public StripeProgressBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StripeProgressBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        BACKGROUND_COLOR = getResources().getColor(R.color.progress_bar_background_color, context.getTheme());
        BAR_COLOR = getResources().getColor(R.color.progress_bar_color, context.getTheme());
        BAR_RECT_RADIUS = getResources().getDimensionPixelSize(R.dimen.progress_bar_rect_radius);
        PADDING_PX = getResources().getDimensionPixelSize(R.dimen.progress_bar_padding);
        BAR_STRIPE_COLOR = getResources().getColor(R.color.progress_bar_stripe_color, context.getTheme());
        BAR_STRIPE_WIDTH_PX = getResources().getDimensionPixelSize(R.dimen.progress_bar_stripe_width);
        BAR_STRIPE_INTERVAL_WIDTH_PX = getResources().getDimensionPixelSize(R.dimen.progress_bar_stripe_interval_width);
        BAR_STRIPE_ROTATE_DEGREE = getAngleBetween0And180(getResources().getDimensionPixelSize(R.dimen.progress_bar_stripe_rotate_degree));
        BAR_STRIPE_TRANSLATION_OFFSET_PX = getResources().getDimensionPixelSize(R.dimen.progress_bar_stripe_translation_offset);
        BAR_STRIPE_TRANSLATION_TIME_UNIT = DEFAULT_BAR_STRIPE_TRANSLATION_TIME_UNIT;

        initObjectAllocations();
        initBarStripesAnimator();
    }

    private void initObjectAllocations() {
        mBarStripesPaint = new Paint();
        mBarStripesPaint.setAntiAlias(true);
        mBarStripesPaint.setDither(true);
        mBarStripesPaint.setStyle(Paint.Style.FILL);
        mBarStripesPaint.setColor(BAR_STRIPE_COLOR);

        mBarRectBitmapPaint = new Paint();
        mBarRectBitmapPaint.setAntiAlias(true);
        mBarRectBitmapPaint.setDither(true);
        mBarRectBitmapPaint.setColor(BAR_COLOR);
        mBarRectBitmapPaint.setStyle(Paint.Style.FILL);
        mBarRectBitmapPaint.setFilterBitmap(true);

        mBarPaint = new Paint();
        mBarPaint.setAntiAlias(true);
        mBarPaint.setDither(true);

        mBackgroundRoundRectPaint = new Paint();
        mBackgroundRoundRectPaint.setAntiAlias(true);
        mBackgroundRoundRectPaint.setColor(BACKGROUND_COLOR);
        mBackgroundRoundRectPaint.setStyle(Paint.Style.FILL);

        mBarStripesPath = new Path();
        mBarRectMaskPath = new Path();
        mBarRoundRectMaskPath = new Path();

        mProgressBarRectCanvas = new Canvas();
        mBarPaintShaderMatrix = new Matrix();
    }

    private void initBarStripesAnimator() {
        // speed = BAR_STRIPE_TRANSLATION_OFFSET_PX / BAR_STRIPE_TRANSLATION_TIME_UNIT
        mBarStripesValueAnimator = ValueAnimator.ofFloat(0, BAR_STRIPE_TRANSLATION_OFFSET_PX);
        mBarStripesValueAnimator.setDuration(BAR_STRIPE_TRANSLATION_TIME_UNIT);
        mBarStripesValueAnimator.setRepeatMode(ValueAnimator.RESTART);
        mBarStripesValueAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mBarStripesValueAnimator.setInterpolator(new LinearInterpolator());
        mBarStripesValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (mBarRectLength == 0f) {
                    return;
                }
                if (mBarRectLeftPosition <= -(mBarRectLength / 2)) {
                    mBarRectLeftPosition = 0;
                } else {
                    mBarRectLeftPosition -= 1f;
                }
                invalidate();
            }
        });
        mBarStripesValueAnimator.start();
    }

    private void cancelBarStripesAnimator() {
        mBarStripesValueAnimator.cancel();
    }

    public void setProgress(int progress) {
        ThreadUtil.checkRunInMainThread("can not change ui in work thread.");

        progress = progress < 0 ? 0 : (Math.min(progress, MAX_PROGRESS));
        mProgress = progress;

        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mWidth = getMeasuredWidth();
        mHeight = getMeasuredHeight();
        mBarWidth = mWidth - PADDING_PX;
        mBarHeight = mHeight - PADDING_PX;

        generateBarStripesPath();
        generateProgressBarRectBitmap();

        mBarRoundRectMaskPath.addRoundRect(PADDING_PX, PADDING_PX, mBarWidth, mBarHeight,
                BAR_RECT_RADIUS, BAR_RECT_RADIUS, Path.Direction.CW);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRoundRect(0, 0, mWidth, mHeight, BAR_RECT_RADIUS, BAR_RECT_RADIUS, mBackgroundRoundRectPaint);

        mBarRectMaskPath.reset();
        mBarRectMaskPath.addRect(PADDING_PX, PADDING_PX, getProgressWidth(mBarWidth), mBarHeight, Path.Direction.CW);
        mBarRectMaskPath.op(mBarRoundRectMaskPath, Path.Op.INTERSECT);

        mBarPaint.getShader().setLocalMatrix(mBarPaintShaderMatrix);
        mBarPaintShaderMatrix.setTranslate(mBarRectLeftPosition, 0);
        canvas.drawPath(mBarRectMaskPath, mBarPaint);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        cancelBarStripesAnimator();
        if (mBarRectBitmap != null) {
            mBarRectBitmap.recycle();
        }
    }

    private float getAngleBetween0And180(float rawDegree) {
        float tempPositiveDegree = Math.abs(rawDegree);
        if (tempPositiveDegree >= 180f) {
            tempPositiveDegree = tempPositiveDegree - 180f * (int) (tempPositiveDegree / 180f);
        }
        if (tempPositiveDegree == 90f) {
            throw new IllegalArgumentException("bar stripe rotation degree cannot be 90");
        }
        return rawDegree < 0f ? (180f - tempPositiveDegree) : tempPositiveDegree;
    }

    private void generateBarStripesPath() {
        float normalLineRadian = getNormalLineAcuteAngleRadianWithSign();
        float oneStripeWidthOnXAxis = BAR_STRIPE_WIDTH_PX / (float) Math.cos(normalLineRadian);
        float oneStripeAndIntervalWidthOnXAxis = Math.abs(oneStripeWidthOnXAxis) + BAR_STRIPE_INTERVAL_WIDTH_PX;
        int barStripeCount = (int) Math.ceil(mBarWidth / oneStripeAndIntervalWidthOnXAxis) * 2; // * 2 for infinite animation
        mBarRectLength = oneStripeAndIntervalWidthOnXAxis * barStripeCount;

        // for fill in blank triangle space at both ends
        float leftBottomPointX = mBarHeight * (float) Math.tan(normalLineRadian);
        float leftBlankWidthOnXAxis = Math.abs(leftBottomPointX);
        int leftMoreStripesCount = 0;
        while (leftBlankWidthOnXAxis > BAR_STRIPE_INTERVAL_WIDTH_PX) {
            leftBlankWidthOnXAxis -= oneStripeAndIntervalWidthOnXAxis;
            leftMoreStripesCount++;
        }
        barStripeCount += (leftMoreStripesCount * 2); // add extra stripes for left and right

        float startXOffset = leftMoreStripesCount * oneStripeAndIntervalWidthOnXAxis;
        float leftTopX = 0f - startXOffset;
        float leftBottomX = leftBottomPointX - startXOffset;
        float rightBottomX = leftBottomPointX + oneStripeWidthOnXAxis - startXOffset;
        float rightTopX = oneStripeWidthOnXAxis - startXOffset;

        mBarStripesPath.reset();
        while (barStripeCount > 0) {
            mBarStripesPath.moveTo(leftTopX, 0f);
            mBarStripesPath.lineTo(leftBottomX, mBarHeight);
            mBarStripesPath.lineTo(rightBottomX, mBarHeight);
            mBarStripesPath.lineTo(rightTopX, 0f);
            mBarStripesPath.close();

            leftTopX += oneStripeAndIntervalWidthOnXAxis;
            leftBottomX += oneStripeAndIntervalWidthOnXAxis;
            rightBottomX += oneStripeAndIntervalWidthOnXAxis;
            rightTopX += oneStripeAndIntervalWidthOnXAxis;
            barStripeCount--;
        }
    }

    private float getNormalLineAcuteAngleRadianWithSign() {
        float normalLineAcuteAngle = BAR_STRIPE_ROTATE_DEGREE <= 90f ? -BAR_STRIPE_ROTATE_DEGREE : (180f - BAR_STRIPE_ROTATE_DEGREE);
        return (float) (Math.PI * normalLineAcuteAngle / 180);
    }

    private void generateProgressBarRectBitmap() {
        // make bar bitmap width longer than bar rect
        if (mBarRectBitmap != null) {
            mBarRectBitmap.recycle();
        }
        mBarRectBitmap = Bitmap.createBitmap((int) Math.ceil(mBarRectLength), (int) Math.ceil(mBarHeight),
                Bitmap.Config.ARGB_8888);
        mProgressBarRectCanvas.setBitmap(mBarRectBitmap);

        mProgressBarRectCanvas.drawRect(0f, 0f, mBarRectLength, mBarHeight, mBarRectBitmapPaint);
        mProgressBarRectCanvas.drawPath(mBarStripesPath, mBarStripesPaint);

        mBarPaint.setShader(new BitmapShader(mBarRectBitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT));
    }

    private float getProgressWidth(float wholeBarWidth) {
        return wholeBarWidth * mProgress / MAX_PROGRESS;
    }
}
