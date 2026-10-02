/*
 * JQt - Java bindings for Qt.
 * Copyright (c) SilentStudio
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */
package org.jqt;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * QTest 兼容层（v1.9.1 P1）：用 QtTest 的写法给 JQt 应用写测试。
 * <pre>
 * public class MyWidgetTest {
 *     private QApplication app;
 *
 *     public void init()    { app = new QApplication(); }      // 每个测试前
 *     public void cleanup() { app.quit(); }                    // 每个测试后
 *
 *     public void testButtonClick() {
 *         QPushButton b = new QPushButton("ok");
 *         JQtTest.QCOMPARE(b.text(), "ok");
 *         JQtTest.QVERIFY(b.isEnabled());
 *     }
 *
 *     public static void main(String[] args) {                 // 或直接
 *         JQtTest.runAndExit(MyWidgetTest.class);
 *     }
 * }
 * </pre>
 * 约定（与 QtTest 一致）：测试方法名以 {@code test} 开头、无参数、public;
 * 可选 {@code init()} / {@code cleanup()} 在每个测试前后各跑一次;
 * 断言失败抛 {@link AssertionError} 并被计为失败;{@link #QSKIP(String)} 计为跳过。
 * <p>为什么不用 Qt 的 QTest:JQt 的测试要跑在 Java 侧（构造控件、断言 API），
 * 用 QtTest 的 C++ 宏没意义;这里提供**相同的语义与输出风格**,零原生依赖。
 */
public final class JQtTest {

    private JQtTest() {}

    /** 跳过当前测试（QtTest 的 QSKIP）。 */
    public static final class SkipException extends RuntimeException {
        public SkipException(String message) { super(message); }
    }

    // ==================== 断言 ====================

    /** 断言为真。 */
    public static void QVERIFY(boolean condition) {
        QVERIFY2(condition, "QVERIFY 失败");
    }

    /** 断言为真，失败时附带说明。 */
    public static void QVERIFY2(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    /** 断言相等（对象、数值、字符串统一入口）。 */
    public static void QCOMPARE(Object actual, Object expected) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("QCOMPARE 失败: 实际 <" + actual + "> 期望 <" + expected + ">");
        }
    }

    public static void QCOMPARE(int actual, int expected) {
        if (actual != expected) {
            throw new AssertionError("QCOMPARE 失败: 实际 <" + actual + "> 期望 <" + expected + ">");
        }
    }

    public static void QCOMPARE(long actual, long expected) {
        if (actual != expected) {
            throw new AssertionError("QCOMPARE 失败: 实际 <" + actual + "> 期望 <" + expected + ">");
        }
    }

    public static void QCOMPARE(boolean actual, boolean expected) {
        if (actual != expected) {
            throw new AssertionError("QCOMPARE 失败: 实际 <" + actual + "> 期望 <" + expected + ">");
        }
    }

    /** 浮点比较（容差）。 */
    public static void QCOMPARE(double actual, double expected, double delta) {
        if (Math.abs(actual - expected) > delta) {
            throw new AssertionError("QCOMPARE 失败: 实际 <" + actual + "> 期望 <" + expected
                                   + "> 容差 <" + delta + ">");
        }
    }

    /** 直接判定失败。 */
    public static void QFAIL(String message) {
        throw new AssertionError(message);
    }

    /** 跳过当前测试。 */
    public static void QSKIP(String message) {
        throw new SkipException(message == null ? "跳过" : message);
    }

    // ==================== 运行器 ====================

    /** 一次测试运行的结果。 */
    public static final class Result {
        private final List<String> failures = new ArrayList<>();
        private int passed;
        private int failed;
        private int skipped;

        public int passed()  { return passed; }
        public int failed()  { return failed; }
        public int skipped() { return skipped; }
        public int total()   { return passed + failed + skipped; }
        public boolean ok()  { return failed == 0; }
        public List<String> failures() { return Collections.unmodifiableList(failures); }
    }

    /** 运行若干测试类（每个测试方法用**新的实例**，与 QtTest 一致）。 */
    public static Result run(Class<?>... classes) {
        Result r = new Result();
        for (Class<?> cls : classes) {
            List<Method> tests = new ArrayList<>();
            for (Method m : cls.getMethods()) {
                if (m.getName().startsWith("test") && m.getParameterCount() == 0
                        && Modifier.isPublic(m.getModifiers()) && !Modifier.isStatic(m.getModifiers())) {
                    tests.add(m);
                }
            }
            tests.sort((a, b) -> a.getName().compareTo(b.getName()));
            if (tests.isEmpty()) {
                System.out.println("[jqttest] 注意: " + cls.getSimpleName() + " 没有 test* 方法");
            }
            for (Method m : tests) {
                String id = cls.getSimpleName() + "::" + m.getName();
                Object inst;
                try {
                    inst = cls.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    r.failed++;
                    r.failures.add(id + " 无法实例化: " + e);
                    System.out.println("[jqttest] FAIL " + id + " (无法实例化)");
                    continue;
                }
                try {
                    invokeIfPresent(inst, "init");
                    m.invoke(inst);
                    r.passed++;
                    System.out.println("[jqttest] PASS " + id);
                } catch (Throwable t) {
                    Throwable cause = (t.getCause() != null) ? t.getCause() : t;
                    if (cause instanceof SkipException) {
                        r.skipped++;
                        System.out.println("[jqttest] SKIP " + id + " (" + cause.getMessage() + ")");
                    } else {
                        r.failed++;
                        String msg = cause.getClass().getSimpleName()
                                   + (cause.getMessage() == null ? "" : ": " + cause.getMessage());
                        r.failures.add(id + " → " + msg);
                        System.out.println("[jqttest] FAIL " + id + " → " + msg);
                    }
                } finally {
                    invokeIfPresent(inst, "cleanup");
                }
            }
        }
        System.out.println("[jqttest] 合计: 通过 " + r.passed + " / 失败 " + r.failed
                         + " / 跳过 " + r.skipped);
        for (String f : r.failures) System.out.println("[jqttest]   失败明细: " + f);
        return r;
    }

    /** 运行测试并在有失败时以退出码 1 结束（适合 CI / main 入口）。 */
    public static void runAndExit(Class<?>... classes) {
        Result r = run(classes);
        System.out.println("[jqttest] " + (r.ok() ? "ALL PASS ✅" : "FAILED"));
        if (!r.ok()) System.exit(1);
    }

    private static void invokeIfPresent(Object inst, String name) {
        try {
            Method m = inst.getClass().getMethod(name);
            m.invoke(inst);
        } catch (NoSuchMethodException ignored) {
            // 可选钩子
        } catch (Throwable t) {
            System.out.println("[jqttest] 警告: " + name + "() 抛出 " + t);
        }
    }
}
