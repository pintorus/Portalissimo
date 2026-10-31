# Portalissimo

[English](README.md) | Français

Portalissimo est un fork expérimental de [Jami pour Android](https://github.com/savoirfairelinux/jami-client-android) pour **Meta Portal TV sous Android 9**. Il conserve le moteur d'appel et l'interface TV de Jami, avec le cadrage automatique du SDK Smart Camera de Meta lorsque l'appareil autorise son utilisation.

Il s'agit d'un projet indépendant, et non d'une distribution officielle de Meta ou de Jami.

## Fonctionnalités et état actuel

- Appels vidéo et visioconférences Jami, avec une interface TV.
- Suivi du sujet Smart Camera `BasicSpotlight` pendant la prévisualisation et la capture vidéo Camera2, avec repli sur `DefaultAuto` si le mode est refusé. Le centrage sur le visage dépend du firmware et reste à valider visuellement.
- Capture standard conservée si le service Meta est absent, refuse l'accès ou se déconnecte.
- Contrôle de la caméra libéré à l'arrêt ou à la reconfiguration de la capture.
- Variante `noPush` sans les services de notifications Google.

L'application de base a été installée et son assistant de compte TV ouvert sur un Portal TV sous Android 9 (API 28). L'intégration Smart Camera compile et dispose de tests unitaires, mais **le cadrage automatique réel et les appels de bout en bout restent à tester sur l'appareil**. Aucun sélecteur manuel de mode n’est exposé.

## Compilation

Prérequis :

- JDK 21 complet, avec `javac`.
- Android SDK 36, Build Tools 36.0.0, NDK 28.2.13676358 et CMake 4.1.2.
- SWIG 4.2 ou supérieur, Autoconf, Automake, Autopoint, Libtool, Gettext, Meson, Yasm, Ninja, M4, pkg-config et une chaîne de compilation native.
- Rust avec `rustup` ; la compilation native installe les cibles Android nécessaires.
- Node.js 24 et npm pour l'éditeur collaboratif intégré à Jami.

Configurer `JAVA_HOME` et `ANDROID_HOME` pour les installations locales et rendre les outils accessibles dans `PATH`. Puis :

```sh
git clone --recurse-submodules --branch portal-tv https://github.com/pintorus/Portalissimo.git
cd Portalissimo
cd daemon/bin/jni
PACKAGEDIR="$PWD/../../../jami-android/libjamiclient/src/main/java" bash ./make-swig.sh
cd ../../../jami-android
./gradlew :app:assembleNoPushDebug -Parchs=arm64-v8a
```

Pour un dépôt déjà cloné, exécuter `git submodule update --init --recursive` avant de générer les interfaces JNI. Il est également possible d'ouvrir `jami-android` dans Android Studio après avoir configuré les dépendances.

L'APK ARM64 est généré ici :

```text
jami-android/app/build/outputs/apk/noPush/debug/app-noPush-debug.apk
```

Le niveau d'API Android minimum est 26, ce qui inclut Android 9/API 28. Compiler avec le SDK 36 ne met pas à jour le système du Portal.

## Tests

Depuis `jami-android` :

```sh
./gradlew :app:testNoPushDebugUnitTest -Parchs=arm64-v8a
```

Validation effectuée : 15 tests JVM et 64 tests JavaScript de l'éditeur collaboratif réussis, et compilation de l'APK debug ARM64 réussie. Les tests Smart Camera couvrent la disponibilité, les refus de permission, les rappels tardifs, les déconnexions et la libération des ressources.

## Installation sur Portal TV

Activer le débogage USB et identifier le numéro de série du Portal TV avec `adb devices`. Depuis `jami-android`, remplacer `PORTAL_SERIAL` par ce numéro :

```sh
adb -s PORTAL_SERIAL install -r app/build/outputs/apk/noPush/debug/app-noPush-debug.apk
adb -s PORTAL_SERIAL shell pm grant cx.ring android.permission.CAMERA
adb -s PORTAL_SERIAL shell pm grant cx.ring android.permission.RECORD_AUDIO
adb -s PORTAL_SERIAL shell am start -W -n cx.ring/.tv.main.HomeActivity
```

L'identifiant du paquet reste `cx.ring`. Cette version debug utilise une clé de signature debug locale et ne peut pas remplacer une installation Jami signée avec une autre clé. Sauvegarder les comptes avant de supprimer une installation existante : la désinstallation efface ses comptes et données locales.

## Smart Camera

L'intégration utilise `com.facebook.portal:smartcamera:1.4.0` et demande `com.facebook.portal.permission.SMART_CAMERA_CONTROL`. Elle demande `BasicSpotlight`, avec repli sur `DefaultAuto`, uniquement si le service Meta `com.facebook.portal.aiservice` est présent et accorde le contrôle. Déclarer la permission ne garantit pas que le firmware du Portal l'accordera.

Pour consulter les messages d'activation et de retour à la capture standard :

```sh
adb -s PORTAL_SERIAL logcat -s PortalSmartCamera
```

Notes techniques complémentaires en français : [compilation et installation sur Portal TV](PORTAL-TV.md) et [recherche sur le SDK Smart Camera et ses limites](SMART-CAMERA.md). Les [exemples du SDK Meta](https://github.com/fbsamples/portal-sdk-samples) présentent son utilisation en amont.

## Projet d'origine et licence

Portalissimo est basé sur Jami, développé par Savoir-faire Linux et ses contributeurs. Le code Jami est distribué sous GPL-3.0-or-later ; voir [COPYING](COPYING). Les dépendances tierces conservent leurs licences respectives.

Signaler les problèmes propres au fork dans les [issues de Portalissimo](https://github.com/pintorus/Portalissimo/issues). Le [projet Jami d'origine](https://git.jami.net/savoirfairelinux/jami-client-android) reste la source du développement de Jami.
