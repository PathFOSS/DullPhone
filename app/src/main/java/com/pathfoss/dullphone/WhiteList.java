package com.pathfoss.dullphone;

import static android.content.Context.MODE_PRIVATE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class WhiteList extends Fragment {

    private final StartServiceListener startServiceListener;
    private final UsagePermissionListener usagePermissionListener;
    private final ArrayList<String> allowedApps = new ArrayList<>();

    private PackageManager packageManager;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor sharedPreferencesEditor;

    // Create constructor to pass the StartServiceListener interface
    public WhiteList (StartServiceListener startServiceListener, UsagePermissionListener usagePermissionListener) {
        this.startServiceListener = startServiceListener;
        this.usagePermissionListener = usagePermissionListener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        packageManager = requireContext().getPackageManager();
        sharedPreferences = requireActivity().getSharedPreferences("DullPhone", MODE_PRIVATE);
        sharedPreferencesEditor = sharedPreferences.edit();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        // Inflate the view
        View view = inflater.inflate(R.layout.fragment_whitelist, container, false);

        // Initialize required whitelist elements
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> appList = packageManager.queryIntentActivities(intent, 0);
        LinearLayout whitelistLinearLayout = view.findViewById(R.id.ll);

        // Sort apps in alphabetical order
        SortedMap<String, String> sortedAppList = new TreeMap<>();
        for (ResolveInfo resolveInfo : appList) {
            try {
                String packageName = resolveInfo.activityInfo.packageName;
                sortedAppList.put((String) packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)), packageName);
            } catch (Exception ignored){}
        }

        // Create layout for each enabled app
        for (Map.Entry<String, String> stringStringEntry : sortedAppList.entrySet()) {

            // Define app and package name
            String appName = stringStringEntry.getKey();
            String packageName = stringStringEntry.getValue();

            if (!packageName.equals(sharedPreferences.getString("DefaultDialer", "com.android.dialer"))
                    && !packageName.equals("com.pathfoss.dullphone")
                    && !packageName.equals("com.android.settings")) {

                    // Initialize layout elements
                    ConstraintLayout clApp = (ConstraintLayout) inflater.inflate(R.layout.application_list_item, container, false);

                    ImageView appIcon = clApp.findViewById(R.id.iv);
                    TextView appTitle = clApp.findViewById(R.id.tv);

                    // Stylize app containers with identifiers
                    try {
                        clApp.setTag(packageName);
                        appIcon.setBackground(packageManager.getApplicationIcon(packageName));
                        appTitle.setText(appName);
                    } catch (Exception ignored) {
                    }

                    // Add icons and names in containers
                    whitelistLinearLayout.addView(clApp);

                    // Highlight old whitelist applications
                    if (sharedPreferences.getStringSet("WhitelistApps", new HashSet<>()).contains(packageName)) {
                        allowedApps.add(packageName);
                        clApp.setBackgroundResource(R.color.natural_white);
                        appTitle.setTextColor(AppCompatResources.getColorStateList(requireContext(), R.color.black));
                    }

                    // Initialize listener for the application selector
                    createWhitelistAppClickListener(clApp, appTitle);
            }
        }

        view.findViewById(R.id.ll_back).setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction().replace(R.id.fcv, new MainMenu(startServiceListener, usagePermissionListener)).commit();
            sharedPreferencesEditor.putStringSet("WhitelistApps", new HashSet<>(allowedApps)).apply();
            Toast.makeText(requireContext(), "Whitelist saved", Toast.LENGTH_SHORT).show();
        });

        return view;
    }

    // Create method to generate OnClickListeners for whitelist apps in a list
    private void createWhitelistAppClickListener(ConstraintLayout constraintLayout, TextView textView) {
        new Handler(Looper.getMainLooper()).post(() -> constraintLayout.setOnClickListener(v -> {
            if (!allowedApps.contains(constraintLayout.getTag().toString())) {
                allowedApps.add((String) constraintLayout.getTag());
                constraintLayout.setBackgroundResource(R.color.natural_white);
                textView.setTextColor(AppCompatResources.getColorStateList(requireContext(), R.color.black));
            } else {
                allowedApps.remove(constraintLayout.getTag().toString());
                constraintLayout.setBackgroundResource(R.color.black);
                textView.setTextColor(AppCompatResources.getColorStateList(requireContext(), R.color.natural_white));
            }
        }));
    }
}