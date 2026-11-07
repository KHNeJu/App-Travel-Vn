package vn.travelvn.app;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.AtomicFile;
import android.util.Patterns;
import android.widget.EditText;
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
    private String screenMode = "login";

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (state != null) screenMode = state.getString("mode", "login");
        render();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putString("mode", screenMode);
    }

    private void render() {
        JSONObject account = TravelCloud.session(this);
        if (!account.optString("uid").isEmpty()) profile(account);
        else form(screenMode);
    }

    private void form(String mode) {
        if (busy) return;
        screenMode = mode;
        boolean signup = mode.equals("signup");
        boolean reset = mode.equals("reset");
        screen(signup ? "Tạo tài khoản" : reset ? "Quên mật khẩu" : "Chào mừng trở lại",
                reset ? "Nhập email tài khoản để nhận liên kết đặt lại mật khẩu." : "Tài khoản Travel VN");
        LinearLayout form = card(body);
        EditText name = signup ? field(form, "Tên hiển thị", InputType.TYPE_CLASS_TEXT) : null;
        EditText email = field(form, "Email", EMAIL);
        EditText password = reset ? null : field(form, "Mật khẩu", PASSWORD);
        EditText confirm = signup ? field(form, "Nhập lại mật khẩu", PASSWORD) : null;
        status = label(form, TravelCloud.key(this).isEmpty() ? "Cần bật Firebase cho bản cài này." : "", 13, MUTED, false);
        space(form, 7);
        form.addView(button(signup ? "Tạo tài khoản" : reset ? "Gửi email khôi phục" : "Đăng nhập", BLUE, WHITE, () -> {
            String address = email.getText().toString().trim();
            if (!Patterns.EMAIL_ADDRESS.matcher(address).matches()) { email.setError("Nhập email hợp lệ"); return; }
            String secret = reset ? "" : password.getText().toString();
            String display = signup ? name.getText().toString().trim() : "";
            if (signup && (display.isEmpty() || display.length() > 60)) { name.setError("Nhập tên tối đa 60 ký tự"); return; }
            if (!reset && (secret.length() < (signup ? 6 : 1) || secret.length() > 256)) {
                password.setError(signup ? "Dùng ít nhất 6 ký tự" : "Nhập mật khẩu"); return;
            }
            if (signup && !secret.contentEquals(confirm.getText())) { confirm.setError("Mật khẩu xác nhận chưa khớp"); return; }
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
        if (!signup && !reset) {
            form.addView(button("Quên mật khẩu?", WHITE, BLUE, () -> form("reset")));
            body.addView(button("Tạo tài khoản", PALE, BLUE, () -> form("signup")));
        } else body.addView(button("Quay lại đăng nhập", PALE, BLUE, () -> form("login")));
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
            form("login");
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
