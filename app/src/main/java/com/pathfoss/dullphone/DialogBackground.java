package com.pathfoss.dullphone;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.InsetDrawable;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import java.util.Objects;

public class DialogBackground {

    // Create method to set window parameter for DialogFragments
    public static void setDialogWindowParameters(@NonNull Dialog dialog) {

        Window window = dialog.getWindow();

        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.copyFrom(Objects.requireNonNull(window).getAttributes());
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        layoutParams.dimAmount = 0.7f;
        window.setAttributes(layoutParams);

        window.setBackgroundDrawable(new InsetDrawable(new ColorDrawable(Color.TRANSPARENT), Controller.convertDPtoPX(20)));
    }
}