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

package com.h6ah4i.android.example.tablayouthelper;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.h6ah4i.android.tablayouthelper.TabLayoutHelper;

public class TabLayoutDemoFragment extends Fragment {
    private static final String ARG_USE_CUSTOM_TAB = "use custom tab";

    private static String KEY_SAVED_STATE_NUM_PAGES = "num_pages";

    public static TabLayoutDemoFragment newInstance(boolean useCustomTab) {
        TabLayoutDemoFragment fragment = new TabLayoutDemoFragment();
        Bundle bundle = new Bundle();
        bundle.putBoolean(ARG_USE_CUSTOM_TAB, useCustomTab);
        fragment.setArguments(bundle);
        return fragment;
    }

    private TabLayout mTabLayout;
    private ViewPager mViewPager;
    private Button mButtonAdd;
    private Button mButtonRemove;

    private TabLayoutDemoPagerAdapter mAdapter;
    private TabLayoutHelper mTabLayoutHelper;
    private boolean mUseCustomTab;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mUseCustomTab = getArguments().getBoolean(ARG_USE_CUSTOM_TAB);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // save current num pages
        outState.putInt(KEY_SAVED_STATE_NUM_PAGES, mAdapter.getCount());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(
                mUseCustomTab ? R.layout.fragment_custom_tab_layout : R.layout.fragment_normal_tab_layout,
                container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mTabLayout = (TabLayout) view.findViewById(R.id.tab_layout);
        mViewPager = (ViewPager) view.findViewById(R.id.pager);
        mButtonAdd = (Button) view.findViewById(R.id.button_add);
        mButtonRemove = (Button) view.findViewById(R.id.button_remove);

        mButtonAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleOnClickAddButton();
            }
        });
        mButtonRemove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleOnClickRemoveButton();
            }
        });


        mAdapter = new TabLayoutDemoPagerAdapter(getChildFragmentManager());

        // restore pages
        if (savedInstanceState != null) {
            int numPages = savedInstanceState.getInt(KEY_SAVED_STATE_NUM_PAGES);
            for (int i = mAdapter.getCount(); i < numPages; i++) {
                mAdapter.addPage();
            }
        }

        mViewPager.setAdapter(mAdapter);

        // initialize the TabLayoutHelper instance
        if (mUseCustomTab) {
            mTabLayoutHelper = new CustomTabLayoutHelper(mTabLayout, mViewPager);
        } else {
            mTabLayoutHelper = new TabLayoutHelper(mTabLayout, mViewPager);
        }

        // [Optional] enables auto tab mode adjustment
        mTabLayoutHelper.setAutoAdjustTabModeEnabled(true);

        // set OnTabSelectedListener
        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            private void showMessage(String message) {
                Snackbar.make(getView(), message, Snackbar.LENGTH_SHORT).show();
            }

            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                showMessage("onTabSelected(position = " + tab.getPosition() + ")");
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                showMessage("onTabUnselected(position = " + tab.getPosition() + ")");
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                showMessage("onTabReselected(position = " + tab.getPosition() + ")");
            }
        });
    }

    @Override
    public void onDestroyView() {
        // release the TabLayoutHelper instance
        if (mTabLayoutHelper != null) {
            mTabLayoutHelper.release();
            mTabLayoutHelper = null;
        }

        super.onDestroyView();
    }

    public void resetAdapter() {
        mAdapter = new TabLayoutDemoPagerAdapter(getChildFragmentManager());
        mViewPager.setAdapter(mAdapter);
    }

    private void handleOnClickAddButton() {
        mAdapter.addPage();
    }

    private void handleOnClickRemoveButton() {
        mAdapter.removePage();
    }
}
