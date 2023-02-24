package cn.quibbler.flipview

import android.animation.Animator
import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.GestureDetectorCompat

class FlipView : FrameLayout {

    companion object {
        val TAG = "TAG_${FlipView::class.simpleName}"

        const val DEFAULT_FLIP_DURATION = 400
        const val DEFAULT_AUTO_FLIP_BACK_TIME = 1000
    }

    enum class FlipState {
        FRONT_SIDE, BACK_SIDE
    }

    private val animFlipHorizontalOutId: Int = R.animator.animation_horizontal_flip_out
    private val animFlipHorizontalInId: Int = R.animator.animation_horizontal_flip_in
    private val animFlipHorizontalRightOutId: Int = R.animator.animation_horizontal_right_out
    private val animFlipHorizontalRightInId: Int = R.animator.animation_horizontal_right_in
    private val animFlipVerticalOutId: Int = R.animator.animation_vertical_flip_out
    private val animFlipVerticalInId: Int = R.animator.animation_vertical_flip_in
    private val animFlipVerticalFrontOutId: Int = R.animator.animation_vertical_front_out
    private val animFlipVerticalFrontInId: Int = R.animator.animation_vertical_flip_front_in

    private var mSetRightOut: AnimatorSet? = null
    private var mSetLeftIn: AnimatorSet? = null
    private var mSetTopOut: AnimatorSet? = null
    private var mSetBottomIn: AnimatorSet? = null

    private var mIsBackVisible = false

    private var mCardFrontLayout: View? = null
    private var mCardBackLayout: View? = null

    private var flipType = "vertical"
    private var flipTypeFrom = "right"

    private var flipOnTouch = false
    private var flipDuration = 0
    private var flipEnabled = false
    private var flipOnceEnabled = false
    private var autoFlipBack = false
    private var autoFlipBackTime = DEFAULT_AUTO_FLIP_BACK_TIME

    private var x1 = 0f
    private var y1 = 0f

    private var mFlipState = FlipState.FRONT_SIDE

    private var onFlipListener: OnFlipAnimationListener? = null

    private var gestureDetector: GestureDetectorCompat? = null

    private val handler = Handler(Looper.getMainLooper())

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(context, attrs, defStyleAttr, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        // Setting Default Values
        flipOnTouch = true
        flipDuration = DEFAULT_FLIP_DURATION
        flipEnabled = true
        flipOnceEnabled = false
        autoFlipBack = false
        autoFlipBackTime = DEFAULT_AUTO_FLIP_BACK_TIME

        // Check for the attributes
        val attrArray = context.obtainStyledAttributes(attrs, R.styleable.FlipView, 0, 0)
        flipOnTouch = attrArray.getBoolean(R.styleable.FlipView_flipOnTouch, true)
        flipDuration = attrArray.getInt(R.styleable.FlipView_flipDuration, DEFAULT_FLIP_DURATION)
        flipEnabled = attrArray.getBoolean(R.styleable.FlipView_flipEnabled, true)
        flipOnceEnabled = attrArray.getBoolean(R.styleable.FlipView_flipOnceEnabled, false)
        autoFlipBack = attrArray.getBoolean(R.styleable.FlipView_autoFlipBack, false)
        autoFlipBackTime = attrArray.getInt(R.styleable.FlipView_autoFlipBackTime, DEFAULT_AUTO_FLIP_BACK_TIME)
        flipType = attrArray.getString(R.styleable.FlipView_flipType) ?: "vertical"
        flipTypeFrom = attrArray.getString(R.styleable.FlipView_flipFrom) ?: "left"
        attrArray.recycle()

        loadAnimations()
    }


    override fun onFinishInflate() {
        super.onFinishInflate()
        if (childCount > 2) {
            throw IllegalStateException("FlipView can host only two direct children!")
        }

        findViews()
        changeCameraDistance()
        setupInitializations()
        initGestureDetector()
    }

    override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
        if (childCount == 2) {
            throw IllegalStateException("FlipView can host only two direct children!")
        }

        super.addView(child, index, params)

        findViews()
        changeCameraDistance()
    }

    override fun removeAllViewsInLayout() {
        super.removeAllViewsInLayout()

        // Reset the state
        mFlipState = FlipState.FRONT_SIDE

        findViews()
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        try {
            return gestureDetector?.onTouchEvent(ev) == true || super.dispatchTouchEvent(ev)
        } catch (throwable: Throwable) {
            throw IllegalStateException("Error in dispatchTouchEvent: ", throwable)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return if (isEnabled && flipOnTouch) {
            gestureDetector?.onTouchEvent(event) == true
        } else {
            super.onTouchEvent(event)
        }
    }

    private fun findViews() {
        // Invalidation since we use this also on removeView
        mCardBackLayout = null
        mCardFrontLayout = null

        val childs = childCount
        if (childs < 1) {
            return
        }

        if (childs < 2) {
            // Only invalidate flip state if we have a single child
            mFlipState = FlipState.FRONT_SIDE
            mCardFrontLayout = getChildAt(0)
        } else if (childs == 2) {
            mCardFrontLayout = getChildAt(1)
            mCardBackLayout = getChildAt(0)
        }

        if (!flipOnTouch) {
            mCardFrontLayout?.visibility = View.VISIBLE
            mCardBackLayout?.visibility = View.GONE
        }

    }

    private fun setupInitializations() {
        mCardBackLayout?.visibility = GONE
    }

    private fun initGestureDetector() {
        gestureDetector = GestureDetectorCompat(this.context, SwipeDetector())
    }

    private fun loadAnimations() {
        if (flipType.equals("horizontal", true)) {
            if (flipTypeFrom.equals("left", true)) {
                mSetRightOut = AnimatorInflater.loadAnimator(context, animFlipHorizontalOutId) as AnimatorSet
                mSetLeftIn = AnimatorInflater.loadAnimator(context, animFlipHorizontalInId) as AnimatorSet
            } else {
                mSetRightOut = AnimatorInflater.loadAnimator(this.context, animFlipHorizontalRightOutId) as AnimatorSet
                mSetLeftIn = AnimatorInflater.loadAnimator(this.context, animFlipHorizontalRightInId) as AnimatorSet
            }

            if (mSetRightOut == null || mSetLeftIn == null) {
                throw RuntimeException("No Animations Found! Please set Flip in and Flip out animation Ids.")
            }

            mSetRightOut?.let {
                it.removeAllListeners()
                it.addListener(object : Animator.AnimatorListener {
                    override fun onAnimationStart(animation: Animator) {
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        if (mFlipState == FlipState.FRONT_SIDE) {
                            mCardBackLayout?.visibility = GONE
                            mCardFrontLayout?.visibility = VISIBLE
                            onFlipListener?.onViewFlipCompleted(this@FlipView, FlipState.FRONT_SIDE)
                        } else {
                            mCardBackLayout?.visibility = VISIBLE
                            mCardFrontLayout?.visibility = GONE

                            onFlipListener?.onViewFlipCompleted(this@FlipView, FlipState.BACK_SIDE)

                            // Auto Flip Back
                            if (autoFlipBack) {
                                handler.postDelayed({
                                    flipTheView()
                                }, autoFlipBackTime.toLong())
                            }
                        }
                    }

                    override fun onAnimationCancel(animation: Animator) {
                    }

                    override fun onAnimationRepeat(animation: Animator) {
                    }
                })
            }
            setFlipDuration(flipDuration)
        } else {
            if (flipTypeFrom.equals("front", true)) {
                mSetTopOut = AnimatorInflater.loadAnimator(this.context, animFlipVerticalFrontOutId) as AnimatorSet
                mSetBottomIn = AnimatorInflater.loadAnimator(this.context, animFlipVerticalFrontInId) as AnimatorSet
            } else {
                mSetTopOut = AnimatorInflater.loadAnimator(this.context, animFlipVerticalOutId) as AnimatorSet
                mSetBottomIn = AnimatorInflater.loadAnimator(this.context, animFlipVerticalInId) as AnimatorSet
            }
            if (mSetTopOut == null || mSetBottomIn == null) {
                throw RuntimeException("No Animations Found! Please set Flip in and Flip out animation Ids.")
            }
            mSetTopOut?.apply {
                removeAllListeners()
                addListener(object : Animator.AnimatorListener {
                    override fun onAnimationStart(animation: Animator) {
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        if (mFlipState == FlipState.FRONT_SIDE) {
                            mCardBackLayout?.visibility = GONE
                            mCardFrontLayout?.visibility = VISIBLE
                            onFlipListener?.onViewFlipCompleted(this@FlipView, FlipState.FRONT_SIDE)
                        } else {
                            mCardBackLayout?.visibility = VISIBLE
                            mCardFrontLayout?.visibility = GONE

                            onFlipListener?.onViewFlipCompleted(this@FlipView, FlipState.BACK_SIDE)

                            // Auto Flip Back
                            if (autoFlipBack) {
                                handler.postDelayed({
                                    flipTheView()
                                }, autoFlipBackTime.toLong())
                            }
                        }
                    }

                    override fun onAnimationCancel(animation: Animator) {
                    }

                    override fun onAnimationRepeat(animation: Animator) {
                    }
                })
                setFlipDuration(flipDuration)
            }
        }
    }

    private fun changeCameraDistance() {
        val distance = 8000
        val scale = resources.displayMetrics.density * distance
        mCardFrontLayout?.cameraDistance = scale
        mCardBackLayout?.cameraDistance = scale
    }

    /**
     * Play the animation of flipping and flip the view for one side!
     */
    private fun flipTheView() {
        if (!flipEnabled || childCount < 2) return
        if (flipOnceEnabled && mFlipState == FlipState.BACK_SIDE) return

        if (flipType.equals("horizontal", true)) {
            if (mSetRightOut?.isRunning == true || mSetLeftIn?.isRunning == true) return

            mCardBackLayout?.visibility = VISIBLE
            mCardFrontLayout?.visibility = VISIBLE

            if (mFlipState == FlipState.FRONT_SIDE) {
                // From front to back
                mSetRightOut?.setTarget(mCardFrontLayout)
                mSetLeftIn?.setTarget(mCardBackLayout)
                mSetRightOut?.start()
                mSetLeftIn?.start()
                mIsBackVisible = true
                mFlipState = FlipState.BACK_SIDE
            } else {
                // from back to front
                mSetRightOut?.setTarget(mCardBackLayout)
                mSetLeftIn?.setTarget(mCardFrontLayout)
                mSetRightOut?.start()
                mSetLeftIn?.start()
                mIsBackVisible = false
                mFlipState = FlipState.FRONT_SIDE
            }
        } else {
            if (mSetTopOut?.isRunning == true || mSetBottomIn?.isRunning == true) return

            mCardBackLayout?.visibility = VISIBLE
            mCardFrontLayout?.visibility = VISIBLE

            if (mFlipState == FlipState.FRONT_SIDE) {
                // From front to back
                mSetTopOut?.setTarget(mCardFrontLayout)
                mSetBottomIn?.setTarget(mCardBackLayout)
                mSetTopOut?.start()
                mSetBottomIn?.start()
                mIsBackVisible = true
                mFlipState = FlipState.BACK_SIDE
            } else {
                // from back to front
                mSetTopOut?.setTarget(mCardBackLayout)
                mSetBottomIn?.setTarget(mCardFrontLayout)
                mSetTopOut?.start()
                mSetBottomIn?.start()
                mIsBackVisible = false
                mFlipState = FlipState.FRONT_SIDE
            }
        }
    }

    /**
     * Flip the view for one side with or without animation.
     *
     * @param withAnimation true means flip view with animation otherwise without animation.
     */
    fun flipTheView(withAnimation: Boolean) {
        if (childCount < 2) return

        if (flipType.equals("horizontal", true)) {
            if (!withAnimation) {
                mSetLeftIn?.duration = 0L
                mSetRightOut?.duration = 0L
                val oldFlipEnabled = flipEnabled
                flipEnabled = true

                flipTheView()

                mSetLeftIn?.duration = flipDuration.toLong()
                mSetRightOut?.duration = flipDuration.toLong()
                flipEnabled = oldFlipEnabled
            } else {
                flipTheView()
            }
        } else {
            if (!withAnimation) {
                mSetBottomIn?.duration = 0L
                mSetTopOut?.duration = 0L
                val oldFlipEnabled = flipEnabled
                flipEnabled = true

                flipTheView()

                mSetBottomIn?.duration = flipDuration.toLong()
                mSetTopOut?.duration = flipDuration.toLong()
                flipEnabled = oldFlipEnabled
            } else {
                flipTheView()
            }
        }
    }

    /**
     * Sets the flip duration (in milliseconds)
     *
     * @param flipDuration duration in milliseconds
     */
    fun setFlipDuration(flipDuration: Int) {
        this.flipDuration = flipDuration
        if (flipType.equals("horizontal", true)) {
            //mSetRightOut.setDuration(flipDuration);
            mSetRightOut?.childAnimations?.get(0)?.duration = flipDuration.toLong()
            mSetRightOut?.childAnimations?.get(1)?.startDelay = (flipDuration / 2).toLong()

            //mSetLeftIn.setDuration(flipDuration);
            mSetLeftIn?.childAnimations?.get(1)?.duration = flipDuration.toLong()
            mSetLeftIn?.childAnimations?.get(2)?.startDelay = (flipDuration / 2).toLong()
        } else {
            mSetTopOut?.childAnimations?.get(0)?.duration = flipDuration.toLong()
            mSetTopOut?.childAnimations?.get(1)?.startDelay = (flipDuration / 2).toLong()

            mSetBottomIn?.childAnimations?.get(1)?.duration = flipDuration.toLong()
            mSetBottomIn?.childAnimations?.get(2)?.startDelay = (flipDuration / 2).toLong()
        }
    }

    /**
     * The Flip Animation Listener for animations and flipping complete listeners
     */
    interface OnFlipAnimationListener {

        /**
         * Called when flip animation is completed.
         *
         * @param newCurrentSide After animation, the new side of the view. Either can be
         *                       FlipState.FRONT_SIDE or FlipState.BACK_SIDE
         */
        fun onViewFlipCompleted(flipView: FlipView, newCurrentSide: FlipState)
    }

    private inner class SwipeDetector : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            if (isEnabled && flipOnTouch) {
                return true
            }
            return super.onDown(e)
        }

        override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean = false

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            if (isEnabled && flipOnTouch) {
                flipTheView()
            }
            return super.onSingleTapConfirmed(e)
        }
    }

}