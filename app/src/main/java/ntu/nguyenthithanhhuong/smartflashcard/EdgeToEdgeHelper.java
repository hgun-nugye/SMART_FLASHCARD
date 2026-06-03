package ntu.nguyenthithanhhuong.smartflashcard;

import android.view.View;
import android.view.Window;

import androidx.annotation.Nullable;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public final class EdgeToEdgeHelper {
    private EdgeToEdgeHelper() {
    }

    public static void enable(AppCompatActivity activity) {
        EdgeToEdge.enable(activity);
    }

    public static void setupBottomNavHost(@Nullable View fragmentContainer,
                                          @Nullable View bottomNavigation) {
        if (fragmentContainer != null) {
            ViewCompat.setOnApplyWindowInsetsListener(fragmentContainer, (v, windowInsets) -> {
                Insets status = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars());
                v.setPadding(v.getPaddingLeft(), 0, v.getPaddingRight(), v.getPaddingBottom());
                return windowInsets;
            });
            ViewCompat.requestApplyInsets(fragmentContainer);
        }
        applyBottomNavigationInsets(bottomNavigation);
    }

    public static void hideStatusBar(AppCompatActivity activity) {
        applySystemBarsVisibility(activity, WindowInsetsCompat.Type.statusBars(), false);
    }

    public static void showStatusBar(AppCompatActivity activity) {
        applySystemBarsVisibility(activity, WindowInsetsCompat.Type.statusBars(), true);
    }

    public static void reapplyHiddenBars(AppCompatActivity activity, boolean hideStatusBarOnly) {
        if (!activity.hasWindowFocus()) {
            return;
        }
        if (hideStatusBarOnly) {
            hideStatusBar(activity);
        }
    }

    private static void applySystemBarsVisibility(AppCompatActivity activity, int types, boolean show) {
        Window window = activity.getWindow();
        if (window == null) {
            return;
        }
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(window, window.getDecorView());
        if (controller == null) {
            return;
        }
        if (show) {
            controller.show(types);
        } else {
            controller.hide(types);
            controller.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }
    }

    public static void applyRootInsets(@Nullable View root) {
        if (root == null) {
            return;
        }
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
        ViewCompat.requestApplyInsets(root);
    }

    public static void applyToolbarTopInset(@Nullable View toolbar) {
        if (toolbar == null) {
            return;
        }
        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, windowInsets) -> {
            Insets status = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars());
            v.setPadding(v.getPaddingLeft(), status.top, v.getPaddingRight(), v.getPaddingBottom());
            return WindowInsetsCompat.CONSUMED;
        });
        ViewCompat.requestApplyInsets(toolbar);
    }

    public static void applyCoordinatorInsets(@Nullable View coordinatorRoot,
                                              @Nullable View appBarLayout) {
        applyToolbarTopInset(appBarLayout);
        if (coordinatorRoot == null) {
            return;
        }
        ViewCompat.setOnApplyWindowInsetsListener(coordinatorRoot, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, 0, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
        ViewCompat.requestApplyInsets(coordinatorRoot);
    }

    public static void applyScreenWithToolbar(
            @Nullable View root,
            @Nullable View toolbar
    ) {
        if (root == null) {
            return;
        }

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
            );

            v.setPadding(
                    bars.left,
                    0,
                    bars.right,
                    bars.bottom
            );

            return WindowInsetsCompat.CONSUMED;
        });

        ViewCompat.requestApplyInsets(root);
    }

    public static void applyBottomNavigationInsets(@Nullable View bottomNav) {
        if (bottomNav == null) {
            return;
        }
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, windowInsets) -> {
            Insets nav = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), nav.bottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(bottomNav);
    }
}
