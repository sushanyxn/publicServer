<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<nav class="sidebar">
    <div class="sidebar-header">
        <h3>SLG GM</h3>
    </div>
    <ul class="nav-menu">
        <li class="nav-item">
            <a href="${pageContext.request.contextPath}/gm/console/index" class="nav-link">控制台</a>
        </li>
        <li class="nav-item">
            <a href="${pageContext.request.contextPath}/gm/server/page" class="nav-link">服务器管理</a>
        </li>
        <li class="nav-item">
            <a href="${pageContext.request.contextPath}/gm/health/page" class="nav-link">服务状态</a>
        </li>
        <li class="nav-divider"></li>
        <li class="nav-item">
            <a href="${pageContext.request.contextPath}/gm/logout" class="nav-link">退出登录</a>
        </li>
    </ul>
</nav>
