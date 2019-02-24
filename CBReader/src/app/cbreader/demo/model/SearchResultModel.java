package app.cbreader.demo.model;

import java.util.ArrayList;
import java.util.List;

public class SearchResultModel {
    private String key;
    private List<String> documents = new ArrayList<>(); //结果的来源

    public SearchResultModel(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public List<String> getDocuments() {
        return documents;
    }

    public void add(String doc) {
        documents.add(doc);
    }

    public void addAll(List<String> docs) {
        documents.addAll(docs);
    }

    public List<String> getListData() {
        List<String> docs = new ArrayList<>();
        for (String doc : documents) {
            docs.add(key + "   " + doc);
        }
        return docs;
    }
}
