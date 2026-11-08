package vn.travelvn.app;

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
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
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

public final class MainActivity extends TravelScreen {
    private static final int BLUE = 0xFF265FC4;
    private static final int NAVY = 0xFF102746;
    private static final int MUTED = 0xFF52647E;
    private static final int PAGE = 0xFFF3F8FC;
    private static final int TINT = 0xFFEAF1FF;
    private static final int BORDER = 0xFFE8EDF5;
    private static final int WHITE = Color.WHITE;
    private static final int GREEN = 0xFF086644;
    private static final int RED = 0xFFAA3030;
    private static final String[] TABS = {"Trang chủ", "Khám phá", "Kỷ niệm", "Menu"};
    private static final String[] EXPLORE_FILTERS = {"Tất cả", "Cẩm nang", "Chỗ ở", "Tham quan", "Ăn uống",
            "Biển", "Thiên nhiên", "Cà phê", "Lịch sử", "Giải trí", "Mua sắm"};
    private static final int HOME = 0, EXPLORE = 1, MEMORIES = 2, MENU = 3, TRIPS = 4, CALENDAR = 5;
    private static final int PLAN = 0, TRIP_EXPLORE = 1, GUIDE = 2, MONEY = 3, JOURNAL = 4;
    private static final int ACCOUNT_REQUEST = 42;
    private static final int[] NAV_ICONS = {0, 2, 8, 9};
    private static final String[] SECTIONS = {"Kế hoạch", "Khám phá", "Cẩm nang", "Chi tiêu", "Nhật ký"};
    private static final String[] DESTINATIONS = {"Phú Quốc", "Nha Trang", "Huế", "Sa Pa", "Quy Nhơn", "Đà Lạt", "Hội An", "Phú Yên", "Mũi Né", "Ninh Bình", "Đà Nẵng"};
    private static final String[] PEOPLE = {"Bạn", "Linh", "An", "Huy", "Mai"};
    private static final String[] PACKING = {"Giấy tờ tùy thân", "Sạc điện thoại", "Đồ vệ sinh cá nhân", "Áo khoác nhẹ", "Giày đi bộ", "Thuốc cá nhân"};
    private static final String[] DAY_NAMES = {"Cố đô & Tràng An", "Bái Đính & Thung Nham", "Tam Cốc thong thả"};
    private static final String[][] PLACES = {
        {"08:00   Cố đô Hoa Lư", "10:30   Tràng An", "15:30   Hang Múa"},
        {"08:30   Chùa Bái Đính", "12:00   Cơm cháy dê núi", "14:00   Vườn chim Thung Nham"},
        {"07:30   Đạp xe Tam Cốc", "10:00   Bến thuyền Tam Cốc", "13:30   Cà phê ngắm núi"}
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
    private String exploreCity = "";
    private int exploreCategory;
    private String exploreQuery = "";
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
            exploreCity = state.getString("exploreCity", "");
            exploreCategory = state.getInt("exploreCategory");
            exploreQuery = state.getString("exploreQuery", "");
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
        state.putString("exploreCity", exploreCity);
        state.putInt("exploreCategory", exploreCategory);
        state.putString("exploreQuery", exploreQuery);
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
        openTrip(selected, selectedSection, 0);
    }

    private void openTrip(Trip selected, int selectedSection, int selectedDay) {
        trip = selected;
        section = selectedSection;
        day = selectedDay;
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
            FrameLayout.LayoutParams pos = new FrameLayout.LayoutParams(dp(80), dp(48), Gravity.BOTTOM | Gravity.END);
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
            FrameLayout badge = new FrameLayout(this);
            badge.setBackground(rounded(active ? TINT : WHITE, 19));
            badge.addView(new TravelIcon(this, NAV_ICONS[i], color),
                    new FrameLayout.LayoutParams(dp(21), dp(21), Gravity.CENTER));
            item.addView(badge, new LinearLayout.LayoutParams(dp(51), dp(29)));
            TextView label = line(item, TABS[i], 11, color, active);
            label.setGravity(Gravity.CENTER);
            item.setContentDescription(TABS[i]);
            item.setSelected(active);
            item.setFocusable(true);
            item.setOnClickListener(view -> navigate(index));
            nav.addView(item, new LinearLayout.LayoutParams(0, -1, 1));
        }
        if (trip == null && tab == MENU && !signedIn()) menuGuestFooter();
        root.addView(nav, new LinearLayout.LayoutParams(-1, dp(60)));
    }

    private void home() {
        boolean signedIn = TravelCloud.session(this).has("uid");
        Trip active = signedIn ? homeTrip() : null;
        LinearLayout header = row();
        header.setGravity(Gravity.CENTER_VERTICAL);
        String name = signedIn ? accountName() : "Du khách";
        String[] words = name.split("\\s+");
        String initials = initials(words[0]) + (words.length > 1 ? initials(words[words.length - 1]) : "");
        TextView avatar = action(initials, TINT, NAVY, this::openAccount);
        avatar.setContentDescription(signedIn ? "Hồ sơ cá nhân" : "Đăng nhập Travel VN");
        header.addView(avatar, new LinearLayout.LayoutParams(dp(44), dp(44)));
        LinearLayout greeting = column();
        greeting.setPadding(dp(12), 0, dp(8), 0);
        header.addView(greeting, new LinearLayout.LayoutParams(0, -2, 1));
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        line(greeting, hour < 12 ? "Chào buổi sáng ☀" : hour < 18 ? "Chào buổi chiều ☀" : "Chào buổi tối ☾",
                13, MUTED, false);
        TextView displayName = line(greeting, name, 18, NAVY, true);
        displayName.setMaxLines(1);
        displayName.setEllipsize(TextUtils.TruncateAt.END);
        header.addView(homeIcon(5, TINT, "Thông báo", () -> homeReminders(active)),
                new LinearLayout.LayoutParams(dp(48), dp(48)));
        content.addView(header);
        gap(content, 23);
        int today = active == null ? -1 : tripToday(active);
        homeHero(active, today);
        if (active == null) {
            if (!storage.getBoolean("hideHomeChecklist", false)) homeChecklist();
        } else if (today >= 0) {
            homeTripToday(active, today);
        }
        homeShortcuts(active, today);
        homeNearby(active);
        if (signedIn) homeRecentTrips();
        visitedCard();
        gap(content, 64);
    }

    private String initials(String word) {
        return word.substring(0, word.offsetByCodePoints(0, 1)).toUpperCase(Locale.ROOT);
    }

    private Trip homeTrip() {
        long today = dayStart(System.currentTimeMillis());
        Trip upcoming = null;
        for (int index = trips.size() - 1; index > 0; index--) {
            Trip item = trips.get(index);
            if (item.startAt() <= 0) continue;
            long start = dayStart(item.startAt());
            if (start <= today && addDays(start, item.days()) > today) return item;
            if (start > today && (upcoming == null || item.startAt() < upcoming.startAt())) upcoming = item;
        }
        return upcoming;
    }

    private long dayStart(long timestamp) {
        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(timestamp);
        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);
        return date.getTimeInMillis();
    }

    private int tripToday(Trip item) {
        long today = dayStart(System.currentTimeMillis());
        long start = dayStart(item.startAt());
        if (item.startAt() <= 0 || today < start) return -1;
        for (int day = 0; day < item.days(); day++) {
            if (today < addDays(start, day + 1)) return day;
        }
        return -1;
    }

    private void homeHero(Trip active, int today) {
        TravelCatalog.Destination city = TravelCatalog.destination(active == null ? "Nha Trang" : active.destination());
        FrameLayout hero = new FrameLayout(this);
        hero.setBackground(rounded(TINT, 23));
        hero.setClipToOutline(true);
        if (city != null) {
            ImageView image = new ImageView(this);
            image.setImageResource(city.image());
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setContentDescription("Ảnh " + city.name() + " của " + city.author());
            hero.addView(image, new FrameLayout.LayoutParams(-1, -1));
        } else {
            hero.addView(new TravelIcon(this, 2, BLUE),
                    new FrameLayout.LayoutParams(dp(64), dp(64), Gravity.CENTER));
        }
        if (active != null) {
            TextView state = new TextView(this);
            state.setText(today >= 0 ? "●  Đang diễn ra" : "●  Sắp khởi hành");
            state.setTextColor(NAVY);
            state.setTextSize(12);
            state.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            state.setPadding(dp(12), dp(7), dp(12), dp(7));
            state.setBackground(rounded(WHITE, 18));
            FrameLayout.LayoutParams position = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.START);
            position.setMargins(dp(12), dp(12), dp(12), 0);
            hero.addView(state, position);
        }
        content.addView(hero, new LinearLayout.LayoutParams(-1, dp(210)));
        gap(content, 13);
        String tag = active == null ? "GỢI Ý CHO BẠN · 3 NGÀY" : today >= 0
                ? "NGÀY " + (today + 1) + "/" + active.days() + " · " + active.people() + " NGƯỜI"
                : tripDates(active) + " · " + tripMeta(active).toUpperCase(Locale.ROOT);
        line(content, tag, 12, BLUE, true).setLetterSpacing(0.08f);
        gap(content, 5);
        line(content, active == null ? "Khám phá Nha Trang" : active.destination(), 32, NAVY, true);
        gap(content, 4);
        List<String> plan = today >= 0 ? activities(active, today) : java.util.Collections.emptyList();
        String detail = active == null ? city.description() : today < 0 ? active.title()
                : plan.isEmpty() ? "Bạn có thể thêm điểm dừng cho hôm nay." : "Trong lịch hôm nay · " + plan.get(0).substring(5).trim();
        line(content, detail, 14, MUTED, false).setLineSpacing(dp(3), 1);
        gap(content, 13);
        TextView primary = action(active == null ? "Lên kế hoạch cho nhóm"
                        : today >= 0 ? "Xem lịch trình hôm nay" : "Xem lịch trình chuyến đi", NAVY, WHITE,
                () -> { if (active == null) createTrip("Nha Trang"); else openTrip(active, PLAN, Math.max(today, 0)); });
        primary.setTextSize(15);
        primary.setMinHeight(dp(49));
        content.addView(primary);
        gap(content, 22);
    }

    private void homeReminders(Trip active) {
        String detail = active == null ? "Bạn chưa có nhắc việc cho chuyến đi."
                : active.title() + " · " + tripDates(active) + "\nHành lý đã chuẩn bị " + packedCount(active) + "/" + PACKING.length;
        new AlertDialog.Builder(this).setTitle("Nhắc việc chuyến đi").setMessage(detail)
                .setNegativeButton("Đóng", null).setPositiveButton("Chuyến đi của bạn", (d, w) -> navigate(TRIPS)).show();
    }

    private void homeChecklist() {
        Trip latest = trips.size() > 1 ? trips.get(trips.size() - 1) : null;
        String style = storage.getString("travelStyle", "");
        boolean[] done = {!style.isEmpty(), latest != null, storage.getBoolean("openedSharePlan", false),
                latest != null && savedArray("expenses:" + latest.id()).length() > 0};
        int count = 0;
        for (boolean value : done) if (value) count++;
        LinearLayout panel = card(content, WHITE);
        LinearLayout title = row();
        title.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout text = column();
        line(text, "Bắt đầu với Travel VN", 19, NAVY, true);
        line(text, count + "/4 việc · khoảng 3 phút", 13, MUTED, false);
        title.addView(text, new LinearLayout.LayoutParams(0, -2, 1));
        TextView close = action("×", WHITE, MUTED, () -> {
            storage.edit().putBoolean("hideHomeChecklist", true).apply();
            panel.setVisibility(View.GONE);
        });
        close.setTextSize(21);
        close.setContentDescription("Ẩn hướng dẫn bắt đầu");
        title.addView(close, new LinearLayout.LayoutParams(dp(48), dp(48)));
        panel.addView(title);
        ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(done.length);
        progress.setProgress(count);
        progress.setProgressTintList(ColorStateList.valueOf(BLUE));
        panel.addView(progress, new LinearLayout.LayoutParams(-1, dp(9)));
        gap(panel, 5);
        String[] tasks = {"Chọn phong cách du lịch", "Tạo chuyến đầu tiên", "Chia sẻ kế hoạch", "Ghi khoản chi đầu tiên"};
        for (int index = 0; index < tasks.length; index++) {
            int selected = index;
            LinearLayout step = row();
            step.setGravity(Gravity.CENTER_VERTICAL);
            TextView name = line(step, (done[index] ? "✓  " : "○  ") + tasks[index], 14, NAVY, true);
            name.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
            TextView open = action(done[index] ? "Xem" : "Làm", WHITE, BLUE, () -> {
                if (selected == 0) chooseTravelStyle();
                else if (latest == null) createTrip();
                else if (selected == 1) openTrip(latest, PLAN);
                else if (selected == 2) shareTrip(latest);
                else openTrip(latest, MONEY);
            });
            open.setContentDescription(tasks[index]);
            step.addView(open, new LinearLayout.LayoutParams(dp(49), dp(48)));
            panel.addView(step);
            if (index < tasks.length - 1) {
                View divider = new View(this);
                divider.setBackgroundColor(BORDER);
                panel.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
            }
        }
    }

    private void chooseTravelStyle() {
        String[] styles = {"Nghỉ dưỡng", "Ẩm thực", "Thiên nhiên", "Văn hóa"};
        new AlertDialog.Builder(this).setTitle("Phong cách du lịch").setItems(styles, (dialog, selected) -> {
            storage.edit().putString("travelStyle", styles[selected]).apply();
            showTab(HOME);
        }).setNegativeButton("Đóng", null).show();
    }

    private void homeTripToday(Trip item, int today) {
        LinearLayout weather = card(content, TINT);
        line(weather, "THỜI TIẾT", 11, BLUE, true).setLetterSpacing(0.09f);
        line(weather, "Dự báo tại " + item.destination(), 17, NAVY, true);
        line(weather, "Kiểm tra trước khi ra ngoài theo lịch hôm nay.", 13, MUTED, false);
        weather.addView(action("Xem dự báo hôm nay", WHITE, NAVY,
                () -> openLink("https://www.google.com/search?q=" + Uri.encode("thời tiết hôm nay " + item.destination()))));
        gap(content, 8);
        String key = "lodging:" + item.id() + ":" + dateText(addDays(item.startAt(), today), "yyyy-MM-dd");
        String lodging = storage.getString(key, "");
        line(content, lodging.isEmpty() ? "Đêm nay chưa lưu chỗ ở" : "Chỗ ở đêm nay", 21, NAVY, true);
        line(content, dateText(addDays(item.startAt(), today), "d/M") + " · " + item.people() + " người", 13, MUTED, false);
        gap(content, 8);
        LinearLayout stay = card(content, 0xFFFFF0E4);
        line(stay, lodging.isEmpty() ? "Tìm chỗ ở tại " + item.destination() : lodging, 17, NAVY, true);
        stay.addView(action("Xem chỗ ở trên Google Maps", WHITE, BLUE,
                () -> openMaps(lodging.isEmpty() ? "khách sạn và homestay " + item.destination() : lodging + " " + item.destination())));
        stay.addView(action(lodging.isEmpty() ? "Đã đặt rồi? Thêm vào chuyến" : "Sửa chỗ ở",
                0xFFFFF0E4, BLUE, () -> editLodging(key, lodging)));
    }

    private void editLodging(String key, String lodging) {
        LinearLayout fields = column();
        fields.setPadding(dp(20), 0, dp(20), 0);
        EditText name = field(fields, "Tên chỗ ở", InputType.TYPE_CLASS_TEXT, 120);
        name.setText(lodging);
        new AlertDialog.Builder(this).setTitle("Chỗ ở đêm nay").setView(fields)
                .setNegativeButton("Hủy", null).setPositiveButton("Lưu", (dialog, which) -> {
                    storage.edit().putString(key, name.getText().toString().trim()).apply();
                    showTab(HOME);
                }).show();
    }

    private View homeIcon(int icon, int fill, String description, Runnable click) {
        FrameLayout tile = new FrameLayout(this);
        tile.setBackground(new RippleDrawable(ColorStateList.valueOf(0x26809BD4), rounded(fill, 18), null));
        tile.addView(new TravelIcon(this, icon, NAVY),
                new FrameLayout.LayoutParams(dp(23), dp(23), Gravity.CENTER));
        tile.setContentDescription(description);
        tile.setFocusable(true);
        tile.setOnClickListener(view -> click.run());
        return tile;
    }

    private void homeNearby(Trip item) {
        String city = item == null ? "" : item.destination();
        LinearLayout heading = row();
        heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = line(heading, "Quanh bạn", 22, NAVY, true);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
        heading.addView(action("Bản đồ", PAGE, BLUE, () -> nearbyMap("điểm tham quan", city)),
                new LinearLayout.LayoutParams(-2, dp(48)));
        content.addView(heading);
        line(content, item == null ? "Theo vị trí hiện tại" : city + " · Theo chuyến đi của bạn", 13, MUTED, false);
        gap(content, 10);
        if (item == null) {
            LinearLayout panel = card(content, TINT);
            line(panel, "Tìm nơi ăn chơi gần vị trí của bạn.", 16, NAVY, true);
            panel.addView(action("Xem quanh bạn trên bản đồ", WHITE, NAVY, () -> nearbyMap("điểm tham quan", "")));
            return;
        }
        String[] labels = {"Tất cả", "Ăn uống", "Cà phê", "Tham quan"};
        LinearLayout chips = row();
        LinearLayout results = column();
        for (int index = 0; index < labels.length; index++) {
            String category = labels[index];
            TextView chip = action(category, index == 0 ? NAVY : WHITE, index == 0 ? WHITE : NAVY, () -> {});
            chip.setSelected(index == 0);
            chip.setOnClickListener(view -> {
                for (int child = 0; child < chips.getChildCount(); child++) {
                    TextView button = (TextView) chips.getChildAt(child);
                    boolean selected = button == view;
                    button.setSelected(selected);
                    button.setBackground(rounded(selected ? NAVY : WHITE, 22));
                    button.setTextColor(selected ? WHITE : NAVY);
                }
                homePlaces(results, city, category);
            });
            LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(0, -2, 1);
            size.setMarginEnd(dp(4));
            chips.addView(chip, size);
        }
        content.addView(chips);
        gap(content, 10);
        content.addView(results);
        homePlaces(results, city, labels[0]);
        gap(content, 16);
    }

    private void homePlaces(LinearLayout results, String city, String category) {
        results.removeAllViews();
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout tiles = row();
        for (TravelCatalog.Place spot : TravelCatalog.PLACES) {
            if (!TravelCatalog.fold(city).equals(TravelCatalog.fold(spot.city()))) continue;
            if (!category.equals("Tất cả") && !category.equals(spot.category())) continue;
            LinearLayout tile = column();
            tile.setBackground(rounded(WHITE, 18));
            tile.setClipToOutline(true);
            LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(dp(160), -1);
            size.setMarginEnd(dp(9));
            tiles.addView(tile, size);
            if (spot.name().equals("Bãi Kem") || spot.name().equals("Hòn Mun")) {
                ImageView photo = new ImageView(this);
                boolean kem = spot.name().equals("Bãi Kem");
                photo.setImageResource(kem ? R.drawable.bai_kem : R.drawable.nha_trang);
                photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
                photo.setContentDescription(kem ? "Ảnh Bãi Kem của Vivu Vietnam" : "Ảnh vịnh nhìn từ Hòn Mun của Tony24986");
                tile.addView(photo, new LinearLayout.LayoutParams(-1, dp(110)));
            }
            LinearLayout details = column();
            details.setPadding(dp(11), dp(9), dp(11), 0);
            tile.addView(details);
            line(details, spot.category().toUpperCase(Locale.ROOT), 11, BLUE, true);
            line(details, spot.name(), 16, NAVY, true);
            details.addView(action("Xem trên bản đồ", WHITE, BLUE, () -> openMaps(spot.name() + " " + city)));
        }
        if (tiles.getChildCount() > 0) {
            scroll.addView(tiles);
            results.addView(scroll);
        } else {
            results.addView(action("Tìm " + category.toLowerCase(Locale.ROOT) + " trên Google Maps", TINT, BLUE,
                    () -> nearbyMap(category.equals("Tất cả") ? "điểm tham quan" : category, city)));
        }
    }

    private void nearbyMap(String query, String city) {
        if (!city.isEmpty()) {
            openMaps(query + " " + city);
            return;
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(query))));
        } catch (ActivityNotFoundException error) {
            openMaps(query + " gần tôi");
        }
    }

    private void homeRecentTrips() {
        if (trips.size() <= 1) return;
        LinearLayout title = row();
        title.setGravity(Gravity.CENTER_VERTICAL);
        line(title, "Chuyến đi gần đây", 21, NAVY, true).setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));
        title.addView(action("Tất cả", PAGE, BLUE, () -> navigate(TRIPS)));
        content.addView(title);
        for (int index = trips.size() - 1; index >= Math.max(1, trips.size() - 2); index--) {
            Trip item = trips.get(index);
            LinearLayout panel = card(content, 0xFFFFF0E4);
            LinearLayout entry = row();
            entry.setGravity(Gravity.CENTER_VERTICAL);
            TravelCatalog.Destination city = TravelCatalog.destination(item.destination());
            if (city != null) {
                LinearLayout thumb = column();
                thumb.setBackground(rounded(TINT, 14));
                thumb.setClipToOutline(true);
                addPhoto(thumb, city, 58);
                LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(dp(58), dp(58));
                size.setMarginEnd(dp(12));
                entry.addView(thumb, size);
            }
            LinearLayout text = column();
            entry.addView(text, new LinearLayout.LayoutParams(0, -2, 1));
            line(text, item.title(), 16, NAVY, true);
            line(text, tripDates(item) + " · " + item.people() + " người", 13, MUTED, false);
            panel.addView(entry);
            panel.addView(action("Xem chuyến đi  ›", 0xFFFFF0E4, BLUE, () -> openTrip(item, PLAN)));
        }
    }

    private void tripDetails() {
        content.addView(action("‹  Chuyến đi", PAGE, BLUE, () -> navigate(TRIPS)));
        tripCard(trip);
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
        content.addView(action("⌖  Lộ trình", NAVY, WHITE, this::openRoute));
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

    private void openRoute() {
        try {
            JSONArray itinerary = new JSONArray();
            for (int d = 0; d < trip.days(); d++) {
                JSONArray stops = new JSONArray();
                for (String activity : activities(trip, d)) {
                    String title = activity.substring(5).trim();
                    JSONObject stop = new JSONObject().put("time", activity.substring(0, 5)).put("name", title);
                    TravelCatalog.Place place = TravelCatalog.place(trip.destination(), title);
                    if (place != null) stop.put("lat", place.lat()).put("lon", place.lon());
                    stops.put(stop);
                }
                itinerary.put(stops);
            }
            startActivity(new Intent(this, RouteActivity.class).putExtra("title", trip.title())
                    .putExtra("destination", trip.destination()).putExtra("day", day).putExtra("days", itinerary.toString()));
        } catch (JSONException error) {
            notifyUser("Chưa mở được lộ trình.");
        }
    }

    private void createTrip() {
        createTrip("");
    }

    private void createTrip(String suggestedDestination) {
        wizardStep = 0;
        destination = suggestedDestination;
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
                addPhoto(tile, TravelCatalog.destination(option), 82);
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
        TravelCatalog.Destination city = TravelCatalog.destination(destination);
        addPhoto(content, city, 116);
        int count = 0;
        for (TravelCatalog.Place stop : TravelCatalog.PLACES) {
            if (!TravelCatalog.fold(stop.city()).equals(TravelCatalog.fold(destination))) continue;
            count++;
            String name = stop.name();
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
        if (count == 0) line(card(content, WHITE), "Bạn có thể tự thêm điểm hoặc mở trợ lý để tìm hiểu về " + destination + ".", 15, MUTED, false);
        footer.addView(action("Hỏi trợ lý về lịch trình", NAVY, WHITE, () -> finishWizard(false, true)));
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
        for (int i = trips.size() - 1; i >= 0; i--) tripCard(trips.get(i));
        gap(content, 48);
    }

    private void tripCard(Trip item) {
        LinearLayout surface = column();
        surface.setBackground(rounded(WHITE, 19));
        surface.setClipToOutline(true);
        LinearLayout.LayoutParams placement = new LinearLayout.LayoutParams(-1, -2);
        placement.bottomMargin = dp(12);
        content.addView(surface, placement);
        addPhoto(surface, TravelCatalog.destination(item.destination()), 126);
        LinearLayout details = column();
        details.setPadding(dp(13), dp(10), dp(13), dp(10));
        surface.addView(details);
        line(details, item.title(), 18, NAVY, true);
        String period = tripDates(item);
        line(details, (period.isEmpty() ? "" : period + "  ·  ") + tripMeta(item), 13, MUTED, false);
        if (trip == null) {
            TextView open = action("Xem chuyến đi  ›", WHITE, BLUE, () -> openTrip(item, PLAN));
            open.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
            details.addView(open);
        }
    }

    private void addPhoto(LinearLayout parent, TravelCatalog.Destination city, int height) {
        if (city == null) return;
        ImageView photo = new ImageView(this);
        photo.setImageResource(city.image());
        photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        photo.setContentDescription("Ảnh " + city.name() + " của " + city.author());
        parent.addView(photo, new LinearLayout.LayoutParams(-1, dp(height)));
    }

    private void destinationGallery() {
        destinationGallery(content);
    }

    private void destinationGallery(LinearLayout parent) {
        HorizontalScrollView scroller = new HorizontalScrollView(this);
        scroller.setHorizontalScrollBarEnabled(false);
        LinearLayout row = row();
        for (TravelCatalog.Destination city : TravelCatalog.DESTINATIONS) {
            LinearLayout tile = column();
            tile.setBackground(rounded(WHITE, 16));
            tile.setClipToOutline(true);
            LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(dp(148), -2);
            layout.rightMargin = dp(10);
            row.addView(tile, layout);
            addPhoto(tile, city, 91);
            tile.addView(action(city.name(), WHITE, NAVY, () -> {
                if (trip != null) openAssistant(trip, "Gợi ý những điểm tham quan ở " + city.name() + ".");
                else {
                    exploreCity = city.name();
                    exploreCategory = 0;
                    exploreQuery = "";
                    showTab(EXPLORE);
                }
            }));
        }
        scroller.addView(row);
        parent.addView(scroller);
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
        Trip current = trip;
        if (current == null && tab == HOME && TravelCloud.session(this).has("uid")) current = homeTrip();
        openAssistant(current != null ? current : trips.get(trips.size() - 1), "");
    }

    private void openAssistant(Trip item, String prompt) {
        Intent intent = new Intent(this, AssistantActivity.class);
        StringBuilder data = new StringBuilder();
        if (item != null) {
            intent.putExtra("trip", item.destination());
            String summary = (item.id().isEmpty() ? "Chuyến mẫu " : "Chuyến ")
                    + item.destination() + ", " + tripMeta(item)
                    + (tripDates(item).isEmpty() ? "" : ", " + tripDates(item))
                    + (item.budget() == 0 ? "" : ", ngân sách nhóm " + money(item.budget()));
            data.append(summary);
            ArrayList<String> days = new ArrayList<>();
            for (int index = 0; index < item.days(); index++) {
                List<String> stops = activities(item, index);
                String plan = stops.isEmpty() ? "Chưa có hoạt động." : TextUtils.join("\n", stops);
                days.add(plan);
                data.append("\nNgày ").append(index + 1).append(": ").append(plan);
            }
            intent.putStringArrayListExtra("days", days);
            List<String> todo = new ArrayList<>();
            int checked = storage.getInt("packing:" + item.id(), 0);
            for (int index = 0; index < PACKING.length; index++) {
                if ((checked & (1 << index)) == 0) todo.add(PACKING[index]);
            }
            data.append("\nHành lý còn thiếu: ").append(todo.isEmpty() ? "Đã đủ" : TextUtils.join(", ", todo));
            JSONArray expenses = savedArray(item.id().isEmpty() ? "expenses" : "expenses:" + item.id());
            long recorded = 0;
            for (int index = 0; index < expenses.length(); index++) {
                JSONObject expense = expenses.optJSONObject(index);
                if (expense != null) recorded += expense.optLong("amount");
            }
            if (expenses.length() > 0) data.append("\nChi phí đã ghi: ").append(money(recorded));
        }
        for (TravelCatalog.Place place : TravelCatalog.PLACES) {
            if (storage.getBoolean("bookmark:" + place.city() + ":" + place.name(), false)) {
                data.append("\nĐiểm đã lưu: ").append(place.name()).append(" · ").append(place.city());
            }
        }
        intent.putExtra("appData", data.substring(0, Math.min(8000, data.length())));
        intent.putExtra("prompt", prompt);
        startActivity(intent);
    }

    private void homeShortcuts(Trip item, int today) {
        LinearLayout tiles = row();
        Trip selected = item == null ? trips.get(trips.size() - 1) : item;
        String[] names = item == null ? new String[]{"Tạo Trip", "Ghi chi tiêu", "Tài liệu", "Lịch trình"}
                : new String[]{"Lịch trình", "Ghi chi tiêu", "Hành lý", "Tài liệu"};
        int[] icons = item == null ? new int[]{6, 10, 7, 3} : new int[]{3, 10, 1, 7};
        Runnable schedule = () -> openTrip(selected, PLAN, Math.max(today, 0));
        Runnable money = () -> openTrip(selected, MONEY);
        Runnable docs = () -> documents(selected);
        Runnable[] actions = item == null ? new Runnable[]{this::createTrip, money, docs, () -> navigate(CALENDAR)}
                : new Runnable[]{schedule, money, () -> packing(selected), docs};
        int[] fills = {TINT, 0xFFFFF0E4, 0xFFE7F6EC, WHITE};
        for (int i = 0; i < names.length; i++) {
            LinearLayout tile = column();
            tile.setGravity(Gravity.CENTER);
            Runnable click = actions[i];
            tile.addView(homeIcon(icons[i], fills[i], names[i], click), new LinearLayout.LayoutParams(dp(53), dp(53)));
            TextView name = line(tile, names[i], 12, NAVY, true);
            name.setGravity(Gravity.CENTER);
            name.setPadding(0, dp(6), 0, dp(8));
            name.setOnClickListener(view -> click.run());
            tiles.addView(tile, new LinearLayout.LayoutParams(0, -2, 1));
        }
        content.addView(tiles);
        gap(content, 12);
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
            explore();
            return;
        }
        line(content, "Khám phá quanh " + trip.destination(), 19, NAVY, true);
        if (TravelCatalog.destination(trip.destination()) == null) {
            String place = trip.destination();
            content.addView(action("Tìm nơi tham quan trên Google Maps", TINT, NAVY, () -> openMaps("Điểm tham quan " + place)));
            content.addView(action("Hỏi AI về " + place, NAVY, WHITE, () -> openAssistant(trip, "Ở " + place + " nên đi đâu?")));
            return;
        }
        filters();
        places(true);
    }

    private void explore() {
        line(content, "Khám Phá", 32, NAVY, true);
        gap(content, 13);
        LinearLayout searchBar = row();
        searchBar.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable outline = rounded(PAGE, 21);
        outline.setStroke(dp(1), MUTED);
        searchBar.setBackground(outline);
        EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setInputType(InputType.TYPE_CLASS_TEXT);
        search.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        search.setFilters(new InputFilter[]{new InputFilter.LengthFilter(120)});
        search.setTextSize(15);
        search.setPadding(dp(15), 0, dp(4), 0);
        search.setBackground(null);
        search.setText(exploreQuery);
        searchBar.addView(search, new LinearLayout.LayoutParams(0, dp(50), 1));
        content.addView(searchBar);
        gap(content, 12);
        HorizontalScrollView strip = new HorizontalScrollView(this);
        strip.setHorizontalScrollBarEnabled(false);
        LinearLayout chips = row();
        strip.addView(chips);
        content.addView(strip);
        gap(content, 13);
        LinearLayout areaBar = row();
        areaBar.setGravity(Gravity.CENTER_VERTICAL);
        content.addView(areaBar);
        gap(content, 12);
        LinearLayout section = column();
        content.addView(section);
        LinearLayout extras = column();
        content.addView(extras);
        Runnable refresh = () -> {
            boolean guide = exploreCategory == 1;
            search.setHint(guide ? "Tìm theo điểm đến, vd. Đà Lạt" : "Tìm nơi, món ăn, chỗ ở");
            areaBar.removeAllViews();
            section.removeAllViews();
            extras.removeAllViews();
            for (int i = 0; i < chips.getChildCount(); i++) {
                TextView chip = (TextView) chips.getChildAt(i);
                boolean active = i == exploreCategory;
                chip.setSelected(active);
                GradientDrawable fill = rounded(active ? NAVY : WHITE, 24);
                if (!active) fill.setStroke(dp(1), BORDER);
                chip.setBackground(fill);
                chip.setTextColor(active ? WHITE : NAVY);
            }
            if (guide) {
                exploreGuides(section);
                return;
            }
            Trip upcoming = signedIn() ? homeTrip() : null;
            String area = exploreCity.isEmpty() ? (upcoming != null ? upcoming.destination() : "Hà Nội") : exploreCity;
            TextView choose = action(area + "  ▾", WHITE, NAVY, () -> pickExploreArea(search));
            choose.setContentDescription("Đổi khu vực, hiện tại " + area);
            areaBar.addView(choose, new LinearLayout.LayoutParams(0, -2, 1));
            areaBar.addView(action("63 tỉnh/thành  ›", TINT, BLUE, () -> startActivity(new Intent(this, ProvinceActivity.class))));
            LinearLayout results = column();
            section.addView(results);
            new PlaceFeed(this, results, area, EXPLORE_FILTERS[exploreCategory], exploreQuery);
            if (exploreCategory == 0 && exploreQuery.isEmpty()) {
                gap(extras, 18);
                line(extras, "Điểm đến nổi bật", 21, NAVY, true);
                gap(extras, 10);
                destinationGallery(extras);
                if (TravelCatalog.destination(exploreCity) != null) places(extras, true);
            }
        };
        Runnable submit = () -> {
            exploreQuery = search.getText().toString().trim();
            InputMethodManager keyboard = getSystemService(InputMethodManager.class);
            if (keyboard != null) keyboard.hideSoftInputFromWindow(search.getWindowToken(), 0);
            search.clearFocus();
            refresh.run();
        };
        search.setOnEditorActionListener((view, action, event) -> {
            if (action != EditorInfo.IME_ACTION_SEARCH) return false;
            submit.run();
            return true;
        });
        TextView searchButton = action("Tìm", TINT, NAVY, submit);
        searchButton.setContentDescription("Tìm kiếm");
        searchBar.addView(searchButton, new LinearLayout.LayoutParams(dp(58), dp(46)));
        for (int i = 0; i < EXPLORE_FILTERS.length; i++) {
            int index = i;
            TextView chip = action(EXPLORE_FILTERS[i], WHITE, NAVY, () -> {
                exploreCategory = index;
                submit.run();
            });
            chip.setMinHeight(dp(42));
            LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(-2, -2);
            size.setMarginEnd(dp(7));
            chips.addView(chip, size);
        }
        strip.post(() -> strip.scrollTo(chips.getChildAt(exploreCategory).getLeft() - dp(12), 0));
        refresh.run();
    }

    private void pickExploreArea(EditText search) {
        LinearLayout panel = column();
        panel.setPadding(dp(18), 0, dp(18), 0);
        EditText find = field(panel, "Tìm điểm đến hoặc tỉnh/thành", InputType.TYPE_CLASS_TEXT, 45);
        LinearLayout list = column();
        ScrollView scroll = new ScrollView(this);
        scroll.addView(list);
        panel.addView(scroll, new LinearLayout.LayoutParams(-1, dp(300)));
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Chọn khu vực").setView(panel)
                .setNegativeButton("Hủy", null).create();
        LinkedHashSet<String> choices = new LinkedHashSet<>(java.util.Arrays.asList(DESTINATIONS));
        choices.addAll(java.util.Arrays.asList(ProvinceActivity.PROVINCES));
        Runnable update = () -> {
            list.removeAllViews();
            String keyword = TravelCatalog.fold(find.getText().toString());
            for (String city : choices) if (TravelCatalog.fold(city).contains(keyword)) {
                list.addView(action(city, WHITE, NAVY, () -> {
                    exploreCity = city.equals("Thừa Thiên Huế") ? "Huế" : city;
                    exploreQuery = search.getText().toString().trim();
                    dialog.dismiss();
                    showTab(EXPLORE);
                }));
            }
            if (list.getChildCount() == 0) line(list, "Không tìm thấy khu vực phù hợp.", 14, MUTED, false);
        };
        find.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int c, int f) {}
            @Override public void onTextChanged(CharSequence s, int a, int before, int count) { update.run(); }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });
        update.run();
        dialog.show();
    }

    private void exploreGuides(LinearLayout panel) {
        line(panel, "Cẩm nang", 24, NAVY, true);
        line(panel, "Gợi ý lịch trình và kinh nghiệm từ Travel VN", 14, MUTED, false);
        gap(panel, 13);
        String filter = TravelCatalog.fold(exploreQuery);
        int count = 0;
        for (TravelCatalog.Destination city : TravelCatalog.DESTINATIONS) {
            if (!TravelCatalog.fold(city.name()).contains(filter)) continue;
            count++;
            LinearLayout item = row();
            item.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout thumbnail = column();
            thumbnail.setBackground(rounded(TINT, 16));
            thumbnail.setClipToOutline(true);
            addPhoto(thumbnail, city, 78);
            item.addView(thumbnail, new LinearLayout.LayoutParams(dp(84), dp(78)));
            LinearLayout description = column();
            description.setPadding(dp(12), 0, 0, 0);
            line(description, "Cẩm nang " + city.name(), 17, NAVY, true);
            line(description, city.description(), 13, MUTED, false);
            item.addView(description, new LinearLayout.LayoutParams(0, -2, 1));
            item.setFocusable(true);
            item.setOnClickListener(view -> {
                if (city.name().equals("Ninh Bình")) { openTrip(SAMPLE, GUIDE); return; }
                new AlertDialog.Builder(this).setTitle("Cẩm nang " + city.name())
                        .setMessage(city.description() + "\n\nĂn uống\n" + city.food() + "\n\nChỗ ở\n" + city.stay())
                        .setNegativeButton("Đóng", null)
                        .setPositiveButton("Lên kế hoạch", (dialog, which) -> createTrip(city.name())).show();
            });
            panel.addView(item);
            gap(panel, 14);
        }
        if (count == 0) line(panel, "Chưa có cẩm nang cho từ khóa này.", 14, MUTED, false);
    }

    private void places(boolean showAll) {
        places(content, showAll);
    }

    private void places(LinearLayout parent, boolean showAll) {
        int shown = 0;
        String city = trip != null ? trip.destination() : tab == HOME ? "Ninh Bình" : exploreCity;
        for (TravelCatalog.Place spot : TravelCatalog.PLACES) {
            if (!city.isEmpty() && !TravelCatalog.fold(city).equals(TravelCatalog.fold(spot.city()))) continue;
            if (!discoverFilter.equals("Tất cả") && !discoverFilter.equals(spot.category())) continue;
            LinearLayout entry = card(parent, WHITE);
            line(entry, spot.category() + " · " + spot.city(), 11, GREEN, true);
            line(entry, spot.name(), 16, NAVY, true);
            if (showAll) line(entry, spot.tip(), 14, MUTED, false);
            if (showAll) entry.addView(action("Xem trên Google Maps", WHITE, BLUE, () -> openMaps(spot.name() + " " + spot.city())));
            if (trip != null) entry.addView(action("+  Thêm vào ngày " + (day + 1), TINT, BLUE, () -> {
                List<String> list = activities(trip, day);
                if (list.stream().anyMatch(activity -> activity.substring(5).trim().equals(spot.name()))) {
                    notifyUser("Điểm này đã có trong lịch ngày " + (day + 1) + ".");
                    return;
                }
                list.add("09:00   " + spot.name());
                if (saveActivities(list)) openTripDay();
            }));
            else if (showAll) {
                String key = "bookmark:" + spot.city() + ":" + spot.name();
                TextView bookmark = action(storage.getBoolean(key, false) ? "★ Đã lưu" : "☆ Lưu địa điểm", TINT, BLUE, () -> {});
                bookmark.setOnClickListener(view -> {
                    boolean saved = !storage.getBoolean(key, false);
                    storage.edit().putBoolean(key, saved).apply();
                    bookmark.setText(saved ? "★ Đã lưu" : "☆ Lưu địa điểm");
                });
                entry.addView(bookmark);
            }
            if (++shown >= 2 && !showAll) break;
        }
        if (shown == 0) line(card(parent, WHITE), "Chưa có địa điểm thuộc mục này.", 14, MUTED, false);
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
        visitedCard();
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

    private void visitedCard() {
        LinkedHashSet<String> locations = visitedProvinces();
        LinearLayout visited = card(content, 0xFFE7F6EC);
        line(visited, "Nơi bạn đã đi", 17, NAVY, true);
        gap(visited, 5);
        line(visited, locations.size() + "/" + ProvinceActivity.PROVINCES.length + " tỉnh thành", 27, NAVY, true);
        line(visited, locations.isEmpty() ? "Đánh dấu những nơi bạn từng đến, kể cả trước khi dùng Travel VN."
                : TextUtils.join(" · ", locations), 14, MUTED, false);
        gap(visited, 10);
        visited.addView(action("Đánh dấu nơi đã đi", WHITE, NAVY, this::markVisited));
    }

    private LinkedHashSet<String> visitedProvinces() {
        LinkedHashSet<String> provinces = new LinkedHashSet<>();
        for (String place : storage.getStringSet("visited", java.util.Collections.emptySet())) {
            provinces.add(switch (place) {
                case "Phú Quốc" -> "Kiên Giang";
                case "Huế" -> "Thừa Thiên Huế";
                case "Sa Pa" -> "Lào Cai";
                case "Đà Lạt" -> "Lâm Đồng";
                case "Hội An" -> "Quảng Nam";
                default -> place;
            });
        }
        return provinces;
    }

    private void markVisited() {
        String[] names = ProvinceActivity.PROVINCES;
        LinkedHashSet<String> selected = visitedProvinces();
        boolean[] checked = new boolean[names.length];
        for (int i = 0; i < names.length; i++) checked[i] = selected.contains(names[i]);
        new AlertDialog.Builder(this).setTitle("Nơi bạn đã đi")
                .setMultiChoiceItems(names, checked, (dialog, index, value) -> {
                    if (value) selected.add(names[index]); else selected.remove(names[index]);
                }).setNegativeButton("Hủy", null).setPositiveButton("Lưu", (dialog, which) -> {
                    storage.edit().putStringSet("visited", selected).apply();
                    showTab(tab);
                }).show();
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
        } else if (TravelCatalog.destination(item.destination()) != null) {
            List<TravelCatalog.Place> suggestions = new ArrayList<>();
            for (TravelCatalog.Place stop : TravelCatalog.PLACES) {
                if (TravelCatalog.fold(stop.city()).equals(TravelCatalog.fold(item.destination()))) suggestions.add(stop);
            }
            for (int i = index; i < suggestions.size(); i += item.days()) {
                result.add(String.format(Locale.ROOT, "%02d:00   %s", 9 + 2 * (i / item.days()), suggestions.get(i).name()));
            }
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
        if (request == ACCOUNT_REQUEST) {
            showTab(tab);
            return;
        }
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
        Trip selected = trip;
        new AlertDialog.Builder(this).setTitle("Thành viên · " + trip.people())
                .setItems(people(), null).setNegativeButton("Đóng", null)
                .setPositiveButton("Chia sẻ kế hoạch", (dialog, which) -> shareTrip(selected)).show();
    }

    private void shareTrip(Trip item) {
        StringBuilder plan = new StringBuilder(item.title() + "\n" + tripMeta(item));
        for (int i = 0; i < item.days(); i++) plan.append("\n\nNgày ").append(i + 1).append("\n")
                .append(TextUtils.join("\n", activities(item, i)));
        startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND).setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, plan.toString()), "Chia sẻ kế hoạch"));
        storage.edit().putBoolean("openedSharePlan", true).apply();
        if (tab == HOME) showTab(HOME);
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

    private String accountName() {
        JSONObject session = TravelCloud.session(this);
        String name = session.optString("name").trim();
        if (name.isEmpty()) name = session.optString("email").trim();
        if (name.isEmpty()) name = storage.getString("displayName", "Khách").trim();
        return name.isEmpty() ? "Khách" : name;
    }

    private void openAccount() {
        openAccount("welcome");
    }

    private void openAccount(String mode) {
        startActivityForResult(new Intent(this, AccountActivity.class).putExtra("mode", mode), ACCOUNT_REQUEST);
    }

    private boolean signedIn() {
        return !TravelCloud.session(this).optString("uid").isEmpty();
    }

    private void profile() {
        content.setBackgroundColor(WHITE);
        gap(content, 14);
        boolean loggedIn = signedIn();
        String name = loggedIn ? accountName() : "Khách";
        String[] words = name.split("\\s+");
        LinearLayout header = row();
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView avatar = action(loggedIn ? initials(words[0]) + (words.length > 1 ? initials(words[words.length - 1]) : "")
                : "KH", TINT, NAVY, this::openAccount);
        avatar.setTextSize(23);
        avatar.setBackground(rounded(TINT, 36));
        avatar.setContentDescription(loggedIn ? "Mở hồ sơ" : "Đăng nhập");
        header.addView(avatar, new LinearLayout.LayoutParams(dp(65), dp(65)));
        LinearLayout identity = column();
        identity.setPadding(dp(14), 0, 0, 0);
        line(identity, name, 23, NAVY, true);
        line(identity, loggedIn ? TravelCloud.session(this).optString("email") : "Chưa đăng nhập", 14, MUTED, false);
        header.addView(identity, new LinearLayout.LayoutParams(0, -2, 1));
        content.addView(header);
        menuHeading("CHUYẾN ĐI");
        menuRow(1, "Chuyến đi của bạn", "", () -> navigate(TRIPS));
        menuRow(7, "Ví giấy tờ", "", () -> documents(trips.get(trips.size() - 1)));
        if (loggedIn) {
            menuRow(11, "Thống kê", "", () -> menuInfo("Thống kê", (trips.size() - 1) + " chuyến đi đã tạo\n"
                    + visitedProvinces().size() + "/63 tỉnh/thành đã đánh dấu."));
            menuRow(12, "Công nợ", "", () -> openTrip(trips.get(trips.size() - 1), MONEY));
            menuRow(13, "Đánh giá của tôi", "", this::myReviews);
        }
        menuRow(3, "Lịch chuyến đi", "", () -> navigate(CALENDAR));
        menuRow(2, "Địa điểm đã lưu", "", this::bookmarks);
        menuHeading("CÀI ĐẶT");
        if (loggedIn) menuRow(4, "Hồ sơ và tài khoản", "", this::openAccount);
        menuRow(5, "Thông báo", "", () -> {
            Intent intent = Build.VERSION.SDK_INT >= 26
                    ? new Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, getPackageName())
                    : new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
            try { startActivity(intent); } catch (ActivityNotFoundException error) {
                menuInfo("Thông báo", "Mở Cài đặt Android → Ứng dụng → Travel VN để quản lý thông báo.");
            }
        });
        menuRow(14, "Ngôn ngữ", "Tiếng Việt", () -> menuInfo("Ngôn ngữ", "Travel VN hiện hỗ trợ tiếng Việt."));
        menuRow(12, "Tiền tệ", "VND", () -> menuInfo("Tiền tệ", "Ngân sách chuyến đi dùng VND. Mức giá địa điểm hiển thị theo dữ liệu Google."));
        menuRow(15, "Giao diện", "Sáng", () -> menuInfo("Giao diện", "Travel VN hiện dùng giao diện sáng."));
        menuHeading("HỖ TRỢ");
        menuRow(16, "Trợ giúp và góp ý", "", () -> new AlertDialog.Builder(this).setTitle("Trợ giúp và góp ý")
                .setMessage("Vào Khám phá để tìm địa điểm, hoặc Chuyến đi của bạn để quản lý lịch và chi tiêu.")
                .setNegativeButton("Đóng", null)
                .setPositiveButton("Mở GitHub", (dialog, which) -> openLink("https://github.com/KHNeJu/App-Travel-Vn")).show());
        menuRow(17, "Quyền riêng tư", "", () -> new AlertDialog.Builder(this).setTitle("Quyền riêng tư")
                .setMessage("Chuyến đi và ảnh bìa lưu trên thiết bị. Firebase xử lý tài khoản email. Google Maps nhận từ khóa khi bạn tìm địa điểm. Hỏi AI gửi ngữ cảnh chuyến đi để trả lời khi dịch vụ được bật.")
                .setNegativeButton("Đóng", null)
                .setPositiveButton("Chính sách Google", (d, w) -> openLink("https://policies.google.com/privacy?hl=vi")).show());
        menuRow(16, "Về ứng dụng", "", () -> menuInfo("Travel VN", "Phiên bản " + BuildConfig.VERSION_NAME
                + "\nỨng dụng lập kế hoạch và khám phá du lịch Việt Nam."));
        menuRow(8, "Nguồn ảnh điểm đến", "", this::photoCredit);
        if (BuildConfig.DEBUG && getString(R.string.travel_backend_url).isEmpty()) {
            menuRow(2, "Kết nối máy chủ thử nghiệm", "",
                    () -> startActivity(new Intent(this, AssistantActivity.class).putExtra("settings", true)));
        }
        gap(content, 12);
        if (loggedIn) {
            TextView signOut = action("Đăng xuất", WHITE, NAVY, () -> new AlertDialog.Builder(this)
                    .setTitle("Đăng xuất Travel VN?").setMessage("Chuyến đi đã lưu vẫn còn trên thiết bị này.")
                    .setNegativeButton("Hủy", null).setPositiveButton("Đăng xuất", (d, w) -> {
                        TravelCloud.signOut(this);
                        showTab(MENU);
                    }).show());
            GradientDrawable outline = rounded(WHITE, 20);
            outline.setStroke(dp(1), BORDER);
            signOut.setBackground(outline);
            content.addView(signOut);
            gap(content, 12);
        }
        line(content, "Travel VN " + BuildConfig.VERSION_NAME, 12, MUTED, false).setGravity(Gravity.CENTER);
    }

    private void menuHeading(String title) {
        gap(content, 21);
        line(content, title, 12, MUTED, true).setLetterSpacing(.08f);
        gap(content, 5);
    }

    private void menuRow(int icon, String title, String value, Runnable click) {
        LinearLayout item = row();
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setMinimumHeight(dp(53));
        item.setBackground(new RippleDrawable(ColorStateList.valueOf(TINT), rounded(WHITE, 10), null));
        FrameLayout badge = new FrameLayout(this);
        badge.setBackground(rounded(PAGE, 12));
        badge.addView(new TravelIcon(this, icon, NAVY), new FrameLayout.LayoutParams(dp(21), dp(21), Gravity.CENTER));
        item.addView(badge, new LinearLayout.LayoutParams(dp(36), dp(36)));
        TextView label = new TextView(this);
        label.setText(title);
        label.setTextSize(16);
        label.setTextColor(NAVY);
        label.setPadding(dp(11), dp(8), dp(4), dp(8));
        item.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
        if (!value.isEmpty()) label(item, value, 13, MUTED, false);
        TextView arrow = label(item, "›", 23, MUTED, false);
        arrow.setPadding(dp(9), 0, dp(5), 0);
        item.setContentDescription(value.isEmpty() ? title : title + ", " + value);
        item.setFocusable(true);
        item.setOnClickListener(v -> click.run());
        content.addView(item);
        View divider = new View(this);
        divider.setBackgroundColor(BORDER);
        content.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
    }

    private void menuGuestFooter() {
        LinearLayout footer = column();
        footer.setBackgroundColor(WHITE);
        footer.setElevation(dp(4));
        footer.setPadding(dp(16), dp(10), dp(16), dp(3));
        footer.addView(action("Tạo tài khoản miễn phí", 0xFF1267D8, WHITE, () -> openAccount("signup")));
        footer.addView(action("Đã có tài khoản?  Đăng nhập", WHITE, BLUE, this::openAccount));
        root.addView(footer);
    }

    private void menuInfo(String title, String message) {
        new AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton("Đóng", null).show();
    }

    private void myReviews() {
        String key = "reviews:" + TravelCloud.session(this).optString("uid");
        JSONArray saved = savedArray(key);
        String[] values = new String[saved.length()];
        for (int i = 0; i < values.length; i++) values[i] = saved.optString(i);
        AlertDialog.Builder list = new AlertDialog.Builder(this).setTitle("Đánh giá của tôi")
                .setNegativeButton("Đóng", null).setPositiveButton("Viết đánh giá", (d, w) -> {
                    LinearLayout editor = column();
                    editor.setPadding(dp(18), 0, dp(18), 0);
                    EditText name = field(editor, "Tên địa điểm", InputType.TYPE_CLASS_TEXT, 100);
                    EditText note = field(editor, "Cảm nhận của bạn", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE, 500);
                    AlertDialog write = new AlertDialog.Builder(this).setTitle("Đánh giá riêng tư")
                            .setView(editor).setNegativeButton("Hủy", null).setPositiveButton("Lưu", null).create();
                    write.setOnShowListener(event -> write.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                        String place = name.getText().toString().trim();
                        String review = note.getText().toString().trim();
                        if (place.isEmpty()) { name.setError("Nhập tên địa điểm"); return; }
                        if (review.isEmpty()) { note.setError("Nhập cảm nhận của bạn"); return; }
                        if (writeArray(key, saved.put(place + "\n" + review))) { write.dismiss(); myReviews(); }
                    }));
                    write.show();
                });
        if (values.length == 0) list.setMessage("Đánh giá bạn viết ở đây chỉ lưu trên thiết bị này.");
        else list.setItems(values, (d, which) -> menuInfo("Đánh giá của tôi", values[which]));
        list.show();
    }

    private void bookmarks() {
        List<TravelCatalog.Place> saved = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (TravelCatalog.Place place : TravelCatalog.PLACES) {
            if (!storage.getBoolean("bookmark:" + place.city() + ":" + place.name(), false)) continue;
            saved.add(place);
            labels.add(place.name() + " · " + place.city());
        }
        AlertDialog.Builder dialog = new AlertDialog.Builder(this).setTitle("Địa điểm đã lưu").setNegativeButton("Đóng", null);
        if (saved.isEmpty()) dialog.setMessage("Bấm Lưu địa điểm trong tab Khám phá để xem lại tại đây.");
        else dialog.setItems(labels.toArray(new String[0]), (popup, index) -> openMaps(labels.get(index)));
        dialog.show();
    }

    private void photoCredit() {
        String[] credits = java.util.Arrays.stream(TravelCatalog.DESTINATIONS)
                .map(city -> city.name() + " · " + city.author() + " · " + city.license()).toArray(String[]::new);
        new AlertDialog.Builder(this).setTitle("Ảnh Wikimedia Commons")
                .setItems(credits, (dialog, index) -> {
                    TravelCatalog.Destination city = TravelCatalog.DESTINATIONS[index];
                    openLink("https://commons.wikimedia.org/wiki/File:" + Uri.encode(city.file()));
                }).setNegativeButton("Đóng", null).show();
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
        button.setBackground(new RippleDrawable(ColorStateList.valueOf(0x26809BD4), rounded(fill, 22), null));
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
        GradientDrawable surface = rounded(WHITE, 18);
        surface.setStroke(dp(1), BORDER);
        input.setBackground(surface);
        input.setMinHeight(dp(48));
        input.setPadding(dp(14), dp(8), dp(14), dp(8));
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(limit)});
        parent.addView(input, new LinearLayout.LayoutParams(-1, -2));
        gap(parent, 10);
        return input;
    }

    private LinearLayout card(LinearLayout parent, int color) {
        LinearLayout card = column();
        card.setPadding(dp(17), dp(15), dp(17), dp(15));
        GradientDrawable background = rounded(color, 24);
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

    private LinearLayout row() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        return layout;
    }

}
