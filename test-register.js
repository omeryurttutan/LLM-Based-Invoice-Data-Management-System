const http = require('http');

const data = JSON.stringify({
  fullName: "John Doe",
  email: "johndoe123@example.com",
  companyName: "Test Company AS",
  taxNumber: "1234567890",
  password: "Password123!"
});

const req = http.request({
  hostname: 'localhost',
  port: 8080,
  path: '/api/v1/auth/register',
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Content-Length': data.length
  }
}, (res) => {
  let body = '';
  res.on('data', d => body += d);
  res.on('end', () => console.log(res.statusCode, body));
});

req.on('error', e => console.error(e));
req.write(data);
req.end();
