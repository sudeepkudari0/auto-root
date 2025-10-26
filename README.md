# Root Voice Assistant (Java)

This project is a skeleton Android Studio app (Java) that implements a rooted voice assistant service.

Key features implemented in this scaffold:

- Foreground `VoiceService` that listens continuously for a keyword (`BOSS`) and then uses
  `SpeechRecognizer` to capture the user's command.
- `CommandExecutor` that demonstrates how to open apps, send WhatsApp messages via intent,
  toggle Wi-Fi using `svc wifi` via root, and run arbitrary root commands using `su -c`.
- `Utils` helper with `runRootCommand`, `isRooted`, timestamp helpers, and simple package checks.
- Minimal `MainActivity` UI showing logs and buttons to start/stop the service.

Files added
- `app/src/main/java/com/assistant/root/VoiceService.java` - foreground service, wake+command recognizers
- `app/src/main/java/com/assistant/root/CommandExecutor.java` - example command parsing + root execution
- `app/src/main/java/com/assistant/root/Utils.java` - helper utils for shell and formatting
- `app/src/main/res/layout/activity_main.xml` and `strings.xml` - basic UI resources

How to build
- Open this folder in Android Studio. The project uses Gradle; if the Gradle wrapper is missing,
  Android Studio will prompt to download Gradle or you can generate a wrapper.

Permissions
- The app requires runtime permission for `RECORD_AUDIO`. The manifest includes additional
  permissions like `FOREGROUND_SERVICE`, `POST_NOTIFICATIONS`, `SYSTEM_ALERT_WINDOW`, `WAKE_LOCK`,
  `CHANGE_WIFI_STATE`, etc. Grant them as needed.

Root requirements and safety
- Many commands use `su -c`. Ensure the device is rooted and that the app is granted root access.
- Root commands can be destructive. The sample code logs outputs and catches exceptions but do
  not run unknown commands.

Where to add new voice "skills"
- Edit `CommandExecutor.executeParsedCommand(String)` and add new branches. Example:

    // new skill
    if (cmd.contains("take screenshot")) {
        executeRoot("screencap -p /sdcard/screenshot.png");
        return;
    }

- For complex parsing, factor out a `Skill` interface and register implementations in a list.

Customization
- Change the keyword in `VoiceService.keyword` and timeout in `VoiceService.listenTimeoutMs`.

Limitations and notes
- Continuous speech recognition in the background is limited by Android OEMs and power
  management; the provided foreground service and persistent notification improve survivability.
- SpeechRecognizer behaves differently across devices; for robust always-on hotword detection
  consider integrating an offline hotword engine (Snowboy, Porcupine) or a native library.

Next steps you may want me to do
- Add a Gradle wrapper and run a CI build to ensure compile success.
- Add a simple `Skill` registration system and unit tests for the parser.
- Add more robust contact lookup and WhatsApp messaging flow.
