package com.example.quitely.activities;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.quitely.R;
import com.example.quitely.database.AppDatabase;
import com.example.quitely.database.entities.SoundSchedule;
import com.example.quitely.managers.ScheduleManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AddEditScheduleActivity extends AppCompatActivity {

    public static final String EXTRA_SCHEDULE_ID = "extra_schedule_id";

    private TextInputLayout tilScheduleName;
    private TextInputEditText etScheduleName;
    private MaterialCardView cardStartTime, cardEndTime;
    private TextView tvStartTime, tvEndTime;

    private Chip chipMon, chipTue, chipWed, chipThu, chipFri, chipSat, chipSun;
    private MaterialButton btnPresetWeekdays, btnPresetWeekends, btnPresetEveryday;
    private MaterialButtonToggleGroup toggleScheduleSoundMode;
    private MaterialButton btnSaveSchedule, btnCancelSchedule;

    private int startHour = 9, startMinute = 0;
    private int endHour = 17, endMinute = 0;
    private int existingScheduleId = -1;
    private SoundSchedule existingSchedule;

    private AppDatabase db;
    private ScheduleManager scheduleManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_schedule);

        db = AppDatabase.getInstance(this);
        scheduleManager = ScheduleManager.getInstance(this);

        initViews();
        setupToolbar();
        setupListeners();

        existingScheduleId = getIntent().getIntExtra(EXTRA_SCHEDULE_ID, -1);
        if (existingScheduleId != -1) {
            loadExistingSchedule();
        } else {
            toggleScheduleSoundMode.check(R.id.btnScheduleSilent);
            updateTimeViews();
        }
    }

    private void initViews() {
        tilScheduleName = findViewById(R.id.tilScheduleName);
        etScheduleName = findViewById(R.id.etScheduleName);
        cardStartTime = findViewById(R.id.cardStartTime);
        cardEndTime = findViewById(R.id.cardEndTime);
        tvStartTime = findViewById(R.id.tvStartTime);
        tvEndTime = findViewById(R.id.tvEndTime);

        chipMon = findViewById(R.id.chipMon);
        chipTue = findViewById(R.id.chipTue);
        chipWed = findViewById(R.id.chipWed);
        chipThu = findViewById(R.id.chipThu);
        chipFri = findViewById(R.id.chipFri);
        chipSat = findViewById(R.id.chipSat);
        chipSun = findViewById(R.id.chipSun);

        btnPresetWeekdays = findViewById(R.id.btnPresetWeekdays);
        btnPresetWeekends = findViewById(R.id.btnPresetWeekends);
        btnPresetEveryday = findViewById(R.id.btnPresetEveryday);

        toggleScheduleSoundMode = findViewById(R.id.toggleScheduleSoundMode);
        btnSaveSchedule = findViewById(R.id.btnSaveSchedule);
        btnCancelSchedule = findViewById(R.id.btnCancelSchedule);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbarAddSchedule);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupListeners() {
        cardStartTime.setOnClickListener(v -> showTimePicker(true));
        cardEndTime.setOnClickListener(v -> showTimePicker(false));

        btnPresetWeekdays.setOnClickListener(v -> setWeekdaysPreset());
        btnPresetWeekends.setOnClickListener(v -> setWeekendsPreset());
        btnPresetEveryday.setOnClickListener(v -> setEverydayPreset());

        btnSaveSchedule.setOnClickListener(v -> saveSchedule());
        btnCancelSchedule.setOnClickListener(v -> finish());
    }

    private void showTimePicker(boolean isStart) {
        int initialHour = isStart ? startHour : endHour;
        int initialMinute = isStart ? startMinute : endMinute;

        TimePickerDialog picker = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    if (isStart) {
                        startHour = hourOfDay;
                        startMinute = minute;
                    } else {
                        endHour = hourOfDay;
                        endMinute = minute;
                    }
                    updateTimeViews();
                },
                initialHour,
                initialMinute,
                false
        );
        picker.show();
    }

    private void updateTimeViews() {
        tvStartTime.setText(formatTime(startHour, startMinute));
        tvEndTime.setText(formatTime(endHour, endMinute));
    }

    private String formatTime(int hour, int minute) {
        String period = (hour >= 12) ? "PM" : "AM";
        int displayHour = hour % 12;
        if (displayHour == 0) displayHour = 12;
        return String.format(Locale.getDefault(), "%02d:%02d %s", displayHour, minute, period);
    }

    private void setWeekdaysPreset() {
        chipMon.setChecked(true);
        chipTue.setChecked(true);
        chipWed.setChecked(true);
        chipThu.setChecked(true);
        chipFri.setChecked(true);
        chipSat.setChecked(false);
        chipSun.setChecked(false);
    }

    private void setWeekendsPreset() {
        chipMon.setChecked(false);
        chipTue.setChecked(false);
        chipWed.setChecked(false);
        chipThu.setChecked(false);
        chipFri.setChecked(false);
        chipSat.setChecked(true);
        chipSun.setChecked(true);
    }

    private void setEverydayPreset() {
        chipMon.setChecked(true);
        chipTue.setChecked(true);
        chipWed.setChecked(true);
        chipThu.setChecked(true);
        chipFri.setChecked(true);
        chipSat.setChecked(true);
        chipSun.setChecked(true);
    }

    private String getSelectedDaysString() {
        List<String> days = new ArrayList<>();
        // Calendar.SUNDAY = 1, MONDAY = 2, ..., SATURDAY = 7
        if (chipSun.isChecked()) days.add("1");
        if (chipMon.isChecked()) days.add("2");
        if (chipTue.isChecked()) days.add("3");
        if (chipWed.isChecked()) days.add("4");
        if (chipThu.isChecked()) days.add("5");
        if (chipFri.isChecked()) days.add("6");
        if (chipSat.isChecked()) days.add("7");

        return String.join(",", days);
    }

    private void loadExistingSchedule() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Edit Schedule");
        }
        btnSaveSchedule.setText("Update Schedule");

        AppDatabase.databaseWriteExecutor.execute(() -> {
            existingSchedule = db.soundScheduleDao().getScheduleById(existingScheduleId);
            if (existingSchedule != null) {
                runOnUiThread(() -> {
                    etScheduleName.setText(existingSchedule.getScheduleName());
                    startHour = existingSchedule.getStartHour();
                    startMinute = existingSchedule.getStartMinute();
                    endHour = existingSchedule.getEndHour();
                    endMinute = existingSchedule.getEndMinute();
                    updateTimeViews();

                    String days = existingSchedule.getSelectedDays();
                    chipSun.setChecked(days.contains("1"));
                    chipMon.setChecked(days.contains("2"));
                    chipTue.setChecked(days.contains("3"));
                    chipWed.setChecked(days.contains("4"));
                    chipThu.setChecked(days.contains("5"));
                    chipFri.setChecked(days.contains("6"));
                    chipSat.setChecked(days.contains("7"));

                    if (existingSchedule.getSoundMode() == 1) {
                        toggleScheduleSoundMode.check(R.id.btnScheduleVibrate);
                    } else {
                        toggleScheduleSoundMode.check(R.id.btnScheduleSilent);
                    }
                });
            }
        });
    }

    private void saveSchedule() {
        tilScheduleName.setError(null);
        String name = (etScheduleName.getText() != null) ? etScheduleName.getText().toString().trim() : "";
        if (name.isEmpty()) {
            tilScheduleName.setError("Schedule name is required");
            etScheduleName.requestFocus();
            return;
        }

        String selectedDays = getSelectedDaysString();
        if (selectedDays.isEmpty()) {
            Toast.makeText(this, "Please select at least one day for the schedule", Toast.LENGTH_SHORT).show();
            return;
        }

        if (startHour == endHour && startMinute == endMinute) {
            Toast.makeText(this, "Start time and end time cannot be the same", Toast.LENGTH_SHORT).show();
            return;
        }

        int soundMode = (toggleScheduleSoundMode.getCheckedButtonId() == R.id.btnScheduleVibrate) ? 1 : 0;

        AppDatabase.databaseWriteExecutor.execute(() -> {
            SoundSchedule schedule = (existingSchedule != null) ? existingSchedule : new SoundSchedule();
            schedule.setScheduleName(name);
            schedule.setStartHour(startHour);
            schedule.setStartMinute(startMinute);
            schedule.setEndHour(endHour);
            schedule.setEndMinute(endMinute);
            schedule.setSelectedDays(selectedDays);
            schedule.setSoundMode(soundMode);
            schedule.setEnabled(true);

            if (existingScheduleId != -1) {
                db.soundScheduleDao().update(schedule);
                scheduleManager.scheduleAlarmsForSchedule(schedule);
            } else {
                long newId = db.soundScheduleDao().insert(schedule);
                schedule.setId((int) newId);
                scheduleManager.scheduleAlarmsForSchedule(schedule);
            }

            runOnUiThread(() -> {
                Toast.makeText(this, (existingScheduleId != -1) ? "Schedule updated" : "Schedule created", Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }
}
