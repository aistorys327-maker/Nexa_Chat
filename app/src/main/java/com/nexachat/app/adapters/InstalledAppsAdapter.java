package com.nexachat.app.adapters;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nexachat.app.R;
import com.nexachat.app.databinding.ItemAppBinding;
import com.nexachat.app.models.InstalledAppInfo;

import java.util.ArrayList;
import java.util.List;

public class InstalledAppsAdapter extends RecyclerView.Adapter<InstalledAppsAdapter.ViewHolder> {

    public interface OnAppActionListener {
        void onAppClick(InstalledAppInfo appInfo);
        void onAppLongClick(InstalledAppInfo appInfo);
        void onAppRemove(InstalledAppInfo appInfo);
    }

    private final Context context;
    private final List<InstalledAppInfo> appsList = new ArrayList<>();
    private final OnAppActionListener listener;
    private final PackageManager packageManager;

    public InstalledAppsAdapter(Context context, OnAppActionListener listener) {
        this.context = context;
        this.listener = listener;
        this.packageManager = context.getPackageManager();
    }

    public void setApps(List<InstalledAppInfo> apps) {
        appsList.clear();
        if (apps != null) {
            appsList.addAll(apps);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAppBinding binding = ItemAppBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        InstalledAppInfo app = appsList.get(position);
        holder.bind(app);
    }

    @Override
    public int getItemCount() {
        return appsList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemAppBinding binding;

        ViewHolder(ItemAppBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(InstalledAppInfo app) {
            binding.tvAppName.setText(app.getAppName());
            binding.tvAppPackage.setText(app.getPackageName());

            binding.ivAppSecureShield.setVisibility(app.isSecured() ? View.VISIBLE : View.GONE);

            // Fetch app icon from package manager
            try {
                Drawable icon = packageManager.getApplicationIcon(app.getPackageName());
                binding.ivAppIcon.setImageDrawable(icon);
            } catch (PackageManager.NameNotFoundException e) {
                binding.ivAppIcon.setImageResource(R.drawable.ic_apps);
            }

            // Normal Tap: open normally
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAppClick(app);
                }
            });

            // Long Press: start security verification
            binding.getRoot().setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onAppLongClick(app);
                }
                return true;
            });

            // Remove button
            binding.btnRemoveApp.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAppRemove(app);
                }
            });
        }
    }
}
