package pers.roger.placeholder.axml;

public class Attribute {
    int ns_id, name_id;
    int value_id, type, data;

    public Attribute(int ns_id, int name_id, int value_id, int type, int data) {
        this.ns_id = ns_id;
        this.name_id = name_id;
        this.value_id = value_id;
        this.type = type;
        this.data = data;
    }
}
