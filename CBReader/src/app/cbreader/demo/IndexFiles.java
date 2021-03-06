package app.cbreader.demo;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.wltea.analyzer.lucene.IKAnalyzer;

import app.cbreader.demo.model.NoteModel;
import app.cbreader.demo.model.SortStrokeItem;
import app.cbreader.demo.model.WrongReferenceItem;


/**
 * Index all text files under a directory.
 * <p>
 * This is a command-line application demonstrating simple Lucene indexing. Run
 * it with no command-line arguments for usage information.
 */
public class IndexFiles {
	private String inPath;
	private boolean parseXml;
	private boolean parseReference;
	private boolean updateIndex;
	private boolean writeFull; //兼容全文和异文检索
	private boolean buildCatalog;
	CatalogGenerator catalogGenerator;
	EmendationParser emendationParser = null;

	public IndexFiles(String chooseDir, boolean parseXml, boolean writeFull, boolean buildCatalog, boolean parseReference, boolean updateIndex) {
		inPath = chooseDir;
		this.parseXml = parseXml;
		this.parseReference = parseReference;
		this.updateIndex = updateIndex;
		this.writeFull = writeFull;
		this.buildCatalog = buildCatalog;
		if(parseXml) {
			if (writeFull) {
				emendationParser = new FullTextGenerator(inPath);
			} else {
				emendationParser = new EmendationParser(inPath);
			}
		}
	}

	public void prepareIndex() {
		if(parseXml) {
			emendationParser.parseAllDocs(catalogGenerator.getMatchFileMap());
		}
	}

	public void buildCatalog() {
		catalogGenerator = new CatalogGenerator(inPath, emendationParser);
		if (buildCatalog) {
			catalogGenerator.buildCatalog();
		}
	}

	public List<File> getDefaultDocDirs() {
		String docsPath = Utils.getBaseDir();
		if (writeFull) {
			docsPath += Utils.FULLTEXT_PATH;
		} else {
			docsPath += Utils.NOTE_PATH;
		}

		final File docDir = new File(docsPath);
		if (!docDir.exists() || !docDir.canRead()) {
			System.out
					.println("Document directory '"
							+ docDir.getAbsolutePath()
							+ "' does not exist or is not readable, please check the path");
			System.exit(1);
		}
		return Collections.singletonList(docDir);
	}

	public boolean buildIndex(List<File> docDirs, boolean newIndex) {
		boolean ret = true;
		//System.getProperty("user.dir")返回执行java程序的目录

		String indexPath = Utils.getIndexPath(writeFull);

		Date start = new Date();
		try {
			System.out.println("Indexing to directory '" + indexPath + "'...");

			Directory dir = FSDirectory.open(new File(indexPath));
			// :Post-Release-Update-Version.LUCENE_XY:
			// Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_48);
			Analyzer analyzer = new IKAnalyzer();
			IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_48,
					analyzer);

			// Add new documents to an existing index:
			if (newIndex) {
				iwc.setOpenMode(OpenMode.CREATE);
			} else {
				iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
			}

			
			IndexWriter writer = new IndexWriter(dir, iwc);
			for (File docDir : docDirs) {
				ret = ret && indexDocs(writer, docDir);
			}
			System.out.println("words in catalog=" + inCatalogCount);
			// NOTE: if you want to maximize search performance,
			// you can optionally call forceMerge here. This can be
			// a terribly costly operation, so generally it's only
			// worth it when your index is relatively static (ie
			// you're done adding documents to it):
			//
			// writer.forceMerge(1);

			writer.close();

			Date end = new Date();
			System.out.println((end.getTime() - start.getTime()) + " total milliseconds");
			
			if(parseReference) {
				ReadStrokeData(); 
				outputReferences();
			}

		} catch (IOException e) {
			System.out.println(" caught a " + e.getClass()
					+ "\n with message: " + e.getMessage());
			ret = false;
		}
		return ret;
	}


	/**
	 * Indexes the given file using the given writer, or if a directory is
	 * given, recurses over files and directories found under the given
	 * directory.
	 * 
	 * NOTE: This method indexes one document per input file. This is slow. For
	 * good throughput, put multiple documents into your input file(s). An
	 * example of this is in the benchmark module, which can create "line doc"
	 * files, one document per line, using the <a href=
	 * "../../../../../contrib-benchmark/org/apache/lucene/benchmark/byTask/tasks/WriteLineDocTask.html"
	 * >WriteLineDocTask</a>.
	 * 
	 * @param writer
	 *            Writer to the index where the given file/dir info will be
	 *            stored
	 * @param file
	 *            The file to index, or the directory to recurse into to find
	 *            files to index
	 * @throws IOException
	 *             If there is a low-level I/O error
	 */
	public boolean indexDocs(IndexWriter writer, File file) {
		boolean ret = true;
		// do not try to index files that cannot be read
		if (file.canRead()) {
			if (file.isDirectory()) {
				String[] files = file.list();
				// an IO error could occur
				if (files != null) {
					for (String file1 : files) {
						indexDocs(writer, new File(file, file1));
					}
				}
			} else {

				FileInputStream fis;
				try {
					fis = new FileInputStream(file);
				} catch (FileNotFoundException fnfe) {
					// at least on windows, some temporary files raise this
					// exception with an "access denied" message
					// checking if the file can be read doesn't help
					return false;
				}

				try {
					// make a new, empty document
					Document doc = new Document();
					if(updateIndex) {
						// Add the path of the file as a field named "path". Use a
						// field that is indexed (i.e. searchable), but don't
						// tokenize
						// the field into separate words and don't index term
						// frequency
						// or positional information:
						Field pathField = new StringField("path", file.getPath(),
								Field.Store.YES);
						doc.add(pathField);
						// Add the last modified date of the file a field named
						// "modified".
						// Use a LongField that is indexed (i.e. efficiently
						// filterable with
						// NumericRangeFilter). This indexes to milli-second
						// resolution, which
						// is often too fine. You could instead create a number
						// based on
						// year/month/day/hour/minutes/seconds, down the resolution
						// you require.
						// For example the long value 2011021714 would mean
						// February 17, 2011, 2-3 PM.
						doc.add(new LongField("modified", file.lastModified(),
								Field.Store.NO));
						// Add the contents of the file to a field named "contents".
						// Specify a Reader,
						// so that the text of the file is tokenized and indexed,
						// but not stored.
						// Note that FileReader expects the file to be in UTF-8
						// encoding.
						// If that's not the case searching for special characters
						// will fail.
					}
					String fileName = file.getName(); //去除扩展名
					String[] nameArray = fileName.split("\\.");

					if(nameArray.length>=2) {
						fileName = nameArray[0];
					} else {
						System.out.println("index one wrong filename="+fileName);
					}
					// String onlyId = fileName.substring(0, fileName.lastIndexOf("_"));
					BufferedReader bfReader = new BufferedReader(
							new InputStreamReader(fis, StandardCharsets.UTF_8));
					StringBuilder sb = new StringBuilder();
					while (true) {
						String tmp = bfReader.readLine();
						if (null == tmp)
							break;
						if(parseReference) {
							// 如果有catalog，则需要校验是否在其中，即有catalog时根据其范围生成reference，否则生成全部reference
							if (!buildCatalog || catalogGenerator.isFileMatch(fileName)) {
								referenceOne(tmp, fileName);
							}
						}
						sb.append(tmp).append("\r\n");
					}
					bfReader.close();
					if(updateIndex) {
						TextField textField = new TextField("contents",
								sb.toString(), Field.Store.YES);
						doc.add(textField);
						if (writer.getConfig().getOpenMode() == OpenMode.CREATE) {
							// New index, so we just add the document (no old
							// document can be there):
							System.out.println("adding " + file);
							writer.addDocument(doc);
						} else {
							// Existing index (an old copy of this document may have
							// been indexed) so
							// we use updateDocument instead to replace the old one
							// matching the exact
							// path, if present:
							System.out.println("updating " + file);
							writer.updateDocument(new Term("path", file.getPath()),
									doc);
						}
					}
					fis.close();
				} catch(IOException e) {
					e.printStackTrace();
					ret = false;
				}
			}
		}
		return ret;
	}
	////////////////////////////////////////////生成体例相关//////////////////////////////////////////////////////
	
	//TreeMap的comparator是对两个key进行比较
	private HashMap<String, Reference> references = new HashMap<>();
	//存放需要人工分析的例子
	private ArrayList<WrongReferenceItem> wrongItems = new ArrayList<>();
	


	private int inCatalogCount = 0; //统计有多少是属于catalog的
	
	private void referenceOne(String sample, String fileName) {
		int catalogCount = 0; //统计这次有多少条异文

		List<NoteModel> notes = Utils.getNotes(sample, false);
		for (int i = 0; i < notes.size(); i++) {
			NoteModel noteModel = notes.get(i);
			String firstKey = noteModel.getKey();
			String secondKey = noteModel.getNote();

			if(!references.containsKey(firstKey)) {
				Reference ref = new Reference(firstKey, references);
				references.put(firstKey, ref);
			}


			Reference ref = references.get(firstKey);
			ref.add(secondKey, fileName, noteModel.getLineNumStr());
			catalogCount++;
			// 只有相等的情况，才需要以异文作为主key
			if(noteModel.isEqual()) {
				if(!references.containsKey(secondKey)) {
					Reference ref2 = new Reference(secondKey, references);
					references.put(secondKey, ref2);
				}
				Reference ref2 = references.get(secondKey);
				ref2.addBackKey(firstKey);
			}
		}

		if (emendationParser.isInCatalog(fileName)) {
			inCatalogCount += catalogCount;
		}
	}

	private String referenceRootPath;

	//输出整个体例
	private void outputReferences() {
		//操作文件
		referenceRootPath = Utils.getBaseDir()+"/references";
		File outDir = new File(referenceRootPath);
		if(!outDir.exists()) {
			outDir.mkdirs();
		}

		int fileNum = 0;
		StringBuilder builder = new StringBuilder();
		//按照笔画顺序输出每一个不同项的体例
		ArrayList<SortStrokeItem> keyList = new ArrayList<>();
		for (String key : references.keySet()) {
			//带缺字的可能会作为主key，但不作为主key列出来
			if(!key.contains(Utils.LACK_WORD)) {
				keyList.add(new SortStrokeItem(key));
			}
		}
		keyList.sort(SortStrokeItem::compareTo);
		
		StringBuilder indexBuilder = new StringBuilder(); //存放索引
		String zeroPart="", zeroIndex="";
		String indexFileName = "/目录.txt";
		String referenceFileName = "/体例.txt";
		write2File(indexFileName, "", false); //重置文件
		write2File(referenceFileName, "", false); //重置文件
        for (SortStrokeItem item : keyList) {
            Reference val = references.get(item.key);
            String str = val.toString();
            int itemStroke = item.getFirstStroke();
            if (fileNum != itemStroke) {
                write2File(String.format("/%03d.txt", fileNum), builder.toString(), false);
                if (fileNum == 0) {
                    indexBuilder.insert(0, "【特殊笔画】\r\n");
                    indexBuilder.append("\r\n");
                    zeroIndex = indexBuilder.toString();
                } else {
                    String strokeStr = String.format("【%d画】\r\n", fileNum);
                    indexBuilder.insert(0, strokeStr);
                    indexBuilder.append("\r\n");
                    write2File(indexFileName, indexBuilder.toString(), true);
                }
                builder.delete(0, builder.length());
                indexBuilder.delete(0, indexBuilder.length());
                fileNum = itemStroke;
            }
            indexBuilder.append(item.key).append("\r\n");
            builder.append(str).append("\r\n");
        }
		//输出最后一部分
		write2File(String.format("/%03d.txt", fileNum), builder.toString(), false);
		String strokeStr = String.format("【%d画】\r\n", fileNum);
		indexBuilder.insert(0, strokeStr);
		indexBuilder.append("\r\n");
		write2File(indexFileName, indexBuilder.toString(), true);
		builder.delete(0, builder.length());
		indexBuilder.delete(0, indexBuilder.length());
		
		write2File(indexFileName, zeroIndex, true);
		fileNum = 0;
		//输出总和的正文
		int count = 0;
        for (SortStrokeItem item : keyList) {
            Reference val = references.get(item.key);
            String str = val.toString();
            count += val.getRealCount();
            int itemStroke = item.getFirstStroke();
            if (fileNum != itemStroke) {
                if (fileNum == 0) {
                    builder.insert(0, "【特殊笔画】\r\n");
                    builder.append("\r\n");
                    zeroPart = builder.toString();
                } else {
                    strokeStr = String.format("【%d画】\r\n", fileNum);
                    builder.insert(0, strokeStr);
                    builder.append("\r\n");
                    write2File(referenceFileName, builder.toString(), true);
                }
                builder.delete(0, builder.length());
                fileNum = itemStroke;
            }
            builder.append(str).append("\r\n");
        }
		System.out.println(String.format("总共条目数=%d 总共词对=%d", keyList.size(), count));
		strokeStr = String.format("【%d画】\r\n", fileNum);
		builder.insert(0, strokeStr);
		builder.append("\r\n");
		write2File(referenceFileName, builder.toString(), true);
		write2File(referenceFileName, zeroPart, true);
		
		//输出附录
		if(null!=emendationParser) {
			write2File("/附录.txt", emendationParser.appendix2Str(), false);
		}
		
		//列出需要人工调试的部分
		if(wrongItems.size()>0) {
			builder.delete(0, builder.length());
			for (WrongReferenceItem item : wrongItems) {
				builder.append(item.toString());
			}
			write2File("/需要人工纠错.txt", builder.toString(), false);
		}
	}
	
	private void write2File(String relativeName, String content, boolean isAppend) {
		File outFile = new File(referenceRootPath+relativeName);
		try {
			if(!outFile.exists()) {
				//先创建父目录
				outFile.getParentFile().mkdirs();
				outFile.createNewFile();
			}
			PrintWriter pw;
			if(isAppend) {
				FileWriter fw = new FileWriter(outFile, true);
				pw = new PrintWriter(fw);
				pw.write(content);
				pw.flush();
				fw.flush();
				pw.close();
				fw.close();
			} else {
				pw = new PrintWriter(outFile, StandardCharsets.UTF_8.toString());
				pw.write(content);
				pw.flush();
				pw.close();
			}
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public static HashMap<String, Integer> strokeMap = new HashMap<String, Integer>();
	private void ReadStrokeData() {
		BufferedReader reader = Utils.openFile("/dictData/Unihan_DictionaryLikeData.txt");
		if(reader == null) {
			return;
		}
		try {
			while(true) {
				String tmp = reader.readLine();
				if(null == tmp) {
					break;
				}
				if(tmp.startsWith("#")) {
					continue;
				}
				String[] arr = tmp.split("\t");
				if(arr.length!=3) {
					continue;
				}
				if(!"kTotalStrokes".equals(arr[1])) {
					continue;
				}
				strokeMap.put(arr[0].substring(2), Integer.parseInt(arr[2]));
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/** Index all text files under a directory. */
//	public static void main(String[] args) {
//		String usage = "java org.apache.lucene.demo.IndexFiles"
//				+ " [-index INDEX_PATH] [-docs DOCS_PATH] [-update]\n\n"
//				+ "This indexes the documents in DOCS_PATH, creating a Lucene index"
//				+ "in INDEX_PATH that can be searched with SearchFiles";
//		String indexPath = "index";
//		String docsPath = null;
//		boolean create = true;
//		for (int i = 0; i < args.length; i++) {
//			if ("-index".equals(args[i])) {
//				indexPath = args[i + 1];
//				i++;
//			} else if ("-docs".equals(args[i])) {
//				docsPath = args[i + 1];
//				i++;
//			} else if ("-update".equals(args[i])) {
//				create = false;
//			}
//		}
//
//		EmendationParser parser = new EmendationParser("F:/XML");
//		parser.parseAllDocs();
//
//		if (docsPath == null) {
//			System.err.println("Usage: " + usage);
//			System.exit(1);
//		}
//
//		final File docDir = new File(docsPath);
//		if (!docDir.exists() || !docDir.canRead()) {
//			System.out
//					.println("Document directory '"
//							+ docDir.getAbsolutePath()
//							+ "' does not exist or is not readable, please check the path");
//			System.exit(1);
//		}
//
//		Date start = new Date();
//		try {
//			System.out.println("Indexing to directory '" + indexPath + "'...");
//
//			Directory dir = FSDirectory.open(new File(indexPath));
//			// :Post-Release-Update-Version.LUCENE_XY:
//			// Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_48);
//			Analyzer analyzer = new IKAnalyzer();
//			IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_48,
//					analyzer);
//
//			if (create) {
//				// Create a new index in the directory, removing any
//				// previously indexed documents:
//				iwc.setOpenMode(OpenMode.CREATE);
//			} else {
//				// Add new documents to an existing index:
//				iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
//			}
//
//			// Optional: for better indexing performance, if you
//			// are indexing many documents, increase the RAM
//			// buffer. But if you do this, increase the max heap
//			// size to the JVM (eg add -Xmx512m or -Xmx1g):
//			//
//			// iwc.setRAMBufferSizeMB(256.0);
//
//			IndexWriter writer = new IndexWriter(dir, iwc);
//			indexDocs(writer, docDir);
//
//			// NOTE: if you want to maximize search performance,
//			// you can optionally call forceMerge here. This can be
//			// a terribly costly operation, so generally it's only
//			// worth it when your index is relatively static (ie
//			// you're done adding documents to it):
//			//
//			// writer.forceMerge(1);
//
//			writer.close();
//
//			Date end = new Date();
//			System.out.println(end.getTime() - start.getTime()
//					+ " total milliseconds");
//
//		} catch (IOException e) {
//			System.out.println(" caught a " + e.getClass()
//					+ "\n with message: " + e.getMessage());
//		}
//	}
}
