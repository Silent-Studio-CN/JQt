/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.lang.ref.Cleaner;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 媒体播放器（Qt {@code QMediaPlayer}，QtMultimedia 模块）。
 * <pre>
 * QMediaPlayer p = new QMediaPlayer();
 * p.setAudioOutput(new QAudioOutput());
 * p.onDurationChanged(ms -&gt; System.out.println("时长 " + ms + " ms"));
 * p.onError(msg -&gt; System.out.println("错误: " + msg));
 * p.setSource("/path/to/audio.wav");
 * p.play();
 * </pre>
 * <b>线程</b>：回调在 Qt 主线程（事件循环里驱动）。
 * <p>后端：Qt 6 用 ffmpeg 或平台后端插件（Windows 上还有 WMP 后端）——
 * 某些精简安装可能没有可用后端，此时设置/播放会走 {@code onError} 回调。
 */
public class QMediaPlayer {

    /** 播放状态（Qt {@code QMediaPlayer::PlaybackState}）。 */
    public static final class PlaybackState {
        private PlaybackState() {}
        public static final int StoppedState = 0;
        public static final int PlayingState = 1;
        public static final int PausedState = 2;
    }

    /** 媒体状态（Qt {@code QMediaPlayer::MediaStatus} 子集）。 */
    public static final class MediaStatus {
        private MediaStatus() {}
        public static final int NoMedia = 0;
        public static final int LoadingMedia = 1;
        public static final int LoadedMedia = 2;
        public static final int BufferedMedia = 4;
        public static final int EndOfMedia = 5;
        public static final int InvalidMedia = 6;
    }

    private static final Cleaner CLEANER = Cleaner.create();

    private final List<Consumer<Integer>> stateHandlers = new ArrayList<>();
    private final List<Consumer<Integer>> statusHandlers = new ArrayList<>();
    private final List<Consumer<Long>> durationHandlers = new ArrayList<>();
    private final List<Consumer<Long>> positionHandlers = new ArrayList<>();
    private final List<Consumer<String>> errorHandlers = new ArrayList<>();

    private long nativeHandle;
    private QAudioOutput audioOutput;
    private String lastError = "";
    private long lastDuration = 0;
    private long lastPosition = 0;
    private int lastState = PlaybackState.StoppedState;
    private int lastStatus = MediaStatus.NoMedia;
    private String source = "";

    /** 本库是否编译进 QtMultimedia 支持。 */
    public static boolean isAvailable() { return nativeAvailable(); }
    private static native boolean nativeAvailable();

    /** 创建播放器（未设置媒体）。 */
    public QMediaPlayer() {
        if (!isAvailable()) {
            throw new IllegalStateException(
                "本库未编译 QtMultimedia 支持(缺少 qtmultimedia 模块);先安装该模块再构建 libjqt");
        }
        nativeHandle = nativeCreate();
        final long h = nativeHandle;
        CLEANER.register(this, () -> nativeDispose(h));
        nativeConnectSignals(nativeHandle);
    }

    private static native long nativeCreate();
    private static native void nativeDispose(long handle);
    private native void nativeConnectSignals(long handle);   // 实例 native:回调需要 Java 对象引用

    long nativeHandle() { return nativeHandle; }

    /** 接入音频输出（Qt 6 必须显式接才会出声）。 */
    public void setAudioOutput(QAudioOutput output) {
        audioOutput = output;
        nativeSetAudioOutput(nativeHandle, output == null ? 0 : output.nativeHandle());
    }
    private static native void nativeSetAudioOutput(long handle, long outputHandle);

    /** 当前音频输出。 */
    public QAudioOutput audioOutput() { return audioOutput; }

    /** 设置媒体源（本地路径或 URL）。 */
    public void setSource(String url) {
        source = url == null ? "" : url;
        nativeSetSource(nativeHandle, source);
    }
    private static native void nativeSetSource(long handle, String url);

    /** 当前媒体源。 */
    public String source() { return source; }

    /** 播放。 */
    public void play() { nativePlay(nativeHandle); }
    private static native void nativePlay(long handle);

    /** 暂停。 */
    public void pause() { nativePause(nativeHandle); }
    private static native void nativePause(long handle);

    /** 停止。 */
    public void stop() { nativeStop(nativeHandle); }
    private static native void nativeStop(long handle);

    /** 播放状态（{@link PlaybackState}）。 */
    public int playbackState() { return lastState; }

    /** 媒体状态（{@link MediaStatus}）。 */
    public int mediaStatus() { return lastStatus; }

    /** 媒体时长（毫秒；未知为 0）。 */
    public long duration() { return lastDuration; }

    /** 当前播放位置（毫秒）。 */
    public long position() { return lastPosition; }

    /** 跳转到指定位置。 */
    public void setPosition(long ms) { nativeSetPosition(nativeHandle, ms); }
    private static native void nativeSetPosition(long handle, long ms);

    /** 最近一次错误（无错误为空串）。 */
    public String lastError() { return lastError; }

    /** 是否正在播放。 */
    public boolean isPlaying() { return lastState == PlaybackState.PlayingState; }

    // ---------------- 回调 ----------------

    /** 播放状态变化。 */
    public QMediaPlayer onPlaybackStateChanged(Consumer<Integer> handler) {
        stateHandlers.add(handler); return this;
    }

    /** 媒体状态变化。 */
    public QMediaPlayer onMediaStatusChanged(Consumer<Integer> handler) {
        statusHandlers.add(handler); return this;
    }

    /** 时长变化（毫秒）。 */
    public QMediaPlayer onDurationChanged(Consumer<Long> handler) {
        durationHandlers.add(handler); return this;
    }

    /** 播放位置变化（毫秒）。 */
    public QMediaPlayer onPositionChanged(Consumer<Long> handler) {
        positionHandlers.add(handler); return this;
    }

    /** 发生错误（参数为描述）。 */
    public QMediaPlayer onError(Consumer<String> handler) {
        errorHandlers.add(handler); return this;
    }

    // ---------------- 由 C++ 侧回调（JNI，Qt 主线程）----------------

    void nativeHandleStateChanged(int state) {
        lastState = state;
        for (Consumer<Integer> h : stateHandlers) h.accept(state);
    }

    void nativeHandleMediaStatusChanged(int status) {
        lastStatus = status;
        for (Consumer<Integer> h : statusHandlers) h.accept(status);
    }

    void nativeHandleDurationChanged(long ms) {
        lastDuration = ms;
        for (Consumer<Long> h : durationHandlers) h.accept(ms);
    }

    void nativeHandlePositionChanged(long ms) {
        lastPosition = ms;
        for (Consumer<Long> h : positionHandlers) h.accept(ms);
    }

    void nativeHandleError(String message) {
        lastError = message == null ? "" : message;
        for (Consumer<String> h : errorHandlers) h.accept(lastError);
    }
}
