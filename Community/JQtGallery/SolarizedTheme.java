/*
 * JQt Theme Pack - Solarized（护眼 · 亮色）
 * (C) SilentStudio
 * All rights reserved.
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */

import java.util.HashMap;
import java.util.Map;

/**
 * Solarized 主题 —— 米黄底护眼亮色，低对比度长时阅读友好。
 * 亮色主题，用于: app.setTheme("themes/fluent.qss.tpl", SolarizedTheme.vars(), true);
 */
public class SolarizedTheme {

    public static Map<String, String> vars() {
        Map<String, String> m = new HashMap<>();
        m.put("win-bg", "#FDF6E3");          m.put("fg", "#657B83");
        m.put("fg-strong", "#073642");       m.put("fg-hint", "#93A1A1");
        m.put("fg-disabled", "#B8C4C4");
        m.put("card-bg", "#EEE8D5");         m.put("card-border", "#E0D9C8");
        m.put("btn-bg", "#EEE8D5");          m.put("btn-fg", "#586E75");
        m.put("btn-hover", "#E4DCC9");       m.put("btn-pressed", "#D9D2BF");
        m.put("btn-disabled", "#F5EEDC");
        m.put("accent", "#268BD2");          m.put("accent-fg", "#FDF6E3");
        m.put("accent-hover", "#1F7BB8");
        m.put("input-bg", "#FDF6E3");        m.put("input-border", "#D0CBB8");
        m.put("nav-fg", "#657B83");          m.put("nav-hover", "#EEE8D5");
        m.put("nav-selected", "#E4DCC9");
        m.put("switch-off", "#D0CBB8");      m.put("switch-off-hover", "#B8C4C4");
        return m;
    }
}
