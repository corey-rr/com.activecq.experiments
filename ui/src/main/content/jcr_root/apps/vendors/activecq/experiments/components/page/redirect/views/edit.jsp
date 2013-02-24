<%@page session="false" contentType="text/html; charset=utf-8" pageEncoding="UTF-8"
import="com.day.cq.wcm.api.WCMMode"%><%
%><%@include file="/libs/foundation/global.jsp" %><%

/*final String hideInNav = currentPage.isHideInNav() ? "Yes" : "No";
final String redirect = currentPage.getProperties().get("cq:redirect", "");
final String explicitRedirect = StringUtils.isNotBlank(redirect);
*/
%>

<html>
<head>
	<title>Redirect</title>

	<style>
	.activecq .folder-container {
		width: 600px;
		margin: 2em auto;
	}

	.activecq .folder-icon {
		color: #ccc;
		font-family: Wingdings;
		font-size: 475px;
		text-align: center;
		margin-top: -60px;
	}

	.activecq .folder-info {
		font-family: arial, sans-serif;
		background-color: #f7f7f7;
		border: solid 1px #eee;
		padding: 1em 2em;
		margin-top: -60px;
	}

	.activecq .folder-info ul {
		margin: 0;
		padding: 0;
	}

	.activecq .folder-info li {
		list-style: none;
		margin: 1em 0;
	}

	.activecq .folder-info label {
		font-weight: bold;
	}

	.activecq .folder-info a {
		color: #666;
	}

	.activecq .folder-info a:hover {
		color: #000;
	}
	</style>
</head>

<body>
	<div class="activecq">
        <div class="folder-container">
          <div class="folder-icon">1</div>

          <div class="folder-info">
              <ul>
                  <li><label>Hide in navigation:</label> Yes</li>
                  <li><label>Redirect to:</label> <a href="#">/content/foo/bar.html</a></li>
                  <li><label>Redirect type:</label> Explicit</li>
              </ul>
          </div>
      </div>
	</div>
</body>

</html>
