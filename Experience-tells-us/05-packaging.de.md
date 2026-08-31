# 05 packen und auspacken als start vorbereiten

> Über die paketweise zur JQt - anwendung und zur installation Von Qt informationen
> Erdateityp: jl-ordner, Qt DLL ll, modul pfad, SQL drive und mehrdimensionale kohärenz

## 1. Stark zur oberfläche wie alle anderen auch zur oberfläche wie alle anderen auch.

```
JQtGallery-Pack/
├── JQtGallery/                 # 免安装版（整个文件夹一起分发）
│   ├── JQtGallery.exe          # jpackage 启动器
│   ├── jqt.dll                 # JQt native 库（必须在 exe 目录！）
│   ├── Qt6*.dll                # Qt 运行时（Core/Gui/Widgets/...）
│   ├── platforms/qwindows.dll  # QPA 平台插件
│   ├── sqldrivers/qsqlite.dll  # SQL 驱动插件
│   ├── app/
│   │   ├── gallery.jar         # 应用 fat jar
│   │   └── JQtGallery.cfg      # jpackage 配置
│   └── src/                    # 源码副本（分发用）
├── lib/gallery.jar             # JDK 模式主程序
└── runtime/                    # JDK 模式 Qt 运行时
```

## 2. Schlüsselpfad-regeln (alles heruntergetreten)

| Regeln. | Gründe? 
|------|------|
| **Die sache ist tatsächlich seltsam**Zur oberfläche wie alle anderen auch. | Installieren sie Qt nicht Von hand 
| **Qt DLL ll muss in exe-ordner sein** | Windows ladegeräte Laufen auf exe - ordner 
| **Das j.qt.dll muss in exe-ordner sein** | Gleiche sache. 
| `-Djava.library.path=$APPDIR/..` | Ordne dein apps dazu, die app/ das dll mit deinen oberklassen zu ändern 
| qt.conf `Plugins = .` | Modul = exie-ordner = exe 
| **Das jd.-system kommt mit plu_path klar** | Der befehl für java-apps ist ein adressbuch für java exe, qt conf ist nicht gültig 

## 3. Die zwei mechanismen des plugdows-pfads (großer pool)

Suche nach einem sql-fahrzeug für j.qt:`QCoreApplication::libraryPaths()` + `QLibraryInfo::PluginsPath`.

- **Im exe-modell.**: zz arphs enthält einen exe-katalog plus qt conf Plugins.
`plugins/sqldrivers/qsqlite.dll` mit `sqldrivers/qsqlite.dll` **Lasse beides**Ist der weiche.
- **Das jd.d.-modell (start gallery, gewinner). Mach ich.**Das inhaltsverzeichnis ist java exe.`C:\...\javapath`(lord)
Ich kriege sie nicht **Errichten muss ` QT_PLUGIN_PATH = % RT % \ plugins ` die**.
Oder QSqlDatabase. AddDatabase (" QSQLITE "), IllegalStateException
"Die nicht verfügbar (SQLITE nicht finden. Plugins/sqldrivers/qsqlite)".

```bat
set "RT=%~dp0runtime"
set "QT_QPA_PLATFORM_PLUGIN_PATH=%RT%platforms"
set "QT_PLUGIN_PATH=%RT%plugins"
set "PATH=%RT%;%PATH%"
```

## 4. fat jar aufbau

- `jar --update --file gallery.jar -C workdir JQtGallery.class` Ersatz für den offiziellen jar
demo class (offizielle jar mit jqtallery.class, startet niemals wieder in der alten form.).
- Design in jar:`jar --update --file gallery.jar -C . themes/fluent.qss.tpl`,
Ich kriege sie, ohne externe dokumente!
- Wenn sie in einer fat jar rumlaufen **Was? - die classpath bekommt ihr hier**[en] er [en] druckt sein eigenes outback 10 (v06.jar; * * ,
Andernfalls könnte die bonustante shadow die klasse Von ihnen sein.

## 5. Die version ist konsistenter (nach entsendung notwendig)

Die PowerShell hashi zeigt alle verteilorte an:

```
gallery.jar 三处一致（lib / pack-lib / pack-app）
jqt.dll 三处一致（runtime / pack / repo-lib）
源码四处一致（本地 / pack-src / staging / repo）
```

**.**Das ist eine PowerShell. `Copy-Item` Ein dokument mit mehreren quellen lässt sich nicht in ein ordner einfügen
(nur der erste wird kopiert**Copy und Item** Geh wieder zu chash. Glaube keinen befehl.

## 6. Die existierenden bibliotheken ergänzen sich

- Das jqt veröffentlicht eine ausgabe: jqt-xar-(Java API) plus windows-x64.zip (während des aktiven bug) und FKK -depot
3 - Oder was ist das für ein zirkus? (jqt-windows- 6,12 - häute.dll, arm64, linux - so, macos dylib)
- **In doppelter Qt versionen**(6,8.3 LTS + 6,12) : die in den k. 4 -flachkanal integrierte doppelversion ist nur grün herausgebracht worden
- Qt 9+ die neue API präsentiert präsentiert präsentiert präsentiert präsentiert präsentiert präsentiert präsentiert präsentiert präsentiert präsentiert präsentiert präsentiert präsentiert präsentiert das gesicht einer autodiesischen brde-brüder (lucessier lutscherin).
- Eine. Enthält plugins/sqldrivers/qsqlite DLL (SQLite kiste und).

