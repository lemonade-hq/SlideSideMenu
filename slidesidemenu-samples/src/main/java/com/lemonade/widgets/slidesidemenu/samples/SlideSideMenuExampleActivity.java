package com.lemonade.widgets.slidesidemenu.samples;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.lemonade.widgets.slidesidemenu.SlideSideMenuTransitionLayout;

public class SlideSideMenuExampleActivity extends AppCompatActivity {

    private SlideSideMenuTransitionLayout mSlideSideMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_slide_side_menu_example);

        mSlideSideMenu = (SlideSideMenuTransitionLayout)findViewById(R.id.slide_side_menu);
    }

    @Override
    public void onBackPressed() {
        if (mSlideSideMenu != null && mSlideSideMenu.closeSideMenu()) {
            // Closed the side menu, override the default back pressed behavior
            return;
        }
        super.onBackPressed();
    }
}
