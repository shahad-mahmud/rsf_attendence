package com.shahad.rsfattendence.helperClasses;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.shahad.rsfattendence.R;

/*
 * Created by SHAHAD MAHMUD on 7/23/20
 */
public class IconDialog {
    private AlertDialog dialogs;
    private Activity activity;


    public IconDialog(Activity activity) {
        this.activity = activity;
    }

    public void startIconDialog(String message, int iconResource) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);

        LayoutInflater inflater = activity.getLayoutInflater();
        View view = inflater.inflate(R.layout.design_icon_text_dialog, null);

        TextView testView = view.findViewById(R.id.icon_dialog_text);
        ImageView iconView = view.findViewById(R.id.icon_dialog_icon);
        Button button = view.findViewById(R.id.icon_dialog_button);

        testView.setText(message);
        iconView.setImageResource(iconResource);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogs.dismiss();
            }
        });

        builder.setView(view);
        builder.setCancelable(true);

        dialogs = builder.create();
        dialogs.show();
    }

    public void dismissIconDialog() {
        dialogs.dismiss();
    }
}
