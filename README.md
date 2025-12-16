## **SmartBottomSheetLibrary**
---
A customizable **Android Smart Bottom Sheet library**.
It starts with a **normal fixed height** and **expands to full screen**only when the user scrolls or drags, not immediately on open.

This library gives you **smooth animations, scroll-aware expansion, blur background, and corner radius** customization with simple XML usage.

---

## **✨ Features**
- Initial fixed height (does NOT cover full screen by default)

- Expands to full screen only on scroll or drag

- Smooth expand & collapse animations

- Scroll-aware behavior (no forced expansion)

- Drag sensitivity control

- Rounded corner support

- Optional background blur

- State change callback (expanded / collapsed)

- Works with ScrollView, RecyclerView, NestedScrollView

- Lightweight & easy to integrate 

  
---

# **Preview**
---
<p align="center">
  <img src="https://github.com/user-attachments/assets/425b32b2-2acc-44d9-8cda-214107f959fd"
       alt="Demo GIF"
       width="200">



</p>


## **⚡ Installation**


**Step 1:** Add JitPack repository to your root `build.gradle`:

```gradle
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```

**Step 2:** Add dependency to your app module `build.gradle`:
```
dependencies {
implementation("com.github.Excelsior-Technologies-Community:ImageCrop:1.0.0")
 }
```

## **📦 Usage**

**Add SmartBottomSheet in XML**


```

<com.ext.smart_bottomsheet_library.SmartBottomSheet
    android:id="@+id/smartSheet"
    android:layout_width="match_parent"
    android:layout_height="550dp"
    android:layout_gravity="bottom"

    app:collapsedHeight="550dp"
    app:expandedHeight="0dp"
    app:dragSensitivity="1"
    app:cornerRadius="32dp"
    app:sheetBackgroundColor="#FFFFFF"
    app:autoExpand="false"
    app:enableBlur="true"
    app:blurRadius="20">

    <!-- Your scrollable content -->
    <ScrollView
        android:layout_width="match_parent"
        android:layout_height="match_parent">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="24dp">

            <!-- Content goes here -->

        </LinearLayout>
    </ScrollView>

</com.ext.smart_bottomsheet_library.SmartBottomSheet>


```

**🧠 Important Behavior (Very Important)**

- BottomSheet starts at 550dp height

- It does NOT expand automatically

- It expands to full screen only when:

- User scrolls content upward

- User drags the sheet upward

- On downward drag → it collapses back
  

**📌 MainActivity.kt Example**

```

class MainActivity : AppCompatActivity() {

    private lateinit var bottomSheet: SmartBottomSheet

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomSheet = findViewById(R.id.smartSheet)

        bottomSheet.setOnStateChangeListener { isExpanded ->
            if (isExpanded) {
                // BottomSheet is full screen
            } else {
                // BottomSheet is collapsed
            }
        }
    }
}

```

**attrs file**

```

<?xml version="1.0" encoding="utf-8"?>
<resources>

    <declare-styleable name="SmartBottomSheet">

        <!-- Height Configuration -->
        <!-- Height of the sheet when collapsed (default: 120dp) -->
        <attr name="collapsedHeight" format="dimension"/>

        <!-- Height of the sheet when expanded (default: 600dp) -->
        <attr name="expandedHeight" format="dimension"/>

        <!-- Drag Configuration -->
        <!-- Drag sensitivity multiplier (default: 1.0, higher = more sensitive) -->
        <attr name="dragSensitivity" format="float"/>

        <!-- Visual Styling -->
        <!-- Corner radius for top corners (default: 24dp) -->
        <attr name="cornerRadius" format="dimension"/>

        <!-- Background color of the sheet (default: white) -->
        <attr name="sheetBackgroundColor" format="color"/>

        <!-- Elevation/shadow of the sheet (default: 16dp) -->
        <attr name="sheetElevation" format="dimension"/>

        <!-- Dim overlay color (default: #99000000) -->
        <attr name="dimOverlayColor" format="color"/>

        <!-- Dim overlay opacity (default: 0.85, range: 0.0-1.0) -->
        <attr name="dimOverlayOpacity" format="float"/>

        <!-- Blur Effect -->
        <!-- Enable blur effect on background (requires Android 12+) -->
        <attr name="enableBlur" format="boolean"/>

        <!-- Blur radius in pixels (default: 20, only works on Android 12+) -->
        <attr name="blurRadius" format="integer"/>

        <!-- Behavior -->
        <!-- Auto expand sheet on start (default: false) -->
        <attr name="autoExpand" format="boolean"/>

        <!-- Animation duration in milliseconds (default: 350ms) -->
        <attr name="animationDuration" format="integer"/>

        <!-- Enable tap outside to close (default: true) -->
        <attr name="closeOnTapOutside" format="boolean"/>

    </declare-styleable>

</resources>

```


## **📄 License**

**MIT License**  
```
Copyright (c) 2025 Excelsior Technologies

Permission is hereby granted, free of charge, to any person obtaining a copy  
of this software and associated documentation files (the "Software"), to deal  
in the Software without restriction, including without limitation the rights  
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell  
copies of the Software, and to permit persons to whom the Software is  
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all  
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED **"AS IS"**, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR  
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,  
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
```



