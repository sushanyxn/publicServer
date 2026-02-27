<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>SLG GM 后台 - 无权限</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/pstatic/css/style.css">
</head>
<body class="login-body">
    <div class="login-container">
        <div class="login-header">
            <h1>403 无权限</h1>
            <p>您没有权限访问此页面</p>
        </div>
        <div style="text-align:center;margin-top:20px;">
            <a href="${pageContext.request.contextPath}/gm/console/index" class="btn btn-primary">返回控制台</a>
        </div>
    </div>
</body>
</html>
