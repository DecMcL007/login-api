# login-api (OAuth2 Authorization Server)

- Spring Boot 3.4.x + Spring Authorization Server
- Port: 8082

## OAuth2 Endpoints
- Discovery: http://localhost:8082/.well-known/openid-configuration
- JWKS:      http://localhost:8082/oauth2/jwks
- Token:     http://localhost:8082/oauth2/token
- Authorize: http://localhost:8082/oauth2/authorize

## Smoke test (client credentials)
curl -u golf-client:golf-secret \
  -X POST http://localhost:8082/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials&scope=courses.read"

#TODO.  I'm using port 80 at the minute but i need to go back to 8082
