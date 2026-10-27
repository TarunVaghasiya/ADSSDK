package com.adssetup.adssdk.sdk;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.MobileAds;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SdkAuthorization {
    private static final String ENDPOINT = "https://adssdk-auth.tarunpatel4138.workers.dev/";
    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 8_000;
    private static final int MAX_BODY_BYTES = 1_024;

    private enum State {UNKNOWN, IN_PROGRESS, AUTHORIZED, DENIED}

    private static final AtomicBoolean ADS_INITIALIZED = new AtomicBoolean();
    private static State state = State.UNKNOWN;

    interface Callback {
        void onResult(boolean authorized);
    }

    private static final Object LOCK = new Object();
    private static final List<Callback> PENDING = new ArrayList<>();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "sdk-authorization");
        thread.setDaemon(true);
        return thread;
    });

    private SdkAuthorization() {
    }

    public static void init(@NonNull Context context) {
        authorize(context, authorized -> {
        });
    }

    static void authorize(@NonNull Context context, @NonNull Callback callback) {
        synchronized (LOCK) {
            if (state == State.AUTHORIZED || state == State.DENIED) {
                boolean result = state == State.AUTHORIZED;
                MAIN.post(() -> callback.onResult(result));
                return;
            }
            PENDING.add(callback);
            if (state == State.IN_PROGRESS) {
                return;
            }
            state = State.IN_PROGRESS;
        }
        Context appContext = context.getApplicationContext();
        EXECUTOR.execute(() -> finish(appContext, request(appContext.getPackageName())));
    }

    private static void finish(@NonNull Context appContext, Boolean result) {
        boolean granted = Boolean.TRUE.equals(result);
        List<Callback> callbacks;
        synchronized (LOCK) {
            callbacks = new ArrayList<>(PENDING);
            PENDING.clear();
            state = result == null ? State.UNKNOWN : granted ? State.AUTHORIZED : State.DENIED;
        }
        MAIN.post(() -> {
            if (granted && ADS_INITIALIZED.compareAndSet(false, true)) {
                MobileAds.initialize(appContext);
            }
            for (Callback callback : callbacks) {
                callback.onResult(granted);
            }
        });
    }

    private static Boolean request(@NonNull String packageName) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(ENDPOINT).openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setUseCaches(false);
            connection.setInstanceFollowRedirects(false);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("Accept", "application/json");

            byte[] body = new JSONObject().put("packageName", packageName).toString().getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(body.length);
            try (OutputStream out = connection.getOutputStream()) {
                out.write(body);
            }

            int code = connection.getResponseCode();
            if (code == HttpURLConnection.HTTP_FORBIDDEN) {
                return Boolean.FALSE;
            }
            if (code != HttpURLConnection.HTTP_OK) {
                return null;
            }
            try (InputStream in = connection.getInputStream()) {
                return parse(readBounded(in));
            }
        } catch (IOException | JSONException | RuntimeException e) {
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static Boolean parse(@NonNull String body) throws JSONException {
        Object value = new JSONObject(body).opt("authorized");
        return value instanceof Boolean ? (Boolean) value : null;
    }

    private static String readBounded(@NonNull InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[256];
        int read;
        while ((read = in.read(buffer)) != -1) {
            if (out.size() + read > MAX_BODY_BYTES) {
                throw new IOException("Authorization response too large");
            }
            out.write(buffer, 0, read);
        }
        return out.toString(StandardCharsets.UTF_8.name());
    }
}