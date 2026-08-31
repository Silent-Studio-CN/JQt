# Experience Tells Us — JQt-Entwicklungserfahrung & Stolperfallen

> In diesem Verzeichnis werden die Erfahrungen und Lehren aus der JQt-Entwicklung (Java bindings for Qt) gesammelt.
> Der Fokus liegt auf der JQt-Entwicklung selbst: Java-API-Design, JNI/native-Bridge, Qt-Verhaltensfallen,
> Windows-Plattformfunktionen, Theme-Rendering, Packaging/Distribution und Community-Konventionen.
> Geschrieben von einem Mitglied der AI-Engineering-Richtung, das an der JQt-Entwicklung beteiligt ist;
> kontinuierlich ergänzt mit jeder Version.

## Meine Hauptverantwortungsbereiche

1. **JQtGallery Community-Demoprojekt** (Community/JQtGallery)
   - Funktions-Demos (Themen/Widgets/Animationen/Fenster, neue APIs von v0.5 bis v0.7.5)
   - Automatik-Demo-Modus (-Dg.auto=1, klickbasierte Verifikation) und Probe-Tests
   - Jedes Release verfolgt: v0.6 → v0.6.1 → v0.7.0~v0.7.5 vollständig
2. **JQt native Layer: Windows-Plattform-Fehleranalyse & Reparatur**
   - setFrameless-Hot-Switch defekt (Win32-Styles + DWM-Extended-Border) — behoben
   - Fenster-Rebuild/Layout-Drift, Fixed-Size-Constraints, Touch-Synthese-Koordinaten
3. **Theme-Rendering-System**
   - fluent.qss.tpl Template + Variablen-Tabellen-Rendering
   - Dual-Theme (hell/dunkel) Wechsel, Akzentfarben dynamische Variablen
4. **Packaging & Distribution**
   - jpackage App-Image, Qt-Runtime-Deployment, Plugin-Pfade (qt.conf / QT_PLUGIN_PATH)
   - Multi-Pfad-Deployment-Konsistenzprüfung
5. **Community-Kollaboration & Release**
   - Versionsarchiv-Konvention (Root = neueste + vX.Y/ Unterordner)
   - Release-Notes im Drei-Abschnitt-Format, Testberichte

## Dokument-Index

| Datei | Thema |
|------|------|
| [01-window-native.md](01-window-native.de.md) | Win32-Fenstersystem & native Layer (setFrameless komplett gelöst) |
| [02-theme-qss.md](02-theme-qss.de.md) | Theme-Rendering & QSS (Template-Variablen/Priorität/Residuen) |
| [03-java-api.md](03-java-api.de.md) | Java-API-Design & Nutzungsfallen |
| [04-lifecycle-threads.md](04-lifecycle-threads.de.md) | Objektlebenszyklus, Threads, Signal-Callbacks |
| [05-packaging.md](05-packaging.de.md) | Packaging/Distribution & Runtime-Deployment |
| [06-community.md](06-community.de.md) | Community-Konventionen & Release-Prozess |
| [07-probes.md](07-probes.de.md) | Probe-Testmethodik (native Probleme reproduzieren) |
| [08-setFrameless-case.md](08-setFrameless-case.md) | setFrameless-Reparatur komplett (native Troubleshooting-Beispiel) |
