package com.financemanager.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.financemanager.app.FinanceManagerApp;
import com.financemanager.app.R;
import com.financemanager.app.databinding.ActivityMainBinding;
import com.financemanager.app.di.AppContainer;
import com.financemanager.app.ui.common.BottomNavDock;
import com.financemanager.app.ui.common.SwipeNavigationLayout;
import com.financemanager.app.ui.lock.LockActivity;
import com.financemanager.app.utils.LanguageHelper;
import com.financemanager.app.workers.WorkScheduler;

/** Single-activity shell hosting every screen plus the app-lock gate. */
public class MainActivity extends AppCompatActivity {

    private static final int[] TOP_LEVEL = {
            R.id.home, R.id.transactions, R.id.budgets, R.id.reports, R.id.more
    };

    private ActivityMainBinding binding;
    private AppContainer container;
    private NavController navController;
    private boolean lockRequested;
    private long lastSwipeAt;

    private final ActivityResultLauncher<Intent> lockLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                lockRequested = false;
                if (result.getResultCode() == RESULT_OK) {
                    revealContent();
                } else {
                    finishAffinity();
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        container = ((FinanceManagerApp) getApplication()).container();
        super.onCreate(savedInstanceState);
        LanguageHelper.syncDefaultLocale();
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applyScreenshotPolicy();
        applyWindowInsets();
        setUpNavigation();
        binding.getRoot().setVisibility(View.INVISIBLE);

        // Seed after super.onCreate so default categories use the selected app language.
        container.repository().seedIfNeeded(this, container.settings().getCurrency(), () -> {
            container.settings().markSeeded();
            WorkScheduler.scheduleInitialWork(this);
        });
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }

    private void setUpNavigation() {
        NavHostFragment host = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (host == null) {
            return;
        }
        navController = host.getNavController();
        NavigationUI.setupWithNavController(binding.bottomNavigation, navController);
        // Labels are always visible, so suppress the long-press tooltip bubbles.
        for (int i = 0; i < binding.bottomNavigation.getMenu().size(); i++) {
            MenuItem item = binding.bottomNavigation.getMenu().getItem(i);
            item.setTooltipText(null);
            View itemView = binding.bottomNavigation.findViewById(item.getItemId());
            if (itemView != null) {
                itemView.setTooltipText(null);
                // Consume long presses so the system never shows a tooltip bubble.
                itemView.setOnLongClickListener(v -> true);
            }
        }
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            boolean topLevel = isTopLevel(destination.getId());
            binding.bottomNavDock.setVisibility(topLevel ? View.VISIBLE : View.GONE);
        });

        binding.navHostContainer.setListener(new SwipeNavigationLayout.Listener() {
            @Override
            public boolean canSwipe() {
                if (navController == null || binding.getRoot().getVisibility() != View.VISIBLE
                        || binding.bottomNavDock.getVisibility() != View.VISIBLE) {
                    return false;
                }
                NavDestination destination = navController.getCurrentDestination();
                return destination != null && isTopLevel(destination.getId())
                        && isTopLevel(binding.bottomNavigation.getSelectedItemId());
            }

            @Override
            public void onSwipe(boolean next) {
                long now = SystemClock.elapsedRealtime();
                if (now - lastSwipeAt < 500) {
                    return;
                }
                int index = indexOf(binding.bottomNavigation.getSelectedItemId());
                if (index < 0) {
                    return;
                }
                int target = index + (next ? 1 : -1);
                if (target < 0 || target >= TOP_LEVEL.length) {
                    return;
                }
                lastSwipeAt = now;
                navigateTopLevel(TOP_LEVEL[target]);
            }
        });

        binding.bottomNavDock.setListener(new BottomNavDock.Listener() {
            @Override
            public boolean canDrag() {
                return binding.bottomNavDock.getVisibility() == View.VISIBLE;
            }

            @Override
            public int indexAt(float rawX) {
                return nearestTab(rawX);
            }

            @Override
            public void onDragTo(int index) {
                int selected = indexOf(binding.bottomNavigation.getSelectedItemId());
                if (index >= 0 && index != selected) {
                    navigateTopLevel(TOP_LEVEL[index]);
                }
            }
        });
    }

    private int nearestTab(float rawX) {
        int best = -1;
        float bestDistance = Float.MAX_VALUE;
        for (int i = 0; i < TOP_LEVEL.length; i++) {
            View item = binding.bottomNavigation.findViewById(TOP_LEVEL[i]);
            if (item == null || item.getVisibility() != View.VISIBLE) {
                continue;
            }
            int[] location = new int[2];
            item.getLocationInWindow(location);
            float distance = Math.abs(location[0] + item.getWidth() / 2f - rawX);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = i;
            }
        }
        return best;
    }

    private static boolean isTopLevel(int destinationId) {
        return indexOf(destinationId) >= 0;
    }

    private static int indexOf(int destinationId) {
        for (int i = 0; i < TOP_LEVEL.length; i++) {
            if (TOP_LEVEL[i] == destinationId) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void onStart() {
        super.onStart();
        applyScreenshotPolicy();
        if (container.sessionManager().requiresUnlock(container.settings())) {
            requestUnlock();
        } else {
            revealContent();
        }
    }

    private void requestUnlock() {
        if (lockRequested) {
            return;
        }
        lockRequested = true;
        lockLauncher.launch(new Intent(this, LockActivity.class));
    }

    private void revealContent() {
        binding.getRoot().setVisibility(View.VISIBLE);
    }

    private void applyScreenshotPolicy() {
        if (container.settings().isBlockScreenshots()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
    }

    /** Switches to one of the five bottom navigation destinations. */
    public void navigateTopLevel(int destinationId) {
        binding.bottomNavigation.setSelectedItemId(destinationId);
    }

    @Override
    public boolean onSupportNavigateUp() {
        return (navController != null && navController.navigateUp())
                || super.onSupportNavigateUp();
    }
}
