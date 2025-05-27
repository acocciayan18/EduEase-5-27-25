package com.example.eduease;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class BaseActivity extends AppCompatActivity {
    private LoadingDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadingDialog = new LoadingDialog(this);
    }

    public void showLoading() {
        if (loadingDialog != null) loadingDialog.show();
    }

    public void hideLoading() {
        if (loadingDialog != null) loadingDialog.dismiss();
    }
}
