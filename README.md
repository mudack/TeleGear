
# TeleGear Readme  
## What is TeleGear?
TeleGear is a simple fork what change almost nothing in original code of Telegram but extend it functionality!

Currently, my **primary objective**:
- [ ] to extend current telegram player to make it similar to VLC audio player
- [ ] to create double bottom like https://postufgram.com/ because as far I know there is not any implementation for android

And I have aspiration to implement next features:
- [ ] [framework to create own encryption queue](#About_encryption_queue_framework)
...


## About_encryption_queue_framework
In university I was learnt that if I want to create my own algorithm of encryption it should be created with the expectation that everybody knows how it works, so I decide to break it conception a bit by providing for every telegram user a framework what will let them create their own encryption queue what will be based on exists one, and of course they will be warned that all on their responsibility!
## Native Code Build Optimization  
  
Compiling native C/C++ components (FFmpeg, BoringSSL, RLottie, etc.) is a resource-intensive process. On mid-range hardware or older laptops (like ThinkPads), a full clean build can take **over 10 minutes**, while a standard incremental build takes only **~6 seconds**.  
  
If you are not actively modifying C++ source files, it is highly recommended to use the **Precompiled Libraries** mode.  
  
### How it works  
We use a custom Gradle property `buildNative` to toggle the NDK build process:  
- `buildNative=true`: Triggers a full CMake build (slow).  
- `buildNative=false`: Skips CMake and packages pre-compiled `.so` binaries (fast). It require to have built binaries in separated dir!  
#### To generate/update these libraries:  
1. Run: `./gradlew :TMessagesProj:assembleDebug -PbuildNative=true`  
2. Copy files from: TMessagesProj/build/intermediates/stripped_native_libs/debug/stripDebugDebugSymbols/out/lib/*  
3. Paste to: TMessagesProj/src/main/compiledJniLibs/  
  
### Configuration  
In your root `gradle.properties` file, set:  
```properties  
buildNative=false  
```  


---

# Telegram official Readme   
## Telegram messenger for Android  
  
[Telegram](https://telegram.org) is a messaging app with a focus on speed and security. It’s superfast, simple and free.
This repo contains the official source code for [Telegram App for Android](https://play.google.com/store/apps/details?id=org.telegram.messenger).

## Creating your Telegram Application

We welcome all developers to use our API and source code to create applications on our platform.
There are several things we require from **all developers** for the moment.

1. [**Obtain your own api_id**](https://core.telegram.org/api/obtaining_api_id) for your application.
2. Please **do not** use the name Telegram for your app — or make sure your users understand that it is unofficial.
3. Kindly **do not** use our standard logo (white paper plane in a blue circle) as your app's logo.
4. Please study our [**security guidelines**](https://core.telegram.org/mtproto/security_guidelines) and take good care of your users' data and privacy.
5. Please remember to publish **your** code too in order to comply with the licences.

### API, Protocol documentation

Telegram API manuals: https://core.telegram.org/api

MTproto protocol manuals: https://core.telegram.org/mtproto

### Compilation Guide

**Note**: In order to support [reproducible builds](https://core.telegram.org/reproducible-builds), this repo contains dummy release.keystore,  google-services.json and filled variables inside BuildVars.java. Before publishing your own APKs please make sure to replace all these files with your own.

You will require Android Studio 3.4, Android NDK rev. 20 and Android SDK 8.1

1. Download the Telegram source code from https://github.com/DrKLO/Telegram ( git clone https://github.com/DrKLO/Telegram.git )
2. Copy your release.keystore into TMessagesProj/config
3. Fill out RELEASE_KEY_PASSWORD, RELEASE_KEY_ALIAS, RELEASE_STORE_PASSWORD in gradle.properties to access your  release.keystore
4.  Go to https://console.firebase.google.com/, create two android apps with application IDs org.telegram.messenger and org.telegram.messenger.beta, turn on firebase messaging and download google-services.json, which should be copied to the same folder as TMessagesProj.
5. Open the project in the Studio (note that it should be opened, NOT imported).
6. Fill out values in TMessagesProj/src/main/java/org/telegram/messenger/BuildVars.java – there’s a link for each of the variables showing where and which data to obtain.
7. You are ready to compile Telegram.

### Localization

We moved all translations to https://translations.telegram.org/en/android/. Please use it.
