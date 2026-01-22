package com.good.difference.x1co.newweb;

import android.app.Activity;
import android.app.Dialog;
import android.app.PictureInPictureParams;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.util.Rational;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final String HOME_URL = "https://www.google.com";

    private FrameLayout webContainer;
    private EditText urlBar;
    private ImageButton tabsButton;

    private final ArrayList<WebView> tabs = new ArrayList<>();
    private int currentTab = -1;

    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webContainer = findViewById(R.id.webContainer);
        urlBar = findViewById(R.id.urlBar);
        tabsButton = findViewById(R.id.tabsButton);

        setupUrlBar();
        tabsButton.setOnClickListener(v -> showTabsScreen());

        addNewTab(HOME_URL);
    }

    // =========================
    // URL BAR
    // =========================
    private void setupUrlBar() {
        urlBar.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                String text = urlBar.getText().toString().trim();
                loadInput(text);
                return true;
            }
            return false;
        });
    }

    private void loadInput(String input) {
        if (!input.contains("://") && !input.contains(".")) {
            input = "https://www.google.com/search?q=" + input.replace(" ", "+");
        } else if (!input.startsWith("http://") && !input.startsWith("https://")) {
            input = "https://" + input;
        }
        tabs.get(currentTab).loadUrl(input);
    }

    // =========================
    // TABS
    // =========================
    private void addNewTab(String url) {
        WebView webView = createWebView();
        tabs.add(webView);
        switchToTab(tabs.size() - 1);
        webView.loadUrl(url);
    }

    private void closeTab(int index) {
        if (tabs.size() <= 1) return;

        WebView webView = tabs.remove(index);
        webContainer.removeView(webView);
        webView.destroy();

        if (currentTab >= tabs.size()) {
            currentTab = tabs.size() - 1;
        }
        switchToTab(currentTab);
    }

    private void switchToTab(int index) {
        webContainer.removeAllViews();
        webContainer.addView(tabs.get(index),
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                ));
        currentTab = index;
    }

    // =========================
    // WEBVIEW
    // =========================
    private WebView createWebView() {
        WebView webView = new WebView(this);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                urlBar.setText(url);
            }
        });

        webView.setWebChromeClient(chromeClient);
        return webView;
    }

    // =========================
    // FULLSCREEN + PiP
    // =========================
    private final WebChromeClient chromeClient = new WebChromeClient() {

        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            customView = view;
            customViewCallback = callback;

            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            );
        }

        @Override
        public void onHideCustomView() {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);

            if (customViewCallback != null) {
                customViewCallback.onCustomViewHidden();
            }

            customView = null;
            customViewCallback = null;
        }
    };

    @Override
    protected void onUserLeaveHint() {
        if (Build.VERSION.SDK_INT >= 26) {
            PictureInPictureParams params =
                    new PictureInPictureParams.Builder()
                            .setAspectRatio(new Rational(16, 9))
                            .build();
            enterPictureInPictureMode(params);
        }
    }

    // =========================
    // TABS SCREEN
    // =========================
    private void showTabsScreen() {
        Dialog dialog = new Dialog(this, android.R.style.Theme_DeviceDefault_NoActionBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_tabs);

        LinearLayout list = dialog.findViewById(R.id.tabsList);
        TextView newTab = dialog.findViewById(R.id.newTabBtn);

        newTab.setOnClickListener(v -> {
            addNewTab(HOME_URL);
            dialog.dismiss();
        });

        for (int i = 0; i < tabs.size(); i++) {
            int index = i;

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(16, 16, 16, 16);

            TextView title = new TextView(this);
            title.setText("Aba " + (i + 1));
            title.setLayoutParams(new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1));

            TextView close = new TextView(this);
            close.setText("✕");
            close.setTextSize(18);

            title.setOnClickListener(v -> {
                switchToTab(index);
                dialog.dismiss();
            });

            close.setOnClickListener(v -> {
                closeTab(index);
                dialog.dismiss();
                showTabsScreen();
            });

            row.addView(title);
            row.addView(close);
            list.addView(row);
        }

        dialog.show();
    }

    // =========================
    // BACK
    // =========================
    @Override
    public void onBackPressed() {
        WebView webView = tabs.get(currentTab);
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}