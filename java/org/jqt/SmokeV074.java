/*
 * JQt - v0.7.4 QSerialPort 冒烟（无设备环境：枚举 + 配置 + open 失败不崩）。
 */
package org.jqt;

public class SmokeV074 {

    public static void main(String[] args) {
        System.out.println("[v074] start");
        QApplication app = new QApplication();

        // 端口枚举
        java.util.List<String> ports = QSerialPort.availablePorts();
        System.out.println("[v074] availablePorts count=" + ports.size() + " " + ports);

        // 构造 + 配置
        QSerialPort port = new QSerialPort("COM9");
        System.out.println("[v074] portName=" + port.portName());
        boolean br = port.setBaudRate(115200);
        System.out.println("[v074] setBaudRate(115200)=" + br + " baudRate=" + port.baudRate());
        port.setDataBits(QSerialPort.DataBits.DATA_8);
        port.setParity(QSerialPort.Parity.NO_PARITY);
        port.setStopBits(QSerialPort.StopBits.ONE_STOP);
        port.setFlowControl(QSerialPort.FlowControl.NO_FLOW_CONTROL);

        // 信号注册（不触发不崩）
        final int[] ready = { 0 };
        port.onReadyRead(() -> ready[0]++);
        port.onBytesWritten(n -> {});

        // open（无设备 → false 不崩）
        boolean opened = port.open(QSerialPort.OpenMode.READ_WRITE);
        System.out.println("[v074] open(COM9)=" + opened + " isOpen=" + port.isOpen()
            + " error=" + port.errorString());

        // 关闭 + 清理
        port.close();
        System.out.println("[v074] close ok, isOpen=" + port.isOpen());
        port.clear();
        System.out.println("[v074] clear ok");

        // bytesAvailable / readAll（未打开时安全）
        System.out.println("[v074] bytesAvailable=" + port.bytesAvailable() + " readAll len="
            + (port.readAll() == null ? 0 : port.readAll().length));

        app.scheduleQuit(300);
        app.exec();
        System.out.println("[v074] PASS");
    }
}
