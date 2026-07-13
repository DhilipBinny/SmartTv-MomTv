# OnePlus 6 - Smart TV Project Device Info

## Device Specs
- **Model**: ONEPLUS A6000 (OnePlus 6)
- **SoC**: Qualcomm Snapdragon 845 (SDM845)
- **Android**: 11 (API 30)
- **RAM**: ~5.5 GB (5,765,956 KB total)
- **Storage**: ~49 GB total, ~43 GB free on /data
- **Screen**: 1080x2280, 450 DPI, 60fps, AMOLED
- **HDR**: Supported (HDR10, HLG)
- **Battery**: 21% at time of check (charging)

## Connectivity (for Smart TV use)
- **USB-C**: Has USB host + accessory support (OTG works)
- **HDMI/DisplayPort Alt Mode**: NOT natively supported (OnePlus 6 does NOT support USB-C to HDMI wired output)
- **Miracast/WiFi Display**: Available but disabled by default
- **WiFi Direct**: Supported
- **Wireless ADB**: Connected at 192.168.1.13

## Display Output Options
Since OnePlus 6 lacks DisplayPort Alt Mode, the options are:
1. **Chromecast** — Cast screen to Chromecast dongle connected to monitor
2. **Miracast** — Wireless display to Miracast-compatible monitor/dongle
3. **scrcpy** — Mirror phone screen to a connected PC/Raspberry Pi via USB/WiFi
4. **USB-C Display Link adapter** — Third-party adapters (DisplayLink based) may work but need app support

## Installed Media Apps
- YouTube
- Netflix
- Wynk (Airtel)
- YouTube Music

## Project Goal
Repurpose this OnePlus 6 as a **smart TV for mom**, connected to:
- External monitor (via wireless casting or adapter)
- External speaker
- Air remote / Bluetooth remote for navigation

## Key Constraints
- No wired HDMI output — must use wireless casting or adapter
- Phone needs to stay plugged in for power (always-on use)
- UI must be simple enough for mom to use with a remote
- Auto-launch TV interface on boot
