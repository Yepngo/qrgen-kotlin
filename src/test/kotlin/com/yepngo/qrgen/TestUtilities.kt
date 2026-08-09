package com.yepngo.qrgen

import com.yepngo.qrgen.config.ImageFileType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import javax.imageio.ImageIO

class TestUtilities {
    @Test
    fun byteConversionsForMagicNumbersAreCorrect() {
        Assertions.assertEquals(0x89, (toByte(0x89).toInt() and 0xff))
        Assertions.assertEquals(0xFF, (toByte(0xFF).toInt() and 0xff))
        Assertions.assertEquals(0xd8, (toByte(0xd8).toInt() and 0xff))
        Assertions.assertEquals(0xe0, (toByte(0xe0).toInt() and 0xff))
    }

    companion object {
        private val bx89: Byte = toByte(0x89)
        private val bxff: Byte = toByte(0xff)
        private val bxd8: Byte = toByte(0xd8)
        private val bxe0: Byte = toByte(0xe0)

        private val MAGIC_BYTES_BMP = byteArrayOf(0x42, 0x4d) // ascii 'BM'
        private val MAGIC_BYTES_GIF = byteArrayOf(0x47, 0x49, 0x46, 0x38) // ascii 'GIF8'
        private val MAGIC_BYTES_PNG = byteArrayOf(bx89, 0x50, 0x4e, 0x47) // ascii '.PNG'
        private val MAGIC_BYTES_JPG = byteArrayOf(bxff, bxd8, bxff, bxe0) // ascii '....'

        private fun toByte(value: Int): Byte {
            if (value <= 127) return value.toByte()
            return (value - 256).toByte()
            // 128(int) = 0x80 = -128(byte) = 128 - 256
            // 129(int) = 0x81 = -127(byte) = 129 - 256
            // 130(int) = 0x82 = -126(byte) = 130 - 256
            // 131(int) = 0x83 = -125(byte) = 131 - 256
            // 132(int) = 0x84 = -124(byte) = 132 - 256
            // 133(int) = 0x85 = -123(byte) = 133 - 256
            // 134(int) = 0x86 = -122(byte) = 134 - 256
            // 135(int) = 0x87 = -121(byte) = 135 - 256
        }

        @kotlin.Throws(IOException::class)
        fun readProducedFile(path: Path): BufferedImage {
            val img = ImageIO.read(path.toFile())
            Assertions.assertNotNull(img)
            return img
        }

        @kotlin.Throws(IOException::class)
        fun findOutImageTypeOfFile(path: Path): ImageFileType? {
            Files.newInputStream(path).use { input ->
                val numberMagicBytes: Int = MAGIC_BYTES_GIF.size
                // read the first bytes of the file and compare with well-known magic byte sequences
                val magic = ByteArray(numberMagicBytes)
                val bytesRead: Int = input.read(magic)
                Assertions.assertEquals(numberMagicBytes, bytesRead)

                // the following checks only work properly if the arrays have the same size
                Assertions.assertEquals(numberMagicBytes, MAGIC_BYTES_GIF.size)
                Assertions.assertEquals(numberMagicBytes, MAGIC_BYTES_PNG.size)
                Assertions.assertEquals(numberMagicBytes, MAGIC_BYTES_JPG.size)

                if (magic.contentEquals(MAGIC_BYTES_PNG)) {
                    return ImageFileType.PNG
                }
                if (magic.contentEquals(MAGIC_BYTES_JPG)) {
                    return ImageFileType.JPG
                }
                if (magic.contentEquals(MAGIC_BYTES_GIF)) {
                    return ImageFileType.GIF
                }

                // the magic byte sequence of BMP is shorter
                Assertions.assertTrue(numberMagicBytes > MAGIC_BYTES_BMP.size)
                val bmpMagic = ByteArray(MAGIC_BYTES_BMP.size)
                System.arraycopy(magic, 0, bmpMagic, 0, MAGIC_BYTES_BMP.size)

                if (bmpMagic.contentEquals(MAGIC_BYTES_BMP)) {
                    return ImageFileType.BMP
                }
                return null
            }
        }

        @kotlin.Throws(IOException::class)
        fun assertFileExistsAndNotEmpty(file200: Path?) {
            Assertions.assertNotNull(file200)
            Assertions.assertTrue(file200!!.toFile().exists())
            val attrs = Files.readAttributes<BasicFileAttributes>(file200, BasicFileAttributes::class.java)
            Assertions.assertTrue(attrs.isRegularFile())
            Assertions.assertTrue(attrs.size() > 0)
        }
    }
}
