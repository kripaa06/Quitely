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
import com.example.quitely.activities.AddEditScheduleActivity;
import com.example.quitely.adapters.ScheduleAdapter;
import com.example.quitely.database.AppDatabase;
import com.example.quitely.database.entities.SoundSchedule;
import com.example.quitely.managers.ScheduleManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

public class SchedulesFragment extends Fragment implements ScheduleAdapter.OnScheduleActionListener {

    private RecyclerView rvSchedules;
    private LinearLayout layoutEmptySchedules;
    private TextView chipTotalSchedules;
    private TextView chipActiveSchedules;
    private MaterialButton btnEmptyAddSchedule;
    private ExtendedFloatingActionButton fabAddSchedule;

    private ScheduleAdapter adapter;
    private AppDatabase db;
    private ScheduleManager scheduleManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_schedules, container, false);

        db = AppDatabase.getInstance(requireContext());
        scheduleManager = ScheduleManager.getInstance(requireContext());

        rvSchedules = view.findViewById(R.id.rvSchedules);
        layoutEmptySchedules = view.findViewById(R.id.layoutEmptySchedules);
        chipTotalSchedules = view.findViewById(R.id.chipTotalSchedules);
        chipActiveSchedules = view.findViewById(R.id.chipActiveSchedules);
        btnEmptyAddSchedule = view.findViewById(R.id.btnEmptyAddSchedule);
        fabAddSchedule = view.findViewById(R.id.fabAddSchedule);

        rvSchedules.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ScheduleAdapter(requireContext(), this);
        rvSchedules.setAdapter(adapter);

        View.OnClickListener addListener = v -> {
            Intent intent = new Intent(requireContext(), AddEditScheduleActivity.class);
            startActivity(intent);
        };

        fabAddSchedule.setOnClickListener(addListener);
        btnEmptyAddSchedule.setOnClickListener(addListener);

        observeSchedules();

        return view;
    }

    private void observeSchedules() {
        db.soundScheduleDao().getAllSchedulesLive().observe(getViewLifecycleOwner(), schedules -> {
            if (schedules == null || schedules.isEmpty()) {
                rvSchedules.setVisibility(View.GONE);
                layoutEmptySchedules.setVisibility(View.VISIBLE);
                chipTotalSchedules.setText("0 Schedules");
                chipActiveSchedules.setText("0 Active");
            } else {
                rvSchedules.setVisibility(View.VISIBLE);
                layoutEmptySchedules.setVisibility(View.GONE);

                int enabledCount = 0;
                for (SoundSchedule s : schedules) {
                    if (s.isEnabled()) enabledCount++;
                }

                chipTotalSchedules.setText(schedules.size() + " Schedules");
                chipActiveSchedules.setText(enabledCount + " Active");
                adapter.setSchedules(schedules);
            }
        });
    }

    @Override
    public void onToggleSchedule(SoundSchedule schedule, boolean isEnabled) {
        schedule.setEnabled(isEnabled);
        AppDatabase.databaseWriteExecutor.execute(() -> {
            db.soundScheduleDao().update(schedule);
            if (isEnabled) {
                scheduleManager.scheduleAlarmsForSchedule(schedule);
            } else {
                scheduleManager.cancelAlarmsForSchedule(schedule.getId());
            }
        });
        Toast.makeText(requireContext(), isEnabled ? "Schedule enabled" : "Schedule disabled", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onEditSchedule(SoundSchedule schedule) {
        Intent intent = new Intent(requireContext(), AddEditScheduleActivity.class);
        intent.putExtra(AddEditScheduleActivity.EXTRA_SCHEDULE_ID, schedule.getId());
        startActivity(intent);
    }

    @Override
    public void onDeleteSchedule(SoundSchedule schedule) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Schedule")
                .setMessage("Are you sure you want to delete \"" + schedule.getScheduleName() + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        scheduleManager.cancelAlarmsForSchedule(schedule.getId());
                        db.soundScheduleDao().delete(schedule);
                    });
                    Toast.makeText(requireContext(), "Schedule deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
