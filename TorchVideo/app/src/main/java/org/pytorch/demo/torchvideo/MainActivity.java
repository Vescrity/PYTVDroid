package org.pytorch.demo.torchvideo;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements Runnable {
    private static String[] mClasses;
    private final String TAG = MainActivity.class.getSimpleName();
    private final String[] mTestVideos = {"video1"};
    private final int REQUEST_SETTING = 0x5ee;
    private Button mButtonPauseResume;
    private Button mButtonTest;
    private Module mModule = null;
    private int mTestVideoIndex = 0;
    private List<String> mResults = new ArrayList<>();
    private VideoView mVideoView;
    private TextView mTextView;
    private Button buttonSettings;
    private TextView ptlPathText;
    private Uri mVideoUri;
    private Thread mThread;
    private boolean mStopThread;
    private int lastIndex = 0;
    private Config mConfig;

    public static String assetFilePath(Context context, String assetName) throws IOException {
        File file = new File(context.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }

        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        }
    }

    public static String[] getClasses() {
        return mClasses;
    }
    private void updateConfig() {
        try {
            mConfig = Config.getInstance();
            mModule = LiteModuleLoader.load(mConfig.ptlPath);
            BufferedReader br = new BufferedReader(new InputStreamReader(Files.newInputStream(new File(mConfig.classPath).toPath())));
            String line;
            List<String> classes = new ArrayList<>();
            while ((line = br.readLine()) != null) {
                classes.add(line);
            }
            mClasses = new String[classes.size()];
            classes.toArray(mClasses);

            mTextView.setVisibility(View.INVISIBLE);
            buttonSettings.setText(getString(R.string.settings));
            ptlPathText.setText(mConfig.ptlPath);

        } catch (IOException e) {
            Log.e(TAG, "Error reading model file", e);
            finish();
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        buttonSettings = findViewById(R.id.settingButton);
        ptlPathText = findViewById(R.id.pathText);
        mButtonTest = findViewById(R.id.testButton);
        mTextView = findViewById(R.id.vibTimeText);
        ConfigManager.loadConfig(getApplicationContext());
        mConfig = Config.getInstance();
        try {
            if(Objects.equals(mConfig.classPath, "Default"))
                mConfig.classPath = MainActivity.assetFilePath(getApplicationContext(), Constants.CLASSES_TXT);
            if(Objects.equals(mConfig.ptlPath, "Default"))
                mConfig.ptlPath = MainActivity.assetFilePath(getApplicationContext(), Constants.PTL_FILE);

        } catch (IOException e) {
            Log.e(TAG, "Error reading model file", e);
            finish();
        }

        updateConfig();

        mButtonTest.setText(String.format("测试 1/%d", mTestVideos.length));
        mButtonTest.setEnabled(false);
        mButtonTest.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mTestVideoIndex = (mTestVideoIndex + 1) % mTestVideos.length;
                mButtonTest.setText(String.format("测试 %d/%d", mTestVideoIndex + 1, mTestVideos.length));
                mButtonTest.setEnabled(false);
                mTextView.setText("");
                mTextView.setVisibility(View.INVISIBLE);
                mStopThread = true;
                mVideoUri = getMedia(mTestVideos[mTestVideoIndex]);
                startVideo();
            }
        });


        mButtonPauseResume = findViewById(R.id.pauseResumeButton);
        mButtonPauseResume.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mVideoView.isPlaying()) {
                    mButtonPauseResume.setText(getString(R.string.resume));
                    mVideoView.pause();
                } else {
                    mButtonPauseResume.setText(getString(R.string.pause));
                    mVideoView.start();
                }
            }
        });

        final Button buttonSelect = findViewById(R.id.selectButton);
        buttonSelect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mStopThread = true;
                Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                pickIntent.setType("video/*");
                startActivityForResult(pickIntent, 1);
            }
        });

        final Button buttonLive = findViewById(R.id.liveButton);
        buttonLive.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mStopThread = true;
                final Intent intent = new Intent(MainActivity.this, LiveVideoClassificationActivity.class);
                startActivity(intent);
            }
        });

        buttonSettings.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mStopThread = true;
                final Intent intent = new Intent(MainActivity.this, Settings.class);
                startActivityForResult(intent, REQUEST_SETTING);
            }
        });

        mVideoView = findViewById(R.id.videoView);
        mVideoUri = getMedia(mTestVideos[mTestVideoIndex]);
        startVideo();
    }

    private void startVideo() {
        mVideoView.setVideoURI(mVideoUri);
        mVideoView.start();
        mButtonPauseResume.setVisibility(View.VISIBLE);
        mButtonPauseResume.setText(getString(R.string.pause));

        if (mThread != null && mThread.isAlive()) {
            try {
                mThread.join();
            } catch (InterruptedException e) {
                Log.e(TAG, e.getLocalizedMessage());
            }
        }

        mStopThread = false;
        mThread = new Thread(MainActivity.this);
        mThread.start();
    }

    private void stopVideo() {
        mVideoView.stopPlayback();
        mButtonPauseResume.setVisibility(View.INVISIBLE);
        mButtonTest.setEnabled(true);
        mStopThread = true;
    }

    private Uri getMedia(String mediaName) {
        return Uri.parse("android.resource://" + getPackageName() + "/raw/" + mediaName);
    }

    @Override
    protected void onPause() {
        super.onPause();

        mVideoView.pause();
    }
    @Override
    protected void onResume() {
        super.onResume();
        updateConfig();
    }

    @Override
    protected void onStop() {
        super.onStop();
        ConfigManager.saveConfig(mConfig, getApplicationContext());

        stopVideo();
    }

    @Override
    public void run() {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(this.getApplicationContext(), mVideoUri);
        String stringDuration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        double durationMs = Double.parseDouble(stringDuration);

        // 定义更新间隔（例如每200ms）
        final int UPDATE_INTERVAL_MS = 500; // 可配置为参数
        mResults.clear();

        // 按固定间隔处理（而非按秒）
        for (int currentTimeMs = 0; !mStopThread && currentTimeMs < durationMs; currentTimeMs += UPDATE_INTERVAL_MS) {
            int from = currentTimeMs;
            int to = Math.min(currentTimeMs + UPDATE_INTERVAL_MS, (int) durationMs);

            // 获取当前时间段的推理结果
            final Pair<Integer[], Long> pair = getResult(from, to, mmr);
            final Integer[] scoresIdx = pair.first;
            String tops[] = new String[Constants.TOP_COUNT];
            for (int j = 0; j < Constants.TOP_COUNT; j++)
                tops[j] = mClasses[scoresIdx[j]];
            final String result = tops[0]; // 取最高概率结果
            final long inferenceTime = pair.second;

            // 同步视频播放进度
            if (currentTimeMs > mVideoView.getCurrentPosition()) {
                try {
                    Thread.sleep(currentTimeMs - mVideoView.getCurrentPosition());
                } catch (InterruptedException e) {
                    Log.e(TAG, "Thread sleep exception: " + e.getLocalizedMessage());
                }
            }

            // 等待视频播放
            while (!mVideoView.isPlaying()) {
                if (mStopThread || (mVideoView.getCurrentPosition() >= mVideoView.getDuration()))
                    break;
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Log.e(TAG, "Thread sleep exception: " + e.getLocalizedMessage());
                }
            }

            // 更新UI
            final int finalCurrentTimeMs = currentTimeMs;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mTextView.setVisibility(View.VISIBLE);
                    int currentIndex = scoresIdx[0];
                    if(lastIndex != currentIndex && mConfig.enableVibration)
                        BaseUtils.vibrate(getApplicationContext(), mConfig.vibrationTime);
                    mTextView.setBackgroundColor(Constants.COLOR_LIST[scoresIdx[0]]);
                    lastIndex = scoresIdx[0];
                    mTextView.setText(
                            String.format("%.2fs: %s (Cost: %dms)",
                                    finalCurrentTimeMs / 1000.0, result, inferenceTime));
                }
            });
            mResults.add(result);
        }

        // 视频播放完成后的处理
        if (!mStopThread) {
            runOnUiThread(() -> mButtonPauseResume.setVisibility(View.INVISIBLE));
            runOnUiThread(() -> mButtonTest.setEnabled(true));
        }

    }

    private Pair<Integer[], Long> getResult(int fromMs, int toMs, MediaMetadataRetriever mmr) {

        // 1. 定义跳帧间隔（例如每隔5帧处理1帧）
        final int FRAME_SKIP_INTERVAL = 60; // 可配置为参数
        int processedFrames = 0;
        int totalFrames = 0;

        // 2. 分配单帧的缓冲区（不再是多帧合并）
        FloatBuffer inTensorBuffer = Tensor.allocateFloatBuffer(3 * Constants.TARGET_VIDEO_SIZE * Constants.TARGET_VIDEO_SIZE);

        // 3. 初始化结果存储（假设模型输出类别数固定）
        float[] accumulatedScores = new float[Constants.NUM_CLASSES]; // 需定义常量
        Arrays.fill(accumulatedScores, 0f);

        final long startTime = SystemClock.elapsedRealtime();

        // 4. 遍历视频帧，按间隔处理
        for (long timeUs = fromMs * 1000; timeUs <= toMs * 1000; timeUs += (FRAME_SKIP_INTERVAL * 1000000 / 30)) { // 假设30fps
            Bitmap bitmap = mmr.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
            if (bitmap == null) continue;

            // 5. 单帧预处理（缩放+裁剪）
            float ratio = Math.min(bitmap.getWidth(), bitmap.getHeight()) / (float) Constants.TARGET_VIDEO_SIZE;
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap,
                    (int) (bitmap.getWidth() / ratio),
                    (int) (bitmap.getHeight() / ratio),
                    true);
            Bitmap centerCroppedBitmap = Bitmap.createBitmap(
                    resizedBitmap,
                    resizedBitmap.getWidth() > resizedBitmap.getHeight() ? (resizedBitmap.getWidth() - resizedBitmap.getHeight()) / 2 : 0,
                    resizedBitmap.getHeight() > resizedBitmap.getWidth() ? (resizedBitmap.getHeight() - resizedBitmap.getWidth()) / 2 : 0,
                    Constants.TARGET_VIDEO_SIZE, Constants.TARGET_VIDEO_SIZE
            );

            // 6. 转换当前帧为Tensor输入
            TensorImageUtils.bitmapToFloatBuffer(
                    centerCroppedBitmap, 0, 0,
                    Constants.TARGET_VIDEO_SIZE, Constants.TARGET_VIDEO_SIZE,
                    Constants.MEAN_RGB, Constants.STD_RGB,
                    inTensorBuffer, 0 // 单帧无偏移
            );

            Tensor inputTensor = Tensor.fromBlob(inTensorBuffer, new long[]{1, 3, Constants.TARGET_VIDEO_SIZE, Constants.TARGET_VIDEO_SIZE});

            // 7. 单帧推理
            Tensor outputTensor = mModule.forward(IValue.from(inputTensor)).toTensor();
            float[] frameScores = outputTensor.getDataAsFloatArray();

            // 8. 累计结果（可选：取平均或最大值）
            for (int i = 0; i < frameScores.length; i++) {
                accumulatedScores[i] += frameScores[i]; // 或 Math.max(accumulatedScores[i], frameScores[i])
            }

            processedFrames++;
            totalFrames++;
        }

        final long inferenceTime = SystemClock.elapsedRealtime() - startTime;

        // 9. 计算平均得分（如果累计方式为求和）
        if (processedFrames > 0) {
            for (int i = 0; i < accumulatedScores.length; i++) {
                accumulatedScores[i] /= processedFrames;
            }
        }

        // 10. 排序结果（与原逻辑一致）
        Integer scoresIdx[] = new Integer[accumulatedScores.length];
        for (int i = 0; i < scoresIdx.length; i++) scoresIdx[i] = i;
        Arrays.sort(scoresIdx, (o1, o2) -> Float.compare(accumulatedScores[o2], accumulatedScores[o1]));

        return new Pair<>(scoresIdx, inferenceTime);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == 1 && data != null) {
            Uri selectedMediaUri = data.getData();
            if (selectedMediaUri.toString().contains("video")) {
                mVideoUri = selectedMediaUri;
            }
        }
        startVideo();

    }
}