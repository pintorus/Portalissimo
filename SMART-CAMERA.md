# SDK Meta Smart Camera : recherche du 30 octobre 2026

Un SDK **publié et téléchargeable** existe :
`com.facebook.portal:smartcamera:1.4.0`.

- [AAR sur Maven Central](https://repo.maven.apache.org/maven2/com/facebook/portal/smartcamera/1.4.0/smartcamera-1.4.0.aar)
- [Métadonnées Maven](https://repo.maven.apache.org/maven2/com/facebook/portal/smartcamera/maven-metadata.xml)
- [Exemple officiel Meta](https://github.com/fbsamples/portal-sdk-samples)
- [Dépendances de l'exemple officiel](https://github.com/fbsamples/portal-sdk-samples/blob/main/app/build.gradle)

Les versions 1.3.0 et 1.4.0 sont présentes dans Maven Central. Les AAR
`smartcamera`, `smartcamera-ipc`, `smartcamera-editor` et `base` 1.4.0 ont été
téléchargés dans `/tmp/portalissimo-smartcamera-research` et examinés sans les
exécuter. Leur manifeste déclare `minSdkVersion=26`, compatible avec Android 9
(API 28). Cette compatibilité de version ne prouve pas l'accès au service du
Portal TV.

## Fonctions confirmées dans le binaire

- `ModeSpec.DefaultAuto` : cadrage automatique.
- `ModeSpec.BasicSpotlight` : suivi d'un sujet.
- `ModeSpec.Desk` : cadrage de bureau configurable.
- `ModeSpec.Meeting` : cadrage de groupe.
- `ModeSpec.Fixed` : zone de cadrage manuelle.

L'API expose `SmartCameraControlConnectionFactory.connect()`,
`ControlConnection.requestControls()` et `ControlSession.setMode(ModeSpec)`.
L'[exemple de connexion officiel](https://github.com/fbsamples/portal-sdk-samples/blob/main/app/src/main/java/com/meta/portal/sdk/app/smartcamera/CameraServiceWrapper.java)
montre aussi la libération des sessions et le traitement des échecs d'accès.

Le binaire se connecte au paquet `com.facebook.portal.aiservice`, via l'action
`com.facebook.portal.SMART_CAMERA_EXTERNAL_CONTROL_SERVICE`.
Le [manifeste de l'exemple officiel](https://github.com/fbsamples/portal-sdk-samples/blob/main/app/src/main/AndroidManifest.xml)
déclare les permissions `com.facebook.portal.permission.SMART_CAMERA_CONTROL`
et `com.facebook.portal.permission.SMART_CAMERA_METADATA`.

## Conséquence pour Portalissimo

Le SDK commande le cadrage ; le flux vidéo reste fourni par Camera2. La piste
minimale consiste à garder la capture vidéo de Jami et ouvrir une session Smart
Camera pendant la capture. Un refus d'accès doit conserver la caméra standard.

La [note récente Meta](https://github.com/meta-quest/agentic-tools/blob/main/skills/portal/resources/smart-camera-sdk.md)
indique encore un binaire en attente pour `smartcamera:1.1.+`. Cela ne décrit pas
la disponibilité de l'ancien artefact 1.4.0, vérifiée directement dans Maven
Central et dans l'exemple officiel.

Le SDK n'est pas encore intégré au fork. Restent à vérifier sur le Portal TV :
présence et version du service, autorisation des permissions pour notre APK,
application effective des modes au flux envoyé par Jami et libération du contrôle
à la fin d'un appel. Aucun nouveau test matériel n'a été effectué pour cette
recherche.
