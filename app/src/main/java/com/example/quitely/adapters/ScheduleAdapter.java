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
import com.example.quitely.database.entities.SoundSchedule;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.List;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder> {

    public interface OnScheduleActionListener {
        void onToggleSchedule(SoundSchedule schedule, boolean isEnabled);
        void onEditSchedule(SoundSchedule schedule);
        void onDeleteSchedule(SoundSchedule schedule);
    }

    private final Context context;
    private List<SoundSchedule> schedules = new ArrayList<>();
    private final OnScheduleActionListener listener;

    public ScheduleAdapter(Context context, OnScheduleActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setSchedules(List<SoundSchedule> schedules) {
        this.schedules = (schedules != null) ? schedules : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ScheduleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_schedule, parent, false);
        return new ScheduleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ScheduleViewHolder holder, int position) {
        SoundSchedule schedule = schedules.get(position);

        holder.tvScheduleName.setText(schedule.getScheduleName());
        String timeString = schedule.getFormattedStartTime() + " – " + schedule.getFormattedEndTime();
        holder.tvScheduleTime.setText(timeString);
        holder.chipDays.setText(schedule.getDaysDisplayString());

        // Mode chip
        holder.chipScheduleSoundMode.setText(schedule.getSoundModeLabel() + " Mode");
        if (schedule.getSoundMode() == 0) { // Silent
            holder.chipScheduleSoundMode.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.state_silent_light)));
            holder.chipScheduleSoundMode.setTextColor(ContextCompat.getColor(context, R.color.state_silent));
        } else { // Vibrate
            holder.chipScheduleSoundMode.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.state_vibrate_light)));
            holder.chipScheduleSoundMode.setTextColor(ContextCompat.getColor(context, R.color.state_vibrate));
        }

        // Toggle
        holder.switchScheduleEnabled.setOnCheckedChangeListener(null);
        holder.switchScheduleEnabled.setChecked(schedule.isEnabled());
        holder.switchScheduleEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) {
                listener.onToggleSchedule(schedule, isChecked);
            }
        });

        // Edit & Delete
        holder.btnEditSchedule.setOnClickListener(v -> {
            if (listener != null) listener.onEditSchedule(schedule);
        });

        holder.btnDeleteSchedule.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteSchedule(schedule);
        });
    }

    @Override
    public int getItemCount() {
        return schedules.size();
    }

    static class ScheduleViewHolder extends RecyclerView.ViewHolder {
        TextView tvScheduleName;
        TextView tvScheduleTime;
        TextView chipDays;
        TextView chipScheduleSoundMode;
        SwitchMaterial switchScheduleEnabled;
        ImageView btnEditSchedule;
        ImageView btnDeleteSchedule;

        public ScheduleViewHolder(@NonNull View itemView) {
            super(itemView);
            tvScheduleName = itemView.findViewById(R.id.tvScheduleName);
            tvScheduleTime = itemView.findViewById(R.id.tvScheduleTime);
            chipDays = itemView.findViewById(R.id.chipDays);
            chipScheduleSoundMode = itemView.findViewById(R.id.chipScheduleSoundMode);
            switchScheduleEnabled = itemView.findViewById(R.id.switchScheduleEnabled);
            btnEditSchedule = itemView.findViewById(R.id.btnEditSchedule);
            btnDeleteSchedule = itemView.findViewById(R.id.btnDeleteSchedule);
        }
    }
}
