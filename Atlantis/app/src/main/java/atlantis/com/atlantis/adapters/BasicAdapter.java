package atlantis.com.atlantis.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Basic adapter class with simple view holder
 * Created by ricardo on 2/22/15.
 */
public abstract class BasicAdapter extends RecyclerView.Adapter<BasicAdapter.ViewHolder> {

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final View mView;

        public View getView() {
            return mView;
        }

        public ViewHolder(View itemView) {
            super(itemView);
            this.mView = itemView;
        }
    }
}
