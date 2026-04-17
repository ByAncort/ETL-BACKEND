###
POST http://localhost:8083/api/register
Content-Type: application/json

{
 "method": "GET",
 "url": "https://us2.unifier.oraclecloud.com/aesgec",
 "pathParams": "/ws/rest/service/v1/admin/bps/AM-CO630%2F620A",
 "description": "Oracle Unifier BPS Service",
 "authType": "BEARER",
 "authHeader": "Authorization",
 "authValue": "eyJ0eXAiOiJEQiJ9.eyJ1c2VybmFtZSI6IiQkcG9zdG1hbiJ9.7566E1DF-24F6-2971-3513-6A087BC863BE4707E384E0842A3A2B4AB06CD5932437",
  "apiAuth" :{
    "method": "GET",
    "url": "https://us2.unifier.oraclecloud.com/aesgec",
    "pathParams": "/ws/rest/service/v1/login",
    "description": "Oracle Unifier Login",
    "authType": "BEARER",
    "authHeader": "Basic",
    "authValue": "JCRQT1NUTUFOOiQkUE9TVE1BTg=="
  }
}




###
GET http://localhost:8083/api/register/2

<> 2026-04-16T223548.200.json
<> 2026-04-16T222737.200.json
<> 2026-04-16T222522.200.json
<> 2026-04-16T221511.200.json

###
GET http://localhost:8083/api/register/2/auth-api

<> 2026-04-16T222725.200.json
<> 2026-04-16T222530.200.txt
<> 2026-04-16T221603.200.json


###
PUT http://localhost:8083/api/register/2
Content-Type: application/json

{
  "method": "GET",
  "url": "https://us2.unifier.oraclecloud.com/aesgec",
  "description": "adsdasdasd",
  "pathParams": "/ws/rest/service/v1/admin/bps/AM-CO630%2F620A",
  "queryParams": "",
  "authType": "BEARER",
  "authHeader": "Authorization",
  "authValue": "",
  "username": "",
  "password": "",
  "tokenEndpoint": "eyJ0eXAiOiJEQiJ9.eyJ1c2VybmFtZSI6IiQkcG9zdG1hbiJ9.7566E1DF-24F6-2971-3513-6A087BC863BE4707E384E0842A3A2B4AB06CD5932437"
}

<> 2026-04-16T223648.200.json
<> 2026-04-16T223450.200.json
<> 2026-04-16T223416.405.json