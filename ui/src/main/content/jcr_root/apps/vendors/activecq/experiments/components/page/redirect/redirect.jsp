<%@page session="false" contentType="text/html; charset=utf-8" pageEncoding="UTF-8"
import="com.day.cq.wcm.api.WCMMode"%><%
%><%@include file="/libs/foundation/global.jsp" %><%

/* WCMMode retrieval */
final WCMMode mode = WCMMode.fromRequest(slingRequest);
String view = "views.publish";

/* View selection */
if(WCMMode.EDIT.equals(mode) || WCMMode.DESIGN.equals(mode)) { view = "views.edit"; }

/* View inclusion */
%><sling:include replaceSelectors="<%= view %>"/>