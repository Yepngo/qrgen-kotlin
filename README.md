# QR code generator

qrgen is a Kotlin-first QR image generator with configurable colors, module
styles, position-marker styles, and optional logo overlays. It embeds the
QR-generation subset derived from
[ZXing 3.5.3](https://github.com/zxing/zxing/tree/zxing-3.5.3); see
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) for attribution.

Version 5.0.0 is an intentionally breaking Kotlin rewrite. It requires Java
17, depends on `kotlin-stdlib`, and no longer provides a JPMS module descriptor.

## Dependency

The Yepngo fork is available through JitPack:

```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.Yepngo:qrgen:5.0.2")
}
```

Maven:

```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>
<dependency>
    <groupId>com.github.Yepngo</groupId>
    <artifactId>qrgen</artifactId>
    <version>5.0.2</version>
</dependency>
```

## Usage

Configuration is validated while the immutable generator is built. Rendering
and output errors are returned as Kotlin `Result` values.

```kotlin
import com.yepngo.qrgen.colors.QrColor
import com.yepngo.qrgen.config.ErrorCorrectionLevel
import com.yepngo.qrgen.config.ImageFileType
import com.yepngo.qrgen.config.MarkerStyle
import com.yepngo.qrgen.config.PixelStyle
import com.yepngo.qrgen.qrGenerator
import java.awt.image.BufferedImage
import java.nio.file.Path

val generator = qrGenerator {
    size(300, 300)
    margin(3)
    imageType = ImageFileType.PNG
    charset = Charsets.UTF_8
    errorCorrection = ErrorCorrectionLevel.Q
    pixelStyle = PixelStyle.DOTS
    markerStyle = MarkerStyle.ROUND_CORNERS

    colors {
        pixels = QrColor.rgb(0x00, 0x21, 0x47)
        background = QrColor.rgb(0xff, 0xff, 0xff)
        outerMarker = QrColor.hsl(28.0, 85.0, 42.0)
        innerMarker = QrColor.rgb(0xaf, 0x00, 0x2a)
    }

    logo(Path.of("logo.png"))
}

val image: BufferedImage = generator.render("Hello, World!").getOrThrow()
generator.write("Hello, World!", outputStream).getOrThrow()
val temporaryFile: Path = generator.writeTemp("Hello, World!").getOrThrow()
```

The caller retains ownership of streams passed to `logo(InputStream)` and
`write(payload, OutputStream)`. Logos supplied as a `Path`, `InputStream`, or
`BufferedImage` are decoded or copied during construction. A configured
generator is therefore reusable and safe for concurrent rendering.

The defaults are PNG, 200 by 200 pixels, margin 4, UTF-8, error correction L,
rectangular pixels and markers, black foreground, white background, and no
logo.

## Colors and formats

`QrColor` provides validated `rgb`, `rgba`, `argb`, `hsl`, and `hsla`
factories. Colors can be scaled, inspected for alpha, converted to an opaque
color, or converted to `java.awt.Color`. PNG and GIF retain alpha; JPG and BMP
do not.

Output formats are `PNG`, `GIF`, `BMP`, and `JPG`. Pixel styles are
`RECTANGLES`, `SMALL_RECTANGLES`, `DOTS`, `ROUND_CORNERS`, `ROWS`, `COLUMNS`,
`SNAKES`, and `WATER`. Marker styles are `RECTANGLES`, `ROUND_CORNERS`,
`CIRCLES`, `DROP_IN`, `DROP_OUT`, `ROUND_IN`, `ROUND_OUT`, `EDGE_IN`, and
`EDGE_OUT`.

Each `ErrorCorrectionLevel` exposes its conservative `maximumPayloadBytes`.

## Migrating from 4.x

- Replace `QrGenerator()` and mutable `with...` calls with `qrGenerator {}`.
- Replace the former color hierarchy with `QrColor` factories.
- Replace `writeToImage`, `writeTo`, and `writeToTmpFile` with `render`,
  `write`, and `writeTemp`, then explicitly handle or unwrap the returned
  `Result`.
- Remove uses of cloning and standalone generator capacity constants.
- Run on Java 17 or newer and include the Kotlin standard library at runtime.
- Do not require the removed `module-info.java` descriptor.

## License

The library is licensed under the Apache License 2.0.
