/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 文件操作：封装 C++ 侧的 {@code QFile}（静态工具）。
 */
public class QFile {

    /** 复制文件，成功返回 true。 */
    public static boolean copy(String src, String dst) {
        return nativeCopy(src, dst);
    }
    private static native boolean nativeCopy(String src, String dst);

    /** 文件/目录是否存在。 */
    public static boolean exists(String path) {
        return nativeExists(path);
    }
    private static native boolean nativeExists(String path);

    /** 删除文件，成功返回 true。 */
    public static boolean remove(String path) {
        return nativeRemove(path);
    }
    private static native boolean nativeRemove(String path);

    /** 重命名/移动文件，成功返回 true。 */
    public static boolean rename(String oldName, String newName) {
        return nativeRename(oldName, newName);
    }
    private static native boolean nativeRename(String oldName, String newName);

    /** 文件大小（字节；不存在返回 -1）。 */
    public static long size(String path) {
        return nativeSize(path);
    }
    private static native long nativeSize(String path);

    /** 截断/扩展文件到指定大小（字节），成功返回 true。 */
    public static boolean resize(String path, long size) {
        return nativeResize(path, size);
    }
    private static native boolean nativeResize(String path, long size);

    // ---- 实例 API（QFile 对象 + open/close，v0.8.0 L1 收尾）----

    /** 打开模式（QIODevice::OpenModeFlag）。 */
    public enum OpenMode { READ_ONLY, WRITE_ONLY, READ_WRITE, APPEND }

    private long nativeHandle;

    /** 创建文件对象（尚未打开）。 */
    public QFile() {
        nativeHandle = nativeCreate();
    }

    /** 打开文件（QIODevice::open）。 */
    public boolean open(String path, OpenMode mode) {
        return nativeOpen(nativeHandle, path, mode.ordinal());
    }

    /** 关闭文件。 */
    public void close() {
        nativeClose(nativeHandle);
    }

    /** 是否已打开。 */
    public boolean isOpen() {
        return nativeIsOpen(nativeHandle);
    }

    /** 写入文本（UTF-8；文件需以写模式打开）。 */
    public boolean write(String text) {
        return nativeWrite(nativeHandle, text);
    }

    /** 读取全部文本（UTF-8；文件需以读模式打开）。 */
    public String readAll() {
        return nativeReadAll(nativeHandle);
    }

    /** 读取一行（UTF-8；无更多内容返回 null）。 */
    public String readLine() {
        return nativeReadLine(nativeHandle);
    }

    private native long nativeCreate();
    private native boolean nativeOpen(long handle, String path, int mode);
    private native void nativeClose(long handle);
    private native boolean nativeIsOpen(long handle);
    private native boolean nativeWrite(long handle, String text);
    private native String nativeReadAll(long handle);
    private native String nativeReadLine(long handle);

// ---- 生成器批次（jqt-gen 自动生成，直传型） ----
    /** supportsMoveToTrash（Qt supportsMoveToTrash）。 */
    public boolean supportsMoveToTrash() {
        return nativeSupportsMoveToTrash(nativeHandle);
    }
    private static native boolean nativeSupportsMoveToTrash(long nativeHandle);

    /** symLinkTarget（Qt symLinkTarget）。 */
    public String symLinkTarget(String arg0) {
        return nativeSymLinkTarget(nativeHandle, arg0);
    }
    private static native String nativeSymLinkTarget(long nativeHandle, String arg0);

    /** symLinkTarget（Qt symLinkTarget）。 */
    public String symLinkTarget() {
        return nativeSymLinkTarget(nativeHandle);
    }
    private static native String nativeSymLinkTarget(long nativeHandle);

}