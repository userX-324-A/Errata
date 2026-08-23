# Upload keystore (local only)

`errata-upload.jks` in this folder is the Play **upload** key. It is gitignored.

- Passwords live in `local.properties` (`errata.release.*`), also gitignored.
- Back up the `.jks` **and** those four properties to somewhere that is not this repo (USB, password manager). Losing them means you cannot update the Play listing without a key-reset with Google.
- After Play App Signing is on, add **both** SHA-1s to the Android OAuth client: this upload key, and the App Signing key Play Console shows.
