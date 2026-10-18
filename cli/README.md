# Example CLI implementation for linux x64

If you are not running on linux you can still try it out after build via:

```sh
docker run --rm --platform linux/amd64 \
  -v "$PWD:/work" -w /work \
  debian:bookworm-slim \
  bash -c 'apt-get update -qq >&2 && apt-get install -y -qq ca-certificates >&2 && \
  exec /work/cli/build/bin/linuxX64/releaseExecutable/cli.kexe "$1"' \
  _ "https://vm.tiktok.com/ZN8SyMfnP/" \
  > out.mp4
```

## Tests

Run test with `./gradlew :cli:linuxX64Test`

Or 
```
docker run --rm \
  --platform linux/amd64 \
  -v "$PWD:/work" -w /work \
  eclipse-temurin:21-jdk \
  bash -c 'apt-get update -qq >&2 && apt-get install -y -qq ca-certificates >&2 && \
    ./gradlew :cli:linuxX64Test --no-daemon --project-cache-dir /tmp/gradle-project-cache'
```