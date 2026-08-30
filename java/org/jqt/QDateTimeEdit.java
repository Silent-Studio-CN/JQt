/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 日期时间编辑框：封装 C++ 侧的 {@code QDateTimeEdit}（初始为当前时间）。
 * <p>信号槽：{@link #onTextChanged(Consumer)} — 编辑框文本变化（按当前显示格式）。
 */
public class QDateTimeEdit extends QWidget {

    private final List<Consumer<String>> onTextChangedHandlers = new ArrayList<>();

    public QDateTimeEdit() {
        nativeHandle = nativeCreate();
        registerCleaner();
    }

    private native long nativeCreate();

    /** 设置显示格式，如 "yyyy-MM-dd HH:mm:ss"。 */
    public void setDisplayFormat(String format) {
        nativeSetDisplayFormat(nativeHandle, format);
    }
    private native void nativeSetDisplayFormat(long handle, String format);

    /** 设置日期时间。 */
    public void setDateTime(int year, int month, int day, int hour, int minute, int second) {
        nativeSetDateTime(nativeHandle, year, month, day, hour, minute, second);
    }
    private native void nativeSetDateTime(long handle, int year, int month, int day, int hour, int minute, int second);

    /** 当前显示文本（按显示格式）。 */
    public String text() {
        return nativeText(nativeHandle);
    }
    private native String nativeText(long handle);

    /** 文本变化回调（参数为当前显示文本）。 */
    public QDateTimeEdit onTextChanged(Consumer<String> handler) {
        onTextChangedHandlers.add(handler);
        return this;
    }

    /** 由 C++ 侧回调（JNI）。 */
    void nativeHandleTextChanged(String text) {
        for (Consumer<String> h : onTextChangedHandlers) {
            h.accept(text);
        }
    }

// ---- 生成器批次（jqt-gen 自动生成，直传型） ----
    /** calendarPopup（Qt calendarPopup）。 */
    public boolean calendarPopup() {
        return nativeCalendarPopup(nativeHandle);
    }
    private static native boolean nativeCalendarPopup(long nativeHandle);

    /** clear（Qt clear）。 */
    public void clear() {
        nativeClear(nativeHandle);
    }
    private static native void nativeClear(long nativeHandle);

    /** clearMaximumDate（Qt clearMaximumDate）。 */
    public void clearMaximumDate() {
        nativeClearMaximumDate(nativeHandle);
    }
    private static native void nativeClearMaximumDate(long nativeHandle);

    /** clearMaximumDateTime（Qt clearMaximumDateTime）。 */
    public void clearMaximumDateTime() {
        nativeClearMaximumDateTime(nativeHandle);
    }
    private static native void nativeClearMaximumDateTime(long nativeHandle);

    /** clearMaximumTime（Qt clearMaximumTime）。 */
    public void clearMaximumTime() {
        nativeClearMaximumTime(nativeHandle);
    }
    private static native void nativeClearMaximumTime(long nativeHandle);

    /** clearMinimumDate（Qt clearMinimumDate）。 */
    public void clearMinimumDate() {
        nativeClearMinimumDate(nativeHandle);
    }
    private static native void nativeClearMinimumDate(long nativeHandle);

    /** clearMinimumDateTime（Qt clearMinimumDateTime）。 */
    public void clearMinimumDateTime() {
        nativeClearMinimumDateTime(nativeHandle);
    }
    private static native void nativeClearMinimumDateTime(long nativeHandle);

    /** clearMinimumTime（Qt clearMinimumTime）。 */
    public void clearMinimumTime() {
        nativeClearMinimumTime(nativeHandle);
    }
    private static native void nativeClearMinimumTime(long nativeHandle);

    /** currentSectionIndex（Qt currentSectionIndex）。 */
    public int currentSectionIndex() {
        return nativeCurrentSectionIndex(nativeHandle);
    }
    private static native int nativeCurrentSectionIndex(long nativeHandle);

    /** sectionCount（Qt sectionCount）。 */
    public int sectionCount() {
        return nativeSectionCount(nativeHandle);
    }
    private static native int nativeSectionCount(long nativeHandle);

    /** setCalendarPopup（Qt setCalendarPopup）。 */
    public void setCalendarPopup(boolean arg0) {
        nativeSetCalendarPopup(nativeHandle, arg0);
    }
    private static native void nativeSetCalendarPopup(long nativeHandle, boolean arg0);

    /** setCurrentSectionIndex（Qt setCurrentSectionIndex）。 */
    public void setCurrentSectionIndex(int arg0) {
        nativeSetCurrentSectionIndex(nativeHandle, arg0);
    }
    private static native void nativeSetCurrentSectionIndex(long nativeHandle, int arg0);

    /** stepBy（Qt stepBy）。 */
    public void stepBy(int arg0) {
        nativeStepBy(nativeHandle, arg0);
    }
    private static native void nativeStepBy(long nativeHandle, int arg0);

}