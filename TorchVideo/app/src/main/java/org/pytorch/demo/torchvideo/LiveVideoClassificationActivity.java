package org.pytorch.demo.torchvideo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.SystemClock;
import android.util.Log;
import android.view.TextureView;
import android.view.ViewStub;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.camera.core.ImageProxy;

import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.PyTorchAndroid;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.Comparator;


public class LiveVideoClassificationActivity extends AbstractCameraXActivity<LiveVideoClassificationActivity.AnalysisResult> {
        private Module mModule = null;
        private TextView mResultView;
        private int mFrameCount = 0;
        private int mProcessedFrames = 0;
        private float[] mAccumulatedScores;
        private FloatBuffer inTensorBuffer;


    static class AnalysisResult {
            private final String mResults;
            private int mId = 0;

            public AnalysisResult(String results) {
                mResults = results;
            }
            public AnalysisResult(String results, int id) {
                mResults = results;
                mId = id;
            }
        }

        @Override
        protected int getContentViewLayoutId() {
            return R.layout.activity_live_video_classification;
        }

        @Override
        protected TextureView getCameraPreviewTextureView() {
            mResultView = findViewById(R.id.resultView);
            return ((ViewStub) findViewById(R.id.object_detection_texture_view_stub))
                    .inflate()
                    .findViewById(R.id.object_detection_texture_view);
        }

        @Override
        protected void applyToUiAnalyzeImageResult(AnalysisResult result) {
            mResultView.setText(result.mResults);
            mResultView.setBackgroundColor(Constants.COLOR_LIST[result.mId]);
            mResultView.invalidate();
        }

        private Bitmap imgToBitmap(Image image) {
            Image.Plane[] planes = image.getPlanes();
            ByteBuffer yBuffer = planes[0].getBuffer();
            ByteBuffer uBuffer = planes[1].getBuffer();
            ByteBuffer vBuffer = planes[2].getBuffer();

            int ySize = yBuffer.remaining();
            int uSize = uBuffer.remaining();
            int vSize = vBuffer.remaining();

            byte[] nv21 = new byte[ySize + uSize + vSize];
            yBuffer.get(nv21, 0, ySize);
            vBuffer.get(nv21, ySize, vSize);
            uBuffer.get(nv21, ySize + vSize, uSize);

            YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 75, out);

            byte[] imageBytes = out.toByteArray();
            return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        }


        @Override
        @WorkerThread
        @Nullable
        protected AnalysisResult analyzeImage(ImageProxy image, int rotationDegrees) {
            // 1. 初始化模型和成员变量
            if (mModule == null) {
                try {
                    mModule = LiteModuleLoader.load(MainActivity.assetFilePath(
                            getApplicationContext(), Constants.PTL_FILE));
                } catch (IOException e) {
                    return null;
                }
                mFrameCount = 0;
                mAccumulatedScores = new float[Constants.NUM_CLASSES]; // 需要定义常量
            }

            // 2. 跳帧逻辑（每 FRAME_SKIP_INTERVAL 帧处理1帧）
            final int FRAME_SKIP_INTERVAL = 10; // 可配置参数
            if (mFrameCount++ % FRAME_SKIP_INTERVAL != 0) {
                //image.close();
                return null;
            }

            // 3. 单帧预处理
            Bitmap bitmap = imgToBitmap(image.getImage());
            try {
                // 图像旋转调整
                Matrix matrix = new Matrix();
                matrix.postRotate(90.0f);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0,
                        bitmap.getWidth(), bitmap.getHeight(), matrix, true);

                // 缩放和中心裁剪
                float ratio = Math.min(bitmap.getWidth(), bitmap.getHeight()) /
                        (float) Constants.TARGET_VIDEO_SIZE;
                Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap,
                        (int)(bitmap.getWidth() / ratio),
                        (int)(bitmap.getHeight() / ratio), true);

                Bitmap centerCroppedBitmap = Bitmap.createBitmap(resizedBitmap,
                        resizedBitmap.getWidth() > resizedBitmap.getHeight() ?
                                (resizedBitmap.getWidth() - resizedBitmap.getHeight()) / 2 : 0,
                        resizedBitmap.getHeight() > resizedBitmap.getWidth() ?
                                (resizedBitmap.getHeight() - resizedBitmap.getWidth()) / 2 : 0,
                        Constants.TARGET_VIDEO_SIZE, Constants.TARGET_VIDEO_SIZE);

                // 4. 转换到Tensor输入
                FloatBuffer inTensorBuffer = Tensor.allocateFloatBuffer(
                        3 * Constants.TARGET_VIDEO_SIZE * Constants.TARGET_VIDEO_SIZE);
                TensorImageUtils.bitmapToFloatBuffer(
                        centerCroppedBitmap, 0, 0,
                        Constants.TARGET_VIDEO_SIZE, Constants.TARGET_VIDEO_SIZE,
                        Constants.MEAN_RGB, Constants.STD_RGB,
                        inTensorBuffer, 0);

                Tensor inputTensor = Tensor.fromBlob(inTensorBuffer,
                        new long[]{1, 3, Constants.TARGET_VIDEO_SIZE, Constants.TARGET_VIDEO_SIZE});

                // 5. 单帧推理
                final long startTime = SystemClock.elapsedRealtime();
                Tensor outputTensor = mModule.forward(IValue.from(inputTensor)).toTensor();
                final float[] frameScores = outputTensor.getDataAsFloatArray();

                // 6. 累计分数（最大值策略）
                for (int i = 0; i < mAccumulatedScores.length; i++) {
                    mAccumulatedScores[i] = Math.max(mAccumulatedScores[i], frameScores[i]);
                }

                // 7. 每处理N帧后返回结果（例如每秒更新一次）
                final int FRAMES_PER_RESULT = 1;
                if (mProcessedFrames++ % FRAMES_PER_RESULT == 0) {
                    Integer scoresIdx[] = new Integer[mAccumulatedScores.length];
                    for (int i = 0; i < scoresIdx.length; i++) scoresIdx[i] = i;

                    Arrays.sort(scoresIdx, (o1, o2) ->
                            Float.compare(mAccumulatedScores[o2], mAccumulatedScores[o1]));

                    String tops[] = new String[Constants.TOP_COUNT];
                    for (int j = 0; j < Constants.TOP_COUNT; j++)
                        tops[j] = MainActivity.getClasses()[scoresIdx[j]];

                    // 重置累计结果
                    Arrays.fill(mAccumulatedScores, 0f);

                    return new AnalysisResult(
                            String.format("Status: %s (Cost: %dms)",
                                    tops[0],SystemClock.elapsedRealtime() - startTime),
                                    scoresIdx[0]);
                }
            }
            finally {
                // 8. 资源清理
                if (bitmap != null) bitmap.recycle();
                // uncomment it will cause crash
                //image.close();
            }
            return null;
        }
    }
