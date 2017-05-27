package com.jventura.pyapp;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.jventura.pybridge.AssetExtractor;
import com.jventura.pybridge.PyBridge;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.PrintWriter;
import java.io.StringWriter;


public class MainActivity extends AppCompatActivity {

    // Reference to the activity so it can be accessed from the python interpreter
    public static MainActivity mActivity = null;

    // Class name to pass to python for loading
    private final String mActivityId = "com.jventura.pyapp.MainActivity";

    // Assets version
    public final int mAssetsVersion = 1;
    public final boolean mAssetsAlwaysOverwrite = false; // TODO: Set only on debug builds

    // Save layout elements to display a fade in animation
    // When the view is loaded from python
    private FrameLayout mContentView;
    private View mLoadingView;
    private int mShortAnimationDuration = 300;

    // Handler to hook python into the Android application event loop
    private final Handler mCallbackHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Save reference so python can access it
        mActivity = this;

        // Show loading screen
        setContentView(R.layout.activity_main);

        // Save views for animation when loading is complete
        mContentView = (FrameLayout) findViewById(R.id.contentView);
        mLoadingView = (View) findViewById(R.id.loadingView);

        // Load python in the background
        (new PythonTask()).execute("python");
    }

    /**
     * AsyncTask that:
     *  1. Extracts the python assets folder
     *  2. Initializes the python interpreter
     *  3. Calls the app start function of the bootstrap
     */
    private class PythonTask extends AsyncTask<String,String,JSONObject>{

        protected JSONObject doInBackground(String... dirs) {
            // Extract python files from assets
            String path = dirs[0];
            AssetExtractor assetExtractor = new AssetExtractor(mActivity);

            // If assets version changed, remove the old, and copy the new ones
            if (mAssetsAlwaysOverwrite || assetExtractor.getAssetsVersion() != mAssetsVersion) {
                publishProgress("Unpacking... Please wait.");
                assetExtractor.removeAssets(path);
                assetExtractor.copyAssets(path);
                assetExtractor.setAssetsVersion(mAssetsVersion);
            }

            // Get the extracted assets directory
            String pythonPath = assetExtractor.getAssetsDataDir() + path;

            // Start the Python interpreter
            publishProgress("Initializing... Please wait.");
            PyBridge.start(pythonPath);

            // Load the View
            publishProgress("Loading... Please wait.");

            try {
                JSONObject json = new JSONObject();
                JSONObject params = new JSONObject();
                json.put("method", "load");
                params.put("activity", mActivityId);
                json.putOpt("params", params);
                JSONObject result = PyBridge.call(json);
                publishProgress("Starting... Please wait.");
                return result;
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        /**
         * When the python has finished the loading in the background
         * Call start to display the view.
         * @param result
         */
        protected void onPostExecute(JSONObject result) {
            try {
                // Start python
                if (result!=null && !result.has("error")) {
                    JSONObject json = new JSONObject();
                    json.put("method", "start");
                    result = PyBridge.call(json);
                }

                // Check and display any errors
                if (result!=null && result.has("error")) {
                    JSONObject error = result.getJSONObject("error");
                    if (error!=null) {
                        showErrorMessage(error.getString("message"));
                    }
                }
            } catch (JSONException e) {
                showErrorMessage(e);
            }
        }

        /**
         * Set error message text in loading view.
         * @param message: Message to display
         */
        protected void showErrorMessage(String message) {
            if (message!=null) {
                TextView textView = (TextView) findViewById(R.id.textView);
                textView.setTop(0);
                textView.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
                textView.setTextColor(Color.RED);
                textView.setText(message);

                // Hide progress bar
                View progressBar = (View) findViewById(R.id.progressBar);
                progressBar.setVisibility(View.INVISIBLE);
            }
        }

        /**
         * Set error message text in loading view from an exception
         * @param e: Exception to display.
         */
        protected void showErrorMessage(Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            showErrorMessage(sw.toString());
        }

        /**
         * Set the progress text to make loading not seem as slow.
         * @param status
         */
        protected void onProgressUpdate(String... status) {
            TextView textView = (TextView) findViewById(R.id.textView);
            textView.setText(status[0]);
        }
    }


    /**
     * Class
     */
    private class PythonCallback implements Runnable {

        private long mCallbackId;

        PythonCallback(long callbackId) {
            mCallbackId = callbackId;
        }

        @Override
        public void run() {
            try {
                JSONObject json = new JSONObject();
                json.put("method", "invoke");
                JSONObject params = new JSONObject();
                params.put("callback_id",mCallbackId);
                json.putOpt("params",params);
                PyBridge.call(json);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Set the content view and fade out the loading view
     * @param view
     */
    public void setView(View view) {
        mContentView.addView(view);

        // Set the content view to 0% opacity but visible, so that it is visible
        // (but fully transparent) during the animation.
        view.setAlpha(0f);
        view.setVisibility(View.VISIBLE);

        // Animate the content view to 100% opacity, and clear any animation
        // listener set on the view.
        view.animate()
            .alpha(1f)
            .setDuration(mShortAnimationDuration)
            .setListener(null);

        // Animate the loading view to 0% opacity. After the animation ends,
        // set its visibility to GONE as an optimization step (it won't
        // participate in layout passes, etc.)
        mLoadingView.animate()
            .alpha(0f)
            .setDuration(mShortAnimationDuration)
            .setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoadingView.setVisibility(View.GONE);
                }
            });

    }

    /**
     * Have android call a python callback after a given delay.
     *
     * Used so python can hook into the Activity's event loop without
     * blocking.
     *
     * @param callbackId: Python callbackId to invoke
     * @param delay
     */
    public boolean scheduleCallback(long callbackId, long delay) {
        return mCallbackHandler.postDelayed(
            new PythonCallback(callbackId),delay);
    }

    @Override
    protected void onStop() {
        super.onStop();
        PyBridge.stop();
    }
}
