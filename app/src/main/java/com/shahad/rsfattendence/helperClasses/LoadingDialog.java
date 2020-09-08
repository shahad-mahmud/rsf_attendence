package com.shahad.rsfattendence.helperClasses;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.shahad.rsfattendence.R;

/*
 * Created by SHAHAD MAHMUD on 7/23/20
 */
public class LoadingDialog {
    private AlertDialog dialogs;
    private Activity activity;


    public LoadingDialog(Activity activity) {
        this.activity = activity;
    }

    public void startLoadingDialog(String message) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);

        LayoutInflater inflater = activity.getLayoutInflater();
        View view = inflater.inflate(R.layout.design_loading_dialog, null);

        TextView loadingText = view.findViewById(R.id.loading_text);
        loadingText.setText(message);

        builder.setView(view);
        builder.setCancelable(false);

        dialogs = builder.create();
        dialogs.show();
    }

    public void dismissLoadingDialog() {
        dialogs.dismiss();
    }

    public boolean isShowing() {
        return dialogs.isShowing();
    }

    public Context getContext() {
        return dialogs.getContext();
    }

}
