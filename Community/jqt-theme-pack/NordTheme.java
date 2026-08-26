/*
 * JQt Theme Pack - Nord（北极蓝 · 暗色）
 * (C) SilentStudio
 * All rights reserved.
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */

import java.util.HashMap;
import java.util.Map;

/**
 * Nord 主题 —— 北极圈冷色调，程序员经典配色。
 * 暗色主题，用于: app.setTheme("themes/fluent.qss.tpl", NordTheme.vars(), false);
 */
public class NordTheme {

    public static Map<String, String> vars() {
        Map<String, String> m = new HashMap<>();
        m.put("win-bg", "#2E3440");          m.put("fg", "#D8DEE9");
        m.put("fg-strong", "#ECEFF4");       m.put("fg-hint", "#8A94A8");
        m.put("fg-disabled", "#4C566A");
        m.put("card-bg", "#3B4252");         m.put("card-border", "#434C5E");
        m.put("btn-bg", "#434C5E");          m.put("btn-fg", "#D8DEE9");
        m.put("btn-hover", "#4C566A");       m.put("btn-pressed", "#3B4252");
        m.put("btn-disabled", "#343B49");
        m.put("accent", "#88C0D0");          m.put("accent-fg", "#2E3440");
        m.put("accent-hover", "#8FBCBB");
        m.put("input-bg", "#3B4252");        m.put("input-border", "#4C566A");
        m.put("nav-fg", "#D8DEE9");          m.put("nav-hover", "#434C5E");
        m.put("nav-selected", "#4C566A");
        m.put("switch-off", "#4C566A");      m.put("switch-off-hover", "#5E81AC");
        return m;
    }
}
