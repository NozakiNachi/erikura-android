package jp.co.recruit.erikura.presenters.util;

import android.webkit.JavascriptInterface;

public class WebViewResizeHeightJavascriptInterface {
    public interface ResizeHeightCallback {
        public void onResizeHeight(float height);
    }

    private ResizeHeightCallback callback = null;

    public WebViewResizeHeightJavascriptInterface(ResizeHeightCallback callback) {
        this.callback = callback;
    }

    @JavascriptInterface
    public void resizeHeight(final float height) {
        if (this.callback != null) {
            this.callback.onResizeHeight(height);
        }
    }
}
