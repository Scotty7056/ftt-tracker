# Datenschutzerklärung / Privacy Policy – FTT Tracker

*Zuletzt aktualisiert: Oktober 2026*

Der Schutz deiner persönlichen Daten ist uns ein wichtiges Anliegen. Die App **FTT Tracker** („wir“, „uns“ oder „unsere App“) wurde nach dem Prinzip der Datensparsamkeit („Privacy by Design“) entwickelt. **Alle erfassten Ausflugs- und Finanzdaten verbleiben ausschließlich lokal auf deinem Gerät.**

---

## 1. Verantwortlicher

Verantwortlich für die Datenverarbeitung im Sinne der Datenschutz-Grundverordnung (DSGVO):

**Thomas Schott**  
GitHub: [https://github.com/th-schott/freizeitticket-tracker](https://github.com/th-schott/freizeitticket-tracker)

---

## 2. Erhebung und Verarbeitung personenbezogener Daten

### a) Nutzungs- und Buchungsdaten (Lokal auf dem Gerät)
* **Erfasste Daten:** Datum deiner Ausflüge, ausgewählte Partnerbetriebe (Skigebiete, Bäder, Museen etc.), hinterlegte Ticketpreise/Ersparnisse und persönliche Notizen.
* **Zweck:** Berechnung deines Amortisationsstatus, Darstellung des Kalenders, der Statistiken und Bereitstellung des Homescreen-Widgets.
* **Speicherort:** Die Daten werden ausschließlich in einer lokalen SQLite/Room-Datenbank auf deinem Smartphone gespeichert. Es findet **keine Übertragung** an externe Server, Werbenetzwerke oder Tracking-Dienstleister statt.

### b) Standortdaten (ACCESS_FINE_LOCATION, ACCESS_BACKGROUND_LOCATION)
* **Erfasste Daten:** Deine aktuellen GPS-Koordinaten (Breiten- und Längengrad).
* **Zweck der Verarbeitung:**
  1. **Distanzanzeige & Kartenzentrierung:** Berechnung der Entfernung von deinem Standort zu den Partnerbetrieben sowie Anzeige deiner Position auf der Karte.
  2. **1-Tap Schnellbuchung im Widget:** Ermittlung des nächstgelegenen Partnerskigebiets bei Betätigung der Schnellbuchung auf dem Homescreen.
  3. **Hintergrund-Erkennung (Geofencing, optional):** Nach 15 Minuten Verweildauer an einer Talstation erinnert dich eine lokale Benachrichtigung an das Einbuchen.
* **Keine Speicherung oder Weitergabe:** Dein Standort wird in Echtzeit auf dem Gerät verarbeitet und zu keinem Zeitpunkt protokolliert oder an Dritte übermittelt.

---

## 3. Externe Dienste & Drittanbieter

### a) OpenTopoMap / OpenStreetMap
* **Zweck:** Abruf topografischer Kartenkacheln zur Darstellung der Alpenregion.
* **Hinweis:** Zur Übertragung der Bilddaten sendet dein Gerät die IP-Adresse an die Kachelserver von OpenTopoMap.

### b) Open-Meteo API
* **Zweck:** Abruf von Wetter- und Schneelagedaten für die Detailansicht eines Partners.
* **Hinweis:** Es werden nur die statischen Koordinaten des ausgewählten Ausflugsziels per HTTPS übermittelt (nicht dein Standort).

---

## 4. Berechtigungen der App

* `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`: Distanzberechnung und Kartenzentrierung.
* `ACCESS_BACKGROUND_LOCATION`: Optionale Talstation-Erkennung nach 15 Minuten Verweildauer.
* `POST_NOTIFICATIONS`: Anzeige von Buchungserinnerungen.
* `VIBRATE`: Haptisches Feedback beim Einbuchen per Longpress.
* `INTERNET`: Abruf von Kartenkacheln und Wetterdaten.

---

## 5. Rechte der Nutzer (DSGVO)

* **Export:** Alle Daten können jederzeit als CSV oder JSON exportiert werden.
* **Löschung:** Durch Deinstallation der App oder Löschen des App-Speichers werden alle Daten restlos entfernt.

---

## 6. Haftungsausschluss

**FTT Tracker** ist ein unabhängiges Open-Source-Projekt und steht in keiner geschäftlichen Verbindung, Partnerschaft oder Kooperation mit der Betreibergesellschaft des „Freizeitticket Tirol“ oder den beteiligten Partnerbetrieben.

---
---

# Privacy Policy – FTT Tracker (English Summary)

*Last updated: October 2026*

**FTT Tracker** is designed with privacy in mind. **All visit and financial data are stored strictly locally on your device.**

1. **Personal & Financial Data:** Tracked visits, ticket setups, and notes remain on-device in a local SQLite/Room database. No tracking servers are involved.
2. **Location Data:** GPS coordinates are used on-device for distance calculations, map positioning, 1-tap widget actions, and smart 15-minute dwell geofence alerts. Location data is never stored remotely or shared.
3. **Third-Party Services:** OpenTopoMap (map tiles) and Open-Meteo (weather forecasts for venues).
4. **Data Ownership:** Export data anytime via CSV/JSON or delete it by clearing app storage.
5. **Disclaimer:** FTT Tracker is an independent, unofficial project and is not affiliated with or endorsed by the Freizeitticket Tirol operators.