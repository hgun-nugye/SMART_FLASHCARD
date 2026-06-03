package ntu.nguyenthithanhhuong.smartflashcard.card;

import androidx.appcompat.app.AppCompatActivity;

import ntu.nguyenthithanhhuong.smartflashcard.EdgeToEdgeHelper;

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
