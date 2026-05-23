package ntu.nguyenthithanhhuong.smartflashcard;

import androidx.appcompat.app.AppCompatActivity;

public abstract class BaseAppActivity extends AppCompatActivity {

    @Override
    protected void onResume() {
        super.onResume();
        EdgeToEdgeHelper.hideStatusBar(this);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            EdgeToEdgeHelper.reapplyHiddenBars(this, true);
        }
    }
}
