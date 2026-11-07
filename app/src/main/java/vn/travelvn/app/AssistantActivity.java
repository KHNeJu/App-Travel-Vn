package vn.travelvn.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AssistantActivity extends Activity {
    private static final int INK = 0xFF112747;
    private static final int PURPLE = 0xFF6D4DF6;
    private static final int PALE = 0xFFF0EDFF;
    private static final int MUTED = 0xFF58677E;
    private static final int WHITE = Color.WHITE;
    private static final Pattern PLAN_DAY = Pattern.compile("\\bngay ([1-9][0-9]?)\\b");
    private final List<Turn> history = new ArrayList<>();
    private final List<WebView> widgets = new ArrayList<>();
    private LinearLayout messages;
    private LinearLayout welcome;
    private ScrollView scroll;
    private EditText input;
    private TextView send;
    private TextView status;
    private String context;
    private String appData;
    private List<String> days;
    private boolean waiting;
    private TravelCatalog.Destination lastDestination;

    private record Turn(String role, String text) {}

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        context = getIntent().getStringExtra("trip");
        if (context == null) context = "";
        appData = getIntent().getStringExtra("appData");
        if (appData == null) appData = "";
        days = getIntent().getStringArrayListExtra("days");
        LinearLayout root = column();
        root.setBackgroundColor(WHITE);
        root.setFocusableInTouchMode(true);
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
                Insets safe = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout()
                        | WindowInsets.Type.ime());
                view.setPadding(safe.left, safe.top, safe.right, safe.bottom);
                return insets;
            });
        }
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.END);
        top.setPadding(dp(12), 0, dp(12), 0);
        TextView close = button("×", WHITE, INK, this::finish);
        close.setTextSize(24);
        close.setContentDescription("Đóng trợ lý");
        top.addView(close, new LinearLayout.LayoutParams(dp(48), dp(48)));
        root.addView(top);

        scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        messages = column();
        messages.setPadding(dp(16), dp(8), dp(16), dp(12));
        scroll.addView(messages);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        welcome = column();
        messages.addView(welcome);
        welcome();

        LinearLayout composer = column();
        composer.setPadding(dp(12), dp(6), dp(12), dp(10));
        composer.setBackgroundColor(PALE);
        status = label(TravelCloud.available(this) ? (context.isEmpty() ? "Gemini · Hỏi về Travel VN hoặc tra cứu."
                : "Gemini dùng lịch chuyến " + context + " để hiểu câu hỏi.") : "Cẩm nang trên máy · Gemini chưa kết nối.",
                11, MUTED, false);
        composer.addView(status);
        LinearLayout box = new LinearLayout(this);
        box.setGravity(Gravity.BOTTOM);
        box.setPadding(dp(8), dp(6), dp(6), dp(6));
        box.setBackground(round(WHITE, 22));
        input = new EditText(this);
        input.setTextSize(15);
        input.setTextColor(INK);
        input.setHint("Hỏi trợ lý…");
        input.setBackgroundColor(Color.TRANSPARENT);
        input.setMinHeight(dp(60));
        input.setMaxLines(4);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(1600)});
        box.addView(input, new LinearLayout.LayoutParams(0, -2, 1));
        send = button("↑", INK, WHITE, this::send);
        send.setTextSize(20);
        send.setContentDescription("Gửi câu hỏi");
        box.addView(send, new LinearLayout.LayoutParams(dp(48), dp(48)));
        composer.addView(box);
        root.addView(composer);
        root.requestFocus();
        String prompt = getIntent().getStringExtra("prompt");
        if (prompt != null && !prompt.isEmpty()) input.setText(prompt);
        if (getIntent().getBooleanExtra("settings", false)) configure();
    }

    private void welcome() {
        TextView icon = label("✦", 32, WHITE, true);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(round(PURPLE, 18));
        LinearLayout.LayoutParams iconSize = new LinearLayout.LayoutParams(dp(59), dp(59));
        iconSize.gravity = Gravity.CENTER;
        iconSize.setMargins(0, dp(17), 0, dp(10));
        welcome.addView(icon, iconSize);
        TextView title = label("Trợ lý du lịch", 23, INK, true);
        title.setGravity(Gravity.CENTER);
        welcome.addView(title);
        TextView subtitle = label("Hỏi mình về điểm đến, quán ăn hay mẹo đi chơi nhé!", 13, MUTED, false);
        subtitle.setGravity(Gravity.CENTER);
        welcome.addView(subtitle);
        String[][] prompts = {{"Quán cà phê view đẹp", "Ăn gì ngon"}, {"Chơi gì buổi tối", "Mẹo đi chơi"}};
        for (String[] group : prompts) {
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER);
            for (String text : group) {
                TextView chip = button(text, PALE, INK, () -> suggest(text));
                LinearLayout.LayoutParams place = new LinearLayout.LayoutParams(-2, -2);
                place.setMargins(dp(3), dp(5), dp(3), 0);
                row.addView(chip, place);
            }
            welcome.addView(row);
        }
        suggestions("Gợi ý cho bạn", new String[][]{
                {"Chia tiền chuyến đi", "Gợi ý cách chia tiền công bằng khi đi nhóm"},
                {"AI gợi ý lịch trình", "Lên lịch trình 3 ngày cho chuyến đi của mình"},
                {"Chuẩn bị hành lý", "Mình cần chuẩn bị gì cho chuyến đi?"}
        });
        suggestions("Trợ lý làm được gì?", new String[][]{
                {"Tìm địa điểm, quán ăn", "Gợi ý các quán ăn địa phương đáng thử"},
                {"Gợi ý lịch trình theo ngày", "Lên kế hoạch tham quan cho ngày 2"},
                {"Tìm khách sạn", "Gợi ý khu vực thuận tiện để đặt khách sạn"},
                {"Mẹo và kinh nghiệm du lịch", "Mình nên lưu ý gì khi đi lần đầu?"}
        });
    }

    private void suggestions(String title, String[][] items) {
        LinearLayout card = column();
        card.setPadding(dp(12), dp(9), dp(12), dp(5));
        card.setBackground(round(0xFFF6F8FD, 22));
        LinearLayout.LayoutParams place = new LinearLayout.LayoutParams(-1, -2);
        place.topMargin = dp(12);
        welcome.addView(card, place);
        card.addView(label(title, 14, PURPLE, true));
        for (String[] item : items) {
            TextView row = button(item[0] + "  ↗", 0xFFF6F8FD, INK, () -> suggest(item[1]));
            row.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            card.addView(row);
        }
    }

    private void suggest(String text) {
        input.setText(text);
        input.setSelection(text.length());
        input.requestFocus();
    }

    private void configure() {
        if (waiting) return;
        LinearLayout fields = column();
        fields.setPadding(dp(20), 0, dp(20), 0);
        EditText address = new EditText(this);
        address.setSingleLine();
        address.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        address.setHint("https://may-chu-cua-ban/api/chat");
        address.setText(TravelCloud.testEndpoint(this));
        fields.addView(address);
        EditText token = new EditText(this);
        token.setSingleLine();
        token.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        token.setHint("Mã truy cập Travel VN");
        token.setFilters(new InputFilter[]{new InputFilter.LengthFilter(128)});
        fields.addView(token);
        fields.addView(label("Nhập địa chỉ và mã truy cập của máy chủ Travel VN.", 13, MUTED, false));
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Kết nối trợ lý")
                .setView(fields).setNegativeButton("Hủy", null).setPositiveButton("Lưu", null).create();
        dialog.setOnShowListener(event -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            String next = address.getText().toString().trim();
            String secret = token.getText().toString().trim();
            if (!TravelCloud.validUrl(this, next)) {
                address.setError("Dùng HTTPS hoặc địa chỉ loopback để thử qua USB");
                return;
            }
            if (!secret.matches("[A-Za-z0-9_-]{24,128}")) {
                token.setError("Nhập mã truy cập máy chủ");
                return;
            }
            history.clear();
            messages.removeAllViews();
            releaseSearchWidgets();
            messages.addView(welcome);
            welcome.setVisibility(View.VISIBLE);
            TravelCloud.configureTest(this, next, secret);
            status.setText("Đã lưu kết nối. Bạn có thể gửi câu hỏi.");
            dialog.dismiss();
        }));
        dialog.show();
    }

    private void send() {
        String question = input.getText().toString().trim();
        if (waiting || question.isEmpty()) return;
        waiting = true;
        send.setEnabled(false);
        welcome.setVisibility(View.GONE);
        input.setText("");
        ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(input.getWindowToken(), 0);
        bubble(question, true);
        if (!TravelCloud.available(this)) {
            replyFromGuide(question);
            return;
        }
        status.setText("Đang hỏi Gemini…");
        List<Turn> request = new ArrayList<>(history.subList(Math.max(0, history.size() - 12), history.size()));
        request.add(new Turn("user", question));
        new Thread(() -> {
            try {
                JSONObject reply = ask(request);
                runOnUiThread(() -> finishTurn(question, reply, null));
            } catch (Exception error) {
                String message = error instanceof TravelCloud.ApiError ? error.getMessage()
                        : "Chưa kết nối được máy chủ trợ lý. Kiểm tra mạng rồi gửi lại.";
                runOnUiThread(() -> finishTurn(question, null, message));
            }
        }, "travel-assistant").start();
    }

    private void replyFromGuide(String question) {
        if (isFinishing() || isDestroyed()) return;
        try {
            TravelCatalog.Destination found = LocalTravelGuide.mentioned(question);
            String words = TravelCatalog.fold(question);
            Matcher day = PLAN_DAY.matcher(words);
            JSONObject reply;
            boolean otherArea = false;
            String padded = " " + words.replaceAll("[^a-z0-9 ]", " ") + " ";
            for (String province : ProvinceActivity.PROVINCES) {
                String area = TravelCatalog.fold(province).replaceAll("[^a-z0-9 ]", " ");
                if (padded.contains(" " + area + " ") && !province.equals(context)) {
                    otherArea = true;
                    break;
                }
            }
            if (days != null && !otherArea && (found == null || found.name().equals(context))
                    && (words.contains("di dau") || words.contains("lich") || words.contains("lam gi") || words.contains("co gi"))
                    && day.find() && !words.contains("goi y") && !words.contains("len lich")) {
                int index = Integer.parseInt(day.group(1));
                String text = index <= days.size() ? "Lịch ngày " + index + " · " + context + "\n\n" + days.get(index - 1)
                        : "Chuyến " + context + " có " + days.size() + " ngày trong lịch.";
                reply = new JSONObject().put("text", text).put("local", true);
            } else if ((found != null || !otherArea) && LocalTravelGuide.supports(question)) {
                if (found != null) lastDestination = found;
                reply = LocalTravelGuide.answer(question, context, lastDestination);
            } else {
                JSONObject search = new JSONObject().put("title", "Tìm bằng Google Search")
                        .put("url", "https://www.google.com/search?q=" + Uri.encode(question));
                reply = new JSONObject().put("local", true).put("sources", new JSONArray().put(search))
                        .put("text", "Gemini chưa kết nối. Câu này cần tra cứu thêm, bạn có thể mở Google Search bên dưới.");
            }
            finishTurn(question, reply, null);
        } catch (org.json.JSONException error) {
            finishTurn(question, null, "Chưa tạo được gợi ý. Bạn thử lại nhé.");
        }
    }

    private JSONObject ask(List<Turn> turns) throws Exception {
        JSONArray messages = new JSONArray();
        for (Turn turn : turns) messages.put(new JSONObject().put("role", turn.role()).put("text", turn.text()));
        JSONObject reply = TravelCloud.backend(this, "/api/chat",
                new JSONObject().put("context", appData).put("messages", messages));
        if (reply.optString("text").trim().isEmpty()) throw new TravelCloud.ApiError("Câu trả lời trống. Bạn thử lại nhé.");
        return reply;
    }

    private void finishTurn(String question, JSONObject reply, String error) {
        if (isFinishing() || isDestroyed()) return;
        waiting = false;
        send.setEnabled(true);
        if (reply == null) {
            messages.removeViewAt(messages.getChildCount() - 1);
            if (history.isEmpty()) welcome.setVisibility(View.VISIBLE);
            if (input.length() == 0) suggest(question);
            status.setText(error);
            return;
        }
        String answer = reply.optString("text");
        history.add(new Turn("user", question));
        history.add(new Turn("assistant", answer.substring(0, Math.min(6500, answer.length()))));
        if (history.size() > 12) history.subList(0, 2).clear();
        LinearLayout card = bubble(answer.replace("**", ""), false);
        boolean local = reply.optBoolean("local");
        if (local) card.addView(label("Cẩm nang Travel VN · Dữ liệu trên máy", 11, MUTED, false), 0);
        JSONArray sources = reply.optJSONArray("sources");
        if (sources != null && sources.length() > 0) {
            card.addView(label("Nguồn tham khảo", 13, PURPLE, true));
            for (int i = 0; i < sources.length(); i++) {
                JSONObject source = sources.optJSONObject(i);
                if (source == null) continue;
                String name = ("maps".equals(source.optString("kind")) ? "Google Maps · " : "") + source.optString("title");
                TextView link = button(name, WHITE, PURPLE, () -> openSource(Uri.parse(source.optString("url"))));
                link.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
                card.addView(link);
            }
        }
        String searchHtml = reply.optString("searchHtml");
        if (!searchHtml.isEmpty()) showSearchSuggestions(card, searchHtml);
        status.setText(local ? "Dữ liệu trên máy · Gemini chưa kết nối." : "Gemini · Trợ lý Travel VN");
        scroll.post(() -> scroll.smoothScrollTo(0, card.getTop()));
    }

    private void showSearchSuggestions(LinearLayout card, String html) {
        WebView web = new WebView(this);
        web.setContentDescription("Gợi ý từ Google Search");
        web.getSettings().setAllowFileAccess(false);
        web.getSettings().setAllowContentAccess(false);
        web.getSettings().setBlockNetworkLoads(true);
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (request.isForMainFrame() && request.hasGesture()) openSource(request.getUrl());
                return true;
            }
        });
        card.addView(web, new LinearLayout.LayoutParams(-1, dp(78)));
        widgets.add(web);
        web.loadDataWithBaseURL(null, "<meta name='viewport' content='width=device-width, initial-scale=1'>" + html, "text/html", "UTF-8", null);
    }

    private void openSource(Uri uri) {
        if (!"https".equals(uri.getScheme()) || uri.getHost() == null) return;
        try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); }
        catch (ActivityNotFoundException error) { status.setText("Thiết bị chưa có trình duyệt để mở nguồn."); }
    }

    private LinearLayout bubble(String value, boolean user) {
        LinearLayout card = column();
        card.setPadding(dp(12), dp(10), dp(12), dp(10));
        card.setBackground(round(user ? PALE : 0xFFF5F7FC, 18));
        LinearLayout.LayoutParams place = new LinearLayout.LayoutParams(user ? -2 : -1, -2);
        place.gravity = user ? Gravity.END : Gravity.START;
        place.setMargins(user ? dp(32) : 0, 0, 0, dp(10));
        messages.addView(card, place);
        TextView text = label(value, 15, INK, false);
        text.setTextIsSelectable(true);
        card.addView(text);
        scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
        return card;
    }

    @Override protected void onDestroy() {
        releaseSearchWidgets();
        super.onDestroy();
    }

    private void releaseSearchWidgets() {
        for (WebView widget : widgets) {
            if (widget.getParent() instanceof ViewGroup parent) parent.removeView(widget);
            widget.destroy();
        }
        widgets.clear();
    }

    private LinearLayout column() {
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.VERTICAL);
        return view;
    }

    private TextView label(String text, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private TextView button(String text, int fill, int ink, Runnable action) {
        TextView view = label(text, 13, ink, true);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(10), dp(7), dp(10), dp(7));
        view.setMinHeight(dp(44));
        view.setBackground(round(fill, 22));
        view.setFocusable(true);
        view.setOnClickListener(button -> action.run());
        return view;
    }

    private GradientDrawable round(int fill, int radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(fill);
        shape.setCornerRadius(dp(radius));
        return shape;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
