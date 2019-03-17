package app.cbreader.demo;

import java.io.File;
import java.util.List;

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
    protected void processOneFile(File file) {
        ParseModel textModel = parseOneDoc(file, ParseDocType.BODY, ""); //正文
        ParseModel noteModel = parseOneDoc(file, ParseDocType.BACK, ""); //异文
        List<String> fullTexts = textModel.getFullText(noteModel);
        write2File(file, fullTexts, getOutputPath(file), true);
    }
}
