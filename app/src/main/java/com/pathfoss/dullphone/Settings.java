package com.pathfoss.dullphone;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.Fragment;

import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputLayout;

import java.util.HashMap;
import java.util.Objects;

public class Settings extends Fragment {

    private AutoCompleteTextView actTaps;
    private Slider sVibration;
    private Slider sWhitelist;

    private final StartServiceListener startServiceListener;
    private final HashMap<ImageView, int[]> iconMap = new HashMap<>();
    private final String[] tapsList = {"1000", "2000", "5000", "10000", "25000","50000", "100000"};

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor sharedPreferencesEditor;

    // Create constructor to pass the StartServiceListener interface
    public Settings (StartServiceListener startServiceListener) {
        this.startServiceListener = startServiceListener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = requireContext().getSharedPreferences("DullPhone", MODE_PRIVATE);
        sharedPreferencesEditor = sharedPreferences.edit();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        // Inflate and define views
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        TextInputLayout tilTaps = view.findViewById(R.id.til_taps);
        tilTaps.setHint("Taps to Unlock");

        sVibration = view.findViewById(R.id.s_vibration);
        sWhitelist = view.findViewById(R.id.s_whitelist);
        actTaps = tilTaps.findViewById(R.id.act);

        // Configure dropdown
        actTaps.setDropDownBackgroundDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.background_dropdown));

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(requireContext(), R.layout.layout_autocomplete_textview_item, tapsList);
        arrayAdapter.setDropDownViewResource(R.layout.layout_autocomplete_textview_item);

        actTaps.setAdapter(arrayAdapter);
        actTaps.setText(String.valueOf(sharedPreferences.getInt("TapsToUnlockPreference", 5000)), false);

        // Configure toggles
        sVibration.setValue(convertBooleanToFloat(sharedPreferences.getBoolean("TapVibration", false)));
        sWhitelist.setValue(convertBooleanToFloat(sharedPreferences.getBoolean("WhitelistEnabled", true)));

        // Set action on exit
        view.findViewById(R.id.ll_back).setOnClickListener(v -> {
            sharedPreferencesEditor.putInt("TapsToUnlockPreference", Integer.parseInt(actTaps.getText().toString())).apply();
            sharedPreferencesEditor.putBoolean("TapVibration", sVibration.getValue() == 1f).apply();
            sharedPreferencesEditor.putBoolean("WhitelistEnabled", sWhitelist.getValue() == 1f).apply();

            Toast.makeText(requireContext(), "Settings saved", Toast.LENGTH_SHORT).show();
            getParentFragmentManager().beginTransaction().replace(R.id.fcv, new MainMenu(startServiceListener)).commit();
        });

        // Create hashmap for icon set
        iconMap.put(view.findViewById(R.id.iv_dullphone), new int[]{R.drawable.icon_dullphone, R.drawable.icon_dullphone_selected, R.drawable.icon_dullphone_small});
        iconMap.put(view.findViewById(R.id.iv_skull), new int[]{R.drawable.icon_skull, R.drawable.icon_skull_selected, R.drawable.icon_skull_small});
        iconMap.put(view.findViewById(R.id.iv_fire), new int[]{R.drawable.icon_fire, R.drawable.icon_fire_selected, R.drawable.icon_fire_small});

        configureIcons();

        return view;
    }

    // Create method to work with the icon gallery
    private void configureIcons() {
        int current = sharedPreferences.getInt("DefaultIcon", R.drawable.icon_dullphone);
        for (ImageView imageView : iconMap.keySet()) {
            if (Objects.requireNonNull(iconMap.get(imageView))[0] == current) {
                imageView.setBackgroundResource(Objects.requireNonNull(iconMap.get(imageView))[1]);
            } else {
                imageView.setBackgroundResource(Objects.requireNonNull(iconMap.get(imageView))[2]);
            }
            setIconListener(imageView);
        }
    }

    // Create method to set an icon element listener
    private void setIconListener(@NonNull ImageView imageView) {
        imageView.setOnClickListener( v -> {
            sharedPreferencesEditor.putInt("DefaultIcon", Objects.requireNonNull(iconMap.get(imageView))[0]).apply();
            configureIcons();
        });
    }

    // Create method to convert from boolean to float
    private float convertBooleanToFloat (boolean bool) {
        if (bool) {
            return 1f;
        }
        return 0f;
    }
}