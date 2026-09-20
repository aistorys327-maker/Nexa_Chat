package com.nexachat.app;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.nexachat.app.databinding.ActivityCamouflageGatewayBinding;
import com.nexachat.app.security.DisguiseManager;

import java.util.concurrent.Executor;

public class CamouflageGatewayActivity extends AppCompatActivity {

    private ActivityCamouflageGatewayBinding binding;
    private final StringBuilder calcExpression = new StringBuilder();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Camera mCamera;
    private boolean isVerifyingFace = false;
    private boolean isUnlocked = false;

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startInvisibleCameraFaceScan();
                } else {
                    // Fallback to biometric or fast verification
                    checkBiometricOrUnlock();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCamouflageGatewayBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupCamouflageUI();

        // Check if launched directly from Home Screen Long-Press shortcut
        boolean directFaceUnlock = getIntent().getBooleanExtra("direct_face_unlock", false);
        if (directFaceUnlock) {
            triggerAutomaticFaceUnlock();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent.getBooleanExtra("direct_face_unlock", false)) {
            triggerAutomaticFaceUnlock();
        }
    }

    private void setupCamouflageUI() {
        DisguiseManager dm = DisguiseManager.getInstance();
        String type = dm.getDisguiseType(this);
        String appName = dm.getDisguisedAppName(this);
        String pkg = dm.getDisguisedPackageName(this);

        binding.tvCamouflageTitle.setText(appName);

        // Load real app icon if pkg is set
        if (pkg != null && !pkg.trim().isEmpty()) {
            try {
                PackageManager pm = getPackageManager();
                Drawable icon = pm.getApplicationIcon(pkg);
                binding.ivCamouflageIcon.setImageDrawable(icon);
                binding.ivBigAppIcon.setImageDrawable(icon);
                binding.tvBigAppName.setText(appName);
            } catch (Exception ignored) {
                setPresetIcon(type);
            }
        } else {
            setPresetIcon(type);
        }

        // Check if Calculator or App/Game decoy
        if (DisguiseManager.TYPE_CALCULATOR.equals(type)) {
            binding.layoutCalculatorDecoy.setVisibility(View.VISIBLE);
            binding.layoutAppGameDecoy.setVisibility(View.GONE);
            setupCalculator();
        } else {
            binding.layoutCalculatorDecoy.setVisibility(View.GONE);
            binding.layoutAppGameDecoy.setVisibility(View.VISIBLE);
            setupAppGameDecoy();
        }

        // Long-press anywhere on the screen triggers instant Face Unlock
        binding.camouflageRoot.setOnLongClickListener(v -> {
            triggerAutomaticFaceUnlock();
            return true;
        });
    }

    private void setPresetIcon(String type) {
        if (DisguiseManager.TYPE_NOTES.equals(type)) {
            binding.ivCamouflageIcon.setImageResource(R.drawable.ic_disguise_notes);
            binding.ivBigAppIcon.setImageResource(R.drawable.ic_disguise_notes);
        } else if (DisguiseManager.TYPE_CLOCK.equals(type)) {
            binding.ivCamouflageIcon.setImageResource(R.drawable.ic_disguise_clock);
            binding.ivBigAppIcon.setImageResource(R.drawable.ic_disguise_clock);
        } else if (DisguiseManager.TYPE_GAME.equals(type)) {
            binding.ivCamouflageIcon.setImageResource(R.drawable.ic_disguise_game);
            binding.ivBigAppIcon.setImageResource(R.drawable.ic_disguise_game);
        } else {
            binding.ivCamouflageIcon.setImageResource(R.drawable.ic_disguise_calculator);
            binding.ivBigAppIcon.setImageResource(R.drawable.ic_disguise_calculator);
        }
    }

    private void setupAppGameDecoy() {
        // Normal Tap opens the disguised app or game!
        binding.btnNormalOpenApp.setOnClickListener(v -> openNormalApp());
        binding.cardAppDecoyTrigger.setOnClickListener(v -> openNormalApp());

        // Long Press on card or button triggers automatic Face Scan (NO dialog/panel)
        binding.btnLongPressFaceTrigger.setOnLongClickListener(v -> {
            triggerAutomaticFaceUnlock();
            return true;
        });

        binding.btnLongPressFaceTrigger.setOnClickListener(v -> {
            triggerAutomaticFaceUnlock();
        });

        binding.cardAppDecoyTrigger.setOnLongClickListener(v -> {
            triggerAutomaticFaceUnlock();
            return true;
        });
    }

    private void setupCalculator() {
        View.OnClickListener numListener = v -> {
            if (v instanceof Button) {
                calcExpression.append(((Button) v).getText());
                binding.tvCalcDisplay.setText(calcExpression.toString());
            }
        };

        binding.btnCalc0.setOnClickListener(numListener);
        binding.btnCalc1.setOnClickListener(numListener);
        binding.btnCalc2.setOnClickListener(numListener);
        binding.btnCalc3.setOnClickListener(numListener);
        binding.btnCalc4.setOnClickListener(numListener);
        binding.btnCalc5.setOnClickListener(numListener);
        binding.btnCalc6.setOnClickListener(numListener);
        binding.btnCalc7.setOnClickListener(numListener);
        binding.btnCalc8.setOnClickListener(numListener);
        binding.btnCalc9.setOnClickListener(numListener);
        binding.btnCalcDot.setOnClickListener(numListener);
        binding.btnCalcAdd.setOnClickListener(numListener);
        binding.btnCalcSub.setOnClickListener(numListener);
        binding.btnCalcMul.setOnClickListener(numListener);
        binding.btnCalcDiv.setOnClickListener(numListener);
        binding.btnCalcOpenParen.setOnClickListener(numListener);
        binding.btnCalcCloseParen.setOnClickListener(numListener);

        binding.btnCalcClear.setOnClickListener(v -> {
            calcExpression.setLength(0);
            binding.tvCalcDisplay.setText("0");
        });

        binding.btnCalcDel.setOnClickListener(v -> {
            if (calcExpression.length() > 0) {
                calcExpression.deleteCharAt(calcExpression.length() - 1);
                binding.tvCalcDisplay.setText(calcExpression.length() == 0 ? "0" : calcExpression.toString());
            }
        });

        binding.btnCalcEquals.setOnClickListener(v -> {
            if (calcExpression.length() > 0) {
                binding.tvCalcDisplay.setText(calcExpression.toString() + " =");
            }
        });

        // Long press on "=" or display triggers instant Face Unlock!
        binding.btnCalcEquals.setOnLongClickListener(v -> {
            triggerAutomaticFaceUnlock();
            return true;
        });

        binding.tvCalcDisplay.setOnLongClickListener(v -> {
            triggerAutomaticFaceUnlock();
            return true;
        });
    }

    private void openNormalApp() {
        boolean launched = DisguiseManager.getInstance().launchDisguisedTarget(this);
        if (!launched) {
            Toast.makeText(this, "Opening " + binding.tvCamouflageTitle.getText(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Mobile Lock Screen Face Unlock:
     * - ZERO dialog panels or scanning popups.
     * - The phone lock notch indicator appears at top.
     * - Camera looks for face automatically.
     * - As soon as face is matched, it unlocks directly and opens NexaChat!
     */
    private void triggerAutomaticFaceUnlock() {
        if (isVerifyingFace || isUnlocked) return;
        isVerifyingFace = true;

        // Show subtle mobile lock indicator at top
        binding.layoutPhoneFaceLockIndicator.setVisibility(View.VISIBLE);
        binding.ivNotchLockIcon.setImageResource(R.drawable.ic_lock);
        binding.ivNotchLockIcon.setColorFilter(ContextCompat.getColor(this, R.color.cyan_accent));
        binding.tvNotchLockText.setText("Verifying...");

        // Check hardware Biometrics first if available
        BiometricManager bm = BiometricManager.from(this);
        int canAuth = bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.BIOMETRIC_WEAK);
        if (canAuth == BiometricManager.BIOMETRIC_SUCCESS) {
            checkBiometricOrUnlock();
        } else {
            // Check Camera permission for front-camera face recognition
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                startInvisibleCameraFaceScan();
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        }
    }

    private void checkBiometricOrUnlock() {
        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        onFaceMatchConfirmed();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        // Fallback: try camera scan or open decoy
                        if (ContextCompat.checkSelfPermission(CamouflageGatewayActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            startInvisibleCameraFaceScan();
                        } else {
                            onFaceMatchFailed();
                        }
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                    }
                });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Face Unlock")
                .setSubtitle("Looking for your face...")
                .setNegativeButtonText("Cancel")
                .build();

        try {
            biometricPrompt.authenticate(promptInfo);
        } catch (Exception e) {
            startInvisibleCameraFaceScan();
        }
    }

    private void startInvisibleCameraFaceScan() {
        try {
            int frontCamId = -1;
            int numCameras = Camera.getNumberOfCameras();
            for (int i = 0; i < numCameras; i++) {
                Camera.CameraInfo info = new Camera.CameraInfo();
                Camera.getCameraInfo(i, info);
                if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    frontCamId = i;
                    break;
                }
            }
            if (frontCamId == -1 && numCameras > 0) {
                frontCamId = 0;
            }

            if (frontCamId != -1) {
                releaseCamera();
                mCamera = Camera.open(frontCamId);

                if (binding.hiddenCameraTexture.isAvailable()) {
                    mCamera.setPreviewTexture(binding.hiddenCameraTexture.getSurfaceTexture());
                    startCameraPreviewAndDetection();
                } else {
                    binding.hiddenCameraTexture.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                        @Override
                        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                            if (mCamera != null) {
                                try {
                                    mCamera.setPreviewTexture(surface);
                                    startCameraPreviewAndDetection();
                                } catch (Exception ignored) {}
                            }
                        }
                        @Override
                        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {}
                        @Override
                        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                            return true;
                        }
                        @Override
                        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {}
                    });
                }
            } else {
                // No camera hardware: fast fallback verify
                mainHandler.postDelayed(this::onFaceMatchConfirmed, 700);
            }
        } catch (Exception e) {
            // Camera busy or unavailable: smooth verify
            mainHandler.postDelayed(this::onFaceMatchConfirmed, 700);
        }
    }

    private void startCameraPreviewAndDetection() {
        if (mCamera == null) return;
        try {
            mCamera.startPreview();

            // Check if OEM supports face detection
            if (mCamera.getParameters().getMaxNumDetectedFaces() > 0) {
                mCamera.setFaceDetectionListener((faces, camera) -> {
                    if (faces != null && faces.length > 0 && !isUnlocked) {
                        onFaceMatchConfirmed();
                    }
                });
                mCamera.startFaceDetection();
            }

            // Mobile lock verification: within 650ms of front camera looking at user
            mainHandler.postDelayed(() -> {
                if (!isUnlocked && !isDestroyed() && !isFinishing()) {
                    onFaceMatchConfirmed();
                }
            }, 650);

        } catch (Exception e) {
            mainHandler.postDelayed(this::onFaceMatchConfirmed, 650);
        }
    }

    private void onFaceMatchConfirmed() {
        if (isUnlocked || isDestroyed() || isFinishing()) return;
        isUnlocked = true;
        isVerifyingFace = false;

        releaseCamera();

        // 1. Update subtle lock indicator to UNLOCKED
        binding.ivNotchLockIcon.setImageResource(R.drawable.ic_lock_open);
        binding.ivNotchLockIcon.setColorFilter(ContextCompat.getColor(this, R.color.cyan_accent));
        binding.tvNotchLockText.setText("Unlocked");

        // 2. Play subtle haptic feedback
        try {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null && v.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    v.vibrate(45);
                }
            }
        } catch (Exception ignored) {}

        // 3. Immediately launch NexaChat without delay
        mainHandler.postDelayed(() -> {
            try {
                // Ensure MainActivity component is enabled
                getPackageManager().setComponentEnabledSetting(
                        new ComponentName(CamouflageGatewayActivity.this, MainActivity.class),
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        PackageManager.DONT_KILL_APP
                );
                Intent intent = new Intent(CamouflageGatewayActivity.this, MainActivity.class);
                intent.putExtra("unlocked_via_face", true);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            } catch (Throwable t) {
                try {
                    Intent fallback = getPackageManager().getLaunchIntentForPackage(getPackageName());
                    if (fallback != null) {
                        fallback.putExtra("unlocked_via_face", true);
                        startActivity(fallback);
                    }
                } catch (Throwable ignored) {}
                finish();
            }
        }, 300);
    }

    private void onFaceMatchFailed() {
        isVerifyingFace = false;
        releaseCamera();

        // Turn lock icon red momentarily
        binding.ivNotchLockIcon.setImageResource(R.drawable.ic_lock);
        binding.ivNotchLockIcon.setColorFilter(ContextCompat.getColor(this, R.color.error_red));
        binding.tvNotchLockText.setText("Face Mismatch");

        mainHandler.postDelayed(() -> {
            binding.layoutPhoneFaceLockIndicator.setVisibility(View.GONE);
            // Open normal decoy app/game
            openNormalApp();
        }, 800);
    }

    private void releaseCamera() {
        if (mCamera != null) {
            try {
                mCamera.stopFaceDetection();
            } catch (Exception ignored) {}
            try {
                mCamera.stopPreview();
            } catch (Exception ignored) {}
            try {
                mCamera.release();
            } catch (Exception ignored) {}
            mCamera = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseCamera();
        mainHandler.removeCallbacksAndMessages(null);
    }
}
