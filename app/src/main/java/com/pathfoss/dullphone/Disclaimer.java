package com.pathfoss.dullphone;

import static android.content.Context.MODE_PRIVATE;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class Disclaimer extends Fragment {

    private final StartServiceListener startServiceListener;
    private final UsagePermissionListener usagePermissionListener;

    // Create constructor to pass the StartServiceListener interface
    public Disclaimer (StartServiceListener startServiceListener, UsagePermissionListener usagePermissionListener) {
        this.startServiceListener = startServiceListener;
        this.usagePermissionListener = usagePermissionListener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        //Inflate the view
        View view = inflater.inflate(R.layout.fragment_disclaimer, container, false);

        // Create a listener to confirm accepted terms
        view.findViewById(R.id.b_accept).setOnClickListener(v -> {
            requireContext().getSharedPreferences("DullPhone", MODE_PRIVATE).edit().putBoolean("TermsAccepted", true).apply();
            getParentFragmentManager().beginTransaction().replace(R.id.fcv, new MainMenu(startServiceListener, usagePermissionListener)).commit();
        });

        return view;
    }
}