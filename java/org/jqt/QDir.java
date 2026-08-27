/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 目录操作：封装 C++ 侧的 {@code QDir}（静态工具，简化形态）。
 */
public class QDir {

    private QDir() {
    }

    /** 当前工作目录。 */
    public static String current() {
        return nativeCurrent();
    }
    private static native String nativeCurrent();

    /** 删除目录（空目录），成功返回 true。 */
    public static boolean remove(String path) {
        return nativeRemove(path);
    }
    private static native boolean nativeRemove(String path);

    /** 目录中条目数（文件+子目录，不含 . 和 ..）。 */
    public static int count(String path) {
        return nativeCount(path);
    }
    private static native int nativeCount(String path);
}
