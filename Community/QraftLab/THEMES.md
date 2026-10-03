# 主题文件位置说明

本目录**不再自带 QSS 副本**。QraftLab 的样式统一取自仓库根目录的
`themes/`（加载器 `QraftLab.java` 先读文件 `themes/qraft-styles.qss`，
失败再回退到 classpath `/themes/qraft-styles.qss`）。

历史上这里曾放一份逐字节相同的副本，属于**死代码**且会随根目录修改而漂移
（第三方问题报告 G2 就是据此提出的）。请在根目录 `themes/` 维护唯一来源。
