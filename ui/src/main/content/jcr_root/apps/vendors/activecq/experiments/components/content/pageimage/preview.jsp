<%@page session="false" contentType="text/html; charset=utf-8" pageEncoding="UTF-8"
import="com.day.cq.wcm.api.WCMMode,
        com.activecq.experiments.pageimage.*"%><%
%><%@include file="/libs/foundation/global.jsp" %><%

WCMMode mode = WCMMode.fromRequest(slingRequest);
if(mode == null || WCMMode.DISABLED.equals(mode)) {
    slingResponse.sendRedirect(currentPage.getPath() + ".html");
    return;
}
%>

<%
PageImage image16x10 = new PageImage(currentPage, "16x10", 0, 0);
PageImage image16x9 = new PageImage(currentPage, "16x9", 0, 0);
PageImage image4x3 = new PageImage(currentPage, "4x3", 0, 0);

boolean hasErrors = image16x10.hasError() || image16x9.hasError() || image4x3.hasError();
%>

<html>
    <head>
        <title>Page Image Preview</title>
        <style>
            body {
                font-family: Arial, sans-serif;
                margin: 2em auto;
                width: 960px;
            }

            h2 {
                background: #f7f7f7;
                border-bottom: solid 1px #eee;
                margin-top: 1.5em;
                padding: .5em 1em;
            }

            button {
                float: right;
                font-size: 1em;
                padding: .75em 1.5em;
            }

            label {
                font-weight: bold;
            }

            .alt {
                margin: 0 1.5em;
            }

            .error {
                font-color: red;
                background-color: pink;
                padding: 1em;
            }

            h3.error {
                margin: 1em 0;
            }

            .clear {
                clear: both;
            }
        </style>
    </head>
    
    
    <body>
        <button style="float: right;" onclick="window.close();">Close Preview Window</button>

        <h1>Page Image Preview</h1>

        <% if(hasErrors) { %>
        <h3 class="error clear">At least one expected aspect ratio has not been configured. Please review the list below to identify the issues.</h3>
        <% } %>

        <!-- Alt Text -->
        <h2>Default Alt Text</h2>
        <p class="alt"><%= image16x9.getAlt() %></p>


        <!-- 16 x 10 -->
        <h2>16 x 10 Aspect Ratio</h2>
        <% if(image16x10.hasContent()) { %>
            <img src="<%= image16x10.getSrc() %>" alt="<%= image16x10.getAlt() %>"/>
        <% } else { %>
            <p class="error">The configured image could not be rendered.</p>
        <% } %>


        <!-- 16 x 9 -->
        <h2>16 x 9 Aspect Ratio</h2>
        <% if(image16x9.hasContent()) { %>
            <img src="<%= image16x9.getSrc() %>" alt="<%= image16x9.getAlt() %>"/>
        <% } else { %>
            <p class="error">The configured image could not be rendered.</p>
        <% } %>


        <!-- 4 x 3 -->
        <h2>4 x 3 Aspect Ratio</h2>
        <% if(image16x10.hasContent()) { %>
            <img src="<%= image4x3.getSrc() %>" alt="<%= image4x3.getAlt() %>"/>
        <% } else { %>
            <p class="error">The configured image could not be rendered.</p>
        <% } %>
    </body>
</html>

