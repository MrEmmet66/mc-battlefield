# FPSMatch (fork)

BlockOffensive depends on FPSMatch (`com.ptcrys:fpsmatch:1.3.0`), vendored from
our fork of [SSOrangeCATY/FPSMatch](https://github.com/SSOrangeCATY/FPSMatch)
kept as a git submodule (branch `fpsmatch`) in this repository.

## One-time setup

```sh
git submodule update --init
```

## Building the FPSMatch jar

The FPSMatch build is intentionally not part of the normal `build` task: it is
slow and needs network access. Build it explicitly into local Maven first:

```sh
./gradlew buildFpsmatch
```

(On Windows: `gradlew.bat buildFpsmatch`.)

If dependency download fails behind a proxy, unset the proxy variables and retry:

```sh
unset HTTP_PROXY HTTPS_PROXY
```

or in PowerShell:

```powershell
$env:HTTP_PROXY = $null
$env:HTTPS_PROXY = $null
```

Then run the main build:

```sh
./gradlew build
```

The reobfuscated jar is copied to
`local-maven/com/ptcrys/fpsmatch/1.3.0/fpsmatch-1.3.0.jar` (gitignored via `*.jar`).
