package pers.roger.placeholder.axml;

public class StringData {
    String[] string;
    int stringNum;
    int size;
    int[] offsetTable;

    StringData(String app, String pak) {
        string = new String[] {
                "versionCode",
                "versionName",
                "compileSdkVersion",
                "compileSdkVersionCodename",
                "minSdkVersion",
                "targetSdkVersion",
                "label",
                "name",
                "android",
                "http://schemas.android.com/apk/res/android",
                "",
                "package",
                "platformBuildVersionCode",
                "platformBuildVersionName",
                "manifest",
                pak,
                "place-holder",
                "11",
                "30",
                "uses-sdk",
                "application",
                app,
                "activity",
                "com.example.MainActivity",
                "intent-filter",
                "action",
                "android.intent.action.MAIN",
                "category",
                "android.intent.category.LAUNCHER"
        };
        stringNum = string.length;
        offsetTable = new int[stringNum];
    }

    void update(int begin) {
        int offset = 0;
        for (int i = 0; i < stringNum; i++) {
            offsetTable[i] = offset;
            offset += string[i].length() * 2 + 4;
        }
        size = offset - begin + stringNum * 4 + 36;
        if(size % 4 != 0) {
            size += 4 - size % 4;
        }
    }
}
