package com.good.difference.x1co.newweb;

import android.app.PictureInPictureParams;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Rational;
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

    // Fullscreen
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;

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

    /* ================= URL / BUSCA ================= */

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

        if (!input.startsWith("http")) {
            input = "https://" + input;
        }

        return input;
    }

    /* ================= ABAS ================= */

    private void addNewTab(String url) {
        WebView w = new WebView(this);

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

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(24, 24, 24, 24);

            TextView title = new TextView(this);
            title.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            title.setText(tabs.get(i).getTitle() != null
                    ? tabs.get(i).getTitle()
                    : "Aba " + (i + 1));

            title.setOnClickListener(v -> {
                switchToTab(index);
            });

            TextView close = new TextView(this);
            close.setText("✕");
            close.setTextSize(18);
            close.setPadding(16, 0, 16, 0);
            close.setOnClickListener(v -> {
                closeTab(index);
            });

            row.addView(title);
            row.addView(close);
            list.addView(row);
        }

        new AlertDialog.Builder(this)
                .setTitle("Abas")
                .setView(list)
                .setPositiveButton("+ Nova aba", (d, w) ->
                        addNewTab(HOME_URL))
                .setNegativeButton("Fechar", null)
                .show();
    }

    /* ================= WEB ================= */

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
        public void onShowCustomView(View view, CustomViewCallback callback) {
            if (customView != null) {
                callback.onCustomViewHidden();
                return;
            }

            customView = view;
            customViewCallback = callback;

            webContainer.removeAllViews();
            webContainer.addView(view);

            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            urlBar.setVisibility(View.GONE);
            tabsBtn.setVisibility(View.GONE);
        }

        @Override
        public void onHideCustomView() {
            if (customView == null) return;

            webContainer.removeView(customView);
            customView = null;

            webContainer.addView(tabs.get(currentTab));

            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            urlBar.setVisibility(View.VISIBLE);
            tabsBtn.setVisibility(View.VISIBLE);

            if (customViewCallback != null) {
                customViewCallback.onCustomViewHidden();
            }
        }
    };

    private boolean isAdult(String url) {
        String u = url.toLowerCase();
        return u.contains("porn") || u.contains("xxx") || u.contains("sex");
    }

    /* ================= PiP ================= */

    @Override
    protected void onUserLeaveHint() {
        if (Build.VERSION.SDK_INT >= 26 && customView != null) {
            PictureInPictureParams params =
                    new PictureInPictureParams.Builder()
                            .setAspectRatio(new Rational(16, 9))
                            .build();
            enterPictureInPictureMode(params);
        }
    }

    @Override
    public void onBackPressed() {
        if (customView != null) {
            chromeClient.onHideCustomView();
            return;
        }

        if (currentTab >= 0 && tabs.get(currentTab).canGoBack()) {
            tabs.get(currentTab).goBack();
        } else {
            super.onBackPressed();
        }
    }
}