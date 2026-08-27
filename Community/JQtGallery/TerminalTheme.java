/*
 * JQt Theme Pack - Terminal（荧光绿 · 暗色赛博）
 * (C) SilentStudio
 * All rights reserved.
 * SPDX-License-Identifier: LicenseRef-SilentStudio-JQt-1.0
 * Licensed under the JQt Source License v1.0 - see LICENSE.md.
 */

import java.util.HashMap;
import java.util.Map;

/**
 * Terminal 主题 —— 黑底荧光绿，复古终端赛博风。
 * 暗色主题，用于: app.setTheme("themes/fluent.qss.tpl", TerminalTheme.vars(), false);
 */
public class TerminalTheme {

    public static Map<String, String> vars() {
        Map<String, String> m = new HashMap<>();
        m.put("win-bg", "#0A0F0A");          m.put("fg", "#33FF66");
        m.put("fg-strong", "#66FF99");       m.put("fg-hint", "#1F9E4A");
        m.put("fg-disabled", "#14532D");
        m.put("card-bg", "#0F1A10");         m.put("card-border", "#1F3A24");
        m.put("btn-bg", "#122615");          m.put("btn-fg", "#33FF66");
        m.put("btn-hover", "#1A3D1F");       m.put("btn-pressed", "#0F2A13");
        m.put("btn-disabled", "#0C1A0E");
        m.put("accent", "#00FF44");          m.put("accent-fg", "#0A0F0A");
        m.put("accent-hover", "#33FF66");
        m.put("input-bg", "#0A0F0A");        m.put("input-border", "#1F3A24");
        m.put("nav-fg", "#33FF66");          m.put("nav-hover", "#122615");
        m.put("nav-selected", "#1A3D1F");
        m.put("switch-off", "#1F3A24");      m.put("switch-off-hover", "#2E5A36");
        return m;
    }
}
