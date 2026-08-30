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
 * 串口（QSerialPort，Qt SerialPort 模块）：工业串口通信。
 * <p>
 * v0.7.4 完整实现（Qt API 语义对齐）：
 * <pre>
 * QSerialPort port = new QSerialPort("COM3");
 * port.setBaudRate(115200);
 * if (port.open(QSerialPort.OpenMode.READ_WRITE)) {
 *     port.write("AT\r\n".getBytes());
 *     port.onReadyRead(() -> System.out.println(port.readAllText()));
 * }
 * </pre>
 * 可用端口：{@link #availablePorts()}。
 */
public class QSerialPort {

    /** 数据位（QSerialPort::DataBits）。 */
    public enum DataBits { DATA_5, DATA_6, DATA_7, DATA_8 }

    /** 校验位（QSerialPort::Parity）。 */
    public enum Parity { NO_PARITY, EVEN_PARITY, ODD_PARITY, MARK_PARITY, SPACE_PARITY }

    /** 停止位（QSerialPort::StopBits）。 */
    public enum StopBits { ONE_STOP, ONE_AND_HALF_STOP, TWO_STOP }

    /** 流控（QSerialPort::FlowControl）。 */
    public enum FlowControl { NO_FLOW_CONTROL, HARDWARE_FLOW_CONTROL, SOFTWARE_FLOW_CONTROL }

    /** 打开模式（QIODevice::OpenMode 子集）。 */
    public enum OpenMode { READ_ONLY, WRITE_ONLY, READ_WRITE }

    private static final Cleaner CLEANER = Cleaner.create();

    private final List<Runnable> readyReadHandlers = new ArrayList<>();
    private final List<Consumer<Integer>> bytesWrittenHandlers = new ArrayList<>();

    private long nativeHandle;

    /** 创建串口对象（未打开；需 setPortName + open）。 */
    public QSerialPort() {
        nativeHandle = nativeCreate();
        registerCleaner();
    }

    /** 创建串口对象并指定端口名（如 "COM3" / "/dev/ttyUSB0"）。 */
    public QSerialPort(String portName) {
        this();
        setPortName(portName);
    }

    /** 系统可用串口列表（QSerialPortInfo::availablePorts → portName）。 */
    public static List<String> availablePorts() {
        List<String> out = new ArrayList<>();
        for (String s : nativeAvailablePorts()) out.add(s);
        return out;
    }
    private static native String[] nativeAvailablePorts();

    private native long nativeCreate();
    private native void nativeDispose(long handle);

    /** 注册 Cleaner 释放（GC 时释放 C++ 对象）。 */
    private void registerCleaner() {
        final long handle = nativeHandle;
        CLEANER.register(this, () -> nativeDispose(handle));
    }

    /** 设置端口名（如 "COM3" / "/dev/ttyUSB0"）。 */
    public void setPortName(String name) {
        nativeSetPortName(nativeHandle, name);
    }
    private native void nativeSetPortName(long handle, String name);

    /** 当前端口名。 */
    public String portName() {
        return nativePortName(nativeHandle);
    }
    private native String nativePortName(long handle);

    /** 波特率（如 9600/115200；QSerialPort::setBaudRate），成功返回 true。 */
    public boolean setBaudRate(int baudRate) {
        return nativeSetBaudRate(nativeHandle, baudRate);
    }
    private native boolean nativeSetBaudRate(long handle, int baudRate);

    /** 当前波特率。 */
    public int baudRate() {
        return nativeBaudRate(nativeHandle);
    }
    private native int nativeBaudRate(long handle);

    /** 数据位。 */
    public void setDataBits(DataBits bits) {
        nativeSetDataBits(nativeHandle, bits.ordinal());
    }
    private native void nativeSetDataBits(long handle, int bits);

    /** 校验位。 */
    public void setParity(Parity parity) {
        nativeSetParity(nativeHandle, parity.ordinal());
    }
    private native void nativeSetParity(long handle, int parity);

    /** 停止位。 */
    public void setStopBits(StopBits bits) {
        nativeSetStopBits(nativeHandle, bits.ordinal());
    }
    private native void nativeSetStopBits(long handle, int bits);

    /** 流控。 */
    public void setFlowControl(FlowControl flow) {
        nativeSetFlowControl(nativeHandle, flow.ordinal());
    }
    private native void nativeSetFlowControl(long handle, int flow);

    /** 打开串口（QIODevice::open），成功返回 true。 */
    public boolean open(OpenMode mode) {
        return nativeOpen(nativeHandle, mode.ordinal());
    }
    private native boolean nativeOpen(long handle, int mode);

    /** 关闭串口。 */
    public void close() {
        nativeClose(nativeHandle);
    }
    private native void nativeClose(long handle);

    /** 是否已打开。 */
    public boolean isOpen() {
        return nativeIsOpen(nativeHandle);
    }
    private native boolean nativeIsOpen(long handle);

    /** 发送字节（返回实际写入字节数；-1 表示错误）。 */
    public int write(byte[] data) {
        return nativeWrite(nativeHandle, data);
    }
    private native int nativeWrite(long handle, byte[] data);

    /** 发送文本（UTF-8 编码）。 */
    public int write(String text) {
        return nativeWriteUtf8(nativeHandle, text);
    }
    private native int nativeWriteUtf8(long handle, String text);

    /** 读取全部可用字节（无数据返回空数组）。 */
    public byte[] readAll() {
        return nativeReadAll(nativeHandle);
    }
    private native byte[] nativeReadAll(long handle);

    /** 读取全部可用数据并转 UTF-8 文本。 */
    public String readAllText() {
        byte[] data = readAll();
        return data == null || data.length == 0 ? "" : new String(data, java.nio.charset.StandardCharsets.UTF_8);
    }

    /** 读取一行（以 \n 结尾；无完整行返回 null）。 */
    public String readLine() {
        return nativeReadLine(nativeHandle);
    }
    private native String nativeReadLine(long handle);

    /** 缓冲区可用字节数。 */
    public int bytesAvailable() {
        return nativeBytesAvailable(nativeHandle);
    }
    private native int nativeBytesAvailable(long handle);

    /** 阻塞等待数据到达（最多 timeoutMs 毫秒；QSerialPort::waitForReadyRead）。 */
    public boolean waitForReadyRead(int timeoutMs) {
        return nativeWaitForReadyRead(nativeHandle, timeoutMs);
    }
    private native boolean nativeWaitForReadyRead(long handle, int timeoutMs);

    /** 等待写缓冲区排空（QSerialPort::flush）。 */
    public boolean flush() {
        return nativeFlush(nativeHandle);
    }
    private native boolean nativeFlush(long handle);

    /** 清空读写缓冲区（QSerialPort::clear）。 */
    public void clear() {
        nativeClear(nativeHandle);
    }
    private native void nativeClear(long handle);

    /** 最近一次错误描述。 */
    public String errorString() {
        return nativeErrorString(nativeHandle);
    }
    private native String nativeErrorString(long handle);

    /** 数据到达回调（readyRead 信号）。 */
    public QSerialPort onReadyRead(Runnable handler) {
        readyReadHandlers.add(handler);
        nativeConnectReadyRead(nativeHandle);
        return this;
    }
    private native void nativeConnectReadyRead(long handle);

    /** 写入完成回调（bytesWritten 信号，参数为本次写入字节数）。 */
    public QSerialPort onBytesWritten(Consumer<Integer> handler) {
        bytesWrittenHandlers.add(handler);
        nativeConnectBytesWritten(nativeHandle);
        return this;
    }
    private native void nativeConnectBytesWritten(long handle);

    void nativeHandleReadyRead() {
        for (Runnable h : readyReadHandlers) h.run();
    }

    void nativeHandleBytesWritten(int count) {
        for (Consumer<Integer> h : bytesWrittenHandlers) h.accept(count);
    }

// ---- 生成器批次（jqt-gen 自动生成，直传型） ----
    /** bytesToWrite（Qt bytesToWrite）。 */
    public long bytesToWrite() {
        return nativeBytesToWrite(nativeHandle);
    }
    private static native long nativeBytesToWrite(long nativeHandle);

    /** canReadLine（Qt canReadLine）。 */
    public boolean canReadLine() {
        return nativeCanReadLine(nativeHandle);
    }
    private static native boolean nativeCanReadLine(long nativeHandle);

    /** clearError（Qt clearError）。 */
    public void clearError() {
        nativeClearError(nativeHandle);
    }
    private static native void nativeClearError(long nativeHandle);

    /** isBreakEnabled（Qt isBreakEnabled）。 */
    public boolean isBreakEnabled() {
        return nativeIsBreakEnabled(nativeHandle);
    }
    private static native boolean nativeIsBreakEnabled(long nativeHandle);

    /** isDataTerminalReady（Qt isDataTerminalReady）。 */
    public boolean isDataTerminalReady() {
        return nativeIsDataTerminalReady(nativeHandle);
    }
    private static native boolean nativeIsDataTerminalReady(long nativeHandle);

    /** isRequestToSend（Qt isRequestToSend）。 */
    public boolean isRequestToSend() {
        return nativeIsRequestToSend(nativeHandle);
    }
    private static native boolean nativeIsRequestToSend(long nativeHandle);

    /** isSequential（Qt isSequential）。 */
    public boolean isSequential() {
        return nativeIsSequential(nativeHandle);
    }
    private static native boolean nativeIsSequential(long nativeHandle);

    /** setBreakEnabled（Qt setBreakEnabled）。 */
    public boolean setBreakEnabled(boolean arg0) {
        return nativeSetBreakEnabled(nativeHandle, arg0);
    }
    private static native boolean nativeSetBreakEnabled(long nativeHandle, boolean arg0);

    /** setDataTerminalReady（Qt setDataTerminalReady）。 */
    public boolean setDataTerminalReady(boolean arg0) {
        return nativeSetDataTerminalReady(nativeHandle, arg0);
    }
    private static native boolean nativeSetDataTerminalReady(long nativeHandle, boolean arg0);

    /** setReadBufferSize（Qt setReadBufferSize）。 */
    public void setReadBufferSize(long arg0) {
        nativeSetReadBufferSize(nativeHandle, arg0);
    }
    private static native void nativeSetReadBufferSize(long nativeHandle, long arg0);

    /** setRequestToSend（Qt setRequestToSend）。 */
    public boolean setRequestToSend(boolean arg0) {
        return nativeSetRequestToSend(nativeHandle, arg0);
    }
    private static native boolean nativeSetRequestToSend(long nativeHandle, boolean arg0);

    /** setSettingsRestoredOnClose（Qt setSettingsRestoredOnClose）。 */
    public void setSettingsRestoredOnClose(boolean arg0) {
        nativeSetSettingsRestoredOnClose(nativeHandle, arg0);
    }
    private static native void nativeSetSettingsRestoredOnClose(long nativeHandle, boolean arg0);

    /** settingsRestoredOnClose（Qt settingsRestoredOnClose）。 */
    public boolean settingsRestoredOnClose() {
        return nativeSettingsRestoredOnClose(nativeHandle);
    }
    private static native boolean nativeSettingsRestoredOnClose(long nativeHandle);

    /** writeBufferSize（Qt writeBufferSize）。 */
    public long writeBufferSize() {
        return nativeWriteBufferSize(nativeHandle);
    }
    private static native long nativeWriteBufferSize(long nativeHandle);

}