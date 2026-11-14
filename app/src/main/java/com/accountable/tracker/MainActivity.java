package com.accountable.tracker;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import androidx.webkit.WebViewAssetLoader;

/**
 * Hosts the Accountable web app (index.html + vendor/ libraries) in a WebView.
 * Everything is served from the APK's assets under https://accountable.local/, a stable origin,
 * so localStorage (key accountable_v3) persists across launches and the app works fully offline.
 */
public class MainActivity extends Activity {
  private static final String ORIGIN = "https://accountable.local/";
  private WebView web;

  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    boolean night = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    int bg = night ? Color.rgb(0x14, 0x19, 0x14) : Color.rgb(0xFA, 0xFA, 0xF7); // sprig bg, dark / light
    getWindow().setStatusBarColor(bg);
    getWindow().setNavigationBarColor(bg);
    if (Build.VERSION.SDK_INT >= 26 && !night) {
      getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    }

    final WebViewAssetLoader loader = new WebViewAssetLoader.Builder()
        .setDomain("accountable.local")
        .addPathHandler("/", new WebViewAssetLoader.AssetsPathHandler(this))
        .build();

    web = new WebView(this);
    web.setBackgroundColor(bg);
    web.getSettings().setJavaScriptEnabled(true);
    web.getSettings().setDomStorageEnabled(true);
    web.getSettings().setAllowFileAccess(false);
    web.getSettings().setAllowContentAccess(false);
    web.setWebViewClient(new WebViewClient() {
      @Override public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
        return loader.shouldInterceptRequest(r.getUrl());
      }
      @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
        // Stay inside the app for our own origin; send everything else to the system browser.
        if (ORIGIN.startsWith(r.getUrl().getScheme() + "://" + r.getUrl().getHost() + "/")) return false;
        try { startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW, r.getUrl())); } catch (Exception ignored) {}
        return true;
      }
    });

    FrameLayout container = new FrameLayout(this);
    container.addView(web, new FrameLayout.LayoutParams(-1, -1));
    setContentView(container);
    container.setOnApplyWindowInsetsListener((v, insets) -> {
      if (Build.VERSION.SDK_INT >= 30) {
        int types = WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime();
        android.graphics.Insets i = insets.getInsets(types);
        v.setPadding(i.left, i.top, i.right, i.bottom);
        return new WindowInsets.Builder(insets).setInsets(types, android.graphics.Insets.NONE).build();
      }
      v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
      return insets.consumeSystemWindowInsets();
    });
    container.requestApplyInsets();

    if (state != null) web.restoreState(state); else web.loadUrl(ORIGIN + "index.html");
  }

  @Override protected void onSaveInstanceState(Bundle out) { super.onSaveInstanceState(out); if (web != null) web.saveState(out); }
  @Override protected void onPause() { super.onPause(); web.onPause(); web.pauseTimers(); }
  @Override protected void onResume() { super.onResume(); web.onResume(); web.resumeTimers(); }
  @Override protected void onDestroy() { if (web != null) web.destroy(); super.onDestroy(); }
}
