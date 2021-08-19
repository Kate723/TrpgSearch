package search;
import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSON;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.SimpleSpanFragmenter;
import org.apache.lucene.store.FSDirectory;
import org.jsoup.Jsoup;

/**
 * Lucene 搜索工具类
 */
public class SearchEngine {
    private static final String HTML_PRETAG = "<font color='red'>";
    private static final String HTML_POSTTAG = "</font>";
    public SearchEngine() {}

    public static ArrayList<String> search(String query) throws Exception {
        var indexDir = new File(Const.INDEX_PATH);
        return indexSearch(indexDir, query);
    }

    public static void main(String[] args) throws Exception {
        var indexDir = new File(Const.INDEX_PATH);
        indexSearch(indexDir, "登录");
    }

    public static ArrayList<String> indexSearch(File indexDir, String queryWord) throws Exception {
        if (indexDir == null || queryWord == null) return null;
        /** 开始计时 */
        var startTime =  System.currentTimeMillis();
        /** 建立对query的解析器 */
        var analyzer = new SmartChineseAnalyzer();
        String[] fields = { Const.TITLE_FIELD_NAME, Const.CONTENT_FIELD_NAME };
        Map<String,Float> boosts = new HashMap<String, Float>();
		boosts.put(Const.TITLE_FIELD_NAME, 3f);
        var queryParser = new MultiFieldQueryParser(fields, analyzer,boosts);
        var query = queryParser.parse(QueryParser.escape(queryWord));
        /** 对索引文件夹建立搜索类 */
        var directory = FSDirectory.open(indexDir.toPath());
        var dirReader = DirectoryReader.open(directory);
        var indexSearcher = new IndexSearcher(dirReader);
        /** 开始检索 */
        var topdocs = indexSearcher.search(query, 20);
 
        /**从搜索结果对象中获取结果集
         * 如果没有查询到值，则 ScoreDoc[] 数组大小为 0
         * */
        var scoreDocs = topdocs.scoreDocs;
        var docList = new ArrayList<ResDocument>();
        if (scoreDocs != null){
            var highlighter = queryHighlighter(query);
            for (var scoreDoc:scoreDocs) {    
                /** 通过文档ID从硬盘中读取出对应的文档*/
                var document = dirReader.document(scoreDoc.doc);
                /** 对标题和内容进行高亮处理 */
                var title = getHighlightedTitle(document, analyzer, highlighter);
                var content = getHighlightedContent(document, analyzer, highlighter);

                /** 获取对应域名的值 */
                var resDoc = new ResDocument(
                    document.get(Const.URL_FIELD_NAME), 
                    title, 
                    content
                    );
                docList.add(resDoc);
            }
        }
        
        var endTime =  System.currentTimeMillis();
        var usedTime = String.valueOf(endTime-startTime);

        var jsonList = new ArrayList<String>();
        jsonList.add(String.valueOf(scoreDocs.length));
        jsonList.add(JSON.toJSONString(docList));
        jsonList.add(usedTime);
        return jsonList;
    }
    
    /** 生成高亮器 */
    private static Highlighter queryHighlighter(Query query){
        var queryScorer = new QueryScorer(query);
        var simpleHTMLFormatter = new SimpleHTMLFormatter(
            HTML_PRETAG, 
            HTML_POSTTAG
            );
        var fragmenter = new SimpleSpanFragmenter(queryScorer);
        var highlighter = new Highlighter(simpleHTMLFormatter, queryScorer);
        highlighter.setTextFragmenter(fragmenter);
        return highlighter;
    }

    private static String getHighlightedTitle(Document document, Analyzer analyzer, Highlighter highlighter)
    throws Exception{
        var title = document.get(Const.TITLE_FIELD_NAME);
        String highlightedTitle = null;
        if (title != null) {
            var tokenStream = analyzer.tokenStream(Const.TITLE_FIELD_NAME, new StringReader(title));
            highlightedTitle = highlighter.getBestFragment(tokenStream, title);
        }
        if (highlightedTitle != null) return highlightedTitle;
        return title;
    }

    private static String getHighlightedContent(Document document, Analyzer analyzer, Highlighter highlighter)
    throws Exception{
        /** 读取document对应的文件 */
        var path = document.get(Const.PATH_FIELD_NAME);
        var file = new File(Const.HTML_PATH + path);
        String content = null;
        try{
            content = FileUtils.readFileToString(file, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("[WARNING]文件内容读取失败！File:" + file.getName());
        }
        /** 获取文件核心内容 */
        var htmlDoc = Jsoup.parse(content);
        var mainElement = htmlDoc.selectFirst("#main_content_section");
        if (mainElement == null) mainElement = htmlDoc.selectFirst("body");
        content = mainElement.text();

        /** 进行高亮 */
        String highlightedContent = null;
        if (content != null) {
            var tokenStream = analyzer.tokenStream(Const.CONTENT_FIELD_NAME, new StringReader(content));
            highlightedContent = highlighter.getBestFragment(tokenStream, content);
        }
        if(highlightedContent == null) {
            content = content.substring(0, Math.min(300, content.length())) + "……";
            return content;
        }
        /** 截取高亮片段进行显示 */
        var head = highlightedContent.indexOf(HTML_PRETAG);
        highlightedContent = "……" + highlightedContent.substring(head, Math.min(head + 300, highlightedContent.length())) + "……";
        int cssHead = countMatches(highlightedContent, HTML_PRETAG);
        int cssTail = countMatches(highlightedContent, HTML_POSTTAG);
        if(cssHead > cssTail) highlightedContent += HTML_POSTTAG; 
        return highlightedContent;
    }

    private static int countMatches(String context, String keyword){
        int count = 0;
        while(context.indexOf(keyword) != -1) {
            context = context.substring(context.indexOf(context) + 1,context.length());
            count++;
        }
        return count;
    }
}
