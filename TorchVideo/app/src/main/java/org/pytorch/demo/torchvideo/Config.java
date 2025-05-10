package org.pytorch.demo.torchvideo;





public class Config {
    private static Config instance;
    public String ptlPath = "Default";
    public String classPath = "Default";
    public boolean enableVibration = true;
    public int vibrationTime = 500;

    private Config() {}

    // 获取单例实例
    public static synchronized Config getInstance() {
        if (instance == null) {
            instance = new Config();
        }
        return instance;
    }

    public void someMethod() {
        // 方法实现
    }
}
