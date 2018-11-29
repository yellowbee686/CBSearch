package app.cbreader.demo;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

/**
 * @author huangjunyi
 * Created on 2018-11-26
 * 解析一个目录文件，并按照该文件生成层级文件夹，并导出经文内容到对应的文本文件中
 */
public class CatalogGenerator {
    EmendationParser parser;

    public CatalogGenerator(String dirPath, EmendationParser parser) {
        initFiles(dirPath);
        this.parser = parser;
    }

    private final String baseOutputPath = Utils.getBaseDir() + "/catalog/";
    // 不构造成map是因为目录中可能不会补全前面的那么多0，因此改成按数字来获取
    private HashMap<String, ArrayList<File>> dataFileMap = new HashMap<>();

    // 将数据目录的先构造成 List<List<File>> 存起来
    private void initFiles(String dirPath) {
        File rootDir = new File(dirPath);
        LinkedList<File> dirList = new LinkedList<>();
        if (rootDir.exists()) {
            if(rootDir.isDirectory()) {
                dirList.add(rootDir);
            } else {
                recordOneFile(rootDir);
            }
        }

        while (!dirList.isEmpty()) {
            File dir = dirList.pop();
            if (dir.exists()) {
                File dirs[] = dir.listFiles();
                for (File file : dirs) {
                    if (file.isDirectory()) {
                        dirList.add(file);
                    } else {
                        // T用来标记大正藏
                        if(file.getName().startsWith("T"))
                            recordOneFile(file);
                    }
                }
            }
        }
    }
    // 将一个文件记在dataFileMap中
    private void recordOneFile(File file) {
        //原始name是T10n0279_001.xml
        String[] nameTokens = file.getName().split("_");
        // 取name的第一段的数字作为index
        String key = nameTokens[0].substring(nameTokens[0].indexOf("n") + 1);
        key = makeupKey(key);
        ArrayList<File> fileList = dataFileMap.get(key);
        if (fileList == null) {
            fileList = new ArrayList<>();
            dataFileMap.put(key, fileList);
        }
        fileList.add(file);
    }
    // 在key前面补0凑足5位，这样存储和找的时候能够对上，因为总共4位数字+最后可能有的a或b
    // 同时兼容大小写的a b的区别
    private String makeupKey(String key) {
        key = key.toLowerCase(); //不管读写时都转小写保持一致
        while (key.length() < 5) {
            key = "0" + key;
        }
        return key;
    }

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
                        String[] items = line.split("\\.");
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
                    int lastIndex = line.indexOf("】");
                    title = line.substring(1, lastIndex).trim();
                    if (!lastAuthor.isEmpty()) {
                        paths.push(lastAuthor);
                    }
                    line = line.substring(lastIndex + 1);
                } else {
                    int partIndex = line.indexOf(authorMark);
                    title = line.substring(0, partIndex).trim();
                    line = line.substring(partIndex + 1);
                    lastAuthor = title;
                }
                paths.push(title);
                String[] papers = line.split("；");
                // Stack的foreach依然是顺序遍历
                String absolutePath = mkdir(makeRelativePath(paths));
                for (String paper : papers) {
                    String[] items = paper.trim().split(" ");
                    if (items.length <= 0 || items[0].length() <= 0) {
                        continue;
                    }
                    String key = makeupKey(items[0].substring(1));
                    //TODO 等待品级的区分，对需要修改的files范围进行过滤
                    if (key.contains("(")) {
                        continue;
                    }
                    ArrayList<File> files = dataFileMap.get(key);
                    if (files != null) {
                        for (File file : files) {
                            // 传入的outPath的文件名不完整，会在方法中补全title并填充内容
                            parser.parseOneDoc(file, ParseDocType.BODY, absolutePath + items[0]);
                        }
                    }
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

    //创建嵌套文件夹并返回absolutePath
    private String mkdir(String relativePath) {
        String path = baseOutputPath + relativePath;
        File dir = new File(path);
        if(!dir.exists()) {
            dir.mkdirs();
        }
        return path;
    }
}
