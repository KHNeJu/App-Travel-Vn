package vn.travelvn.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final int BLUE = 0xFF1458CC;
    private static final int NAVY = 0xFF102746;
    private static final int MUTED = 0xFF52647E;
    private static final int PAGE = 0xFFF1F5FC;
    private static final int TINT = 0xFFE6EFFF;
    private static final int WHITE = Color.WHITE;
    private static final int GREEN = 0xFF086644;
    private static final int RED = 0xFFAA3030;
    private static final String TRIP = "Ninh Bình cuối tuần";
    private static final long BUDGET = 10_000_000;
    private static final String[] TABS = {"Trang chủ", "Lịch trình", "Chi tiêu", "Cá nhân"};
    private static final String[] ICONS = {"⌂", "✈", "₫", "●"};
    private static final String[] PEOPLE = {"Bạn", "Linh", "An", "Huy", "Mai"};
    private static final String[] THEMES = {"Ẩm thực", "Thiên nhiên", "Văn hóa", "Thư giãn", "Phiêu lưu"};
    private static final String[] DAY_NAMES = {"Cố đô & Tràng An", "Bái Đính & Thung Nham", "Tam Cốc thong thả"};
    private static final String[][] PLACES = {
        {"08:00   Cố đô Hoa Lư", "10:30   Tràng An", "15:30   Hang Múa"},
        {"08:30   Chùa Bái Đính", "12:00   Cơm cháy dê núi", "14:00   Vườn chim Thung Nham"},
        {"07:30   Đạp xe Tam Cốc", "10:00   Bến thuyền Tam Cốc", "13:30   Cà phê ngắm núi"}
    };

    private record Expense(String name, int payer, long amount) {}

    private final List<Expense> expenses = new ArrayList<>();
    private final NumberFormat numbers = NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN"));
    private SharedPreferences storage;
    private LinearLayout root;
    private LinearLayout content;
    private int tab;
    private int day;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        storage = getSharedPreferences("travel-vn", MODE_PRIVATE);
        expenses.add(new Expense("Homestay 2 đêm", 1, 3_200_000));
        expenses.add(new Expense("Ăn tối dê núi", 0, 1_250_000));
        expenses.add(new Expense("Thuê xe 3 ngày", 3, 2_100_000));
        try {
            JSONArray saved = new JSONArray(storage.getString("expenses", "[]"));
            for (int i = 0; i < saved.length(); i++) {
                JSONObject item = saved.getJSONObject(i);
                expenses.add(0, new Expense(item.getString("name"), item.getInt("payer"), item.getLong("amount")));
            }
        } catch (JSONException error) {
            Toast.makeText(this, "Không đọc được một số khoản chi đã lưu.", Toast.LENGTH_LONG).show();
        }
        if (state != null) {
            tab = state.getInt("tab");
            day = state.getInt("day");
        }

        root = column();
        root.setBackgroundColor(PAGE);
        setContentView(root);
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                int appearance = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                controller.setSystemBarsAppearance(appearance, appearance);
            }
            root.setOnApplyWindowInsetsListener((view, insets) -> {
                Insets safe = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                view.setPadding(safe.left, safe.top, safe.right, safe.bottom);
                return insets;
            });
        } else if (Build.VERSION.SDK_INT >= 26) {
            root.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        } else {
            getWindow().setNavigationBarColor(NAVY);
        }
        showTab(tab);
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putInt("tab", tab);
        state.putInt("day", day);
    }

    private void showTab(int selected) {
        tab = selected;
        root.removeAllViews();
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        content = column();
        content.setPadding(dp(20), dp(20), dp(20), dp(24));
        scroll.addView(content);
        switch (tab) {
            case 0 -> home();
            case 1 -> itinerary();
            case 2 -> expenses();
            case 3 -> profile();
        }

        LinearLayout nav = row();
        nav.setBackgroundColor(WHITE);
        nav.setElevation(dp(6));
        nav.setPadding(dp(8), dp(6), dp(8), dp(6));
        for (int i = 0; i < TABS.length; i++) {
            int index = i;
            TextView item = action(ICONS[i] + "\n" + TABS[i], i == tab ? TINT : WHITE,
                    i == tab ? BLUE : MUTED, () -> showTab(index));
            item.setContentDescription(TABS[i]);
            item.setSelected(i == tab);
            nav.addView(item, new LinearLayout.LayoutParams(0, -1, 1));
        }
        root.addView(nav, new LinearLayout.LayoutParams(-1, dp(68)));
    }

    private void home() {
        heading("TRAVEL VN  ·  DU LỊCH NHÓM", "Sẵn sàng đi đâu?");
        LinearLayout upcoming = card(content, BLUE);
        line(upcoming, "CHUYẾN ĐI SẮP TỚI", 12, WHITE, true);
        gap(upcoming, 9);
        line(upcoming, TRIP, 26, WHITE, true);
        line(upcoming, "12–14 tháng 12  ·  3 ngày 2 đêm  ·  5 người", 14, WHITE, false);
        gap(upcoming, 16);
        upcoming.addView(action("Mở chuyến đi  →", WHITE, BLUE, () -> showTab(1)));

        LinearLayout planner = card(content, WHITE);
        line(planner, "Lên kế hoạch chuyến đi", 19, NAVY, true);
        line(planner, "Chọn điểm đến, số người và sở thích để xem lịch trình mẫu.", 14, MUTED, false);
        gap(planner, 12);
        planner.addView(action("✦  Tạo lịch trình mẫu", TINT, BLUE, this::createTrip));

        LinearLayout next = card(content, WHITE);
        line(next, "VIỆC TIẾP THEO  ·  MẪU", 12, BLUE, true);
        line(next, "Đặt homestay Tràng An", 17, NAVY, true);
        line(next, "Hoàn thành trước 20:00, ngày 12/12", 14, MUTED, false);
    }

    private void itinerary() {
        heading("LỊCH TRÌNH  ·  CHUYẾN ĐI MẪU", TRIP);
        LinearLayout info = card(content, BLUE);
        line(info, "12–14 tháng 12", 22, WHITE, true);
        line(info, "9 hoạt động  ·  3 ngày  ·  5 người", 14, WHITE, false);
        LinearLayout days = row();
        for (int i = 0; i < PLACES.length; i++) {
            int index = i;
            TextView item = action("Ngày " + (i + 1), i == day ? BLUE : TINT, i == day ? WHITE : BLUE, () -> {
                day = index;
                showTab(1);
            });
            item.setSelected(i == day);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1);
            params.setMarginEnd(dp(i == PLACES.length - 1 ? 0 : 8));
            days.addView(item, params);
        }
        content.addView(days);
        gap(content, 16);
        line(content, DAY_NAMES[day], 20, NAVY, true);
        gap(content, 10);
        for (String place : PLACES[day]) {
            line(card(content, WHITE), place, 16, NAVY, true);
        }
    }

    private void createTrip() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout fields = column();
        fields.setPadding(dp(24), 0, dp(24), dp(8));
        scroll.addView(fields);
        line(fields, "Lịch trình mẫu · 3 ngày 2 đêm", 14, MUTED, false);
        EditText destination = field(fields, "Điểm đến", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, 60);
        destination.setText("Ninh Bình");
        line(fields, "Số người", 14, NAVY, true);
        NumberPicker people = new NumberPicker(this);
        people.setMinValue(1);
        people.setMaxValue(30);
        people.setValue(4);
        people.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        people.setContentDescription("Số người");
        fields.addView(people);
        EditText budget = field(fields, "Ngân sách mỗi người (₫)", InputType.TYPE_CLASS_NUMBER, 12);
        budget.setText("2500000");
        line(fields, "Sở thích", 14, NAVY, true);
        List<CheckBox> choices = new ArrayList<>();
        for (String theme : THEMES) {
            CheckBox choice = new CheckBox(this);
            choice.setText(theme);
            fields.addView(choice);
            choices.add(choice);
        }
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Tạo chuyến đi")
                .setView(scroll).setNegativeButton("Hủy", null)
                .setPositiveButton("Xem lịch trình", null).create();
        dialog.setOnShowListener(event -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            String place = destination.getText().toString().trim();
            if (place.isEmpty()) {
                destination.setError("Nhập điểm đến");
                return;
            }
            long perPerson = positiveAmount(budget);
            if (perPerson == 0) return;
            List<String> interests = new ArrayList<>();
            for (CheckBox choice : choices) {
                if (choice.isChecked()) interests.add(choice.getText().toString());
            }
            StringBuilder preview = new StringBuilder(people.getValue() + " người  ·  " + money(perPerson) + "/người");
            if (!interests.isEmpty()) preview.append("\n").append(String.join(", ", interests));
            preview.append("\n\nLịch trình mẫu");
            if (place.equalsIgnoreCase("Ninh Bình")) {
                for (int i = 0; i < PLACES.length; i++) {
                    preview.append("\n\nNgày ").append(i + 1).append(" · ").append(DAY_NAMES[i])
                            .append("\n").append(String.join("\n", PLACES[i]));
                }
            } else {
                preview.append("\n\nNgày 1 · Đến nơi, nhận phòng và khám phá trung tâm.")
                        .append("\n\nNgày 2 · ")
                        .append(interests.isEmpty() ? "Tham quan và thử món địa phương." :
                                "Chọn hoạt động theo sở thích: " + String.join(", ", interests) + ".")
                        .append("\n\nNgày 3 · Mua quà, trả phòng và về nhà.");
            }
            dialog.dismiss();
            new AlertDialog.Builder(this).setTitle(place).setMessage(preview)
                    .setPositiveButton("Đóng", null).show();
        }));
        dialog.show();
    }

    private void expenses() {
        heading("CHI TIÊU NHÓM", TRIP);
        long total = 0;
        long share = 0;
        long paid = 0;
        for (Expense item : expenses) {
            total += item.amount();
            share += shareOf(item.amount(), 0);
            if (item.payer() == 0) paid += item.amount();
        }
        long balance = paid - share;
        LinearLayout summary = card(content, NAVY);
        line(summary, "TỔNG CHI TIÊU", 12, WHITE, true);
        line(summary, money(total), 29, WHITE, true);
        gap(summary, 10);
        line(summary, "Phần của bạn  ·  " + money(share), 14, WHITE, false);
        line(summary, balance == 0 ? "Bạn đã cân bằng chi phí" :
                (balance > 0 ? "Bạn được nhận  " : "Bạn cần trả  ") + money(Math.abs(balance)), 15, WHITE, true);

        LinearLayout budget = card(content, WHITE);
        line(budget, "Ngân sách nhóm", 17, NAVY, true);
        line(budget, money(total) + " / " + money(BUDGET), 14, total > BUDGET ? RED : MUTED, false);
        ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setProgressTintList(ColorStateList.valueOf(total > BUDGET ? RED : BLUE));
        progress.setProgress((int) Math.min(100, total * 100.0 / BUDGET));
        budget.addView(progress);
        content.addView(action("+  Thêm khoản chi", BLUE, WHITE, this::addExpense));
        gap(content, 16);
        line(content, "Khoản chi  ·  " + expenses.size(), 19, NAVY, true);
        gap(content, 9);
        for (Expense item : expenses) {
            LinearLayout entry = card(content, WHITE);
            line(entry, item.name(), 16, NAVY, true);
            line(entry, PEOPLE[item.payer()] + " đã trả  ·  " + money(item.amount()), 14, MUTED, false);
        }
        content.addView(action("Xem gợi ý quyết toán", TINT, BLUE, this::showSettlement));
    }

    private void addExpense() {
        LinearLayout fields = column();
        fields.setPadding(dp(24), 0, dp(24), dp(8));
        EditText name = field(fields, "Tên khoản chi", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, 80);
        EditText amount = field(fields, "Số tiền (₫)", InputType.TYPE_CLASS_NUMBER, 12);
        line(fields, "Bạn ứng tiền, chia đều cho 5 thành viên.", 13, MUTED, false);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Thêm khoản chi").setView(fields)
                .setNegativeButton("Hủy", null).setPositiveButton("Lưu", null).create();
        dialog.setOnShowListener(event -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            String title = name.getText().toString().trim();
            if (title.isEmpty()) {
                name.setError("Nhập tên khoản chi");
                return;
            }
            long value = positiveAmount(amount);
            if (value == 0) return;
            try {
                JSONArray saved = new JSONArray(storage.getString("expenses", "[]"));
                saved.put(new JSONObject().put("name", title).put("payer", 0).put("amount", value));
                if (!storage.edit().putString("expenses", saved.toString()).commit()) {
                    Toast.makeText(this, "Chưa lưu được khoản chi. Vui lòng thử lại.", Toast.LENGTH_LONG).show();
                    return;
                }
                expenses.add(0, new Expense(title, 0, value));
                dialog.dismiss();
                showTab(2);
            } catch (JSONException error) {
                Toast.makeText(this, "Không đọc được dữ liệu chi tiêu đã lưu.", Toast.LENGTH_LONG).show();
            }
        }));
        dialog.show();
    }

    private long shareOf(long amount, int person) {
        return amount / PEOPLE.length + (person < amount % PEOPLE.length ? 1 : 0);
    }

    private void showSettlement() {
        long[] balances = new long[PEOPLE.length];
        for (Expense item : expenses) {
            balances[item.payer()] += item.amount();
            for (int i = 0; i < balances.length; i++) balances[i] -= shareOf(item.amount(), i);
        }
        // ponytail: a greedy settlement is sufficient for suggestions; exact minimization is optional.
        List<String> transfers = new ArrayList<>();
        while (true) {
            int sender = 0;
            int receiver = 0;
            for (int i = 1; i < balances.length; i++) {
                if (balances[i] < balances[sender]) sender = i;
                if (balances[i] > balances[receiver]) receiver = i;
            }
            if (balances[sender] >= 0) break;
            long value = Math.min(-balances[sender], balances[receiver]);
            transfers.add(PEOPLE[sender] + " → " + PEOPLE[receiver] + "  " + money(value));
            balances[sender] += value;
            balances[receiver] -= value;
        }
        new AlertDialog.Builder(this).setTitle("Gợi ý quyết toán")
                .setMessage(transfers.isEmpty() ? "Mọi người đã cân bằng chi phí." : String.join("\n\n", transfers))
                .setPositiveButton("Đóng", null).show();
    }

    private void profile() {
        heading("TRAVEL VN", "Cá nhân");
        LinearLayout account = card(content, WHITE);
        line(account, "HỒ SƠ MẪU", 12, BLUE, true);
        line(account, "Minh Nguyễn", 22, NAVY, true);
        line(account, "minh@example.com", 14, MUTED, false);
        LinearLayout group = card(content, WHITE);
        line(group, "Nhóm du lịch · 5 thành viên", 17, NAVY, true);
        line(group, String.join("  ·  ", PEOPLE), 14, MUTED, false);
        LinearLayout about = card(content, TINT);
        line(about, "Travel VN · Bản thử nghiệm 1.0.1", 15, BLUE, true);
        line(about, "Tiếng Việt  ·  Dữ liệu lưu trên thiết bị", 14, MUTED, false);
    }

    private long positiveAmount(EditText input) {
        String value = input.getText().toString();
        if (value.matches("[0-9]{1,12}")) {
            long amount = Long.parseLong(value);
            if (amount > 0) return amount;
        }
        input.setError("Nhập số tiền lớn hơn 0 (tối đa 12 chữ số)");
        return 0;
    }

    private String money(long value) {
        return numbers.format(value) + " ₫";
    }

    private void heading(String label, String title) {
        line(content, label, 12, BLUE, true).setLetterSpacing(0.12f);
        gap(content, 4);
        line(content, title, 28, NAVY, true);
        gap(content, 17);
    }

    private TextView line(LinearLayout parent, String value, int size, int color, boolean bold) {
        TextView label = new TextView(this);
        label.setText(value);
        label.setTextColor(color);
        label.setTextSize(size);
        if (bold) label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        parent.addView(label, new LinearLayout.LayoutParams(-1, -2));
        return label;
    }

    private TextView action(String value, int fill, int color, Runnable click) {
        TextView button = new TextView(this);
        button.setText(value);
        button.setTextSize(13);
        button.setTextColor(color);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setMinHeight(dp(48));
        button.setPadding(dp(6), dp(6), dp(6), dp(6));
        button.setBackground(new RippleDrawable(ColorStateList.valueOf(0x26809BD4), rounded(fill, 14), null));
        button.setOnClickListener(view -> click.run());
        button.setFocusable(true);
        button.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override
            public void onInitializeAccessibilityNodeInfo(View host, android.view.accessibility.AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName("android.widget.Button");
            }
        });
        return button;
    }

    private EditText field(LinearLayout parent, String hint, int type, int limit) {
        TextView label = line(parent, hint, 14, NAVY, true);
        EditText input = new EditText(this);
        input.setId(View.generateViewId());
        label.setLabelFor(input.getId());
        input.setHint(hint);
        input.setInputType(type);
        input.setSingleLine();
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(limit)});
        parent.addView(input, new LinearLayout.LayoutParams(-1, -2));
        gap(parent, 10);
        return input;
    }

    private LinearLayout card(LinearLayout parent, int color) {
        LinearLayout card = column();
        card.setPadding(dp(17), dp(15), dp(17), dp(15));
        card.setBackground(rounded(color, 22));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(12);
        parent.addView(card, params);
        return card;
    }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(color);
        background.setCornerRadius(dp(radius));
        return background;
    }

    private void gap(LinearLayout parent, int height) {
        parent.addView(new View(this), new LinearLayout.LayoutParams(1, dp(height)));
    }

    private LinearLayout column() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private LinearLayout row() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        return layout;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
