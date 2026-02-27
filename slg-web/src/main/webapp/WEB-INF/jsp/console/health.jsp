<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>SLG GM 后台 - 服务状态</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/pstatic/css/style.css">
</head>
<body>
    <jsp:include page="../nav.jsp"/>
    <div class="main-content">
        <div class="page-header">
            <h2>服务状态</h2>
            <button class="btn btn-primary" onclick="refreshHealth()">刷新</button>
        </div>
        <div class="dashboard" id="summaryCards">
        </div>
        <div class="table-container">
            <table class="data-table" id="healthTable">
                <thead>
                    <tr>
                        <th>服务器ID</th>
                        <th>类型</th>
                        <th>存活状态</th>
                        <th>启用状态</th>
                    </tr>
                </thead>
                <tbody id="healthTableBody">
                </tbody>
            </table>
        </div>
    </div>
    <script src="${pageContext.request.contextPath}/pstatic/js/common.js"></script>
    <script>
        var contextPath = '${pageContext.request.contextPath}';

        function refreshHealth() {
            fetchGet(contextPath + '/gm/health/overview')
            .then(function(data) {
                if (data.code === 0) {
                    renderSummary(data.data);
                    renderHealthTable(data.data.details);
                }
            });
        }

        function renderSummary(data) {
            var gs = data.gameServers;
            var ss = data.sceneServers;
            document.getElementById('summaryCards').innerHTML =
                '<div class="stat-card"><div class="stat-title">GameServer</div>' +
                '<div class="stat-value">' + gs.alive + ' / ' + gs.total + '</div>' +
                '<div class="stat-desc">存活 / 总数</div></div>' +
                '<div class="stat-card"><div class="stat-title">SceneServer</div>' +
                '<div class="stat-value">' + ss.alive + ' / ' + ss.total + '</div>' +
                '<div class="stat-desc">存活 / 总数</div></div>';
        }

        function renderHealthTable(details) {
            var tbody = document.getElementById('healthTableBody');
            tbody.innerHTML = '';
            details.forEach(function(d) {
                var tr = document.createElement('tr');
                tr.innerHTML =
                    '<td>' + d.serverId + '</td>' +
                    '<td><span class="badge badge-info">' + d.type + '</span></td>' +
                    '<td>' + (d.alive ? '<span class="badge badge-success">存活</span>' : '<span class="badge badge-danger">离线</span>') + '</td>' +
                    '<td>' + (d.enable ? '<span class="badge badge-success">启用</span>' : '<span class="badge badge-danger">禁用</span>') + '</td>';
                tbody.appendChild(tr);
            });
        }

        refreshHealth();
    </script>
</body>
</html>
