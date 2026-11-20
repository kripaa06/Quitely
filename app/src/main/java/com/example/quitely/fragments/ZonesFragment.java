package com.example.quitely.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quitely.R;
import com.example.quitely.activities.AddEditZoneActivity;
import com.example.quitely.adapters.SilentZoneAdapter;
import com.example.quitely.database.AppDatabase;
import com.example.quitely.database.entities.SilentZone;
import com.example.quitely.managers.GeofenceManager;
import com.example.quitely.managers.SoundAutomationManager;
import com.example.quitely.utils.PreferenceManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.List;
import java.util.Set;

public class ZonesFragment extends Fragment implements SilentZoneAdapter.OnZoneActionListener {

    private RecyclerView rvZones;
    private LinearLayout layoutEmptyZones;
    private TextView chipTotalZones;
    private TextView chipActiveGeofences;
    private MaterialButton btnEmptyAddZone;
    private ExtendedFloatingActionButton fabAddZone;

    private SilentZoneAdapter adapter;
    private AppDatabase db;
    private GeofenceManager geofenceManager;
    private PreferenceManager prefManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_zones, container, false);

        db = AppDatabase.getInstance(requireContext());
        geofenceManager = GeofenceManager.getInstance(requireContext());
        prefManager = PreferenceManager.getInstance(requireContext());

        rvZones = view.findViewById(R.id.rvZones);
        layoutEmptyZones = view.findViewById(R.id.layoutEmptyZones);
        chipTotalZones = view.findViewById(R.id.chipTotalZones);
        chipActiveGeofences = view.findViewById(R.id.chipActiveGeofences);
        btnEmptyAddZone = view.findViewById(R.id.btnEmptyAddZone);
        fabAddZone = view.findViewById(R.id.fabAddZone);

        rvZones.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new SilentZoneAdapter(requireContext(), this);
        rvZones.setAdapter(adapter);

        View.OnClickListener addZoneListener = v -> {
            Intent intent = new Intent(requireContext(), AddEditZoneActivity.class);
            startActivity(intent);
        };

        fabAddZone.setOnClickListener(addZoneListener);
        btnEmptyAddZone.setOnClickListener(addZoneListener);

        observeZones();

        return view;
    }

    private void observeZones() {
        db.silentZoneDao().getAllZonesLive().observe(getViewLifecycleOwner(), zones -> {
            if (zones == null || zones.isEmpty()) {
                rvZones.setVisibility(View.GONE);
                layoutEmptyZones.setVisibility(View.VISIBLE);
                chipTotalZones.setText("0 Zones Registered");
                chipActiveGeofences.setText("0 Active");
            } else {
                rvZones.setVisibility(View.VISIBLE);
                layoutEmptyZones.setVisibility(View.GONE);

                Set<String> activeZoneIds = prefManager.getActiveZones();
                int enabledCount = 0;
                for (SilentZone z : zones) {
                    if (z.isEnabled()) enabledCount++;
                    z.setInside(activeZoneIds.contains(String.valueOf(z.getId())));
                }

                chipTotalZones.setText(zones.size() + " Zones Registered");
                chipActiveGeofences.setText(enabledCount + " Active");
                adapter.setZones(zones);
            }
        });
    }

    @Override
    public void onToggleZone(SilentZone zone, boolean isEnabled) {
        zone.setEnabled(isEnabled);
        AppDatabase.databaseWriteExecutor.execute(() -> {
            db.silentZoneDao().update(zone);
            if (isEnabled) {
                geofenceManager.addGeofence(zone, null);
            } else {
                geofenceManager.removeGeofence(zone.getId(), null);
                if (prefManager.getActiveZones().contains(String.valueOf(zone.getId()))) {
                    com.example.quitely.managers.SoundAutomationManager.getInstance(requireContext()).onGeofenceExit(zone.getId());
                }
            }
        });
        Toast.makeText(requireContext(), isEnabled ? "Zone enabled & geofence registered" : "Zone disabled", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onEditZone(SilentZone zone) {
        Intent intent = new Intent(requireContext(), AddEditZoneActivity.class);
        intent.putExtra(AddEditZoneActivity.EXTRA_ZONE_ID, zone.getId());
        startActivity(intent);
    }

    @Override
    public void onDeleteZone(SilentZone zone) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Silent Zone")
                .setMessage("Are you sure you want to delete \"" + zone.getZoneName() + "\"? The associated geofence will be removed.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        geofenceManager.removeGeofence(zone.getId(), null);
                        if (prefManager.getActiveZones().contains(String.valueOf(zone.getId()))) {
                            com.example.quitely.managers.SoundAutomationManager.getInstance(requireContext()).onGeofenceExit(zone.getId());
                        }
                        db.silentZoneDao().delete(zone);
                    });
                    Toast.makeText(requireContext(), "Zone deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onZoneClicked(SilentZone zone) {
        onEditZone(zone);
    }

    @Override
    public void onSimulateZone(SilentZone zone) {
        boolean willEnter = !zone.isInside();
        SoundAutomationManager.getInstance(requireContext()).simulateZoneTransition(zone.getId(), willEnter);
        String msg = willEnter
                ? "Simulated ENTRY into: " + zone.getZoneName() + " (" + zone.getSoundModeLabel() + ")"
                : "Simulated EXIT from: " + zone.getZoneName();
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    }
}
