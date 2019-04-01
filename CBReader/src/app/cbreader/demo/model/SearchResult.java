package app.cbreader.demo.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import app.cbreader.demo.Utils;

public class SearchResult {
    private Map<String, SearchResultModel> results = new HashMap<>(); //一次搜索结果会被拆成多个词
    private String searchKey; //搜索的原词，应该排在最前面

    public SearchResult(String searchKey) {
        this.searchKey = searchKey;
    }

    public void add(SearchResultModel resultModel) {
        String key = resultModel.getKey();
        if (results.containsKey(key)) {
            results.get(key).addAll(resultModel.getDocuments());
        } else {
            results.put(key, resultModel);
        }
    }

    public void add(String key, String doc, String content) {
        String summary = summaryContent(content, key);
        String fullContent = String.format("doc:%s  %s", doc, summary);
        if (results.containsKey(key)) {
            results.get(key).add(fullContent);
        } else {
            SearchResultModel model = new SearchResultModel(key);
            model.add(fullContent);
            results.put(key, model);
        }
    }

    public Map<String, SearchResultModel> getResults() {
        return results;
    }

    /**
     * 返回搜索结果
     * @return
     */
    public Vector<String> getListData() {
        Vector<String> ret = new Vector<>();
        SearchResultModel keyModel = results.get(searchKey);
        // 首先列出原始的搜索词
        if (keyModel != null) {
            ret.addAll(keyModel.getListData());
        }
        results.forEach((key, model) -> {
            if (!key.equals(searchKey)) {
                ret.addAll(model.getListData());
            }
        });
        // 将异文放在前面进行排序
        ret.sort((r1, r2) -> {
            int a1 = r1.contains(Utils.NOTE_PREFIX) ? 0 : 1;
            int a2 = r2.contains(Utils.NOTE_PREFIX) ? 0 : 1;
            return a1 - a2;
        });
        return ret;
    }

    private static final int SUMMARY_LIMIT = 10;
    private static final String SUMMARY_STR = "...";

    private String summaryContent(String content, String key) {
        String ret = "";
        // 异文则将异文标记添加在最前面
        if (content.startsWith(Utils.NOTE_PREFIX)) {
            ret = Utils.NOTE_PREFIX;
            content = content.substring(Utils.NOTE_PREFIX.length());
        }

        int idx = content.indexOf(key);
        int start = Math.max(0, idx - SUMMARY_LIMIT);
        int end = Math.min(content.length(), idx + SUMMARY_LIMIT);
        if (start > 0) {
            ret += SUMMARY_STR;
        }
        ret += content.substring(start, end);
        if (end < content.length()) {
            ret += SUMMARY_STR;
        }
        return ret;
    }
}
