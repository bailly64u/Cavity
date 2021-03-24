package com.louis.app.cavity.ui.home.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.View.MeasureSpec.*
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import androidx.core.graphics.toRectF
import com.google.android.material.card.MaterialCardView
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.shape.ShapeAppearancePathProvider
import com.louis.app.cavity.R
import kotlin.math.round

/**
 * A CardView that forces its shape to be a perfect hexagone. Thus, you can only control width or
 * height (but never both at the same time) of the view depending on the flat attribute.
 * Doesn't support padding for now, since it could break the perfect hexagonal shape.
 */
class HexagonalView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.materialCardViewStyle,
    defStyleRes: Int = R.style.Widget_MaterialComponents_CardView
) :
    MaterialCardView(context, attrs, defStyleAttr) {

    companion object {
        private const val HEXAGONAL_SQUARE_RATIO = 0.866
    }

    private val path = Path()
    private val clipPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(context, android.R.color.white)
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
        }
    }

    private val linePaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ContextCompat.getColor(context, R.color.cavity_red)
            style = Paint.Style.STROKE
            strokeWidth = 10f
        }
    }

    private var isFlat = false

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.HexagonalView,
            defStyleAttr,
            defStyleRes
        )
            .use {
                isFlat = it.getBoolean(R.styleable.HexagonalView_flat, false)
            }

        setLayerType(View.LAYER_TYPE_HARDWARE, null)
        applyShape()
    }

    private fun applyShape() {
        val topRightBottomLeftCorners = HexagonalCornerTreatment(!isFlat)
        val topLeftBottomRightCorners = HexagonalCornerTreatment(isFlat)

        shapeAppearanceModel = ShapeAppearanceModel.builder()
            .setAllCornerSizes { if (isFlat) it.height() / 2 else it.width() / 2 }
            .setTopLeftCorner(topLeftBottomRightCorners)
            .setTopRightCorner(topRightBottomLeftCorners)
            .setBottomRightCorner(topLeftBottomRightCorners)
            .setBottomLeftCorner(topRightBottomLeftCorners)
            .build()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        val r = Rect(0, 0, w, h).toRectF()
        ShapeAppearancePathProvider().calculatePath(shapeAppearanceModel, 1f, r, path)

        // Reverse the given path to get correct clipping out of it
        path.fillType = Path.FillType.INVERSE_WINDING
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var widthSpec = widthMeasureSpec
        var heightSpec = heightMeasureSpec

        if (isFlat) {
            // Force our width based on height to get perfect hexagonal shape
            widthSpec = makeMeasureSpec(
                round(getSize(heightMeasureSpec) / HEXAGONAL_SQUARE_RATIO).toInt(),
                EXACTLY
            )
        } else {
            // Force our height based on width to get perfect hexagonal shape
            heightSpec = makeMeasureSpec(
                round(getSize(widthMeasureSpec) / HEXAGONAL_SQUARE_RATIO).toInt(),
                EXACTLY
            )
        }

        super.onMeasure(widthSpec, heightSpec)
    }

    override fun dispatchDraw(canvas: Canvas) {
        val saveCount = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)

        super.dispatchDraw(canvas)

        canvas.drawLine(width/2f, height.toFloat(), 0f, height * 0.75f, linePaint)
        canvas.drawPath(path, clipPaint)
        canvas.restoreToCount(saveCount)
    }

    // Yeah, this is an ugly fix. Too bad !
    // We need to eat requestLayout calls to avoid our HoneycombLayoutManager#OnLayoutChildren
    // to be called when we load an image into this view.
    // Erasing this will cause some RecyclerView madness to happen.
    @SuppressLint("MissingSuperCall")
    override fun requestLayout() {
    }
}
