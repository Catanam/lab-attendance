# Sistem de prezență la laborator cu amprentă

Proiect de licență. Un dispozitiv ESP32 cu cititor de amprente marchează prezența
studenților la laborator, iar un server web ține evidența și le oferă profesorilor o
interfață de administrare pentru grupe, orar și rapoarte.

Proiectul are două părți:

- `firmware/` conține codul pentru ESP32 (PlatformIO). Citește amprenta de pe un
  senzor AS608, arată starea pe un ecran OLED și trimite imaginea către server,
  semnată HMAC.
- `backend/` conține aplicația Spring Boot. Primește scanările, le potrivește cu
  studenții înrolați și salvează prezența în Postgres.

## De ce ai nevoie

- Java 21 și Maven pentru backend. Maven vine cu wrapper-ul `mvnw`, deci nu trebuie
  instalat separat.
- Docker pentru baza de date Postgres locală.
- PlatformIO pentru compilarea și încărcarea firmware-ului. Cel mai simplu prin
  extensia de VS Code.

## Backend

Pornește baza de date:

```bash
docker compose up -d
```

Apoi aplicația:

```bash
cd backend/attendance
./mvnw spring-boot:run
```

Pe Windows folosește `mvnw.cmd` și verifică să ai `JAVA_HOME` setat spre un JDK 21.

La prima pornire, Flyway creează schema și rulează migrările din
`src/main/resources/db/migration`. Serverul ascultă pe `http://localhost:8080`.
Datele de conectare la Postgres sunt în `application.yml` și se potrivesc cu cele din
`docker-compose.yml`.

Pentru un jar de producție:

```bash
./mvnw clean package
java -jar target/attendance-0.0.1-SNAPSHOT.jar
```

Testele sunt în `src/test` și rulează cu `./mvnw test`.

## Firmware

Deschide folderul `firmware/` în VS Code cu PlatformIO instalat. Înainte de încărcare,
pune-ți propriile valori în `src/main.cpp`, în funcția `load_hardcoded_config`: SSID-ul
și parola WiFi, adresa serverului, ID-ul dispozitivului, secretul HMAC și ID-ul sălii.
Secretul HMAC și ID-ul dispozitivului trebuie să fie aceleași și în backend, altfel
serverul respinge scanările cu 401.

Placa este `esp32dev`, iar portul implicit `COM3`. Dacă al tău e altul, schimbă-l în
`platformio.ini`. Compilare și încărcare:

```bash
pio run --target upload
```

Bibliotecile (senzorul Adafruit, ecranul SSD1306, ArduinoJson) se descarcă singure la
primul build.

## Cum comunică cele două

ESP32 trimite un POST la `/api/scan` cu imaginea brută a amprentei. Cererea e semnată
HMAC-SHA256 peste timestamp, ID-ul dispozitivului, un nonce și hash-ul corpului, ca
serverul să știe că vine de la un dispozitiv cunoscut și că imaginea n-a fost
modificată pe drum. Serverul caută amprenta printre cele înrolate, marchează prezența
și răspunde cu rezultatul, pe care ESP32 îl afișează pe ecran.

## Note

Valorile din `application.yml` și din `main.cpp` sunt pentru dezvoltare locală. Pentru
o instalare reală, schimbă-le.
