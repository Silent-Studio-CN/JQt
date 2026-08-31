# Experience Tells Us - JQt development experience and Pitfalls recorded

> This directory compiles the experiences and lessons accumulated during the development process of JQt (Java bindings for Qt).
> Content Focus: "JQt Development Itself" : Java API design, JNI/native Bridging, Qt Behavioral traps
> Windows platform features, theme rendering, packaging and distribution, community engineering conventions.
> Written by members of the AI engineering direction who participated in the development of JQt, and continuously supplemented as versions evolve.

## My main responsibility direction

1. **JQtGallery Community Demonstration Project ** (Community/JQtGallery
- Functional partition Demonstration (Theme/Controls/Animations/Windows/New apis of v0.5 to v0.7.5 versions)
- Automatic demonstration mode (-Dg.auto=1 click to verify one by one) and probe testing
- Follow each release update: v0.6 → v0.6.1 → v0.7.0 to v0.7.5
2. Troubleshooting and Fixing JQt native Layer Issues on the Windows Platform
- setFrameless hot swapping failed (Win32 style bit + DWM extended border) - has been completely resolved
- Window reconstruction/layout drift, fixed size constraints, touch synthesis event coordinates
3. "Theme Rendering System
- fluent.qss.tpl template + variable table rendering mechanism
- Dual themes (light/dark) switching, emphasizing color dynamic variables
4. Packaging and distribution
- jpackage application image, qt runtime deployment, plugin path (qt.conf/QT_PLUGIN_PATH)
- Multi-location deployment consistency verification
5. "Community Collaboration and Release.
- Version archiving convention (root directory = latest + vX.Y/ subdirectory)
- Release instructions in three-part format and test reports

## Document index

| 文件 | 主题 |
|------|------|
| [01-window-native.md](01-window-native.en.md) | Win32 Window System and native Layer (Complete Solution to the setFrameless Pitfall)
| [02-theme-qss.md](02-theme-qss.en.md) | Theme Rendering and QSS (Template Variables/Priorities/Residues)
| [03-java-api.md](03-java-api.en.md) | Java API Design and Usage Traps
| [04-lifecycle-threads.md](04-lifecycle-threads.en.md) | Object lifecycle, thread, signal callback
| [05-packaging.md](05-packaging.en.md) | Packaging, distribution and runtime deployment
| [06-community.md](06-community.en.md) | Community engineering agreement and release process
| [07-probes.md](07-probes.en.md) | Probe Testing Methodology (Reproducing native Issues)
| [08-setFrameless-case.md](08-setFrameless-case.md) | setFrameless Repair Full Record (native Troubleshooting Example)

