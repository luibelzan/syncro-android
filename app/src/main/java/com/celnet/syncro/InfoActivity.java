package com.celnet.syncro;

import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.celnet.syncro.licenses.LicenseManager;

public class InfoActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_info);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        fillLicenseInfo();
    }

    private void fillLicenseInfo() {
        TextView tvLicenseCode = findViewById(R.id.tvLicenseCode);
        TextView tvLicenseType = findViewById(R.id.tvLicenseType);
        TextView tvExpires     = findViewById(R.id.tvExpires);
        TextView tvCustomer    = findViewById(R.id.tvCustomer);

        String code     = LicenseManager.getCachedCode(this);
        String expires  = LicenseManager.getCachedExpires(this);
        String customer = LicenseManager.getCachedCustomer(this);

        tvLicenseCode.setText("Código de licencia: " +
                (code != null ? code : "Sin licencia"));

        // Tipo: DLMS, IEC-870 o DLMS + IEC-870 (sin licencia guardada no se muestra tipo)
        tvLicenseType.setText("Tipo de licencia: " +
                (code != null ? LicenseManager.getCachedType(this).label() : "—"));

        tvExpires.setText("Licencia válida hasta: " +
                (expires != null && !expires.isEmpty() ? expires : "—"));

        tvCustomer.setText("Usuario actual: " +
                (customer != null && !customer.isEmpty() ? customer : "—"));
    }
}