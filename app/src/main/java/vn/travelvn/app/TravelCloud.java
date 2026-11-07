package vn.travelvn.app;

import android.content.Context;
import android.util.AtomicFile;
import android.util.Base64;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

final class TravelCloud {
    private static final Object sessionLock = new Object();
    private static final Object refreshLock = new Object();
    private static volatile String testToken = "";

    static class ApiError extends Exception {
        ApiError(String message) { super(message); }
    }

    static String key(Context context) {
        return context.getString(R.string.travel_firebase_key).trim();
    }

    static JSONObject firebase(Context context, String operation, JSONObject values) throws Exception {
        if (key(context).isEmpty()) throw new ApiError("Bản cài này cần cấu hình Firebase để dùng tài khoản.");
        return post("https://identitytoolkit.googleapis.com/v1/accounts:" + operation + "?key=" + key(context),
                "application/json", values.toString(), "", 12_000);
    }

    private static AtomicFile sessionFile(Context context) {
        return new AtomicFile(new File(context.getNoBackupFilesDir(), "travel-account.json"));
    }

    static JSONObject session(Context context) {
        synchronized (sessionLock) {
            try {
                return new JSONObject(new String(sessionFile(context).readFully(), StandardCharsets.UTF_8));
            } catch (Exception error) {
                return new JSONObject();
            }
        }
    }

    static void signOut(Context context) {
        synchronized (sessionLock) { sessionFile(context).delete(); }
    }

    static JSONObject save(Context context, JSONObject reply, JSONObject previous) throws Exception {
        JSONObject saved = new JSONObject()
                .put("uid", reply.optString("localId", reply.optString("user_id", previous.optString("uid"))))
                .put("email", reply.optString("email", previous.optString("email")))
                .put("name", reply.optString("displayName", previous.optString("name")))
                .put("idToken", reply.optString("idToken", reply.optString("id_token", previous.optString("idToken"))))
                .put("refresh", reply.optString("refreshToken", reply.optString("refresh_token", previous.optString("refresh"))))
                .put("expires", System.currentTimeMillis() + 1000L * reply.optLong("expiresIn", reply.optLong("expires_in", 3600)));
        synchronized (sessionLock) {
            AtomicFile file = sessionFile(context);
            FileOutputStream stream = file.startWrite();
            try {
                stream.write(saved.toString().getBytes(StandardCharsets.UTF_8));
                file.finishWrite(stream);
            } catch (Exception error) {
                file.failWrite(stream);
                throw error;
            }
        }
        return saved;
    }

    static String idToken(Context context) throws Exception {
        synchronized (refreshLock) {
            JSONObject saved = session(context);
            if (saved.optString("uid").isEmpty()) return "";
            if (saved.optLong("expires") > System.currentTimeMillis() + 60_000) return saved.optString("idToken");
            String refresh = saved.optString("refresh");
            if (refresh.isEmpty() || key(context).isEmpty()) return "";
            String body = "grant_type=refresh_token&refresh_token=" + URLEncoder.encode(refresh, "UTF-8");
            JSONObject updated = post("https://securetoken.googleapis.com/v1/token?key=" + key(context),
                    "application/x-www-form-urlencoded", body, "", 12_000);
            synchronized (sessionLock) {
                JSONObject latest = session(context);
                if (!saved.optString("idToken").equals(latest.optString("idToken"))) return latest.optString("idToken");
                return save(context, updated, latest).optString("idToken");
            }
        }
    }

    static File cover(Context context, String uid) {
        String name = Base64.encodeToString(uid.getBytes(StandardCharsets.UTF_8), Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
        return new File(context.getFilesDir(), "cover-" + name + ".jpg");
    }

    static void configureTest(Context context, String chatUrl, String token) {
        context.getSharedPreferences("cloud", Context.MODE_PRIVATE).edit().putString("endpoint", chatUrl).apply();
        testToken = token;
    }

    static String testEndpoint(Context context) {
        return context.getSharedPreferences("cloud", Context.MODE_PRIVATE).getString("endpoint", "");
    }

    static boolean available(Context context) {
        return !context.getString(R.string.travel_backend_url).trim().isEmpty() || !testEndpoint(context).isEmpty() && !testToken.isEmpty();
    }

    static JSONObject backend(Context context, String path, JSONObject body) throws Exception {
        String base = context.getString(R.string.travel_backend_url).trim().replaceAll("/+$", "");
        String token;
        if (!base.isEmpty()) {
            if (!validUrl(context, base)) throw new ApiError("Địa chỉ máy chủ Travel VN không hợp lệ.");
            token = idToken(context);
        } else {
            base = testEndpoint(context).replaceFirst("/api/chat/?$", "").replaceAll("/+$", "");
            token = testToken;
            if (base.isEmpty() || !validUrl(context, base)) throw new ApiError("Bản cài này cần máy chủ Travel VN để truy cập dữ liệu Google.");
        }
        if (token.isEmpty()) throw new ApiError("Đăng nhập Travel VN để tiếp tục.");
        return post(base + path, "application/json; charset=utf-8", body.toString(), token, 48_000);
    }

    static boolean validUrl(Context context, String address) {
        try {
            URI uri = URI.create(address);
            String host = uri.getHost();
            if (host == null || uri.getRawUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) return false;
            if ("https".equals(uri.getScheme())) return true;
            boolean debug = (context.getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0;
            return debug && "http".equals(uri.getScheme()) && ("127.0.0.1".equals(host) || "10.0.2.2".equals(host));
        } catch (IllegalArgumentException error) {
            return false;
        }
    }

    private static JSONObject post(String url, String type, String body, String token, int timeout) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        try {
            connection.setRequestMethod("POST");
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(timeout);
            connection.setRequestProperty("Content-Type", type);
            if (!token.isEmpty()) connection.setRequestProperty("Authorization", "Bearer " + token);
            byte[] data = body.getBytes(StandardCharsets.UTF_8);
            connection.setDoOutput(true);
            connection.setFixedLengthStreamingMode(data.length);
            try (var out = connection.getOutputStream()) { out.write(data); }
            int code = connection.getResponseCode();
            try (InputStream input = code == 200 ? connection.getInputStream() : connection.getErrorStream()) {
                if (input == null) throw new ApiError("Máy chủ chưa phản hồi.");
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                for (int count; (count = input.read(buffer)) >= 0; ) {
                    if (bytes.size() + count > 230_000) throw new ApiError("Phản hồi quá dài.");
                    bytes.write(buffer, 0, count);
                }
                JSONObject result = new JSONObject(new String(bytes.toByteArray(), StandardCharsets.UTF_8));
                if (code == 200) return result;
                JSONObject error = result.optJSONObject("error");
                throw new ApiError(error == null ? result.optString("error", "Máy chủ chưa xử lý được yêu cầu.")
                        : error.optString("message", "Dịch vụ Google chưa xử lý được yêu cầu."));
            }
        } finally {
            connection.disconnect();
        }
    }

    private TravelCloud() {}
}
