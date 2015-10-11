/*
 *    Copyright (C) 2015 Haruki Hasegawa
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.h6ah4i.android.tablayouthelper;

import android.database.DataSetObserver;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


public class TabLayoutHelper {
    private static final Method mMethodTabGetCustomView;

    protected TabLayout mTabLayout;
    protected ViewPager mViewPager;

    protected TabLayout.OnTabSelectedListener mUserOnTabSelectedListener;

    protected TabLayout.OnTabSelectedListener mInternalOnTabSelectedListener;
    protected DataSetObserver mInternalDataSetObserver;
    protected FixedTabLayoutOnPageChangeListener mInternalTabLayoutOnPageChangeListener;
    protected Runnable mAdjustTabModeRunnable;
    protected Runnable mSetTabsFromPagerAdapterRunnable;
    protected Runnable mUpdateScrollPositionRunnable;
    protected boolean mAutoAdjustTabMode = false;
    protected boolean mIsInTabSelectedContext = false;
    protected View.OnClickListener mInternalTabOnClickListener;


    static {
        try {
            mMethodTabGetCustomView = TabLayout.Tab.class.getDeclaredMethod("getCustomView");
            mMethodTabGetCustomView.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Constructor.
     *
     * @param tabLayout TabLayout instance
     * @param viewPager ViewPager instance
     */
    public TabLayoutHelper(@NonNull TabLayout tabLayout, @NonNull ViewPager viewPager) {
        PagerAdapter adapter = viewPager.getAdapter();

        if (adapter == null) {
            throw new IllegalArgumentException("ViewPager does not have a PagerAdapter set");
        }

        mTabLayout = tabLayout;
        mViewPager = viewPager;

        mInternalDataSetObserver = new DataSetObserver() {
            @Override
            public void onChanged() {
                handleOnDataSetChanged();
            }
        };

        mInternalOnTabSelectedListener = new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                handleOnTabSelected(tab);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                handleOnTabUnselected(tab);
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                handleOnTabReselected(tab);
            }
        };

        mInternalTabLayoutOnPageChangeListener = new FixedTabLayoutOnPageChangeListener(mTabLayout);

        mInternalTabOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleOnTabClick(v);
            }
        };

        Internal.setTabOnClickListener(mTabLayout, mInternalTabOnClickListener);

        viewPager.getAdapter().registerDataSetObserver(mInternalDataSetObserver);
        mTabLayout.setOnTabSelectedListener(mInternalOnTabSelectedListener);
        viewPager.addOnPageChangeListener(mInternalTabLayoutOnPageChangeListener);

        setTabsFromPagerAdapter(tabLayout, adapter, viewPager.getCurrentItem());
    }

    //
    // public methods
    //

    /**
     * Retrieve underlying TabLayout instance.
     *
     * @return TabLayout instance
     */
    public TabLayout getTabLayout() {
        return mTabLayout;
    }

    /**
     * Retrieve ViewPager instance.
     *
     * @return ViewPager instance
     */
    public ViewPager getViewPager() {
        return mViewPager;
    }

    /**
     * Sets auto tab mode adjustment enabled
     *
     * @param enabled True for enabled, otherwise false.
     */
    public void setAutoAdjustTabModeEnabled(boolean enabled) {
        if (mAutoAdjustTabMode == enabled) {
            return;
        }
        mAutoAdjustTabMode = enabled;

        if (mAutoAdjustTabMode) {
            adjustTabMode(-1);
        } else {
            cancelPendingAdjustTabMode();
        }
    }

    /**
     * Gets whether auto tab mode adjustment is enabled.
     *
     * @return True for enabled, otherwise false.
     */
    public boolean isAutoAdjustTabModeEnabled() {
        return mAutoAdjustTabMode;
    }

    /**
     * Sets {@link android.support.design.widget.TabLayout.OnTabSelectedListener}
     *
     * @param listener Listener
     */
    public void setOnTabSelectedListener(TabLayout.OnTabSelectedListener listener) {
        mUserOnTabSelectedListener = listener;
    }

    /**
     * Unregister internal listener objects, release object references, etc.
     * This method should be called in order to avoid memory leaks.
     */
    public void release() {
        cancelPendingAdjustTabMode();
        cancelPendingSetTabsFromPagerAdapter();
        cancelPendingUpdateScrollPosition();

        if (mInternalDataSetObserver != null) {
            mViewPager.getAdapter().unregisterDataSetObserver(mInternalDataSetObserver);
            mInternalDataSetObserver = null;
        }
        if (mInternalOnTabSelectedListener != null) {
            mTabLayout.setOnTabSelectedListener(null);
            mInternalOnTabSelectedListener = null;
        }
        if (mInternalTabLayoutOnPageChangeListener != null) {
            mViewPager.removeOnPageChangeListener(mInternalTabLayoutOnPageChangeListener);
            mInternalTabLayoutOnPageChangeListener = null;
        }
        mUserOnTabSelectedListener = null;
        mViewPager = null;
        mTabLayout = null;
    }

    public void updateAllTabs() {
        int count = mTabLayout.getTabCount();
        for (int i = 0; i < count; i++) {
            updateTab(mTabLayout.getTabAt(i));
        }
    }

    /**
     * Override this method if you want to use custom tab layout.
     *
     * @param tabLayout TabLayout
     * @param adapter   PagerAdapter
     * @param position  Position of the item
     * @return TabLayout.Tab
     */
    protected TabLayout.Tab onCreateTab(TabLayout tabLayout, PagerAdapter adapter, int position) {
        TabLayout.Tab tab = tabLayout.newTab();
        tab.setText(adapter.getPageTitle(position));
        return tab;
    }

    /**
     * Override this method if you want to use custom tab layout
     *
     * @param tab Tab
     */
    protected void onUpdateTab(TabLayout.Tab tab) {
        tab.setCustomView(null); // invokes update() method internally.
    }

    //
    // internal methods
    //
    protected void handleOnDataSetChanged() {
        cancelPendingUpdateScrollPosition();

        if (mSetTabsFromPagerAdapterRunnable == null) {
            mSetTabsFromPagerAdapterRunnable = new Runnable() {
                @Override
                public void run() {
                    setTabsFromPagerAdapter(mTabLayout, mViewPager.getAdapter(), mViewPager.getCurrentItem());
                }
            };
        }

        mTabLayout.post(mSetTabsFromPagerAdapterRunnable);
    }

    protected void handleOnTabSelected(TabLayout.Tab tab) {
        cancelPendingUpdateScrollPosition();

        mIsInTabSelectedContext = true;
        if (mUserOnTabSelectedListener != null) {
            mUserOnTabSelectedListener.onTabSelected(tab);
        }

        int position = tab.getPosition();
        if (mViewPager.getCurrentItem() != position) {
            mViewPager.setCurrentItem(position, true);
        }
        mIsInTabSelectedContext = false;
    }

    protected void handleOnTabUnselected(TabLayout.Tab tab) {
        if (mUserOnTabSelectedListener != null) {
            mUserOnTabSelectedListener.onTabUnselected(tab);
        }
    }

    protected void handleOnTabReselected(TabLayout.Tab tab) {
        if (mUserOnTabSelectedListener != null) {
            mUserOnTabSelectedListener.onTabReselected(tab);
        }
    }

    protected void handleOnTabClick(View v) {
        ViewGroup tabStrip = (ViewGroup) v.getParent();
        int tabCount = tabStrip.getChildCount();
        int tabIndex = -1;

        for (int i = 0; i < tabCount; i++) {
            if (tabStrip.getChildAt(i) == v) {
                tabIndex = i;
                break;
            }
        }

        if (tabIndex >= 0) {
            // consume the pending selection here to avoid invoking the tab re-selected state
            mInternalTabLayoutOnPageChangeListener.consumePendingSelection(mTabLayout);

            TabLayout.Tab tab = Internal.safeGetTabAt(mTabLayout, tabIndex);
            if (tab != null) {
                Internal.selectTab(mTabLayout, tab);
            }

            mInternalTabLayoutOnPageChangeListener.clearPendingSelection();
        }
    }

    protected void cancelPendingAdjustTabMode() {
        if (mAdjustTabModeRunnable != null) {
            mTabLayout.removeCallbacks(mAdjustTabModeRunnable);
            mAdjustTabModeRunnable = null;
        }
    }

    protected void cancelPendingSetTabsFromPagerAdapter() {
        if (mSetTabsFromPagerAdapterRunnable != null) {
            mTabLayout.removeCallbacks(mSetTabsFromPagerAdapterRunnable);
            mSetTabsFromPagerAdapterRunnable = null;
        }
    }

    protected void cancelPendingUpdateScrollPosition() {
        if (mUpdateScrollPositionRunnable != null) {
            mTabLayout.removeCallbacks(mUpdateScrollPositionRunnable);
            mUpdateScrollPositionRunnable = null;
        }
    }

    protected void adjustTabMode(int prevScrollX) {
        if (mAdjustTabModeRunnable != null) {
            return;
        }

        if (prevScrollX < 0) {
            prevScrollX = mTabLayout.getScrollX();
        }

        if (ViewCompat.isLaidOut(mTabLayout)) {
            adjustTabModeInternal(mTabLayout, prevScrollX);
        } else {
            final int prevScrollX1 = prevScrollX;
            mAdjustTabModeRunnable = new Runnable() {
                @Override
                public void run() {
                    mAdjustTabModeRunnable = null;
                    adjustTabModeInternal(mTabLayout, prevScrollX1);
                }
            };
            mTabLayout.post(mAdjustTabModeRunnable);
        }
    }

    private TabLayout.Tab createNewTab(TabLayout tabLayout, PagerAdapter adapter, int position) {
        return onCreateTab(tabLayout, adapter, position);
    }

    protected void setTabsFromPagerAdapter(@NonNull TabLayout tabLayout, PagerAdapter adapter, int currentItem) {
        int prevSelectedTab = tabLayout.getSelectedTabPosition();
        int prevScrollX = tabLayout.getScrollX();

        // remove all tabs
        tabLayout.removeAllTabs();

        // add tabs
        int count = adapter.getCount();
        for (int i = 0; i < count; i++) {
            TabLayout.Tab tab = createNewTab(tabLayout, adapter, i);
            tabLayout.addTab(tab, false);
            updateTab(tab);
        }

        // select current tab
        currentItem = Math.min(currentItem, count - 1);
        if (currentItem >= 0) {
            if (prevSelectedTab == currentItem) {
                tabLayout.setOnTabSelectedListener(null);
            }
            tabLayout.getTabAt(currentItem).select();
            if (prevSelectedTab == currentItem) {
                tabLayout.setOnTabSelectedListener(mInternalOnTabSelectedListener);
            }
        }

        // adjust tab mode & gravity
        if (mAutoAdjustTabMode) {
            adjustTabMode(prevScrollX);
        } else {
            // restore scroll position if needed
            int curTabMode = tabLayout.getTabMode();
            if (curTabMode == TabLayout.MODE_SCROLLABLE) {
                tabLayout.scrollTo(prevScrollX, 0);
            }
        }
    }

    protected void updateTab(TabLayout.Tab tab) {
        onUpdateTab(tab);
    }

    protected void adjustTabModeInternal(@NonNull TabLayout tabLayout, int prevScrollX) {
        LinearLayout slidingTabStrip = (LinearLayout) tabLayout.getChildAt(0);
        int prevTabMode = tabLayout.getTabMode();

        tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
        tabLayout.setTabGravity(TabLayout.GRAVITY_CENTER);

        slidingTabStrip.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);

        int stripWidth = slidingTabStrip.getMeasuredWidth();
        int tabLayoutWidth = tabLayout.getMeasuredWidth();

        cancelPendingUpdateScrollPosition();

        if (stripWidth < tabLayoutWidth) {
            tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
            tabLayout.setTabMode(TabLayout.MODE_FIXED);
        } else {
            if (prevTabMode == TabLayout.MODE_SCROLLABLE) {
                // restore scroll position
                tabLayout.scrollTo(prevScrollX, 0);
            } else {
                // scroll to current selected tab
                mUpdateScrollPositionRunnable = new Runnable() {
                    @Override
                    public void run() {
                        mUpdateScrollPositionRunnable = null;
                        updateScrollPosition();
                    }
                };
                mTabLayout.post(mUpdateScrollPositionRunnable);
            }
        }
    }

    private void updateScrollPosition() {
        mTabLayout.setScrollPosition(mTabLayout.getSelectedTabPosition(), 0, false);
    }

    protected static View getCustomView(TabLayout.Tab tab) {
        try {
            return (View) mMethodTabGetCustomView.invoke(tab);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    protected static class FixedTabLayoutOnPageChangeListener implements ViewPager.OnPageChangeListener {
        private final WeakReference<TabLayout> mTabLayoutRef;
        private int mPendingSelection = -1;
        private int mScrollState;

        public FixedTabLayoutOnPageChangeListener(TabLayout tabLayout) {
            mTabLayoutRef = new WeakReference<>(tabLayout);
        }

        public void onPageScrollStateChanged(int state) {
            TabLayout tabLayout = (TabLayout) mTabLayoutRef.get();

            mScrollState = state;
            if (mScrollState == 0) {
                consumePendingSelection(tabLayout);
            }
        }

        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            TabLayout tabLayout = (TabLayout) mTabLayoutRef.get();
            if (tabLayout != null) {
                if (mPendingSelection == -1 || Internal.getScrollPosition(tabLayout) != (float) mPendingSelection) {
                    tabLayout.setScrollPosition(position, positionOffset, true);
                }
            }
        }

        public void onPageSelected(int position) {
            mPendingSelection = position;
        }

        public void clearPendingSelection() {
            mPendingSelection = -1;
        }

        public void consumePendingSelection(TabLayout tabLayout) {
            if (mPendingSelection == -1) {
                return;
            }

            if (tabLayout.getSelectedTabPosition() != mPendingSelection) {
                Internal.selectTab(tabLayout, tabLayout.getTabAt(mPendingSelection));
            }
            mPendingSelection = -1;
        }
    }

    static class Internal {
        private static final Method mGetScrollPosition;
        private static final Field mTabClickListener;

        static {
            mGetScrollPosition = getAccessiblePrivateMethod("getScrollPosition");
            mTabClickListener = getAccessiblePrivateField("mTabClickListener");
        }

        private static Method getAccessiblePrivateMethod(String methodName) throws RuntimeException {
            try {
                Method m = TabLayout.class.getDeclaredMethod(methodName);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(e);
            }
        }

        private static Field getAccessiblePrivateField(String fieldName) throws RuntimeException {
            try {
                Field f = TabLayout.class.getDeclaredField(fieldName);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException e) {
                throw new IllegalStateException(e);
            }
        }

        public static void selectTab(TabLayout tabLayout, TabLayout.Tab tab) {
            tab.select();
        }

        public static void setTabOnClickListener(TabLayout tabLayout, View.OnClickListener listener) {
            try {
                mTabClickListener.set(tabLayout, listener);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        public static float getScrollPosition(TabLayout tabLayout) {
            try {
                return (Float) mGetScrollPosition.invoke(tabLayout);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            } catch (InvocationTargetException e) {
                Throwable targetException = e.getTargetException();
                if (targetException instanceof RuntimeException) {
                    throw (RuntimeException) targetException;
                } else {
                    throw new IllegalStateException(targetException);
                }
            }
        }

        public static TabLayout.Tab safeGetTabAt(TabLayout tabLayout, int index) {
            int tabCount = tabLayout.getTabCount();

            if (index >= 0 && index < tabCount) {
                return tabLayout.getTabAt(index);
            } else {
                return null;
            }
        }
    }
}
