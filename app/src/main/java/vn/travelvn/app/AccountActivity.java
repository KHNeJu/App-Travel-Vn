package vn.travelvn.app;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.AtomicFile;
import android.util.Patterns;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public final class AccountActivity extends TravelScreen {
    private static final int EMAIL = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS;
    private static final int PASSWORD = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD;
    private static final int COVER = 41;
    private boolean busy;
    private TextView status;
    private String screenMode = "welcome";

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        String start = getIntent().getStringExtra("mode");
        if ("signup".equals(start)) screenMode = start;
        if (state != null) screenMode = state.getString("mode", "welcome");
        render();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putString("mode", screenMode);
    }

    private void render() {
        JSONObject account = TravelCloud.session(this);
        if (!account.optString("uid").isEmpty()) profile(account);
        else if ("welcome".equals(screenMode)) welcome();
        else form(screenMode);
    }

    @Override public void onBackPressed() {
        if (busy) return;
        if (!TravelCloud.session(this).optString("uid").isEmpty() || "welcome".equals(screenMode)) super.onBackPressed();
        else if ("reset".equals(screenMode)) form("login");
        else welcome();
    }

    private void welcome() {
        screenMode = "welcome";
        screen("", "", WHITE);
        FrameLayout banner = new FrameLayout(this);
        banner.setBackground(round(0xFF1267D8, 26));
        banner.setClipToOutline(true);
        ImageView art = new ImageView(this);
        art.setImageResource(R.drawable.auth_landscape);
        art.setScaleType(ImageView.ScaleType.FIT_XY);
        art.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        banner.addView(art, new FrameLayout.LayoutParams(-1, -1));
        TextView brand = new TextView(this);
        brand.setText("Travel VN");
        brand.setTextSize(30);
        brand.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        brand.setTextColor(WHITE);
        brand.setGravity(Gravity.CENTER);
        banner.addView(brand, new FrameLayout.LayoutParams(-1, -1));
        body.addView(banner, new LinearLayout.LayoutParams(-1, dp(196)));
        space(body, 21);
        label(body, "Chào mừng trở lại", 32, INK, true);
        label(body, "Tiếp tục hành trình của bạn nhé.", 15, MUTED, false);
        space(body, 20);
        providers(body, true);
        body.addView(button("✉   Dùng email", PALE, INK, () -> form("login")));
        space(body, 10);
        body.addView(button("Chưa có tài khoản?  Đăng ký", WHITE, BLUE, () -> form("signup")));
        body.addView(new View(this), new LinearLayout.LayoutParams(1, 0, 1));
        space(body, 18);
        body.addView(button("Tiếp tục không cần đăng ký  →", WHITE, INK, this::finish));
    }

    private void providers(LinearLayout parent, boolean appleFirst) {
        TextView apple = button("Đăng nhập bằng Apple", INK, WHITE, () -> providerUnavailable("Apple"));
        TextView google = button("Đăng nhập bằng Google", WHITE, INK, () -> providerUnavailable("Google"));
        google.setBackground(outline(WHITE, 19, 0xFFDCE5EF));
        parent.addView(appleFirst ? apple : google);
        space(parent, 8);
        parent.addView(appleFirst ? google : apple);
        space(parent, 8);
    }

    private void providerUnavailable(String provider) {
        if (busy) return;
        new AlertDialog.Builder(this).setTitle("Đăng nhập bằng " + provider)
                .setMessage("Phương thức này chưa được bật cho Travel VN. Bạn có thể đăng nhập bằng email hoặc dùng chế độ khách.")
                .setNegativeButton("Đóng", null).setPositiveButton("Dùng email", (d, w) -> form("login")).show();
    }

    private android.graphics.drawable.GradientDrawable outline(int fill, int radius, int stroke) {
        android.graphics.drawable.GradientDrawable shape = round(fill, radius);
        shape.setStroke(dp(1), stroke);
        return shape;
    }

    private EditText authField(String title, String hint, int type) {
        if (type != PASSWORD) {
            EditText input = field(body, title, type);
            input.setHint(hint);
            input.setBackground(outline(WHITE, 19, 0xFF8B99AD));
            return input;
        }
        TextView caption = label(body, title, 14, INK, true);
        space(body, 5);
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setGravity(Gravity.CENTER_VERTICAL);
        wrapper.setBackground(outline(WHITE, 19, 0xFF8B99AD));
        EditText input = new EditText(this);
        input.setId(View.generateViewId());
        caption.setLabelFor(input.getId());
        input.setSingleLine();
        input.setTextSize(15);
        input.setPadding(dp(15), dp(8), dp(4), dp(8));
        input.setInputType(type);
        input.setTypeface(android.graphics.Typeface.DEFAULT);
        input.setHint(hint);
        input.setBackground(null);
        wrapper.addView(input, new LinearLayout.LayoutParams(0, dp(50), 1));
        TextView show = button("Hiện", WHITE, BLUE, () -> {});
        show.setBackground(new android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(0x287BA4F5), null, round(WHITE, 20)));
        show.setMinWidth(dp(56));
        show.setContentDescription("Hiện " + title.toLowerCase(java.util.Locale.ROOT));
        show.setOnClickListener(v -> {
            boolean visible = input.getTransformationMethod() == null;
            int cursor = input.getSelectionEnd();
            input.setTransformationMethod(visible ? PasswordTransformationMethod.getInstance() : null);
            if (cursor >= 0) input.setSelection(cursor);
            show.setText(visible ? "Hiện" : "Ẩn");
            show.setContentDescription(show.getText() + " " + title.toLowerCase(java.util.Locale.ROOT));
        });
        wrapper.addView(show);
        body.addView(wrapper);
        space(body, 11);
        return input;
    }

    private void form(String mode) {
        if (busy) return;
        screenMode = mode;
        boolean signup = mode.equals("signup");
        boolean reset = mode.equals("reset");
        screen(signup ? "Hành trình của bạn đang chờ!" : reset ? "Quên mật khẩu" : "Đăng nhập",
                reset ? "Nhập email tài khoản để nhận liên kết đặt lại mật khẩu."
                        : signup ? "Bắt đầu hành trình chỉ trong một phút." : "Tiếp tục hành trình với email của bạn.", WHITE);
        EditText name = signup ? authField("Họ và tên", "Nhập họ và tên", InputType.TYPE_CLASS_TEXT) : null;
        EditText email = authField("Email", "email@example.com", EMAIL);
        EditText password = reset ? null : authField("Mật khẩu", "Nhập mật khẩu", PASSWORD);
        if (signup) {
            label(body, "Ít nhất 8 ký tự, gồm chữ hoa, chữ thường và số.", 13, MUTED, false);
            space(body, 12);
        }
        EditText confirm = signup ? authField("Xác nhận mật khẩu", "Nhập lại mật khẩu", PASSWORD) : null;
        CheckBox terms = new CheckBox(this);
        if (signup) {
            terms.setText("Tôi đồng ý với điều khoản sử dụng");
            terms.setTextColor(INK);
            body.addView(terms);
            body.addView(button("Xem điều khoản và quyền riêng tư", WHITE, BLUE, () ->
                    new AlertDialog.Builder(this).setTitle("Tài khoản Travel VN")
                            .setMessage("Travel VN dùng Firebase để quản lý tài khoản email. Thông tin chuyến đi và ảnh bìa được lưu trên thiết bị. Khi bạn dùng Khám phá, từ khóa địa điểm được gửi tới Google Maps.")
                            .setNegativeButton("Đóng", null).setPositiveButton("Quyền riêng tư Google", (d, w) ->
                                    openPrivacy()).show()));
        }
        status = label(body, "", 13, MUTED, false);
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        body.addView(button(signup ? "Tạo tài khoản" : reset ? "Gửi email khôi phục" : "Đăng nhập", INK, WHITE, () -> {
            String address = email.getText().toString().trim();
            if (!Patterns.EMAIL_ADDRESS.matcher(address).matches()) { email.setError("Nhập email hợp lệ"); return; }
            String secret = reset ? "" : password.getText().toString();
            String display = signup ? name.getText().toString().trim() : "";
            if (signup && (display.isEmpty() || display.length() > 60)) { name.setError("Nhập tên tối đa 60 ký tự"); return; }
            if (!reset && (secret.isEmpty() || secret.length() > 256 || signup && (secret.length() < 8
                    || !secret.chars().anyMatch(Character::isUpperCase) || !secret.chars().anyMatch(Character::isLowerCase)
                    || !secret.chars().anyMatch(Character::isDigit)))) {
                password.setError(signup ? "Dùng ít nhất 8 ký tự, gồm chữ hoa, chữ thường và số" : "Nhập mật khẩu"); return;
            }
            if (signup && !secret.contentEquals(confirm.getText())) { confirm.setError("Mật khẩu xác nhận chưa khớp"); return; }
            if (signup && !terms.isChecked()) { status.setText("Bạn cần đồng ý với điều khoản để tạo tài khoản."); return; }
            work(() -> {
                if (reset) {
                    try {
                        TravelCloud.firebase(this, "sendOobCode", new JSONObject().put("requestType", "PASSWORD_RESET").put("email", address));
                    } catch (TravelCloud.ApiError error) {
                        if (!"EMAIL_NOT_FOUND".equals(error.getMessage())) throw error;
                    }
                    return "Nếu email thuộc một tài khoản, bạn sẽ nhận được liên kết khôi phục.";
                }
                JSONObject result = TravelCloud.firebase(this, signup ? "signUp" : "signInWithPassword",
                        new JSONObject().put("email", address).put("password", secret).put("returnSecureToken", true));
                JSONObject session = TravelCloud.save(this, result, new JSONObject());
                if (signup) {
                    try {
                        updateName(session, display);
                    } catch (Exception error) {
                        return "Tài khoản đã tạo. Bạn có thể lưu tên trong hồ sơ.";
                    }
                }
                return "Đã đăng nhập.";
            }, !reset);
        }));
        space(body, 10);
        if (signup) {
            label(body, "hoặc tiếp tục với", 14, MUTED, false).setGravity(Gravity.CENTER);
            space(body, 10);
            providers(body, false);
            body.addView(button("Đã có tài khoản?  Đăng nhập", WHITE, BLUE, () -> form("login")));
        } else if (reset) body.addView(button("Quay lại đăng nhập", PALE, BLUE, () -> form("login")));
        else {
            body.addView(button("Quên mật khẩu?", WHITE, BLUE, () -> form("reset")));
            body.addView(button("Chưa có tài khoản?  Đăng ký", WHITE, BLUE, () -> form("signup")));
        }
    }

    private void openPrivacy() {
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://policies.google.com/privacy?hl=vi"))); }
        catch (android.content.ActivityNotFoundException error) { status.setText("Thiết bị chưa có trình duyệt."); }
    }

    private void profile(JSONObject account) {
        screen("Hồ sơ của bạn", account.optString("email"));
        LinearLayout card = card(body);
        card.setClipToOutline(true);
        String uid = account.optString("uid");
        File file = TravelCloud.cover(this, uid);
        ImageView cover = new ImageView(this);
        cover.setScaleType(ImageView.ScaleType.CENTER_CROP);
        cover.setBackground(round(PALE, 22));
        cover.setClipToOutline(true);
        cover.setContentDescription("Ảnh bìa hồ sơ");
        if (file.isFile()) cover.setImageURI(Uri.fromFile(file));
        else cover.setImageResource(R.drawable.trang_an);
        card.addView(cover, new LinearLayout.LayoutParams(-1, dp(154)));
        card.addView(button("Đổi ảnh bìa", PALE, BLUE, () -> {
            if (busy) return;
            startActivityForResult(new Intent(Intent.ACTION_GET_CONTENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE), COVER);
        }));
        space(card, 10);
        EditText name = field(card, "Tên hiển thị", InputType.TYPE_CLASS_TEXT);
        name.setText(account.optString("name"));
        status = label(card, "", 13, MUTED, false);
        card.addView(button("Lưu tên", BLUE, WHITE, () -> {
            String value = name.getText().toString().trim();
            if (value.isEmpty() || value.length() > 60) { name.setError("Nhập tên tối đa 60 ký tự"); return; }
            work(() -> { updateName(account, value); return "Đã lưu tên."; }, true);
        }));
        body.addView(button("Đổi mật khẩu", PALE, BLUE, () -> changePassword(account)));
        space(body, 8);
        body.addView(button("Đăng xuất", WHITE, 0xFF9E303B, () -> {
            if (busy) return;
            TravelCloud.signOut(this);
            welcome();
        }));
    }

    private void updateName(JSONObject account, String name) throws Exception {
        JSONObject reply = TravelCloud.firebase(this, "update", new JSONObject().put("idToken", TravelCloud.idToken(this))
                .put("displayName", name).put("returnSecureToken", true));
        TravelCloud.save(this, reply, account);
    }

    private void changePassword(JSONObject account) {
        if (busy) return;
        LinearLayout fields = column();
        fields.setPadding(dp(20), dp(10), dp(20), 0);
        EditText old = field(fields, "Mật khẩu hiện tại", PASSWORD);
        EditText next = field(fields, "Mật khẩu mới", PASSWORD);
        EditText confirm = field(fields, "Nhập lại mật khẩu mới", PASSWORD);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Đổi mật khẩu").setView(fields)
                .setNegativeButton("Hủy", null).setPositiveButton("Lưu", null).create();
        dialog.setOnShowListener(event -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String current = old.getText().toString();
            String password = next.getText().toString();
            if (current.isEmpty()) { old.setError("Nhập mật khẩu hiện tại"); return; }
            if (password.length() < 6 || password.length() > 256) { next.setError("Dùng từ 6 đến 256 ký tự"); return; }
            if (!password.contentEquals(confirm.getText())) { confirm.setError("Mật khẩu xác nhận chưa khớp"); return; }
            dialog.dismiss();
            work(() -> {
                JSONObject proof = TravelCloud.firebase(this, "signInWithPassword", new JSONObject()
                        .put("email", account.optString("email")).put("password", current).put("returnSecureToken", true));
                JSONObject updated = TravelCloud.firebase(this, "update", new JSONObject()
                        .put("idToken", proof.getString("idToken")).put("password", password).put("returnSecureToken", true));
                TravelCloud.save(this, updated, account);
                return "Đã đổi mật khẩu.";
            }, true);
        }));
        dialog.show();
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != COVER || result != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        String uid = TravelCloud.session(this).optString("uid");
        if (uid.isEmpty()) return;
        work(() -> {
            BitmapFactory.Options info = new BitmapFactory.Options();
            info.inJustDecodeBounds = true;
            try (InputStream input = getContentResolver().openInputStream(uri)) { BitmapFactory.decodeStream(input, null, info); }
            if (info.outWidth <= 0 || info.outHeight <= 0) throw new TravelCloud.ApiError("Không đọc được ảnh đã chọn.");
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 1;
            while (Math.max(info.outWidth, info.outHeight) / options.inSampleSize > 1800) options.inSampleSize *= 2;
            Bitmap photo;
            try (InputStream input = getContentResolver().openInputStream(uri)) { photo = BitmapFactory.decodeStream(input, null, options); }
            if (photo == null) throw new TravelCloud.ApiError("Không đọc được ảnh đã chọn.");
            AtomicFile cover = new AtomicFile(TravelCloud.cover(this, uid));
            FileOutputStream output = cover.startWrite();
            try {
                if (!photo.compress(Bitmap.CompressFormat.JPEG, 88, output)) throw new TravelCloud.ApiError("Chưa lưu được ảnh bìa.");
                cover.finishWrite(output);
            } catch (Exception error) {
                cover.failWrite(output);
                throw error;
            } finally {
                photo.recycle();
            }
            return "Ảnh bìa đã lưu trên thiết bị.";
        }, true);
    }

    private interface Task { String run() throws Exception; }

    private void work(Task task, boolean refresh) {
        if (busy) return;
        busy = true;
        status.setText("Đang xử lý…");
        new Thread(() -> {
            String message;
            boolean success;
            try { message = task.run(); success = true; }
            catch (Exception error) { message = explain(error); success = false; }
            String result = message;
            boolean renderAgain = success && refresh;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                busy = false;
                if (renderAgain) render();
                status.setText(result);
            });
        }, "travel-account").start();
    }

    private String explain(Exception error) {
        if (!(error instanceof TravelCloud.ApiError)) return "Chưa kết nối được dịch vụ tài khoản. Bạn thử lại nhé.";
        return switch (error.getMessage()) {
            case "INVALID_LOGIN_CREDENTIALS", "EMAIL_NOT_FOUND", "INVALID_PASSWORD" -> "Email hoặc mật khẩu chưa đúng.";
            case "EMAIL_EXISTS" -> "Email này đã có tài khoản.";
            case "OPERATION_NOT_ALLOWED" -> "Firebase chưa bật đăng nhập bằng email.";
            case "TOO_MANY_ATTEMPTS_TRY_LATER" -> "Có quá nhiều lần thử. Hãy thử lại sau.";
            default -> error.getMessage().startsWith("WEAK_PASSWORD") ? "Mật khẩu chưa đáp ứng yêu cầu của Firebase." : error.getMessage();
        };
    }
}
