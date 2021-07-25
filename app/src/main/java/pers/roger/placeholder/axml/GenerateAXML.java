package pers.roger.placeholder.axml;

import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.nio.charset.StandardCharsets;

public class GenerateAXML {
    byte[] mData;
    int mOffset = 0;
    int WORD_SIZE = 4;
    StringData stringData;
    ResourceTable resourceTable;
    Namespace namespace;
    TagsTable tagsTable;


    void putLEWord(int data, int off) {
        int d1 = data & 0x000000ff;
        int d2 = (data & 0x0000ff00) >> 8;
        int d3 = (data & 0x00ff0000) >> 16;
        int d4 = (data & 0xff000000) >> 24;
        mData[off + 3] = (byte) d4;
        mData[off + 2] = (byte) d3;
        mData[off + 1] = (byte) d2;
        mData[off] = (byte) d1;
    }

    int off(int o) {
        return mOffset + o * WORD_SIZE;
    }

    public GenerateAXML(String name, String pak) {
        stringData = new StringData(name, pak);
        namespace = new Namespace();
        resourceTable = new ResourceTable();
        tagsTable = new TagsTable();

        mOffset = 8;
        stringData.update(mOffset);
        mOffset += stringData.size;
        mOffset += resourceTable.size;


        mOffset += 0x18;
        mOffset += tagsTable.manifest.size + 0x18;
        mOffset += tagsTable.uses.size + 0x18;
        mOffset += tagsTable.application.size + 0x18;
        mOffset += tagsTable.activity.size + 0x18;
        mOffset += tagsTable.inter_filter.size + 0x18;
        mOffset += tagsTable.action.size + 0x18;
        mOffset += tagsTable.category.size + 0x18;
        mOffset += 0x18;

        mData = new byte[mOffset];
    }



    void putTagStart(Tag tag) {
        putLEWord(Util.WORD_START_TAG, off(0));
        putLEWord(tag.size, off(1));
        putLEWord(tag.start, off(2));
        putLEWord(tag.xml_comment, off(3));
        putLEWord(tag.uri_id, off(4));
        putLEWord(tag.name_id, off(5));
        putLEWord(tag.unknow, off(6));
        putLEWord(tag.attrs.length, off(7));
        putLEWord(tag.attribute_id, off(8));
        mOffset = off(9);
        for(Attribute a : tag.attrs) {
            putLEWord(a.ns_id, off(0));
            putLEWord(a.name_id, off(1));
            putLEWord(a.value_id, off(2));
            putLEWord(a.type, off(3));
            putLEWord(a.data, off(4));
            mOffset = off(5);
        }
    }

    void putTagEnd(Tag tag) {
        putLEWord(Util.WORD_END_TAG, off(0));
        putLEWord(0x18, off(1));
        putLEWord(tag.end, off(2));
        putLEWord(tag.xml_comment, off(3));
        putLEWord(tag.uri_id, off(4));
        putLEWord(tag.name_id, off(5));
        mOffset = off(6);
    }

    void putStartDocument() {
        putLEWord(Util.WORD_START_DOCUMENT, off(0));
        putLEWord(mData.length, off(1));
        mOffset = off(2);
    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    void putStringTable() {
        putLEWord(Util.WORD_STRING_TABLE, off(0));
        putLEWord(stringData.size, off(1));
        putLEWord(stringData.stringNum, off(2));
        putLEWord(0, off(3));
        putLEWord(0, off(4));
        putLEWord(0x90, off(5));
        putLEWord(0, off(6));
        mOffset = off(7);
        for (int i : stringData.offsetTable) {
            putLEWord(i, off(0));
            mOffset = off(1);
        }

        for (String s : stringData.string) {
            byte[] bytes = s.getBytes(StandardCharsets.UTF_16LE);
            int len = bytes.length;
            mData[off(0)] = (byte) (len / 2);
            mOffset += 2;
            System.arraycopy(bytes, 0, mData, mOffset + 0, len);
            mOffset += len + 2;
        }
        if(mOffset % 4 != 0) {
            mOffset += 4 - (mOffset % 4);
        }
    }


    void putResourceTable() {
        putLEWord(Util.WORD_RES_TABLE, off(0));
        putLEWord(resourceTable.size, off(1));
        mOffset = off(2);
        for (int i : resourceTable.ids) {
            putLEWord(i, off(0));
            mOffset = off(1);
        }
    }


    void putNamespace() {
        putLEWord(Util.WORD_START_NS, off(0));
        putLEWord(0x18, off(1));
        putLEWord(namespace.start, off(2));
        putLEWord(namespace.xml_comment, off(3));
        putLEWord(namespace.ns_prefix_id, off(4));
        putLEWord(namespace.ns_uri_id, off(5));
        mOffset = off(6);

        putTagStart(tagsTable.manifest);
            putTagStart(tagsTable.uses);
            putTagEnd(tagsTable.uses);
            putTagStart(tagsTable.application);
                putTagStart(tagsTable.activity);
                    putTagStart(tagsTable.inter_filter);
                        putTagStart(tagsTable.action);
                        putTagEnd(tagsTable.action);
                        putTagStart(tagsTable.category);
                        putTagEnd(tagsTable.category);
                    putTagEnd(tagsTable.inter_filter);
                putTagEnd(tagsTable.activity);
            putTagEnd(tagsTable.application);
        putTagEnd(tagsTable.manifest);

        putLEWord(Util.WORD_END_NS, off(0));
        putLEWord(namespace.size, off(1));
        putLEWord(namespace.end, off(2));
        putLEWord(namespace.xml_comment, off(3));
        putLEWord(namespace.ns_prefix_id, off(4));
        putLEWord(namespace.ns_uri_id, off(5));
        mOffset = off(6);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public byte[] generate() {
        mOffset = 0;
        putStartDocument();
        putStringTable();
        putResourceTable();
        putNamespace();
        return mData;
    }
}

class ResourceTable {
    int[] ids = {
            0x101021b,
            0x101021c,
            0x1010572,
            0x1010573,
            0x101020c,
            0x1010270,
            0x1010001,
            0x1010003
    };
    int size = ids.length * 4 + 8;
}

class Namespace {
    int size = 0x18;
    int start = 2;
    int end = 15;
    int xml_comment = -1;
    int ns_prefix_id = 8;
    int ns_uri_id = 9;
}