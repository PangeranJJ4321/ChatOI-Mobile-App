# ChatOI Mobile App (Android)

Ini adalah aplikasi klien Android untuk platform MyChat. Aplikasi ini memungkinkan pengguna untuk melakukan chatting secara real-time, mengelola profil, melihat detail percakapan, dan fitur-fitur lainnya.

### Link Repository API Backend ChatOI App Mobile (Python/FastAPI) : [**ChatOI**](https://github.com/PangeranJJ4321/Mobile-Chat-API)

## Teknologi

* **Android Development**: Dikembangkan menggunakan Kotlin/Java dengan Android Studio.
* **Minimum SDK**: API 21 (Android 5.0 Lollipop)
* **Target SDK**: API 34 (Android 14)
* **Gradle**: Sistem build otomatis.
* **Retrofit**: Klien HTTP untuk komunikasi dengan backend REST API.
* **Pusher Android Client**: Untuk fungsionalitas pesan real-time.
* **Glide**: Library untuk memuat dan menampilkan gambar (avatar, dll.).
* **Gson**: Library untuk serialisasi/deserialisasi objek Java ke/dari JSON.
* **Material Design Components**: Untuk komponen UI modern.
* **Lottie**: Untuk animasi.
* **SharedPreferences**: Untuk caching data lokal.

## Memulai

Untuk meng-setup dan menjalankan aplikasi ini di lingkungan pengembangan Anda:

### Prasyarat

* **Android Studio**: Versi terbaru disarankan. [Unduh di sini](https://developer.android.com/studio).
* **Java Development Kit (JDK)**: Versi yang kompatibel dengan Android Studio Anda.
* Akses ke **MyChat Backend** yang sedang berjalan (diperlukan untuk fungsionalitas penuh).

### Langkah-langkah Instalasi & Konfigurasi

1.  **Kloning Repositori:**
    ```bash
    git clone [https://github.com/PangeranJJ4321/ChatOI-Mobile-App](https://github.com/PangeranJJ4321/ChatOI-Mobile-App)
    cd ChatOI-Mobile-App
    cd MyChat
    ```

2.  **Buka Proyek di Android Studio:**
    * Buka Android Studio.
    * Pilih `File` > `Open` atau `Open an existing Android Studio project`.
    * Navigasikan ke direktori `mychat-mobile` yang baru saja Anda kloning dan klik `OK`.
    * Biarkan Gradle menyinkronkan proyek. Ini mungkin memerlukan waktu untuk mengunduh semua dependensi.

3.  **Konfigurasi Network Security (Penting untuk Backend Lokal):**
    Jika Anda menjalankan backend secara lokal (misalnya di Docker pada IP lokal Anda) dan tidak menggunakan HTTPS, Anda perlu mengizinkan *cleartext traffic* untuk domain/IP tersebut.

    * Buat file XML baru bernama `network_security_config.xml` di `app/src/main/res/xml/`.
        Jika direktori `xml` belum ada di `res/`, buatlah terlebih dahulu.
    * Tambahkan konten berikut ke dalam file `network_security_config.xml`:
        192.168.43.43 dari url Backend anda misal http://192.168.43.43:8000

        ```xml
        <?xml version="1.0" encoding="utf-8"?>
        <network-security-config>
            <domain-config cleartextTrafficPermitted="true">
                <domain includeSubdomains="true">192.168.43.43</domain>
                </domain-config>
        </network-security-config>
        ```

    * Tambahkan referensi ke file konfigurasi ini di `AndroidManifest.xml` Anda, di dalam tag `<application>`:

        ```xml
        <application
            android:networkSecurityConfig="@xml/network_security_config"
            android:allowBackup="true"
            android:icon="@mipmap/ic_launcher"
            android:label="@string/app_name"
            android:roundIcon="@mipmap/ic_launcher_round"
            android:supportsRtl="true"
            android:theme="@style/AppTheme">
            </application>
        ```

4.  **Konfigurasi API Base URL dan Pusher Keys:**
    Anda perlu mengonfigurasi aplikasi agar dapat berkomunikasi dengan *backend* Anda dan layanan Pusher. Ini biasanya dilakukan di file seperti `RetrofitClient.java` atau di file konfigurasi khusus.

    * **Retrofit Base URL**: Pastikan `RetrofitClient.BASE_URL` menunjuk ke URL backend FastAPI Anda yang sedang berjalan (misalnya, `http://192.168.43.43:8000/` atau URL *server* Anda), Anda bisa mendapatkan IP lokal host dengan perintah seperti `ipconfig` (Windows) atau `ifconfig`/`ip a` (Linux/macOS). 
    Jika Anda menjalankan backend dengan Docker di mesin lokal, gunakan alamat IP host Anda, bukan `localhost` atau `127.0.0.1`, karena kontainer emulator/perangkat Android memiliki lingkungan jaringan yang terpisah.
        ```java
        // app/src/main/java/com/example/mychat/retrofit_client/RetrofitClient.java
        public class RetrofitClient {
            // Ubah ini ke URL backend FastAPI Anda
            public static final String BASE_URL = "http://YOUR_BACKEND_IP_OR_DOMAIN:8000/";
            // Contoh: [http://192.168.43.43:8000/](http://192.168.43.43:8000/)
            // ...
        }
        ```
    * **Pusher Keys**: Pastikan `PUSHER_APP_KEY`, `PUSHER_APP_CLUSTER`, dan `PUSHER_AUTH_ENDPOINT` di `ChatActivity.java` (atau di tempat lain Anda mengelola konfigurasi Pusher) sesuai dengan kredensial aplikasi Pusher Anda. `PUSHER_AUTH_ENDPOINT` harus menunjuk ke endpoint otorisasi Pusher di backend FastAPI Anda (misalnya, `http://YOUR_BACKEND_IP_OR_DOMAIN:8000/messages/pusher/auth`).
        ```java
        // app/src/main/java/com/example/mychat/ChatActivity.java
        // ...
        String PUSHER_APP_KEY = "your_pusher_app_key"; // Ganti dengan kunci Pusher Anda
        String PUSHER_APP_CLUSTER = "ap1"; // Ganti dengan cluster Pusher Anda (e.g., us2, eu)
        String PUSHER_AUTH_ENDPOINT = RetrofitClient.BASE_URL + "messages/pusher/auth"; // Pastikan ini benar
        // ...
        ```

5.  **Siapkan Emulator atau Perangkat Fisik:**
    * Anda bisa menggunakan Android Emulator yang dikonfigurasi di Android Studio.
    * Atau sambungkan perangkat Android fisik Anda dan aktifkan `USB Debugging` di `Developer Options`.

6.  **Jalankan Aplikasi:**
    * Setelah sinkronisasi Gradle selesai dan Anda telah mengonfigurasi semua yang diperlukan, klik tombol `Run 'app'` (ikon panah hijau) di toolbar Android Studio.
    * Pilih emulator atau perangkat fisik target Anda.

## Fitur Caching

Aplikasi ini mengimplementasikan caching lokal menggunakan `SharedPreferences` dan `Gson` untuk meningkatkan pengalaman pengguna saat koneksi jaringan tidak stabil atau tidak ada. Data yang di-cache meliputi:

* **Profil Pengguna**: Informasi profil Anda sendiri di `ProfileFragment`.
* **Daftar Pengguna Diblokir**: Daftar kontak yang telah Anda blokir.
* **Riwayat Pesan**: Pesan-pesan dalam setiap percakapan di `ChatActivity`.
* **Detail Chat Pribadi**: Informasi kontak dan pengaturan notifikasi di `DetailChatPribadiActivity`.
* **Detail Grup**: Nama grup, deskripsi, avatar, dan daftar anggota di `GroupDetailsActivity`.
* **Daftar Anggota yang Tersedia**: Daftar pengguna yang bisa ditambahkan ke grup di `AddMembersDialogFragment`.

Data ini akan dimuat dari cache terlebih dahulu untuk tampilan yang cepat, lalu di-refresh dari API jika koneksi tersedia.

## Kontribusi

Ini adalah tugas mandiri pemrograman mobile android dengan Java

**Catatan Penting:**

* **Sesuaikan versi SDK** dengan yang ada di `build.gradle` aplikasi Anda.
* **Instruksi konfigurasi Pusher/Retrofit** harus akurat sesuai dengan bagaimana Anda mengelola *constant* ini di kode Anda.
* **Alamat IP Backend:** Ingatkan pengguna bahwa jika backend berjalan di Docker di mesin lokal, mereka mungkin perlu menggunakan IP lokal host mereka (misalnya, `192.168.1.xxx`) dan bukan `localhost` atau `127.0.0.1` dari emulator/perangkat. Anda bisa mendapatkan IP lokal host dengan perintah seperti `ipconfig` (Windows) atau `ifconfig`/`ip a` (Linux/macOS).

