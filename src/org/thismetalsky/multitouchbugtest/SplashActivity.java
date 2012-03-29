
package org.thismetalsky.multitouchbugtest;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;

public class SplashActivity extends Activity {

    private WebView  wv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.act_credits);
        wv = (WebView) findViewById(R.id.wv);
        wv.loadUrl("file:///android_asset/splash.html");
    }

    public void onContinue(View v) {
        startActivity(new Intent(this, TouchTestActivity.class));
        finish();
    }
}
