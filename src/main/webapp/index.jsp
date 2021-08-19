<%@ page import = "search.SearchEngine" %>
<%@ page import = "search.ResDocument" %>
<%@ page import = "com.alibaba.fastjson.*" %>
<%@ page language = "java" import = "java.util.*" pageEncoding = "UTF-8"%>
<!DOCTYPE html>
<html lang="zh"></html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width,initial-scale=1.0, minimum-scale=1.0, maximum-scale=1.0, user-scalable=no"/>
    <title>TRPGQuill-主页</title>
    <link rel="stylesheet" type = "text/css" href="./css/index.css">
</head>

<body>
    <header>
        <div class="header_text">
            <h1>TRPGQuill</h1>
            <p>今天你<font color = #222222>跑团</font>了吗？</p>
        </div>
        <form action="index.jsp" method="post">
            <button class = "Button" type = "submit">搜索</button>
            <input name = "query" class = "inputBox" type = "text" placeholder = "找点什么呢……"
             value = "<%= request.getParameter("query") == null ? "" : request.getParameter("query") %>">
        </form>
    </header>


    <div class="main">
        <% 
            String query = request.getParameter("query");
            if (query != null && query != ""){
                SearchEngine searchEngine = new SearchEngine();
                ArrayList<String> resJson = searchEngine.search(query);
                List<ResDocument> resDocs = JSONArray.parseArray(resJson.get(1), ResDocument.class);
                out.println("<h6>共花费" + resJson.get(2) + "ms，检索到" + resJson.get(0) + "个结果</h6>");
                for(ResDocument resDoc:resDocs) {
                    out.println("<h2><a href=\"" + resDoc.url + "\">");
                    out.println(resDoc.title + "</a></h2>");
                    out.println("<p>" + resDoc.content + "</p>");
                }
            }
        %> 
    </div>
    <footer>

    </footer>
</body>

</html>
