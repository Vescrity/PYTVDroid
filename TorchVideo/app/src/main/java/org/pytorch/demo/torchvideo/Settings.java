package org.pytorch.demo.torchvideo;


import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.VideoView;
import android.content.Context;
import android.database.Cursor;

import android.provider.MediaStore;

import org.w3c.dom.Text;
import java.io.File;
public class Settings extends AppCompatActivity {
    private static final int FILE_SELECT_CODE = 0xbed;
    private static final int CLASS_FILE_SELECT_CODE = 0xced;
    private Config mConfig;
    private void updateUI(){
        mConfig = Config.getInstance();

        final TextView mPtlPath = findViewById(R.id.ptlPath);
        mPtlPath.setText(mConfig.ptlPath);
        final TextView mClassPath = findViewById(R.id.classPath);
        mClassPath.setText(mConfig.classPath);
        final Switch mVibration = findViewById(R.id.vibrationSwitch);
        mVibration.setChecked(mConfig.enableVibration);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);
        findViewById(R.id.ptlFileButton).setOnClickListener(v -> openFileChooser());
        findViewById(R.id.classFileButton).setOnClickListener(v -> openClassFileChooser());
        findViewById(R.id.returnButton).setOnClickListener(v -> finish());
        updateUI();

    }
    @Override
    protected void onResume(){
        super.onResume();
        updateUI();
    }
    @Override
    protected void onStop(){
        super.onStop();
        final Switch mVibration = findViewById(R.id.vibrationSwitch);
        mConfig.enableVibration = mVibration.isChecked();
    }

    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "选择文件"), FILE_SELECT_CODE);
    }
    private void openClassFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("text/plain");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "选择文件"), CLASS_FILE_SELECT_CODE);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_SELECT_CODE && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            String filePath = GetFilePathFromUri.getFileAbsolutePath(getApplicationContext(), uri);
            /*File file = new File(getApplicationContext().getFilesDir(), String.valueOf(uri));
            String filePath = file.getAbsolutePath();*/
            Config.getInstance().ptlPath = filePath;
            Intent resultIntent = new Intent();
            resultIntent.setData(Uri.parse(filePath));
            setResult(RESULT_OK, resultIntent);
        }
        else if (requestCode == CLASS_FILE_SELECT_CODE && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            String filePath = GetFilePathFromUri.getFileAbsolutePath(getApplicationContext(), uri);

            Config.getInstance().classPath = filePath;
            Intent resultIntent = new Intent();
            resultIntent.setData(Uri.parse(filePath));
            setResult(RESULT_OK, resultIntent);
        }
    }

}
