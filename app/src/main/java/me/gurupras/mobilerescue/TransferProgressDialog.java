package me.gurupras.mobilerescue;

import android.app.Activity;
import android.app.ProgressDialog;

public class TransferProgressDialog extends ProgressDialog {
    private long max;
    private long progress;

    public TransferProgressDialog (Activity activity) {
        super(activity);
    }

    public void setProgress(long value) {
        progress = value;
        int superProgress = (int) ((progress * 100) / max);
        super.setProgress(superProgress);
    }

    public int getProgress() {
        return super.getProgress();
    }

    public void setMax(long max) {
        this.max = max;
        super.setMax(100);
    }
}