const { createApp, ref, reactive, computed, watch, onMounted, nextTick } = Vue;

const app = createApp({
    setup() {
        const token = ref(localStorage.getItem('slg-log-token') || '');
        const currentUser = ref(localStorage.getItem('slg-log-user') || '');
        const activeMenu = ref('search');
        const isAdmin = ref(false);

        // ========== 登录 ==========
        const loginForm = reactive({ username: '', password: '' });
        const loginLoading = ref(false);

        async function handleLogin() {
            if (!loginForm.username || !loginForm.password) {
                ElementPlus.ElMessage.warning('请输入用户名和密码');
                return;
            }
            loginLoading.value = true;
            try {
                const resp = await api('/api/auth/login', 'POST', loginForm, false);
                token.value = resp.token;
                currentUser.value = loginForm.username;
                localStorage.setItem('slg-log-token', resp.token);
                localStorage.setItem('slg-log-user', loginForm.username);
                decodeRole();
                loadServerIds();
            } catch (e) {
                ElementPlus.ElMessage.error(e.message || '登录失败');
            } finally {
                loginLoading.value = false;
            }
        }

        function handleLogout() {
            token.value = '';
            currentUser.value = '';
            localStorage.removeItem('slg-log-token');
            localStorage.removeItem('slg-log-user');
        }

        function decodeRole() {
            try {
                const payload = JSON.parse(atob(token.value.split('.')[1]));
                isAdmin.value = payload.role === 'ADMIN';
            } catch { isAdmin.value = false; }
        }

        // ========== 通用 API ==========
        async function api(url, method = 'GET', body = null, auth = true) {
            const headers = { 'Content-Type': 'application/json' };
            if (auth && token.value) headers['Authorization'] = 'Bearer ' + token.value;
            const opts = { method, headers };
            if (body) opts.body = JSON.stringify(body);
            const resp = await fetch(url, opts);
            if (resp.status === 401) { handleLogout(); throw new Error('认证过期，请重新登录'); }
            const data = await resp.json();
            if (!resp.ok) throw new Error(data.error || '请求失败');
            return data;
        }

        // ========== 日志搜索 ==========
        const searchForm = reactive({ query: '', serverId: '', serverType: '', level: '' });
        const searchDateRange = ref(null);
        const searchLoading = ref(false);
        const searchPage = ref(1);
        const searchResult = reactive({ total: 0, entries: [] });
        const serverIds = ref([]);
        const detailVisible = ref(false);
        const detailEntry = ref(null);

        async function loadServerIds() {
            try {
                serverIds.value = await api('/api/logs/server-ids');
            } catch { /* ES 可能不可用 */ }
        }

        async function searchLogs() {
            searchLoading.value = true;
            try {
                const body = {
                    query: searchForm.query,
                    serverId: searchForm.serverId,
                    serverType: searchForm.serverType,
                    level: searchForm.level,
                    page: searchPage.value - 1,
                    size: 50
                };
                if (searchDateRange.value && searchDateRange.value.length === 2) {
                    body.startTime = searchDateRange.value[0];
                    body.endTime = searchDateRange.value[1];
                }
                const data = await api('/api/logs/search', 'POST', body);
                searchResult.total = data.total;
                searchResult.entries = data.entries || [];
            } catch (e) {
                ElementPlus.ElMessage.error(e.message);
            } finally {
                searchLoading.value = false;
            }
        }

        function showLogDetail(entry) {
            detailEntry.value = entry;
            detailVisible.value = true;
        }

        function logRowClass({ row }) {
            if (row.level === 'ERROR') return 'error-row';
            if (row.level === 'WARN') return 'warn-row';
            return '';
        }

        function levelTagType(level) {
            if (level === 'ERROR') return 'danger';
            if (level === 'WARN') return 'warning';
            if (level === 'INFO') return 'info';
            return '';
        }

        // ========== 统计分析 ==========
        const statsForm = reactive({ serverId: '', level: '' });
        const statsDateRange = ref(null);
        const statsLoading = ref(false);
        const dayChartRef = ref(null);
        const serverChartRef = ref(null);
        const levelChartRef = ref(null);

        let dayChart = null, serverChart = null, levelChart = null;

        async function loadStats() {
            statsLoading.value = true;
            try {
                const body = { serverId: statsForm.serverId, level: statsForm.level };
                if (statsDateRange.value && statsDateRange.value.length === 2) {
                    body.startTime = statsDateRange.value[0];
                    body.endTime = statsDateRange.value[1];
                }
                const data = await api('/api/stats', 'POST', body);
                renderCharts(data);
            } catch (e) {
                ElementPlus.ElMessage.error(e.message);
            } finally {
                statsLoading.value = false;
            }
        }

        function renderCharts(data) {
            nextTick(() => {
                if (dayChartRef.value) {
                    if (!dayChart) dayChart = echarts.init(dayChartRef.value, 'dark');
                    dayChart.setOption({
                        backgroundColor: 'transparent',
                        tooltip: { trigger: 'axis' },
                        xAxis: { type: 'category', data: (data.byDay || []).map(d => d.key) },
                        yAxis: { type: 'value' },
                        series: [{ name: '告警数', type: 'bar', data: (data.byDay || []).map(d => d.count),
                            itemStyle: { color: '#f85149' } }],
                        grid: { left: 50, right: 20, bottom: 30, top: 20 }
                    }, true);
                }
                if (serverChartRef.value) {
                    if (!serverChart) serverChart = echarts.init(serverChartRef.value, 'dark');
                    serverChart.setOption({
                        backgroundColor: 'transparent',
                        tooltip: { trigger: 'item' },
                        series: [{
                            type: 'pie', radius: ['40%', '70%'],
                            data: (data.byServer || []).map(d => ({ name: '服务器 ' + d.key, value: d.count })),
                            label: { formatter: '{b}: {c} ({d}%)', color: '#c9d1d9' }
                        }]
                    }, true);
                }
                if (levelChartRef.value) {
                    if (!levelChart) levelChart = echarts.init(levelChartRef.value, 'dark');
                    const colors = { ERROR: '#f85149', WARN: '#d29922', INFO: '#58a6ff', DEBUG: '#3fb950' };
                    levelChart.setOption({
                        backgroundColor: 'transparent',
                        tooltip: { trigger: 'item' },
                        series: [{
                            type: 'pie', radius: ['40%', '70%'],
                            data: (data.byLevel || []).map(d => ({
                                name: d.key, value: d.count,
                                itemStyle: { color: colors[d.key] || '#8b949e' }
                            })),
                            label: { formatter: '{b}: {c} ({d}%)', color: '#c9d1d9' }
                        }]
                    }, true);
                }
            });
        }

        // ========== ES 管理 ==========
        const esLoading = ref(false);
        const esCluster = ref(null);
        const esIndices = ref([]);

        async function loadEsInfo() {
            esLoading.value = true;
            try {
                const [cluster, indices] = await Promise.all([
                    api('/api/es/cluster'),
                    api('/api/es/indices')
                ]);
                esCluster.value = cluster;
                esIndices.value = indices;
            } catch (e) {
                ElementPlus.ElMessage.error('获取 ES 信息失败: ' + e.message);
            } finally {
                esLoading.value = false;
            }
        }

        function esStatusType(status) {
            if (status === 'green') return 'success';
            if (status === 'yellow') return 'warning';
            if (status === 'red') return 'danger';
            return 'info';
        }

        function deleteEsIndex(row) {
            ElementPlus.ElMessageBox.confirm(
                `确认删除索引 "${row.indexName}" ？此操作不可恢复！`, '确认删除',
                { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
            ).then(async () => {
                try {
                    await api(`/api/es/admin/indices/${encodeURIComponent(row.indexName)}`, 'DELETE');
                    ElementPlus.ElMessage.success('索引已删除');
                    loadEsInfo();
                } catch (e) {
                    ElementPlus.ElMessage.error(e.message);
                }
            }).catch(() => {});
        }

        // ========== 告警管理 ==========
        const alertTab = ref('rules');
        const alertRules = ref([]);
        const alertRulesLoading = ref(false);
        const alertRecords = ref([]);
        const alertRecordsLoading = ref(false);
        const alertRecordPage = ref(1);
        const alertRecordTotal = ref(0);
        const alertRecordFilter = reactive({ ruleId: null });

        const ruleDialogVisible = ref(false);
        const editingRule = ref(null);
        const ruleForm = reactive({
            name: '', level: '', keyword: '', serverId: '',
            threshold: 10, timeWindowMinutes: 5, cooldownMinutes: 30, webhookUrl: ''
        });
        const testNotifyLoading = ref(false);

        async function loadAlertRules() {
            alertRulesLoading.value = true;
            try {
                alertRules.value = await api('/api/alert/rules');
            } catch (e) {
                ElementPlus.ElMessage.error(e.message);
            } finally {
                alertRulesLoading.value = false;
            }
        }

        async function loadAlertRecords() {
            alertRecordsLoading.value = true;
            try {
                const params = new URLSearchParams();
                params.set('page', alertRecordPage.value - 1);
                params.set('size', '20');
                if (alertRecordFilter.ruleId) params.set('ruleId', alertRecordFilter.ruleId);
                const data = await api('/api/alert/records?' + params.toString());
                alertRecords.value = data.records || [];
                alertRecordTotal.value = data.total || 0;
            } catch (e) {
                ElementPlus.ElMessage.error(e.message);
            } finally {
                alertRecordsLoading.value = false;
            }
        }

        function showRuleDialog(rule) {
            editingRule.value = rule;
            if (rule) {
                ruleForm.name = rule.name;
                ruleForm.level = rule.level || '';
                ruleForm.keyword = rule.keyword || '';
                ruleForm.serverId = rule.serverId || '';
                ruleForm.threshold = rule.threshold;
                ruleForm.timeWindowMinutes = rule.timeWindowMinutes;
                ruleForm.cooldownMinutes = rule.cooldownMinutes;
                ruleForm.webhookUrl = rule.webhookUrl || '';
            } else {
                ruleForm.name = '';
                ruleForm.level = '';
                ruleForm.keyword = '';
                ruleForm.serverId = '';
                ruleForm.threshold = 10;
                ruleForm.timeWindowMinutes = 5;
                ruleForm.cooldownMinutes = 30;
                ruleForm.webhookUrl = '';
            }
            ruleDialogVisible.value = true;
        }

        async function saveAlertRule() {
            if (!ruleForm.name) {
                ElementPlus.ElMessage.warning('请输入规则名称');
                return;
            }
            try {
                const body = { ...ruleForm, enabled: true };
                if (editingRule.value) {
                    await api(`/api/alert/rules/${editingRule.value.id}`, 'PUT', body);
                    ElementPlus.ElMessage.success('规则已更新');
                } else {
                    await api('/api/alert/rules', 'POST', body);
                    ElementPlus.ElMessage.success('规则已创建');
                }
                ruleDialogVisible.value = false;
                loadAlertRules();
            } catch (e) {
                ElementPlus.ElMessage.error(e.message);
            }
        }

        async function toggleAlertRule(id, enabled) {
            try {
                await api(`/api/alert/rules/${id}/enabled`, 'PUT', { enabled });
                loadAlertRules();
            } catch (e) {
                ElementPlus.ElMessage.error(e.message);
            }
        }

        function deleteAlertRule(rule) {
            ElementPlus.ElMessageBox.confirm(`确认删除规则 "${rule.name}" ？`, '确认', { type: 'warning' })
                .then(async () => {
                    await api(`/api/alert/rules/${rule.id}`, 'DELETE');
                    ElementPlus.ElMessage.success('已删除');
                    loadAlertRules();
                }).catch(() => {});
        }

        async function testWebhook() {
            if (!ruleForm.webhookUrl) {
                ElementPlus.ElMessage.warning('请先填写 Webhook 地址');
                return;
            }
            testNotifyLoading.value = true;
            try {
                const result = await api('/api/alert/test-notify', 'POST', { webhookUrl: ruleForm.webhookUrl });
                if (result.success) {
                    ElementPlus.ElMessage.success('测试消息已发送');
                } else {
                    ElementPlus.ElMessage.error('发送失败: ' + result.message);
                }
            } catch (e) {
                ElementPlus.ElMessage.error(e.message);
            } finally {
                testNotifyLoading.value = false;
            }
        }

        // ========== 用户管理 ==========
        const users = ref([]);
        const showCreateUser = ref(false);
        const newUser = reactive({ username: '', password: '', role: 'OPERATOR' });

        async function loadUsers() {
            try {
                users.value = await api('/api/admin/users');
            } catch (e) {
                ElementPlus.ElMessage.error(e.message);
            }
        }

        async function createUser() {
            try {
                await api('/api/admin/users', 'POST', newUser);
                ElementPlus.ElMessage.success('用户创建成功');
                showCreateUser.value = false;
                newUser.username = '';
                newUser.password = '';
                newUser.role = 'OPERATOR';
                loadUsers();
            } catch (e) {
                ElementPlus.ElMessage.error(e.message);
            }
        }

        async function toggleUser(id, enabled) {
            try {
                await api(`/api/admin/users/${id}/enabled`, 'PUT', { enabled });
                loadUsers();
            } catch (e) {
                ElementPlus.ElMessage.error(e.message);
            }
        }

        function deleteUserConfirm(user) {
            ElementPlus.ElMessageBox.confirm(`确认删除用户 "${user.id}" ?`, '确认', {
                type: 'warning'
            }).then(async () => {
                await api(`/api/admin/users/${user.id}`, 'DELETE');
                ElementPlus.ElMessage.success('已删除');
                loadUsers();
            }).catch(() => {});
        }

        // ========== 工具方法 ==========
        function formatTime(ts) {
            if (!ts) return '-';
            try {
                const d = new Date(ts);
                if (isNaN(d.getTime())) return ts;
                return d.toLocaleString('zh-CN', { hour12: false });
            } catch { return ts; }
        }

        function formatNumber(n) {
            if (n === null || n === undefined) return '-';
            return n.toLocaleString();
        }

        function handleMenuSelect(index) {
            activeMenu.value = index;
            if (index === 'users') loadUsers();
            if (index === 'esManage') loadEsInfo();
            if (index === 'alert') {
                loadAlertRules();
                loadAlertRecords();
            }
            if (index === 'stats') nextTick(() => {
                if (dayChart) dayChart.resize();
                if (serverChart) serverChart.resize();
                if (levelChart) levelChart.resize();
            });
        }

        // ========== 初始化 ==========
        onMounted(() => {
            if (token.value) {
                decodeRole();
                loadServerIds();
            }
            window.addEventListener('resize', () => {
                if (dayChart) dayChart.resize();
                if (serverChart) serverChart.resize();
                if (levelChart) levelChart.resize();
            });
        });

        return {
            token, currentUser, activeMenu, isAdmin,
            loginForm, loginLoading, handleLogin, handleLogout,
            searchForm, searchDateRange, searchLoading, searchPage, searchResult,
            serverIds, detailVisible, detailEntry,
            searchLogs, showLogDetail, logRowClass, levelTagType,
            statsForm, statsDateRange, statsLoading,
            dayChartRef, serverChartRef, levelChartRef, loadStats,
            esLoading, esCluster, esIndices, loadEsInfo, esStatusType, deleteEsIndex,
            alertTab, alertRules, alertRulesLoading,
            alertRecords, alertRecordsLoading, alertRecordPage, alertRecordTotal, alertRecordFilter,
            ruleDialogVisible, editingRule, ruleForm, testNotifyLoading,
            loadAlertRules, loadAlertRecords, showRuleDialog, saveAlertRule,
            toggleAlertRule, deleteAlertRule, testWebhook,
            users, showCreateUser, newUser,
            createUser, toggleUser, deleteUserConfirm,
            formatTime, formatNumber, handleMenuSelect
        };
    }
});

app.use(ElementPlus);
app.mount('#app');
