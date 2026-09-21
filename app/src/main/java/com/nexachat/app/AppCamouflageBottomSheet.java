package com.nexachat.app;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.nexachat.app.databinding.BottomSheetAppCamouflageBinding;
import com.nexachat.app.security.DisguiseManager;
import com.nexachat.app.security.SecurityHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AppCamouflageBottomSheet extends BottomSheetDialogFragment {

    private BottomSheetAppCamouflageBinding binding;
    private String selectedType = DisguiseManager.TYPE_CALCULATOR;
    private String selectedAppName = "Calculator";
    private String selectedPackage = "";
    private String selectedAlias = DisguiseManager.ALIAS_CALCULATOR;

    public static AppCamouflageBottomSheet newInstance() {
        return new AppCamouflageBottomSheet();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetAppCamouflageBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        loadCurrentState();
        setupListeners();
    }

    private void loadCurrentState() {
        Context context = requireContext();
        DisguiseManager dm = DisguiseManager.getInstance();

        boolean isEnabled = dm.isDisguiseEnabled(context);
        binding.switchEnableCamouflage.setChecked(isEnabled);
        updateStatusText(isEnabled);

        selectedType = dm.getDisguiseType(context);
        selectedAppName = dm.getDisguisedAppName(context);
        selectedPackage = dm.getDisguisedPackageName(context);
        selectedAlias = dm.getActiveAlias(context);

        if (selectedPackage != null && !selectedPackage.isEmpty()) {
            try {
                PackageManager pm = context.getPackageManager();
                ApplicationInfo info = pm.getApplicationInfo(selectedPackage, 0);
                binding.tvSelectedAppName.setText(selectedAppName);
                binding.tvSelectedAppPackage.setText(selectedPackage);
                binding.ivCustomAppIcon.setImageDrawable(pm.getApplicationIcon(info));
                if (binding.etCustomAppName != null) {
                    binding.etCustomAppName.setText(selectedAppName);
                }
            } catch (Exception e) {
                binding.tvSelectedAppName.setText(selectedAppName);
                binding.tvSelectedAppPackage.setText(selectedPackage);
                if (binding.etCustomAppName != null) {
                    binding.etCustomAppName.setText(selectedAppName);
                }
            }
        } else if (binding.etCustomAppName != null && selectedAppName != null) {
            binding.etCustomAppName.setText(selectedAppName);
        }

        highlightSelectedPreset(selectedType);
    }

    private void updateStatusText(boolean isEnabled) {
        if (isEnabled) {
            binding.tvCamouflageStatus.setText("ACTIVE: Disguised as " + selectedAppName);
            binding.tvCamouflageStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.cyan_accent));
        } else {
            binding.tvCamouflageStatus.setText("DISABLED: Default NexaChat icon shown on home screen");
            binding.tvCamouflageStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        }
    }

    private void highlightSelectedPreset(String type) {
        int colorSelected = ContextCompat.getColor(requireContext(), R.color.cyan_glow_start);
        int colorNormal = ContextCompat.getColor(requireContext(), R.color.dark_surface_card);

        binding.cardDisguiseCalculator.setBackgroundColor(DisguiseManager.TYPE_CALCULATOR.equals(type) ? colorSelected : colorNormal);
        binding.cardDisguiseNotes.setBackgroundColor(DisguiseManager.TYPE_NOTES.equals(type) ? colorSelected : colorNormal);
        binding.cardDisguiseGame.setBackgroundColor(DisguiseManager.TYPE_GAME.equals(type) ? colorSelected : colorNormal);
        binding.cardDisguiseClock.setBackgroundColor(DisguiseManager.TYPE_CLOCK.equals(type) ? colorSelected : colorNormal);
    }

    private void setupListeners() {
        binding.btnCloseCamouflage.setOnClickListener(v -> dismiss());

        binding.switchEnableCamouflage.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateStatusText(isChecked);
        });

        binding.cardDisguiseCalculator.setOnClickListener(v -> {
            selectedType = DisguiseManager.TYPE_CALCULATOR;
            selectedAppName = "Calculator";
            selectedPackage = "";
            selectedAlias = DisguiseManager.ALIAS_CALCULATOR;
            highlightSelectedPreset(selectedType);
            updateStatusText(binding.switchEnableCamouflage.isChecked());
        });

        binding.cardDisguiseNotes.setOnClickListener(v -> {
            selectedType = DisguiseManager.TYPE_NOTES;
            selectedAppName = "Notes";
            selectedPackage = "";
            selectedAlias = DisguiseManager.ALIAS_NOTES;
            highlightSelectedPreset(selectedType);
            updateStatusText(binding.switchEnableCamouflage.isChecked());
        });

        binding.cardDisguiseGame.setOnClickListener(v -> {
            selectedType = DisguiseManager.TYPE_GAME;
            selectedAppName = "Game Space";
            selectedPackage = "";
            selectedAlias = DisguiseManager.ALIAS_GAME;
            highlightSelectedPreset(selectedType);
            updateStatusText(binding.switchEnableCamouflage.isChecked());
        });

        binding.cardDisguiseClock.setOnClickListener(v -> {
            selectedType = DisguiseManager.TYPE_CLOCK;
            selectedAppName = "Clock";
            selectedPackage = "";
            selectedAlias = DisguiseManager.ALIAS_CLOCK;
            highlightSelectedPreset(selectedType);
            updateStatusText(binding.switchEnableCamouflage.isChecked());
        });

        binding.btnPickInstalledApp.setOnClickListener(v -> showInstalledAppPickerDialog());
        binding.cardAddCustomApp.setOnClickListener(v -> showInstalledAppPickerDialog());

        binding.btnApplyCamouflage.setOnClickListener(v -> {
            Context context = requireContext();
            boolean enable = binding.switchEnableCamouflage.isChecked();

            if (enable) {
                // If custom name is entered, use that custom name
                if (binding.etCustomAppName != null && !TextUtils.isEmpty(binding.etCustomAppName.getText())) {
                    selectedAppName = binding.etCustomAppName.getText().toString().trim();
                }

                // Prompt user with fingerprint or face/PIN verification before saving disguise settings
                SecurityHelper.authenticate(requireActivity(), "Disguise Verification", "Verify Fingerprint, Face, or PIN to save disguise", new SecurityHelper.AuthCallback() {
                    @Override
                    public void onSuccess() {
                        if (isAdded() && getContext() != null) {
                            DisguiseManager.getInstance().enableDisguise(
                                    requireContext(),
                                    selectedType,
                                    selectedAppName,
                                    selectedPackage,
                                    selectedAlias
                            );
                            Toast.makeText(requireContext(), "Disguise Applied! Saved as " + selectedAppName, Toast.LENGTH_LONG).show();
                            dismiss();
                        }
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        if (isAdded() && getContext() != null) {
                            Toast.makeText(requireContext(), "Authentication required: " + errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            } else {
                DisguiseManager.getInstance().disableDisguise(context);
                Toast.makeText(context, "Camouflage Disabled! Default NexaChat restored.", Toast.LENGTH_SHORT).show();
                dismiss();
            }
        });
    }

    private static class AppEntry {
        String label;
        String packageName;
        Drawable icon;

        AppEntry(String label, String packageName, Drawable icon) {
            this.label = label;
            this.packageName = packageName;
            this.icon = icon;
        }
    }

    private void showInstalledAppPickerDialog() {
        Context context = requireContext();
        PackageManager pm = context.getPackageManager();

        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(mainIntent, 0);

        List<AppEntry> apps = new ArrayList<>();
        String myPackage = context.getPackageName();

        for (ResolveInfo ri : resolveInfos) {
            if (ri.activityInfo != null && !ri.activityInfo.packageName.equals(myPackage)) {
                String label = ri.loadLabel(pm).toString();
                Drawable icon = ri.loadIcon(pm);
                apps.add(new AppEntry(label, ri.activityInfo.packageName, icon));
            }
        }

        // Sort alphabetically
        Collections.sort(apps, (a, b) -> a.label.compareToIgnoreCase(b.label));

        if (apps.isEmpty()) {
            Toast.makeText(context, "No external apps found to disguise with.", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayAdapter<AppEntry> adapter = new ArrayAdapter<AppEntry>(context, android.R.layout.select_dialog_item, apps) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView tv = view.findViewById(android.R.id.text1);
                AppEntry entry = getItem(position);
                if (entry != null) {
                    tv.setText(entry.label);
                    tv.setTextColor(ContextCompat.getColor(context, R.color.text_white));
                    int size = (int) (32 * context.getResources().getDisplayMetrics().density);
                    entry.icon.setBounds(0, 0, size, size);
                    tv.setCompoundDrawables(entry.icon, null, null, null);
                    tv.setCompoundDrawablePadding((int) (12 * context.getResources().getDisplayMetrics().density));
                }
                return view;
            }
        };

        new MaterialAlertDialogBuilder(context)
                .setTitle("Select App or Game to Add")
                .setAdapter(adapter, (dialog, which) -> {
                    AppEntry chosen = apps.get(which);
                    selectedType = DisguiseManager.TYPE_CUSTOM;
                    selectedAppName = chosen.label;
                    selectedPackage = chosen.packageName;
                    selectedAlias = DisguiseManager.ALIAS_CUSTOM;

                    binding.tvSelectedAppName.setText(chosen.label);
                    binding.tvSelectedAppPackage.setText(chosen.packageName);
                    binding.ivCustomAppIcon.setImageDrawable(chosen.icon);
                    if (binding.etCustomAppName != null) {
                        binding.etCustomAppName.setText(chosen.label);
                    }

                    highlightSelectedPreset("");
                    binding.switchEnableCamouflage.setChecked(true);
                    updateStatusText(true);
                    Toast.makeText(context, "Selected " + chosen.label + " for disguise", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
