/*
 * Copyright (C) 2024-25 Project Infinity X
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

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Vibrator;
import android.os.VibrationEffect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextClock;
import android.widget.TextView;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;
import androidx.fragment.app.Fragment;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.Utils;

import com.android.internal.util.infinity.ThemeUtils;
import com.infinity.suite.utils.SystemUtils;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class LockClockFonts extends SettingsPreferenceFragment {

    private RecyclerView mRecyclerView;
    private ThemeUtils mThemeUtils;
    private String mCategory = "android.theme.customization.lockscreen_clock_font";
    private List<String> mPkgs;
    private String mSelectedPkg;
    private String mAppliedPkg;
    private SnapHelper mSnapHelper;
    private Adapter mAdapter;

    private int mLastCenteredPosition = -1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().setTitle(R.string.theme_customization_lock_clock_title);

        mThemeUtils = new ThemeUtils(getActivity());
        mPkgs = mThemeUtils.getOverlayPackagesForCategory(mCategory, "android");

        if (savedInstanceState != null) {
            mLastCenteredPosition = savedInstanceState.getInt("lastCenteredPosition", 0);
        } else {
            mLastCenteredPosition = mPkgs.indexOf(mAppliedPkg);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("selectedPkg", mSelectedPkg);
        outState.putString("appliedPkg", mAppliedPkg);
        outState.putInt("lastCenteredPosition", mLastCenteredPosition);
    }
    
    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            mSelectedPkg = savedInstanceState.getString("selectedPkg");
            mAppliedPkg = savedInstanceState.getString("appliedPkg");
            mLastCenteredPosition = savedInstanceState.getInt("lastCenteredPosition", 0);
        }
    }
    
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        if (mRecyclerView != null && mLastCenteredPosition != -1) {
            mRecyclerView.post(() -> {
                mRecyclerView.scrollToPosition(mLastCenteredPosition);
            });
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.clock_view_horizontal, container, false);

        mRecyclerView = view.findViewById(R.id.recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);
        mRecyclerView.setLayoutManager(layoutManager);

        mSnapHelper = new LinearSnapHelper();
        mSnapHelper.attachToRecyclerView(mRecyclerView);

        int spacing = getResources().getDimensionPixelSize(R.dimen.item_spacing);
        int extraSpacing = getResources().getDimensionPixelSize(R.dimen.item_extra_spacing);
        mRecyclerView.addItemDecoration(new HorizontalSpacingItemDecoration(spacing, extraSpacing));

        mAdapter = new Adapter(getActivity(), mPkgs);
        mRecyclerView.setAdapter(mAdapter);

        mRecyclerView.post(() -> {
            mRecyclerView.scrollToPosition(mLastCenteredPosition);
            scaleItems(mRecyclerView);
        });

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                scaleItems(recyclerView);
                updatePreview(recyclerView);
            }

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    updatePreview(recyclerView);
                }
            }
        });

        Button applyButton = view.findViewById(R.id.apply_button);
        applyButton.setOnClickListener(v -> {
            if (mSelectedPkg != null) {
                Settings.Secure.putIntForUser(getContext().getContentResolver(),
                    Settings.Secure.LOCK_SCREEN_CUSTOM_CLOCK_FACE, 0, UserHandle.USER_CURRENT);
                int selectedPosition = mPkgs.indexOf(mSelectedPkg);
                enableOverlays(selectedPosition);
                mAppliedPkg = mSelectedPkg;
                
                mAdapter.notifyDataSetChanged();

                mRecyclerView.smoothScrollToPosition(selectedPosition);
                mLastCenteredPosition = selectedPosition;

                SystemUtils.restartSystemUI(getContext());
            }
        });

        return view;
    }

    private void scaleItems(RecyclerView recyclerView) {
        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        int firstVisible = layoutManager.findFirstVisibleItemPosition();
        int lastVisible = layoutManager.findLastVisibleItemPosition();

        int centerX = recyclerView.getWidth() / 2;

        for (int i = firstVisible; i <= lastVisible; i++) {
            View itemView = layoutManager.findViewByPosition(i);
            if (itemView != null) {
                int itemCenterX = (itemView.getLeft() + itemView.getRight()) / 2;
                int distanceFromCenter = Math.abs(centerX - itemCenterX);

                float maxDistance = recyclerView.getWidth() / 2f;
                float scale = 1.0f - (0.3f * (distanceFromCenter / maxDistance));
                float alpha = 1.0f - (0.2f * (distanceFromCenter / maxDistance));

                itemView.setScaleX(scale);
                itemView.setScaleY(scale);
                itemView.setAlpha(alpha);
            }
        }
    }

    private void updatePreview(RecyclerView recyclerView) {
        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        View centerView = mSnapHelper.findSnapView(layoutManager);
        if (centerView != null) {
            int centerPosition = layoutManager.getPosition(centerView);

            if (centerPosition >= 0 && centerPosition < mPkgs.size()) {
                String pkg = mPkgs.get(centerPosition);
                TextClock previewText = getActivity().findViewById(R.id.preview_text);
                previewText.setTypeface(getTypeface(getActivity(), pkg));
                mSelectedPkg = pkg;
                if (mLastCenteredPosition != centerPosition) {
                    vibrateOnCenter(getActivity());
                    mLastCenteredPosition = centerPosition;
                }
            }
        }
    }

    private void vibrateOnCenter(Context context) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
        }
    }

    public static class HorizontalSpacingItemDecoration extends RecyclerView.ItemDecoration {
        private final int spacing;
        private final int extraSpacing;

        public HorizontalSpacingItemDecoration(int spacing, int extraSpacing) {
            this.spacing = spacing;
            this.extraSpacing = extraSpacing;
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            if (position == 0) {
                outRect.left = spacing + extraSpacing;
                outRect.right = spacing / 2;
            } else if (position == parent.getAdapter().getItemCount() - 1) {
                outRect.left = spacing / 2;
                outRect.right = spacing + extraSpacing;
            } else {
                outRect.left = spacing / 2;
                outRect.right = spacing / 2;
            }
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.INFINITY;
    }

    public class Adapter extends RecyclerView.Adapter<Adapter.CustomViewHolder> {
        private Context context;
        private List<String> mPkgs;

        public Adapter(Context context, List<String> pkgs) {
            this.context = context;
            this.mPkgs = pkgs;
        }

        @Override
        public CustomViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.fonts_option, parent, false);
            return new CustomViewHolder(v);
        }

        @Override
        public void onBindViewHolder(CustomViewHolder holder, final int position) {
            String pkg = mPkgs.get(position);
            String label = getLabel(holder.itemView.getContext(), pkg);

            holder.cardView.setStrokeColor(ContextCompat.getColor(context, android.R.color.transparent));
            holder.cardView.setStrokeWidth(0);

            holder.title.setText("android".equals(pkg) ? "Default" : label);
            holder.title.setTypeface(getTypeface(holder.title.getContext(), pkg));

            int accentColor = resolveSystemAccentColor(context);

            if (pkg.equals(mAppliedPkg)) {
                holder.cardView.setStrokeColor(accentColor);
        	holder.cardView.setStrokeWidth((int) context.getResources().getDimension(R.dimen.stroke_width_active));
    	    } else if (pkg.equals(mSelectedPkg)) {
        	holder.cardView.setStrokeColor(accentColor);
        	holder.cardView.setStrokeWidth((int) context.getResources().getDimension(R.dimen.stroke_width_active));
    	    }
        }

        @Override
        public int getItemCount() {
            return mPkgs.size();
        }

        public class CustomViewHolder extends RecyclerView.ViewHolder {
            TextView title;
            MaterialCardView cardView;

            public CustomViewHolder(View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.option_title);
                cardView = itemView.findViewById(R.id.card_view);
            }
        }
    }

    private int resolveSystemAccentColor(Context context) {
        return Utils.getColorAttrDefaultColor(context, com.android.internal.R.attr.colorAccent);
    }

    public Typeface getTypeface(Context context, String pkg) {
        try {
            PackageManager pm = context.getPackageManager();
            Resources res = pkg.equals("android") ? Resources.getSystem()
                    : pm.getResourcesForApplication(pkg);
            return Typeface.create(res.getString(
                    res.getIdentifier("config_clockFontFamily",
                            "string", pkg)), Typeface.NORMAL);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getLabel(Context context, String pkg) {
        PackageManager pm = context.getPackageManager();
        try {
            return pm.getApplicationInfo(pkg, 0)
                    .loadLabel(pm).toString();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return pkg;
    }

    public void enableOverlays(int position) {
        mThemeUtils.setOverlayEnabled(mCategory, mPkgs.get(position), "android");
    }
}