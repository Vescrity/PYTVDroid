package org.pytorch.demo.torchvideo;


import android.content.Context;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;


public class ConfigManager {
    private static final String FILE_NAME = "config.dat";

    // 保存配置到文件
    public static void saveConfig(Config config, Context context) {
        /*TODO*/
        return;
        /*try (FileOutputStream fos = context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(config);
        } catch (IOException e) {
            e.printStackTrace();
        }*/
    }

    // 从文件加载配置
    public static void loadConfig(Context context) {
        /*TODO*/
        return;
        /*Config config = Config.getInstance();
        try (FileInputStream fis = context.openFileInput(FILE_NAME);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
             config = (Config) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return;*/
    }
}