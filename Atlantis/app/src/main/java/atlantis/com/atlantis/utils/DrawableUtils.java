package atlantis.com.atlantis.utils;

import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;

/**
 * Provides compatibility tinting to drawables
 * Created by ricardo on 2/28/15.
 */
public class DrawableUtils {

    public static void tintDrawablesIfNotLollipop(Drawable drawables[], int color) {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            for(Drawable drawable : drawables) {
                drawable.setColorFilter(color, PorterDuff.Mode.SRC_IN);
            }
        }
    }

}
