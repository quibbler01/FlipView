# FlipView

![flipview_preview](./demo.gif)

[![](https://jitpack.io/v/quibbler01/FlipView.svg)](https://jitpack.io/#quibbler01/FlipView)

Step 1. Add it in your root build.gradle at the end of repositories:

	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
Step 2. Add the dependency

	dependencies {
	        implementation 'com.github.quibbler01:FlipView:1.0.0'
	}

Step 3. Add SwitchButton in your layout xml:

    <cn.quibbler.flipview.FlipView
        android:id="@+id/flip_view"
        android:layout_width="300dp"
        android:layout_height="200dp"
        android:layout_gravity="center_horizontal"
        app:autoFlipBack="true"
        app:flipDuration="500"
        app:flipFrom="right"
        app:flipType="vertical">

        <include layout="@layout/flip_view_back" />

        <include layout="@layout/flip_view_front" />

    </cn.quibbler.flipview.FlipView>

Step 4. explain:

        //first layout is back view
        <include layout="@layout/flip_view_back" />

        //second layout is front view
        <include layout="@layout/flip_view_front" />
