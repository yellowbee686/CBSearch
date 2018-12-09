package app.cbreader.demo;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author huangjunyi
 * Created on 2018-12-09
 */

public class FullTextGenerator extends EmendationParser {
    public FullTextGenerator(String dirPath) {
        super(dirPath);
        outDirPath = Utils.getBaseDir() + Utils.FULLTEXT_PATH;
    }

    // 将所有经文写入文件中，同时将异文混合也写入文件
    @Override
    protected void processOneFile(File file) {
        List<String> texts = parseOneDoc(file, ParseDocType.BODY, ""); //正文
        List<String> emendations = parseOneDoc(file, ParseDocType.BACK, ""); //异文
        Map<String, String> pairs = parseEmendations(emendations);
        List<String> fullTexts = new ArrayList<>(texts); //先放正文
        for (String text : texts) {
            String copied = text;
            List<String> used = new ArrayList<>();
            for (Map.Entry<String, String> entry : pairs.entrySet()) {
                String ori = entry.getKey();
                String emendation = entry.getValue();
                if (copied.contains(ori)) {
                    copied = copied.replaceAll(ori, emendation);
                    used.add(ori);
                }
            }
            fullTexts.add(copied);
            for (String key : used) {
                pairs.remove(key);
            }
        }
        write2File(file, fullTexts, getOutputPath(file), true);
    }

    private Map<String, String> parseEmendations(List<String> emendations) {
        Map<String, String> pairs = new HashMap<>();
        for (String item : emendations) {
            int index = item.indexOf("=");
            if (index >= 0) {
                String key = item.substring(0, index).trim();
                if (!key.isEmpty()) {
                    int tailIndex = item.indexOf("【");
                    String value = item.substring(index + 1, (tailIndex == -1 ? item.length() : tailIndex)).trim();
                    pairs.put(key, value);
                }
            }
            int addIndex = item.indexOf("+");
            if (addIndex >= 0) {
                String key = item.substring(0, addIndex).trim();
                int bonusFront = item.indexOf("（");
                int bonusEnd = item.indexOf("）");
                if (bonusFront != -1 && bonusEnd != -1) {
                    String bonus = item.substring(bonusFront+1, bonusEnd).trim();
                    if (bonusFront > addIndex) {
                        pairs.put(key, key + bonus);
                    } else {
                        pairs.put(key, bonus + key);
                    }
                }
            }
            if (item.contains("-")) {
                int front = item.indexOf("〔");
                int end = item.indexOf("〕");
                if (front != -1 && end != -1) {
                    String key = item.substring(front + 1, end);
                    pairs.put(key, "");
                }
            }
        }
        return pairs;
    }

}
