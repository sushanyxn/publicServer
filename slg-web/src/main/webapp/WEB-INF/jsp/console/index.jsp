<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>SLG GM 后台 - 控制台</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/pstatic/css/style.css">
</head>
<body>
    <jsp:include page="../nav.jsp"/>
    <div class="main-content">
        <div class="page-header">
            <h2>控制台</h2>
        </div>
        <div class="dashboard">
            <div class="stat-card" id="gameServerCard">
                <div class="stat-title">GameServer</div>
                <div class="stat-value" id="gameServerCount">--</div>
                <div class="stat-desc">存活 / 总数</div>
            </div>
            <div class="stat-card" id="sceneServerCard">
                <div class="stat-title">SceneServer</div>
                <div class="stat-value" id="sceneServerCount">--</div>
                <div class="stat-desc">存活 / 总数</div>
            </div>
        </div>
    </div>
    <script src="${pageContext.request.contextPath}/pstatic/js/common.js"></script>
    <script>
        fetchGet('${pageContext.request.contextPath}/gm/health/overview')
        .then(function(data) {
            if (data.code === 0) {
                var gs = data.data.gameServers;
                var ss = data.data.sceneServers;
                document.getElementById('gameServerCount').textContent = gs.alive + ' / ' + gs.total;
                document.getElementById('sceneServerCount').textContent = ss.alive + ' / ' + ss.total;
            }
        });
    </script>
</body>
</html>
