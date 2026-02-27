<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>SLG GM 后台 - 登录</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/pstatic/css/style.css">
</head>
<body class="login-body">
    <div class="login-container">
        <div class="login-header">
            <h1>SLG GM 后台</h1>
            <p>服务器管理系统</p>
        </div>
        <form id="loginForm" class="login-form">
            <div class="form-group">
                <label for="username">用户名</label>
                <input type="text" id="username" name="username" placeholder="请输入用户名" required autofocus>
            </div>
            <div class="form-group">
                <label for="password">密码</label>
                <input type="password" id="password" name="password" placeholder="请输入密码" required>
            </div>
            <div class="form-group">
                <button type="submit" class="btn btn-primary btn-block">登录</button>
            </div>
            <div id="errorMsg" class="error-msg" style="display:none;"></div>
        </form>
    </div>
    <script src="${pageContext.request.contextPath}/pstatic/js/common.js"></script>
    <script>
        document.getElementById('loginForm').addEventListener('submit', function(e) {
            e.preventDefault();
            var username = document.getElementById('username').value;
            var password = document.getElementById('password').value;
            var errorMsg = document.getElementById('errorMsg');

            fetchPost('${pageContext.request.contextPath}/gm/doLogin',
                'username=' + encodeURIComponent(username) + '&password=' + encodeURIComponent(password))
            .then(function(data) {
                if (data.code === 0) {
                    window.location.href = '${pageContext.request.contextPath}/gm/console/index';
                } else {
                    errorMsg.textContent = data.message;
                    errorMsg.style.display = 'block';
                }
            })
            .catch(function(err) {
                errorMsg.textContent = '网络错误，请重试';
                errorMsg.style.display = 'block';
            });
        });
    </script>
</body>
</html>
