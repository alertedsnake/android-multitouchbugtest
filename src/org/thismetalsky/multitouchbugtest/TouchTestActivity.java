
package org.thismetalsky.multitouchbugtest;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;

public class TouchTestActivity extends Activity {

    private final String TAG = "GFXTest";
    private FrameLayout layout;
    private TouchTestView  gameView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
 
        setContentView(R.layout.act_main);
        layout = (FrameLayout) findViewById(R.id.main);

        // setup the view and thread
        gameView = new TouchTestView(this);

        // clean out the layout and add the view
        layout.removeAllViews();
        layout.addView(gameView);

        // okay, start running it
        gameView.getThread().doStart();
    }

    @Override
    protected void onPause() {
        super.onPause();
        gameView.getThread().setPaused();
    }

    @Override
    protected void onResume() {
        super.onResume();
        gameView.getThread().setRunning();
    }
}

