package com.agarz.multi;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class WebViewFragment extends Fragment {

    private static final String ARG_ACCOUNT_ID = "account_id";
    private static final String GAME_URL = "https://agarz.com";

    private WebView  webView;
    private ProgressBar progressBar;
    private TextView tvScore;
    private TextView tvStatus;
    private Button   btnBot;

    private int     accountId;
    private boolean botEnabled = false;
    private final Handler handler = new Handler(Looper.getMainLooper());

    // ─── Bot JS — WebView içine inject edilir ────────────────────────────────
    // D tuşu: 0-120k → D bas (bot aç), 180k+ → D bas (bot kapat)
    // Z tuşu: 0-5k arası basılı tut (gold büyütme)
    // Ölüm: #nick görününce respawn
    private static final String BOT_JS =
        "(function() {" +
        "  if (window.__agarzBotInjected) return;" +
        "  window.__agarzBotInjected = true;" +
        "  window.__agarzBotRunning  = false;" +

        "  var SCORE_UPPER  = 180000;" +
        "  var SCORE_LOWER  = 120000;" +
        "  var SCORE_Z_STOP = 5000;" +
        "  var D_COOLDOWN   = 3000;" +

        "  var lastDPress  = 0;" +
        "  var botActive   = false;" +
        "  var zHeld       = false;" +
        "  var justSpawned = true;" +
        "  var isDead      = false;" +

        // Tuş gönderme yardımcıları
        "  function pressKey(key, code, kc) {" +
        "    var o={key:key,code:code,keyCode:kc,which:kc,bubbles:true,cancelable:true};" +
        "    document.dispatchEvent(new KeyboardEvent('keydown',o));" +
        "    setTimeout(function(){document.dispatchEvent(new KeyboardEvent('keyup',o));},80);" +
        "  }" +
        "  function holdKey(key,code,kc) {" +
        "    var o={key:key,code:code,keyCode:kc,which:kc,bubbles:true,cancelable:true};" +
        "    document.dispatchEvent(new KeyboardEvent('keydown',o));" +
        "  }" +
        "  function releaseKey(key,code,kc) {" +
        "    var o={key:key,code:code,keyCode:kc,which:kc,bubbles:true,cancelable:true};" +
        "    document.dispatchEvent(new KeyboardEvent('keyup',o));" +
        "  }" +
        "  function pressD()   { pressKey('d','KeyD',68); }" +
        "  function holdZ()    { holdKey('z','KeyZ',90);   zHeld=true; }" +
        "  function releaseZ() { releaseKey('z','KeyZ',90); zHeld=false; }" +

        // Respawn
        "  function respawn() {" +
        "    var sels=['#playButton','#play','#btnPlay','#startButton','[id*=play]','button'];" +
        "    for(var i=0;i<sels.length;i++){" +
        "      var el=document.querySelector(sels[i]);" +
        "      if(el&&el.offsetParent!==null){el.click();break;}" +
        "    }" +
        "    pressKey('Enter','Enter',13);" +
        "  }" +

        // Ana döngü
        "  function tick() {" +
        "    if (!window.__agarzBotRunning) {" +
        "      if(zHeld) releaseZ();" +
        "      botActive=false; isDead=false; justSpawned=true;" +
        "      AndroidBridge.onStatus('BOT KAPALI',0);" +
        "      return;" +
        "    }" +

        "    var score = window.userScoreCurrent || 0;" +
        "    AndroidBridge.onScore(Math.round(score));" +

        // Ölüm kontrolü
        "    var nick = document.getElementById('nick');" +
        "    var nickVisible = nick && nick.offsetParent!==null &&" +
        "      window.getComputedStyle(nick).display!=='none';" +

        "    if (!isDead && nickVisible && score < 100) {" +
        "      isDead=true;" +
        "      if(zHeld) releaseZ();" +
        "      botActive=false; justSpawned=true;" +
        "      AndroidBridge.onStatus('ÖLDÜ - yeniden doğuluyor',score);" +
        "      setTimeout(function(){" +
        "        respawn();" +
        "        setTimeout(function(){ isDead=false; },2000);" +
        "      },1200);" +
        "      return;" +
        "    }" +
        "    if(isDead) return;" +

        // Z fazı
        "    if (justSpawned) {" +
        "      if (score < SCORE_Z_STOP) {" +
        "        if(!zHeld){ holdZ(); AndroidBridge.onStatus('Z BASILI - gold büyütüyor',score); }" +
        "      } else {" +
        "        if(zHeld) releaseZ();" +
        "        justSpawned=false;" +
        "        AndroidBridge.onStatus('Z BİTTİ - D makrosu devrede',score);" +
        "      }" +
        "      return;" +
        "    }" +

        // D makrosu
        "    var now=Date.now();" +
        "    if(now-lastDPress < D_COOLDOWN) return;" +

        "    if (!botActive && score>0 && score<SCORE_LOWER) {" +
        "      pressD(); botActive=true; lastDPress=now;" +
        "      AndroidBridge.onStatus('D → BOT AÇILDI',score);" +
        "    } else if (botActive && score>SCORE_UPPER) {" +
        "      pressD(); botActive=false; lastDPress=now;" +
        "      AndroidBridge.onStatus('D → BOT KAPATILDI',score);" +
        "    } else {" +
        "      AndroidBridge.onStatus(botActive?'BOT AÇIK ✓':'bekliyor...',score);" +
        "    }" +
        "  }" +

        "  window.__agarzBotInterval = setInterval(tick, 600);" +
        "  AndroidBridge.onStatus('BOT YÜKLENDİ',0);" +
        "})();";

    public static WebViewFragment newInstance(int accountId) {
        WebViewFragment f = new WebViewFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_ACCOUNT_ID, accountId);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null)
            accountId = getArguments().getInt(ARG_ACCOUNT_ID, 1);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_webview, container, false);
    }

    @SuppressLint({"SetJavaScriptEnabled","JavascriptInterface"})
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        webView     = view.findViewById(R.id.webView);
        progressBar = view.findViewById(R.id.progressBar);
        tvScore     = view.findViewById(R.id.tvScore);
        tvStatus    = view.findViewById(R.id.tvStatus);
        btnBot      = view.findViewById(R.id.btnBot);

        webView.addJavascriptInterface(new AndroidBridge(), "AndroidBridge");

        setupWebView();
        webView.loadUrl(GAME_URL);

        btnBot.setOnClickListener(v -> toggleBot());
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setSupportZoom(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        // Her hesap farklı UA → oturum karışmaz
        s.setUserAgentString(s.getUserAgentString() + " AgarzAcc/" + accountId);

        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(true);
        cm.setAcceptThirdPartyCookies(webView, true);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView v, int p) {
                if (progressBar == null) return;
                progressBar.setProgress(p);
                progressBar.setVisibility(p == 100 ? View.GONE : View.VISIBLE);
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView v, String url) {
                CookieManager.getInstance().flush();
                injectStorageIsolation();
                // Oyun yüklendikten 2 sn sonra bot'u inject et
                if (botEnabled) {
                    handler.postDelayed(() -> {
                        if (webView != null)
                            webView.evaluateJavascript(BOT_JS, null);
                    }, 2000);
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView v, String url) {
                if (url.contains("agarz")) v.loadUrl(url);
                else v.loadUrl(GAME_URL);
                return true;
            }
        });
    }

    private void toggleBot() {
        botEnabled = !botEnabled;
        if (botEnabled) {
            // Bot'u inject et ve başlat
            webView.evaluateJavascript(BOT_JS, null);
            handler.postDelayed(() -> {
                if (webView != null)
                    webView.evaluateJavascript("window.__agarzBotRunning=true;", null);
            }, 500);
        } else {
            webView.evaluateJavascript("window.__agarzBotRunning=false;", null);
        }
        updateBotButton();
    }

    private void injectStorageIsolation() {
        if (webView == null) return;
        String js =
            "(function(){" +
            "  var p='acc_" + accountId + "_';" +
            "  var o=window.localStorage;" +
            "  try{Object.defineProperty(window,'localStorage',{get:function(){return{" +
            "    getItem:function(k){return o.getItem(p+k);}," +
            "    setItem:function(k,v){o.setItem(p+k,v);}," +
            "    removeItem:function(k){o.removeItem(p+k);}," +
            "    clear:function(){Object.keys(o).filter(function(k){return k.startsWith(p);}).forEach(function(k){o.removeItem(k);});}," +
            "    get length(){return o.length;}" +
            "  }}});}catch(e){}" +
            "})();";
        webView.evaluateJavascript(js, null);
    }

    private void updateBotButton() {
        if (btnBot == null) return;
        if (botEnabled) {
            btnBot.setText("🤖 BOT: AÇIK");
            btnBot.setBackgroundColor(0xFF00C853);
            btnBot.setTextColor(0xFF000000);
        } else {
            btnBot.setText("🤖 BOT: KAPALI");
            btnBot.setBackgroundColor(0xFFD50000);
            btnBot.setTextColor(0xFFFFFFFF);
        }
    }

    // ─── JS → Android köprüsü ────────────────────────────────────────────────
    private class AndroidBridge {
        @JavascriptInterface
        public void onScore(int score) {
            handler.post(() -> {
                if (tvScore == null) return;
                String txt;
                if      (score >= 1_000_000) txt = String.format("%.2fM", score / 1_000_000f);
                else if (score >= 1_000)     txt = String.format("%.1fK", score / 1_000f);
                else                          txt = String.valueOf(score);
                tvScore.setText(txt);
            });
        }

        @JavascriptInterface
        public void onStatus(String status, int score) {
            handler.post(() -> {
                if (tvStatus != null) tvStatus.setText(status);
            });
        }
    }

    @Override public void onPause()  { super.onPause();  if (webView != null) webView.onPause(); }
    @Override public void onResume() { super.onResume(); if (webView != null) webView.onResume(); }

    @Override
    public void onDestroyView() {
        if (webView != null) {
            webView.evaluateJavascript(
                "window.__agarzBotRunning=false; clearInterval(window.__agarzBotInterval);", null);
            webView.stopLoading();
            webView.destroy();
            webView = null;
        }
        super.onDestroyView();
    }
}
