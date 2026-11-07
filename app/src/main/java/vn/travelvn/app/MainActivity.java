package vn.travelvn.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ArrayAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class MainActivity extends Activity {
    private static final int BLUE = 0xFF265FC4;
    private static final int NAVY = 0xFF102746;
    private static final int MUTED = 0xFF52647E;
    private static final int PAGE = 0xFFF5F7FC;
    private static final int TINT = 0xFFEAF1FF;
    private static final int BORDER = 0xFFE8EDF5;
    private static final int WHITE = Color.WHITE;
    private static final int GREEN = 0xFF086644;
    private static final int RED = 0xFFAA3030;
    private static final String[] TABS = {"Trang chủ", "Khám phá", "Kỷ niệm", "Menu"};
    private static final int HOME = 0, EXPLORE = 1, MEMORIES = 2, MENU = 3, TRIPS = 4, CALENDAR = 5;
    private static final int PLAN = 0, TRIP_EXPLORE = 1, GUIDE = 2, MONEY = 3, JOURNAL = 4;
    private static final int[] NAV_ICONS = {0, 2, 3, 4};
    private static final String[] SECTIONS = {"Kế hoạch", "Khám phá", "Cẩm nang", "Chi tiêu", "Nhật ký"};
    private static final String[] DESTINATIONS = {"Phú Quốc", "Huế", "Sa Pa", "Quy Nhơn", "Đà Lạt", "Hội An", "Phú Yên", "Mũi Né", "Ninh Bình", "Đà Nẵng"};
    private static final String[] PEOPLE = {"Bạn", "Linh", "An", "Huy", "Mai"};
    private static final String[] PACKING = {"Giấy tờ tùy thân", "Sạc điện thoại", "Đồ vệ sinh cá nhân", "Áo khoác nhẹ", "Giày đi bộ", "Thuốc cá nhân"};
    private static final String[] DAY_NAMES = {"Cố đô & Tràng An", "Bái Đính & Thung Nham", "Tam Cốc thong thả"};
    private static final String[][] PLACES = {
        {"08:00   Cố đô Hoa Lư", "10:30   Tràng An", "15:30   Hang Múa"},
        {"08:30   Chùa Bái Đính", "12:00   Cơm cháy dê núi", "14:00   Vườn chim Thung Nham"},
        {"07:30   Đạp xe Tam Cốc", "10:00   Bến thuyền Tam Cốc", "13:30   Cà phê ngắm núi"}
    };

    private static final String[][] DISCOVER = {
        {"Tràng An", "Tham quan", "Đi thuyền giữa những dãy núi đá vôi."},
        {"Cơm cháy Ninh Bình", "Ăn uống", "Đặc sản giòn, thường ăn cùng sốt dê."},
        {"Hang Múa", "Thiên nhiên", "Đi bộ lên điểm ngắm toàn cảnh Tam Cốc."},
        {"Cố đô Hoa Lư", "Tham quan", "Khám phá các đền và di tích của cố đô."}
    };
    private static final Trip SAMPLE = new Trip("", "Ninh Bình cuối tuần", "Ninh Bình", 3, 5, 10_000_000, "", 0,
            java.util.Collections.emptyList(), false);

    private record Trip(String id, String title, String destination, int days, int people, long budget,
                        String interests, long startAt, List<String> stops, boolean manual) {}
    private record Expense(String name, int payer, long amount, int participants) {}

    private final List<Expense> expenses = new ArrayList<>();
    private final List<Trip> trips = new ArrayList<>();
    private final NumberFormat numbers = NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN"));
    private SharedPreferences storage;
    private LinearLayout root;
    private LinearLayout content;
    private int tab;
    private int day;
    private int section;
    private Trip trip;
    private String discoverFilter = "Tất cả";
    private String documentKey;
    private int wizardStep = -1;
    private String destination = "";
    private long startAt;
    private long endAt;
    private long tripBudget;
    private int travelers = 1;
    private final LinkedHashSet<String> chosenStops = new LinkedHashSet<>();

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        storage = getSharedPreferences("travel-vn", MODE_PRIVATE);
        trips.add(SAMPLE);
        JSONArray saved = savedArray("trips");
        for (int i = 0; i < saved.length(); i++) {
            JSONObject item = saved.optJSONObject(i);
            if (item == null || item.optString("id").isEmpty()) continue;
            List<String> stops = new ArrayList<>();
            JSONArray selectedStops = item.optJSONArray("stops");
            if (selectedStops != null) for (int j = 0; j < selectedStops.length(); j++) stops.add(selectedStops.optString(j));
            trips.add(new Trip(item.optString("id"), item.optString("title"), item.optString("destination"),
                    item.optInt("days", 3), item.optInt("people", 1), item.optLong("budget"), item.optString("interests"),
                    item.optLong("startAt"), stops, item.optBoolean("manual")));
        }
        if (state != null) {
            tab = state.getInt("navTab");
            day = state.getInt("day");
            section = state.getInt("tripSection");
            documentKey = state.getString("documentKey");
            wizardStep = state.getInt("wizardStep", -1);
            destination = state.getString("destination", "");
            startAt = state.getLong("startAt");
            endAt = state.getLong("endAt");
            tripBudget = state.getLong("tripBudget");
            travelers = state.getInt("travelers", 1);
            ArrayList<String> stops = state.getStringArrayList("chosenStops");
            if (stops != null) chosenStops.addAll(stops);
            String tripId = state.getString("trip");
            for (Trip savedTrip : trips) {
                if (savedTrip.id().equals(tripId)) trip = savedTrip;
            }
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
        if (wizardStep >= 0) showWizard();
        else showTab(tab);
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putInt("navTab", tab);
        state.putInt("day", day);
        state.putInt("tripSection", section);
        state.putString("documentKey", documentKey);
        state.putInt("wizardStep", wizardStep);
        state.putString("destination", destination);
        state.putLong("startAt", startAt);
        state.putLong("endAt", endAt);
        state.putLong("tripBudget", tripBudget);
        state.putInt("travelers", travelers);
        state.putStringArrayList("chosenStops", new ArrayList<>(chosenStops));
        if (trip != null) state.putString("trip", trip.id());
    }

    @Override
    public void onBackPressed() {
        if (wizardStep == 0) {
            wizardStep = -1;
            showTab(tab);
        } else if (wizardStep > 0) {
            wizardStep--;
            showWizard();
        } else if (trip != null) navigate(TRIPS);
        else if (tab != HOME) navigate(HOME);
        else super.onBackPressed();
    }

    private void navigate(int selected) {
        trip = null;
        discoverFilter = "Tất cả";
        showTab(selected);
    }

    private void openTrip(Trip selected, int selectedSection) {
        trip = selected;
        section = selectedSection;
        day = 0;
        discoverFilter = "Tất cả";
        showTab(TRIPS);
    }

    private void showTab(int selected) {
        tab = selected;
        root.removeAllViews();
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        FrameLayout body = new FrameLayout(this);
        body.addView(scroll);
        root.addView(body, new LinearLayout.LayoutParams(-1, 0, 1));
        content = column();
        content.setPadding(dp(16), dp(12), dp(16), dp(25));
        scroll.addView(content);
        if (trip != null) tripDetails();
        else switch (tab) {
            case HOME -> home();
            case EXPLORE -> discover();
            case MEMORIES -> memories();
            case MENU -> profile();
            case TRIPS -> trips();
            case CALENDAR -> calendar();
        }
        if (tab == HOME || trip != null) {
            TextView floating = action("Hỏi AI", NAVY, WHITE, this::askAssistant);
            floating.setTextSize(13);
            floating.setBackground(rounded(NAVY, 30));
            floating.setContentDescription("Hỏi AI");
            floating.setElevation(dp(5));
            FrameLayout.LayoutParams pos = new FrameLayout.LayoutParams(dp(72), dp(45), Gravity.BOTTOM | Gravity.END);
            pos.setMargins(dp(12), 0, dp(17), dp(14));
            body.addView(floating, pos);
        }

        LinearLayout nav = row();
        nav.setBackgroundColor(WHITE);
        nav.setElevation(dp(2));
        for (int i = 0; i < TABS.length; i++) {
            int index = i;
            LinearLayout item = column();
            item.setGravity(Gravity.CENTER);
            item.setBackground(new RippleDrawable(ColorStateList.valueOf(TINT), rounded(WHITE, 8), null));
            boolean active = i == tab || i == HOME && (tab == TRIPS || tab == CALENDAR);
            int color = active ? NAVY : MUTED;
            item.addView(new TravelIcon(this, NAV_ICONS[i], color), new LinearLayout.LayoutParams(dp(21), dp(21)));
            TextView label = line(item, TABS[i], 10, color, active);
            label.setGravity(Gravity.CENTER);
            item.setContentDescription(TABS[i]);
            item.setSelected(active);
            item.setFocusable(true);
            item.setOnClickListener(view -> navigate(index));
            nav.addView(item, new LinearLayout.LayoutParams(0, -1, 1));
        }
        root.addView(nav, new LinearLayout.LayoutParams(-1, dp(60)));
    }

    private void home() {
        LinearLayout header = row();
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView avatar = action("M", TINT, BLUE, () -> navigate(MENU));
        avatar.setContentDescription("Hồ sơ cá nhân");
        avatar.setBackground(rounded(TINT, 24));
        header.addView(avatar, new LinearLayout.LayoutParams(dp(44), dp(44)));
        LinearLayout greeting = column();
        greeting.setPadding(dp(10), 0, 0, 0);
        header.addView(greeting, new LinearLayout.LayoutParams(0, -2, 1));
        line(greeting, "Chào bạn ☀", 12, MUTED, false);
        line(greeting, storage.getString("displayName", "Minh Nguyễn"), 19, NAVY, true);
        TextView tripsButton = action("▤", WHITE, NAVY, () -> navigate(TRIPS));
        tripsButton.setContentDescription("Chuyến đi của bạn");
        header.addView(tripsButton, new LinearLayout.LayoutParams(dp(45), dp(45)));
        content.addView(header);
        gap(content, 12);
        Trip upcoming = trips.get(trips.size() - 1);
        tripCard(upcoming, true);
        if (trips.size() == 1) {
            LinearLayout start = card(content, WHITE);
            line(start, "Bắt đầu với Travel VN", 17, NAVY, true);
            line(start, "Lên kế hoạch, thêm điểm đến và chia chi tiêu cùng nhóm.", 13, MUTED, false);
            start.addView(action("Lên kế hoạch cho nhóm", NAVY, WHITE, this::createTrip));
        }
        shortcuts(upcoming);
        line(content, "Quanh bạn", 19, NAVY, true);
        line(content, "Gợi ý địa điểm ở Ninh Bình", 12, MUTED, false);
        gap(content, 8);
        filters();
        places(false);
        gap(content, 45);
    }

    private void tripDetails() {
        content.addView(action("‹  Chuyến đi", PAGE, BLUE, () -> navigate(TRIPS)));
        tripCard(trip, false);
        Trip selectedTrip = trip;
        chipRow(new String[]{"Hành lý " + packedCount(trip) + "/" + PACKING.length, "Tài liệu", "Thành viên"}, -1, index -> {
            switch (index) {
                case 0 -> packing(selectedTrip);
                case 1 -> documents(selectedTrip);
                case 2 -> members();
            }
        }, false);
        gap(content, 9);
        chipRow(SECTIONS, section, index -> {
            section = index;
            discoverFilter = "Tất cả";
            showTab(TRIPS);
        }, true);
        gap(content, 15);
        switch (section) {
            case PLAN -> itinerary();
            case TRIP_EXPLORE -> discover();
            case GUIDE -> guide();
            case MONEY -> expenses();
            case JOURNAL -> journal();
        }
        gap(content, 44);
    }

    private void itinerary() {
        line(content, "Lịch trình theo ngày", 20, NAVY, true);
        gap(content, 8);
        HorizontalScrollView dates = new HorizontalScrollView(this);
        dates.setHorizontalScrollBarEnabled(false);
        LinearLayout days = row();
        for (int i = 0; i < trip.days(); i++) {
            int index = i;
            TextView item = action("Ngày " + (i + 1), i == day ? BLUE : TINT, i == day ? WHITE : BLUE, () -> {
                day = index;
                showTab(TRIPS);
            });
            item.setSelected(i == day);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(76), -2);
            params.setMarginEnd(dp(8));
            days.addView(item, params);
        }
        dates.addView(days);
        content.addView(dates);
        dates.post(() -> dates.scrollTo(Math.max(0, day - 1) * dp(84), 0));
        gap(content, 14);
        String date = trip.id().isEmpty() ? (12 + day) + " Th12"
                : trip.startAt() == 0 ? "" : dateText(addDays(trip.startAt(), day), "d MMM");
        line(content, "NGÀY " + (day + 1) + (date.isEmpty() ? "" : " · " + date), 11, BLUE, true);
        line(content, dayTitle(trip, day), 17, NAVY, true);
        gap(content, 10);
        List<String> items = activities(trip, day);
        for (int i = 0; i < items.size(); i++) {
            int itemIndex = i;
            LinearLayout entry = card(content, WHITE);
            String value = items.get(i);
            String time = value.substring(0, 5);
            String title = value.substring(5).trim();
            line(entry, (i + 1) + "  ·  " + time, 12, BLUE, true);
            line(entry, title, 15, NAVY, true);
            entry.setOnClickListener(view -> editActivity(itemIndex));
        }
        content.addView(action("+  Thêm hoạt động", TINT, BLUE, () -> editActivity(-1)));
    }

    private void createTrip() {
        wizardStep = 0;
        destination = "";
        startAt = 0;
        endAt = 0;
        tripBudget = 0;
        travelers = 1;
        chosenStops.clear();
        showWizard();
    }

    private void showWizard() {
        root.removeAllViews();
        LinearLayout header = row();
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = action("‹", WHITE, NAVY, this::onBackPressed);
        back.setTextSize(24);
        back.setContentDescription("Quay lại");
        header.addView(back, new LinearLayout.LayoutParams(dp(48), dp(48)));
        for (int i = 0; i < 4; i++) {
            View bar = new View(this);
            bar.setBackground(rounded(i <= wizardStep ? BLUE : BORDER, 6));
            LinearLayout.LayoutParams place = new LinearLayout.LayoutParams(0, dp(4), 1);
            place.setMargins(dp(3), 0, dp(3), 0);
            header.addView(bar, place);
        }
        header.setPadding(0, 0, dp(12), 0);
        root.addView(header);
        ScrollView scroll = new ScrollView(this);
        content = column();
        content.setPadding(dp(17), dp(11), dp(17), dp(16));
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout footer = column();
        footer.setBackgroundColor(WHITE);
        footer.setPadding(dp(16), dp(8), dp(16), dp(8));
        root.addView(footer);
        switch (wizardStep) {
            case 0 -> chooseDestination(footer);
            case 1 -> chooseDates(footer);
            case 2 -> chooseBudget(footer);
            case 3 -> chooseStops(footer);
        }
    }

    private void wizardTitle(String title, String detail) {
        line(content, title, 26, NAVY, true);
        line(content, detail, 14, MUTED, false);
        gap(content, 14);
    }

    private void chooseDestination(LinearLayout footer) {
        wizardTitle("Bạn muốn đi đâu?", "Chọn điểm đến hoặc nhập nơi bạn muốn khám phá.");
        EditText location = field(content, "Tìm điểm đến", InputType.TYPE_CLASS_TEXT, 60);
        location.setText(destination);
        for (int i = 0; i < DESTINATIONS.length; i += 2) {
            LinearLayout row = row();
            content.addView(row);
            for (int j = i; j < Math.min(DESTINATIONS.length, i + 2); j++) {
                String option = DESTINATIONS[j];
                LinearLayout tile = column();
                tile.setBackground(rounded(WHITE, 15));
                tile.setClipToOutline(true);
                LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(0, -2, 1);
                size.setMargins(dp(3), 0, dp(3), dp(7));
                row.addView(tile, size);
                if ("Ninh Bình".equals(option)) {
                    ImageView photo = new ImageView(this);
                    photo.setImageResource(R.drawable.trang_an);
                    photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    tile.addView(photo, new LinearLayout.LayoutParams(-1, dp(49)));
                }
                tile.addView(action(option, TINT, NAVY, () -> location.setText(option)));
            }
        }
        footer.addView(action("Tiếp tục", NAVY, WHITE, () -> {
            String selected = location.getText().toString().trim();
            if (selected.isEmpty()) { location.setError("Chọn hoặc nhập điểm đến"); return; }
            if (!selected.equals(destination)) chosenStops.clear();
            destination = selected;
            ((android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(location.getWindowToken(), 0);
            nextStep();
        }));
    }

    private void chooseDates(LinearLayout footer) {
        wizardTitle("Bạn đi khi nào?", "Chọn ngày bắt đầu và ngày kết thúc chuyến đi.");
        line(content, "CHỌN NGÀY ĐI VÀ NGÀY VỀ", 11, MUTED, true);
        gap(content, 6);
        content.addView(action(startAt == 0 ? "Chọn ngày bắt đầu" : "Ngày đi · " + dateText(startAt, "dd/MM/yyyy"),
                TINT, NAVY, () -> pickDate(true)));
        gap(content, 8);
        content.addView(action(endAt == 0 ? "Chọn ngày kết thúc" : "Ngày về · " + dateText(endAt, "dd/MM/yyyy"),
                TINT, NAVY, () -> pickDate(false)));
        if (startAt > 0 && endAt > 0) line(content, "Chuyến đi " + dayCount() + " ngày", 14, BLUE, true);
        footer.addView(action("Tiếp tục", NAVY, WHITE, () -> {
            if (startAt == 0 || endAt == 0) { notifyUser("Chọn đủ ngày đi và ngày về."); return; }
            if (dayCount() > 14) { notifyUser("Chọn chuyến đi tối đa 14 ngày."); return; }
            nextStep();
        }));
    }

    private void pickDate(boolean first) {
        Calendar initial = Calendar.getInstance();
        long existing = first ? startAt : endAt == 0 ? startAt : endAt;
        if (existing > 0) initial.setTimeInMillis(existing);
        DatePickerDialog picker = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar date = Calendar.getInstance();
            date.clear();
            date.set(year, month, dayOfMonth, 12, 0);
            if (first) {
                startAt = date.getTimeInMillis();
                if (endAt < startAt) endAt = 0;
            } else endAt = date.getTimeInMillis();
            showWizard();
        }, initial.get(Calendar.YEAR), initial.get(Calendar.MONTH), initial.get(Calendar.DAY_OF_MONTH));
        if (!first && startAt > 0) {
            picker.getDatePicker().setMinDate(startAt);
            picker.getDatePicker().setMaxDate(addDays(startAt, 13));
        }
        picker.show();
    }

    private void chooseBudget(LinearLayout footer) {
        wizardTitle("Ngân sách & số người", "Lên kế hoạch vừa túi tiền cho cả nhóm.");
        EditText budget = field(content, "Tổng ngân sách (₫)", InputType.TYPE_CLASS_NUMBER, 12);
        budget.setText(tripBudget == 0 ? "" : Long.toString(tripBudget));
        chipRow(new String[]{"3 triệu", "5 triệu", "10 triệu"}, -1,
                index -> budget.setText(new String[]{"3000000", "5000000", "10000000"}[index]), false);
        gap(content, 16);
        line(content, "Số người đi", 15, NAVY, true);
        NumberPicker people = new NumberPicker(this);
        people.setMinValue(1);
        people.setMaxValue(20);
        people.setValue(travelers);
        people.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        people.setContentDescription("Số người đi");
        content.addView(people);
        footer.addView(action("Tiếp tục", NAVY, WHITE, () -> {
            String amount = budget.getText().toString().trim();
            if (!amount.isEmpty() && !amount.matches("[0-9]{1,12}")) {
                budget.setError("Nhập tổng ngân sách bằng số");
                return;
            }
            tripBudget = amount.isEmpty() ? 0 : Long.parseLong(amount);
            travelers = people.getValue();
            ((android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(budget.getWindowToken(), 0);
            nextStep();
        }));
    }

    private void chooseStops(LinearLayout footer) {
        wizardTitle("Chọn địa điểm bạn thích", "Chọn các điểm muốn đưa vào chuyến " + destination + ".");
        String[] suggestions = switch (destination.toLowerCase(Locale.ROOT)) {
            case "phú quốc" -> new String[]{"Quần đảo An Thới", "Bãi Dài", "Bãi Gành Gió", "Bãi Kem", "Chợ đêm Phú Quốc"};
            case "ninh bình" -> new String[]{"Tràng An", "Hang Múa", "Tam Cốc", "Cố đô Hoa Lư", "Chùa Bái Đính"};
            case "hội an" -> new String[]{"Chùa Cầu", "Phố cổ Hội An", "Sông Hoài", "Làng rau Trà Quế"};
            case "đà nẵng" -> new String[]{"Bãi biển Mỹ Khê", "Bảo tàng Chăm", "Cầu Rồng", "Ngũ Hành Sơn"};
            default -> new String[0];
        };
        for (String name : suggestions) {
            CheckBox item = new CheckBox(this);
            item.setText(name);
            item.setMinHeight(dp(52));
            item.setChecked(chosenStops.contains(name));
            item.setOnCheckedChangeListener((view, checked) -> {
                if (checked) chosenStops.add(name);
                else chosenStops.remove(name);
            });
            card(content, WHITE).addView(item);
        }
        if (suggestions.length == 0) line(card(content, WHITE), "Bạn có thể tự thêm điểm hoặc hỏi trợ lý Gemini về " + destination + ".", 15, MUTED, false);
        footer.addView(action("Để AI gợi ý lịch trình", NAVY, WHITE, () -> finishWizard(false, true)));
        footer.addView(action("Tự tạo lịch trình từng ngày", WHITE, NAVY, () -> finishWizard(true, false)));
    }

    private void nextStep() {
        wizardStep++;
        showWizard();
    }

    private void finishWizard(boolean manual, boolean assistant) {
        Trip draft = new Trip(UUID.randomUUID().toString(), destination, destination, dayCount(), travelers,
                tripBudget, "", startAt, new ArrayList<>(chosenStops), manual);
        if (!saveTrip(draft)) return;
        wizardStep = -1;
        openTrip(draft, PLAN);
        if (assistant) openAssistant(draft, "Gợi ý lịch trình theo ngày cho chuyến " + draft.destination() + " của mình.");
    }

    private int dayCount() {
        for (int days = 1; days <= 14; days++) if (addDays(startAt, days - 1) >= endAt) return days;
        return 15;
    }

    private long addDays(long date, int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(date);
        calendar.add(Calendar.DAY_OF_YEAR, days);
        return calendar.getTimeInMillis();
    }

    private String dateText(long date, String format) {
        return new SimpleDateFormat(format, Locale.forLanguageTag("vi-VN")).format(new java.util.Date(date));
    }

    private boolean saveTrip(Trip draft) {
        try {
            JSONArray saved = savedArray("trips");
            saved.put(new JSONObject().put("id", draft.id()).put("title", draft.title())
                    .put("destination", draft.destination()).put("days", draft.days()).put("people", draft.people())
                    .put("budget", draft.budget()).put("interests", draft.interests()).put("startAt", draft.startAt())
                    .put("stops", new JSONArray(draft.stops())).put("manual", draft.manual()));
            if (!writeArray("trips", saved)) return false;
            trips.add(draft);
            return true;
        } catch (JSONException error) {
            notifyUser("Chưa lưu được chuyến đi.");
            return false;
        }
    }

    private void trips() {
        heading("KẾ HOẠCH DU LỊCH", "Chuyến đi của bạn");
        content.addView(action("Tạo chuyến đi", NAVY, WHITE, this::createTrip));
        gap(content, 12);
        for (int i = trips.size() - 1; i >= 0; i--) tripCard(trips.get(i), false);
        gap(content, 48);
    }

    private void tripCard(Trip item, boolean overview) {
        LinearLayout surface = column();
        surface.setBackground(rounded(WHITE, 19));
        surface.setClipToOutline(true);
        LinearLayout.LayoutParams placement = new LinearLayout.LayoutParams(-1, -2);
        placement.bottomMargin = dp(12);
        content.addView(surface, placement);
        if (item.destination().equalsIgnoreCase("Ninh Bình")) {
            ImageView photo = new ImageView(this);
            photo.setImageResource(R.drawable.trang_an);
            photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
            photo.setContentDescription("Phong cảnh Tràng An, ảnh của Jakub Hałun");
            surface.addView(photo, new LinearLayout.LayoutParams(-1, dp(overview ? 155 : 114)));
        }
        LinearLayout details = column();
        details.setPadding(dp(13), dp(10), dp(13), dp(10));
        surface.addView(details);
        line(details, item.title(), 18, NAVY, true);
        String period = tripDates(item);
        line(details, (period.isEmpty() ? "" : period + "  ·  ") + tripMeta(item), 13, MUTED, false);
        if (overview) {
            gap(details, 10);
            List<String> firstDay = activities(item, 0);
            if (!firstDay.isEmpty()) {
                line(details, "◷  Tiếp theo", 12, MUTED, false);
                line(details, firstDay.get(0), 13, NAVY, true);
            }
            gap(details, 9);
            line(details, "Sẵn sàng  " + packedCount(item) + "/" + PACKING.length, 12, MUTED, true);
            ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            progress.setProgressTintList(ColorStateList.valueOf(BLUE));
            progress.setMax(PACKING.length);
            progress.setProgress(packedCount(item));
            details.addView(progress);
        }
        if (overview || trip == null) {
            TextView open = action(overview ? "Xem lịch trình hôm nay" : "Xem chuyến đi  ›",
                    overview ? NAVY : WHITE, overview ? WHITE : BLUE, () -> openTrip(item, PLAN));
            if (!overview) open.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
            details.addView(open);
        }
    }

    private String tripMeta(Trip item) {
        return item.days() + " ngày  ·  " + item.people() + " người";
    }

    private String tripDates(Trip item) {
        if (item.id().isEmpty()) return "12–14 Th12";
        if (item.startAt() == 0) return "";
        return dateText(item.startAt(), "d/M") + "–" + dateText(addDays(item.startAt(), item.days() - 1), "d/M");
    }

    private void askAssistant() {
        openAssistant(trip != null ? trip : trips.size() > 1 ? trips.get(trips.size() - 1) : null, "");
    }

    private void openAssistant(Trip item, String prompt) {
        Intent intent = new Intent(this, AssistantActivity.class);
        if (item != null) intent.putExtra("trip", item.destination() + ", " + tripMeta(item)
                + (tripDates(item).isEmpty() ? "" : ", " + tripDates(item))
                + (item.budget() == 0 ? "" : ", ngân sách nhóm " + money(item.budget()))
                + (item.stops().isEmpty() ? "" : ", nơi muốn ghé: " + TextUtils.join(", ", item.stops())));
        intent.putExtra("prompt", prompt);
        startActivity(intent);
    }

    private void shortcuts(Trip item) {
        LinearLayout tiles = row();
        String[] names = {"Tạo Trip", "Ghi chi tiêu", "Tài liệu", "Lịch trình"};
        String[] glyphs = {"+", "₫", "▤", "▦"};
        int[] fills = {TINT, 0xFFE6F7EE, 0xFFFFF0DC, 0xFFE6F2FF};
        int[] inks = {BLUE, GREEN, 0xFF9A540B, 0xFF1561A7};
        for (int i = 0; i < names.length; i++) {
            int index = i;
            LinearLayout tile = column();
            tile.setGravity(Gravity.CENTER);
            TextView symbol = action(glyphs[i], fills[i], inks[i], () -> shortcut(item, index));
            symbol.setContentDescription(names[i]);
            symbol.setTextSize(20);
            tile.addView(symbol, new LinearLayout.LayoutParams(dp(46), dp(46)));
            TextView name = line(tile, names[i], 11, NAVY, true);
            name.setGravity(Gravity.CENTER);
            name.setPadding(0, dp(4), 0, dp(8));
            name.setOnClickListener(view -> shortcut(item, index));
            tiles.addView(tile, new LinearLayout.LayoutParams(0, -2, 1));
        }
        content.addView(tiles);
        gap(content, 12);
    }

    private void shortcut(Trip item, int index) {
        switch (index) {
            case 0 -> createTrip();
            case 1 -> openTrip(item, MONEY);
            case 2 -> documents(item);
            case 3 -> openTrip(item, PLAN);
        }
    }

    private void chipRow(String[] labels, int active, java.util.function.IntConsumer click, boolean scrollable) {
        LinearLayout chips = row();
        for (int i = 0; i < labels.length; i++) {
            int index = i;
            TextView chip = action(labels[i], i == active ? BLUE : WHITE, i == active ? WHITE : MUTED,
                    () -> click.accept(index));
            chip.setSelected(i == active);
            chip.setTextSize(12);
            LinearLayout.LayoutParams layout = scrollable ? new LinearLayout.LayoutParams(-2, -2)
                    : new LinearLayout.LayoutParams(0, -2, 1);
            layout.setMarginEnd(dp(4));
            chips.addView(chip, layout);
        }
        if (scrollable) {
            HorizontalScrollView strip = new HorizontalScrollView(this);
            strip.setHorizontalScrollBarEnabled(false);
            strip.addView(chips);
            content.addView(strip);
        } else content.addView(chips);
    }

    private void filters() {
        String[] labels = {"Tất cả", "Ăn uống", "Tham quan", "Thiên nhiên"};
        int active = java.util.Arrays.asList(labels).indexOf(discoverFilter);
        chipRow(labels, active, index -> {
            discoverFilter = labels[index];
            showTab(tab);
        }, true);
        gap(content, 8);
    }

    private void discover() {
        if (trip == null) {
            heading("GỢI Ý ĐIỂM ĐẾN", "Khám Phá");
            EditText search = field(content, "Tìm nơi, món ăn, chỗ ở", InputType.TYPE_CLASS_TEXT, 100);
            content.addView(action("Tìm trên Google Maps", NAVY, WHITE, () -> {
                String query = search.getText().toString().trim();
                if (!query.isEmpty()) openMaps(query);
                else search.setError("Nhập nơi bạn muốn tìm");
            }));
            gap(content, 10);
            chipRow(new String[]{"Cẩm nang", "Chỗ ở", "Hỏi AI"}, -1, index -> {
                if (index == 0) new AlertDialog.Builder(this).setTitle("Cẩm nang")
                        .setItems(new String[]{"Ninh Bình 3 ngày", "Hỏi trợ lý về điểm đến"}, (dialog, which) -> {
                            if (which == 0) openTrip(SAMPLE, GUIDE);
                            else openAssistant(null, "Gợi ý cẩm nang du lịch cho mình.");
                        }).show();
                else if (index == 1) openAssistant(null, "Gợi ý chỗ ở phù hợp cho chuyến đi của mình.");
                else askAssistant();
            }, false);
            gap(content, 10);
        }
        else line(content, "Khám phá quanh " + trip.destination(), 19, NAVY, true);
        if (trip != null && !trip.destination().equalsIgnoreCase("Ninh Bình")) {
            String place = trip.destination();
            content.addView(action("Tìm nơi tham quan trên Google Maps", TINT, NAVY, () -> openMaps("Điểm tham quan " + place)));
            content.addView(action("Hỏi AI về " + place, NAVY, WHITE, () -> openAssistant(trip, "Ở " + place + " nên đi đâu?")));
            return;
        }
        filters();
        places(true);
    }

    private void places(boolean showAll) {
        int shown = 0;
        for (String[] spot : DISCOVER) {
            if (!discoverFilter.equals("Tất cả") && !discoverFilter.equals(spot[1])) continue;
            LinearLayout entry = card(content, WHITE);
            line(entry, spot[1], 11, GREEN, true);
            line(entry, spot[0], 16, NAVY, true);
            if (showAll) line(entry, spot[2], 14, MUTED, false);
            if (showAll) entry.addView(action("Xem trên Google Maps", WHITE, BLUE, () -> openMaps(spot[0] + " Ninh Bình")));
            if (trip != null) entry.addView(action("+  Thêm vào ngày " + (day + 1), TINT, BLUE, () -> {
                List<String> list = activities(trip, day);
                list.add("09:00   " + spot[0]);
                if (saveActivities(list)) openTripDay();
            }));
            if (++shown >= 2 && !showAll) break;
        }
    }

    private void openMaps(String query) {
        openLink("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(query));
    }

    private void guide() {
        line(content, "Cẩm nang " + trip.destination(), 20, NAVY, true);
        line(content, "Gợi ý chuẩn bị và khám phá điểm đến", 14, MUTED, false);
        gap(content, 8);
        for (String[] item : new String[][]{
                {"Di chuyển", "Gợi ý phương tiện và cách đi lại ở " + trip.destination()},
                {"Ăn uống", "Món địa phương và quán ăn ở " + trip.destination()},
                {"Chỗ ở", "Nên ở khu vực nào tại " + trip.destination() + " để tiện di chuyển?"},
                {"Mẹo đi chơi", "Cần lưu ý gì trước khi đi " + trip.destination() + "?"}
        }) {
            LinearLayout row = card(content, WHITE);
            line(row, item[0], 16, NAVY, true);
            row.addView(action("Hỏi trợ lý  ›", TINT, BLUE, () -> openAssistant(trip, item[1])));
        }
    }

    private void memories() {
        heading("NHẬT KÝ & KỶ NIỆM", "Kỷ niệm");
        LinkedHashSet<String> locations = new LinkedHashSet<>();
        for (Trip item : trips) {
            if (item.startAt() > 0 && item.startAt() <= System.currentTimeMillis()) locations.add(item.destination());
        }
        LinearLayout visited = card(content, 0xFFE7F6EC);
        line(visited, "Nơi bạn đã đi", 14, GREEN, true);
        line(visited, locations.size() + " điểm đến", 23, NAVY, true);
        line(visited, locations.isEmpty() ? "Chuyến đi đã bắt đầu sẽ xuất hiện ở đây." : TextUtils.join(" · ", locations), 13, MUTED, false);
        line(content, "Nhật ký", 19, NAVY, true);
        gap(content, 8);
        for (int i = trips.size() - 1; i >= 0; i--) {
            Trip item = trips.get(i);
            int notes = savedArray("journal:" + item.id()).length();
            LinearLayout entry = card(content, WHITE);
            line(entry, item.title(), 16, NAVY, true);
            line(entry, notes + " ghi chép", 13, MUTED, false);
            entry.addView(action("Viết nhật ký", TINT, NAVY, () -> openTrip(item, JOURNAL)));
        }
        content.addView(action("Tạo chuyến đi", NAVY, WHITE, this::createTrip));
    }

    private void calendar() {
        heading("TỔNG HỢP CHUYẾN ĐI", "Lịch");
        for (Trip item : trips) {
            if (item.startAt() == 0) continue;
            LinearLayout entry = card(content, WHITE);
            line(entry, tripDates(item) + "  ·  " + item.destination(), 12, BLUE, true);
            line(entry, item.title(), 16, NAVY, true);
            entry.addView(action("Xem lịch trình", TINT, BLUE, () -> openTrip(item, PLAN)));
        }
        line(content, "Tháng 12", 17, NAVY, true);
        gap(content, 8);
        for (int i = 0; i < SAMPLE.days(); i++) {
            int selected = i;
            LinearLayout entry = card(content, WHITE);
            line(entry, (12 + i) + " THG 12  ·  " + SAMPLE.destination(), 12, BLUE, true);
            line(entry, DAY_NAMES[i], 16, NAVY, true);
            entry.addView(action("Xem lịch ngày " + (i + 1), TINT, BLUE, () -> {
                trip = SAMPLE;
                day = selected;
                openTripDay();
            }));
        }
    }

    private String dayTitle(Trip item, int index) {
        if (item.destination().equalsIgnoreCase("Ninh Bình") && index < DAY_NAMES.length) return DAY_NAMES[index];
        if (index == 0) return "Đến " + item.destination() + " và khám phá";
        if (index == item.days() - 1) return "Tận hưởng buổi cuối và trở về";
        return item.interests().isEmpty() ? "Khám phá địa phương" : item.interests();
    }

    private String activityKey(Trip item, int index) {
        return "plan:" + item.id() + ":" + index;
    }

    private List<String> activities(Trip item, int index) {
        List<String> result = new ArrayList<>();
        String key = activityKey(item, index);
        if (storage.contains(key)) {
            JSONArray saved = savedArray(key);
            for (int i = 0; i < saved.length(); i++) result.add(saved.optString(i));
        } else if (!item.stops().isEmpty()) {
            for (int i = index; i < item.stops().size(); i += item.days()) {
                result.add(String.format(Locale.ROOT, "%02d:00   %s", 9 + 2 * (i / item.days()), item.stops().get(i)));
            }
        } else if (item.manual()) {
            return result;
        } else if (item.destination().equalsIgnoreCase("Ninh Bình") && index < PLACES.length) {
            java.util.Collections.addAll(result, PLACES[index]);
        } else {
            result.add("09:00   " + (index == 0 ? "Đến nơi và gửi hành lý" : "Khám phá " + item.destination()));
            result.add("12:00   Thưởng thức món địa phương");
            result.add(index == item.days() - 1 ? "15:00   Chuẩn bị về" : "15:00   Tự do tham quan");
        }
        return result;
    }

    private boolean saveActivities(List<String> list) {
        list.sort(String::compareTo);
        return writeArray(activityKey(trip, day), new JSONArray(list));
    }

    private void openTripDay() {
        section = PLAN;
        showTab(TRIPS);
    }

    private void editActivity(int index) {
        List<String> list = activities(trip, day);
        LinearLayout fields = column();
        fields.setPadding(dp(22), 0, dp(22), 0);
        EditText title = field(fields, "Tên hoạt động", InputType.TYPE_CLASS_TEXT, 80);
        EditText time = field(fields, "Giờ (HH:mm)", InputType.TYPE_CLASS_TEXT, 5);
        time.setText(index < 0 ? "09:00" : list.get(index).substring(0, 5));
        if (index >= 0) title.setText(list.get(index).substring(5).trim());
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(index < 0 ? "Thêm hoạt động" : "Chỉnh hoạt động")
                .setView(fields).setNegativeButton("Hủy", null).setPositiveButton("Lưu", null).create();
        dialog.setOnShowListener(event -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            String name = title.getText().toString().trim();
            String clock = time.getText().toString().trim();
            if (name.isEmpty()) { title.setError("Nhập tên hoạt động"); return; }
            if (!clock.matches("(?:[01][0-9]|2[0-3]):[0-5][0-9]")) { time.setError("Nhập giờ theo dạng 09:30"); return; }
            if (index >= 0) list.set(index, clock + "   " + name);
            else list.add(clock + "   " + name);
            if (!saveActivities(list)) return;
            dialog.dismiss();
            openTripDay();
        }));
        dialog.show();
    }

    private int packedCount(Trip item) {
        return Integer.bitCount(storage.getInt("packing:" + item.id(), 0));
    }

    private void packing(Trip item) {
        String key = "packing:" + item.id();
        int mask = storage.getInt(key, 0);
        boolean[] checked = new boolean[PACKING.length];
        for (int i = 0; i < checked.length; i++) checked[i] = (mask & 1 << i) != 0;
        new AlertDialog.Builder(this).setTitle("Hành lý · " + item.title())
                .setMultiChoiceItems(PACKING, checked, (dialog, index, value) -> checked[index] = value)
                .setNegativeButton("Hủy", null).setPositiveButton("Lưu", (dialog, which) -> {
                    int updated = 0;
                    for (int i = 0; i < checked.length; i++) if (checked[i]) updated |= 1 << i;
                    storage.edit().putInt(key, updated).apply();
                    showTab(tab);
                }).show();
    }

    private void documents(Trip item) {
        String key = "docs:" + item.id();
        JSONArray docs = savedArray(key);
        String[] titles = new String[docs.length()];
        for (int i = 0; i < docs.length(); i++) titles[i] = docs.optJSONObject(i).optString("title");
        AlertDialog.Builder dialog = new AlertDialog.Builder(this).setTitle("Tài liệu · " + item.title());
        if (titles.length == 0) dialog.setMessage("Lưu vé, xác nhận đặt phòng và tài liệu của chuyến đi.");
        else dialog.setItems(titles, (popup, index) -> {
            Uri uri = Uri.parse(docs.optJSONObject(index).optString("uri"));
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
            } catch (ActivityNotFoundException | SecurityException error) {
                notifyUser("Không mở được tài liệu này.");
            }
        });
        dialog.setNegativeButton("Đóng", null).setPositiveButton("Thêm tài liệu", (popup, which) -> {
            documentKey = key;
            try {
                startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*")
                        .addCategory(Intent.CATEGORY_OPENABLE), 8);
            } catch (ActivityNotFoundException error) {
                notifyUser("Thiết bị chưa có trình chọn tài liệu.");
            }
        }).show();
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != 8 || result != RESULT_OK || data == null || data.getData() == null || documentKey == null) return;
        Uri uri = data.getData();
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            String name = "Tài liệu";
            try (android.database.Cursor cursor = getContentResolver().query(uri,
                    new String[]{android.provider.OpenableColumns.DISPLAY_NAME}, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) name = cursor.getString(0);
            }
            JSONArray docs = savedArray(documentKey);
            docs.put(new JSONObject().put("uri", uri.toString()).put("title", name));
            if (writeArray(documentKey, docs)) notifyUser("Đã lưu tài liệu");
        } catch (JSONException | SecurityException error) {
            notifyUser("Chưa lưu được tài liệu.");
        } finally {
            documentKey = null;
        }
    }

    private void members() {
        new AlertDialog.Builder(this).setTitle("Thành viên · " + trip.people())
                .setItems(people(), null).setNegativeButton("Đóng", null)
                .setPositiveButton("Chia sẻ kế hoạch", (dialog, which) -> {
                    StringBuilder plan = new StringBuilder(trip.title() + "\n" + tripMeta(trip));
                    for (int i = 0; i < trip.days(); i++) plan.append("\n\nNgày ").append(i + 1).append("\n")
                            .append(TextUtils.join("\n", activities(trip, i)));
                    startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND).setType("text/plain")
                            .putExtra(Intent.EXTRA_TEXT, plan.toString()), "Chia sẻ kế hoạch"));
                }).show();
    }

    private void journal() {
        String key = "journal:" + trip.id();
        JSONArray notes = savedArray(key);
        line(content, "Nhật ký chuyến đi", 19, NAVY, true);
        gap(content, 8);
        if (notes.length() == 0) line(card(content, WHITE), "Ghi lại kỷ niệm đầu tiên của bạn.", 14, MUTED, false);
        for (int i = notes.length() - 1; i >= 0; i--) line(card(content, WHITE), notes.optString(i), 15, NAVY, false);
        content.addView(action("+  Viết nhật ký", TINT, BLUE, () -> {
            LinearLayout fields = column();
            fields.setPadding(dp(22), 0, dp(22), 0);
            EditText note = field(fields, "Kỷ niệm của bạn", InputType.TYPE_CLASS_TEXT, 500);
            note.setSingleLine(false);
            new AlertDialog.Builder(this).setTitle("Viết nhật ký").setView(fields)
                    .setNegativeButton("Hủy", null).setPositiveButton("Lưu", (dialog, which) -> {
                        String text = note.getText().toString().trim();
                        if (!text.isEmpty() && writeArray(key, notes.put(text))) showTab(TRIPS);
                    }).show();
        }));
    }

    private JSONArray savedArray(String key) {
        try {
            return new JSONArray(storage.getString(key, "[]"));
        } catch (JSONException error) {
            notifyUser("Không đọc được dữ liệu đã lưu.");
            return new JSONArray();
        }
    }

    private boolean writeArray(String key, JSONArray data) {
        if (storage.edit().putString(key, data.toString()).commit()) return true;
        notifyUser("Chưa lưu được dữ liệu. Vui lòng thử lại.");
        return false;
    }

    private void notifyUser(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void expenses() {
        loadExpenses();
        String[] people = people();
        long total = 0;
        long share = 0;
        long paid = 0;
        for (Expense item : expenses) {
            total += item.amount();
            share += shareOf(item, 0);
            if (item.payer() == 0) paid += item.amount();
        }
        long balance = paid - share;
        line(content, "Chi tiêu", 20, NAVY, true);
        gap(content, 10);
        LinearLayout summary = card(content, BLUE);
        line(summary, "TỔNG CHI TIÊU", 12, WHITE, true);
        line(summary, money(total), 27, WHITE, true);
        gap(summary, 9);
        line(summary, "Phần của bạn  ·  " + money(share), 13, WHITE, false);
        line(summary, balance == 0 ? "Bạn đã cân bằng chi phí" :
                (balance > 0 ? "Bạn được nhận  " : "Bạn cần trả  ") + money(Math.abs(balance)), 15, WHITE, true);

        LinearLayout budget = card(content, WHITE);
        line(budget, "Ngân sách nhóm", 17, NAVY, true);
        long limit = trip.budget();
        line(budget, limit == 0 ? "Chưa đặt ngân sách" : money(total) + " / " + money(limit), 14, limit > 0 && total > limit ? RED : MUTED, false);
        ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setProgressTintList(ColorStateList.valueOf(total > limit ? RED : BLUE));
        progress.setProgress(limit == 0 ? 0 : (int) Math.min(100, total * 100.0 / limit));
        budget.addView(progress);
        chipRow(new String[]{"+ Thêm khoản chi", "Xem công nợ"}, 0, index -> {
            if (index == 0) addExpense();
            else showSettlement();
        }, false);
        gap(content, 16);
        line(content, "Khoản chi  ·  " + expenses.size(), 19, NAVY, true);
        gap(content, 9);
        for (Expense item : expenses) {
            LinearLayout entry = card(content, WHITE);
            line(entry, item.name(), 16, NAVY, true);
            line(entry, people[item.payer()] + " đã trả  ·  " + money(item.amount()), 14, MUTED, false);
        }
    }

    private String expenseKey() {
        return trip.id().isEmpty() ? "expenses" : "expenses:" + trip.id();
    }

    private String[] people() {
        if (trip.id().isEmpty()) return PEOPLE;
        String[] names = new String[trip.people()];
        names[0] = "Bạn";
        for (int i = 1; i < names.length; i++) names[i] = "Thành viên " + (i + 1);
        return names;
    }

    private void loadExpenses() {
        expenses.clear();
        int all = (1 << trip.people()) - 1;
        if (trip.id().isEmpty()) {
            expenses.add(new Expense("Homestay 2 đêm", 1, 3_200_000, all));
            expenses.add(new Expense("Ăn tối dê núi", 0, 1_250_000, all));
            expenses.add(new Expense("Thuê xe 3 ngày", 3, 2_100_000, all));
        }
        JSONArray saved = savedArray(expenseKey());
        for (int i = 0; i < saved.length(); i++) {
            JSONObject item = saved.optJSONObject(i);
            if (item == null) continue;
            int payer = item.optInt("payer", -1);
            long amount = item.optLong("amount");
            int participants = item.optInt("participants", all) & all;
            if (payer >= 0 && payer < trip.people() && amount > 0 && participants != 0) {
                expenses.add(0, new Expense(item.optString("name"), payer, amount, participants));
            }
        }
    }

    private void addExpense() {
        LinearLayout fields = column();
        fields.setPadding(dp(24), 0, dp(24), dp(8));
        ScrollView scroll = new ScrollView(this);
        scroll.addView(fields);
        EditText name = field(fields, "Tên khoản chi", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, 80);
        EditText amount = field(fields, "Số tiền (₫)", InputType.TYPE_CLASS_NUMBER, 12);
        String[] names = people();
        line(fields, "Người đã trả", 14, NAVY, true);
        Spinner payer = new Spinner(this);
        ArrayAdapter<String> options = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
        options.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        payer.setAdapter(options);
        fields.addView(payer, new LinearLayout.LayoutParams(-1, dp(46)));
        line(fields, "Chia đều cho những người đã chọn", 13, MUTED, false);
        CheckBox[] participants = new CheckBox[names.length];
        for (int i = 0; i < names.length; i++) {
            participants[i] = new CheckBox(this);
            participants[i].setText(names[i]);
            participants[i].setChecked(true);
            fields.addView(participants[i]);
        }
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Thêm khoản chi").setView(scroll)
                .setNegativeButton("Hủy", null).setPositiveButton("Lưu", null).create();
        dialog.setOnShowListener(event -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            String title = name.getText().toString().trim();
            if (title.isEmpty()) {
                name.setError("Nhập tên khoản chi");
                return;
            }
            long value = positiveAmount(amount);
            if (value == 0) return;
            int selected = 0;
            for (int i = 0; i < participants.length; i++) if (participants[i].isChecked()) selected |= 1 << i;
            if (selected == 0) {
                notifyUser("Chọn ít nhất một người để chia.");
                return;
            }
            try {
                JSONArray saved = savedArray(expenseKey());
                saved.put(new JSONObject().put("name", title).put("payer", payer.getSelectedItemPosition())
                        .put("amount", value).put("participants", selected));
                if (!writeArray(expenseKey(), saved)) return;
                dialog.dismiss();
                showTab(TRIPS);
            } catch (JSONException error) {
                notifyUser("Chưa lưu được khoản chi.");
            }
        }));
        dialog.show();
    }

    private long shareOf(Expense expense, int person) {
        int bit = 1 << person;
        int selected = expense.participants();
        if ((selected & bit) == 0) return 0;
        int size = Integer.bitCount(selected);
        int position = Integer.bitCount(selected & (bit - 1));
        return expense.amount() / size + (position < expense.amount() % size ? 1 : 0);
    }

    private void showSettlement() {
        String[] people = people();
        long[] balances = new long[people.length];
        for (Expense item : expenses) {
            balances[item.payer()] += item.amount();
            for (int i = 0; i < balances.length; i++) balances[i] -= shareOf(item, i);
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
            transfers.add(people[sender] + "  →  " + people[receiver] + "  ·  " + money(value));
            balances[sender] += value;
            balances[receiver] -= value;
        }
        ScrollView scroll = new ScrollView(this);
        LinearLayout sheet = column();
        sheet.setPadding(dp(16), dp(4), dp(16), dp(4));
        scroll.addView(sheet);
        LinearLayout stats = card(sheet, BLUE);
        line(stats, "Kế hoạch thanh toán", 17, WHITE, true);
        line(stats, transfers.size() + " giao dịch đề xuất  ·  " + people.length + " thành viên", 13, WHITE, false);
        if (transfers.isEmpty()) line(card(sheet, PAGE), "Mọi người đã cân bằng chi phí.", 14, MUTED, false);
        for (String transfer : transfers) line(card(sheet, 0xFFEAF7EF), transfer, 14, GREEN, true);
        new AlertDialog.Builder(this).setTitle("Tối ưu công nợ")
                .setView(scroll)
                .setPositiveButton("Đóng", null).show();
    }

    private void profile() {
        heading("TRAVEL VN", "Menu");
        LinearLayout account = card(content, WHITE);
        line(account, "HỒ SƠ TRÊN THIẾT BỊ", 12, BLUE, true);
        line(account, storage.getString("displayName", "Minh Nguyễn"), 22, NAVY, true);
        account.addView(action("Sửa tên hiển thị", TINT, BLUE, () -> {
            LinearLayout fields = column();
            fields.setPadding(dp(20), 0, dp(20), 0);
            EditText name = field(fields, "Tên hiển thị", InputType.TYPE_CLASS_TEXT, 48);
            name.setText(storage.getString("displayName", "Minh Nguyễn"));
            new AlertDialog.Builder(this).setTitle("Hồ sơ").setView(fields)
                    .setNegativeButton("Hủy", null).setPositiveButton("Lưu", (dialog, index) -> {
                        String text = name.getText().toString().trim();
                        if (!text.isEmpty()) storage.edit().putString("displayName", text).apply();
                        showTab(MENU);
                    }).show();
        }));
        LinearLayout group = card(content, WHITE);
        line(group, "CHUYẾN ĐI", 12, BLUE, true);
        group.addView(action("Chuyến đi của bạn  ›", WHITE, NAVY, () -> navigate(TRIPS)));
        group.addView(action("Lịch chuyến đi  ›", WHITE, NAVY, () -> navigate(CALENDAR)));
        group.addView(action("Ví giấy tờ  ›", WHITE, NAVY, () -> documents(trips.get(trips.size() - 1))));
        group.addView(action("Công nợ  ›", WHITE, NAVY, () -> openTrip(trips.get(trips.size() - 1), MONEY)));
        LinearLayout about = card(content, TINT);
        line(about, "Travel VN · 1.2", 15, BLUE, true);
        line(about, "Tiếng Việt  ·  Dữ liệu lưu trên thiết bị", 14, MUTED, false);
        about.addView(action("Ảnh Tràng An · Jakub Hałun · CC BY 4.0", WHITE, BLUE, this::photoCredit));
    }

    private void photoCredit() {
        new AlertDialog.Builder(this).setTitle("Ảnh bìa Tràng An")
                .setMessage("Jakub Hałun, Wikimedia Commons. Giấy phép CC BY 4.0. Ảnh được thu nhỏ và cắt khung khi hiển thị.")
                .setNegativeButton("Đóng", null)
                .setPositiveButton("Nguồn ảnh", (dialog, which) -> openLink("https://commons.wikimedia.org/wiki/File:Trang_An_Landscape_Complex,_Ninh_Binh_Province,_Vietnam,_20240202_1428_5272.jpg"))
                .setNeutralButton("Giấy phép", (dialog, which) -> openLink("https://creativecommons.org/licenses/by/4.0/"))
                .show();
    }

    private void openLink(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException error) {
            notifyUser("Thiết bị chưa có trình duyệt.");
        }
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
        GradientDrawable background = rounded(color, 15);
        if (color == WHITE) background.setStroke(dp(1), BORDER);
        card.setBackground(background);
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
