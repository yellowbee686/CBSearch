package app.cbreader.demo.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class SearchResult {
    private Map<String, SearchResultModel> results = new HashMap<>(); //一次搜索结果会被拆成多个词

    public void add(SearchResultModel resultModel) {
        String key = resultModel.getKey();
        if (results.containsKey(key)) {
            results.get(key).addAll(resultModel.getDocuments());
        } else {
            results.put(key, resultModel);
        }
    }

    public void add(String key, String doc) {
        if (results.containsKey(key)) {
            results.get(key).add(doc);
        } else {
            SearchResultModel model = new SearchResultModel(key);
            model.add(doc);
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
        results.forEach((key, model) -> ret.addAll(model.getListData()));
        return ret;
    }
}
