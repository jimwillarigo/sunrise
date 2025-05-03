package com.example.myapplication;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

public class dislib extends Fragment {

    private Button highlightedButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_dislib, container, false);

        // Retrieve the passed disease name
        String diseaseName = getArguments() != null ? getArguments().getString("diseaseName") : "";

        // Reference buttons
        Button leafBlastButton = view.findViewById(R.id.buttonLeafBlast);
        Button BrownSpotButton = view.findViewById(R.id.buttonBrownSpot);
        Button NeckBlastButton = view.findViewById(R.id.buttonNeckBlast);
        Button TungroButton = view.findViewById(R.id.buttonTungro);
        Button StemBorerButton = view.findViewById(R.id.buttonStemBorer);
        Button HealthyButton = view.findViewById(R.id.buttonHealthy);

        // Set onClickListeners for each button
        BrownSpotButton.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), brownspot.class);
            intent.putExtra("diseaselibraryID", "-OCIB-OYADG_zbhOmfC_");
            startActivity(intent);
        });

        leafBlastButton.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), leafblast.class);
            intent.putExtra("diseaselibraryID", "-OCIBAWTLF3QM3QV5vKz");
            startActivity(intent);
        });

        NeckBlastButton.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), neckblast.class);
            intent.putExtra("diseaselibraryID", "-OCIBDFaiCgADVPkth9D");
            startActivity(intent);
        });

        HealthyButton.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), healthy.class);
            intent.putExtra("diseaselibraryID", "-OCIBG_LuYHQZHTUo0UO");
            startActivity(intent);
        });

        TungroButton.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), tungro.class);
            intent.putExtra("diseaselibraryID", "-OCIBK0w9CTJe-5LJ14z");
            startActivity(intent);
        });

        StemBorerButton.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), stemborer.class);
            intent.putExtra("diseaselibraryID", "-OCIBLrPUH5lT4wOw6Eb");
            startActivity(intent);
        });

        // Adjust the tint of the button corresponding to the disease
        if ("Leaf Blast".equalsIgnoreCase(diseaseName)) {
            highlightedButton = leafBlastButton;
        } else if ("Brown Spot".equalsIgnoreCase(diseaseName)) {
            highlightedButton = BrownSpotButton;
        } else if ("Neck Blast".equalsIgnoreCase(diseaseName)) {
            highlightedButton = NeckBlastButton;
        } else if ("Tungro".equalsIgnoreCase(diseaseName)) {
            highlightedButton = TungroButton;
        } else if ("Stem Borer".equalsIgnoreCase(diseaseName)) {
            highlightedButton = StemBorerButton;
        } else if ("Healthy".equalsIgnoreCase(diseaseName)) {
            highlightedButton = HealthyButton;
        }

        if (highlightedButton != null) {
            adjustButtonTintWithAnimation(highlightedButton);

            // Simulate a button click after a 1-second delay
            new Handler().postDelayed(() -> highlightedButton.performClick(), 2500);
        }

        return view;
    }

    private void adjustButtonTintWithAnimation(Button button) {
        if (button != null) {
            // Start and end colors for the animation
            int startColor = ContextCompat.getColor(requireContext(), android.R.color.white);
            int endColor = ContextCompat.getColor(requireContext(), R.color.autumn);

            // Create ValueAnimator for background tint
            ValueAnimator backgroundAnimator = ValueAnimator.ofArgb(startColor, endColor);
            backgroundAnimator.setDuration(1500); // Duration of the transition (500ms)
            backgroundAnimator.addUpdateListener(animation ->
                    button.setBackgroundTintList(ColorStateList.valueOf((int) animation.getAnimatedValue()))
            );
            backgroundAnimator.start();

            // Optional: Animate the text color
            int startTextColor = ContextCompat.getColor(requireContext(), android.R.color.black);
            int endTextColor = ContextCompat.getColor(requireContext(), android.R.color.white);

            ValueAnimator textAnimator = ValueAnimator.ofArgb(startTextColor, endTextColor);
            textAnimator.setDuration(1500); // Same duration
            textAnimator.addUpdateListener(animation ->
                    button.setTextColor((int) animation.getAnimatedValue())
            );
            textAnimator.start();
        }
    }

}
