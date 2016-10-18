package com.lemonade.widgets.slidesidemenu;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;

/**
 * Created by eyalbiran on 9/29/16.
 */
class Utils {

    public static int convertDPtoPixels(Context context, int dp) {
        Resources resource = context.getResources();
        return(int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resource.getDisplayMetrics());
    }

}
