package com.botty.photoviewer.adapters.galleryViewer.pictureAdapter

import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

abstract class InfiniteScrollListener(private var layoutManager: GridLayoutManager) :
    RecyclerView.OnScrollListener() {

    /*private var previousTotal =
        0 // The total number of items in the dataset after the last load
    private var loading =
        true // True if we are still waiting for the last set of data to load.
    private val visibleThreshold =
        5 // The minimum amount of items to have below your current scroll position before loading more.
    private var firstVisibleItem = 0
    private var visibleItemCount = 0
    private var totalItemCount = 0
    private var currentPage = 0

    //private val loadMore = Runnable { onLoadMore(currentPage) }

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)
        visibleItemCount = recyclerView.childCount
        totalItemCount = layoutManager.itemCount
        firstVisibleItem = layoutManager.findFirstVisibleItemPosition()
        if (loading) {
            if (totalItemCount > previousTotal || totalItemCount == 0) {
                loading = false
                previousTotal = totalItemCount
            }
        }
        // End has been reached
        if (!loading && totalItemCount - visibleItemCount <= firstVisibleItem + visibleThreshold) {
            currentPage++
            recyclerView.post(loadMore)
            loading = true
        }
    }

    abstract fun onLoaddMore(currendt_page: Int) */


    private var previousTotal =
        0 // The total number of items in the dataset after the last load
    private var loading =
        true // True if we are still waiting for the last set of data to load.

    private var visibleItemCount = 0
    private var totalItemCount = 0
    private var currentPage = 0

    //private val loadMore = Runnable { onLoadMore(currentPage) }

    /*override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)
        visibleItemCount = recyclerView.childCount
        totalItemCount = layoutManager.itemCount
        if (loading) {
            if (totalItemCount > previousTotal || totalItemCount == 0) {
                loading = false
                previousTotal = totalItemCount
            }
        }
        // End has been reached
        if (!loading && totalItemCount - visibleItemCount <= layoutManager.findLastVisibleItemPosition() + Companion.VISIBLE_THRESHOLD) {
            currentPage++
            recyclerView.post(loadMore)

            loading = true
        }
    }*/

    abstract fun onLoadMore(current_page: Int)

    companion object {
        private const val VISIBLE_THRESHOLD =
            5 // The minimum amount of items to have below your current scroll position before loading more.
    }
}