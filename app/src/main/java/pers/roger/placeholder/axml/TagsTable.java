package pers.roger.placeholder.axml;

public class TagsTable {
    int INF = 0xffffffff;
    Tag manifest;
    Tag uses;
    Tag application;
    Tag action;
    Tag activity;
    Tag inter_filter;
    Tag category;

    TagsTable() {
        manifest = new Tag(2, 17, 0xe, new Attribute[]{
            new Attribute(9,0, INF, Util.TYPE_INT,999999999),
            new Attribute(9,1,0x10,Util.TYPE_STRING,0x10),
            new Attribute(9,2, INF, Util.TYPE_INT,0x1e),
            new Attribute(9,3,0x11,Util.TYPE_STRING,0x11),
            new Attribute(INF,11,0xf,Util.TYPE_STRING,0xf),
            new Attribute(INF,12, 0x12,Util.TYPE_INT,0x1e),
            new Attribute(INF,13,0x11,Util.TYPE_INT,0xb),
        });
        uses = new Tag(6,8,0x13, new Attribute[]{
                new Attribute(9, 4, INF, Util.TYPE_INT, 0xc),
                new Attribute(9, 5, INF, Util.TYPE_INT, 0x1e)
        });

        application = new Tag(9,14,0x14, new Attribute[]{
                new Attribute(9,6,0x15,Util.TYPE_STRING,0x15)
        });

        activity = new Tag(10,15,0x16, new Attribute[]{
                new Attribute(9,7,0x17,Util.TYPE_STRING,0x17)
        });

        inter_filter = new Tag(11,14,0x18, new Attribute[]{});

        action = new Tag(12,12,0x19, new Attribute[]{
                new Attribute(9, 7,0x1a,Util.TYPE_STRING,0x1a)
        });


        category = new Tag(13,13,0x1b, new Attribute[]{
                new Attribute(9,7,0x1c,Util.TYPE_STRING,0x1c)
        });
    }
}

class Tag {
    int start, end, name_id;
    Attribute[] attrs;

    int xml_comment = -1;
    int uri_id = -1;
    int unknow = 0x140014;
    int attribute_id = 0;

    int size;

    public Tag(int start, int end, int name_id, Attribute[] attrs) {
        this.start = start;
        this.end = end;
        this.name_id = name_id;
        this.attrs = attrs;
        size = attrs.length * 20 + 36;
    }
}
