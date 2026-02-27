/**
 * SLG GM 后台公共 JavaScript
 */

/**
 * 发送 GET 请求并返回 JSON
 */
function fetchGet(url) {
    return fetch(url, {
        method: 'GET',
        headers: {
            'X-Requested-With': 'XMLHttpRequest'
        },
        credentials: 'same-origin'
    }).then(function(response) {
        return response.json();
    });
}

/**
 * 发送 POST 请求（form-urlencoded）并返回 JSON
 */
function fetchPost(url, body) {
    return fetch(url, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            'X-Requested-With': 'XMLHttpRequest'
        },
        credentials: 'same-origin',
        body: body
    }).then(function(response) {
        return response.json();
    });
}

/**
 * 发送 POST 请求（JSON body）并返回 JSON
 */
function fetchPostJson(url, data) {
    return fetch(url, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'X-Requested-With': 'XMLHttpRequest'
        },
        credentials: 'same-origin',
        body: JSON.stringify(data)
    }).then(function(response) {
        return response.json();
    });
}

/**
 * 格式化时间戳为可读字符串
 */
function formatTime(timestamp) {
    if (!timestamp || timestamp === 0) return '-';
    var date = new Date(timestamp);
    return date.getFullYear() + '-' +
        pad(date.getMonth() + 1) + '-' +
        pad(date.getDate()) + ' ' +
        pad(date.getHours()) + ':' +
        pad(date.getMinutes()) + ':' +
        pad(date.getSeconds());
}

function pad(n) {
    return n < 10 ? '0' + n : '' + n;
}
