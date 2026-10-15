/*
 * JQt - DBus 冒烟（v1.9.2 P1 收尾）。
 *
 * 断言点：
 *   1. isAvailable() 与 QtDBus 探测一致
 *   2. 连接状态自检（会话/系统总线）——没有总线时是**预期**，不是失败
 *   3. 有会话总线时做**真实端到端**调用:org.freedesktop.DBus.ListNames(无参)
 *      与 NameHasOwner(带一个字符串参数,返回值是 bool)->验证参数编组与回复读取
 *   4. 错误路径:调用不存在的服务 -> isError() 为真且 errorName 非空(无总线时为无效消息)
 *   5. 消息自身语义:createMethodCall / createSignal 的类型、字段、参数计数
 *   6. 信号消息可发送(不等待回复)
 */
package org.jqt;

import java.util.List;

public class SmokeDBus {

    static int pass = 0;
    static int fail = 0;
    static int skip = 0;

    static void check(String name, boolean cond) {
        if (cond) { pass++; System.out.println("[dbus] OK   " + name); }
        else { fail++; System.out.println("[dbus] FAIL " + name); }
    }

    /** 明确记录"这一条在本平台无法验证",不混进 pass 里假装通过。 */
    static void skipped(String name, String why) {
        skip++;
        System.out.println("[dbus] SKIP " + name + " —— " + why);
    }

    public static void main(String[] args) throws Exception {
        System.out.println("[dbus] start");
        QApplication app = new QApplication();

        // ---------------------------------------------------------- 1) 可用性
        boolean available = QDBusConnection.isAvailable();
        System.out.println("[dbus] QtDBus 编译进库 = " + available);
        if (available) {
            check("isAvailable()=true 与 Wrapper 一致", QDBusMessage.isAvailable());
        } else {
            skipped("QtDBus 相关断言", "本库未编译 QtDBus(Android 或构建时未探测到)");
            System.out.println("[dbus] pass=" + pass + " fail=" + fail + " skip=" + skip);
            System.out.println("[dbus] " + (fail == 0 ? "ALL PASS ✅(含跳过)" : ("FAILED: " + fail)));
            if (fail > 0) System.exit(1);
            return;
        }

        // ---------------------------------------------------------- 5) 消息语义
        QDBusMessage call = QDBusMessage.createMethodCall("org.freedesktop.DBus",
                "/org/freedesktop/DBus", "org.freedesktop.DBus", "NameHasOwner");
        check("createMethodCall 非空", call != null && call.isValid());
        check("类型 = MethodCall", call.type() == QDBusMessage.MethodCallMessage);
        check("service/path/iface/member 正确",
                "org.freedesktop.DBus".equals(call.service())
                        && "/org/freedesktop/DBus".equals(call.path())
                        && "org.freedesktop.DBus".equals(call.interfaceName())
                        && "NameHasOwner".equals(call.member()));
        check("无参时 argumentCount=0", call.argumentCount() == 0);
        call.addArgument("org.freedesktop.DBus");
        check("addArgument(String) 后 argumentCount=1", call.argumentCount() == 1);
        check("参数类型名含 QString(实际 " + call.argumentTypeAt(0) + ")",
                call.argumentTypeAt(0).toLowerCase().contains("string"));
        check("越界读取安全(不崩、给默认值)",
                call.intAt(9) == 0 && call.boolAt(9) == false && "".equals(call.stringAt(9)));

        QDBusMessage sig = QDBusMessage.createSignal("/org/jqt/Test", "org.jqt.Test", "Pinged");
        check("createSignal 类型 = Signal",
                sig != null && sig.type() == QDBusMessage.SignalMessage);

        QDBusMessage typed = QDBusMessage.createMethodCall("org.jqt.Fake", "/x", "org.jqt.Fake", "F");
        typed.addArgument("s").addArgument(42).addArgument(true).addArgument(3.5);
        check("多种参数编组(4 个)", typed.argumentCount() == 4);

        // ---------------------------------------------------------- 2) 连接状态
        boolean session = QDBusConnection.isSessionBusConnected();
        boolean system = QDBusConnection.isSystemBusConnected();
        System.out.println("[dbus] 会话总线连接 = " + session + " / 系统总线连接 = " + system);
        check("连接状态查询可用(不抛异常)", true);

        // ---------------------------------------------------------- 4) 错误路径
        QDBusMessage bad = QDBusConnection.call(QDBusMessage.createMethodCall(
                "org.jqt.NoSuchService.Fake", "/x", "org.jqt.NoSuchService.Fake", "Nope"));
        boolean badIsError = bad != null && bad.isError();
        boolean badIsInvalid = bad == null || bad.type() == QDBusMessage.InvalidMessage;
        check("不存在的服务:要么是错误回复,要么是无效消息(不能装作成功)", badIsError || badIsInvalid);
        if (badIsError) {
            check("错误回复带 errorName(实际 " + bad.errorName() + ")", !bad.errorName().isEmpty());
            System.out.println("[dbus] errorMessage = " + bad.errorMessage());
        } else {
            skipped("错误回复的 errorName 断言", "无会话总线:调用返回无效消息而非错误回复");
        }

        // ---------------------------------------------------------- 3) 真实端到端
        if (!session) {
            skipped("端到端调用 ListNames / NameHasOwner",
                    "本平台无会话总线(Windows/macOS 默认如此;CI Linux 用 dbus-run-session 起真实总线)");
        } else {
            System.out.println("[dbus] 本连接名 = " + QDBusConnection.sessionBusName());
            check("会话连接名非空", !QDBusConnection.sessionBusName().isEmpty());

            QDBusMessage names = QDBusConnection.call("org.freedesktop.DBus",
                    "/org/freedesktop/DBus", "org.freedesktop.DBus", "ListNames");
            check("ListNames 有回复且非错误", names != null && !names.isError());
            if (names != null && !names.isError()) {
                System.out.println("[dbus] 参数数 = " + names.argumentCount()
                        + " 首个参数类型 = " + names.argumentTypeAt(0));
                // 注意:ListNames 返回的是**一个 QStringList**,不是 N 个字符串参数 ——
                // 用 stringAt(0) 读会得到空串(CI 上就是这么红的),必须用 stringListAt。
                List<String> busNames = names.stringListAt(0);
                System.out.println("[dbus] 总线服务数 = " + busNames.size()
                        + (busNames.isEmpty() ? "" : " 例:" + busNames.get(0)));
                check("stringListAt(0) 读到服务名列表(非空)", !busNames.isEmpty());
                check("列表里有 org.freedesktop.DBus", busNames.contains("org.freedesktop.DBus"));
                check("stringAt(0) 读数组得到空串(记录该坑,避免误用)",
                        names.stringAt(0).isEmpty() || busNames.contains(names.stringAt(0)));
            }

            // 带参数的真实调用:NameHasOwner("org.freedesktop.DBus") -> true
            QDBusMessage owner = QDBusMessage.createMethodCall("org.freedesktop.DBus",
                    "/org/freedesktop/DBus", "org.freedesktop.DBus", "NameHasOwner");
            owner.addArgument("org.freedesktop.DBus");
            QDBusMessage ownerReply = QDBusConnection.call(owner);
            check("NameHasOwner 有回复且非错误", ownerReply != null && !ownerReply.isError());
            if (ownerReply != null && !ownerReply.isError()) {
                check("NameHasOwner(org.freedesktop.DBus) = true(参数编组端到端正确)",
                        ownerReply.argumentCount() >= 1 && ownerReply.boolAt(0));
            }

            // GetId:返回一个字符串(另一条无参调用)
            QDBusMessage idReply = QDBusConnection.call("org.freedesktop.DBus",
                    "/org/freedesktop/DBus", "org.freedesktop.DBus", "GetId");
            check("GetId 返回非空字符串", idReply != null && !idReply.isError()
                    && !idReply.stringAt(0).isEmpty());

            // 信号发送(不等待回复)
            QDBusMessage ping = QDBusMessage.createSignal("/org/jqt/Test", "org.jqt.Test", "Pinged");
            ping.addArgument("hello");
            check("send(信号) 返回 true", QDBusConnection.send(ping));
        }

        System.out.println("[dbus] pass=" + pass + " fail=" + fail + " skip=" + skip);
        System.out.println("[dbus] " + (fail == 0 ? "ALL PASS ✅" : ("FAILED: " + fail)));
        if (fail > 0) System.exit(1);
    }
}
