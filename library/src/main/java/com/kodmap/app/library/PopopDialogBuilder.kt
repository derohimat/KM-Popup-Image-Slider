package com.kodmap.app.library

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.support.design.widget.TabLayout
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewPager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.RelativeLayout
import com.kodmap.app.library.adapter.PopupSliderAdapter
import com.kodmap.app.library.adapter.PopupThumbAdapter
import com.kodmap.app.library.constant.ListType
import com.kodmap.app.library.constant.ScaleType
import com.kodmap.app.library.constant.ScrollType
import com.kodmap.app.library.listener.AdapterClickListener
import com.kodmap.app.library.loader.cache.disc.naming.Md5FileNameGenerator
import com.kodmap.app.library.loader.core.ImageLoader
import com.kodmap.app.library.loader.core.ImageLoaderConfiguration
import com.kodmap.app.library.loader.core.assist.QueueProcessingType
import com.kodmap.app.library.model.BaseItem
import com.kodmap.app.library.ui.KmViewPager
import java.lang.ref.WeakReference


class PopopDialogBuilder(mContext: Context) {

    private var isThumbPager = true
    private var mImageList = mutableListOf<BaseItem>()
    private var mDialogBgColor: Int = R.color.color_dialog_bg
    private var mHeaderBgColor: Int = R.color.color_dialog_bg
    private var mSliderImageScaleType = ScaleType.FIT_CENTER
    private var mCloseDrawable: Int = R.drawable.ic_close_white_24dp
    private var mLoadingView: View? = null
    private var mDialogStyle: Int = R.style.KmPopupDialog
    private var mSelectorIndicator: Int = R.drawable.indicator_selector
    private var mIsZoomable: Boolean = false

    private lateinit var mAdapterClickListener: AdapterClickListener
    private lateinit var mDialog: Dialog
    private lateinit var mSliderAdapter: PopupSliderAdapter
    private lateinit var mDialogView: View
    private lateinit var mRvThumb: RecyclerView
    private lateinit var mImagePager: KmViewPager
    private lateinit var mThumbAdapter: PopupThumbAdapter
    private var weakContext: WeakReference<Context> = WeakReference(mContext)

    fun setIsZoomable(bool: Boolean): PopopDialogBuilder {
        mIsZoomable = bool
        return this
    }

    fun setSelectorIndicator(draw: Int): PopopDialogBuilder {
        mSelectorIndicator = draw
        return this
    }

    fun setDialogStyle(style: Int): PopopDialogBuilder {
        mDialogStyle = style
        return this
    }

    fun setLoadingView(viewId: Int): PopopDialogBuilder {
        weakContext.get()?.let { mLoadingView = View.inflate(it, viewId, null) }
        if (mLoadingView == null)
            throw IllegalArgumentException("View could not be inflate")
        else
            return this
    }

    fun setHeaderBackgroundColor(color: Int): PopopDialogBuilder {
        mHeaderBgColor = color
        return this
    }

    fun setCloseDrawable(closeIcon: Int): PopopDialogBuilder {
        mCloseDrawable = closeIcon
        return this
    }

    fun setSliderImageScaleType(type: ImageView.ScaleType): PopopDialogBuilder {
        mSliderImageScaleType = type
        return this
    }

    fun setDialogBackgroundColor(color: Int): PopopDialogBuilder {
        this.mDialogBgColor = color
        return this
    }

    fun setList(imageList: List<Any>?): PopopDialogBuilder {
        this.mImageList.clear()
        if (imageList == null) {
            throw  IllegalArgumentException("List must not be null")
        } else if (imageList.isEmpty()) {
            throw IllegalArgumentException("List must not be empty")
        } else if (imageList[0] is Int) {
            fillImageList(imageList, ListType.Drawable)
            return this
        } else if (imageList[0] is String) {
            fillImageList(imageList, ListType.Url)
            return this
        } else if (imageList[0] is BaseItem) {
            fillImageList(imageList, ListType.BaseItem)
            return this
        } else {
            throw IllegalArgumentException("List contains unsupported type. List contains drawable id (Integer)" +
                    ", image link (String) or BaseItem")
        }
    }

    fun showThumbSlider(isShow: Boolean): PopopDialogBuilder {
        isThumbPager = isShow
        return this
    }

    fun build(): Dialog {
        initListener()
        initDialogView()
        initImageViewPager()
        if (isThumbPager) {
            initThumbReclerView()
        } else {
            initTabLayout()
        }
        initHeader()
        createDialog()
        return mDialog
    }

    private fun initTabLayout() {
        weakContext.get()?.let {
            val tabLayout = mDialogView.findViewById<TabLayout>(R.id.km_tab_layout)
            tabLayout.visibility = View.VISIBLE
            tabLayout.setupWithViewPager(mImagePager)

            if (mSelectorIndicator != R.drawable.indicator_selector) {
                val tabLayoutView = tabLayout.getChildAt(0) as ViewGroup
                for (i in 0 until tabLayout.tabCount) {
                    val tab = tabLayoutView.getChildAt(i) as View
                    tab.background = ContextCompat.getDrawable(it, mSelectorIndicator)
                }
            }
        }
    }

    private fun initHeader() {
        weakContext.get()?.let {
            val headerLayout = mDialogView.findViewById<RelativeLayout>(R.id.km_header_layout)
            headerLayout.setBackgroundColor(ContextCompat.getColor(it, mHeaderBgColor))

            val btn_close = mDialogView.findViewById<ImageView>(R.id.km_iv_close)
            btn_close.setImageDrawable(ContextCompat.getDrawable(it, mCloseDrawable))
            btn_close.setOnClickListener { listener ->
                mDialog.dismiss()
            }
        }
    }

    private fun initImageViewPager() {
        mImagePager = mDialogView.findViewById(R.id.km_view_pager)
        mSliderAdapter = PopupSliderAdapter()
        mSliderAdapter.setLoadingView(mLoadingView)
        mSliderAdapter.setScaleType(mSliderImageScaleType)
        mSliderAdapter.setIsZoomable(mIsZoomable)
        mImagePager.adapter = mSliderAdapter
        (mImagePager.adapter as PopupSliderAdapter).setItemList(mImageList)
        mImagePager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                if (isThumbPager) {
                    setScrollSync(position, ScrollType.PagerScroll)
                }
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })
    }

    private fun initThumbReclerView() {
        weakContext.get()?.let {
            mThumbAdapter = PopupThumbAdapter(mAdapterClickListener)
            mRvThumb = mDialogView.findViewById(R.id.km_rv_thumb)
            mRvThumb.visibility = View.VISIBLE
            mImageList[0].isSelected = true
            mThumbAdapter.setList(mImageList)
            mThumbAdapter.setLoadingView(mLoadingView)
            val layoutManager = LinearLayoutManager(it)
            layoutManager.orientation = LinearLayoutManager.HORIZONTAL
            mRvThumb.layoutManager = layoutManager
            mRvThumb.adapter = mThumbAdapter
            mRvThumb.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        val center = mRvThumb.width / 2
                        val centerView = mRvThumb.findChildViewUnder(center.toFloat(), mRvThumb.left.toFloat())
                        if (centerView != null) {
                            val centerPos = mRvThumb.getChildAdapterPosition(centerView)
                            val firstVisibleItemPosition = (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                            val lastVisibleItemPosition = (recyclerView.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
                            if ((firstVisibleItemPosition != 0 || centerPos < (recyclerView.adapter as PopupThumbAdapter).oldSelectedPosition) && (lastVisibleItemPosition != mImageList.size - 1 || centerPos > (recyclerView.adapter as PopupThumbAdapter).oldSelectedPosition)) {
                                setScrollSync(centerPos, ScrollType.RecyclerViewScroll)
                            }
                        }
                        mImagePager.setSwipeLocked(false)
                    } else if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                        mImagePager.setSwipeLocked(true)
                    }
                }
            })
        }
    }

    private fun initDialogView() {
        weakContext.get()?.let {
            mDialogView = View.inflate(it, R.layout.km_dialog_popup, null)
        }
    }

    private fun initListener() {
        mAdapterClickListener = object : AdapterClickListener {
            override fun onClick(position: Int) {
                mImagePager.currentItem = position
            }
        }
    }

    private fun createDialog() {
        weakContext.get()?.let {
            mDialog = Dialog(it, mDialogStyle)
            mDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

            initLoader()

            val lp = WindowManager.LayoutParams()
            val manager = it.getSystemService(Activity.WINDOW_SERVICE) as WindowManager
            val displaymetrics = DisplayMetrics()
            manager.defaultDisplay.getMetrics(displaymetrics);
            lp.width = displaymetrics.widthPixels
            lp.height = displaymetrics.heightPixels

            mDialog.window!!.attributes = lp
            mDialog.window!!.setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(it, mDialogBgColor)))

            mDialog.setContentView(mDialogView)
            mDialog.setCancelable(true)
        }
    }

    private fun initLoader() {
        weakContext.get()?.let {
            val config = ImageLoaderConfiguration.Builder(it.applicationContext)
            config.threadPriority(Thread.NORM_PRIORITY - 2)
            config.denyCacheImageMultipleSizesInMemory()
            config.diskCacheFileNameGenerator(Md5FileNameGenerator())
            config.diskCacheSize(20 * 1024 * 1024)
            config.tasksProcessingOrder(QueueProcessingType.LIFO)
            // Initialize ImageLoader with configuration.
            ImageLoader.getInstance().init(config.build())
        }
    }

    private fun setScrollSync(position: Int, type: Int) {
        val prevCenterPos = (mRvThumb.adapter as PopupThumbAdapter).oldSelectedPosition
        if (prevCenterPos != position) {
            if (type == ScrollType.RecyclerViewScroll) {
                val center = mRvThumb.width / 2
                val centerView = mRvThumb.findChildViewUnder(center.toFloat(), mRvThumb.left.toFloat())
                mRvThumb.smoothScrollBy(centerView!!.left - center + centerView.width / 2, 0, AccelerateDecelerateInterpolator())
                mImagePager.currentItem = position
            } else if (type == ScrollType.PagerScroll) {
                val view = mRvThumb.layoutManager!!.findViewByPosition(position)
                val middle = mRvThumb.width / 2
                if (view == null) {
                    mRvThumb.scrollToPosition(position)
                } else {
                    mRvThumb.smoothScrollBy(view.left - middle + view.width / 2, 0, AccelerateDecelerateInterpolator())
                }
            }
            (mRvThumb.adapter as PopupThumbAdapter).changeSelectedItem(position)
        }
    }

    private fun fillImageList(imageList: List<Any>, listType: Int) {
        when (listType) {
            ListType.BaseItem -> {
                imageList.forEach {
                    (it as BaseItem).isSelected = false
                    this.mImageList.add(it)
                }
            }
            ListType.Url -> {
                imageList.forEach {
                    val item = BaseItem()
                    item.imageUrl = it as String
                    this.mImageList.add(item)
                }
            }
            ListType.Drawable -> {
                imageList.forEach {
                    val item = BaseItem()
                    item.drawableId = it as Int
                    this.mImageList.add(item)
                }
            }
        }
    }
}
