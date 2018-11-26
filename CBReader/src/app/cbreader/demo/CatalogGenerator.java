package app.cbreader.demo;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Stack;

/**
 * @author huangjunyi
 * Created on 2018-11-26
 * 解析一个目录文件，并按照该文件生成层级文件夹，并导出经文内容到对应的文本文件中
 */
public class CatalogGenerator {
    EmendationParser parser;

    public CatalogGenerator(EmendationParser parser) {
        this.parser = parser;
    }

    private final String basePath = Utils.getBaseDir() + "/catalog";

    public void buildCatalog() {
        BufferedReader reader = Utils.openFile("/dictData/catalog.txt");
        if(reader == null) {
            return;
        }
        Stack<String> paths = new Stack<>(); //存储遍历的各个文件夹层级
        String paperMark = "T"; //经文的标记
        String authorMark = "："; //作者的标记
        String lastAuthor = ""; //用于记录上一个作者路径，碰到【】时需要同时将作者入栈
        try {
            while(true) {
                String line = reader.readLine();
                if(null == line) {
                    break;
                }
                if (line.trim().isEmpty()) {
                    continue;
                }
                // 这一行是一个纯目录，此时需要先跳出上一级目录，再新建一个
                if (!line.contains(authorMark) && !line.contains(paperMark)) {
                    if (!paths.isEmpty()) {
                        paths.pop();
                    }
                    // 如果有序号则过滤掉
                    if (line.contains(".")) {
                        String[] items = line.split(".");
                        if (items.length >= 2) {
                            line = items[1].trim();
                        }
                        paths.clear(); //序号永远是最初级path
                    }
                    paths.push(line);
                    continue;
                }
                // 解析有经文标记的行 每一行都先填充path，最后再抛出，
                // 如果是【开头，则需要把自己归属的作者也入栈
                String title;
                boolean isInner = line.startsWith("【");
                if (isInner) {
                    title = line.substring(1, line.indexOf("】")).trim();
                    if (!lastAuthor.isEmpty()) {
                        paths.push(lastAuthor);
                    }
                    line = line.substring(line.indexOf(authorMark) + 1);
                } else {
                    int partIndex = line.indexOf(authorMark);
                    title = line.substring(0, partIndex).trim();
                    line = line.substring(partIndex + 1);
                    lastAuthor = title;
                }
                paths.push(title);
                String[] papers = line.split("；");
                // Stack的foreach依然是顺序遍历
                mkdir(makeRelativePath(paths));
                for (String paper : papers) {
                    //TODO paper是 T0068 賴吒和羅經 这样的结构，
                    //TODO 需要重构 parseOneDoc 进行拆分 前面获取path和后面写入对应文件的path都需要重构
                }
                paths.pop();
                if (isInner) {
                    paths.pop();
                }
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String makeRelativePath(List<String> paths) {
        String ret = "";
        for (String path : paths) {
            ret += path + "/";
        }
        return ret;
    }

    private void mkdir(String relativePath) {
        String path = basePath + relativePath;
        File dir = new File(path);
        if(!dir.exists()) {
            dir.mkdirs();
        }
    }
}
