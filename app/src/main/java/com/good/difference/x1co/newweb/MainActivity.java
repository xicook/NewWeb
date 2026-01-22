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

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {

    private FrameLayout webContainer;
    private EditText urlBar;

    private final ArrayList<WebView> tabs = new ArrayList<>();
    private int currentTab = 0;

    private static final String HOME_URL = "https://www.google.com";

    private static final String[] ADULT_KEYWORDS = {
            "porn", "sex", "xxx", "hentai", "nsfw", "adult"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webContainer = findViewById(R.id.webContainer);
        urlBar = findViewById(R.id.urlBar);

        hideSystemUI();
        createNewTab(HOME_URL);

        urlBar.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                loadFromBar(urlBar.getText().toString());
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
       WEBVIEW SETUP
       ========================= */

    private void setupWebView(WebView webView) {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setSupportMultipleWindows(true);

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
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                webContainer.removeAllViews();
                webContainer.addView(view);
            }

            @Override
            public void onHideCustomView() {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                switchToTab(currentTab);
            }
        });
    }

    /* =========================
       URL BAR LOGIC
       ========================= */

    private void loadFromBar(String input) {
        if (!input.startsWith("http")) {
            String q = Uri.encode(input);
            input = "https://www.google.com/search?q=" + q;
        }
        tabs.get(currentTab).loadUrl(input);
    }

    /* =========================
       ADULT CONTENT BLOCK
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
                " on NewWeb/1.4 based on WebView " + webViewVer +
                " Safari/537.36";
    }

    /* =========================
       FULLSCREEN + PIP
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

    @Override
    public void onUserLeaveHint() {
        if (Build.VERSION.SDK_INT >= 26) {
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
