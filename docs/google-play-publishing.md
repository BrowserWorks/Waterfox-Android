# Google Play Publishing Setup

This guide explains how to configure the Triple-T gradle-play-publisher plugin for automated Google Play Store deployments.

## Prerequisites

1. A Google Play Developer account
2. An app already created in the Google Play Console
3. A service account with appropriate permissions

## Creating a Service Account

1. Go to the [Google Play Console](https://play.google.com/console)
2. Navigate to **Setup** → **API access**
3. Click **Create new service account**
4. Follow the link to Google Cloud Console
5. Create a new service account with an appropriate name (e.g., `waterfox-android-publisher`)
6. Grant the service account the **Service Account User** role
7. Create and download a JSON key for the service account
8. Back in Play Console, grant the service account appropriate permissions:
   - **Release management** (to upload APKs/AABs)
   - **Production release** (to promote to production)
   - Any other tracks you want to deploy to

## Configuration Methods

### Method 1: Environment Variable (Recommended for CI/CD)

Set the `ANDROID_PUBLISHER_CREDENTIALS` environment variable with the entire JSON content:

```bash
export ANDROID_PUBLISHER_CREDENTIALS=$(cat /path/to/service-account.json)
```

For GitHub Actions:
```yaml
env:
  ANDROID_PUBLISHER_CREDENTIALS: ${{ secrets.PLAY_PUBLISHER_CREDENTIALS }}
```

### Method 2: File Path (Local Development)

You can also specify a file path in your `local.properties` or as a Gradle property:

```properties
playPublisher.serviceAccountJson=/path/to/service-account.json
```

Or via command line:
```bash
./gradlew publishReleaseBundle -PplayPublisher.serviceAccountJson=/path/to/service-account.json
```

### Method 3: Play Console API Key (Legacy)

If using an API key instead of service account:
```bash
export ANDROID_PUBLISHER_CREDENTIALS='{"type":"api_key","key":"YOUR_API_KEY"}'
```

## Usage

### Publishing to Production
```bash
./gradlew publishReleaseBundle
```

### Publishing to Beta Track
```bash
./gradlew publishReleaseBundle --track=beta
```

### Publishing to Internal Testing
```bash
./gradlew publishReleaseBundle --track=internal
```

### Uploading Without Publishing
```bash
./gradlew uploadReleaseBundle
```

## Advanced Configuration

Add to `app/build.gradle` for more control:

```gradle
play {
    serviceAccountCredentials.set(file("path/to/service-account.json"))
    defaultToAppBundles.set(true)
    track.set("internal") // Default track
    releaseStatus.set(ReleaseStatus.DRAFT)
    userFraction.set(0.1) // For staged rollouts
}
```

## Security Best Practices

1. **Never commit credentials to version control**
   - The `.gitignore` is already configured to exclude common credential files
   
2. **Use environment variables in CI/CD**
   - Store credentials as encrypted secrets in your CI/CD platform
   
3. **Rotate service account keys regularly**
   - Create new keys periodically and revoke old ones
   
4. **Use minimal permissions**
   - Only grant the service account the permissions it needs

## Troubleshooting

### "401 Unauthorized" Error
- Verify the service account has been granted permissions in Play Console
- Ensure the JSON credentials are valid and not expired

### "Package name not found" Error
- Confirm the `applicationId` in `build.gradle` matches your Play Store app
- Ensure the app has been created in Play Console first

### "Version code already exists" Error
- The version code must be higher than any previously uploaded version
- Check your version code generation logic

## Additional Resources

- [Triple-T Plugin Documentation](https://github.com/Triple-T/gradle-play-publisher)
- [Google Play Console Help](https://support.google.com/googleplay/android-developer/answer/7159011)
- [Service Account Setup Guide](https://developers.google.com/android-publisher/getting_started#service-account)