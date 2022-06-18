package com.example.stripeprogressbardemo;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;

public class MainActivity extends AppCompatActivity {

    private StripeProgressBar mManualProgressBar;
    private TextView mManualTextView;
    private EditText mManualProgressEditText;
    private int mManualProgress;

    private StripeProgressBar mAutoProgressBar;
    private TextView mAutoTextView;
    private Button mAutoControlButton;
    private int mAutoProgress;

    private AutoProgressTask mAutoTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initListeners();
    }

    private void initViews() {
        mManualProgressBar = findViewById(R.id.progress_bar_manual);
        mManualTextView = findViewById(R.id.tv_manual);
        mManualTextView.setText("0 %");
        mManualProgressEditText = findViewById(R.id.et_manual_progress);

        mAutoProgressBar = findViewById(R.id.progress_bar_auto);
        mAutoTextView = findViewById(R.id.tv_auto);
        mAutoTextView.setText("0 %");

        mAutoControlButton = findViewById(R.id.btn_auto_control);
    }

    private void initListeners() {
        mManualProgressEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence == null || charSequence.toString().isEmpty()) {
                    setManualProgress(0);
                    return;
                }

                int progress = -1;
                try {
                    progress = Integer.parseInt(charSequence.toString());
                } catch (NumberFormatException ignored) { }

                if (progress < 0 || progress > 100) {
                    progress = progress < 0 ? 0 : 100;
                    mManualProgressEditText.setError(getResources().getText(R.string.et_hint_manual));
                }
                setManualProgress(progress);
            }

            @Override
            public void afterTextChanged(Editable editable) {
                updateProgress();
            }
        });

        mAutoControlButton.setOnClickListener(view -> {
            if (mAutoTask != null) {
                mAutoTask.cancel(true);
                mAutoTask = null;
                mAutoControlButton.setText(getResources().getText(R.string.button_auto_start));
            } else {
                mAutoTask = new AutoProgressTask(MainActivity.this);
                mAutoTask.execute();
                mAutoControlButton.setText(getResources().getText(R.string.button_auto_pause));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mAutoTask != null) {
            mAutoTask = new AutoProgressTask(MainActivity.this);
            mAutoTask.execute();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mAutoTask != null) {
            mAutoTask.cancel(true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mAutoTask != null) {
            mAutoTask.cancel(true);
            mAutoTask = null;
        }
    }

    public void setManualProgress(int mManualProgress) {
        this.mManualProgress = mManualProgress;
    }

    public void setAutoProgress(int mAutoProgress) {
        this.mAutoProgress = mAutoProgress;
    }

    public int getAutoProgress() {
        return this.mAutoProgress;
    }

    private void updateProgress() {
        mManualProgressBar.setProgress(mManualProgress);
        mManualTextView.setText(mManualProgress + " %");
        mAutoProgressBar.setProgress(mAutoProgress);
        mAutoTextView.setText(mAutoProgress + " %");
    }

    private static class AutoProgressTask extends AsyncTask<Void, Integer, Void> {
        private final WeakReference<Activity> mActivityRef;
        private final static long ONE_UPDATE_TIME = 1000L;

        AutoProgressTask(Activity activity) {
            mActivityRef = new WeakReference<>(activity);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            ((MainActivity) mActivityRef.get()).updateProgress();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            MainActivity activity = (MainActivity) mActivityRef.get();
            int currentProgress = activity.getAutoProgress();
            while(currentProgress < 100) {
                try {
                    Thread.sleep(ONE_UPDATE_TIME);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
                activity.setAutoProgress(++currentProgress);
                publishProgress();
                if (currentProgress >= 100) {
                    currentProgress = 0;
                }
            }
            return null;
        }
    }
}