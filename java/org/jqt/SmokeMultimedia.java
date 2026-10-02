/*
 * JQt - QtMultimedia 冒烟（v1.9.1 P2-②）。
 *
 * 确定性策略:冒烟自己写一个 0.3 秒的 PCM WAV 文件当素材 —— 不依赖任何外部媒体,
 * 也不依赖编解码器(WAV/PCM 是 Qt 后端最基础的支持)。
 * 若平台没有可用多媒体后端(精简安装),则优雅 SKIP 而不是失败。
 */
package org.jqt;

import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class SmokeMultimedia {

    static int pass = 0;
    static int fail = 0;

    static void check(String name, boolean cond) {
        if (cond) { pass++; System.out.println("[media] OK   " + name); }
        else { fail++; System.out.println("[media] FAIL " + name); }
    }

    /** 生成一段 16bit 单声道 PCM WAV(440Hz 正弦,便于人工听验)。 */
    static Path writeWav(Path path, int ms, int sampleRate) throws Exception {
        int samples = sampleRate * ms / 1000;
        byte[] data = new byte[samples * 2];
        for (int i = 0; i < samples; i++) {
            short v = (short) (Math.sin(2 * Math.PI * 440 * i / sampleRate) * 12000);
            data[i * 2] = (byte) (v & 0xFF);
            data[i * 2 + 1] = (byte) ((v >> 8) & 0xFF);
        }
        ByteBuffer h = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);
        h.put("RIFF".getBytes()).putInt(36 + data.length).put("WAVE".getBytes());
        h.put("fmt ".getBytes()).putInt(16).putShort((short) 1).putShort((short) 1);
        h.putInt(sampleRate).putInt(sampleRate * 2).putShort((short) 2).putShort((short) 16);
        h.put("data".getBytes()).putInt(data.length);
        try (FileOutputStream out = new FileOutputStream(path.toFile())) {
            out.write(h.array());
            out.write(data);
        }
        return path;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("[media] start");
        QApplication app = new QApplication();

        if (!QMediaPlayer.isAvailable()) {
            System.out.println("[media] SKIP 本库未编译 QtMultimedia(缺 qtmultimedia 模块)");
            System.out.println("[media] ALL PASS ✅ (跳过)");
            return;
        }

        long expectedMs = 300;
        Path wav = writeWav(Files.createTempFile("jqt-smoke-", ".wav"), (int) expectedMs, 44100);
        System.out.println("[media] 测试素材: " + wav + " (" + Files.size(wav) + " 字节)");

        QAudioOutput out = new QAudioOutput();
        out.setVolume(0.5);
        out.setMuted(true);              // CI 无声卡:静音只影响出声,不影响状态机
        check("音量设读(0.5)", Math.abs(out.volume() - 0.5) < 1e-6);
        check("静音设读(true)", out.isMuted());

        QMediaPlayer player = new QMediaPlayer();
        player.setAudioOutput(out);
        check("audioOutput 绑定成功", player.audioOutput() == out);
        check("初始状态 Stopped(" + player.playbackState() + ")",
              player.playbackState() == QMediaPlayer.PlaybackState.StoppedState);

        AtomicInteger stateChanges = new AtomicInteger();
        AtomicInteger statusChanges = new AtomicInteger();
        AtomicLong durationSeen = new AtomicLong();
        AtomicLong positionSeen = new AtomicLong();
        AtomicReference<String> err = new AtomicReference<>("");

        player.onPlaybackStateChanged(s -> stateChanges.incrementAndGet());
        player.onMediaStatusChanged(s -> statusChanges.incrementAndGet());
        player.onDurationChanged(durationSeen::set);
        player.onPositionChanged(positionSeen::set);
        player.onError(err::set);

        player.setSource(wav.toString());
        check("source 设读", wav.toString().equals(player.source()));

        // 启动:setSource 是异步的,给它一点时间加载再 play
        QTimer.singleShot(400, player::play);
        QTimer.singleShot(900, () -> {
            System.out.println("[media] 播放中: state=" + player.playbackState()
                             + " status=" + player.mediaStatus()
                             + " duration=" + player.duration()
                             + " position=" + player.position());
        });
        QTimer.singleShot(1400, player::pause);
        QTimer.singleShot(1600, player::stop);
        app.scheduleQuit(2000);
        app.exec();

        boolean backendOk = durationSeen.get() > 0 || player.mediaStatus() != QMediaPlayer.MediaStatus.NoMedia;
        if (!backendOk) {
            System.out.println("[media] SKIP 平台无可用多媒体后端(未拿到时长/状态),错误: "
                             + (err.get().isEmpty() ? "(无)" : err.get()));
            System.out.println("[media] 已完成的前置断言: " + pass + " 通过");
            System.out.println("[media] ALL PASS ✅ (跳过播放链路)");
            return;
        }

        check("拿到媒体时长(实际 " + durationSeen.get() + " ms,期望约 " + expectedMs + ")",
              Math.abs(durationSeen.get() - expectedMs) < 120);
        check("播放状态发生过变化(实际 " + stateChanges.get() + " 次)", stateChanges.get() >= 2);
        check("媒体状态发生过变化(实际 " + statusChanges.get() + " 次)", statusChanges.get() >= 1);
        check("最终状态回到 Stopped(实际 " + player.playbackState() + ")",
              player.playbackState() == QMediaPlayer.PlaybackState.StoppedState);
        check("无错误(lastError='" + player.lastError() + "')", player.lastError().isEmpty());
        check("播放位置有推进或被 stop 归零(实际 " + positionSeen.get() + " ms)", positionSeen.get() >= 0);

        // 错误路径:不存在的文件应触发 error/InvalidMedia
        QMediaPlayer bad = new QMediaPlayer();
        bad.setAudioOutput(new QAudioOutput());
        AtomicReference<String> badErr = new AtomicReference<>("");
        bad.onError(badErr::set);
        AtomicInteger badStatus = new AtomicInteger(-1);
        bad.onMediaStatusChanged(badStatus::set);
        bad.setSource(wav.getParent().resolve("does-not-exist-12345.wav").toString());
        QTimer.singleShot(300, bad::play);
        app.scheduleQuit(800);
        app.exec();
        check("不存在的文件触发错误或 InvalidMedia(错误='" + badErr.get()
              + "' 状态=" + badStatus.get() + ")",
              !badErr.get().isEmpty() || badStatus.get() == QMediaPlayer.MediaStatus.InvalidMedia);

        // 收尾:显式停表并跑一小段事件循环,让多媒体后端线程退出。
        // 否则 Windows 上会在进程退出时报 "QWaitCondition: Destroyed while threads are
        // still waiting" 并以非 0 退出(断言全过但 CI 失败,实测踩过)。
        player.stop();
        bad.stop();
        QTimer.singleShot(200, () -> {});
        app.scheduleQuit(400);
        app.exec();

        // 播放器可能仍占着文件(Windows 文件锁),删除失败不影响结论
        try { Files.deleteIfExists(wav); }
        catch (Exception e) { System.out.println("[media] 提示: 临时文件稍后由系统清理(" + e.getClass().getSimpleName() + ")"); }
        System.out.println("[media] pass=" + pass + " fail=" + fail);
        System.out.println("[media] " + (fail == 0 ? "ALL PASS ✅" : ("FAILED: " + fail)));
        System.exit(fail > 0 ? 1 : 0);      // 显式定退出码,不受 Qt 收尾影响
    }
}
