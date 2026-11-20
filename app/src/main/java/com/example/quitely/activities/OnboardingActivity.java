package com.example.quitely.activities;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.quitely.R;
import com.example.quitely.utils.PreferenceManager;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class OnboardingActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private LinearLayout layoutDots;
    private MaterialButton btnNextOrStart;
    private MaterialButton btnSkip;
    private List<SlideItem> slideItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        viewPager = findViewById(R.id.viewPagerOnboarding);
        layoutDots = findViewById(R.id.layoutDots);
        btnNextOrStart = findViewById(R.id.btnNextOrStart);
        btnSkip = findViewById(R.id.btnSkip);

        setupSlides();
        setupDots(0);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                setupDots(position);
                if (position == slideItems.size() - 1) {
                    btnNextOrStart.setText("Get Started");
                    btnSkip.setVisibility(View.GONE);
                } else {
                    btnNextOrStart.setText("Next");
                    btnSkip.setVisibility(View.VISIBLE);
                }
            }
        });

        btnNextOrStart.setOnClickListener(v -> {
            int current = viewPager.getCurrentItem();
            if (current < slideItems.size() - 1) {
                viewPager.setCurrentItem(current + 1);
            } else {
                finishOnboarding();
            }
        });

        btnSkip.setOnClickListener(v -> finishOnboarding());
    }

    private void setupSlides() {
        slideItems = new ArrayList<>();
        slideItems.add(new SlideItem(
                R.drawable.ic_location_pin,
                "Automatic Silent Zones",
                "Quitely automatically detects when you enter your registered classrooms, lecture halls, or libraries and activates Silent Mode automatically."
        ));
        slideItems.add(new SlideItem(
                R.drawable.ic_sound_normal,
                "Smart Sound Restoration",
                "Leaving the classroom? Quitely remembers the sound mode you had before and safely restores it as soon as no active rules require silence."
        ));
        slideItems.add(new SlideItem(
                R.drawable.ic_schedule,
                "Scheduled Sound Automation",
                "Configure lecture hours, study sessions, and quiet periods with recurring weekly schedules that run seamlessly in the background."
        ));

        viewPager.setAdapter(new OnboardingAdapter(slideItems));
    }

    private void setupDots(int activeIndex) {
        layoutDots.removeAllViews();
        int dotSize = (int) (8 * getResources().getDisplayMetrics().density);
        int activeWidth = (int) (24 * getResources().getDisplayMetrics().density);
        int margin = (int) (4 * getResources().getDisplayMetrics().density);

        for (int i = 0; i < slideItems.size(); i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams params;
            if (i == activeIndex) {
                params = new LinearLayout.LayoutParams(activeWidth, dotSize);
                dot.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary)));
            } else {
                params = new LinearLayout.LayoutParams(dotSize, dotSize);
                dot.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.card_stroke)));
            }
            params.setMargins(margin, 0, margin, 0);
            dot.setLayoutParams(params);
            dot.setBackgroundResource(R.drawable.bg_onboarding_dot);
            layoutDots.addView(dot);
        }
    }

    private void finishOnboarding() {
        PreferenceManager.getInstance(this).setOnboardingCompleted(true);
        Intent intent = new Intent(this, WelcomeActivity.class);
        startActivity(intent);
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private static class SlideItem {
        int iconRes;
        String title;
        String description;

        SlideItem(int iconRes, String title, String description) {
            this.iconRes = iconRes;
            this.title = title;
            this.description = description;
        }
    }

    private static class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.SlideViewHolder> {
        private final List<SlideItem> items;

        OnboardingAdapter(List<SlideItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public SlideViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_onboarding_slide, parent, false);
            return new SlideViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SlideViewHolder holder, int position) {
            SlideItem item = items.get(position);
            holder.ivIcon.setImageResource(item.iconRes);
            holder.tvTitle.setText(item.title);
            holder.tvDescription.setText(item.description);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class SlideViewHolder extends RecyclerView.ViewHolder {
            ImageView ivIcon;
            TextView tvTitle;
            TextView tvDescription;

            SlideViewHolder(@NonNull View itemView) {
                super(itemView);
                ivIcon = itemView.findViewById(R.id.ivSlideIcon);
                tvTitle = itemView.findViewById(R.id.tvSlideTitle);
                tvDescription = itemView.findViewById(R.id.tvSlideDescription);
            }
        }
    }
}
