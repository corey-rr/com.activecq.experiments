<%@ page import="com.day.cq.commons.Doctype,
    org.apache.sling.api.resource.*,
    org.apache.sling.api.request.*,
    java.util.*,
    
    com.day.cq.wcm.foundation.Image" %><%
%><%@include file="/libs/foundation/global.jsp"%><%
    Resource imageResource = resourceResolver.resolve(properties.get("pagePath", String.class) + "/jcr:content/image");
    
   
    
    Image image = new Image(imageResource);

    image.loadStyleData(currentStyle);
    image.setSelector(".img.width.100.height.200"); // use image script
    image.setDoctype(Doctype.fromRequest(request));
    // add design information if not default (i.e. for reference paras)
    if (!currentDesign.equals(resourceDesign)) {
        image.setSuffix(currentDesign.getId());
    }
    %>
    Consumer<br/>
    src: <%= image.getSrc() %>
    
    <% image.draw(out); %>
    
    