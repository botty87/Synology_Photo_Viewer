package com.botty.tvrecyclerview

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import android.view.FocusFinder
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Scroller
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexboxLayoutManager

class TvRecyclerView @JvmOverloads constructor(context: Context?, attrs: AttributeSet? = null, defStyle: Int = 0)
    : RecyclerView(context!!, attrs, defStyle) {


    var mFocusBorderView: FocusBorderView? = null
        private set

    var drawableFocus: Drawable? = null
        private set
    var mIsDrawFocusMoveAnim = false
    var selectedScaleValue = DEFAULT_SELECT_SCALE
        private set
    var focusMoveAnimScale = 0f
        private set
    /**
     * When the TvRecyclerView width is determined, the returned position is correct
     * @return selected view position
     */
    var selectedPosition = 0
        private set
    var nextFocusView: View? = null
        private set
    private var mInLayout = false
    private var mFocusFrameLeft = 22
    private var mFocusFrameTop = 22
    private var mFocusFrameRight = 22
    private var mFocusFrameBottom = 22
    private var mReceivedInvokeKeyDown = false
    var selectedView: View? = null
        private set
    private var mItemStateListener: OnItemStateListener? = null
    private var mScrollListener: OnScrollStateListener? = null
    private var mScrollerFocusMoveAnim: Scroller = Scroller(context)
    private var mPendingMoveSmoothScroller: PendingMoveSmoothScroller? =
        null
    private var mScrollMode = SCROLL_ALIGN
    private var mIsAutoProcessFocus = true
    private var mOrientation = HORIZONTAL
    private var mDirection = 0
    private var mIsSetItemSelected = false
    private var mNumRows = 1
    private var mIsNeedMoved = false
    var mLayerType = View.LAYER_TYPE_SOFTWARE

    init {
        setAttributeSet(attrs)
        addOnScrollListener(object : OnScrollListener() {
            @SuppressLint("LogNotTimber")
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (mIsSetItemSelected) {
                    if (DEBUG) {
                        Log.d(TAG, "onScrolled: mSelectedPosition=$selectedPosition"
                        )
                    }
                    mIsSetItemSelected = false
                    val firstVisiblePos = firstVisiblePosition
                    nextFocusView = getChildAt(selectedPosition - firstVisiblePos)
                    nextFocusView?.run {
                        if (DEBUG) {
                            Log.d(TAG, "onScrolled: start adjust scroll distance")
                        }
                        if (mIsAutoProcessFocus) {
                            scrollToViewToCenter(this)
                        } else {
                            mIsNeedMoved = true
                            this.requestFocus()
                        }
                    }
                }
            }
        })
    }

    private fun setAttributeSet(attrs: AttributeSet?) {
        if (attrs != null) {
            val typeArray =
                context.obtainStyledAttributes(attrs, R.styleable.TvRecyclerView)
            mScrollMode = typeArray.getInteger(
                R.styleable.TvRecyclerView_scrollMode,
                SCROLL_ALIGN
            )
            val drawable = typeArray.getDrawable(R.styleable.TvRecyclerView_focusDrawable)
            drawable?.run { drawableFocus = this }

            selectedScaleValue = typeArray.getFloat(R.styleable.TvRecyclerView_focusScale, DEFAULT_SELECT_SCALE)
            mIsAutoProcessFocus = typeArray.getBoolean(R.styleable.TvRecyclerView_isAutoProcessFocus, true)
            if (!mIsAutoProcessFocus) {
                selectedScaleValue = 1.0f
                isChildrenDrawingOrderEnabled = true
            }
            typeArray.recycle()
        }
        if (mIsAutoProcessFocus) { // set TvRecyclerView process Focus
            descendantFocusability = ViewGroup.FOCUS_BEFORE_DESCENDANTS
        }
    }

    private fun addFlyBorderView(context: Context) {
        if (mFocusBorderView == null) {
            mFocusBorderView = FocusBorderView(context)
            (context as Activity).window.addContentView(
                mFocusBorderView,
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            )
            mFocusBorderView!!.setSelectPadding(
                mFocusFrameLeft, mFocusFrameTop,
                mFocusFrameRight, mFocusFrameBottom
            )
        }
    }

    val firstVisiblePosition: Int
        get() {
            var firstVisiblePos = -1
            if (layoutManager is LinearLayoutManager) {
                firstVisiblePos = (layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
            }
            /*is ModuleLayoutManager -> {
                    firstVisiblePos = findFirstVisibleItemPosition()
                }*/
            return firstVisiblePos
        }

    /*val lastVisiblePosition: Int
        get() {
            var lastVisiblePos = -1
            if (layoutManager is LinearLayoutManager) {
                lastVisiblePos = (layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
            }
            is ModuleLayoutManager -> {
                        firstVisiblePos = findFirstVisibleItemPosition()
                    }
            return lastVisiblePos
        }*/

    @SuppressLint("LogNotTimber")
    override fun setLayoutManager(layoutManager: LayoutManager?) {
        when (layoutManager) {
            is GridLayoutManager -> {
                mOrientation = layoutManager.orientation
                mNumRows = layoutManager.spanCount
            }
            is LinearLayoutManager -> {
                mOrientation = layoutManager.orientation
                mNumRows = 1
            }
            /*is ModuleLayoutManager -> {
                mOrientation = layoutManager.orientation
            }*/
        }
        Log.i(TAG, "setLayoutManager: orientation==$mOrientation")
        super.setLayoutManager(layoutManager)
    }

    override fun focusSearch(focused: View, direction: Int): View {
        mDirection = direction
        return super.focusSearch(focused, direction)
    }

    /**
     * note: if you set the property of isAutoProcessFocus is false, the listener will be invalid
     * @param listener itemStateListener
     */
    fun setOnItemStateListener(listener: OnItemStateListener?) {
        mItemStateListener = listener
    }

    /*fun setOnScrollStateListener(listener: OnScrollStateListener?) {
        mScrollListener = listener
    }

    fun setSelectedScale(scale: Float) {
        if (scale >= 1.0f) {
            selectedScaleValue = scale
        }
    }

    fun setIsAutoProcessFocus(isAuto: Boolean) {
        mIsAutoProcessFocus = isAuto
        if (!isAuto) {
            selectedScaleValue = 1.0f
            isChildrenDrawingOrderEnabled = true
        } else if (selectedScaleValue == 1.0f) {
            selectedScaleValue = DEFAULT_SELECT_SCALE
        }
    }

    fun setScrollMode(mode: Int) {
        mScrollMode = mode
    } */

    /**
     * When call this method, you must ensure that the location of the view has been inflate
     * @param position selected item position
     */
    @SuppressLint("LogNotTimber")
    fun setItemSelected(position: Int) {
        var selectedPosition = position
        if (this.selectedPosition == selectedPosition) {
            return
        }
        if (selectedPosition >= adapter!!.itemCount) {
            selectedPosition = adapter!!.itemCount - 1
        }
        val firstPos = getChildAdapterPosition(getChildAt(0))
        val lastPos = getChildAdapterPosition(getChildAt(childCount - 1))
        if (DEBUG) {
            Log.d(TAG, "setItemSelected: first=$firstPos=last=$lastPos=pos=$selectedPosition")
        }
        if (selectedPosition in firstPos..lastPos) {
            nextFocusView = getChildAt(selectedPosition - firstPos)
            if (mIsAutoProcessFocus && !mIsDrawFocusMoveAnim) {
                scrollToView(nextFocusView, true)
            } else {
                nextFocusView!!.requestFocus()
            }
        } else {
            mIsSetItemSelected = true
            this.selectedPosition = selectedPosition
            scrollToPosition(selectedPosition)
        }
    }

    /*override fun isInTouchMode(): Boolean {
        val result = super.isInTouchMode()
        return if (Build.VERSION.SDK_INT == 19) {
            !(hasFocus() && !result)
        } else {
            result
        }
    }*/

    /**
     * fix issue: not have focus box when change focus
     * @param child child view
     * @param focused the focused view
     */
    @SuppressLint("LogNotTimber")
    override fun requestChildFocus(child: View, focused: View) {
        super.requestChildFocus(child, focused)
        if (selectedPosition < 0) {
            selectedPosition = getAdapterPositionByView(child)
        }
        if (mIsAutoProcessFocus) {
            requestFocus()
        } else {
            val position = getAdapterPositionByView(focused)
            if ((selectedPosition != position || mIsNeedMoved) && !mIsSetItemSelected) {
                selectedPosition = position
                selectedView = focused
                var distance = getNeedScrollDistance(focused)
                if (mIsNeedMoved && mScrollMode != SCROLL_FOLLOW) {
                    distance = getFollowDistance(focused)
                }
                mIsNeedMoved = false
                if (distance != 0) {
                    if (DEBUG) {
                        Log.d(TAG, "requestChildFocus: scroll distance=$distance")
                    }
                    smoothScrollView(distance)
                }
            }
        }
        if (DEBUG) {
            Log.d(TAG, "requestChildFocus: SelectPos=$selectedPosition")
        }
    }

    private fun getAdapterPositionByView(view: View?): Int {
        if (view == null) {
            return NO_POSITION
        }
        val params = view.layoutParams as LayoutParams?
        return if (params?.isItemRemoved != false) { // when item is removed, the position value can be any value.
            NO_POSITION
        } else params.viewAdapterPosition
    }

    @SuppressLint("LogNotTimber")
    override fun onFinishInflate() {
        super.onFinishInflate()
        if (mIsAutoProcessFocus) {
            if (DEBUG) {
                Log.d(TAG, "onFinishInflate: add fly border view")
            }
            mLayerType = layerType
            addFlyBorderView(context)
        }
    }

    @SuppressLint("LogNotTimber")
    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        if (DEBUG) {
            Log.d(TAG, "onFocusChanged: gainFocus==$gainFocus")
        }
        if (mItemStateListener != null) {
            if (selectedView == null) {
                selectedView = getChildAt(selectedPosition - firstVisiblePosition)
            }
            mItemStateListener!!.onItemViewFocusChanged(
                gainFocus, selectedView,
                selectedPosition)
        }
        if (mFocusBorderView == null) {
            return
        }
        mFocusBorderView!!.tvRecyclerView = this
        if (gainFocus) {
            mFocusBorderView!!.bringToFront()
        }
        selectedView?.run {
            isSelected = gainFocus
            if (gainFocus && !mInLayout) {
                mFocusBorderView!!.startFocusAnim()
            }
        }
        if (!gainFocus) {
            mFocusBorderView!!.dismissGetFocus()
        }
    }

    override fun getChildDrawingOrder(childCount: Int, i: Int): Int {
        val focusIndex = indexOfChild(selectedView)
        if (focusIndex < 0) {
            return i
        }
        return when {
            i < focusIndex -> {
                i
            }
            i < childCount - 1 -> {
                focusIndex + childCount - 1 - i
            }
            else -> {
                focusIndex
            }
        }
    }

    @SuppressLint("LogNotTimber")
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        mInLayout = true
        super.onLayout(changed, l, t, r, b)
        // fix issue: when start anim the FocusView location error in AutoProcessFocus mode
        val adapter = adapter
        if (adapter != null && selectedPosition >= adapter.itemCount) {
            selectedPosition = adapter.itemCount - 1
        }
        var selectPos = selectedPosition - firstVisiblePosition
        selectPos = if (selectPos < 0) 0 else selectPos
        selectedView = getChildAt(selectPos)
        mInLayout = false
        if (DEBUG) {
            Log.d(TAG, "onLayout: selectPos=$selectPos=SelectedItem=$selectedView")
        }
    }

    @SuppressLint("LogNotTimber")
    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        if (mFocusBorderView?.tvRecyclerView != null) {
            if (DEBUG) {
                Log.d(TAG, "dispatchDraw: Border view invalidate.")
            }
            mFocusBorderView!!.invalidate()
        }
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        val bundle = state as Bundle
        val superData = bundle.getParcelable<Parcelable>("super_data")
        super.onRestoreInstanceState(superData)
        setItemSelected(bundle.getInt("select_pos", 0))
    }

    override fun onSaveInstanceState(): Parcelable? {
        val bundle = Bundle()
        val superData = super.onSaveInstanceState()
        bundle.putParcelable("super_data", superData)
        bundle.putInt("select_pos", selectedPosition)
        return bundle
    }

    @SuppressLint("LogNotTimber")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            val keyCode = event.keyCode
            if (selectedView == null) {
                selectedView = getChildAt(selectedPosition)
            }
            try {
                FocusFinder.getInstance().run {
                    when (keyCode) {
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            nextFocusView = findNextFocus(this@TvRecyclerView, selectedView, View.FOCUS_LEFT)
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            nextFocusView = findNextFocus(this@TvRecyclerView, selectedView, View.FOCUS_RIGHT)
                        }
                        KeyEvent.KEYCODE_DPAD_UP -> {
                            nextFocusView = findNextFocus(this@TvRecyclerView, selectedView, View.FOCUS_UP)
                        }
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            nextFocusView = findNextFocus(this@TvRecyclerView, selectedView, View.FOCUS_DOWN)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "dispatchKeyEvent: get next focus item error: " + e.message)
                nextFocusView = null
            }
            if (DEBUG) {
                Log.d(TAG, "dispatchKeyEvent: mNextFocused=$nextFocusView=nextPos=${getChildAdapterPosition(nextFocusView!!)}")
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun isFindNextFocusView(keyCode: Int): Boolean {
        if (!mIsAutoProcessFocus) {
            return false
        }
        val movement = getMovement(keyCode)
        if (movement == NEXT_ITEM) {
            if (!hasCreatedLastItem()) {
                processPendingMovement(true)
                return true
            }
        } else if (movement == PREV_ITEM) {
            if (!hasCreatedFirstItem()) {
                processPendingMovement(false)
                return true
            }
        }
        return false
    }

    private fun processPendingMovement(forward: Boolean) {
        if (if (forward) hasCreatedLastItem() else hasCreatedFirstItem()) {
            return
        }
        if (mPendingMoveSmoothScroller == null) { // Stop existing scroller and create a new PendingMoveSmoothScroller.
            stopScroll()
            val linearSmoothScroller = PendingMoveSmoothScroller(context, if (forward) 1 else -1)
            layoutManager!!.startSmoothScroll(linearSmoothScroller)
            if (linearSmoothScroller.isRunning) {
                mPendingMoveSmoothScroller = linearSmoothScroller
            }
        } else {
            if (forward) {
                mPendingMoveSmoothScroller!!.increasePendingMoves()
            } else {
                mPendingMoveSmoothScroller!!.decreasePendingMoves()
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_DOWN -> if (processMoves(keyCode)) {
                return true
            }
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER -> mReceivedInvokeKeyDown =
                true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                if (mReceivedInvokeKeyDown) {
                    if (adapter != null && selectedView != null) {
                        if (mItemStateListener != null) {
                            if (mFocusBorderView != null) {
                                mFocusBorderView!!.startClickAnim()
                            }
                            mItemStateListener!!.onItemViewClick(selectedView, selectedPosition)
                        }
                    }
                    mReceivedInvokeKeyDown = false
                    if (mIsAutoProcessFocus) {
                        return true
                    }
                }
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun computeScroll() {
        if (mScrollerFocusMoveAnim.computeScrollOffset()) {
            if (mIsDrawFocusMoveAnim) {
                focusMoveAnimScale = mScrollerFocusMoveAnim.currX.toFloat() / 100
            }
            postInvalidate()
        } else {
            if (mIsDrawFocusMoveAnim) {
                mIsDrawFocusMoveAnim = false
                updateSelectPositionInLayout(nextFocusView)
                setLayerType(mLayerType, null)
                postInvalidate()
                if (mItemStateListener != null) {
                    mItemStateListener!!.onItemViewFocusChanged(true, selectedView, selectedPosition)
                }
            }
        }
    }

    private fun updateSelectPositionInLayout(nextView: View?) {
        if (nextView != null) {
            selectedView = nextView
            selectedPosition = getChildAdapterPosition(selectedView!!)
        }
    }

    @SuppressLint("LogNotTimber")
    private fun processMoves(keyCode: Int): Boolean {
        return if (nextFocusView == null) { // fix issue: When the childView just fills the display area, it can't slide
            if (isFindNextFocusView(keyCode)) {
                return true
            } else if (mIsAutoProcessFocus) { // scroll start or end
                notifyScrollState(keyCode)
                mIsDrawFocusMoveAnim = false
            }
            if (DEBUG) {
                Log.d(TAG, "processMoves: error")
            }
            false
        } else {
            if (mIsDrawFocusMoveAnim) {
                updateSelectPositionInLayout(nextFocusView)
            }
            scrollToView(nextFocusView, true)
            true
        }
    }

    @SuppressLint("LogNotTimber")
    private fun scrollToViewToCenter(view: View) {
        val scrollDistance = getFollowDistance(view)
        if (DEBUG) {
            Log.d(TAG, "scrollToViewToCenter: scrollDistance==$scrollDistance")
        }
        if (scrollDistance != 0) {
            smoothScrollView(scrollDistance)
        }
        startFocusMoveAnim()
    }

    //TODO verify smooth
    @SuppressLint("LogNotTimber")
    private fun scrollToView(view: View?, smooth: Boolean) {
        val scrollDistance = getNeedScrollDistance(view)
        if (DEBUG) {
            Log.d(TAG, "scrollToView: scrollDistance==$scrollDistance")
        }
        if (scrollDistance != 0) {
            if (smooth) {
                smoothScrollView(scrollDistance)
            } else {
                scrollToView(scrollDistance)
            }
        }
        startFocusMoveAnim()
    }

    private fun notifyScrollState(keyCode: Int) {
        if (mScrollListener != null) {
            if (mOrientation == HORIZONTAL) {
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        mScrollListener!!.onScrollEnd(selectedView)
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        mScrollListener!!.onScrollStart(selectedView)
                    }
                }
            } else {
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        mScrollListener!!.onScrollEnd(selectedView)
                    }
                    KeyEvent.KEYCODE_DPAD_UP -> {
                        mScrollListener!!.onScrollStart(selectedView)
                    }
                }
            }
        }
    }

    private fun getNeedScrollDistance(focusView: View?): Int {
        return when (mScrollMode) {
            SCROLL_ALIGN -> getAlignDistance(focusView)
            SCROLL_FOLLOW -> getFollowDistance(focusView)
            SCROLL_NORMAL -> getNormalDistance(focusView)
            else -> getAlignDistance(focusView)
        }
    }

    private fun getAlignDistance(view: View?): Int {
        var scrollDistance = 0
        val isVisible = isVisibleChild(view)
        val isHalfVisible = isHalfVisibleChild(view)
        if (isHalfVisible || !isVisible) {
            scrollDistance = getScrollPrimary(view)
        }
        return scrollDistance
    }

    private fun getFollowDistance(view: View?): Int {
        var scrollDistance = 0
        val isOver = isOverHalfScreen(view)
        if (isOver) {
            scrollDistance = getScrollPrimary(view)
        }
        return scrollDistance
    }

    private fun getNormalDistance(view: View?): Int {
        if (!mIsAutoProcessFocus) {
            return 0
        }
        var scrollDistance = 0
        val isVisible = isVisibleChild(view)
        val isHalfVisible = isHalfVisibleChild(view)
        if (isHalfVisible || !isVisible) {
            scrollDistance = getNormalScrollDistance(view)
        }
        return scrollDistance
    }

    private val clientSize: Int
        get() = if (mOrientation == HORIZONTAL) {
            width - paddingLeft - paddingRight
        } else {
            height - paddingTop - paddingBottom
        }

    private fun getScrollPrimary(view: View?): Int {
        return if (mOrientation == HORIZONTAL) {
            if (mDirection != DEFAULT_DIRECTION) {
                if (mDirection == View.FOCUS_UP || mDirection == View.FOCUS_DOWN) {
                    return 0
                }
            }
            view!!.left +
                    view.width / 2 - clientSize / 2
        } else {
            if (mDirection != DEFAULT_DIRECTION) {
                if (mDirection == View.FOCUS_LEFT || mDirection == View.FOCUS_RIGHT) {
                    return 0
                }
            }
            view!!.top +
                    view.height / 2 - clientSize / 2
        }
    }

    private fun smoothScrollView(scrollDistance: Int) {
        if (mOrientation == HORIZONTAL) {
            smoothScrollBy(scrollDistance, 0)
        } else {
            smoothScrollBy(0, scrollDistance)
        }
    }

    private fun scrollToView(scrollDistance: Int) {
        if (mOrientation == HORIZONTAL) {
            scrollBy(scrollDistance, 0)
        } else {
            scrollBy(0, scrollDistance)
        }
    }

    private fun getNormalScrollDistance(view: View?): Int {
        var distance = 0
        val viewMin = getDecoratedStart(view)
        val viewMax = getDecoratedEnd(view)
        var firstView: View? = null
        var lastView: View? = null
        val paddingLow = paddingLow
        val clientSize = clientSize
        val maxValue =
            paddingLow + clientSize - NORMAL_SCROLL_COMPENSATION_VAL
        if (viewMin < paddingLow) {
            firstView = view
        } else if (viewMax > maxValue) {
            lastView = view
        }
        if (firstView != null) {
            distance =
                getDecoratedStart(firstView) - paddingLow - NORMAL_SCROLL_COMPENSATION_VAL
        } else if (lastView != null) {
            distance = getDecoratedEnd(lastView) - maxValue
        }
        return distance
    }

    private val paddingLow: Int
        get() = if (mOrientation == HORIZONTAL) {
            paddingLeft
        } else {
            paddingTop
        }

    private fun getDecoratedStart(view: View?): Int {
        val params = view!!.layoutParams as LayoutParams
        return if (mOrientation == VERTICAL) {
            layoutManager!!.getDecoratedTop(view) - params.topMargin
        } else {
            layoutManager!!.getDecoratedLeft(view) - params.leftMargin
        }
    }

    private fun getDecoratedEnd(view: View?): Int {
        val params = view!!.layoutParams as LayoutParams
        return if (mOrientation == VERTICAL) {
            layoutManager!!.getDecoratedBottom(view) + params.bottomMargin
        } else {
            layoutManager!!.getDecoratedRight(view) + params.rightMargin
        }
    }

    private fun hasCreatedLastItem(): Boolean {
        val count = layoutManager!!.itemCount
        return count == 0 || findViewHolderForAdapterPosition(count - 1) != null
    }

    private fun hasCreatedFirstItem(): Boolean {
        val count = layoutManager!!.itemCount
        return count == 0 || findViewHolderForAdapterPosition(0) != null
    }

    private fun getMovement(keyCode: Int): Int {
        var movement = View.FOCUS_LEFT
        if (mOrientation == HORIZONTAL) {
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> movement =
                    PREV_ITEM
                KeyEvent.KEYCODE_DPAD_RIGHT -> movement =
                    NEXT_ITEM
                KeyEvent.KEYCODE_DPAD_UP -> movement =
                    PREV_ROW
                KeyEvent.KEYCODE_DPAD_DOWN -> movement =
                    NEXT_ROW
            }
        } else if (mOrientation == VERTICAL) {
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> movement =
                    PREV_ROW
                KeyEvent.KEYCODE_DPAD_RIGHT -> movement =
                    NEXT_ROW
                KeyEvent.KEYCODE_DPAD_UP -> movement =
                    PREV_ITEM
                KeyEvent.KEYCODE_DPAD_DOWN -> movement =
                    NEXT_ITEM
            }
        }
        return movement
    }

    private fun isHalfVisibleChild(child: View?): Boolean {
        if (child != null) {
            val ret = Rect()
            val isVisible = child.getLocalVisibleRect(ret)
            return if (mOrientation == HORIZONTAL) {
                isVisible && ret.width() < child.width
            } else {
                isVisible && ret.height() < child.height
            }
        }
        return false
    }

    private fun isVisibleChild(child: View?): Boolean {
        if (child != null) {
            val ret = Rect()
            return child.getLocalVisibleRect(ret)
        }
        return false
    }

    private fun isOverHalfScreen(child: View?): Boolean {
        val ret = Rect()
        child!!.getGlobalVisibleRect(ret)
        val size = clientSize
        if (mOrientation == HORIZONTAL) {
            if (ret.right > size / 2 || ret.left < size / 2) {
                return true
            }
        } else {
            if (ret.top < size / 2 || ret.bottom > size / 2) {
                return true
            }
        }
        return false
    }

    @SuppressLint("LogNotTimber")
    private fun startFocusMoveAnim() {
        mScrollerFocusMoveAnim.abortAnimation()
        if(mFocusBorderView != null) {
            mFocusBorderView!!.dismissDraw()
            setLayerType(View.LAYER_TYPE_NONE, null)
            mIsDrawFocusMoveAnim = true
            if (mItemStateListener != null) {
                mItemStateListener!!.onItemViewFocusChanged(
                    false, selectedView,
                    selectedPosition
                )
            }
            mScrollerFocusMoveAnim.startScroll(0, 0, 100, 100, 200)
            invalidate()
        } else {
            Log.d(TAG, "startFocusMoveAnim: mFocusBorderView is null")
        }
    }

    fun setSelectPadding(left: Int, top: Int, right: Int, bottom: Int) {
        mFocusFrameLeft = left
        mFocusFrameTop = top
        mFocusFrameRight = right
        mFocusFrameBottom = bottom
        mFocusBorderView?.setSelectPadding(mFocusFrameLeft, mFocusFrameTop, mFocusFrameRight, mFocusFrameBottom)
    }

    /**
     * The SmoothScroller that remembers pending DPAD keys and consume pending keys
     * during scroll.
     */
    private inner class PendingMoveSmoothScroller internal constructor(context: Context?, private var mPendingMoves: Int)
        : LinearSmoothScroller(context) {

        private val mMaxPendingMoves = 10

        init {
            var targetPos: Int = selectedPosition
            if (mPendingMoves > 0) {
                targetPos += mNumRows
                val maxPos = adapter!!.itemCount - 1
                if (targetPos > maxPos) {
                    targetPos = maxPos
                }
            } else {
                targetPos -= mNumRows
                if (targetPos < 0) {
                    targetPos = 0
                }
            }
            targetPosition = targetPos
        }

        fun increasePendingMoves() {
            if (mPendingMoves < mMaxPendingMoves) {
                mPendingMoves++
            }
        }

        fun decreasePendingMoves() {
            if (mPendingMoves > -mMaxPendingMoves) {
                mPendingMoves--
            }
        }

        override fun updateActionForInterimTarget(action: Action) {
            if (mPendingMoves == 0) {
                return
            }
            super.updateActionForInterimTarget(action)
        }

        override fun computeScrollVectorForPosition(targetPosition: Int): PointF? {
            if (mPendingMoves == 0) {
                return null
            }
            val direction = if (mPendingMoves < 0) -1 else 1
            return if (mOrientation == HORIZONTAL) {
                PointF(direction.toFloat(), 0f)
            } else {
                PointF(0f, direction.toFloat())
            }
        }

        @SuppressLint("LogNotTimber")
        override fun onStop() { // if we hit wall, need clear the remaining pending moves.
            mPendingMoves = 0
            mPendingMoveSmoothScroller = null
            val targetPosition = targetPosition
            //val targetView = findViewByPosition(targetPosition)
            findViewByPosition(targetPosition).let { targetView ->
                Log.i(TAG, "PendingMoveSmoothScroller onStop: targetPos=$targetPosition==targetView=$targetView")

                if (selectedPosition != targetPosition) {
                    selectedPosition = targetPosition
                }
                if (!mIsAutoProcessFocus) {
                    targetView.requestFocus()
                } else {
                    nextFocusView = targetView
                    scrollToView(targetView, true)
                }
            }
            /*if (targetView == null) {
                super.onStop()
                return
            }
            if (selectedPosition != targetPosition) {
                selectedPosition = targetPosition
            }
            if (!mIsAutoProcessFocus) {
                targetView.requestFocus()
            } else {
                nextFocusView = targetView
                scrollToView(targetView, true)
            }*/
            super.onStop()
        }
    }

    interface OnItemStateListener {
        fun onItemViewClick(view: View?, position: Int)
        fun onItemViewFocusChanged(gainFocus: Boolean, view: View?, position: Int)
    }

    /**
     * Only works in isAutoProcessFocus attribute is true
     */
    interface OnScrollStateListener {
        fun onScrollEnd(view: View?)
        fun onScrollStart(view: View?)
    }

    companion object {
        const val TAG = "TvRecyclerView"
        var DEBUG = false
        private const val DEFAULT_SELECT_SCALE = 1.04f
        private const val NORMAL_SCROLL_COMPENSATION_VAL = 45
        private const val SCROLL_ALIGN = 0
        private const val SCROLL_FOLLOW = 1
        private const val SCROLL_NORMAL = 2
        private const val PREV_ITEM = 0
        private const val NEXT_ITEM = 1
        private const val PREV_ROW = 2
        private const val NEXT_ROW = 3
        private const val DEFAULT_DIRECTION = -1
    }
}