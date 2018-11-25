package app.cbreader.demo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

public class AppendixItem {
	private String key; //正确的字
	//纠正错别字的map 每个key都是一个错字
	private HashMap<String, ArrayList<String>> correctMap = new HashMap<String, ArrayList<String>>();
	
	public AppendixItem(String key) {
		this.key = key;
	}
	
	public void add(String wrongKey, String volumn) {
		if(!correctMap.containsKey(wrongKey)) {
			correctMap.put(wrongKey, new ArrayList<String>());
		}
		correctMap.get(wrongKey).add(volumn);
	}
	
	//TODO 实现自己的toString
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("【").append(key).append("】\r\n");
		ArrayList<SortHelper> helperList = new ArrayList<SortHelper>();
		for (Entry<String, ArrayList<String>> entry : correctMap.entrySet()) {
			String key = entry.getKey();
			ArrayList<String> arr = entry.getValue();
			StringBuilder localSb = new StringBuilder();
			localSb.append(key);
			localSb.append("（");
			Reference.appendArrayList(localSb, arr);
			int count = arr.size();
			localSb.append("）\r\n");
			helperList.add(new SortHelper(count, key, localSb.toString()));
		}
		helperList.sort((h1, h2)->{
			return h2.count - h1.count;
		});
		for (int i = 0; i < helperList.size(); i++) {
			SortHelper helper = helperList.get(i);
			//条目太少，先不加序号
			//sb.append(String.format("[%d]", i+1));
			sb.append(helper.str);
		}
		return sb.toString();
	}
}
