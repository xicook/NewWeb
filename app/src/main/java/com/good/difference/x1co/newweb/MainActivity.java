package com.good.difference.x1co.newweb;

import android.app.Activity;
import android.app.Dialog;
import android.app.PictureInPictureParams;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Rational;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import java.util.ArrayList;

public class MainActivity extends Activity {

    private FrameLayout webContainer;
    private LinearLayout tabsOverlay;
    private EditText urlBar;

    private final ArrayList<WebView> tabs = new ArrayList<>();
    private int currentTab = 0;

    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;

    private static final String HOME_URL = "https://www.google.com";

    private static final String[] ADULT_KEYWORDS = {
            "porn", "sex", "xxx", "hentai", "nsfw", "adult"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webContainer = findViewById(R.id.webContainer);
        tabsOverlay = findViewById(R.id.tabsOverlay);
        urlBar = findViewById(R.id.urlBar);

        hideSystemUI();
        createNewTab(HOME_URL);

        urlBar.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                loadFromBar(urlBar.getText().toString());
                hideKeyboardAndFocus();
                return true;
            }
            return false;
        });
    }

    /* =========================
       TABS
       ========================= */

    private void createNewTab(String url) {
        WebView webView = new WebView(this);
        setupWebView(webView);
        tabs.add(webView);
        switchToTab(tabs.size() - 1);
        webView.loadUrl(url);
    }

    private void switchToTab(int index) {
        webContainer.removeAllViews();
        currentTab = index;
        webContainer.addView(tabs.get(index));
    }

    /* =========================
       WEBVIEW
       ========================= */

    private void setupWebView(WebView webView) {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);

        s.setUserAgentString(buildUserAgent());

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (isAdultUrl(url)) {
                    showAdultWarning(url);
                    return true;
                }
                return false;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                urlBar.setText(url);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                customView = view;
                customViewCallback = callback;

                webContainer.removeAllViews();
                webContainer.addView(view);

                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                hideSystemUI();
                urlBar.setVisibility(View.GONE);
            }

            @Override
            public void onHideCustomView() {
                if (customView != null) {
                    webContainer.removeView(customView);
                    customView = null;
                }

                if (customViewCallback != null) {
                    customViewCallback.onCustomViewHidden();
                }

                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                urlBar.setVisibility(View.VISIBLE);
                switchToTab(currentTab);
                hideSystemUI();
            }
        });
    }

    /* =========================
       URL BAR
       ========================= */

    private void loadFromBar(String input) {
        input = input.trim();

        if (input.contains(" ")) {
            tabs.get(currentTab).loadUrl(
                    "https://www.google.com/search?q=" + Uri.encode(input)
            );
            return;
        }

        if (!input.startsWith("http://") && !input.startsWith("https://")) {
            input = "https://" + input;
        }

        tabs.get(currentTab).loadUrl(input);
    }

    private void hideKeyboardAndFocus() {
        urlBar.clearFocus();
    }

    /* =========================
       ADULT BLOCK
       ========================= */

    private boolean isAdultUrl(String url) {
        if (url == null) return false;
        String l = url.toLowerCase();
        for (String k : ADULT_KEYWORDS) {
            if (l.contains(k)) return true;
        }
        return false;
    }

    private void showAdultWarning(String blockedUrl) {
        Dialog d = new Dialog(this, android.R.style.Theme_DeviceDefault_NoActionBar_Fullscreen);
        d.setContentView(R.layout.dialog_adult_warning);

        d.findViewById(R.id.continueBtn).setOnClickListener(v -> {
            d.dismiss();
            tabs.get(currentTab).loadUrl(blockedUrl);
        });

        d.findViewById(R.id.backBtn).setOnClickListener(v -> {
            d.dismiss();
            tabs.get(currentTab).loadUrl(HOME_URL);
        });

        d.show();
    }

    /* =========================
       USER AGENT
       ========================= */

    private String buildUserAgent() {
        String androidVer = Build.VERSION.RELEASE;
        String device = Build.MODEL;
        String webViewVer = WebView.getCurrentWebViewPackage() != null
                ? WebView.getCurrentWebViewPackage().versionName.split("\\.")[0]
                : "0";

        return "Mozilla/5.0 (Android " + androidVer + "; Mobile; " + device + ") " +
                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/" + webViewVer +
                " on NewWeb/1.3 based on WebView " + webViewVer +
                " Safari/537.36";
    }

    /* =========================
       FULLSCREEN
       ========================= */

    private void hideSystemUI() {
        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) {
                c.hide(WindowInsets.Type.systemBars());
                c.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        }
    }

    /* =========================
       PIP
       ========================= */

    @Override
    public void onUserLeaveHint() {
        if (Build.VERSION.SDK_INT >= 26 && customView == null) {
            PictureInPictureParams params =
                    new PictureInPictureParams.Builder()
                            .setAspectRatio(new Rational(16, 9))
                            .build();
            enterPictureInPictureMode(params);
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
}