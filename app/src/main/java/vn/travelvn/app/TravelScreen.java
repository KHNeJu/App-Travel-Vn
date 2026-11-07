package vn.travelvn.app;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

abstract class TravelScreen extends Activity {
    static final int INK = 0xFF102447, BLUE = 0xFF245AC3, PAGE = 0xFFF4F7FD, PALE = 0xFFE8F0FF,
            WHITE = Color.WHITE, MUTED = 0xFF52647D;
    LinearLayout body;

    void screen(String title, String subtitle) {
        LinearLayout root = column();
        root.setBackgroundColor(PAGE);
        root.setFocusableInTouchMode(true);
        setContentView(root);
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                int flags = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                controller.setSystemBarsAppearance(flags, flags);
            }
            root.setOnApplyWindowInsetsListener((view, insets) -> {
                Insets safe = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
                view.setPadding(safe.left, safe.top, safe.right, safe.bottom);
                return insets;
            });
        }
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, -1));
        body = column();
        body.setPadding(dp(18), dp(10), dp(18), dp(24));
        scroll.addView(body);
        TextView back = button("‹", WHITE, INK, this::onBackPressed);
        back.setContentDescription("Quay lại");
        back.setTextSize(24);
        body.addView(back, new LinearLayout.LayoutParams(dp(48), dp(46)));
        space(body, 10);
        label(body, title, 27, INK, true);
        label(body, subtitle, 14, MUTED, false);
        space(body, 15);
        root.requestFocus();
    }

    LinearLayout column() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    LinearLayout card(LinearLayout parent) {
        LinearLayout card = column();
        card.setPadding(dp(16), dp(15), dp(16), dp(15));
        card.setBackground(round(WHITE, 25));
        parent.addView(card, new LinearLayout.LayoutParams(-1, -2));
        space(parent, 12);
        return card;
    }

    TextView label(LinearLayout parent, String text, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        parent.addView(view);
        return view;
    }

    EditText field(LinearLayout parent, String name, int inputType) {
        TextView title = label(parent, name, 13, INK, true);
        EditText edit = new EditText(this);
        edit.setId(View.generateViewId());
        title.setLabelFor(edit.getId());
        edit.setSingleLine(true);
        edit.setInputType(inputType);
        edit.setHint(name);
        edit.setTextSize(15);
        edit.setPadding(dp(15), dp(8), dp(15), dp(8));
        edit.setMinHeight(dp(50));
        edit.setBackground(round(PAGE, 18));
        parent.addView(edit);
        space(parent, 11);
        return edit;
    }

    TextView button(String text, int fill, int color, Runnable action) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(color);
        view.setTextSize(14);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setGravity(Gravity.CENTER);
        view.setMinHeight(dp(48));
        view.setPadding(dp(10), dp(6), dp(10), dp(6));
        view.setBackground(new RippleDrawable(ColorStateList.valueOf(0x287BA4F5), round(fill, 23), null));
        view.setFocusable(true);
        view.setOnClickListener(v -> action.run());
        return view;
    }

    GradientDrawable round(int color, int radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(radius));
        return shape;
    }

    void space(LinearLayout parent, int size) {
        parent.addView(new android.view.View(this), new LinearLayout.LayoutParams(1, dp(size)));
    }

    int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
