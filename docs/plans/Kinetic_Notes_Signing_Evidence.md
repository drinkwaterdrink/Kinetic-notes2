# EXP signing continuity evidence

## Artifact installed for the manual smoke test

The APK the user installed was the CI artifact from run `37239195565`:

- Application ID: `com.aistudio.kineticnotes.kxmpzq.exp`
- APK SHA-256: `e4754fa2a34c0b82043a68d31e2bc69a666cab85abba2b52e1b5677de842171b`
- Signing certificate SHA-256: `eac39d02b0474a94f4add256eb4ea3188dd521f0f9e9b774cb6c8b47ebd230bd`
- CI signing source: `ephemeral-validation-key`
- Manual result: the user reported that the APK launched and worked. This is a smoke test only.

## Recovery review

- No keystore file is present in the checkout or reachable Git history.
- The CI run generated `debug.keystore` on the hosted runner because `KX_EXP_KEYSTORE_B64` was empty for that run. The generated private key was not committed or uploaded.
- The available GitHub API context cannot read secret values. The latest artifact metadata is direct evidence that the stable secret was not supplied to that build; it is not evidence that a value can be recovered here.
- A certificate fingerprint is public evidence only. It cannot reconstruct the private signing key.

The exact private key for the installed ephemeral artifact is therefore **not recoverable from this workspace or the APK**. Do not print, paste, or commit any private key material.

## Compatibility target and options

The installed artifact's certificate fingerprint is the compatibility target for an update attempt, but the corresponding private key is not available. An APK signed by a different key must be reported as `INSTALL_FAILED_UPDATE_INCOMPATIBLE`; it must not be made installable by uninstalling, clearing data, or changing the application ID.

Data-preserving options are:

1. Keep the installed app and its private data untouched while the original key is sought in the owner's authorized secure keystore/secret manager.
2. If the original key is recovered, configure `KX_EXP_KEYSTORE_B64` through GitHub's secret manager, generate two consecutive EXP APKs, compare their certificate fingerprints to the installed target, and perform an in-place `adb install -r` test without clearing data. Migration tests must also be green.
3. If the original key cannot be recovered, use the existing app's own export/backup capability if one is available and verified, then restore only through a future compatible path. The current I-001 work does not claim a tested portable export/restore package.
4. Test an unrelated future build on a separate test device or an installation that does not contain the user's data. Do not replace the current installation as a shortcut.

Changing the application ID would avoid the signature check but would create a separate data store and is not an acceptable in-place compatibility solution.
