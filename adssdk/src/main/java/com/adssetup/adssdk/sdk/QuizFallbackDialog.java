package com.adssetup.adssdk.sdk;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.ColorInt;
import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;

import java.util.function.Consumer;

final class QuizFallbackDialog {
    private final Dialog dialog;

    QuizFallbackDialog(@NonNull Activity activity, @LayoutRes int layoutRes, @IdRes int closeId, @ColorInt int windowColor, @NonNull Consumer<View> binder, @NonNull Runnable onClosed) {
        dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View content = LayoutInflater.from(activity).inflate(layoutRes, null, false);
        binder.accept(content);
        dialog.setContentView(content);
        dialog.setCancelable(true);
        dialog.setOnCancelListener(d -> onClosed.run());
        View close = content.findViewById(closeId);
        close.setVisibility(View.VISIBLE);
        close.setOnClickListener(v -> dialog.cancel());
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawable(new ColorDrawable(windowColor));
        }
    }

    void show() {
        dialog.show();
    }

    void dismiss() {
        dialog.setOnCancelListener(null);
        if (dialog.isShowing()) {
            dialog.dismiss();
        }
    }
}