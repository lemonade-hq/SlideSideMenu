package com.lemonade.widgets.slidesidemenu.samples;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.lemonade.widgets.slidesidemenu.SlideSideMenuTransitionLayout;

public class SlideSideMenuExampleActivity extends AppCompatActivity {

    private SlideSideMenuTransitionLayout mSlideSideMenu;
    private Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_slide_side_menu_example);

        // Grab the widget
        mSlideSideMenu = (SlideSideMenuTransitionLayout)findViewById(R.id.slide_side_menu);

        // Setup the toolbar
        mToolbar = (Toolbar) findViewById(R.id.base_toolbar);
        setSupportActionBar(mToolbar);
        mToolbar.setTitleTextColor(Color.WHITE);

        // Wire SideMenu with Toolbar
        mToolbar.setNavigationIcon(R.drawable.ic_menu);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSlideSideMenu.toggle();
            }
        });
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
