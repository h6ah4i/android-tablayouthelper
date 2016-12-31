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
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {

    private static final String FRAGMENT_NORMAL_TAB_DEMO = "normal tab demo";
    private static final String FRAGMENT_CUSTOMIZED_TAB_DEMO = "customized tab demo";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragment_container_1, TabLayoutDemoFragment.newInstance(false), FRAGMENT_NORMAL_TAB_DEMO)
                    .add(R.id.fragment_container_2, TabLayoutDemoFragment.newInstance(true), FRAGMENT_CUSTOMIZED_TAB_DEMO)
                    .commit();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_reset:
                ((TabLayoutDemoFragment)getSupportFragmentManager().findFragmentByTag(FRAGMENT_NORMAL_TAB_DEMO)).resetAdapter();
                ((TabLayoutDemoFragment)getSupportFragmentManager().findFragmentByTag(FRAGMENT_CUSTOMIZED_TAB_DEMO)).resetAdapter();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
