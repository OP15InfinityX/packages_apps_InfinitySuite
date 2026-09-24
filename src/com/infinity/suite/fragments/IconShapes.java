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

import static com.android.internal.util.infinity.ThemeUtils.ICON_SHAPE_KEY;

import android.content.Context;
import android.content.res.Configuration;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.Log;
import android.util.PathParser;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import com.android.internal.util.infinity.ThemeUtils;
import com.android.settingslib.Utils;
import com.google.android.material.card.MaterialCardView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IconShapes extends SettingsPreferenceFragment {

    private RecyclerView mRecyclerView;
    private ThemeUtils mThemeUtils;
    private LinearSnapHelper snapHelper;
    private Adapter mAdapter;

    private String mCategory = ICON_SHAPE_KEY;
    private List<String> mPkgs;
    private String mSelectedPkg;
    private String mAppliedPkg;
    private int mLastCenteredPosition = -1;

    private Map<String, Drawable> mAppIconsCache = new HashMap<>();
    private Map<String, String> mIconMaskCache = new HashMap<>();
    private Map<String, Drawable> mMaskedIconsCache = new HashMap<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().setTitle(R.string.theme_customization_icon_shape_title);

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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.shape_view_horizontal, container, false);

        mRecyclerView = view.findViewById(R.id.recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);
        mRecyclerView.setLayoutManager(layoutManager);

        snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(mRecyclerView);

        int spacing = getResources().getDimensionPixelSize(R.dimen.item_spacing);
        int extraSpacing = getResources().getDimensionPixelSize(R.dimen.item_extra_spacing);
        mRecyclerView.addItemDecoration(new HorizontalSpacingItemDecoration(spacing, extraSpacing));

        mAdapter = new Adapter(getActivity());
        
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
                int selectedPosition = mPkgs.indexOf(mSelectedPkg);
                enableOverlays(selectedPosition);
                mAppliedPkg = mSelectedPkg;
                mAdapter.notifyItemChanged(selectedPosition);
                mRecyclerView.post(() -> {
                    mRecyclerView.smoothScrollToPosition(selectedPosition);
                    mLastCenteredPosition = selectedPosition;
                    updatePreview(mRecyclerView);
                });
            }
        });

        return view;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.INFINITY;
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
        View centerView = snapHelper.findSnapView(layoutManager);
        if (centerView != null) {
            int centerPosition = layoutManager.getPosition(centerView);

            if (centerPosition != mLastCenteredPosition) {
                if (centerPosition >= 0 && centerPosition < mPkgs.size()) {
                    String pkg = mPkgs.get(centerPosition);
                    mSelectedPkg = pkg;

                    recyclerView.post(() -> {
                        mAdapter.notifyItemChanged(centerPosition);
                        updatePreviewImages(pkg);
                    });

                    vibrateOnCenter(getActivity());
                    mLastCenteredPosition = centerPosition;
                }
            }
        }
    }

    private void updatePreviewImages(String pkg) {
        String[] appPackages = {
                "com.android.settings",
                "com.android.dialer",
                "com.android.messaging",
                "com.android.chrome",
                "com.google.android.dialer",
                "com.google.android.apps.messaging",
                "com.android.vending",
                "com.whatsapp"
        };

        ImageView previewImage1 = getActivity().findViewById(R.id.image1);
        ImageView previewImage2 = getActivity().findViewById(R.id.image2);
        ImageView previewImage3 = getActivity().findViewById(R.id.image3);
        ImageView previewImage4 = getActivity().findViewById(R.id.image4);

        ImageView[] previewImages = {previewImage1, previewImage2, previewImage3, previewImage4};
        int previewCount = 0;

        for (String appPackage : appPackages) {
            if (previewCount >= 4) break;
            
            String cacheKey = appPackage + ":" + pkg;
            if (mMaskedIconsCache.containsKey(cacheKey)) {
                previewImages[previewCount].setImageDrawable(mMaskedIconsCache.get(cacheKey));
                previewImages[previewCount].setVisibility(View.VISIBLE);
                previewCount++;
                continue;
            }

            Drawable appIcon = getAppIcon(getActivity(), appPackage);
            if (appIcon != null) {
                Drawable shapedIcon = applyIconShape(getActivity(), appIcon, pkg);
                if (shapedIcon != null) {
                    previewImages[previewCount].setImageDrawable(shapedIcon);
                    previewImages[previewCount].setVisibility(View.VISIBLE);
                    previewCount++;
                }
            }
        }

        for (int i = previewCount; i < 4; i++) {
            previewImages[i].setVisibility(View.GONE);
        }
    }

    private void vibrateOnCenter(Context context) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
        }
    }

    public Drawable applyIconShape(Context context, Drawable appIcon, String pkg) {
        if (appIcon == null) {
            Log.e("IconShapes", "App icon is null");
            return null;
        }

        try {
            Resources overlayRes = pkg.equals("android") ? Resources.getSystem() : context.getPackageManager().getResourcesForApplication(pkg);

            int iconMaskId = overlayRes.getIdentifier("config_icon_mask", "string", pkg);
            if (iconMaskId == 0) {
                Log.e("IconShapes", "config_icon_mask not found in package: " + pkg);
                return appIcon;
            }

            String pathData = overlayRes.getString(iconMaskId);
            if (TextUtils.isEmpty(pathData)) {
                Log.e("IconShapes", "config_icon_mask is null or empty for package: " + pkg);
                return appIcon;
            }

            Path path = PathParser.createPathFromPathData(pathData);
            if (path == null) {
                Log.e("IconShapes", "Failed to create path from path data: " + pathData);
                return appIcon;
            }

            Bitmap bitmap = drawableToBitmap(appIcon);
            if (bitmap == null) {
                Log.e("IconShapes", "Failed to convert drawable to bitmap");
                return appIcon;
            }
            
            int scaleFactor = 2;
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() * scaleFactor, bitmap.getHeight() * scaleFactor, true);

            path = scalePathToBitmap(path, scaledBitmap);

            android.graphics.RectF pathBounds = new android.graphics.RectF();
        path.computeBounds(pathBounds, true);

        float iconCenterX = scaledBitmap.getWidth() / 2f;
        float iconCenterY = scaledBitmap.getHeight() / 2f;
        float pathCenterX = pathBounds.centerX();
        float pathCenterY = pathBounds.centerY();

        android.graphics.Matrix translateMatrix = new android.graphics.Matrix();
        translateMatrix.postTranslate(iconCenterX - pathCenterX, iconCenterY - pathCenterY);
        path.transform(translateMatrix);

        Bitmap maskedBitmap = applyMaskToBitmap(scaledBitmap, path);
        if (maskedBitmap == null) {
            Log.e("IconShapes", "Failed to apply mask to bitmap");
            return appIcon;
        }

        Bitmap finalBitmap = Bitmap.createScaledBitmap(maskedBitmap, bitmap.getWidth(), bitmap.getHeight(), true);

        return new BitmapDrawable(context.getResources(), finalBitmap);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("IconShapes", "Package not found: " + pkg, e);
        } catch (Resources.NotFoundException e) {
            Log.e("IconShapes", "Resource not found for package: " + pkg, e);
        }
        return appIcon;
    }

    private Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
        if (width <= 0 || height <= 0) {
            return null;
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    private Bitmap applyMaskToBitmap(Bitmap bitmap, Path path) {
        if (bitmap == null || path == null) {
            return null;
        }

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Bitmap output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.BLACK);
        canvas.drawPath(path, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, 0, 0, paint);

        return output;
    }
    
    private Path scalePathToBitmap(Path path, Bitmap bitmap) {
        android.graphics.RectF bounds = new android.graphics.RectF();
        path.computeBounds(bounds, true);

        float scaleX = bitmap.getWidth() / bounds.width();
        float scaleY = bitmap.getHeight() / bounds.height();

        android.graphics.Matrix matrix = new android.graphics.Matrix();
        matrix.postScale(scaleX, scaleY);

        Path scaledPath = new Path();
        path.transform(matrix, scaledPath);

        return scaledPath;
    }

    public Drawable getAppIcon(Context context, String packageName) {
        if (mAppIconsCache.containsKey(packageName)) {
            return mAppIconsCache.get(packageName);
        }

        try {
            PackageManager pm = context.getPackageManager();
            Drawable icon = pm.getApplicationIcon(packageName);
            mAppIconsCache.put(packageName, icon);
            return icon;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("IconShapes", "App icon not found for package: " + packageName, e);
        }
        return null;
    }

    public static class HorizontalSpacingItemDecoration extends RecyclerView.ItemDecoration {
        private final int spacing;
        private final int extraSpacing;

        public HorizontalSpacingItemDecoration(int spacing, int extraSpacing) {
            this.spacing = spacing;
            this.extraSpacing = extraSpacing;
        }

        @Override
        public void getItemOffsets(@NonNull android.graphics.Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
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

    public class Adapter extends RecyclerView.Adapter<Adapter.CustomViewHolder> {
        Context context;

        public Adapter(Context context) {
            this.context = context;
        }

        @Override
        public CustomViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.shape_option, parent, false);
            return new CustomViewHolder(v);
        }

        @Override
        public void onBindViewHolder(CustomViewHolder holder, final int position) {
            holder.image.setBackgroundDrawable(null);
            holder.image.setBackgroundTintList(null);
            holder.cardView.setStrokeWidth(0);
            holder.cardView.setStrokeColor(ContextCompat.getColor(context, android.R.color.transparent));
            String pkg = mPkgs.get(position);
            Drawable shapeDrawable = mThemeUtils.createShapeDrawable(pkg);
            int accentColor = resolveSystemAccentColor(context);
            final int color = ColorUtils.setAlphaComponent(
                    Utils.getColorAttrDefaultColor(getContext(), android.R.attr.colorAccent),
                    170);
            if (shapeDrawable != null) {
                holder.image.setBackgroundDrawable(shapeDrawable);
                holder.image.setBackgroundTintList(ColorStateList.valueOf(color));
            } else {
                Log.e("IconShapes", "Shape drawable is null for package: " + pkg);
            }
            holder.name.setText("android".equals(pkg) ? "Default" : getLabel(holder.name.getContext(), pkg));

            if (pkg.equals(mAppliedPkg)) {
                holder.cardView.setStrokeColor(accentColor);
                holder.cardView.setStrokeWidth((int) context.getResources().getDimension(R.dimen.stroke_width_active));
            } else {
                holder.cardView.setStrokeColor(ContextCompat.getColor(context, android.R.color.transparent));
                holder.cardView.setStrokeWidth(0);
            }

            holder.itemView.setOnClickListener(v -> {
                mSelectedPkg = pkg;
                notifyItemChanged(position);
            });
        }

        @Override
        public int getItemCount() {
            return mPkgs.size();
        }
        
        @Override
        public long getItemId(int position) {
            return mPkgs.get(position).hashCode();
        }

        public class CustomViewHolder extends RecyclerView.ViewHolder {
            TextView name;
            ImageView image;
            MaterialCardView cardView;

            public CustomViewHolder(View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.option_title);
                image = itemView.findViewById(R.id.option_image);
                cardView = itemView.findViewById(R.id.card_view);
            }
        }

        private String getLabel(Context context, String pkg) {
            PackageManager pm = context.getPackageManager();
            try {
                return pm.getApplicationLabel(pm.getApplicationInfo(pkg, PackageManager.GET_META_DATA)).toString();
            } catch (PackageManager.NameNotFoundException e) {
                Log.e("IconShapes", "Label not found for package: " + pkg, e);
            }
            return pkg;
        }
    }

    private int resolveSystemAccentColor(Context context) {
        return Utils.getColorAttrDefaultColor(context, com.android.internal.R.attr.colorAccent);
    }

    public void enableOverlays(int position) {
        String selectedPkg = mPkgs.get(position);
        mThemeUtils.setOverlayEnabled(mCategory, selectedPkg, "android");
    }
}
