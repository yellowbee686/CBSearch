package app.cbreader.demo.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author huangjunyi <huangjunyi@kuaishou.com>
 * Created on 2019-10-17
 */
public class ReferenceItem {
    private String fileName;
    private List<String> lineNums = new ArrayList<>();

    public ReferenceItem(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

    public void addLineNum(String lineNumStr) {
        lineNums.add(lineNumStr);
    }

    public String getLineNums() {
        String ret = "(";
        for (int i = 0; i < lineNums.size(); i++) {
            if (i != 0) {
                ret += ";";
            }
            ret += lineNums.get(i);
        }
        ret += ")";
        return ret;
    }
}
