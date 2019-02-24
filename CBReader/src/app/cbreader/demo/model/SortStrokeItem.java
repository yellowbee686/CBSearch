package app.cbreader.demo.model;

import java.util.ArrayList;
import java.util.HashMap;

import app.cbreader.demo.IndexFiles;
import app.cbreader.demo.Utils;

public class SortStrokeItem {
	public ArrayList<String> uniValues;
	public String key;
	public ArrayList<Integer> strokes;
	public SortStrokeItem(String k) {
		key = k;
		HashMap<String, Integer> strokeMap = IndexFiles.strokeMap;
		uniValues = Utils.utf8ToUnicode(k);
		strokes = new ArrayList<>(uniValues.size());
		for (String str : uniValues) {
			//特殊符号的笔画都算0
			strokes.add(strokeMap.getOrDefault(str, 0));
		}
	}
	//获取首字符的笔画数，用于分文件
	public int getFirstStroke() {
		if(strokes.size()>0) {
			return strokes.get(0);
		} else {
			return 0;
		}
	}
	
	//对每个字符，按照笔画数排序
	//笔画数相同，按照unicode值排序，这样可以把相同字的放在一起
	public int compareTo(SortStrokeItem other) {
		for (int i = 0; i < strokes.size(); i++) {
			int numSelf = strokes.get(i);
			int numOther = 0;
			if(i<other.strokes.size()) {
				numOther = other.strokes.get(i);
			}
			//笔画数少的排在前面
			if(numSelf!=numOther) {
				return numSelf - numOther;
			} else { 
				String valSelf = uniValues.get(i);
				String valOther = "";
				if(i<other.uniValues.size()) {
					valOther = other.uniValues.get(i);
				}
				if(!valSelf.equals(valOther)) {
					return valSelf.compareTo(valOther);
				}
			}
		}
		return -1; //如果前面排序都没return 说明self和other前面的字符串都相同，且self较短，排在前面
	}
}
