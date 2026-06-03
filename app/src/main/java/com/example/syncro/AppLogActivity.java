package com.example.syncro;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.syncro.utils.AppLogger;
import com.example.syncro.utils.PasswordHelper;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class AppLogActivity extends AppCompatActivity {

    // UI
    private RecyclerView recyclerView;
    private LogAdapter adapter;
    private TextView tvLogCount, tvAutoScroll, tvClearLogs, emptyState;
    private Chip chipAll, chipDebug, chipInfo, chipWarn, chipError;
    private TextInputEditText searchInput;

    // Estado
    private AppLogger.Level currentFilter = null; // null = ALL
    private String currentSearch = "";
    private boolean autoScroll = true;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Listener de nuevos logs en tiempo real
    private final AppLogger.OnNewLogListener logListener = entry -> mainHandler.post(this::refreshLogs);

    // -------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_log);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        bindViews();
        setupToolbar();
        setupRecyclerView();
        setupChips();
        setupSearch();
        setupActions();

        // 👇 Pedir contraseña antes de mostrar nada
        showPasswordDialog();

        refreshLogs();

        // Registrar listener para actualizaciones en tiempo real
        AppLogger.getInstance().addListener(logListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AppLogger.getInstance().removeListener(logListener);
    }

    // -------------------------------------------------------------------------
    // Setup

    private void bindViews() {
        recyclerView   = findViewById(R.id.recyclerViewLogs);
        tvLogCount     = findViewById(R.id.tvLogCount);
        tvAutoScroll   = findViewById(R.id.tvAutoScroll);
        tvClearLogs    = findViewById(R.id.tvClearLogs);
        chipAll        = findViewById(R.id.chipAll);
        chipDebug      = findViewById(R.id.chipDebug);
        chipInfo       = findViewById(R.id.chipInfo);
        chipWarn       = findViewById(R.id.chipWarn);
        chipError      = findViewById(R.id.chipError);
        searchInput    = findViewById(R.id.searchInput);
        emptyState     = (TextView) ((ViewGroup) recyclerView.getParent()).findViewWithTag("emptyState");
        // Nota: si emptyState es un LinearLayout usa: View emptyState = ...
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new LogAdapter(new ArrayList<>());
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        // Deshabilitar auto-scroll si el usuario hace scroll hacia arriba
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView rv, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    autoScroll = false;
                    tvAutoScroll.setText("● Auto-scroll OFF");
                    tvAutoScroll.setTextColor(Color.parseColor("#8B949E"));
                }
            }
        });
    }

    private void setupChips() {
        chipAll.setOnClickListener(v   -> setFilter(null));
        chipDebug.setOnClickListener(v -> setFilter(AppLogger.Level.DEBUG));
        chipInfo.setOnClickListener(v  -> setFilter(AppLogger.Level.INFO));
        chipWarn.setOnClickListener(v  -> setFilter(AppLogger.Level.WARN));
        chipError.setOnClickListener(v -> setFilter(AppLogger.Level.ERROR));
    }

    private void setupSearch() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                currentSearch = s != null ? s.toString() : "";
                refreshLogs();
            }
        });
    }

    private void setupActions() {
        // Toggle auto-scroll al pulsar
        tvAutoScroll.setOnClickListener(v -> {
            autoScroll = !autoScroll;
            if (autoScroll) {
                tvAutoScroll.setText("● Auto-scroll ON");
                tvAutoScroll.setTextColor(Color.parseColor("#3FB950"));
                scrollToBottom();
            } else {
                tvAutoScroll.setText("● Auto-scroll OFF");
                tvAutoScroll.setTextColor(Color.parseColor("#8B949E"));
            }
        });

        tvClearLogs.setOnClickListener(v -> {
            AppLogger.getInstance().clear();
            refreshLogs();
            AppLogger.i("AppLogActivity", "Logs limpiados por el usuario");
        });
    }

    // -------------------------------------------------------------------------
    // Lógica

    private void setFilter(AppLogger.Level level) {
        currentFilter = level;

        // Resetear colores de chips
        int colorInactive = Color.parseColor("#8B949E");
        chipAll.setTextColor(level == null ? Color.parseColor("#58A6FF") : colorInactive);
        chipDebug.setTextColor(level == AppLogger.Level.DEBUG ? Color.parseColor("#8B949E") : colorInactive);
        chipInfo.setTextColor(level == AppLogger.Level.INFO ? Color.parseColor("#3FB950") : colorInactive);
        chipWarn.setTextColor(level == AppLogger.Level.WARN ? Color.parseColor("#D29922") : colorInactive);
        chipError.setTextColor(level == AppLogger.Level.ERROR ? Color.parseColor("#F85149") : colorInactive);

        refreshLogs();
    }

    private void refreshLogs() {
        List<AppLogger.LogEntry> filtered =
                AppLogger.getInstance().getEntriesFiltered(currentFilter, currentSearch);

        adapter.setEntries(filtered);
        tvLogCount.setText(filtered.size() + " entradas");

        // Mostrar/ocultar estado vacío
        View emptyView = findViewById(R.id.emptyState);
        if (emptyView != null) {
            emptyView.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
        }

        if (autoScroll) scrollToBottom();
    }

    private void scrollToBottom() {
        int count = adapter.getItemCount();
        if (count > 0) recyclerView.scrollToPosition(count - 1);
    }

    // =========================================================================
    // Adapter
    // =========================================================================

    private static class LogAdapter extends RecyclerView.Adapter<LogAdapter.ViewHolder> {

        private List<AppLogger.LogEntry> entries;

        LogAdapter(List<AppLogger.LogEntry> entries) {
            this.entries = entries;
        }

        void setEntries(List<AppLogger.LogEntry> entries) {
            this.entries = entries;
            notifyDataSetChanged();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_log_entry, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            AppLogger.LogEntry entry = entries.get(position);
            holder.bind(entry);
        }

        @Override
        public int getItemCount() { return entries.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            View levelIndicator;
            TextView tvLevel, tvTag, tvTimestamp, tvMessage;

            ViewHolder(View itemView) {
                super(itemView);
                levelIndicator = itemView.findViewById(R.id.levelIndicator);
                tvLevel        = itemView.findViewById(R.id.tvLevel);
                tvTag          = itemView.findViewById(R.id.tvTag);
                tvTimestamp    = itemView.findViewById(R.id.tvTimestamp);
                tvMessage      = itemView.findViewById(R.id.tvMessage);
            }

            void bind(AppLogger.LogEntry entry) {
                tvTag.setText(entry.tag);
                tvTimestamp.setText(entry.timestamp);
                tvMessage.setText(entry.message);

                int color;
                String label;

                switch (entry.level) {
                    case DEBUG:
                        color = Color.parseColor("#8B949E"); label = "DBG"; break;
                    case INFO:
                        color = Color.parseColor("#3FB950"); label = "INF"; break;
                    case WARN:
                        color = Color.parseColor("#D29922"); label = "WRN"; break;
                    case ERROR:
                        color = Color.parseColor("#F85149"); label = "ERR"; break;
                    default:
                        color = Color.WHITE; label = "???";
                }

                tvLevel.setText(label);
                tvLevel.setTextColor(color);
                levelIndicator.setBackgroundColor(color);

                // Fondo alterno sutil
                int bg = (getAdapterPosition() % 2 == 0)
                        ? Color.parseColor("#0D1117")
                        : Color.parseColor("#161B22");
                itemView.setBackgroundColor(bg);
            }
        }
    }

    private void showPasswordDialog() {
        // Inflar el layout del diálogo
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_admin_password, null);
        com.google.android.material.textfield.TextInputEditText etPassword =
                dialogView.findViewById(R.id.etAdminPassword);
        TextView tvError = dialogView.findViewById(R.id.tvPasswordError);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Acceso restringido")
                .setMessage("Introduce la contraseña de administrador")
                .setView(dialogView)
                .setCancelable(false) // No se puede cerrar sin contraseña
                .setPositiveButton("Entrar", null) // null para manejarlo manualmente
                .setNegativeButton("Cancelar", (d, w) -> finish()) // Cierra la Activity
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String input = etPassword.getText() != null
                        ? etPassword.getText().toString() : "";

                if (PasswordHelper.checkPassword(this, input)) {
                    AppLogger.i("AppLogActivity", "Acceso admin concedido.");
                    dialog.dismiss();
                    // Iniciar la vista normalmente
                    refreshLogs();
                    AppLogger.getInstance().addListener(logListener);
                } else {
                    AppLogger.w("AppLogActivity", "Intento de acceso admin fallido.");
                    tvError.setVisibility(View.VISIBLE);
                    etPassword.setText("");
                }
            });
        });

        dialog.show();
    }
}