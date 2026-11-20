package com.example.quitely.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quitely.R;
import com.example.quitely.database.entities.SilentZone;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.List;

public class SilentZoneAdapter extends RecyclerView.Adapter<SilentZoneAdapter.ZoneViewHolder> {

    public interface OnZoneActionListener {
        void onToggleZone(SilentZone zone, boolean isEnabled);
        void onEditZone(SilentZone zone);
        void onDeleteZone(SilentZone zone);
        void onZoneClicked(SilentZone zone);
        void onSimulateZone(SilentZone zone);
    }

    private final Context context;
    private List<SilentZone> zones = new ArrayList<>();
    private final OnZoneActionListener listener;

    public SilentZoneAdapter(Context context, OnZoneActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setZones(List<SilentZone> zones) {
        this.zones = (zones != null) ? zones : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ZoneViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_silent_zone, parent, false);
        return new ZoneViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ZoneViewHolder holder, int position) {
        SilentZone zone = zones.get(position);

        holder.tvZoneName.setText(zone.getZoneName());
        String categoryText = zone.getZoneType() + " • " + (int) zone.getRadius() + "m radius";
        holder.tvZoneCategory.setText(categoryText);

        // Icon based on zone type
        int iconRes = R.drawable.ic_classroom;
        if ("Library".equalsIgnoreCase(zone.getZoneType())) {
            iconRes = R.drawable.ic_library;
        } else if ("Laboratory".equalsIgnoreCase(zone.getZoneType())) {
            iconRes = R.drawable.ic_laboratory;
        } else if ("Auditorium".equalsIgnoreCase(zone.getZoneType())) {
            iconRes = R.drawable.ic_auditorium;
        } else if ("Exam Hall".equalsIgnoreCase(zone.getZoneType())) {
            iconRes = R.drawable.ic_exam;
        } else if ("Meeting Room".equalsIgnoreCase(zone.getZoneType())) {
            iconRes = R.drawable.ic_meeting;
        }
        holder.ivZoneType.setImageResource(iconRes);

        // Sound mode chip
        holder.chipSoundMode.setText(zone.getSoundModeLabel() + " Mode");
        if (zone.getSoundMode() == 0) { // Silent
            holder.chipSoundMode.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.state_silent_light)));
            holder.chipSoundMode.setTextColor(ContextCompat.getColor(context, R.color.state_silent));
        } else if (zone.getSoundMode() == 1) { // Vibrate
            holder.chipSoundMode.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.state_vibrate_light)));
            holder.chipSoundMode.setTextColor(ContextCompat.getColor(context, R.color.state_vibrate));
        } else { // Normal
            holder.chipSoundMode.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.state_normal_light)));
            holder.chipSoundMode.setTextColor(ContextCompat.getColor(context, R.color.state_normal));
        }

        // Inside vs Outside status
        if (zone.isInside()) {
            holder.chipStatus.setText("● Active Inside");
            holder.chipStatus.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.state_normal_light)));
            holder.chipStatus.setTextColor(ContextCompat.getColor(context, R.color.state_normal));
        } else {
            holder.chipStatus.setText("Outside Zone");
            holder.chipStatus.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.state_disabled_light)));
            holder.chipStatus.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
        }

        // Switch
        holder.switchZoneEnabled.setOnCheckedChangeListener(null);
        holder.switchZoneEnabled.setChecked(zone.isEnabled());
        holder.switchZoneEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) {
                listener.onToggleZone(zone, isChecked);
            }
        });

        // Test simulation
        holder.btnTestZone.setOnClickListener(v -> {
            if (listener != null) listener.onSimulateZone(zone);
        });

        // Edit & Delete
        holder.btnEditZone.setOnClickListener(v -> {
            if (listener != null) listener.onEditZone(zone);
        });

        holder.btnDeleteZone.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteZone(zone);
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onZoneClicked(zone);
        });
    }

    @Override
    public int getItemCount() {
        return zones.size();
    }

    static class ZoneViewHolder extends RecyclerView.ViewHolder {
        ImageView ivZoneType;
        TextView tvZoneName;
        TextView tvZoneCategory;
        SwitchMaterial switchZoneEnabled;
        TextView chipSoundMode;
        TextView chipStatus;
        ImageView btnTestZone;
        ImageView btnEditZone;
        ImageView btnDeleteZone;

        public ZoneViewHolder(@NonNull View itemView) {
            super(itemView);
            ivZoneType = itemView.findViewById(R.id.ivZoneType);
            tvZoneName = itemView.findViewById(R.id.tvZoneName);
            tvZoneCategory = itemView.findViewById(R.id.tvZoneCategory);
            switchZoneEnabled = itemView.findViewById(R.id.switchZoneEnabled);
            chipSoundMode = itemView.findViewById(R.id.chipSoundMode);
            chipStatus = itemView.findViewById(R.id.chipStatus);
            btnTestZone = itemView.findViewById(R.id.btnTestZone);
            btnEditZone = itemView.findViewById(R.id.btnEditZone);
            btnDeleteZone = itemView.findViewById(R.id.btnDeleteZone);
        }
    }
}
