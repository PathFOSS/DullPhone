package com.pathfoss.dullphone;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.util.Objects;

public class OverlayPermissionConfirm extends DialogFragment {

    private final StartServiceListener startServiceListener;

    // Create constructor to pass the StartServiceListener interface
    public OverlayPermissionConfirm (StartServiceListener startServiceListener) {
        this.startServiceListener = startServiceListener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        // Inflate the view
        View view = inflater.inflate(R.layout.dialog_permission_screen_overlay, container, false);

        // Set button listeners
        view.findViewById(R.id.b_cancel).setOnClickListener(v -> dismiss());
        view.findViewById(R.id.b_confirm).setOnClickListener(v -> {
            dismiss();
            startServiceListener.overlayPermissionRequested();
        });

        return view;
    }

    // Create method to draw the dialog window with proper parameters
    @Override
    public void onResume() {
        super.onResume();
        DialogBackground.setDialogWindowParameters(Objects.requireNonNull(getDialog()));
    }
}
