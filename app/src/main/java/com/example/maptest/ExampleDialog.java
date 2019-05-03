package com.example.maptest;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v7.app.AppCompatDialogFragment;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;

public class ExampleDialog extends AppCompatDialogFragment {
    private boolean ok=false;
    Vibrator vibrator;


    @Override


    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            //    listener= (ExampleDialogListener) context();
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()+"Must implement exampleDialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        final long[] pattern={100,200};
        vibrator.vibrate(pattern, 0);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater=getActivity().getLayoutInflater();
        View view=inflater.inflate(R.layout.layout_dialog,null);
        builder.setView(view).setTitle("ALARM").setPositiveButton("Okay", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ok=true;
                vibrator.cancel();
                //   listener.clickedOk(true);
            }
        }).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        Vibrator vibrator;
        return builder.create();
    }




}
