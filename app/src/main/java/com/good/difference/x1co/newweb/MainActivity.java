package com.good.difference.x1co.newweb;

import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final String HOME_URL = "https://www.google.com";
    private static final String APP_VERSION = "1.3";

    private EditText urlBar;
    private FrameLayout webContainer;
    private Button tabsBtn;

    private final ArrayList<WebView> tabs = new ArrayList<>();
    private int currentTab = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        urlBar = findViewById(R.id.urlBar);
        webContainer = findViewById(R.id.webContainer);
        tabsBtn = findViewById(R.id.tabsBtn);
        Button go = findViewById(R.id.goBtn);

        go.setOnClickListener(v -> loadInput());
        tabsBtn.setOnClickListener(v -> showTabsDialog());

        addNewTab(HOME_URL);
    }

    /* ===================== URL / BUSCA ===================== */

    private void loadInput() {
        if (currentTab < 0) return;

        String input = urlBar.getText().toString();
        if (TextUtils.isEmpty(input)) return;

        String url = buildUrl(input);
        tabs.get(currentTab).loadUrl(url);
    }

    private String buildUrl(String input) {
        input = input.trim();

        if (input.contains(" ") || !input.contains(".")) {
            return "https://www.google.com/search?q=" +
                    input.replace(" ", "+");
        }

        if (!input.startsWith("http://") && !input.startsWith("https://")) {
            input = "https://" + input;
        }

        return input;
    }

    /* ===================== ABAS ===================== */

    private void addNewTab(String url) {
        WebView w = new WebView(this);

        /* ===== USER-AGENT ===== */
        String androidVersion = Build.VERSION.RELEASE;
        String device = Build.MODEL;

        String webViewMajor = "unknown";
        if (Build.VERSION.SDK_INT >= 26) {
            try {
                webViewMajor = WebView
                        .getCurrentWebViewPackage()
                        .versionName.split("\\.")[0];
            } catch (Exception ignored) {}
        }

        String userAgent =
                "Mozilla/5.0 (Android " + androidVersion +
                        "; Mobile; " + device + ") " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/" + webViewMajor +
                        " on NewWeb/" + APP_VERSION +
                        " based on WebView " + webViewMajor +
                        " Safari/537.36";

        w.getSettings().setUserAgentString(userAgent);
        /* ===================== */

        w.getSettings().setJavaScriptEnabled(true);
        w.getSettings().setDomStorageEnabled(true);

        w.setWebViewClient(webClient);
        w.setWebChromeClient(chromeClient);

        tabs.add(w);
        switchToTab(tabs.size() - 1);
        w.loadUrl(url);

        updateTabsButton();
    }

    private void switchToTab(int index) {
        if (index < 0 || index >= tabs.size()) return;

        webContainer.removeAllViews();
        webContainer.addView(tabs.get(index));
        currentTab = index;

        updateTabsButton();
    }

    private void closeTab(int index) {
        if (index < 0 || index >= tabs.size()) return;

        tabs.remove(index);

        if (tabs.isEmpty()) {
            addNewTab(HOME_URL);
        } else {
            switchToTab(Math.max(0, index - 1));
        }
    }

    private void updateTabsButton() {
        tabsBtn.setText("Abas (" + tabs.size() + ")");
    }

    private void showTabsDialog() {
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);

        for (int i = 0; i < tabs.size(); i++) {
            int index = i;

            TextView item = new TextView(this);
            item.setPadding(32, 24, 32, 24);
            item.setText(tabs.get(i).getTitle() != null
                    ? tabs.get(i).getTitle()
                    : "Aba " + (i + 1));

            item.setOnClickListener(v -> {
                switchToTab(index);
            });

            list.addView(item);
        }

        new AlertDialog.Builder(this)
                .setTitle("Abas")
                .setView(list)
                .setPositiveButton("+ Nova aba", (d, w) ->
                        addNewTab(HOME_URL))
                .setNegativeButton("Fechar", null)
                .show();
    }

    /* ===================== WEB ===================== */

    private final WebViewClient webClient = new WebViewClient() {
        @Override
        public boolean shouldOverrideUrlLoading(WebView v, String url) {

            if (isAdult(url)) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Conteúdo +18")
                        .setMessage("Este site pode conter conteúdo adulto. Deseja continuar?")
                        .setPositiveButton("Continuar", (d, w) -> v.loadUrl(url))
                        .setNegativeButton("Cancelar", null)
                        .show();
                return true;
            }

            v.loadUrl(url);
            return true;
        }
    };

    private final WebChromeClient chromeClient = new WebChromeClient() {
        @Override
        public void onReceivedTitle(WebView v, String title) {
            updateTabsButton();
        }
    };

    private boolean isAdult(String url) {
        String u = url.toLowerCase();
        return u.contains("porn") || u.contains("xxx") || u.contains("sex");
    }

    @Override
    public void onBackPressed() {
        if (currentTab >= 0 && tabs.get(currentTab).canGoBack()) {
            tabs.get(currentTab).goBack();
        } else {
            super.onBackPressed();
        }
    }
}
