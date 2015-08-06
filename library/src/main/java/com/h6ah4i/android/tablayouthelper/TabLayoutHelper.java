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
import android.widget.LinearLayout;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


public class TabLayoutHelper {
    private static final Method mMethodTabGetCustomView;

    protected TabLayout mTabLayout;
    protected ViewPager mViewPager;

    protected TabLayout.OnTabSelectedListener mUserOnTabSelectedListener;

    protected TabLayout.OnTabSelectedListener mInternalOnTabSelectedListener;
    protected DataSetObserver mInternalDataSetObserver;
    protected TabLayout.TabLayoutOnPageChangeListener mInternalTabLayoutOnPageChangeListener;
    protected Runnable mAdjustTabModeRunnable;
    protected boolean mAutoAdjustTabMode = false;

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
                handleOnDataDetChanged();
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

        mInternalTabLayoutOnPageChangeListener = new TabLayout.TabLayoutOnPageChangeListener(mTabLayout);

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
            adjustTabMode();
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
    protected void handleOnDataDetChanged() {
        mTabLayout.post(new Runnable() {
            @Override
            public void run() {
                setTabsFromPagerAdapter(mTabLayout, mViewPager.getAdapter(), mViewPager.getCurrentItem());
            }
        });
    }

    protected void handleOnTabSelected(TabLayout.Tab tab) {
        if (mUserOnTabSelectedListener != null) {
            mUserOnTabSelectedListener.onTabSelected(tab);
        }

        int position = tab.getPosition();
        if (mViewPager.getCurrentItem() != position) {
            mViewPager.setCurrentItem(position, true);
        }
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

    protected void cancelPendingAdjustTabMode() {
        if (mAdjustTabModeRunnable != null) {
            mTabLayout.removeCallbacks(mAdjustTabModeRunnable);
            mAdjustTabModeRunnable = null;
        }
    }

    protected void adjustTabMode() {
        if (mAdjustTabModeRunnable != null) {
            return;
        }

        if (ViewCompat.isLaidOut(mTabLayout)) {
            adjustTabModeInternal(mTabLayout);
        } else {
            mAdjustTabModeRunnable = new Runnable() {
                @Override
                public void run() {
                    mAdjustTabModeRunnable = null;
                    adjustTabModeInternal(mTabLayout);
                }
            };
            mTabLayout.post(mAdjustTabModeRunnable);
        }
    }

    private TabLayout.Tab createNewTab(TabLayout tabLayout, PagerAdapter adapter, int position) {
        return onCreateTab(tabLayout, adapter, position);
    }

    protected void setTabsFromPagerAdapter(@NonNull TabLayout tabLayout, PagerAdapter adapter, int currentItem) {
        int prevScrollX = tabLayout.getScrollX();
        int prevTabMode = tabLayout.getTabMode();
        int prevSelectedTab = tabLayout.getSelectedTabPosition();

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
            adjustTabMode();
        }

        // restore scroll position if needed
        int curTabMode = tabLayout.getTabMode();
        if (prevTabMode == TabLayout.MODE_SCROLLABLE && curTabMode == TabLayout.MODE_SCROLLABLE) {
            tabLayout.scrollTo(prevScrollX, 0);
        }
    }

    protected void updateTab(TabLayout.Tab tab) {
        onUpdateTab(tab);
    }

    protected void adjustTabModeInternal(@NonNull TabLayout tabLayout) {
        LinearLayout slidingTabStrip = (LinearLayout) tabLayout.getChildAt(0);

        tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);

        slidingTabStrip.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);

        int stripWidth = slidingTabStrip.getMeasuredWidth();
        int tabLayoutWidth = tabLayout.getMeasuredWidth();

        if (stripWidth < tabLayoutWidth) {
            tabLayout.setTabMode(TabLayout.MODE_FIXED);
        }
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
}
