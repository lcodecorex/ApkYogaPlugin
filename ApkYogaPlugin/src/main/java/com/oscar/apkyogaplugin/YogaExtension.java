package com.oscar.apkyogaplugin;

import org.gradle.api.Action;

public class YogaExtension {
    public static final String COMPRESS_TYPE_LOSSY = "lossy"; // 有损压缩
    public static final String COMPRESS_TYPE_LOSSLESS = "lossless"; // 无损压缩

    public boolean enable = true; // 是否开启ApkYoga功能
    public boolean enableOnDebug = true; // 是否在调试时开启
    public WebpConvertConfig webpConvertConfig = new WebpConvertConfig(); // 图片转webp的配置
    public ImageCompressConfig imageCompressConfig = new ImageCompressConfig(); // 图片压缩配置
    public RemoveResourceConfig removeResourceConfig = new RemoveResourceConfig(); // 删除资源的配置
    public WebCompressConfig webCompressConfig = new WebCompressConfig(); // web文件资源压缩配置
    public RemoveApkResourceConfig removeApkResourceConfig = new RemoveApkResourceConfig(); // 删除apk zip包下文件的配置

    public void enable(boolean enable) {
        this.enable = enable;
    }

    public void enableOnDebug(boolean enableOnDebug) {
        this.enableOnDebug = enableOnDebug;
    }

    public void webpConvertConfig(Action<WebpConvertConfig> action) {
        action.execute(webpConvertConfig);
    }

    public void imageCompressConfig(Action<ImageCompressConfig> action) {
        action.execute(imageCompressConfig);
    }

    public void removeResourceConfig(Action<RemoveResourceConfig> action) {
        action.execute(removeResourceConfig);
    }

    public void webCompressConfig(Action<WebCompressConfig> action) {
        action.execute(webCompressConfig);
    }

    public void removeApkResourceConfig(Action<RemoveApkResourceConfig> action) {
        action.execute(removeApkResourceConfig);
    }

    class WebpConvertConfig {
        public boolean enable = true;  // 是否允许图片转webp功能
        public boolean auto = true; // 是否在gradle build时启动此功能
        public String type = COMPRESS_TYPE_LOSSY; // webp压缩类型
        public boolean supportAlpha = true; // 是否支持透明
        public int quality = 75; // webp画质
        public String[] whiteList = new String[]{};
        public String[] blackList = new String[]{};

        public void enable(boolean enable) {
            this.enable = enable;
        }

        public void auto(boolean auto) {
            this.auto = auto;
        }

        public void type(String type) {
            this.type = type;
        }

        public void supportAlpha(boolean supportAlpha) {
            this.supportAlpha = supportAlpha;
        }

        public void quality(int quality) {
            this.quality = quality;
        }

        public void whiteList(String[] whiteList) {
            this.whiteList = whiteList;
        }

        public void blackList(String[] blackList) {
            this.blackList = blackList;
        }
    }

    class ImageCompressConfig {
        public boolean enable = true; // 是否开启图片资源压缩功能
        public boolean auto = true; // 是否在gradle build时启用此功能
        public String type = COMPRESS_TYPE_LOSSY; // 压缩类型
        public int jpegQuality = 84; // jpeg压缩画质
        public int pngQuality = 80; // png压缩画质
        public String[] whiteList = new String[]{};
        public String[] blackList = new String[]{};

        public void enable(boolean enable) {
            this.enable = enable;
        }

        public void auto(boolean auto) {
            this.auto = auto;
        }

        public void type(String type) {
            this.type = type;
        }

        public void jpegQuality(int jpegQuality) {
            this.jpegQuality = jpegQuality;
        }

        public void pngQuality(int pngQuality) {
            this.pngQuality = pngQuality;
        }

        public void whiteList(String[] whiteList) {
            this.whiteList = whiteList;
        }

        public void blackList(String[] blackList) {
            this.blackList = blackList;
        }
    }

    class RemoveResourceConfig {
        public boolean enable = true;
        public boolean removeDuplicate = true; // 移除重复资源
        public String[] blackList = new String[]{}; // 删除不想要的资源

        public void enable(boolean enable) {
            this.enable = enable;
        }

        public void removeDuplicate(boolean removeDuplicate) {
            this.removeDuplicate = removeDuplicate;
        }

        public void blackList(String[] blackList) {
            this.blackList = blackList;
        }
    }

    class WebCompressConfig {
        public boolean enable = true;
        public boolean html = true; // 压缩html
        public boolean js = true; // 压缩js
        public boolean css = true; // 压缩css

        public void enable(boolean enable) {
            this.enable = enable;
        }

        public void html(boolean html) {
            this.html = html;
        }

        public void js(boolean js) {
            this.js = js;
        }

        public void css(boolean css) {
            this.css = css;
        }
    }

    class RemoveApkResourceConfig {
        public boolean enable = true;
        public String[] whiteList = new String[]{};
        public String[] blackList = new String[]{};

        public void enable(boolean enable) {
            this.enable = enable;
        }

        public void whiteList(String[] whiteList) {
            this.whiteList = whiteList;
        }

        public void blackList(String[] blackList) {
            this.blackList = blackList;
        }
    }
}