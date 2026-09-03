package com.meteorsss.tvdisplayinfo;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity implements DisplayManager.DisplayListener {

    private static final int COLOR_BG = Color.rgb(7, 17, 31);
    private static final int COLOR_CARD = Color.rgb(15, 30, 49);
    private static final int COLOR_TITLE = Color.rgb(226, 232, 240);
    private static final int COLOR_TEXT = Color.rgb(203, 213, 225);
    private static final int COLOR_MUTED = Color.rgb(148, 163, 184);
    private static final int COLOR_BLUE = Color.rgb(56, 189, 248);
    private static final int COLOR_GREEN = Color.rgb(52, 211, 153);
    private static final int COLOR_YELLOW = Color.rgb(251, 191, 36);

    private LinearLayout content;
    private TextView statusText;
    private DisplayManager displayManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        );
        buildUi();
        displayManager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
        refreshDisplayInfo();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (displayManager != null) {
            displayManager.registerDisplayListener(this, null);
        }
    }

    @Override
    protected void onStop() {
        if (displayManager != null) {
            displayManager.unregisterDisplayListener(this);
        }
        super.onStop();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(42), dp(28), dp(42), dp(28));
        root.setBackgroundColor(COLOR_BG);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.VERTICAL);

        TextView title = text("电视屏幕与 HDR 检测", 28, COLOR_TITLE, true);
        TextView subtitle = text("读取系统公开显示 API 报告的物理输出模式，而不是只看 UI 渲染尺寸", 15, COLOR_MUTED, false);
        heading.addView(title);
        heading.addView(subtitle, marginTop(dp(4)));
        header.addView(heading, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Button refresh = new Button(this);
        refresh.setText("重新检测");
        refresh.setTextSize(16);
        refresh.setTextColor(Color.WHITE);
        refresh.setAllCaps(false);
        refresh.setFocusable(true);
        refresh.setPadding(dp(24), dp(8), dp(24), dp(8));
        refresh.setOnClickListener(v -> refreshDisplayInfo());
        header.addView(refresh, new LinearLayout.LayoutParams(dp(170), dp(54)));

        root.addView(header);

        statusText = text("正在检测……", 15, COLOR_BLUE, false);
        root.addView(statusText, marginTop(dp(12)));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, dp(8), dp(12), dp(20));
        scroll.addView(content);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        scrollParams.topMargin = dp(4);
        root.addView(scroll, scrollParams);

        setContentView(root);
    }

    private void refreshDisplayInfo() {
        content.removeAllViews();
        Display display = getPrimaryDisplay();
        if (display == null) {
            statusText.setText("未找到主显示器");
            addCard("检测失败", "Android 系统没有返回默认显示器。", COLOR_YELLOW);
            return;
        }

        Display.Mode activeMode = display.getMode();
        Display.Mode[] modes = display.getSupportedModes();
        DisplayMetrics uiMetrics = new DisplayMetrics();
        DisplayMetrics realMetrics = new DisplayMetrics();
        Point appSize = new Point();
        Point realSize = new Point();
        display.getMetrics(uiMetrics);
        display.getRealMetrics(realMetrics);
        display.getSize(appSize);
        display.getRealSize(realSize);

        int physicalWidth = activeMode.getPhysicalWidth();
        int physicalHeight = activeMode.getPhysicalHeight();
        Display.Mode maxMode = findLargestMode(modes);

        boolean activeIs4k = isAtLeast4k(physicalWidth, physicalHeight);
        boolean supports4k = maxMode != null && isAtLeast4k(maxMode.getPhysicalWidth(), maxMode.getPhysicalHeight());
        boolean uiDiffers = normalizedWidth(realSize.x, realSize.y) != normalizedWidth(physicalWidth, physicalHeight)
                || normalizedHeight(realSize.x, realSize.y) != normalizedHeight(physicalWidth, physicalHeight);

        String verdict;
        int verdictColor;
        if (activeIs4k) {
            verdict = "当前物理显示模式为 4K 或更高";
            verdictColor = COLOR_GREEN;
        } else if (supports4k) {
            verdict = "屏幕/输出支持 4K，但当前活动模式不是 4K";
            verdictColor = COLOR_YELLOW;
        } else {
            verdict = "系统公开 API 没有报告 4K 显示模式";
            verdictColor = COLOR_YELLOW;
        }
        statusText.setText(verdict);
        statusText.setTextColor(verdictColor);

        StringBuilder physical = new StringBuilder();
        physical.append("当前活动物理模式：")
                .append(formatResolution(physicalWidth, physicalHeight))
                .append(" @ ")
                .append(formatRefresh(activeMode.getRefreshRate()))
                .append(" Hz\n")
                .append("当前模式判断：").append(activeIs4k ? "4K/更高" : classifyResolution(physicalWidth, physicalHeight)).append("\n");
        if (maxMode != null) {
            physical.append("系统报告的最高模式：")
                    .append(formatResolution(maxMode.getPhysicalWidth(), maxMode.getPhysicalHeight()))
                    .append(" @ ").append(formatRefresh(maxMode.getRefreshRate())).append(" Hz\n")
                    .append("是否报告 4K 支持：").append(supports4k ? "是" : "否");
        }
        addCard("① 物理显示模式（重点）", physical.toString(), verdictColor);

        String ui = "应用可用尺寸（getSize）：" + formatResolution(appSize.x, appSize.y) + "\n"
                + "系统逻辑尺寸（getRealSize）：" + formatResolution(realSize.x, realSize.y) + "\n"
                + "UI metrics：" + uiMetrics.widthPixels + " × " + uiMetrics.heightPixels
                + "，densityDpi=" + uiMetrics.densityDpi + "\n"
                + "Real metrics：" + realMetrics.widthPixels + " × " + realMetrics.heightPixels
                + "，densityDpi=" + realMetrics.densityDpi + "\n"
                + "UI 与物理模式是否不同：" + (uiDiffers ? "是（这通常就是 4K 面板以 1080p UI 渲染）" : "否");
        addCard("② UI / 逻辑渲染尺寸（用于对比）", ui, COLOR_BLUE);

        addHdrCard(display);
        addModesCard(modes, activeMode);
        addSystemCard(display);

        addCard("说明与限制",
                "“物理显示模式”来自 Android Display.Mode 的 physicalWidth/physicalHeight。"
                        + "它代表系统报告给应用的显示输出模式，不是应用画布或 density 缩放后的 UI 尺寸。\n\n"
                        + "某些电视厂商可能在固件中隐藏、固定或错误上报面板信息。若这里所有模式都只有 1920 × 1080，"
                        + "普通第三方应用无法绕过系统公开 API 直接证明内置面板像素；此时需要结合电视工程菜单、厂商规格或 ADB/系统权限进一步确认。\n\n"
                        + "HDR 列表表示显示设备/系统报告的显示能力，不等同于某个播放器、片源、HDMI 端口或解码器一定能成功播放该格式。",
                COLOR_MUTED);
    }

    private void addHdrCard(Display display) {
        Display.HdrCapabilities capabilities = display.getHdrCapabilities();
        int[] types = capabilities.getSupportedHdrTypes();
        StringBuilder text = new StringBuilder();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            text.append("当前显示器 isHdr()：").append(display.isHdr() ? "是" : "否").append("\n")
                    .append("宽色域 isWideColorGamut()：").append(display.isWideColorGamut() ? "是" : "否").append("\n");
        }

        if (types == null || types.length == 0) {
            text.append("HDR 格式：系统未报告任何 HDR 类型");
        } else {
            text.append("系统报告支持的 HDR 格式：\n");
            for (int type : types) {
                text.append("  • ").append(hdrTypeName(type)).append("（类型值 ").append(type).append("）\n");
            }
        }

        float maxLum = capabilities.getDesiredMaxLuminance();
        float maxAverageLum = capabilities.getDesiredMaxAverageLuminance();
        float minLum = capabilities.getDesiredMinLuminance();
        text.append("\n期望最高亮度：").append(formatLuminance(maxLum)).append(" cd/m²\n")
                .append("期望全屏平均最高亮度：").append(formatLuminance(maxAverageLum)).append(" cd/m²\n")
                .append("期望最低亮度：").append(formatLuminance(minLum)).append(" cd/m²\n")
                .append("Android 9 标准公开类型：Dolby Vision、HDR10、HLG；HDR10+ 标准常量从 Android 10 起提供。");

        addCard("③ HDR 与宽色域能力", text.toString(), types != null && types.length > 0 ? COLOR_GREEN : COLOR_YELLOW);
    }

    private void addModesCard(Display.Mode[] modes, Display.Mode activeMode) {
        List<Display.Mode> list = new ArrayList<>(Arrays.asList(modes));
        Collections.sort(list, new Comparator<Display.Mode>() {
            @Override
            public int compare(Display.Mode left, Display.Mode right) {
                long leftPixels = (long) left.getPhysicalWidth() * left.getPhysicalHeight();
                long rightPixels = (long) right.getPhysicalWidth() * right.getPhysicalHeight();
                if (leftPixels != rightPixels) {
                    return Long.compare(rightPixels, leftPixels);
                }
                return Float.compare(right.getRefreshRate(), left.getRefreshRate());
            }
        });

        StringBuilder text = new StringBuilder();
        if (list.isEmpty()) {
            text.append("系统没有返回可用模式列表。");
        } else {
            for (Display.Mode mode : list) {
                boolean active = mode.getModeId() == activeMode.getModeId();
                text.append(active ? "▶ " : "  ")
                        .append(formatResolution(mode.getPhysicalWidth(), mode.getPhysicalHeight()))
                        .append(" @ ").append(formatRefresh(mode.getRefreshRate())).append(" Hz")
                        .append("  [modeId=").append(mode.getModeId()).append("]")
                        .append(active ? "  当前" : "")
                        .append("\n");
            }
        }
        addCard("④ 系统报告的全部显示模式", text.toString().trim(), COLOR_BLUE);
    }

    private void addSystemCard(Display display) {
        String text = "Android：" + Build.VERSION.RELEASE + "（API " + Build.VERSION.SDK_INT + "）\n"
                + "设备：" + Build.MANUFACTURER + " " + Build.MODEL + "\n"
                + "产品：" + Build.PRODUCT + "\n"
                + "CPU ABI：" + join(Build.SUPPORTED_ABIS) + "\n"
                + "显示器名称：" + display.getName() + "\n"
                + "显示器 ID：" + display.getDisplayId() + "\n"
                + "应用实现：纯 Java，无原生 .so，兼容 32 位与 64 位 Android。";
        addCard("⑤ 设备信息", text, COLOR_MUTED);
    }

    private Display getPrimaryDisplay() {
        if (displayManager != null) {
            Display display = displayManager.getDisplay(Display.DEFAULT_DISPLAY);
            if (display != null) {
                return display;
            }
        }
        return getWindowManager().getDefaultDisplay();
    }

    private Display.Mode findLargestMode(Display.Mode[] modes) {
        if (modes == null || modes.length == 0) {
            return null;
        }
        Display.Mode result = modes[0];
        for (Display.Mode mode : modes) {
            long pixels = (long) mode.getPhysicalWidth() * mode.getPhysicalHeight();
            long resultPixels = (long) result.getPhysicalWidth() * result.getPhysicalHeight();
            if (pixels > resultPixels || (pixels == resultPixels && mode.getRefreshRate() > result.getRefreshRate())) {
                result = mode;
            }
        }
        return result;
    }

    private String hdrTypeName(int type) {
        switch (type) {
            case 1:
                return "Dolby Vision（杜比视界）";
            case 2:
                return "HDR10";
            case 3:
                return "HLG";
            case 4:
                return "HDR10+";
            default:
                return "未知/厂商扩展 HDR";
        }
    }

    private boolean isAtLeast4k(int width, int height) {
        return normalizedWidth(width, height) >= 3840 && normalizedHeight(width, height) >= 2160;
    }

    private int normalizedWidth(int width, int height) {
        return Math.max(width, height);
    }

    private int normalizedHeight(int width, int height) {
        return Math.min(width, height);
    }

    private String classifyResolution(int width, int height) {
        int longSide = normalizedWidth(width, height);
        int shortSide = normalizedHeight(width, height);
        if (longSide >= 7680 && shortSide >= 4320) return "8K/更高";
        if (longSide >= 3840 && shortSide >= 2160) return "4K";
        if (longSide >= 2560 && shortSide >= 1440) return "1440p/2K";
        if (longSide >= 1920 && shortSide >= 1080) return "1080p";
        if (longSide >= 1280 && shortSide >= 720) return "720p";
        return "低于 720p";
    }

    private String formatResolution(int width, int height) {
        return width + " × " + height + "（" + classifyResolution(width, height) + "）";
    }

    private String formatRefresh(float refreshRate) {
        return String.format(Locale.US, "%.2f", refreshRate);
    }

    private String formatLuminance(float value) {
        if (value <= 0f || Float.isNaN(value) || Float.isInfinite(value)) {
            return "未报告";
        }
        return String.format(Locale.US, "%.3f", value);
    }

    private String join(String[] values) {
        if (values == null || values.length == 0) return "未知";
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) builder.append(", ");
            builder.append(values[i]);
        }
        return builder.toString();
    }

    private void addCard(String title, String body, int accentColor) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(22), dp(18), dp(22), dp(18));
        card.setBackgroundColor(COLOR_CARD);

        TextView titleView = text(title, 20, accentColor, true);
        TextView bodyView = text(body, 16, COLOR_TEXT, false);
        bodyView.setLineSpacing(dp(3), 1.0f);
        bodyView.setTextIsSelectable(true);

        card.addView(titleView);
        card.addView(bodyView, marginTop(dp(10)));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(14);
        content.addView(card, params);
    }

    private TextView text(String value, float sp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        if (bold) {
            view.setTypeface(view.getTypeface(), android.graphics.Typeface.BOLD);
        }
        return view;
    }

    private LinearLayout.LayoutParams marginTop(int margin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = margin;
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDisplayAdded(int displayId) {
        runOnUiThread(this::refreshDisplayInfo);
    }

    @Override
    public void onDisplayRemoved(int displayId) {
        runOnUiThread(this::refreshDisplayInfo);
    }

    @Override
    public void onDisplayChanged(int displayId) {
        if (displayId == Display.DEFAULT_DISPLAY) {
            runOnUiThread(this::refreshDisplayInfo);
        }
    }
}
