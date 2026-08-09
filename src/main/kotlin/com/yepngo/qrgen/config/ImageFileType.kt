package com.yepngo.qrgen.config

/**
 * Image format to be generated
 *
 *
 * Note that GIF and PNG support an alpha channel while for JPG and BMP it is ignored
 */
public enum class ImageFileType {
    JPG,
    GIF,
    PNG,
    BMP,
}
