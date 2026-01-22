package com.good.difference.x1co.newweb;

import android.app.Dialog;
import android.app.PictureInPictureParams;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Rational;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final String HOME_URL = "https://www.google.com";
    private static final String APP_VERSION = "1.3";

    private EditText urlBar;
    private ImageButton tabsBtn;
    private FrameLayout webContainer;
    private View root;

    private final ArrayList<WebView> tabs = new ArrayList<>();
    private int currentTab = -1;

    // Fullscreen vídeo
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        root = findViewById(R.id.root);
        urlBar = findViewById(R.id.urlBar);
        tabsBtn = findViewById(R.id.tabsBtn);
        webContainer = findViewById(R.id.webContainer);

        // Enter abre site ou pesquisa
        urlBar.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO
                    || actionId == EditorInfo.IME_ACTION_DONE
                    || actionId == EditorInfo.IME_NULL) {
                loadInput();
                return true;
            }
            return false;
        });

        tabsBtn.setOnClickListener(v -> showTabsScreen());

        addNewTab(HOME_URL);
    }

    /* ================= URL / PESQUISA ================= */

    private void loadInput() {
        if (currentTab < 0) return;

        String input = urlBar.getText().toString();
        if (TextUtils.isEmpty(input)) return;

        tabs.get(currentTab).loadUrl(buildUrl(input));
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

        String webViewMajor = "0";
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
        w.getSettings().setMediaPlaybackRequiresUserGesture(false);

        w.setWebViewClient(webClient);
        w.setWebChromeClient(chromeClient);

        tabs.add(w);
        switchToTab(tabs.size() - 1);
        w.loadUrl(url);

        updateTabsIcon();
    }

    private void switchToTab(int index) {
        if (index < 0 || index >= tabs.size()) return;

        webContainer.removeAllViews();
        webContainer.addView(tabs.get(index),
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                ));

        currentTab = index;
        updateTabsIcon();
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

    private void updateTabsIcon() {
        tabsBtn.setContentDescription("Abas (" + tabs.size() + ")");
    }

    /* ================= TELA DE ABAS (FULLSCREEN) ================= */

    private void showTabsScreen() {
        Dialog dialog = new Dialog(
                this,
                android.R.style.Theme_DeviceDefault_NoActionBar_Fullscreen
        );
        dialog.setContentView(R.layout.dialog_tabs);

        LinearLayout list = dialog.findViewById(R.id.tabsList);
        TextView newTabBtn = dialog.findViewById(R.id.newTabBtn);

        newTabBtn.setOnClickListener(v -> {
            addNewTab(HOME_URL);
            dialog.dismiss();
        });

        for (int i = 0; i < tabs.size(); i++) {
            int index = i;

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(24, 24, 24, 24);

            TextView title = new TextView(this);
            title.setText(
                    tabs.get(i).getTitle() != null
                            ? tabs.get(i).getTitle()
                            : "Aba " + (i + 1)
            );
            title.setLayoutParams(
                    new LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1
                    )
            );

            TextView close = new TextView(this);
            close.setText("✕");
            close.setTextSize(18f);
            close.setPadding(24, 0, 24, 0);

            title.setOnClickListener(v -> {
                switchToTab(index);
                dialog.dismiss();
            });

            close.setOnClickListener(v -> {
                closeTab(index);
                dialog.dismiss();
                showTabsScreen(); // atualiza na hora
            });

            row.addView(title);
            row.addView(close);
            list.addView(row);
        }

        dialog.show();
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
            webContainer.addView(
                    view,
                    new FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                    )
            );

            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            );

            root.setVisibility(View.GONE);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }

        @Override
        public void onHideCustomView() {
            if (customView == null) return;

            webContainer.removeView(customView);
            customView = null;

            webContainer.addView(
                    tabs.get(currentTab),
                    new FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                    )
            );

            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);

            root.setVisibility(View.VISIBLE);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

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