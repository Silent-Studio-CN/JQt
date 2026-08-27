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

    private QFile() {
    }

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
}
