package index;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;
import org.jsoup.Jsoup;

import search.Const;

/**
 * Lucene 索引管理工具类
 */
public class Indexer {
    public static void main(String[] args) throws IOException {
        /** 根目录，larbin爬取网页的存储位置 */
        var rootDir = new File(Const.HTML_PATH);
        /** 存储目录，lucene建立索引的存储位置 */
        var saveDir = new File(Const.SAVE_PATH);
        /** 循环遍历所有目录(dxxxxxx)并建立索引 */
        for (File file : rootDir.listFiles()) {
            System.out.println("===索引文件夹" + file.getName() + "中……===");
            addIndex(file,saveDir);
        }
    }

    /**
     * 功能：对指定目录下的文件添加索引
     * @param srcDir 指定目录，程序将解析该目录下的html文件
     * @param dstDir 目标目录，程序将把生成的索引文件存储在该目录中
     * @throws IOException
     */
    private static void addIndex(File srcDir, File dstDir) throws IOException {
        /** 如果传入的路径不是目录或者目录不存在，则放弃*/
        if (!srcDir.isDirectory() || !srcDir.exists()) {
            return;
        }
        /** 开始计时 */
        var startTime =  System.currentTimeMillis();
        /** 过滤将索引和html文件分开 */
        var indexFile = srcDir.listFiles(new prefixFilter("index"));
        var htmlFile = srcDir.listFiles(new prefixFilter("f"));
        
        /** 读取url并放入list中 */
        var urls = new ArrayList<String>();
        try {
            urls = urlRead(indexFile[0]);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("[WARNING]url读取失败！File:" + srcDir.getName() + "/" + indexFile[0].getName());
        }

        /** 对于爬取的html文件按照名称排序以便与url对应 */
        orderByName(htmlFile);
        int i = 0;
        /** 循环遍历每个html文件并转化成document类型，存放在list中 */
        var docList = new ArrayList<Document>();
        int abandonFile = 0;
        for (File file : htmlFile){
            if(i >= urls.size()) break;
            /** 读取url并进行过滤 */
            var fileUrl = urls.get(i++);
            if(urlFilter(fileUrl)) {
                abandonFile++;
                continue;
            }

            /** 读取并对html文件进行预处理，获得标题与内容 */ 
            String fileContext = null;
            try{
                fileContext = FileUtils.readFileToString(file, "UTF-8");
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("[WARNING]文件内容读取失败！File:" + srcDir.getName());
                abandonFile++;
                continue;
            }
            var htmlDoc = Jsoup.parse(fileContext);
            var title = htmlDoc.select("title").first();
            if (title == null) {
                System.out.println("[WARNING]文件空标题！" + file.getName());
                abandonFile++;
                continue;
            }
            var content = htmlDoc.selectFirst("#main_content_section");
            if (content == null) 
                content = htmlDoc.selectFirst("body");
            if (content == null) {
                System.out.println("[WARNING]文件空内容！" + file.getName());
                abandonFile++;
                continue;
            }

            /** 建立doc所需的field */
            var pathField = new StringField(
                Const.PATH_FIELD_NAME,
                '/' + srcDir.getName() + '/' + file.getName(), 
                Store.YES
                );
            var urlField = new StringField(
                Const.URL_FIELD_NAME, 
                fileUrl, 
                Store.YES
                );
            var titleField = new TextField(
                Const.TITLE_FIELD_NAME, 
                title.text(), 
                Store.YES
                );
            var contentFeild = new TextField(
                Const.CONTENT_FIELD_NAME, 
                content.text(), 
                Store.NO
                );
            
            /** 生成文档并将field都存入文档中 */
            var doc = new Document();
            doc.add(pathField);
            doc.add(urlField);
            doc.add(titleField);
            doc.add(contentFeild);
            
            /** 将文档存入总的list中 */
            docList.add(doc);
        }
        /** 创建分词器和存储目录
         * 使用 SmartChineseAnalyzer 中文分词器
         */
        var analyzer = new SmartChineseAnalyzer();
        var directory = FSDirectory.open(dstDir.toPath());

        /** 创建索引写配置和索引写对象 */
        var config = new IndexWriterConfig(analyzer);
        var indexWriter = new IndexWriter(directory, config);
        indexWriter.deleteAll();
        
        /** 将文档加入到写索引中 */
        for(var doc:docList){
            indexWriter.addDocument(doc);
        }

        /** 提交并关闭写索引 */
        indexWriter.commit();
        indexWriter.close();

        var endTime =  System.currentTimeMillis();
        var usedTime = String.valueOf(endTime-startTime);
        System.out.println(
            "[INFO]索引建立完成，成功处理文件" + docList.size() + "个。"+
            "花费时间："+ usedTime +"ms，冗余文件" + abandonFile +"个。"
            );
    }

    /** 
     * 功能：过滤器，将文件通过名称前缀过滤 
     * @param prefix 需要得到文件的前缀
     * @return bool值，前缀是否匹配
    */
    static class prefixFilter implements FilenameFilter{
        private String prefix;
        
        prefixFilter(String prefix){
            this.prefix = prefix;
        }

        public boolean accept(File dir, String name){
            return name.startsWith(prefix);
        }
    }

    private static boolean urlFilter(String url){
        if(url.indexOf("action=profile;") != -1) return true;
        if(url.indexOf("action=printpage;") != -1) return true;
        if(url.indexOf(";prev_next=") != -1) return true;
        if(url.indexOf(".msg") != -1) return true;
        return false;
    }

    /**
     * 功能：按行读取文件
     * @param file 需要读取的文件
     * @return ArrayList<String>型，每个string代表文件的一行
     * @throws IOException
     */
    private static ArrayList<String> urlRead(File file) throws IOException {
        var fr = new FileReader(file);
        var br = new BufferedReader(fr);
        var line = new String();
        var urls = new ArrayList<String>();
        while ((line = br.readLine()) != null) {
            // 读取的内容前会带有行号，进行去除
            line = line.substring(line.indexOf('h'), line.length());
            urls.add(line);          
        }
        br.close();
        fr.close();
        return urls;
    }

    /**
     * 功能：对文件以名称字典序排列
     * @param files 需排序文件数组
     */
    private static void orderByName(File[] files) {
        List<File> fileList = Arrays.asList(files);
        Collections.sort(fileList, 
        new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return o1.getName().compareTo(o2.getName());
            }
        }
        );
    }
}