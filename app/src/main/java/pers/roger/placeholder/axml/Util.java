package pers.roger.placeholder.axml;

public class Util {
    public static final int WORD_START_DOCUMENT = 0x00080003;

    public static final int WORD_STRING_TABLE = 0x001C0001;
    public static final int WORD_RES_TABLE = 0x00080180;

    public static final int WORD_START_NS = 0x00100100;
    public static final int WORD_END_NS = 0x00100101;
    public static final int WORD_START_TAG = 0x00100102;
    public static final int WORD_END_TAG = 0x00100103;
    public static final int WORD_TEXT = 0x00100104;
    public static final int WORD_EOS = -0x1;

    public static final int TYPE_ID_REF = 0x01000008;
    public static final int TYPE_ATTR_REF = 0x02000008;
    public static final int TYPE_STRING = 0x03000008;
    public static final int TYPE_DIMEN = 0x05000008;
    public static final int TYPE_FRACTION = 0x06000008;
    public static final int TYPE_INT = 0x10000008;
    public static final int TYPE_FLOAT = 0x04000008;

    public static final int TYPE_FLAGS = 0x11000008;
    public static final int TYPE_BOOL = 0x12000008;
    public static final int TYPE_COLOR = 0x1C000008;
    public static final int TYPE_COLOR2 = 0x1D000008;
}
