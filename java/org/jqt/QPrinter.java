/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

/**
 * 打印机（QPrinter，QtPrintSupport）：原生打印与 PDF 输出。
 * <p>
 * v0.7.2 工业模块。典型用法（导出 PDF）：
 * <pre>
 * QPrinter p = new QPrinter();
 * p.setOutputFormat(QPrinter.OutputFormat.PDF);
 * p.setOutputFileName("out.pdf");
 * textEdit.print(p);
 * </pre>
 * 或便捷 API：{@code textEdit.printToPdf("out.pdf")}。
 */
public class QPrinter {

    /** 输出格式（QPrinter::OutputFormat）。 */
    public enum OutputFormat { NATIVE, PDF }

    /** 纸张规格（QPageSize 常用子集）。 */
    public enum PageSize { A4, A3, A5, LETTER, LEGAL }

    private long nativeHandle;

    /** 创建打印机（默认原生格式）。 */
    public QPrinter() {
        nativeHandle = nativeCreate();
    }

    /** 输出格式：NATIVE 走系统打印机，PDF 输出到文件。 */
    public void setOutputFormat(OutputFormat format) {
        nativeSetOutputFormat(nativeHandle, format.ordinal());
    }

    /** 输出文件名（PDF 模式必需；原生模式指定打印机作业名）。 */
    public void setOutputFileName(String path) {
        nativeSetOutputFileName(nativeHandle, path);
    }

    /** 分辨率（DPI；默认 1200）。 */
    public void setResolution(int dpi) {
        nativeSetResolution(nativeHandle, dpi);
    }

    /** 纸张规格。 */
    public void setPageSize(PageSize size) {
        nativeSetPageSize(nativeHandle, size.ordinal());
    }

    /** 新起一页（返回 false 表示失败/页满）。 */
    public boolean newPage() {
        return nativeNewPage(nativeHandle);
    }

    /** C++ 侧句柄（QTextEdit.print 等内部使用）。 */
    public long nativeHandle() {
        return nativeHandle;
    }

    /** 释放 C++ 对象（打印完成后调用；一般无需手动）。 */
    public void disposePdf() {
        if (nativeHandle != 0) {
            nativeDispose(nativeHandle);
            nativeHandle = 0;
        }
    }
    private native void nativeDispose(long handle);

    private native long nativeCreate();
    private native void nativeSetOutputFormat(long handle, int format);
    private native void nativeSetOutputFileName(long handle, String path);
    private native void nativeSetResolution(long handle, int dpi);
    private native void nativeSetPageSize(long handle, int size);
    private native boolean nativeNewPage(long handle);
}
