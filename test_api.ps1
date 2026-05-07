$body = '{"username":"diego","email":"diego@test.com","password":"123456"}'
Invoke-RestMethod -Uri 'http://localhost:9898/api/v1/auth/register' -Method Post -Body $body -ContentType application/json