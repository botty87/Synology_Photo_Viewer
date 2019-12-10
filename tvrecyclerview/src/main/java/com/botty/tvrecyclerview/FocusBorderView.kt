package com.botty.tvrecyclerview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.util.Log
import android.view.View
import android.widget.Scroller

class FocusBorderView(context: Context?) : View(context) {
    private var mTvRecyclerView: TvRecyclerView? = null
    private val mScroller: Scroller = Scroller(context)
    private var mScaleX = 0f
    private var mScaleY = 0f
    private var mIsDrawGetFocusAnim = false
    private var mIsClicked = false
    private var mLeftFocusBoundWidth = 0
    private var mTopFocusBoundWidth = 0
    private var mRightFocusBoundWidth = 0
    private var mBottomFocusBoundWidth = 0

    var drawBorder = true

    var tvRecyclerView: TvRecyclerView?
        get() = mTvRecyclerView
        set(tvRecyclerView) {
            if (mTvRecyclerView == null) {
                mTvRecyclerView = tvRecyclerView
            }
        }

    companion object {
        private const val TAG = "TvRecyclerView.FB"
    }

    fun setSelectPadding(left: Int, top: Int, right: Int, bottom: Int) {
        mLeftFocusBoundWidth = left
        mTopFocusBoundWidth = top
        mRightFocusBoundWidth = right
        mBottomFocusBoundWidth = bottom
    }

    @SuppressLint("LogNotTimber")
    fun startFocusAnim() {
        mTvRecyclerView?.run {
            setLayerType(LAYER_TYPE_NONE, null)
            selectedView?.run {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "startFocusAnim: start focus animation")
                }
                mIsDrawGetFocusAnim = true
                mScroller.abortAnimation()
                mScroller.startScroll(0, 0, 100, 100, 245)
                invalidate()
            }
        }
    }

    fun dismissGetFocus() {
        mIsDrawGetFocusAnim = false
    }

    fun dismissDraw() {
        mScroller.abortAnimation()
    }

    @SuppressLint("LogNotTimber")
    fun startClickAnim() {
        mTvRecyclerView?.run {
            setLayerType(LAYER_TYPE_NONE, null)
            val indexChild = mTvRecyclerView!!.selectedPosition
            if (indexChild >= 0 && indexChild < mTvRecyclerView!!.adapter!!.itemCount) {
                selectedView?.run {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "startClickAnim: start click animation")
                    }
                    mIsClicked = true
                    mScroller.abortAnimation()
                    mScroller.startScroll(0, 0, 100, 100, 200)
                    invalidate()
                }
            }
        }
    }

    override fun computeScroll() {
        if (mScroller.computeScrollOffset()) {
            val scaleValue = mTvRecyclerView!!.selectedScaleValue
            if (mIsDrawGetFocusAnim) { // calculate scale when get focus animation
                mScaleX = (scaleValue - 1) * mScroller.currX / 100 + 1
                mScaleY = (scaleValue - 1) * mScroller.currY / 100 + 1
            } else if (mIsClicked) { // calculate scale when key down animation
                mScaleX = scaleValue - (scaleValue - 1) * mScroller.currX / 100
                mScaleY = scaleValue - (scaleValue - 1) * mScroller.currY / 100
            }
            invalidate()
        } else {
            if (mIsDrawGetFocusAnim) {
                mIsDrawGetFocusAnim = false
                if (mTvRecyclerView != null) {
                    mTvRecyclerView!!.setLayerType(mTvRecyclerView!!.mLayerType, null)
                    invalidate()
                }
            } else if (mIsClicked) {
                mIsClicked = false
                if (mTvRecyclerView != null) {
                    mTvRecyclerView!!.setLayerType(mTvRecyclerView!!.mLayerType, null)
                    invalidate()
                }
            }
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        if (mTvRecyclerView?.hasFocus() == true && drawBorder) {
            drawGetFocusOrClickScaleAnim(canvas)
            drawFocusMoveAnim(canvas)
            drawFocus(canvas)
        }
    }

    @SuppressLint("LogNotTimber")
    private fun drawGetFocusOrClickScaleAnim(canvas: Canvas) {
        if (mIsDrawGetFocusAnim || mIsClicked) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG,
                    "drawGetFocusOrClickScaleAnim: ==isClicked=$mIsClicked=GetFocusAnim=$mIsDrawGetFocusAnim")
            }

            val itemView = mTvRecyclerView?.selectedView ?: return
            val itemWidth = itemView.width
            val itemHeight = itemView.height
            val location = IntArray(2)
            itemView.getLocationInWindow(location)
            val drawLocation = IntArray(2)
            getLocationInWindow(drawLocation)
            // draw focus image
            mTvRecyclerView?.drawableFocus?.run {
                val focusWidth = itemWidth + mLeftFocusBoundWidth + mRightFocusBoundWidth
                val focusHeight = itemHeight + mTopFocusBoundWidth + mBottomFocusBoundWidth
                canvas.save()
                canvas.translate(
                    location[0] - mLeftFocusBoundWidth.toFloat(),
                    location[1] - drawLocation[1] - mTopFocusBoundWidth.toFloat()
                )
                canvas.scale(mScaleX, mScaleY, itemWidth / 2.toFloat(), itemHeight / 2.toFloat())
                this.setBounds(0, 0, focusWidth, focusHeight)
                this.draw(canvas)
                canvas.restore()
            }
            // draw item view
            canvas.save()
            canvas.translate(location[0].toFloat(), location[1].toFloat())
            canvas.scale(mScaleX, mScaleY, itemWidth / 2.toFloat(), itemHeight / 2.toFloat())
            itemView.draw(canvas)
            canvas.restore()
        }
    }

    @SuppressLint("LogNotTimber")
    private fun drawFocusMoveAnim(canvas: Canvas) {
        if (mTvRecyclerView!!.mIsDrawFocusMoveAnim) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "drawFocusMoveAnim: ==============")
            }
            mScroller.abortAnimation()
            val curView = mTvRecyclerView?.selectedView
            val nextView = mTvRecyclerView?.nextFocusView
            if (nextView != null && curView != null) {
                val locationDrawLayout = IntArray(2)
                getLocationInWindow(locationDrawLayout)
                val location = IntArray(2)
                nextView.getLocationInWindow(location)
                val nextLeft = location[0]
                val nextTop = location[1] - locationDrawLayout[1]
                val nextWidth = nextView.width
                val nextHeight = nextView.height
                curView.getLocationInWindow(location)
                val curLeft = location[0]
                val curTop = location[1] - locationDrawLayout[1]
                val curWidth = curView.width
                val curHeight = curView.height
                val animScale = mTvRecyclerView!!.focusMoveAnimScale
                val focusLeft = curLeft + (nextLeft - curLeft) * animScale
                val focusTop = curTop + (nextTop - curTop) * animScale
                val focusWidth = curWidth + (nextWidth - curWidth) * animScale
                val focusHeight = curHeight + (nextHeight - curHeight) * animScale

                // draw focus image
                val scaleValue = mTvRecyclerView!!.selectedScaleValue

                mTvRecyclerView?.drawableFocus?.run {
                    canvas.save()
                    canvas.translate(
                        focusLeft - (scaleValue - 1) / 2 * focusWidth,
                        focusTop - (scaleValue - 1) / 2 * focusHeight
                    )
                    canvas.scale(scaleValue, scaleValue, 0f, 0f)
                    this.setBounds(
                        0 - mLeftFocusBoundWidth,
                        0 - mTopFocusBoundWidth,
                        (focusWidth + mRightFocusBoundWidth).toInt(),
                        (focusHeight + mBottomFocusBoundWidth).toInt()
                    )
                    this.draw(canvas)
                    canvas.restore()
                }

                //I you want to anim all the view restore!
                // draw next item view
                /*canvas.save()
                canvas.translate(
                    focusLeft - (scaleValue - 1) / 2 * focusWidth,
                    focusTop - (scaleValue - 1) / 2 * focusHeight
                )
                canvas.scale(
                    scaleValue * focusWidth / nextWidth,
                    scaleValue * focusHeight / nextHeight, 0f, 0f
                )
                canvas.saveLayerAlpha(
                    RectF(0f, 0f, width.toFloat(), height.toFloat()),
                    (0xFF * animScale).toInt())
                nextView.draw(canvas)
                canvas.restore()
                canvas.restore()
                // draw current item view
                canvas.save()
                canvas.translate(
                    focusLeft - (scaleValue - 1) / 2 * focusWidth,
                    focusTop - (scaleValue - 1) / 2 * focusHeight
                )
                canvas.scale(
                    scaleValue * focusWidth / curWidth,
                    scaleValue * focusHeight / curHeight, 0f, 0f
                )
                canvas.saveLayerAlpha(
                    RectF(0f, 0f, width.toFloat(), height.toFloat()),
                    (0xFF * (1 - animScale)).toInt())
                curView.draw(canvas)
                canvas.restore()
                canvas.restore()*/
            }
        }
    }

    @SuppressLint("LogNotTimber")
    private fun drawFocus(canvas: Canvas) {
        if (!mIsDrawGetFocusAnim && !mTvRecyclerView!!.mIsDrawFocusMoveAnim && !mIsClicked) {
            mTvRecyclerView?.selectedView?.run {
                val itemLocation = IntArray(2)
                this.getLocationInWindow(itemLocation)
                Log.i(
                    TAG,
                    "drawFocus: ===itemLocationX===" + itemLocation[0] +
                            "===itemLocationY==" + itemLocation[1]
                )
                val itemWidth = this.width
                val itemHeight = this.height
                val scaleValue = mTvRecyclerView!!.selectedScaleValue
                val itemPositionX =
                    itemLocation[0] - (scaleValue - 1) / 2 * itemWidth
                val itemPositionY =
                    itemLocation[1] - (scaleValue - 1) / 2 * itemHeight
                Log.i(
                    TAG,
                    "drawFocus: ======itemPositionX=====" + itemPositionX +
                            "===itemPositionY===" + itemPositionY
                )

                /*
                //draw focus image
                val drawableFocus = mTvRecyclerView!!.drawableFocus
                val drawWidth = itemWidth + mLeftFocusBoundWidth + mRightFocusBoundWidth
                val drawHeight = itemHeight + mTopFocusBoundWidth + mBottomFocusBoundWidth
                val drawPositionX = itemPositionX - scaleValue * mLeftFocusBoundWidth
                val drawPositionY = itemPositionY - scaleValue * mTopFocusBoundWidth
                Log.i(TAG, "drawFocus: ===drawPositionX==$drawPositionX===drawPositionY===$drawPositionY")
                mTvRecyclerView?.drawableFocus?.run {
                    canvas.save()
                    canvas.translate(drawPositionX, drawPositionY)
                    canvas.scale(scaleValue, scaleValue, 0f, 0f)
                    this.setBounds(0, 0, drawWidth, drawHeight)
                    this.draw(canvas)
                    canvas.restore()
                }
                // draw item view
                canvas.save()
                canvas.translate(itemPositionX, itemPositionY)
                canvas.scale(scaleValue, scaleValue, 0f, 0f)
                this.draw(canvas)
                canvas.restore() */

                val drawableFocus = mTvRecyclerView!!.drawableFocus
                val drawWidth = itemWidth + mLeftFocusBoundWidth + mRightFocusBoundWidth
                val drawHeight = itemHeight + mTopFocusBoundWidth + mBottomFocusBoundWidth
                val drawPositionX = itemPositionX - scaleValue * mLeftFocusBoundWidth
                val drawPositionY = itemPositionY - scaleValue * mTopFocusBoundWidth
                Log.i(TAG, "drawFocus: ===drawPositionX==$drawPositionX===drawPositionY===$drawPositionY")
                mTvRecyclerView?.drawableFocus?.run {
                    canvas.save()
                    canvas.translate(drawPositionX, drawPositionY)
                    canvas.scale(scaleValue, scaleValue, 0f, 0f)
                    this.setBounds(0, 0, drawWidth, drawHeight)
                    this.draw(canvas)
                    canvas.restore()
                }
            }
        }
    }
}