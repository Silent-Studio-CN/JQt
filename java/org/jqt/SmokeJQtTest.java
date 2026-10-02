/*
 * JQt - QTest 兼容层自测冒烟（v1.9.1 P1）。
 * 用一个"故意包含 通过/失败/跳过 三类"的测试类去跑运行器,断言计数与钩子语义正确。
 */
package org.jqt;

import java.util.ArrayList;
import java.util.List;

public class SmokeJQtTest {

    static int pass = 0;
    static int fail = 0;

    static void check(String name, boolean cond) {
        if (cond) { pass++; System.out.println("[jt] OK   " + name); }
        else { fail++; System.out.println("[jt] FAIL " + name); }
    }

    /** 被运行的样例测试类:init/cleanup 钩子 + 通过/失败/跳过各一个。 */
    public static class SampleTest {
        static final List<String> trace = new ArrayList<>();
        private int value;

        public void init() { trace.add("init"); value = 41; }
        public void cleanup() { trace.add("cleanup"); }

        public void testA_verify() {
            trace.add("testA");
            JQtTest.QVERIFY(value == 41);
            JQtTest.QVERIFY2(1 + 1 == 2, "算术");
        }

        public void testB_compare() {
            trace.add("testB");
            JQtTest.QCOMPARE(42, 42);
            JQtTest.QCOMPARE("abc", "abc");
            JQtTest.QCOMPARE(3.0, 3.05, 0.1);
            JQtTest.QCOMPARE(true, true);
        }

        public void testC_intentionallyFails() {
            trace.add("testC");
            JQtTest.QCOMPARE(1, 2);          // 故意失败
        }

        public void testD_intentionallySkips() {
            trace.add("testD");
            JQtTest.QSKIP("不在本平台跑");
        }

        public void testE_freshInstance() {
            trace.add("testE");
            // 每个测试方法都是新实例:value 由 init 重置为 41
            JQtTest.QCOMPARE(value, 41);
        }

        /** 非 test* 方法不应被执行。 */
        public void helperNotATest() { trace.add("helper"); }
    }

    /** 另一个样例类:验证空测试类的处理。 */
    public static class NoTests { public void notATest() {} }

    public static void main(String[] args) {
        System.out.println("[jt] start");

        SampleTest.trace.clear();
        JQtTest.Result r = JQtTest.run(SampleTest.class, NoTests.class);

        check("通过 3 个(testA/testB/testE)", r.passed() == 3);
        check("失败 1 个(testC)", r.failed() == 1);
        check("跳过 1 个(testD)", r.skipped() == 1);
        check("总数 5", r.total() == 5);
        check("结果 ok() = false(有失败)", !r.ok());
        check("失败明细含方法名", r.failures().size() == 1
              && r.failures().get(0).startsWith("SampleTest::testC_intentionallyFails"));
        check("失败信息含实际/期望", r.failures().get(0).contains("实际 <1>") 
              && r.failures().get(0).contains("期望 <2>"));

        long inits = SampleTest.trace.stream().filter("init"::equals).count();
        long cleanups = SampleTest.trace.stream().filter("cleanup"::equals).count();
        check("init 钩子跑了 5 次(实际 " + inits + ")", inits == 5);
        check("cleanup 钩子跑了 5 次(实际 " + cleanups + ")", cleanups == 5);
        check("非 test* 方法未被执行", !SampleTest.trace.contains("helper"));

        // 断言 API 的边界:QVERIFY2 失败时消息带到异常里
        String msg = null;
        try { JQtTest.QVERIFY2(false, "自定义说明"); }
        catch (AssertionError e) { msg = e.getMessage(); }
        check("QVERIFY2 失败消息正确(实际 " + msg + ")", "自定义说明".equals(msg));

        String skipMsg = null;
        try { JQtTest.QSKIP("跳过说明"); }
        catch (JQtTest.SkipException e) { skipMsg = e.getMessage(); }
        check("QSKIP 抛 SkipException(实际 " + skipMsg + ")", "跳过说明".equals(skipMsg));

        System.out.println("[jt] pass=" + pass + " fail=" + fail);
        System.out.println("[jt] " + (fail == 0 ? "ALL PASS ✅" : ("FAILED: " + fail)));
        if (fail > 0) System.exit(1);
    }
}
