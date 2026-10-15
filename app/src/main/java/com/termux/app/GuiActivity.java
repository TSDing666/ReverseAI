package com.termux.app;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.termux.R;

/**
 * AI 逆向助手界面。
 * WebView 加载本地聊天页 (assets/ai_chat/index.html),
 * 通过 JS Bridge (AndroidBridge) 调用原生 HTTP 层访问 Agent (/data/adb/agent/agent.py:8080),
 * 避开 file:// 源的 CORS 限制, 无需明文 HTTP 全局放行。
 */
public class GuiActivity extends AppCompatActivity {

    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        setContentView(R.layout.activity_gui);

        webView = findViewById(R.id.ai_webview);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new Bridge(), "AndroidBridge");
        webView.loadUrl("file:///android_asset/ai_chat/index.html");
    }

    @Override
    public void onBackPressed() {
        // 聊天页不做网页回退, 直接交回上层处理
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            ((ViewGroup) webView.getParent()).removeView(webView);
            webView.destroy();
        }
        super.onDestroy();
    }

    /** 暴露给 JS 的原生桥。所有方法必须在 JS 端异步调用 (返回值经回调送回)。 */
    public class Bridge {

        /** 同步 POST, 返回响应体字符串; JS 端用 setTimeout/worker 包一层避免阻塞 UI。 */
        @JavascriptInterface
        public String post(String url, String jsonBody) {
            return HttpUtil.post(url, jsonBody);
        }

        /** 启动 TermuxActivity (聊天中可直接跳回终端)。 */
        @JavascriptInterface
        public void openTerminal() {
            startActivity(new android.content.Intent(GuiActivity.this, TermuxActivity.class));
        }

        /** 读配置: Agent 端点地址。 */
        @JavascriptInterface
        public String getAgentUrl() {
            return "http://127.0.0.1:8080";
        }
    }
}
