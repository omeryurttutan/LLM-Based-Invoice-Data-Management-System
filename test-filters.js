const http = require('http');

const data = JSON.stringify({ email: 'admin@demo.com', password: 'Admin123!' });

const req = http.request({
  hostname: 'localhost',
  port: 8080,
  path: '/api/v1/auth/login',
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Content-Length': data.length
  }
}, (res) => {
  let body = '';
  res.on('data', d => body += d);
  res.on('end', () => {
    const token = JSON.parse(body).token;
    if (!token) {
        console.error("No token:", body);
        return;
    }
    const apiReq = http.request({
      hostname: 'localhost',
      port: 8080,
      path: '/api/v1/invoices/filter-options',
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    }, (apiRes) => {
      let apiBody = '';
      apiRes.on('data', d => apiBody += d);
      apiRes.on('end', () => console.log(apiRes.statusCode, apiBody));
    });
    apiReq.on('error', e => console.error(e));
    apiReq.end();
  });
});

req.on('error', e => console.error(e));
req.write(data);
req.end();
