package app.cbreader.demo;

import java.io.File;
import java.util.List;
import java.util.Map;

import app.cbreader.demo.model.ParseModel;

/**
 * @author huangjunyi
 * Created on 2018-12-09
 * 负责生成全文和用异文替换后的文本，用来进行索引
 */

public class FullTextGenerator extends EmendationParser {
    public FullTextGenerator(String dirPath) {
        super(dirPath);
        outDirPath = Utils.getBaseDir() + Utils.FULLTEXT_PATH;
    }

    // 将所有经文写入文件中，同时将异文混合也写入文件
    @Override
    protected void processOneFile(File file, Map<String, String> pathMap) {
        ParseModel textModel = parseOneDoc(file, ParseDocType.BODY, ""); //正文
        ParseModel noteModel = parseOneDoc(file, ParseDocType.BACK, ""); //异文
        List<String> fullTexts = textModel.getFullText(noteModel);
        write2File(file, fullTexts, getOutPathInCatalog(file, pathMap), true);
    }

    private String getOutPathInCatalog(File file, Map<String, String> pathMap) {
        String[] nameArray = file.getName().split("\\.");
        String fileName = nameArray[0];
        if (pathMap.containsKey(fileName)) {
            String catalogPath = pathMap.get(fileName);
            String outPath = outDirPath + "/" + catalogPath + fileName;
            outPath = outPath.substring(0, outPath.lastIndexOf("_"));
            return outPath;
        } else {
            return getOutputPath(file);
        }
    }

    // 拼接要输出的目录path，输入是解析的源文件file
    @Override
    protected String getOutputPath(File doc) {
        String midPath = doc.getAbsolutePath().replace(dirPath, "");
        String outPath = outDirPath + "/other" + midPath;
        int idx = outPath.lastIndexOf("_");
        return outPath.substring(0, idx);
    }
}
