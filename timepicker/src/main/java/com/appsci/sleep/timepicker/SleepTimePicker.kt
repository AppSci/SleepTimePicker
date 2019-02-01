package com.appsci.sleep.timepicker

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import com.appsci.sleep.timepicker.Utils.Companion.angleBetweenVectors
import com.appsci.sleep.timepicker.Utils.Companion.angleToMins
import com.appsci.sleep.timepicker.Utils.Companion.snapMinutes
import com.appsci.sleep.timepicker.Utils.Companion.to_0_720
import org.threeten.bp.LocalTime
import timber.log.Timber
import java.util.*
import kotlin.math.*


class SleepTimePicker @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    init {
        init(context, attrs)
    }

    private lateinit var progressPaint: Paint
    private lateinit var progressBackgroundPaint: Paint
    private var progressTopBlurPaint: Paint? = null
    private var progressBottomBlurPaint: Paint? = null
    private lateinit var divisionPaint: Paint
    private lateinit var textPaint: Paint
    private var divisionOffset = 0
    private var labelOffset = 0
    private var divisionLength = 0
    private var divisionWidth = 0
    private val hourLabels = listOf(12, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)

    private lateinit var circleBounds: RectF

    private var radius: Float = 0F
    private var center = Point(0, 0)
    private var progressBottomShadowSize = 0
    private var progressTopShadowSize = 0
    private var strokeBottomShadowColor = Color.TRANSPARENT
    private var strokeTopShadowColor = Color.TRANSPARENT
    private var labelColor = Color.WHITE
    private lateinit var sleepLayout: View
    private lateinit var wakeLayout: View
    private var sleepAngle = 30.0
    private var wakeAngle = 225.0
    private var draggingSleep = false
    private var draggingWake = false
    private val stepMinutes = 15
    private val textRect = Rect()
    private val calendar = Calendar.getInstance()

    var listener: ((bedTime: LocalTime, wakeTime: LocalTime) -> Unit)? = null

    var progressColor: Int
        @ColorInt
        get() = progressPaint.color
        set(@ColorInt color) {
            progressPaint.color = color
            invalidate()
        }

    var progressBackgroundColor: Int
        @ColorInt
        get() = progressBackgroundPaint.color
        set(@ColorInt color) {
            progressBackgroundPaint.color = color
            invalidate()
        }

    fun getBedTime() = computeBedTime()

    fun getWakeTime() = computeWakeTime()

    fun setTime(bedTime: LocalTime, wakeTime: LocalTime) {
        sleepAngle = Utils.minutesToAngle(bedTime.hour * 60 + bedTime.minute)
        wakeAngle = Utils.minutesToAngle(wakeTime.hour * 60 + wakeTime.minute)
        invalidate()
        notifyChanges()
    }

    val progressStrokeWidth: Float
        get() = progressPaint.strokeWidth


    private fun init(@NonNull context: Context, @Nullable attrs: AttributeSet?) {

        divisionOffset = dp2px(DEFAULT_DIVISION_OFFSET_DP)
        divisionLength = dp2px(DEFAULT_DIVISION_LENGTH_DP)
        divisionWidth = dp2px(DEFAULT_DIVISION_WIDTH_DP)
        labelOffset = dp2px(DEFAULT_LABEL_OFFSET_DP)
        var progressColor = Color.WHITE
        var progressBackgroundColor = Color.parseColor(DEFAULT_PROGRESS_BACKGROUND_COLOR)
        var divisionColor = Color.parseColor(DEFAULT_PROGRESS_BACKGROUND_COLOR)
        var progressStrokeWidth = dp2px(DEFAULT_STROKE_WIDTH_DP)
        var progressBgStrokeWidth = dp2px(DEFAULT_STROKE_WIDTH_DP)
        var progressStrokeCap: Paint.Cap = Paint.Cap.ROUND
        var sleepLayoutId = 0
        var wakeLayoutId = 0

        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.SleepTimePicker)

            sleepLayoutId = a.getResourceId(R.styleable.SleepTimePicker_sleepLayoutId, 0)
            wakeLayoutId = a.getResourceId(R.styleable.SleepTimePicker_wakeLayoutId, 0)

            progressColor = a.getColor(R.styleable.SleepTimePicker_progressColor, progressColor)
            progressBackgroundColor =
                a.getColor(R.styleable.SleepTimePicker_progressBackgroundColor, progressBackgroundColor)
            divisionColor = a.getColor(R.styleable.SleepTimePicker_divisionColor, divisionColor)
            progressStrokeWidth =
                a.getDimensionPixelSize(R.styleable.SleepTimePicker_progressStrokeWidth, progressStrokeWidth)
            progressBottomShadowSize = a.getDimensionPixelSize(R.styleable.SleepTimePicker_strokeBottomShadowRadius, 0)
            progressTopShadowSize = a.getDimensionPixelSize(R.styleable.SleepTimePicker_strokeTopShadowRadius, 0)
            progressBgStrokeWidth =
                a.getDimensionPixelSize(R.styleable.SleepTimePicker_progressBgStrokeWidth, progressStrokeWidth)
            strokeBottomShadowColor = a.getColor(R.styleable.SleepTimePicker_strokeBottomShadowColor, progressColor)
            strokeTopShadowColor = a.getColor(R.styleable.SleepTimePicker_strokeTopShadowColor, progressColor)
            labelColor = a.getColor(R.styleable.SleepTimePicker_labelColor, progressColor)
            labelColor = a.getColor(R.styleable.SleepTimePicker_labelColor, progressColor)

            progressStrokeCap = Paint.Cap.ROUND

            a.recycle()
        }

        progressPaint = Paint()
        progressPaint.strokeCap = progressStrokeCap
        progressPaint.strokeWidth = progressStrokeWidth.toFloat()
        progressPaint.style = Paint.Style.STROKE
        progressPaint.color = progressColor
        progressPaint.isAntiAlias = true

        progressBackgroundPaint = Paint()
        progressBackgroundPaint.style = Paint.Style.STROKE
        progressBackgroundPaint.strokeWidth = progressBgStrokeWidth.toFloat()
        progressBackgroundPaint.color = progressBackgroundColor
        progressBackgroundPaint.isAntiAlias = true

        if (progressTopShadowSize > 0) {
            progressTopBlurPaint = Paint()
            progressTopBlurPaint!!.strokeCap = Paint.Cap.ROUND
            progressTopBlurPaint!!.strokeWidth = BLUR_STROKE_RATIO * (progressTopShadowSize + progressStrokeWidth)
            progressTopBlurPaint!!.style = Paint.Style.STROKE
            progressTopBlurPaint!!.isAntiAlias = true
            val topBlurRadius = BLUR_RADIUS_RATIO * (progressTopShadowSize + progressBgStrokeWidth)
            progressTopBlurPaint!!.maskFilter = BlurMaskFilter(topBlurRadius, BlurMaskFilter.Blur.NORMAL)
            progressTopBlurPaint!!.color = strokeTopShadowColor
        }

        if (progressBottomShadowSize > 0) {
            progressBottomBlurPaint = Paint(0)
            progressBottomBlurPaint!!.strokeCap = Paint.Cap.ROUND
            progressBottomBlurPaint!!.strokeWidth = BLUR_STROKE_RATIO * (progressBottomShadowSize + progressStrokeWidth)
            progressBottomBlurPaint!!.style = Paint.Style.STROKE
            progressBottomBlurPaint!!.isAntiAlias = true
            val bottomBlurRadius = BLUR_RADIUS_RATIO * (progressBottomShadowSize + progressBgStrokeWidth)
            progressBottomBlurPaint!!.maskFilter = BlurMaskFilter(bottomBlurRadius, BlurMaskFilter.Blur.NORMAL)
            progressBottomBlurPaint!!.color = strokeBottomShadowColor
        }

        divisionPaint = Paint(0)
        divisionPaint.strokeCap = Paint.Cap.BUTT
        divisionPaint.strokeWidth = divisionWidth.toFloat()
        divisionPaint.color = divisionColor
        divisionPaint.style = Paint.Style.STROKE
        divisionPaint.isAntiAlias = true

        textPaint = Paint()
        textPaint.textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, SCALE_LABEL_TEXT_SIZE,
            resources.displayMetrics
        )
        textPaint.color = labelColor

        val inflater = LayoutInflater.from(context)
        sleepLayout = inflater.inflate(sleepLayoutId, this, false)
        wakeLayout = inflater.inflate(wakeLayoutId, this, false)
        addView(sleepLayout)
        addView(wakeLayout)
        circleBounds = RectF()

        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        setWillNotDraw(false)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val measuredWidth = View.MeasureSpec.getSize(widthMeasureSpec)
        val measuredHeight = View.MeasureSpec.getSize(heightMeasureSpec)
        measureChildren(widthMeasureSpec, heightMeasureSpec)

        val smallestSide = Math.min(measuredWidth, measuredHeight)
        Timber.d("onMeasure $smallestSide")
        setMeasuredDimension(smallestSide, smallestSide)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        calculateBounds(w, h)
        requestLayout()
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        layoutView(sleepLayout, sleepAngle)
        layoutView(wakeLayout, wakeAngle)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            if (isTouchOnView(sleepLayout, ev)) {
                draggingSleep = true
                return true
            }
            if (isTouchOnView(wakeLayout, ev)) {
                draggingWake = true
                return true
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        val x = ev.x
        val y = ev.y

        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val touchAngleRad = atan2(center.y - y, x - center.x).toDouble()
                if (draggingSleep) {
                    val sleepAngleRad = Math.toRadians(sleepAngle)
                    val diff = Math.toDegrees(angleBetweenVectors(sleepAngleRad, touchAngleRad))
                    sleepAngle = to_0_720(sleepAngle + diff)
                    requestLayout()
                    notifyChanges()
                    return true
                } else if (draggingWake) {
                    val wakeAngleRad = Math.toRadians(wakeAngle)
                    val diff = Math.toDegrees(angleBetweenVectors(wakeAngleRad, touchAngleRad))
                    wakeAngle = to_0_720(wakeAngle + diff)
                    requestLayout()
                    notifyChanges()
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                draggingSleep = false
                draggingWake = false
            }
        }
        return super.onTouchEvent(ev)
    }

    override fun onDraw(canvas: Canvas) {
        Timber.d("onDraw sleepAngle=$sleepAngle\nwakeAngle=$wakeAngle")
        drawProgressBackground(canvas)
        drawProgress(canvas)
        drawDivisions(canvas)
    }

    private fun notifyChanges() {
        val computeBedTime = computeBedTime()
        val computeWakeTime = computeWakeTime()
        Timber.d("notifyChanges \n$computeBedTime \n$computeWakeTime")
        listener?.invoke(computeBedTime, computeWakeTime)
    }

    private fun computeBedTime(): LocalTime {
        val bedMins = snapMinutes(angleToMins(sleepAngle), stepMinutes)
        return LocalTime.of((bedMins / 60) % 24, bedMins % 60)
    }

    private fun computeWakeTime(): LocalTime {
        val wakeMins = snapMinutes(angleToMins(wakeAngle), stepMinutes)
        return LocalTime.of((wakeMins / 60) % 24, wakeMins % 60)
    }

    private fun layoutView(view: View, angle: Double) {
        val measuredWidth = view.measuredWidth
        val measuredHeight = view.measuredHeight
        val halfWidth = measuredWidth / 2
        val halfHeight = measuredHeight / 2
        val parentCenterX = width / 2
        val parentCenterY = height / 2
        val centerX = (parentCenterX + radius * cos(Math.toRadians(angle))).toInt()
        val centerY = (parentCenterY - radius * sin(Math.toRadians(angle))).toInt()
        Timber.d("layoutWakeView radius= $radius $parentCenterX $parentCenterY $centerX $centerY")
        view.layout(
            (centerX - halfWidth),
            centerY - halfHeight,
            centerX + halfWidth,
            centerY + halfHeight
        )
    }

    private fun calculateBounds(w: Int, h: Int) {
        val maxChildWidth = max(sleepLayout.measuredWidth, wakeLayout.measuredWidth)
        val maxChildHeight = max(sleepLayout.measuredHeight, wakeLayout.measuredHeight)
        val maxChildSize = max(maxChildWidth, maxChildHeight)
        val offset = abs(progressBackgroundPaint.strokeWidth / 2 - maxChildSize / 2)
        val width = w - paddingStart - paddingEnd - maxChildSize - offset
        val height = h - paddingTop - paddingBottom - maxChildSize - offset

        radius = min(width, height) / 2F
        center = Point(w / 2, h / 2)

        circleBounds.left = center.x - radius
        circleBounds.top = center.y - radius
        circleBounds.right = center.x + radius
        circleBounds.bottom = center.y + radius
    }

    private fun isTouchOnView(view: View, ev: MotionEvent): Boolean {
        return (ev.x > view.left && ev.x < view.right
                && ev.y > view.top && ev.y < view.bottom)
    }


    private fun drawProgressBackground(canvas: Canvas) {
        Timber.d("drawProgressBackground $circleBounds")
        canvas.drawArc(
            circleBounds, ANGLE_START_PROGRESS_BACKGROUND.toFloat(),
            ANGLE_END_PROGRESS_BACKGROUND.toFloat(),
            false, progressBackgroundPaint
        )
    }

    private fun drawProgress(canvas: Canvas) {
        val startAngle = -sleepAngle.toFloat()
        val sweep = Utils.to_0_360(sleepAngle - wakeAngle).toFloat()
        progressBottomBlurPaint?.let {
            canvas.drawArc(circleBounds, startAngle, sweep, false, it)
        }
        progressTopBlurPaint?.let {
            canvas.drawArc(circleBounds, startAngle, sweep, false, it)
        }
        canvas.drawArc(circleBounds, startAngle, sweep, false, progressPaint)
    }

    private fun drawDivisions(canvas: Canvas) {
        val divisionAngle = 360 / hourLabels.size
        hourLabels.forEachIndexed { index, value ->
            val angle = (divisionAngle * index) - 90
            Timber.d("drawDivisions $angle $value")
            val radians = Math.toRadians(angle.toDouble())
            val bgStrokeWidth = progressBackgroundPaint.strokeWidth
            val startX = center.x + (radius - bgStrokeWidth / 2 - divisionOffset) * cos(radians)
            val endX = center.x + (radius - bgStrokeWidth / 2 - divisionOffset - divisionLength) * cos(radians)
            val startY = center.y + (radius - bgStrokeWidth / 2 - divisionOffset) * sin(radians)
            val endY = center.y + (radius - bgStrokeWidth / 2 - divisionOffset - divisionLength) * sin(radians)
            canvas.drawLine(startX.toFloat(), startY.toFloat(), endX.toFloat(), endY.toFloat(), divisionPaint)

            val tmp = value.toString()
            textPaint.getTextBounds(tmp, 0, tmp.length, textRect)
            val x = center.x + (radius - bgStrokeWidth / 2 - labelOffset) * cos(radians) - textRect.width() / 2
            val y = (center.y + (radius - bgStrokeWidth / 2 - labelOffset) * sin(radians) + textRect.height() / 2)
            canvas.drawText(tmp, x.toFloat(), y.toFloat(), textPaint)
        }
    }

    private fun dp2px(dp: Float): Int {
        val metrics = resources.displayMetrics
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, metrics).toInt()
    }

    companion object {

        private const val ANGLE_START_PROGRESS_BACKGROUND = 0
        private const val ANGLE_END_PROGRESS_BACKGROUND = 360
        private const val DEFAULT_STROKE_WIDTH_DP = 8F
        private const val DEFAULT_DIVISION_LENGTH_DP = 8F
        private const val DEFAULT_DIVISION_OFFSET_DP = 12F
        private const val DEFAULT_LABEL_OFFSET_DP = 36F
        private const val DEFAULT_DIVISION_WIDTH_DP = 2F
        private const val SCALE_LABEL_TEXT_SIZE = 13F
        private const val DEFAULT_PROGRESS_BACKGROUND_COLOR = "#e0e0e0"
        private const val BLUR_STROKE_RATIO = 3 / 8F
        private const val BLUR_RADIUS_RATIO = 1 / 4F
    }
}
