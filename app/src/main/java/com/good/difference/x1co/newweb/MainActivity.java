package com.good.difference.x1co.newweb;

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
    private static final String APP_VERSION = "1.1";

    private EditText urlBar;
    private FrameLayout webContainer;
    private LinearLayout tabBar;

    private final ArrayList<WebView> tabs = new ArrayList<>();
    private final ArrayList<View> tabViews = new ArrayList<>();
    private int currentTab = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        urlBar = findViewById(R.id.urlBar);
        webContainer = findViewById(R.id.webContainer);
        tabBar = findViewById(R.id.tabBar);
        Button go = findViewById(R.id.goBtn);

        go.setOnClickListener(v -> loadUrl());

        // Aba inicial
        addNewTab(HOME_URL);

        // Botão +
        addPlusTab();
    }

    /* ===================== ABAS ===================== */

    private void addNewTab(String url) {
        WebView w = new WebView(this);

        // ✅ ANDROID VERSION REAL (ex: 13, 14, 15, 16)
        String androidVersion = android.os.Build.VERSION.RELEASE;

        String userAgent =
                "Mozilla/5.0 NewWeb " + APP_VERSION + " on Android " + androidVersion;

        w.getSettings().setUserAgentString(userAgent);
        w.getSettings().setJavaScriptEnabled(true);
        w.getSettings().setDomStorageEnabled(true);

        w.setWebViewClient(webClient);
        w.setWebChromeClient(chromeClient);

        tabs.add(w);
        int index = tabs.size() - 1;

        View tab = LayoutInflater.from(this)
                .inflate(R.layout.tab_item, tabBar, false);

        TextView title = tab.findViewById(R.id.tabTitle);
        TextView close = tab.findViewById(R.id.tabClose);

        title.setText("Nova aba");

        tab.setOnClickListener(v -> switchToTab(index));
        close.setOnClickListener(v -> closeTab(index));

        tabViews.add(tab);
        tabBar.addView(tab, tabBar.getChildCount() - 1);

        switchToTab(index);
        w.loadUrl(url);
    }

    private void addPlusTab() {
        TextView plus = new TextView(this);
        plus.setText("+");
        plus.setTextSize(22);
        plus.setPadding(32, 8, 32, 8);
        plus.setOnClickListener(v -> addNewTab(HOME_URL));
        tabBar.addView(plus);
    }

    private void switchToTab(int index) {
        if (index < 0 || index >= tabs.size()) return;

        webContainer.removeAllViews();
        webContainer.addView(tabs.get(index));
        currentTab = index;
    }

    private void closeTab(int index) {
        if (index < 0 || index >= tabs.size()) return;

        tabs.remove(index);
        tabBar.removeView(tabViews.remove(index));

        if (tabs.isEmpty()) {
            addNewTab(HOME_URL);
            return;
        }

        switchToTab(Math.max(0, index - 1));
    }

    /* ===================== WEB ===================== */

    private void loadUrl() {
        if (currentTab < 0) return;

        String url = urlBar.getText().toString().trim();
        if (TextUtils.isEmpty(url)) return;
        if (!url.startsWith("http")) url = "https://" + url;

        tabs.get(currentTab).loadUrl(url);
    }

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
            int i = tabs.indexOf(v);
            if (i >= 0) {
                TextView t = tabViews.get(i).findViewById(R.id.tabTitle);
                t.setText(title);
            }
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