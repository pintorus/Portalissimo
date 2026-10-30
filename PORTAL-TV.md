# Portalissimo sur Portal TV

Branche `portal-tv` du fork de Jami Android. Cette première version conserve le
moteur et l'interface TV de Jami et affiche le nom Portalissimo. Le suivi et le
cadrage Smart Camera de Meta ne sont pas encore intégrés.

Le Portal TV testé utilise Android 9 (API 28). Le `minSdk` du client est 26 ;
le SDK 36 sert à compiler et ne met pas à jour Android sur le Portal.

## Compilation

- JDK complet 21 (avec `javac`).
- Android SDK 36, Build Tools 36.0.0, NDK 28.2.13676358, CMake 4.1.2.
- Outils natifs indiqués dans le README Jami, dont SWIG 4.2 minimum,
  autoconf, automake, autopoint, libtool et Meson.
- Node.js et npm pour les ressources de l'éditeur livré avec Jami.

Configurer `JAVA_HOME` et `ANDROID_HOME` vers les installations locales, puis :

```sh
git submodule update --init --recursive
cd daemon/bin/jni
PACKAGEDIR="$PWD/../../../jami-android/libjamiclient/src/main/java" bash ./make-swig.sh
cd ../../../jami-android
./gradlew :app:assembleNoPushDebug -Parchs=arm64-v8a
```

La variante `noPush` évite les services Google. Le paquet Android conserve
l'identifiant `cx.ring` ; la version de développement est signée avec la clé
debug locale et ne peut pas remplacer un Jami signé avec une autre clé.
Désinstaller Jami supprime ses comptes et données locales.

Les constantes de permission Android 37 sont écrites sous forme de valeurs
littérales pour compiler avec le SDK 36, en conservant la vérification de version
Android avant toute demande de permission.

## Installation et vérification

Depuis `jami-android`, avec le Portal TV connecté en USB :

```sh
adb install app/build/outputs/apk/noPush/debug/app-noPush-debug.apk
adb shell pm grant cx.ring android.permission.CAMERA
adb shell pm grant cx.ring android.permission.RECORD_AUDIO
adb shell am start -W -n cx.ring/.tv.main.HomeActivity
```

Vérifié sur Portal TV Android 9 : compilation Debug ARM64, graphes de tâches
Debug/Release séparés, installation et ouverture de l'assistant de compte TV.
Les appels audio/vidéo et le cadrage intelligent ne sont pas encore validés.
