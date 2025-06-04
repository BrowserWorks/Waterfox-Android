# Certificates

The certificates for the official Waterfox builds (`production`)
can be accessed from the `certificates` folder in the root of this repository

# Fingerprints

Channel | Fingerprint type | Fingerprint
---|---|---
Production | SHA-1 | `A2:F6:D8:11:90:53:FC:3A:61:E0:8D:CF:3C:35:B4:D9:48:5B:C8:5F`
Production | SHA-256 | `29:39:99:7A:2D:8F:07:30:3C:EB:37:AD:68:10:AF:EF:0B:DA:71:0B:E2:11:64:76:E3:52:5A:73:79:EC:2E:1A`

# How to Verify Certificate Fingerprints

You can verify that your Waterfox installation is authentic by checking its certificate fingerprints against the ones listed above.

## Verify an Installed App (via ADB)

```bash
# Get the APK path for Waterfox
adb shell pm path net.waterfox.android.release

# Pull the APK from your device
adb pull /path/to/base.apk waterfox.apk

# Check the certificate fingerprints
keytool -printcert -jarfile waterfox.apk
```

## Verify a Downloaded APK

### Using keytool (comes with Java)
```bash
keytool -printcert -jarfile waterfox-android.apk
```

### Using apksigner (comes with Android SDK)
```bash
apksigner verify --print-certs waterfox-android.apk
```

## Verify the Certificate File Directly

```bash
# SHA-1 fingerprint
openssl x509 -in certificates/production.pem -noout -fingerprint -sha1

# SHA-256 fingerprint
openssl x509 -in certificates/production.pem -noout -fingerprint -sha256
```

## Why Verify?

- **Security**: Ensure the app hasn't been tampered with or modified
- **Authenticity**: Confirm you have the official Waterfox build
- **Trust**: Verify the app is signed by the legitimate developer

## Common Use Cases

1. **Before Installation**: Verify an APK downloaded from GitHub releases or other sources
2. **Security Audits**: Confirm the authenticity of installed apps

The fingerprints shown in the output should match exactly with the ones listed in the table above. If they don't match, the APK may have been modified or is not an official build.
