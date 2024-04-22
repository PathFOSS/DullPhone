package com.pathfoss.dullphone;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.NumberPicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import java.util.HashSet;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class MainMenu extends Fragment {

    private ConstraintLayout clRow;

    private final StartServiceListener startServiceListener;
    private final UsagePermissionListener usagePermissionListener;

    private SharedPreferences sharedPreferences;
    private PackageManager packageManager;

    private int currentImage = 0;

    // Create constructor to pass the StartServiceListener interface
    public MainMenu (StartServiceListener startServiceListener, UsagePermissionListener usagePermissionListener) {
        this.startServiceListener = startServiceListener;
        this.usagePermissionListener = usagePermissionListener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = requireActivity().getSharedPreferences("DullPhone", MODE_PRIVATE);
        packageManager = requireContext().getPackageManager();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        // Inflate and initialize the views
        View view = inflater.inflate(R.layout.fragment_main_menu, container, false);

        NumberPicker dayPicker = view.findViewById(R.id.np_day);
        NumberPicker hourPicker = view.findViewById(R.id.np_hour);
        NumberPicker minutePicker = view.findViewById(R.id.np_minute);

        LinearLayout linearLayout = view.findViewById(R.id.ll);

        // Set NumberPicker values for time selection
        setNumberPickerValues(dayPicker, 365, "d");
        setNumberPickerValues(hourPicker, 23, "h");
        setNumberPickerValues(minutePicker, 59, "m");

        // Set button listeners
        view.findViewById(R.id.ib_start).setOnClickListener(v -> startServiceListener.serviceStartRequested(dayPicker.getValue(), hourPicker.getValue(), minutePicker.getValue()));
        view.findViewById(R.id.ll_settings).setOnClickListener(v -> getParentFragmentManager().beginTransaction().replace(R.id.fcv, new Settings(startServiceListener, usagePermissionListener), "Settings").commit());
        view.findViewById(R.id.ll_edit).setOnClickListener(v -> getParentFragmentManager().beginTransaction().replace(R.id.fcv, new WhiteList(startServiceListener, usagePermissionListener), "WhiteList").commit());

        // Order whitelist apps alphabetically
        SortedMap<String, String> sortedAppList = new TreeMap<>();

        for (String packageName : sharedPreferences.getStringSet("WhitelistApps", new HashSet<>())) {
            try {
                sortedAppList.put((String) packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)), packageName);
            } catch (Exception ignored){}
        }

        // Create a clickable layout for each app
        for (Map.Entry<String, String> stringStringEntry : sortedAppList.entrySet()) {

            // Redefine package name and index
            currentImage++;
            String packageName = stringStringEntry.getValue();

            // Initialize Views
            switch (currentImage % 6) {
                case 0:
                    clRow.findViewById(R.id.iv_6).setBackground(getIcon(packageName));
                    break;
                case 1:
                    clRow = (ConstraintLayout) inflater.inflate(R.layout.images_row_layout, container, false);
                    clRow.findViewById(R.id.iv_1).setBackground(getIcon(packageName));
                    linearLayout.addView(clRow);
                    break;
                case 2:
                    clRow.findViewById(R.id.iv_2).setBackground(getIcon(packageName));
                    break;
                case 3:
                    clRow.findViewById(R.id.iv_3).setBackground(getIcon(packageName));
                    break;
                case 4:
                    clRow.findViewById(R.id.iv_4).setBackground(getIcon(packageName));
                    break;
                case 5:
                    clRow.findViewById(R.id.iv_5).setBackground(getIcon(packageName));
                    break;
            }
        }

        return view;
    }

    // Create method to get app icons by package name
    @Nullable
    private Drawable getIcon (String packageName) {
        try {
            return packageManager.getApplicationIcon(packageName);
        } catch (Exception ignored) {
            return null;
        }
    }

    // Create method to set NumberPicker values
    private void setNumberPickerValues(NumberPicker numberPicker, int maxValue, String timeUnit) {

        String[] timePickerArray = new String[maxValue + 1];

        for (int i = 0; i <= maxValue; i++) {
            timePickerArray[i] = i + " " + timeUnit;
        }

        numberPicker.setValue(0);
        numberPicker.setMaxValue(maxValue);
        numberPicker.setDisplayedValues(timePickerArray);
    }
}
