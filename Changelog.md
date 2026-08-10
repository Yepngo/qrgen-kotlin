# Changelog

All notable changes to this project will be documented in this file.

The format is based on
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project
adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

- ...

## [5.0.3] - 2026-08-10

### Fixed

- Preserved the QR code beneath transparent logo pixels instead of clearing
  them, preventing transparent logo backgrounds from rendering as black.

## [5.0.2] - 2026-08-10

### Fixed

- Prevented Detekt's successful process exit from terminating Maven before
  the `install` phase, allowing JitPack to collect the built artifacts.

## [5.0.1] - 2026-08-10

### Fixed

- Configured JitPack to build with Java 17 and documented the canonical
  case-sensitive `com.github.Yepngo` dependency coordinate.

## [5.0.0] - 2026-08-09

### Added

- Added the Kotlin `qrGenerator {}` DSL and immutable, reusable configured
  generators.
- Added `Result`-based `render`, `write`, and `writeTemp` output operations.
- Added the immutable `QrColor` type with RGB, RGBA, ARGB, HSL, and HSLA
  factories.
- Added payload capacity metadata to `ErrorCorrectionLevel`.

### Changed

- Rewrote all production and test sources in Kotlin and moved them to the
  conventional Kotlin source directories.
- Made rendering invocation-local and defensively copied configured logos for
  safe concurrent reuse.
- Raised the minimum runtime to Java 17 and added `kotlin-stdlib` as a runtime
  dependency.
- Replaced Javadoc generation with Dokka and bound ktlint and Detekt to the
  Maven `verify` lifecycle.

### Removed

- Removed the mutable Java fluent API, cloning, legacy color hierarchy, and
  standalone generator payload constants.
- Removed `module-info.java` and JPMS support.

## [4.0.0] - 2026-08-09

### Changed

- Embedded and relocated the QR-generation subset derived from ZXing 3.5.3,
  so the library no longer has runtime dependencies.
- Moved the library's Java packages from `com.github.aytchell.qrgen` to
  `com.yepngo.qrgen` for the Yepngo fork.

### Removed

- Removed `ErrorCorrectionLevel.getZxingLevel()`, which exposed an internal
  implementation type.

## [3.0.1] - 2025-10-30

### Added

- New method `QrGenerator.writeToImage(String)` that returns a `BufferedImage` for a given payload without touching the file system.

## [3.0.0] - 2025-04-04

### Added

- Added a `module-info.java` so that the lib can be used as a Java 9 module

### Changed

- Due to the new `module-info.java` the lib is now compiled with Java 9 and
  this version can no longer be used with Java 8. Imho this shouldn't be a
  problem in 2025.
- Because of the shift towards modules I reordered the packages of the
  library (firstly for having a clear API which can be exported from the
  module and secondly I untangled some dependency cycles between packages).
- This change actually causes a breaking API change (since the package of the
  thrown exceptions changed) and alas ... we have a new major release.

### Removed

- Internally the library no longer uses lombok. This has no effect on its
  clients

## [2.0.1] - 2023-07-31

### Added
- `QrGenerator` has a new method `noAlpha()` which strips off the alpha
  channel from all selected colors.

## [2.0.0] - 2023-06-11

### Added

- It is now possible to render the three markers of the generated QR codes
  in different styles. The markers of the QR code can now be drawn as
    - rectangles
    - rectangles with rounded corners
    - circles
    - 'raindrops' appearing to fall inward
    - 'raindrops' appearing to fall outward
    - rectangles with one rounded edge at the outer corner
    - rectangles with one rounded edge at the inner corner
    - rectangles with rounded edge except one sharp edge at the outer corner
    - rectangles with rounded edge except one sharp edge at the inner corner

### Changed

- When giving a color as parameter you can now choose between ARGB,
  RGBA, RGB, HSLA and HSL
- It is now possible to select extra colors for the three QR code's markers.
  The inner and the outer parts of the markers can be rendered with
  different colors (if requested).
- Added new style options for generated QR codes. The "pixels" of the QR code
  can now also be
    - rectangles with rounded corners or
    - they can be merged in a way
        - to form rows or columns
        - to form snake-like structures
        - to form structures like water (with adhesion)

### Removed

- the colors to be used can no longer be given as raw integer values.
  Instead you have to feed the `withColors(...)` methods whith instances
  of `ArgbValue`, `RgbaValue`, `RgbValue`, `HslaValue` or `HslValue`

## [1.1.1] - 2023-05-09

### Fixed

- Classes ArgbValue, RgbValue and ImgParameter had no real 'equals()'
  method. Fixed this by introducing lombok

## [1.1.0] - 2022-06-07

### Added

- Added new configuration option for generated QR codes:
  the "pixels" of the QR code can now also be dots
- Added another option to draw "smaller (rectangular) pixels" so that a thin
  grid appears in between

### Changed

- Changed name of the enum to selsect generated file format from ImageType to
  ImageFileType

## [1.0.0] - 2022-05-17

### Added

- Started writing unit tests
- Added README.md
- Added javadoc

## [0.9.5] - 2022-05-10

### Added

- Implemented wrapper around [ZXing](https://github.com/zxing/zxing)
  for generating QR codes
- wrapper can be reused (for multiple payloads)
- generated QR code can be configured wth respect to
    - size of generate image
    - file type of produced image (BMP, PNG, JPG, GIF)
    - colors of "on" and "off" pixels
    - error correction level
    - character encoding of encoded payload
    - margin around the QR code (to help scanners)
- generator can place a logo centered over the QR code
