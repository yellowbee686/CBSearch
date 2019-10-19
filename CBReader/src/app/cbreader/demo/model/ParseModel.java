package app.cbreader.demo.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.dom4j.Element;

import app.cbreader.demo.ParseDocType;
import app.cbreader.demo.Utils;

/**
 * @author huangjunyi
 * Created on 2019-03-03
 * 存储一个文本解析后的正文或异文部分，包括所有的String和异文插入的key
 */
public class ParseModel {
    /**
     * 存储一行的信息，包括纯文本和对应的所有异文的key
     */
    class LineModel {
        private String text;
        private List<String> noteKeys;

        public String getText() {
            return text;
        }

        // 将第一个note key的最后四位即行号拼在text前面返回，用于生成notes，进一步完善体例
        public String getTextWithFirstNoteKey() {
            if (noteKeys.size() == 0) {
                return text;
            }
            String key = noteKeys.get(0);
            return "[" + key.substring(key.length() - 4) + "]" + text;
        }

        public List<String> getNoteKeys() {
            return noteKeys;
        }

        public LineModel(String text, List<String> noteKeys) {
            this.text = text;
            this.noteKeys = noteKeys;
        }
    }

    private String title = "";

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    private ParseDocType type;

    public ParseModel(ParseDocType type) {
        this.type = type;
    }

    private List<LineModel> lines = new ArrayList<>();

    // 返回自己包括的正文或异文部分
    public List<String> getTexts(boolean withLineNumber) {
        List<String> strs = new ArrayList<>();
        if (!title.isEmpty()) {
            strs.add(title);
        }
        strs.addAll(lines.stream().map(x -> {
            if(withLineNumber) {
                return x.getTextWithFirstNoteKey();
            } else {
                return x.getText();
            }
        }).collect(Collectors.toList()));
        return strs;
    }

    // 从一行element中解析所有的异文key
    private List<String> getNoteKeys(Element root) {
        List<String> keys = new ArrayList<>();
        if (type == ParseDocType.BODY) {
            for (Element item : (List<Element>) root.elements()) {
                String name = item.getName();
                if (name.contains("anchor")) {
                    String key = item.attributeValue("id");
                    if (key != null && (key.startsWith("nkr_note_orig") || key.startsWith("nkr_note_mod"))) {
                        keys.add(key);
                    }
                }
            }
        } else { //异文独立成行，只有一个key
            String key = root.attributeValue("target");
            keys.add(key.substring(1)); //删除首位的#
        }

        return keys;
    }

    public void addByElement(String text, Element element) {
        LineModel model = new LineModel(text, getNoteKeys(element));
        lines.add(model);
    }

    // {noteKey ->{ori, 异文}} 只针对Back
    private Map<String, List<NoteModel>> parseNotes() {
        Map<String, List<NoteModel>> pairs = new HashMap<>();
        for (LineModel line : lines) {
            String text = line.getText();
            // get(0)是因为异文一行就一个note，因此直接取第0个即可
            String noteKey = line.getNoteKeys().get(0);
            List<NoteModel> notes = Utils.getNotes(text, true);
            pairs.put(noteKey, notes);
        }
        return pairs;
    }


    // 将异文替换正文，都保存之后输出
    public List<String> getFullText(ParseModel noteModel) {
        List<String> fullTexts = new ArrayList<>();
        Map<String, List<NoteModel>> noteMap = noteModel.parseNotes();
        fullTexts.add(title); //标题暂时没有对应的line
        // 将异文对应的整句也添加到文件中，就接在原来的那句后面
        for (LineModel line : lines) {
            String text = line.getText();
            fullTexts.add(text); //先放正文
            for (String noteKey : line.getNoteKeys()) {
                List<NoteModel> notes = noteMap.get(noteKey);
                // 我们的原则是有cbeta校注就不加大正校注了，而原line中两者都有，因此均列出了
                if (notes != null) {
                    for (NoteModel note : notes) {
                        int idx = text.indexOf(note.getKey());
                        if (idx >= 0) {
                            String copied = text.replaceAll(note.getKey(), note.getNote());
                            fullTexts.add(Utils.NOTE_PREFIX + Utils.getNoteMark(note.getNote(), idx) + copied);
                        }
                    }
                }
//                } else {
//                    System.out.println(String.format("no note:%s", noteKey));
//                }
            }
        }
        return fullTexts;
    }
}

