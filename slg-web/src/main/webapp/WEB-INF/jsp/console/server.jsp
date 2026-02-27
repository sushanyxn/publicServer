<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>SLG GM 后台 - 服务器管理</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/pstatic/css/style.css">
</head>
<body>
    <jsp:include page="../nav.jsp"/>
    <div class="main-content">
        <div class="page-header">
            <h2>服务器管理</h2>
            <button class="btn btn-primary" onclick="refreshServerList()">刷新</button>
        </div>
        <div class="table-container">
            <table class="data-table" id="serverTable">
                <thead>
                    <tr>
                        <th>服务器ID</th>
                        <th>地址</th>
                        <th>端口</th>
                        <th>启用</th>
                        <th>列表可见</th>
                        <th>Game 存活</th>
                        <th>Scene 存活</th>
                        <th>注册角色数</th>
                        <th>导量开关</th>
                        <th>操作</th>
                    </tr>
                </thead>
                <tbody id="serverTableBody">
                </tbody>
            </table>
        </div>
    </div>

    <!-- 导量开关修改弹窗 -->
    <div id="diversionModal" class="modal" style="display:none;">
        <div class="modal-content">
            <div class="modal-header">
                <h3>修改导量开关</h3>
                <span class="modal-close" onclick="closeDiversionModal()">&times;</span>
            </div>
            <div class="modal-body">
                <p>服务器ID: <span id="modalServerId"></span></p>
                <div class="form-group">
                    <label>导量开关</label>
                    <select id="modalDiversionSwitch">
                        <option value="close">close</option>
                        <option value="open">open</option>
                        <option value="auto">auto</option>
                    </select>
                </div>
            </div>
            <div class="modal-footer">
                <button class="btn btn-default" onclick="closeDiversionModal()">取消</button>
                <button class="btn btn-primary" onclick="saveDiversion()">保存</button>
            </div>
        </div>
    </div>

    <script src="${pageContext.request.contextPath}/pstatic/js/common.js"></script>
    <script>
        var contextPath = '${pageContext.request.contextPath}';

        function refreshServerList() {
            fetchGet(contextPath + '/gm/server/list')
            .then(function(data) {
                if (data.code === 0) {
                    renderServerTable(data.data);
                }
            });
        }

        function renderServerTable(servers) {
            var tbody = document.getElementById('serverTableBody');
            tbody.innerHTML = '';
            servers.forEach(function(s) {
                var tr = document.createElement('tr');
                tr.innerHTML =
                    '<td>' + s.serverId + '</td>' +
                    '<td>' + (s.gameHost || '-') + '</td>' +
                    '<td>' + s.gamePort + '</td>' +
                    '<td>' + statusBadge(s.enable) + '</td>' +
                    '<td>' + statusBadge(s.inServerList) + '</td>' +
                    '<td>' + aliveBadge(s.alive) + '</td>' +
                    '<td>' + aliveBadge(s.sceneAlive) + '</td>' +
                    '<td>' + s.registedRole + '</td>' +
                    '<td>' + (s.diversionSwitch || '-') + '</td>' +
                    '<td><button class="btn btn-sm" onclick="openDiversionModal(' + s.serverId + ',\'' + (s.diversionSwitch || 'close') + '\')">导量</button></td>';
                tbody.appendChild(tr);
            });
        }

        function statusBadge(val) {
            return val ? '<span class="badge badge-success">是</span>' : '<span class="badge badge-danger">否</span>';
        }

        function aliveBadge(val) {
            return val ? '<span class="badge badge-success">存活</span>' : '<span class="badge badge-danger">离线</span>';
        }

        function openDiversionModal(serverId, currentSwitch) {
            document.getElementById('modalServerId').textContent = serverId;
            document.getElementById('modalDiversionSwitch').value = currentSwitch;
            document.getElementById('diversionModal').style.display = 'flex';
        }

        function closeDiversionModal() {
            document.getElementById('diversionModal').style.display = 'none';
        }

        function saveDiversion() {
            var serverId = document.getElementById('modalServerId').textContent;
            var diversionSwitch = document.getElementById('modalDiversionSwitch').value;
            fetchPost(contextPath + '/gm/server/diversion',
                'serverId=' + serverId + '&diversionSwitch=' + diversionSwitch)
            .then(function(data) {
                if (data.code === 0) {
                    closeDiversionModal();
                    refreshServerList();
                } else {
                    alert(data.message);
                }
            });
        }

        refreshServerList();
    </script>
</body>
</html>
