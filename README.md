<p align="center">
  <img src="logo.png" width="128" alt="CustomTV logo">
</p>

<h1 align="center">CustomTV</h1>

<p align="center">A Twitch client for Android.</p>

<p align="center">
  <img src="screenshots/home.png" width="200" alt="Home with live followed channels">
  <img src="screenshots/stream.png" width="200" alt="Stream with chat and emotes">
  <img src="screenshots/channel.webp" width="200" alt="Channel profile">
  <img src="screenshots/chat-settings.png" width="200" alt="Chat settings with 7TV, FFZ and BTTV emotes">
</p>

This repository holds the source of the free edition and its releases. The Premium features are closed source.

## Download

Get the latest APK from [Releases](https://github.com/AlexWayfer/CustomTV/releases/latest).

News and updates: [Telegram](https://t.me/customtv_app)

## Features

### Watching

- Live streams in the Twitch player
- Mini player and picture-in-picture
- Full screen player with different chat modes
- Audio-only mode
- Background playback with media controls

### Chat

- 7TV, BTTV and FFZ emotes, updated live
- Emote effects, FFZ giant emotes, cheermotes and badges
- Autocompletion for emotes, nicks and commands
- Load recent chat messages when opening a channel
- Replies and threads
- Highlighting, sound and vibration on mentions
- Pinned messages
- Banners for hype trains, polls, predictions and raids
- Sub, gift and channel points reward messages
- Chat modes and chat rules
- Timeout and ban notices
- Chatter card
- Settings for text size, smooth scrolling and more

### Channels

- Followed channels with live previews
- Shared streams with collaborator avatars
- Channel search
- Channel profiles

## Premium

A small subscription unlocks past broadcasts, whispers, channel points, polls and predictions, moderation tools, customizable notifications, link previews in chat, and AI chatter portraits.

<p align="center">
  <img src="screenshots/premium-chat-replay.png" width="200" alt="Past broadcast with chat replay">
  <img src="screenshots/premium-channel-points.png" width="200" alt="Channel points rewards">
</p>

## Security

You never type your Twitch password into the app. It logs in with Twitch's device flow, the one TVs use: you approve it on twitch.tv/activate. The token is stored encrypted with a key from the Android Keystore and is excluded from backups.

## Building

1. Register an application in the [Twitch developer console](https://dev.twitch.tv/console) with the client type Public.
2. Put its client ID into `twitch.properties` at the root of the project:

   ```properties
   TWITCH_CLIENT_ID=your-client-id
   ```

3. Open the project in Android Studio and run the `freeDebug` variant, or build it with `./gradlew assembleFreeDebug`.

The update check reads the releases from a Telegram group and stays off without `telegram.properties`.

## Contributing

Issues with bugs and ideas are welcome. Pull requests are not accepted: the same code goes into the closed Premium build.

## License

[GPL-3.0](LICENSE)
