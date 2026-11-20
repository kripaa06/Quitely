package com.example.quitely.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quitely.R;
import com.example.quitely.adapters.HistoryAdapter;
import com.example.quitely.database.AppDatabase;
import com.example.quitely.database.entities.SoundHistory;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

public class HistoryFragment extends Fragment {

    private RecyclerView rvHistory;
    private LinearLayout layoutEmptyHistory;
    private MaterialButton btnClearHistory;
    private ChipGroup chipGroupHistoryFilter;

    private HistoryAdapter adapter;
    private AppDatabase db;
    private List<SoundHistory> fullHistoryList = new ArrayList<>();
    private String currentFilter = "ALL";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        db = AppDatabase.getInstance(requireContext());

        rvHistory = view.findViewById(R.id.rvHistory);
        layoutEmptyHistory = view.findViewById(R.id.layoutEmptyHistory);
        btnClearHistory = view.findViewById(R.id.btnClearHistory);
        chipGroupHistoryFilter = view.findViewById(R.id.chipGroupHistoryFilter);

        rvHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new HistoryAdapter(requireContext());
        rvHistory.setAdapter(adapter);

        setupListeners();
        observeHistory();

        return view;
    }

    private void setupListeners() {
        btnClearHistory.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Clear History")
                    .setMessage("Are you sure you want to clear all activity history logs?")
                    .setPositiveButton("Clear All", (dialog, which) -> {
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            db.soundHistoryDao().clearAll();
                        });
                        Toast.makeText(requireContext(), "History cleared", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        chipGroupHistoryFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int checkedId = checkedIds.get(0);
            if (checkedId == R.id.chipFilterLocation) {
                currentFilter = "LOCATION";
            } else if (checkedId == R.id.chipFilterSchedule) {
                currentFilter = "SCHEDULE";
            } else if (checkedId == R.id.chipFilterManual) {
                currentFilter = "MANUAL";
            } else {
                currentFilter = "ALL";
            }
            applyFilter();
        });
    }

    private void observeHistory() {
        db.soundHistoryDao().getAllHistoryLive().observe(getViewLifecycleOwner(), historyList -> {
            fullHistoryList = (historyList != null) ? historyList : new ArrayList<>();
            applyFilter();
        });
    }

    private void applyFilter() {
        List<SoundHistory> filtered = new ArrayList<>();
        if ("ALL".equals(currentFilter)) {
            filtered.addAll(fullHistoryList);
        } else {
            for (SoundHistory item : fullHistoryList) {
                if (currentFilter.equalsIgnoreCase(item.getTriggerSource())) {
                    filtered.add(item);
                }
            }
        }

        if (filtered.isEmpty()) {
            rvHistory.setVisibility(View.GONE);
            layoutEmptyHistory.setVisibility(View.VISIBLE);
        } else {
            rvHistory.setVisibility(View.VISIBLE);
            layoutEmptyHistory.setVisibility(View.GONE);
            adapter.setHistoryList(filtered);
        }
    }
}
