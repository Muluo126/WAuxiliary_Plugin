import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.File;

import me.hd.wauxv.plugin.api.callback.PluginCallBack;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONPath;
import com.alibaba.fastjson2.JSONArray;

/**
 * 抖音视频无水印下载
 * 适配纯文本 URL 返回的接口
 */

public static String regex = "https://v\\.douyin\\.com/[^\\s/]+/?";
public static Pattern pattern = Pattern.compile(regex);

//是否开启群聊抖音视频下载
boolean isOpenGroup = true;

void sendDouyinVideo(String talker, String douyinUrl) {
    try {
        String apiUrl = "https://muluo123.online/?url=" + douyinUrl;
        
        get(apiUrl, null, new PluginCallBack.HttpCallback() {
            public void onSuccess(int respCode, String respContent) {
                if (respCode != 200) {
                    sendText(talker, "[抖音解析] 网络请求失败，状态码:" + respCode);
                    return;
                }

                // 去除字符串前后的空格或换行符
                String resultData = respContent.trim();

                // 【核心修改】：如果返回的是直接的 http 链接，直接走下载逻辑，跳过 JSON 解析
                if (resultData.startsWith("http")) {
                    // 使用时间戳作为文件名，防止重复
                    String fileName = "dy_video_" + System.currentTimeMillis() + ".mp4";
                    try {
                        download(resultData, cacheDir + "/" + fileName, null, new PluginCallBack.DownloadCallback() {
                            public void onSuccess(File file) {
                                sendVideo(talker, file.getAbsolutePath());
                            }
                            public void onFailure(Exception e) {
                                sendText(talker, "[抖音解析] 视频下载失败:" + e.getMessage());
                            }
                        });
                    } catch (java.lang.Exception e) {
                        sendText(talker, "下载异常:" + e.getMessage());
                    }
                    return; // 视频处理完毕，直接结束
                }

                // 如果返回的不是纯链接，再尝试当作 JSON 解析（保留图文解析的后路）
                try {
                    JSONObject jsonObj = JSON.parseObject(resultData);
                    
                    if (jsonObj == null || !jsonObj.containsKey("type")) {
                        sendText(talker, "[抖音解析] 未知的数据格式:\n" + resultData);
                        return;
                    }

                    String vType = jsonObj.getString("type");
                    String awemeId = jsonObj.getString("aweme_id");

                    if ("video".equals(vType)) {
                        String vUrl = jsonObj.getString("video_url");
                        try {
                            download(vUrl, cacheDir + "/" + awemeId + ".mp4", null, new PluginCallBack.DownloadCallback() {
                                public void onSuccess(File file) {
                                    sendVideo(talker, file.getAbsolutePath());
                                }
                                public void onFailure(Exception e) {
                                    sendText(talker, "[抖音解析] 视频下载失败:" + e.getMessage());
                                }
                            });
                        } catch (java.lang.Exception e) {
                            sendText(talker, "下载异常:" + e.getMessage());
                        }
                    } else {
                        toast("图文消息");
                        JSONArray imageUrlList = jsonObj.getJSONArray("image_url_list");
                        if (imageUrlList != null) {
                            for (int i = 0; i < imageUrlList.size(); i++) {
                                try {
                                    download(imageUrlList.getString(i), cacheDir + "/img_" + awemeId + "_" + i + ".jpg", null, new PluginCallBack.DownloadCallback() {
                                        public void onSuccess(File file) {
                                            sendImage(talker, file.getAbsolutePath());
                                        }
                                        public void onFailure(Exception e) {
                                            sendText(talker, "[抖音解析] 图片下载失败:" + e.getMessage());
                                        }
                                    });
                                } catch (java.lang.Exception e) {
                                    sendText(talker, "图文下载异常:" + e.getMessage());
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    sendText(talker, "[抖音解析] 接口返回了无法识别的内容，既不是直链也不是JSON。");
                }
            }
            
            public void onFailure(Exception e) {
                sendText(talker, "[抖音解析] 网络请求异常，请检查网络连接:" + e.getMessage());
            }
        });
    } catch (Exception e) {
        sendText(talker, "[抖音解析] 请求解析异常:" + e.getMessage());
    }
}

void onHandleMsg(Object msgInfoBean) {
    if (msgInfoBean.isText()) {
        String content = msgInfoBean.getContent();
        String talker = msgInfoBean.getTalker();
        if (!msgInfoBean.isGroupChat() || isOpenGroup) {
            Matcher matcher = pattern.matcher(content);
            if (matcher.find()) {
                sendDouyinVideo(talker, matcher.group());
            }
        }
    }
}

boolean onClickSendBtn(String text) {
    Matcher matcher = pattern.matcher(text);
    if (matcher.find()) {
        sendDouyinVideo(getTargetTalker(), matcher.group());
        return true;
    }
    return false;
}
