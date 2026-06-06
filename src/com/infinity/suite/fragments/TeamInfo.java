/*
 * Copyright (C) 2024 Project Infinity X
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.infinity.suite.fragments;

import com.android.internal.logging.nano.MetricsProto;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.google.android.material.card.MaterialCardView;

public class TeamInfo extends SettingsPreferenceFragment {

    private static final String PROJECT_WEBSITE = "https://projectinfinity-x.com";
    private static final String GITHUB_TEJAS = "https://github.com/tejas101k";
    private static final String PAYPAL_URL = "https://paypal.me/tejas101k";
    private static final String COFFEE_URL = "https://buymeacoffee.com/tejas101k";
    private static final String UPI_ID = "tejas101k@upi";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.team_info_layout, container, false);
        
        setupLogoClick(view);
        setupTeamCards(view);
        setupDonationButtons(view);
        
        return view;
    }

    private void setupLogoClick(View view) {
        View logoInfinity = view.findViewById(R.id.logo_infinity);
        logoInfinity.setOnClickListener(v -> openUrl(PROJECT_WEBSITE));
    }

    private void setupTeamCards(View view) {
        MaterialCardView cardTejas = view.findViewById(R.id.card_tejas);

        cardTejas.setOnClickListener(v -> openUrl(GITHUB_TEJAS));
    }

    private void setupDonationButtons(View view) {
        View btnPaypal = view.findViewById(R.id.btn_paypal);
        View btnCoffee = view.findViewById(R.id.btn_buymeacoffee);
        View btnUpi = view.findViewById(R.id.btn_upi);

        btnPaypal.setOnClickListener(v -> {
            openUrl(PAYPAL_URL);
            showThankYouMessage();
        });

        btnCoffee.setOnClickListener(v -> {
            openUrl(COFFEE_URL);
            showThankYouMessage();
        });

        btnUpi.setOnClickListener(v -> {
            copyToClipboard(UPI_ID);
            Toast.makeText(getContext(), 
                "UPI ID copied: " + UPI_ID, 
                Toast.LENGTH_LONG).show();
            showThankYouMessage();
        });
    }

    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Unable to open link", Toast.LENGTH_SHORT).show();
        }
    }

    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("UPI ID", text);
        clipboard.setPrimaryClip(clip);
    }

    private void showThankYouMessage() {
        Toast.makeText(getContext(), R.string.donation_thank_you, Toast.LENGTH_SHORT).show();
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.INFINITY;
    }
}
