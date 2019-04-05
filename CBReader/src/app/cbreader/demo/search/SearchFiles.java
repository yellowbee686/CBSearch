package app.cbreader.demo.search;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TotalHitCountCollector;
import org.wltea.analyzer.lucene.IKAnalyzer;

import app.cbreader.demo.Utils;
import app.cbreader.demo.model.SearchResult;

/** Simple command-line based search demo. */
public class SearchFiles {
    private String queryString;
    private boolean writeFull;
    public SearchFiles(String query, boolean writeFull) {
        queryString = query;
        this.writeFull = writeFull;
    }

    /** Simple command-line based search demo. */
//	public static void main(String[] args) throws Exception {
//		String usage = "Usage:\tjava org.apache.lucene.demo.SearchFiles [-index dir] [-field f] [-repeat n] [-queries file] [-query string] [-raw] [-paging hitsPerPage]\n\nSee http://lucene.apache.org/core/4_1_0/demo/ for details.";
//		if (args.length > 0
//				&& ("-h".equals(args[0]) || "-help".equals(args[0]))) {
//			System.out.println(usage);
//			System.exit(0);
//		}
//
//		String index = "index";
//		String field = "contents";
//		String queries = null;
//		int repeat = 0;
//		boolean raw = false;
//		String queryString = null;
//		int hitsPerPage = 10;
//		for (int i = 0; i < args.length; i++) {
//			if ("-index".equals(args[i])) {
//				index = args[i + 1];
//				i++;
//			} else if ("-field".equals(args[i])) {
//				field = args[i + 1];
//				i++;
//			} else if ("-queries".equals(args[i])) {
//				queries = args[i + 1];
//				i++;
//			} else if ("-query".equals(args[i])) {
//				queryString = args[i + 1];
//				i++;
//			} else if ("-repeat".equals(args[i])) {
//				repeat = Integer.parseInt(args[i + 1]);
//				i++;
//			} else if ("-raw".equals(args[i])) {
//				raw = true;
//			} else if ("-paging".equals(args[i])) {
//				hitsPerPage = Integer.parseInt(args[i + 1]);
//				if (hitsPerPage <= 0) {
//					System.err
//							.println("There must be at least 1 hit per page.");
//					System.exit(1);
//				}
//				i++;
//			}
//		}
//
//		IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(
//				index)));
//		IndexSearcher searcher = new IndexSearcher(reader);
//		// :Post-Release-Update-Version.LUCENE_XY:
//		// Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_48);
//		Analyzer analyzer = new IKAnalyzer();
//		BufferedReader in = null;
//		if (queries != null) {
//			in = new BufferedReader(new InputStreamReader(new FileInputStream(
//					queries), StandardCharsets.UTF_8));
//		} else {
//			in = new BufferedReader(new InputStreamReader(System.in,
//					StandardCharsets.UTF_8));
//		}
//		// :Post-Release-Update-Version.LUCENE_XY:
//		QueryParser parser = new QueryParser(Version.LUCENE_48, field, analyzer);
//		parser.setDefaultOperator(QueryParser.OR_OPERATOR);
//		while (true) {
//			if (queries == null && queryString == null) { // prompt the user
//				System.out.println("Enter query: ");
//			}
//
//			String line = queryString != null ? queryString : in.readLine();
//			// String newString = new String(line.getBytes(),"UTF-8");
//			if (line == null || line.length() == -1) {
//				break;
//			}
//
//			line = line.trim();
//			if (line.length() == 0) {
//				break;
//			}
//
//			// line = "xml";
//			//
//			// Term term = new Term(field, line);
//			// 完全匹配查询
//			// Query query = new TermQuery(term);
//			Query query = parser.parse(line);
//			System.out.println("Searching for: " + query.toString());
//
//			if (repeat > 0) { // repeat & time as benchmark
//				Date start = new Date();
//				for (int i = 0; i < repeat; i++) {
//					searcher.search(query, null, 100);
//				}
//				Date end = new Date();
//				System.out.println("Time: " + (end.getTime() - start.getTime())
//						+ "ms");
//			}
//
//			doPagingSearch(in, searcher, query);
//
//			if (queryString != null) {
//				break;
//			}
//		}
//		reader.close();
//	}

    public SearchResult doSearch(SearchOption option) {
        SearchResult result = null;
        String index = Utils.getIndexPath(writeFull);
        String field = "contents";
        IndexReader reader;
        try {
            reader = DirectoryReader.open(FSDirectory.open(new File(index)));
            IndexSearcher searcher = new IndexSearcher(reader);
            // :Post-Release-Update-Version.LUCENE_XY:
            // Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_48);
            Analyzer analyzer = new IKAnalyzer();

            // :Post-Release-Update-Version.LUCENE_XY:
            QueryParser parser = new QueryParser(Version.LUCENE_48, field, analyzer);
            // QueryParser.AND_OPERATOR是完全匹配 OR_OPERATOR是部分匹配，但部分匹配时由于分词不正确可能会都拆成单字
            // 整体的反而没有放在最前，还得自己排序
            parser.setDefaultOperator(QueryParser.AND_OPERATOR);
            String pureString = queryString.trim();
            Query query = parser.parse(pureString);
            System.out.println("Searching for: " + query.toString());
            result = doPagingSearch(searcher, query, pureString, option);

            // 暂时都显示在列表中
            // ret = save2File(pureString, searchResult);


            //使用term进行整个词的搜索是无效的，因为建立索引时也是按照分词来的，并没有整个词
//			Term term = new Term(field, queryString.trim());
//			TermQuery tQuery = new TermQuery(term);
//			System.out.println("Searching for term: " + tQuery.toString());
//			ret = doPagingSearch(searcher, tQuery);
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }
    /**
     * This demonstrates a typical paging search scenario, where the search
     * engine presents pages of size n to the user. The user can then go to the
     * next page if interested in the next hits.
     *
     * When the query is executed for the first time, then only enough results
     * are collected to fill 5 result pages. If the user wants to page beyond
     * this limit, then the query is executed another time and all hits are
     * collected.
     *
     */
    public SearchResult doPagingSearch(IndexSearcher searcher, Query query, String searchKey, SearchOption option) throws IOException {
        // Set<Term> keywords = new HashSet<Term>();
        // query.extractTerms(keywords);
        // 备选单词
//        List<String> queryArr = Arrays.asList(query.toString("contents").split(" "));
//        List<String> strs = new ArrayList<>(queryArr);
//        strs.add(searchKey); // 整个串


        // search第二个参数是需要返回多少条记录，先用total搜索一遍获得记录数目
        TotalHitCountCollector collector = new TotalHitCountCollector();
        searcher.search(query, collector);
        int numTotalHits = collector.getTotalHits();
        System.out.println(numTotalHits + " total matching documents");
        SearchResult ret = new SearchResult(searchKey);
        if (numTotalHits > 0) {
            TopDocs results = searcher.search(query, numTotalHits);
            ScoreDoc[] hits = results.scoreDocs;
            for (int i = 0; i < numTotalHits; i++) {
                Document document = searcher.doc(hits[i].doc);
                //System.out.println((i + 1) + ". " + document.get("path"));
                String contents[] = document.get("contents").split("\r\n");
                String docName = document.get("path");
                docName = docName.substring(docName.lastIndexOf("\\")+1);
                docName = docName.substring(0, docName.lastIndexOf("."));
                for (String content : contents) {
                    if (option.isAbaSearch()) {
                        if (!checkAbaSearch(content, searchKey)) {
                            continue;
                        }
                    }

                    //for (String key : strs) {
                    // 只搜索整个query，则不要需要遍历各个key了，只搜一次即可
                    if (checkCandidate(content, searchKey)) {
                        ret.add(searchKey, docName, content);
                        break; //一条不论是符合哪个key，只出现一次，首先符合的肯定是完整的串
                    }
                    //}
                }
            }
        }
        return ret;
    }

    private boolean checkAbaSearch(String content, String key) {
        // 必须是完整匹配
        int idx = content.indexOf(key);
        if (idx <= 0 || idx == content.length() - 1) {
            return false;
        }
        return content.charAt(idx - 1) == content.charAt(idx + 1);
    }

    // 将搜索结果写入文件
    private boolean save2File(String pureString, SearchResult searchResult) {
        AtomicBoolean ret = new AtomicBoolean(false);
        String dirPath = mkdir(pureString);
        searchResult.getResults().forEach((key, model) -> {
            List<String> results = model.getDocuments();
            if(results.size()>0){
                ret.set(putToFile(key, results, dirPath));
            }
        });
        return ret.get();
    }

    private boolean checkCandidate(String candidate, String key){
        int idx = -1;
        boolean ret = false;
        do {
            idx = candidate.indexOf(key, idx+1);
            //如果没有找到key或者找到的key是被包裹在【】中都不算，否则就返回true
            if(idx==0 || idx>0 && !candidate.substring(idx-1, idx).equals("【")){
                ret = true;
                break;
            }
        } while (idx!=-1);
        // 如果是全文搜索中的异文，要判断是否符合异文的key，去掉所有重复的结果
        if (ret && candidate.startsWith(Utils.NOTE_PREFIX)) {
            int markBackIdx = candidate.indexOf("]", Utils.NOTE_PREFIX.length() + 1);
            String note = candidate.substring(Utils.NOTE_PREFIX.length() + 1, markBackIdx);
            // equal和add的note都是替换的文字，直接比较即可，sub的note是出现的idx，需要和key对比位置
            try {
                int deleteIdx = Integer.parseInt(note) + markBackIdx + 1; //被删除的sub字段应该在的位置
                idx = candidate.indexOf(key);
                // 如果这个位置在搜索词之内，则是正确的
                ret = deleteIdx >= idx && deleteIdx <= idx + key.length();
            } catch (NumberFormatException e) {
                ret = note.contains(key) || key.contains(note);
            }
        }
        return ret;
    }

    //创建文件夹，返回文件夹路径
    public String mkdir(String key) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
        String outDirPath = Utils.getBaseDir()+"/result/"+key+df.format(new Date());
        File outDir = new File(outDirPath);
        if(!outDir.exists() && !outDir.isDirectory()) {
            outDir.mkdirs();
        }
        return outDirPath;
    }

    public Boolean putToFile(String str, List<String> sentences, String outDirPath) {
        boolean ret = true;

        String outPath = outDirPath+"/"+str+"_";
//		for (int i = 0; i < strs.length; i++) {
//			outPath += strs[i]+"_";
//		}
        SimpleDateFormat df = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
        outPath += df.format(new Date())+".txt";
        sentences.sort(String::compareTo);

        File outFile = new File(outPath);
        try {
            if(!outFile.exists())
                outFile.createNewFile();
            PrintWriter pw = new PrintWriter(outFile, StandardCharsets.UTF_8.toString());
            StringBuffer sb = new StringBuffer();
            sb.append(str+":\r\n");
            for (String sen : sentences) {
                sb.append(sen+"\r\n");
            }
            pw.write(sb.toString());
            pw.flush();
            pw.close();
        } catch (IOException e) {
            e.printStackTrace();
            ret = false;
        }
        return ret;
    }
}
