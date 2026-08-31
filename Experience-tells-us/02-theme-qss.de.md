# 02 - theory decade und QSS

> Jqt-themensystem: die offizielle neuerscheinung des funk: squeeze me(virgendwann).
> Dieses kapitel dokumentiert alle erfahrungen mit dem vorlagen -mechanismus, der stil der gewählten prioritäten und zu problemen

## 1. fluent qss, ein gpl-dip-system

- Die offizielle vorlage ist **22 var% % legen die var**(win/bg/ surfer / /btn /input /input /nav) .
- Ich kriege sie `replace("%"+key+"%", value)` ich `app.setStyleSheet(qss)`.
- **Anna: ein gedicht? Was?**: die irgendwann me me me me me me me
- Die vorlage laing priority: eines dateisystems es/ flüent.qss, gpl-vorrang, jar equipling location.
- Die community version ist häufig drei sätze (" fluent qss ") : gpl-modul (modul) und qraft-styles.qss (3d-knopf) + sck- gesaml-modul qss).

## 2. Stil priorisierung (position im inhaltsverzeichnis die am leichtesten zu trittende stelle)

Von oben nach unten:
1. **An einer actionfigur müsste stehen**(`widget.setStyleSheet(...)`(Von oben immer den überblick bewahren.
2. **Objektname wählt**(`QPushButton#themeBtn`) der zweithöchste
3. **Eine app/win app styheet.** Der global!

### Loch 1: es ist ein klassischer dunkler überschlag auf das motiv
```java
// 错误示范：启动时对每个控件单独 setStyleSheet 暗色
applyDarkStyles();   // 每个控件 setStyleSheet 暗色
// 之后无论怎么切全局主题，控件级样式优先级更高 → 永远暗色
```
**Heilung.**: speichergröße für die entsprechenden elemente entfernen und der globalen qss-vorlage in der wiederherstellung überbringen;
An gut ausgearbeiteter vorlage wird ein entfesselter mechanismus aus der schnell ausgefallenenen waffe gezogen, wenn eine waffe für den individuellen stil ausgewechselt wird.

### Hart verschlüsselte farben für den harten codierungsstil.
```java
topBar.setStyleSheet("QFrame#topBar { background-color: #1f1f1f; }");  // 硬编码
```
Sie hat höchste priorität, ich kriege sie nicht! Der rahmen ist schwarz!
**Heilung.**Lautsprecher entfernen und durch die vorlage vorlage & variable ändern`%topbar-bg%`(annabel)

### Grube 3: die harte kodierung der qss-datei für dunkle farben
```css
QTextEdit, QPlainTextEdit { background: #1a1a1a; }   // 日志区永远黑
```
**Heilung.**Quantitative lockerung `%terminal-bg%` / `%terminal-fg%`Und die furchterregende schrift,
Die dunkle uhr hält die schwarze box am ende.

## 3. Zweitthemen-design (hellere/dunkle schlangenstets stets)

- Zwei weitere funktionseinheiten innerhalb einer skala:`lightVars()` / `darkVars()`Nur wenn die verbindungen vollständig übereinstimmten,
Sonst wird ein thema wie %var% verlegt, und die QSS wird versagen.
- **So entwirft man die tafel.**Die polizist einer einheitlichen annäherung: border/border/sekunders/polpols /pill- polze
- Zudem wurde der mechanismus für den wechsel der themenfarbigen mit lighter /darken/ halpha ausgebaut
Aurora. hover/ deep/ exgeist. hover/ geist d
- **Die formel muss irgendwann me me me me verwendet werden**Eine verhärtete verschlüsselung in der vergangenheit
Die version der parameter wird komplett ignoriert (den & größten & unscheinbaren schurk).

## 4. Die QSS auswertung des ersten fehlergebnisses

- Qt news `Could not parse application stylesheet` zeitpunkt
1. Prüft, ob die grafikergebnisse vorliegen**Werden nun die var% %, nicht ersetzt**Heimat der `%[a-z-]+%` Scan ausführen
2. Prüfung des farbformats (funken) und #RRGGBB prüfung
3. Überprüfe auswahlgrammatik (ausgewählte grammatik`QLabel#detailPanel QLabel` Und zwar über das gesamte paket.
- Die anweisung: die anweisung der haltanhaltschleife: %var (wie filleby settme) sollte irgendwann funktionieren; eine warnung:

## 5. Themenknöpfe interaktive design

- Thema, knopf symbole werden sollen "auf alles begriffen." : dunkle modell zeigt ☀ (! Er zurück helle),
Helle modell zeigt ☾ (. Ist dunkle).
- Der benutzer weiß sonst nicht, wie es läuft

## 6. Ermittlung der methoden

- **Die sonde verwirren**: die uhr uhr mit renderfortsausgabe + belässt die ausgabe mit/ohne wichtige farbwerte:
`LIGHT has #f3f3f3: true`,`LIGHT terminal dark: false`.
- **Logbuch führen**:`[SCK] theme=fluent-light qss len=11085`Der stderr ist völlig verrückt.
- Bewachen sie die atmosphäre bei befruchteten befruchtungen und befruchteten befruchtungen.
bg: bei der nachsuche nach dem wort "übrig" lässt sich das gleich auf see Oder bg bestätigen.

