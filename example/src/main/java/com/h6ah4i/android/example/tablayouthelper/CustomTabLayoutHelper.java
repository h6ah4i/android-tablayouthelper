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

import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.h6ah4i.android.tablayouthelper.TabLayoutHelper;

public class CustomTabLayoutHelper extends TabLayoutHelper {
    private static final int[] TAB_COLOR_MAP = new int[]{
            0xfff44336, // red
            0xffe91e63, // pink
            0xff9c27b0, // purple
            0xff7e57c2, // deep purple
            0xff3f51b5, // indigo
            0xff2196f3, // blue
            0xff03a9f4, // light blue
            0xff00bcd4, // cyan
            0xff009688, // teal
            0xff4caf50, // green
            0xff8bc34a, // light green
            0xffcddc39, // lime
            0xffffeb3b, // yellow
            0xffffc107, // amber
            0xffff9800, // orange
            0xffff5722, // deep orange
    };

    public CustomTabLayoutHelper(@NonNull TabLayout tabLayout, @NonNull ViewPager viewPager) {
        super(tabLayout, viewPager);
    }

    @Override
    protected TabLayout.Tab onCreateTab(TabLayout tabLayout, PagerAdapter adapter, int position) {
        // NOTE: should not call super method here!

        TabLayout.Tab tab = tabLayout.newTab();
        tab.setText(adapter.getPageTitle(position));
        View v = LayoutInflater.from(tabLayout.getContext()).inflate(R.layout.custom_tab, tabLayout, false);
        tab.setCustomView(v);

        return tab;
    }

    @Override
    protected void onUpdateTab(TabLayout.Tab tab) {
        // NOTE: should not call super method here!

        int position = tab.getPosition();
        View v = getCustomView(tab);
        TextView tv = (TextView) v.findViewById(R.id.text);

        tv.setTextColor(Color.WHITE);
        tv.setText(tab.getText());

        v.setBackgroundColor(getTabColor(position));
    }

    private static int getTabColor(int position) {
        return TAB_COLOR_MAP[position % TAB_COLOR_MAP.length];
    }
}
