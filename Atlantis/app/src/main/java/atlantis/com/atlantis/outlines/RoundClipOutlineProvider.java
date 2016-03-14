package atlantis.com.atlantis.outlines;

import android.annotation.TargetApi;
import android.graphics.Outline;
import android.os.Build;
import android.view.View;
import android.view.ViewOutlineProvider;

/**
 * Created by ricardo on 5/4/15.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class RoundClipOutlineProvider extends ViewOutlineProvider {

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void getOutline(View view, Outline outline) {
        outline.setOval(0, 0, view.getWidth(), view.getHeight());
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void setClipOutlineProvider(View view) {
        view.setOutlineProvider(this);
        view.setClipToOutline(true);
    }

}
