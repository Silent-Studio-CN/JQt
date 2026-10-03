/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.lang.ref.Cleaner;

/**
 * 帮助引擎（Qt {@code QHelpEngineCore}，QtHelp 模块）—— 读取/管理 Qt 帮助集合（{@code .qhc}）。
 * <pre>
 * QHelpEngineCore help = new QHelpEngineCore("app.qhc");   // 文件不存在会自动创建
 * if (!help.setupData()) System.out.println("帮助初始化失败: " + help.error());
 * help.registerDocumentation("docs/manual.qch");           // 注册已压缩的帮助文档
 * System.out.println(Arrays.toString(help.registeredDocumentations()));
 * byte[] html = help.fileData("doc/index.html");           // 直接取文档内容
 * </pre>
 * <p><b>关于 .qhc/.qch</b>：{@code .qhc} 是**集合文件**，{@code QHelpEngineCore} 会在首次
 * 构造时按需创建（因此不需要预置）；{@code .qch} 是**已压缩的帮助文档**，由 Qt 自带工具
 * {@code qhelpgenerator} 从 {@code .qhp} 工程文件生成 —— 两者都可在测试/构建期现场产出，
 * 不是"必须事先准备"的资源。
 * <p>{@code QHelpEngine}（带 UI 的那个）需要 QtWidgets 与 qhelpgenerator 产物配套使用，
 * 本类只封装 **Core**（无 UI 依赖，可在无界面环境使用）。
 */
public class QHelpEngineCore {

    private static final Cleaner CLEANER = Cleaner.create();
    private long nativeHandle;
    private String collectionFile = "";
    private boolean closed;

    /** 本库是否编译进 QtHelp 支持。 */
    public static boolean isAvailable() { return nativeAvailable(); }
    private static native boolean nativeAvailable();

    /** 打开（或创建）帮助集合文件。 */
    public QHelpEngineCore(String collectionFile) {
        if (!isAvailable()) {
            throw new IllegalStateException(
                "本库未编译 QtHelp 支持(缺少 qttools 模块);先安装该模块再构建 libjqt");
        }
        this.collectionFile = collectionFile == null ? "" : collectionFile;
        nativeHandle = nativeCreate(this.collectionFile);
        final long h = nativeHandle;
        CLEANER.register(this, () -> nativeDispose(h));
    }

    private static native long nativeCreate(String collectionFile);
    private static native void nativeDispose(long handle);

    long nativeHandle() { return nativeHandle; }

    /** 集合文件路径。 */
    public String collectionFile() { return collectionFile; }

    /**
     * 关闭引擎并**把集合写入 {@code .qhc}**。
     * <p><b>重要</b>:{@code QHelpEngineCore} 采用**关闭时落盘**语义 ——
     * {@link #registerDocumentation}、{@link #setCustomValue} 的效果在 close() 之后
     * 才会体现在集合文件里,重新打开一个新实例即可读到(本库不自动关闭,
     * 由调用方显式控制生命周期)。
     */
    public void close() {
        if (nativeHandle == 0) return;
        nativeClose(nativeHandle);
        nativeHandle = 0;
        closed = true;
    }
    private static native void nativeClose(long handle);

    /** 是否已关闭。 */
    public boolean isClosed() { return closed; }

    /** 加载并校验集合数据（任何查询前应调用一次）。 */
    public boolean setupData() { return nativeSetupData(nativeHandle); }
    private static native boolean nativeSetupData(long handle);

    /** 最近一次错误（无错误为空串）。 */
    public String error() { return nativeError(nativeHandle); }
    private static native String nativeError(long handle);

    /**
     * 从 {@code .qch} 文件读出其命名空间（Qt 里这是 **static** 方法,不需要打开集合）。
     * @param qchFile 已压缩帮助文档路径
     */
    public static String namespaceName(String qchFile) {
        return nativeNamespaceName(qchFile == null ? "" : qchFile);
    }
    private static native String nativeNamespaceName(String qchFile);

    /**
     * 读取 {@code .qch} 的元数据项（如 {@code "title"}、{@code "version"}、{@code "homepage"}）。
     */
    public static String metaData(String qchFile, String name) {
        return nativeMetaData(qchFile == null ? "" : qchFile, name == null ? "" : name);
    }
    private static native String nativeMetaData(String qchFile, String name);

    /** 已注册的帮助文档命名空间列表。 */
    public String[] registeredDocumentations() { return nativeRegisteredDocumentations(nativeHandle); }
    private static native String[] nativeRegisteredDocumentations(long handle);

    /** 指定命名空间对应的 .qch 文件路径（未注册则空串）。 */
    public String documentationFileName(String namespace) {
        return nativeDocumentationFileName(nativeHandle, namespace == null ? "" : namespace);
    }
    private static native String nativeDocumentationFileName(long handle, String namespace);

    /** 注册一份 .qch（成功返回 true）。 */
    public boolean registerDocumentation(String qchFile) {
        return nativeRegisterDocumentation(nativeHandle, qchFile == null ? "" : qchFile);
    }
    private static native boolean nativeRegisterDocumentation(long handle, String qchFile);

    /** 注销指定命名空间的文档。 */
    public boolean unregisterDocumentation(String namespace) {
        return nativeUnregisterDocumentation(nativeHandle, namespace == null ? "" : namespace);
    }
    private static native boolean nativeUnregisterDocumentation(long handle, String namespace);

    /**
     * 查询集合内文件（Qt 6 的 {@code files()} 需要三个参数）。
     * @param namespace        文档命名空间（如 {@code org.jqt.smoke}）
     * @param filterAttributes 过滤属性（无则传 {@code null}）
     * @param extensionFilter  扩展名过滤（如 {@code "html"};无则传空串）
     * @return 匹配文件的 {@code qthelp://} URL 数组
     */
    public String[] files(String namespace, String[] filterAttributes, String extensionFilter) {
        return nativeFiles(nativeHandle, namespace == null ? "" : namespace,
                           filterAttributes, extensionFilter == null ? "" : extensionFilter);
    }
    private static native String[] nativeFiles(long handle, String namespace,
                                               String[] filterAttributes, String extensionFilter);

    /**
     * 读取集合内某个文件的内容（{@code QHelpEngineCore::fileData}）。
     * @param url 相对路径或 {@code qthelp://} URL
     * @return 内容字节;不存在返回空数组
     */
    public byte[] fileData(String url) { return nativeFileData(nativeHandle, url == null ? "" : url); }
    private static native byte[] nativeFileData(long handle, String url);

    /** 写入自定义键值（持久化在 .qhc 里,常用于记录帮助窗口状态）。 */
    public boolean setCustomValue(String key, Object value) {
        return nativeSetCustomValue(nativeHandle, key == null ? "" : key, String.valueOf(value));
    }
    private static native boolean nativeSetCustomValue(long handle, String key, String value);

    /** 删除自定义键值（成功返回 true）。 */
    public boolean removeCustomValue(String key) {
        return nativeRemoveCustomValue(nativeHandle, key == null ? "" : key);
    }
    private static native boolean nativeRemoveCustomValue(long handle, String key);

    /** 读取自定义键值（不存在返回 defaultValue）。 */
    public Object customValue(String key, Object defaultValue) {
        String v = nativeCustomValue(nativeHandle, key == null ? "" : key);
        return (v == null || v.isEmpty()) ? defaultValue : v;
    }
    private static native String nativeCustomValue(long handle, String key);
}
