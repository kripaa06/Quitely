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
import com.example.quitely.database.entities.SoundHistory;

import java.util.ArrayList;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private final Context context;
    private List<SoundHistory> historyList = new ArrayList<>();

    public HistoryAdapter(Context context) {
        this.context = context;
    }

    public void setHistoryList(List<SoundHistory> historyList) {
        this.historyList = (historyList != null) ? historyList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        SoundHistory item = historyList.get(position);

        holder.tvEventType.setText(item.getEventType());
        holder.chipSource.setText(item.getTriggerSource());

        if (item.getZoneOrScheduleName() != null && !item.getZoneOrScheduleName().isEmpty()) {
            holder.tvZoneOrSchedule.setVisibility(View.VISIBLE);
            holder.tvZoneOrSchedule.setText(item.getZoneOrScheduleName());
        } else {
            holder.tvZoneOrSchedule.setVisibility(View.GONE);
        }

        String transition = item.getPreviousMode() + " → " + item.getNewMode();
        holder.tvTransition.setText(transition);

        String timeInfo = item.getFormattedTime() + " • " + item.getFormattedDate();
        holder.tvTimestamp.setText(timeInfo);

        // Styling based on trigger source
        if ("LOCATION".equalsIgnoreCase(item.getTriggerSource())) {
            holder.ivEventIcon.setImageResource(R.drawable.ic_location_pin);
            holder.ivEventIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.primary)));
            holder.ivEventIcon.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.primary_light)));
            holder.chipSource.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.primary_light)));
            holder.chipSource.setTextColor(ContextCompat.getColor(context, R.color.primary));
        } else if ("SCHEDULE".equalsIgnoreCase(item.getTriggerSource())) {
            holder.ivEventIcon.setImageResource(R.drawable.ic_schedule);
            holder.ivEventIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.secondary)));
            holder.ivEventIcon.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.secondary_light)));
            holder.chipSource.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.secondary_light)));
            holder.chipSource.setTextColor(ContextCompat.getColor(context, R.color.secondary));
        } else if ("MANUAL".equalsIgnoreCase(item.getTriggerSource())) {
            holder.ivEventIcon.setImageResource(R.drawable.ic_sound_normal);
            holder.ivEventIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.state_vibrate)));
            holder.ivEventIcon.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.state_vibrate_light)));
            holder.chipSource.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.state_vibrate_light)));
            holder.chipSource.setTextColor(ContextCompat.getColor(context, R.color.state_vibrate));
        } else {
            holder.ivEventIcon.setImageResource(R.drawable.ic_settings);
            holder.ivEventIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.state_normal)));
            holder.ivEventIcon.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.state_normal_light)));
            holder.chipSource.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.state_normal_light)));
            holder.chipSource.setTextColor(ContextCompat.getColor(context, R.color.state_normal));
        }
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        ImageView ivEventIcon;
        TextView tvEventType;
        TextView chipSource;
        TextView tvZoneOrSchedule;
        TextView tvTransition;
        TextView tvTimestamp;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            ivEventIcon = itemView.findViewById(R.id.ivEventIcon);
            tvEventType = itemView.findViewById(R.id.tvEventType);
            chipSource = itemView.findViewById(R.id.chipSource);
            tvZoneOrSchedule = itemView.findViewById(R.id.tvZoneOrSchedule);
            tvTransition = itemView.findViewById(R.id.tvTransition);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
        }
    }
}
