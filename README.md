TabLayoutHelper
===============

A small library which helps to use `TabLayout` with `ViewPager` more easily.

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.h6ah4i.android.tablayouthelper/tablayouthelper/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.h6ah4i.android.tablayouthelper/tablayouthelper)
[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-TabLayoutHelper-green.svg?style=flat)](https://android-arsenal.com/details/1/2273)

---

- Automatically switch `TabLayout.MODE_FIXED` and `TabLayout.MODE_SCROLLABLE` depends on total tab width.
- Easily to implement custom tab views

<a href="./pic/demo.gif?raw=true"><img src="./pic/demo.gif?raw=true" alt="Example app" /></a>

Target platforms
---

- API level 14 or later

Latest version
---

- Version 1.0.0  (September 24, 2018)

Getting started
---

This library is published on Maven Central. Just add these lines to `build.gradle`.

```gradle
dependencies {
    implementation 'com.h6ah4i.android.tablayouthelper:tablayouthelper:1.0.0'
}
```

**NOTE**
- Since v1.0.0, this library has been migrated to AndroidX. If you still need to use support libraries, use v0.9.1 instead.

Usage
---

```java
ViewPager viewPager = (ViewPager) findViewById(...);
TabLayout tabLayout = (TabLayout) findViewById(...);
PagerAdapter adapter = new XXXPagerAdapter();

viewPager.setAdapter(adapter);

// initialize the TabLayoutHelper instance
mTabLayoutHelper = new TabLayoutHelper(tabLayout, viewPager);

// [Optional] enables auto tab mode adjustment
mTabLayoutHelper.setAutoAdjustTabModeEnabled(true);
```

License
---

This library is licensed under the [Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

See [`LICENSE`](LICENSE) for full of the license text.

    Copyright (C) 2015 Haruki Hasegawa

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
