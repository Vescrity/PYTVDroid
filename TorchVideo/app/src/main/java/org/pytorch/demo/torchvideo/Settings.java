package org.pytorch.demo.torchvideo;

import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import androidx.appcompat.app.AppCompatActivity;

public class Settings extends AppCompatActivity {
    private static final int FILE_SELECT_CODE = 0xbed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);
        findViewById(R.id.ptlFileButton).setOnClickListener(v -> openFileChooser());
        findViewById(R.id.returnButton).setOnClickListener(v -> finish());

    }
    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/octet-stream"); // 选择任意文件
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "选择文件"), FILE_SELECT_CODE);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_SELECT_CODE && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            Intent resultIntent = new Intent();
            resultIntent.setData(uri);
            setResult(RESULT_OK, resultIntent);
            finish(); // 返回到 MainActivity
        }
    }
}
