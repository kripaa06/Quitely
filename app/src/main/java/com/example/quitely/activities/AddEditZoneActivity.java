package com.example.quitely.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import com.example.quitely.R;
import com.example.quitely.database.AppDatabase;
import com.example.quitely.database.entities.SilentZone;
import com.example.quitely.managers.GeofenceManager;
import com.example.quitely.utils.PermissionManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Locale;

public class AddEditZoneActivity extends AppCompatActivity {

    public static final String EXTRA_ZONE_ID = "extra_zone_id";
    private static final int REQUEST_LOCATION_FOR_GPS = 201;

    private TextInputLayout tilZoneName, tilLatitude, tilLongitude;
    private TextInputEditText etZoneName, etLatitude, etLongitude;
    private ChipGroup chipGroupCategory;
    private Slider sliderRadius;
    private TextView tvRadiusValue, tvRadarSummary;
    private android.view.View viewRadarMiddle;
    private MaterialButtonToggleGroup toggleSoundMode;
    private SwitchMaterial switchEntryNotif, switchExitRestoration;
    private MaterialButton btnSaveZone, btnCancelZone, btnUseCurrentLocation;
    private MaterialButton btnPreset50m, btnPreset100m, btnPreset200m, btnPreset500m;

    private int existingZoneId = -1;
    private SilentZone existingZone;
    private AppDatabase db;
    private GeofenceManager geofenceManager;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_zone);

        db = AppDatabase.getInstance(this);
        geofenceManager = GeofenceManager.getInstance(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initViews();
        setupToolbar();
        setupListeners();

        existingZoneId = getIntent().getIntExtra(EXTRA_ZONE_ID, -1);
        if (existingZoneId != -1) {
            loadExistingZone();
        } else {
            // Default select Silent mode
            toggleSoundMode.check(R.id.btnSelectSilent);
        }
    }

    private void initViews() {
        tilZoneName = findViewById(R.id.tilZoneName);
        tilLatitude = findViewById(R.id.tilLatitude);
        tilLongitude = findViewById(R.id.tilLongitude);
        etZoneName = findViewById(R.id.etZoneName);
        etLatitude = findViewById(R.id.etLatitude);
        etLongitude = findViewById(R.id.etLongitude);
        chipGroupCategory = findViewById(R.id.chipGroupCategory);
        sliderRadius = findViewById(R.id.sliderRadius);
        tvRadiusValue = findViewById(R.id.tvRadiusValue);
        tvRadarSummary = findViewById(R.id.tvRadarSummary);
        viewRadarMiddle = findViewById(R.id.viewRadarMiddle);
        toggleSoundMode = findViewById(R.id.toggleSoundMode);
        switchEntryNotif = findViewById(R.id.switchEntryNotif);
        switchExitRestoration = findViewById(R.id.switchExitRestoration);
        btnSaveZone = findViewById(R.id.btnSaveZone);
        btnCancelZone = findViewById(R.id.btnCancelZone);
        btnUseCurrentLocation = findViewById(R.id.btnUseCurrentLocation);

        btnPreset50m = findViewById(R.id.btnPreset50m);
        btnPreset100m = findViewById(R.id.btnPreset100m);
        btnPreset200m = findViewById(R.id.btnPreset200m);
        btnPreset500m = findViewById(R.id.btnPreset500m);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbarAddZone);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupListeners() {
        sliderRadius.addOnChangeListener((slider, value, fromUser) -> {
            updateRadiusText((int) value);
        });

        btnPreset50m.setOnClickListener(v -> setRadius(50));
        btnPreset100m.setOnClickListener(v -> setRadius(100));
        btnPreset200m.setOnClickListener(v -> setRadius(200));
        btnPreset500m.setOnClickListener(v -> setRadius(500));

        btnUseCurrentLocation.setOnClickListener(v -> fetchCurrentLocation());
        btnSaveZone.setOnClickListener(v -> saveZone());
        btnCancelZone.setOnClickListener(v -> finish());
    }

    private void setRadius(int meters) {
        sliderRadius.setValue((float) meters);
        updateRadiusText(meters);
    }

    private void updateRadiusText(int meters) {
        tvRadiusValue.setText(meters + "m");
        tvRadarSummary.setText("Radius: " + meters + " meters • Active trigger zone");
        if (viewRadarMiddle != null) {
            float fraction = Math.min(1.0f, Math.max(0.25f, meters / 500.0f));
            int sizeDp = (int) (55 + (95 * fraction));
            int sizePx = (int) (sizeDp * getResources().getDisplayMetrics().density);
            android.view.ViewGroup.LayoutParams params = viewRadarMiddle.getLayoutParams();
            if (params != null) {
                params.width = sizePx;
                params.height = sizePx;
                viewRadarMiddle.setLayoutParams(params);
            }
        }
    }

    private void fetchCurrentLocation() {
        if (!PermissionManager.hasLocationPermission(this)) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_LOCATION_FOR_GPS
            );
            return;
        }

        requestGpsCoordinates();
    }

    @SuppressLint("MissingPermission")
    private void requestGpsCoordinates() {
        Toast.makeText(this, "Acquiring GPS location...", Toast.LENGTH_SHORT).show();
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        applyCoordinates(location);
                    } else {
                        // Fallback to last known location
                        fusedLocationClient.getLastLocation().addOnSuccessListener(this, lastLocation -> {
                            if (lastLocation != null) {
                                applyCoordinates(lastLocation);
                            } else {
                                Toast.makeText(this, "Unable to get current GPS fix. Please enter manually or try outdoors.", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Location error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void applyCoordinates(Location location) {
        etLatitude.setText(String.format(Locale.US, "%.6f", location.getLatitude()));
        etLongitude.setText(String.format(Locale.US, "%.6f", location.getLongitude()));
        Toast.makeText(this, "Location coordinates populated", Toast.LENGTH_SHORT).show();
    }

    private void loadExistingZone() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Edit Silent Zone");
        }
        btnSaveZone.setText("Update Silent Zone");

        AppDatabase.databaseWriteExecutor.execute(() -> {
            existingZone = db.silentZoneDao().getZoneById(existingZoneId);
            if (existingZone != null) {
                runOnUiThread(() -> {
                    etZoneName.setText(existingZone.getZoneName());
                    etLatitude.setText(String.valueOf(existingZone.getLatitude()));
                    etLongitude.setText(String.valueOf(existingZone.getLongitude()));
                    sliderRadius.setValue(existingZone.getRadius());
                    updateRadiusText((int) existingZone.getRadius());

                    selectCategoryChip(existingZone.getZoneType());

                    if (existingZone.getSoundMode() == 1) {
                        toggleSoundMode.check(R.id.btnSelectVibrate);
                    } else {
                        toggleSoundMode.check(R.id.btnSelectSilent);
                    }

                    switchEntryNotif.setChecked(existingZone.isEntryNotification());
                    switchExitRestoration.setChecked(existingZone.isExitRestoration());
                });
            }
        });
    }

    private void selectCategoryChip(String category) {
        if ("Library".equalsIgnoreCase(category)) {
            chipGroupCategory.check(R.id.chipLibrary);
        } else if ("Laboratory".equalsIgnoreCase(category)) {
            chipGroupCategory.check(R.id.chipLab);
        } else if ("Exam Hall".equalsIgnoreCase(category)) {
            chipGroupCategory.check(R.id.chipExam);
        } else if ("Auditorium".equalsIgnoreCase(category)) {
            chipGroupCategory.check(R.id.chipAuditorium);
        } else if ("Meeting Room".equalsIgnoreCase(category)) {
            chipGroupCategory.check(R.id.chipMeeting);
        } else if ("Other".equalsIgnoreCase(category)) {
            chipGroupCategory.check(R.id.chipOther);
        } else {
            chipGroupCategory.check(R.id.chipClassroom);
        }
    }

    private String getSelectedCategory() {
        int selectedId = chipGroupCategory.getCheckedChipId();
        if (selectedId != -1) {
            Chip chip = findViewById(selectedId);
            if (chip != null) {
                return chip.getText().toString();
            }
        }
        return "Classroom";
    }

    private void saveZone() {
        tilZoneName.setError(null);
        tilLatitude.setError(null);
        tilLongitude.setError(null);

        String name = (etZoneName.getText() != null) ? etZoneName.getText().toString().trim() : "";
        if (name.isEmpty()) {
            tilZoneName.setError("Zone name is required");
            etZoneName.requestFocus();
            return;
        }

        String latStr = (etLatitude.getText() != null) ? etLatitude.getText().toString().trim() : "";
        String lngStr = (etLongitude.getText() != null) ? etLongitude.getText().toString().trim() : "";

        double latitude, longitude;
        try {
            latitude = Double.parseDouble(latStr);
            if (latitude < -90 || latitude > 90) {
                tilLatitude.setError("Latitude must be between -90 and 90");
                return;
            }
        } catch (NumberFormatException e) {
            tilLatitude.setError("Enter valid decimal latitude");
            return;
        }

        try {
            longitude = Double.parseDouble(lngStr);
            if (longitude < -180 || longitude > 180) {
                tilLongitude.setError("Longitude must be between -180 and 180");
                return;
            }
        } catch (NumberFormatException e) {
            tilLongitude.setError("Enter valid decimal longitude");
            return;
        }

        float radius = sliderRadius.getValue();
        int soundMode = (toggleSoundMode.getCheckedButtonId() == R.id.btnSelectVibrate) ? 1 : 0;
        String category = getSelectedCategory();
        boolean entryNotif = switchEntryNotif.isChecked();
        boolean exitRestore = switchExitRestoration.isChecked();

        AppDatabase.databaseWriteExecutor.execute(() -> {
            SilentZone zone = (existingZone != null) ? existingZone : new SilentZone();
            zone.setZoneName(name);
            zone.setZoneType(category);
            zone.setLatitude(latitude);
            zone.setLongitude(longitude);
            zone.setRadius(radius);
            zone.setSoundMode(soundMode);
            zone.setEntryNotification(entryNotif);
            zone.setExitRestoration(exitRestore);
            zone.setEnabled(true);

            if (existingZoneId != -1) {
                db.silentZoneDao().update(zone);
                geofenceManager.addGeofence(zone, null);
            } else {
                long newId = db.silentZoneDao().insert(zone);
                zone.setId((int) newId);
                geofenceManager.addGeofence(zone, null);
            }

            runOnUiThread(() -> {
                Toast.makeText(this, (existingZoneId != -1) ? "Silent Zone updated" : "Silent Zone registered", Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_FOR_GPS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestGpsCoordinates();
            } else {
                Toast.makeText(this, "Location permission is needed to auto-detect GPS coordinates", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
