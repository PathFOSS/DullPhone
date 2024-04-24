package com.pathfoss.dullphone;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.util.Objects;

public class BlockConfirm extends DialogFragment {

    private final Handler workerThreadHandler = new Handler();
    private final StartServiceListener startServiceListener;

    private final long goalTime;

    private SharedPreferences.Editor sharedPreferencesEditor;

    // Create constructor to pass the StartServiceListener interface and a desired time in milliseconds
    public BlockConfirm (StartServiceListener startServiceListener, long goalTime) {
        this.startServiceListener = startServiceListener;
        this.goalTime = goalTime;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferencesEditor = requireContext().getSharedPreferences("DullPhone", Context.MODE_PRIVATE).edit();
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        // Inflate and initialize views
        View view = inflater.inflate(R.layout.dialog_confirmation_block, container, false);
        TextView tvTime = view.findViewById(R.id.tv_time);

        // Show decrease in blocking time every second and close dialog if time runs out
        workerThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                long timeLeft = goalTime - System.currentTimeMillis();
                int hours = (int) (timeLeft / 3600000);
                int minutes = (int) (timeLeft - hours * 3600000) / 60000;
                int seconds = (int) (timeLeft - hours * 3600000 - minutes * 60000) / 1000;

                String timeText = getTimeNumber(hours) + "h  " + getTimeNumber(minutes) + "m  " + getTimeNumber(seconds) + "s";
                tvTime.setText(timeText);

                if (timeLeft > 0) {
                    workerThreadHandler.postDelayed(this, 1000 - timeLeft + goalTime - System.currentTimeMillis());
                } else {
                    dismiss();
                    workerThreadHandler.removeCallbacksAndMessages(null);
                }
            }
        });

        // Set actions on cancel
        view.findViewById(R.id.b_cancel).setOnClickListener(v -> {
            workerThreadHandler.removeCallbacksAndMessages(null);
            dismiss();
        });

        // Set actions on confirm
        view.findViewById(R.id.b_confirm).setOnClickListener(v -> {
            dismiss();
            sharedPreferencesEditor.putBoolean("UsingWhitelistApp", false).apply();
            sharedPreferencesEditor.putLong("UnlockTime", goalTime).apply();
            workerThreadHandler.removeCallbacksAndMessages(null);
            startServiceListener.serviceStartConfirmed();
        });

        return view;
    }

    // Create method to get consistent time numbers
    @NonNull
    private String getTimeNumber(int input) {
        if (input < 10) {
            return "0" + input;
        }
        return String.valueOf(input);
    }

    // Create method to draw the dialog window with proper parameters
    @Override
    public void onResume() {
        super.onResume();
        DialogBackground.setDialogWindowParameters(Objects.requireNonNull(getDialog()));
    }
}
