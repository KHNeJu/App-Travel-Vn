package vn.travelvn.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class RouteActivity extends Activity {
    private static final int INK = 0xFF102746;
    private static final int BLUE = 0xFF265FC4;
    private static final int PALE = 0xFFEAF1FF;
    private static final int PAGE = 0xFFF5F7FC;
    private static final int WHITE = Color.WHITE;
    private static final String LOCAL = "travelvn.local";
    private static final String MAP_PAGE = "https://" + LOCAL + "/route/index.html";
    private JSONArray days;
    private String destination;
    private int day;
    private boolean loaded;
    private boolean compact;
    private WebView map;
    private LinearLayout tabs;
    private LinearLayout stops;
    private ScrollView stopList;
    private TextView detailedMode;
    private TextView compactMode;
    private TextView summary;
    private TextView directions;

    @SuppressLint("SetJavaScriptEnabled")
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        destination = getIntent().getStringExtra("destination");
        String title = getIntent().getStringExtra("title");
        try {
            days = new JSONArray(getIntent().getStringExtra("days"));
            if (days.length() == 0) { finish(); return; }
        } catch (JSONException | NullPointerException error) {
            finish();
            return;
        }
        day = Math.max(0, Math.min(days.length() - 1, state == null ? getIntent().getIntExtra("day", 0) : state.getInt("day")));
        compact = state != null && state.getBoolean("compact");
        LinearLayout root = column();
        root.setBackgroundColor(PAGE);
        setContentView(root);
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                int flags = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                controller.setSystemBarsAppearance(flags, flags);
            }
            root.setOnApplyWindowInsetsListener((view, insets) -> {
                Insets safe = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                view.setPadding(safe.left, safe.top, safe.right, safe.bottom);
                return insets;
            });
        }
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = button("‹", WHITE, INK, this::finish);
        back.setContentDescription("Quay lại");
        back.setTextSize(26);
        header.addView(back, new LinearLayout.LayoutParams(dp(47), dp(48)));
        LinearLayout heading = column();
        heading.addView(text("Lộ trình chuyến đi", 19, INK, true));
        heading.addView(text(title == null ? destination : title, 12, 0xFF52647E, false));
        header.addView(heading, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(header);
        HorizontalScrollView strip = new HorizontalScrollView(this);
        strip.setHorizontalScrollBarEnabled(false);
        tabs = new LinearLayout(this);
        tabs.setPadding(dp(9), dp(5), dp(9), dp(7));
        strip.addView(tabs);
        root.addView(strip);
        LinearLayout modes = new LinearLayout(this);
        modes.setGravity(Gravity.END);
        modes.setPadding(dp(8), 0, dp(12), dp(6));
        detailedMode = button("Chi tiết", PALE, INK, () -> selectMode(false));
        compactMode = button("Đơn giản", WHITE, INK, () -> selectMode(true));
        modes.addView(detailedMode);
        modes.addView(compactMode);
        root.addView(modes);

        map = new WebView(this);
        map.getSettings().setJavaScriptEnabled(true);
        map.getSettings().setAllowFileAccess(false);
        map.getSettings().setAllowContentAccess(false);
        map.getSettings().setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        map.getSettings().setUserAgentString("TravelVN/1.3 Android (" + getPackageName() + ")");
        map.setContentDescription("Bản đồ lộ trình");
        map.setWebViewClient(new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                Uri url = request.getUrl();
                String host = url.getHost();
                if (!"https".equals(url.getScheme())) return blocked();
                if (LOCAL.equals(host)) {
                    String path = url.getPath();
                    String type = switch (path == null ? "" : path) {
                        case "/route/index.html" -> "text/html";
                        case "/route/leaflet.js" -> "text/javascript";
                        case "/route/leaflet.css" -> "text/css";
                        default -> null;
                    };
                    if (type == null) return blocked();
                    try { return new WebResourceResponse(type, "UTF-8", getAssets().open(path.substring(1))); }
                    catch (IOException error) { return blocked(); }
                }
                if ("tile.openstreetmap.org".equals(host) || "routing.openstreetmap.de".equals(host)) return null;
                return blocked();
            }

            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if (request.hasGesture() && "https".equals(uri.getScheme()) && "www.openstreetmap.org".equals(uri.getHost())) open(uri);
                return true;
            }

            @Override public void onPageFinished(WebView view, String url) {
                if (MAP_PAGE.equals(url)) {
                    loaded = true;
                    renderMap();
                }
            }
        });
        root.addView(map, new LinearLayout.LayoutParams(-1, 0, 5));
        summary = text("", 15, INK, true);
        summary.setPadding(dp(15), dp(11), dp(15), dp(4));
        root.addView(summary);
        stopList = new ScrollView(this);
        stops = column();
        stops.setPadding(dp(12), dp(3), dp(12), dp(3));
        stopList.addView(stops);
        root.addView(stopList, new LinearLayout.LayoutParams(-1, 0, 3));
        directions = button("Mở chỉ đường trên Google Maps", INK, WHITE, this::openDirections);
        LinearLayout.LayoutParams buttonPlace = new LinearLayout.LayoutParams(-1, -2);
        buttonPlace.setMargins(dp(13), dp(4), dp(13), dp(8));
        root.addView(directions, buttonPlace);
        select(day);
        selectMode(compact);
        strip.post(() -> strip.scrollTo(Math.max(0, day - 1) * dp(79), 0));
        map.loadUrl(MAP_PAGE);
    }

    private void selectMode(boolean simple) {
        compact = simple;
        stopList.setVisibility(simple ? android.view.View.GONE : android.view.View.VISIBLE);
        detailedMode.setSelected(!simple);
        compactMode.setSelected(simple);
        detailedMode.setBackgroundTintList(android.content.res.ColorStateList.valueOf(simple ? WHITE : PALE));
        compactMode.setBackgroundTintList(android.content.res.ColorStateList.valueOf(simple ? PALE : WHITE));
    }

    private WebResourceResponse blocked() {
        return new WebResourceResponse("text/plain", "UTF-8", new ByteArrayInputStream(new byte[0]));
    }

    private JSONArray current() {
        JSONArray selected = days.optJSONArray(day);
        return selected == null ? new JSONArray() : selected;
    }

    private void select(int index) {
        day = index;
        tabs.removeAllViews();
        for (int i = 0; i < days.length(); i++) {
            int option = i;
            TextView tab = button("Ngày " + (i + 1), i == day ? BLUE : WHITE, i == day ? WHITE : INK, () -> select(option));
            tab.setSelected(i == day);
            LinearLayout.LayoutParams place = new LinearLayout.LayoutParams(dp(74), -2);
            place.rightMargin = dp(5);
            tabs.addView(tab, place);
        }
        stops.removeAllViews();
        JSONArray items = current();
        int pins = 0;
        for (int i = 0; i < items.length(); i++) {
            JSONObject stop = items.optJSONObject(i);
            if (stop == null) continue;
            boolean mapped = stop.has("lat");
            if (mapped) pins++;
            TextView row = button((i + 1) + "   " + stop.optString("time") + "    " + stop.optString("name")
                    + (mapped ? "" : "  ·  Tìm vị trí"), WHITE, INK,
                    () -> open(Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(query(stop)))));
            row.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams place = new LinearLayout.LayoutParams(-1, -2);
            place.bottomMargin = dp(6);
            stops.addView(row, place);
        }
        if (items.length() == 0) stops.addView(text("Chưa có hoạt động trong ngày này. Thêm điểm từ tab Kế hoạch.", 14, INK, false));
        summary.setText(items.length() + " điểm dừng  ·  " + pins + " ghim bản đồ");
        directions.setEnabled(items.length() > 0);
        directions.setAlpha(items.length() > 0 ? 1f : .5f);
        renderMap();
    }

    private void renderMap() {
        if (!loaded) return;
        try {
            JSONArray points = new JSONArray();
            JSONArray items = current();
            for (int i = 0; i < items.length(); i++) {
                JSONObject stop = items.optJSONObject(i);
                if (stop != null && stop.has("lat")) points.put(new JSONObject().put("name", stop.optString("name"))
                        .put("number", i + 1).put("lat", stop.getDouble("lat")).put("lon", stop.getDouble("lon")));
            }
            JSONObject data = new JSONObject().put("points", points);
            TravelCatalog.Destination center = TravelCatalog.destination(destination);
            if (center != null) data.put("center", new JSONArray().put(center.lat()).put(center.lon()));
            map.evaluateJavascript("if (window.setRoute) window.setRoute(" + data + ");", null);
        } catch (JSONException error) {
            summary.setText("Chưa đọc được dữ liệu bản đồ.");
        }
    }

    private String query(JSONObject stop) {
        if (stop.has("lat")) return stop.optDouble("lat") + "," + stop.optDouble("lon");
        return stop.optString("name") + " " + destination;
    }

    private void openDirections() {
        JSONArray items = current();
        if (items.length() == 0) return;
        if (items.length() == 1) {
            open(Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(query(items.optJSONObject(0)))));
            return;
        }
        List<String> via = new ArrayList<>();
        for (int i = 1; i < items.length() - 1; i++) via.add(query(items.optJSONObject(i)));
        Uri.Builder uri = Uri.parse("https://www.google.com/maps/dir/").buildUpon()
                .appendQueryParameter("api", "1")
                .appendQueryParameter("origin", query(items.optJSONObject(0)))
                .appendQueryParameter("destination", query(items.optJSONObject(items.length() - 1)))
                .appendQueryParameter("travelmode", "driving");
        if (!via.isEmpty()) uri.appendQueryParameter("waypoints", android.text.TextUtils.join("|", via.subList(0, Math.min(9, via.size()))));
        open(uri.build());
    }

    private void open(Uri uri) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); }
        catch (ActivityNotFoundException error) { summary.setText("Thiết bị chưa có ứng dụng để mở bản đồ ngoài."); }
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putInt("day", day);
        state.putBoolean("compact", compact);
    }

    @Override protected void onDestroy() {
        if (map != null) {
            if (map.getParent() instanceof ViewGroup parent) parent.removeView(map);
            map.destroy();
        }
        super.onDestroy();
    }

    private LinearLayout column() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private TextView button(String value, int fill, int ink, Runnable click) {
        TextView button = text(value, 13, ink, true);
        button.setMinHeight(dp(45));
        button.setPadding(dp(10), dp(6), dp(10), dp(6));
        button.setGravity(Gravity.CENTER);
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(fill);
        shape.setCornerRadius(dp(15));
        button.setBackground(shape);
        button.setFocusable(true);
        button.setOnClickListener(view -> click.run());
        return button;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
