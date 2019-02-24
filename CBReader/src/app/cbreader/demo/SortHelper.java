package app.cbreader.demo;

import app.cbreader.demo.model.SortStrokeItem;

//用于对体例或附录进行排序的
public class SortHelper{
	public int count;
	public String str; //体例汇总成的字符串
	public String key; //某一个和【X】相关的key A
	public SortStrokeItem strokeItem;
	public SortHelper(int cnt, String key, String str) {
		count = cnt;
		this.key = key;
		this.str = str;
		strokeItem = new SortStrokeItem(key);
	}
}