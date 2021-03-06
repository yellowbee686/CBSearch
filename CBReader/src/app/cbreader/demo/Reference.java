package app.cbreader.demo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import app.cbreader.demo.model.ReferenceItem;

public class Reference {
	//之所以要分开存储，是因为有一些只存在于backMap中的条目，也要显示在selfKey的体例中，用一个map(即frontMap)来存储，到其他Reference中找的话没办法找到
	//只存在于backMap中的值，除非遍历所有Reference来找，那样效率比较低
	//但这样实现的坏处在于每条记录都存了两遍
	//frontMap中的key在等号后面，selfKey在等号前面
	private Map<String, Map<String, ReferenceItem>> frontMap = new HashMap<>();
	//backKeySet中的key在等号前面，selfKey在等号后面
	private Set<String> backKeySet = new HashSet<>();
	private String selfKey; //一个体例自己对应的key
	
	private Map<String, Reference> references;

    public int getRealCount() {
        return realCount;
    }
    // 当前该reference中有多少条实际的正、异条目
    private int realCount = 0;
		
	public Reference(String key, HashMap<String, Reference> refs) {
		selfKey = key;
		references = refs;
	}
	
	public String getSelfKey() {
		return selfKey;
	}
	
	//体例中添加一条记录
	public void add(String key, String fileName, String lineNumStr) {
		// treeMap中按key排过序
		Map<String, ReferenceItem> map = frontMap.computeIfAbsent(key, k -> new TreeMap<>());
		//每条对应的selfKey=key在一篇文档中只添加一次就够了
		ReferenceItem item = map.computeIfAbsent(fileName, k -> new ReferenceItem(fileName));
		item.addLineNum(lineNumStr);
	}
	
	public void addBackKey(String key) {
		backKeySet.add(key);
	}

	private static String NAME_SPLITER = "_";
	private static String NAME_CONCATER = "+";
	private static String VOLUMN_N = "n";
	private static String VOLUMN_T = "T";
	private static char ZERO = '0';
	//按规则过滤文件名
	private static void trimStr(StringBuilder sb, String str) {
		if(str.startsWith(VOLUMN_T)) {
			sb.append(VOLUMN_T);
			String[] parts = str.split(VOLUMN_N);
			trimZero(sb, parts[0].substring(1));
			sb.append(VOLUMN_N);
			trimZero(sb, parts[1]);
		} else {
			trimZero(sb, str);
		}
	}
	//过滤数字前的0
	private static void trimZero(StringBuilder sb, String str) {
		for (int i = 0; i < str.length(); i++) {
			if(ZERO != str.charAt(i)) {
				sb.append(str.substring(i));
				break;
			}
		}
	}
	
	//arr是已经按字母序排过的文件名列表，需要整合成字符串，并且如果和前一段的t n都相同，则整合成一个整体
	public static void appendArrayList(StringBuilder sb, Map<String, ReferenceItem> map) {
		String lastVolume = "";
		int i = 0;
		for (Entry<String, ReferenceItem> entry : map.entrySet()) {
			String str = entry.getKey();
			String lineNum = entry.getValue().getLineNums();
			String [] parts = str.split(NAME_SPLITER);
			// 同一卷，进行整合
			if(parts.length>=2 && lastVolume.equals(parts[0])) {
				sb.append(NAME_CONCATER);
				trimZero(sb, parts[1]);
			} else { //开启新一段
				if(i != 0) {
					sb.append("/");
				}
				trimStr(sb, parts[0]);
				sb.append(NAME_SPLITER);
				trimZero(sb, parts[1]);
				lastVolume = parts[0];
			}
			sb.append(lineNum);
			i++;
		}
	}

	//appendixItem中用到之前不加行号的版本
	public static void appendArrayList(StringBuilder sb, ArrayList<String> arr) {
		String lastVolume = "";
		for (int i = 0; i < arr.size(); i++) {
			String str = arr.get(i);
			String [] parts = str.split(NAME_SPLITER);
			// 同一卷，进行整合
			if(parts.length>=2 && lastVolume.equals(parts[0])) {
				sb.append(NAME_CONCATER);
				trimZero(sb, parts[1]);
			} else { //开启新一段
				if(i!=0) {
					sb.append("/");
				}
				trimStr(sb, parts[0]);
				sb.append(NAME_SPLITER);
				trimZero(sb, parts[1]);
				lastVolume = parts[0];
			}
		}
	}

	//获取selfKey=key的条目字符串
	public String getBackContent(String key) {
		StringBuilder sb = new StringBuilder();
		appendArrayList(sb, frontMap.get(key));
		return sb.toString();
	}
	
	public int getBackCount(String key) {
		if(frontMap.containsKey(key)) {
			return frontMap.get(key).size();
		}
		return 0;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("【");
		sb.append(selfKey);
		sb.append("】\r\n");
		HashSet<String> keySet = new HashSet<>();
		ArrayList<SortHelper> helperList = new ArrayList<>();
		HashMap<String, SortHelper> helperMap = new HashMap<>();
		realCount = 0; //记录真实条目 other的backCount先不统计进来，避免重复统计
		for (Entry<String, Map<String, ReferenceItem>> entry : frontMap.entrySet()) {
			String key = entry.getKey();
			Map<String, ReferenceItem> map = entry.getValue();
			StringBuilder localSb = new StringBuilder();
			localSb.append(key);
			localSb.append("（正：");
			appendArrayList(localSb, map);
			int count = map.size();
			if(backKeySet.contains(key)) {
				if(references.containsKey(key)) {
					Reference other = references.get(key);
					localSb.append("|異：");
					localSb.append(other.getBackContent(selfKey));
					count += other.getBackCount(selfKey);
				}
			}
			localSb.append("）\r\n");
			keySet.add(key);
			String[] outs = new String[1];
			realCount += count;
			// 如果是+ -号相关的，排在后面
			if(Utils.checkStringContainsAdd(key, outs)) {
				count = -1;
			}
			if(Utils.checkStringContainsSub(key, outs)) {
				count = -2;
			}
			//缺字的也排在最后
			if(key.contains(Utils.LACK_WORD)) {
				count = -3;
			}
			SortHelper helper = new SortHelper(count, key, localSb.toString());
			helperList.add(helper);
			helperMap.put(key, helper);
		}
		
		for (String key : backKeySet) {
			if(!keySet.contains(key) && references.containsKey(key)) {
				Reference other = references.get(key);
				String localSb = key +
						"（異：" +
						other.getBackContent(selfKey) +
						"）\r\n";
				SortHelper helper = new SortHelper(other.getBackCount(selfKey), key, localSb);
				helperList.add(helper);
				helperMap.put(key, helper);
			}
		}
		//带有问号标记的key应该和不带问号标记的排在一起 通过将count赋值进来来实现
		for (SortHelper helper : helperList) {
			if (Utils.endsWithOptionalMark(helper.key)) {
				String trueKey = helper.key.substring(0, helper.key.length() - 1);
				if (helperMap.containsKey(trueKey)) {
					helper.count = helperMap.get(trueKey).count;
				}
			}
		}
		//按照出现次数逆序排列所有的可能
		helperList.sort((h1, h2)->{
			if(h1.count!=h2.count) {
				return h2.count - h1.count;
			} else {
				return h1.strokeItem.compareTo(h2.strokeItem);
			}
		});
		for (int i = 0; i < helperList.size(); i++) {
			SortHelper helper = helperList.get(i);
			sb.append(String.format("[%d]", i+1));
			sb.append(helper.str);
		}
		
		return sb.toString();
	}
}
