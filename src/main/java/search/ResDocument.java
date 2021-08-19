package search;
import com.alibaba.fastjson.annotation.JSONField;

public class ResDocument {
    @JSONField(name = "url")
    public String url;

    @JSONField(name = "title")
    public String title;

    @JSONField(name = "content")
    public String content;

    public ResDocument() {}

    public ResDocument(String url, String title, String content) {
        this.url = url;
        this.title = title;
        this.content = content;
    }
}
