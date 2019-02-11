package app.cbreader.demo;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    public boolean doSearch() {
        boolean ret;
        String index = Utils.getIndexPath(writeFull);
        String field = "contents";
        IndexReader reader;
        try {
            reader = DirectoryReader.open(FSDirectory.open(new File(
                    index)));
            IndexSearcher searcher = new IndexSearcher(reader);
            // :Post-Release-Update-Version.LUCENE_XY:
            // Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_48);
            Analyzer analyzer = new IKAnalyzer();

            // :Post-Release-Update-Version.LUCENE_XY:
            QueryParser parser = new QueryParser(Version.LUCENE_48, field, analyzer);
            parser.setDefaultOperator(QueryParser.OR_OPERATOR);
            String pureString = queryString.trim();
            Query query = parser.parse(pureString);
            System.out.println("Searching for: " + query.toString());
            Map<String, List<String>> searchResults = doPagingSearch(searcher, query, pureString);
            ret = save2File(pureString, searchResults);


            //使用term进行整个词的搜索是无效的，因为建立索引时也是按照分词来的，并没有整个词
//			Term term = new Term(field, queryString.trim());
//			TermQuery tQuery = new TermQuery(term);
//			System.out.println("Searching for term: " + tQuery.toString());
//			ret = doPagingSearch(searcher, tQuery);
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
            ret = false;
        }

        return ret;
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
    public Map<String, List<String>> doPagingSearch(IndexSearcher searcher, Query query, String pureString) throws IOException {
        // Set<Term> keywords = new HashSet<Term>();
        // query.extractTerms(keywords);
        // 备选单词
        List<String> strs = Arrays.asList(query.toString("contents").split(" "));
        strs.add(pureString); // 整个串

        // search第二个参数是需要返回多少条记录，先用total搜索一遍获得记录数目
        TotalHitCountCollector collector = new TotalHitCountCollector();
        searcher.search(query, collector);
        int numTotalHits = collector.getTotalHits();
        System.out.println(numTotalHits + " total matching documents");
        Map<String, List<String>> ret = new HashMap<>();
        if (numTotalHits > 0) {
            for (String str : strs) {
                ret.put(str, new ArrayList<>());
            }
            TopDocs results = searcher.search(query, numTotalHits);
            ScoreDoc[] hits = results.scoreDocs;
            for (int i = 0; i < numTotalHits; i++) {
                Document document = searcher.doc(hits[i].doc);
                //System.out.println((i + 1) + ". " + document.get("path"));
                String contents[] = document.get("contents").split("\r\n");
                String docName = document.get("path");
                docName = docName.substring(docName.lastIndexOf("\\")+1);
                docName = docName.substring(0, docName.lastIndexOf("."));
                for (int j = 0; j < contents.length; j++) {
                    contents[j]+="  doc:"+docName;
                }
                for (String content : contents) {
                    if (content.contains("doc:")) {
                        String firstContent = content.split("doc:")[0];
                        for (String key : strs) {
                            if (checkCandidate(firstContent, key)) {
                                ret.get(key).add(content);
                            }
                        }
                    }
                }
            }
        }
        return ret;
    }

    // 将搜索结果写入文件
    private boolean save2File(String pureString, Map<String, List<String>> searchResults) {
        AtomicBoolean ret = new AtomicBoolean(false);
        String dirPath = mkdir(pureString);
        searchResults.forEach((key, results) -> {
            if(results.size()>0){
                ret.set(putToFile(key, results, dirPath));
            }
        });
        return ret.get();
    }

    private Boolean checkCandidate(String candidate, String key){
        int idx = -1;
        boolean ret = false;
        do {
            idx = candidate.indexOf(key, idx+1);
            //如果没有找到key或者找到的key是被包裹在【】中都不算，否则就返回true
            if(idx==0 || idx>0 && !candidate.substring(idx-1, idx).equals("【".toString())){
                ret = true;
                break;
            }
        } while (idx!=-1);
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
