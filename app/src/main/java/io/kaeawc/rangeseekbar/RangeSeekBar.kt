package io.kaeawc.rangeseekbar

import android.content.Context
import android.graphics.*
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.SeekBar

class RangeSeekBar : SeekBar {

    companion object {
        const val DEFAULT_MIN = 0
        const val DEFAULT_MAX = 100
        const val DEFAULT_LEFT = 25
        const val DEFAULT_RIGHT = 75
        const val DEFAULT_LABEL_SIZE = 16f
        const val DEFAULT_THUMB_RADIUS = 8f
        const val DEFAULT_THUMB_HIT_RADIUS = 15f
    }

    var minimum: Int
    var maximum: Int
    var leftValue: Int
    var rightValue: Int
    var thumbRadius: Float
    var thumbHitRadius: Float
    val blackPaint = Paint()
    val accentPaint = Paint()

    var activePointerId = 0
    var pointerIndex = 0
    var currentThumb = 0
    var downMotionX = 0f

    var leftLabel: String = ""
    var rightLabel: String = ""

    val density: Float = resources.displayMetrics.density

    val track = Rect(0, 0, 0, 0)
    val activeTrack = Rect(0, 0, 0, 0)

    private var listener: OnRangeSeekBarChangedListener? = null

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.RangeSeekBar)
        minimum = typedArray.getInteger(R.styleable.RangeSeekBar_minimum, DEFAULT_MIN)
        maximum = typedArray.getInteger(R.styleable.RangeSeekBar_maximum, DEFAULT_MAX)
        leftValue = typedArray.getInteger(R.styleable.RangeSeekBar_leftValue, DEFAULT_LEFT)
        rightValue = typedArray.getInteger(R.styleable.RangeSeekBar_rightValue, DEFAULT_RIGHT)
        thumbRadius = typedArray.getFloat(R.styleable.RangeSeekBar_thumbRadius, DEFAULT_THUMB_RADIUS)
        thumbHitRadius = typedArray.getFloat(R.styleable.RangeSeekBar_thumbHitRadius, DEFAULT_THUMB_HIT_RADIUS)
        typedArray.recycle()

        if (minimum > maximum)
            minimum = Math.min(0, maximum)

        if (leftValue < minimum)
            leftValue = minimum

        if (rightValue > maximum)
            rightValue = maximum

        if (rightValue < leftValue)
            rightValue = leftValue

        blackPaint.color = Color.BLACK
        blackPaint.textSize = density * DEFAULT_LABEL_SIZE
        accentPaint.color = ContextCompat.getColor(context, R.color.colorAccent)
    }

    fun drawableWidth(): Float = width - undrawableWidth()

    fun undrawableWidth(): Float = drawableLeft() + drawableRight()

    fun drawableLeft(): Float {
        return (paddingLeft + (layoutParams as ViewGroup.MarginLayoutParams).leftMargin).toFloat()
    }

    fun drawableRight(): Float {
        return (paddingRight + (layoutParams as ViewGroup.MarginLayoutParams).rightMargin).toFloat()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val barTop = (seekBarY() - (2 * density)).toInt()
        val barBottom = (seekBarY() + (2 * density)).toInt()
        val lx = leftXPosition().toInt()
        val rx = rightXPosition().toInt()

        track.left = drawableLeft().toInt()
        track.top = barTop
        track.right = drawableWidth().toInt() + drawableLeft().toInt()
        track.bottom = barBottom

        activeTrack.left = lx
        activeTrack.top = barTop
        activeTrack.right = rx
        activeTrack.bottom = barBottom

        val leftText = readLeftLabel()
        drawLabel(canvas, leftText, lx, thumbRadius() - blackPaint.measureText(leftText))
        drawLabel(canvas, readRightLabel(), rx, -thumbRadius())
        drawTrack(canvas)
        drawThumb(canvas, lx,  0f)
        drawThumb(canvas, rx, 0f)
    }

    fun readLeftLabel(): String {
        return when (leftLabel.isNullOrEmpty()) {
            true -> leftValue.toString()
            false -> leftLabel
        }
    }

    fun readRightLabel(): String {
        return when (rightLabel.isNullOrEmpty()) {
            true -> rightValue.toString()
            false -> rightLabel
        }
    }

    fun drawTrack(canvas: Canvas) {
        canvas.drawRect(track, blackPaint)
        canvas.drawRect(activeTrack, accentPaint)
    }

    fun drawLabel(canvas: Canvas, label: String, position: Int, offset: Float) {
        canvas.drawText(label, position + offset, seekBarY() / 2, blackPaint)
    }

    fun relativeMax(): Float = (maximum - minimum).toFloat()

    fun relativeLeftValue(): Float = (leftValue - minimum) / relativeMax()

    fun relativeRightValue(): Float = (rightValue - minimum) / relativeMax()

    fun leftMinCenter() = thumbRadius() + drawableLeft()
    fun leftMaxCenter() = (drawableLeft() + drawableWidth()) - (3 * thumbRadius())
    fun rightMinCenter() = (3 * thumbRadius()) + drawableLeft()
    fun rightMaxCenter() = (drawableLeft() + drawableWidth()) - (thumbRadius())

    fun leftXPosition(): Float {
        val min:Float = leftMinCenter()
        val max:Float = leftMaxCenter()
        return ((relativeLeftValue() * (max - min)) + min)
    }

    fun rightXPosition(): Float {
        val min:Float = rightMinCenter()
        val max:Float = rightMaxCenter()
        return ((relativeRightValue() * (max - min)) + min)
    }

    fun seekBarY(): Float = paddingTop.toFloat()

    fun thumbRadius(): Float = thumbRadius * density

    fun thumbHitRadius(): Float = thumbHitRadius * density

    fun drawThumb(canvas: Canvas, positionX: Int, offset: Float) {
        canvas.drawCircle(positionX + offset, seekBarY(), thumbRadius(), blackPaint)
    }

    fun thumbHit(touchX: Float): Int {
        val leftX = leftXPosition()
        val rightX = rightXPosition()
        val hitRadius = thumbHitRadius()

        return when {
            leftX - (hitRadius * 2) < touchX && touchX < leftX + (hitRadius * 2) -> 1
            rightX - (hitRadius * 2) < touchX && touchX < rightX + (hitRadius * 2) -> 2
            else -> 0
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {

        if (!isEnabled) {
            return false
        }

        val action = event.action
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                activePointerId = event.getPointerId(event.pointerCount - 1)
                pointerIndex = event.findPointerIndex(activePointerId)
                downMotionX = event.getX(pointerIndex)
                currentThumb = thumbHit(downMotionX)

                if (currentThumb == 0) {
                    return super.onTouchEvent(event)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val cx = event.getX(pointerIndex)
                when (currentThumb) {
                    1 -> {
                        val cmin = leftMinCenter()
                        val cmax = leftMaxCenter()
                        val rv = (cx - cmin) / (cmax - cmin)
                        val value = ((rv * (maximum - minimum)) + minimum).toInt()
                        if (value > rightValue) {
                            leftValue = rightValue
                        } else if (value < minimum) {
                            leftValue = minimum
                        } else {
                            leftValue = value
                        }
                    }
                    2 -> {
                        val cmin = rightMinCenter()
                        val cmax = rightMaxCenter()
                        val rv = (cx - cmin) / (cmax - cmin)
                        val value = ((rv * (maximum - minimum)) + minimum).toInt()
                        if (value < leftValue) {
                            rightValue = leftValue
                        } else if (value > maximum) {
                            rightValue = maximum
                        } else {
                            rightValue = value
                        }
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                currentThumb = 0
                onChange()
            }
            else -> currentThumb = 0
        }

        return super.onTouchEvent(event)
    }

    fun onChange() {
        val max = relativeMax()
        val left = (leftValue - minimum) / max
        val right = (rightValue - minimum) / max
        listener?.onRangeChanged(this, left, right)
    }

    fun setOnRangeSeekBarChangeListener(listener: OnRangeSeekBarChangedListener) {
        this.listener = listener
    }

    interface OnRangeSeekBarChangedListener {

        fun onRangeChanged(bar: RangeSeekBar, left: Float, right: Float)

    }
}
