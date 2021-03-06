package app.cbreader.demo.model;

/**
 * @author huangjunyi
 * Created on 2019-03-03
 * 表示一个解析出的Note
 */
public class NoteModel {
    private boolean isEqual; //是否是等号关系
    private String key;
    private String note;
    private String lineNumStr;

    public NoteModel(boolean isEqual, String key, String note, String lineNumStr) {
        this.isEqual = isEqual;
        this.key = key;
        this.note = note;
        this.lineNumStr = lineNumStr;
    }

    public boolean isEqual() {
        return isEqual;
    }

    public String getKey() {
        return key;
    }

    public String getNote() {
        return note;
    }

    public String getLineNumStr() {
        return lineNumStr;
    }
}
