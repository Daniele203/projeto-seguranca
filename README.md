# Gateway de API Seguro para Microserviços

Exercício acadêmico com **2 aplicações Spring Boot**: um gateway (Spring MVC + Security) e um backend de pedidos simplificado.

## Pré-requisitos

- JDK **17 ou superior** (Java 21 também funciona) e Maven 3.9+.
- Portas livres: **8443 (HTTPS do gateway)** e **8081 (HTTP interno, loopback)**.
- Os comandos abaixo funcionam no PowerShell do Windows (ou adapte para Linux/macOS).

## Executar

Abra **dois terminais**, a partir da pasta descompactada:

Terminal 1 — backend:

```powershell
cd backend-mock
mvn spring-boot:run
```

Terminal 2 — gateway:

```powershell
cd api-gateway
mvn spring-boot:run
```

O gateway escuta em **https://localhost:8443**, com certificado **autoassinado somente para desenvolvimento** (`dev-keystore.p12`, senha `changeit`). O backend escuta somente em **127.0.0.1:8081**. Use `curl.exe -k` apenas em ambiente local para ignorar a verificação do certificado autoassinado. Não desative verificação TLS em produção.

## Usuários de demonstração

| Login | Senha (somente desenvolvimento) | Roles |
|---|---|---|
| `usuario` | `usuario123` | `ROLE_USER` |
| `admin` | `admin123` | `ROLE_ADMIN`, `ROLE_USER` |

Em implantação real, não use essas senhas: configure `DEMO_USER_PASSWORD`, `DEMO_ADMIN_PASSWORD`, `JWT_SECRET`, `GATEWAY_SHARED_KEY` e `SSL_KEYSTORE_PASSWORD` como variáveis de ambiente; use certificado real e um gerenciador de segredos. Defina a **mesma** `GATEWAY_SHARED_KEY` em ambos os processos. A chave JWT deve ter pelo menos 32 bytes UTF-8; a chave de exemplo não é segredo.

## Testes manuais (sem JUnit)

**1. Gerar token para usuário:**

```powershell
curl.exe -k -i -X POST https://localhost:8443/api/auth/login -H "Content-Type: application/json" -d '{"username":"usuario","password":"usuario123"}'
```

No **PowerShell**, a passagem do JSON entre aspas pode variar conforme a versão do shell. Uma alternativa confiável é criar um arquivo `login.json` com `{"username":"usuario","password":"usuario123"}` e executar:

```powershell
curl.exe -k -i -X POST https://localhost:8443/api/auth/login -H "Content-Type: application/json" --data-binary '@login.json'
```

Copie o `accessToken` retornado e atribua a `$token`:

```powershell
$token = "COLE_SEU_ACCESS_TOKEN_AQUI"
curl.exe -k -i https://localhost:8443/api/pedidos -H "Authorization: Bearer $token"
```

**2. Testar autenticação e autorização:**

```powershell
# 401: sem token
curl.exe -k -i https://localhost:8443/api/pedidos

# 401: token inválido
curl.exe -k -i https://localhost:8443/api/pedidos -H "Authorization: Bearer token.invalido"

# 403: ROLE_USER não pode entrar na área administrativa
curl.exe -k -i https://localhost:8443/api/admin/status -H "Authorization: Bearer $token"
```

Para testar `200` na rota de administrador, faça login com `admin` / `admin123`, substitua `$token` pelo novo token e use `/api/admin/status`.

**3. Backend sem confiança:**

```powershell
# 403, mesmo tentando forjar apenas o cabeçalho booleano
curl.exe -i http://127.0.0.1:8081/internal/pedidos -H "X-Gateway-Authenticated: true"
```

**4. Rate Limiting:** o gateway permite **20 requisições por minuto por IP**. Após um minuto sem requisições, execute este comando em PowerShell (atenção: todos os endpoints compartilham o mesmo limite):

```powershell
1..25 | ForEach-Object { curl.exe -k -s -o NUL -w "%{http_code}`n" https://localhost:8443/api/pedidos }
```

A partir da 21ª chamada, espera-se `429 Too Many Requests` (as 20 primeiras podem retornar `401` sem token; ainda assim contam para o limite). Em Linux/macOS, substitua `NUL` por `/dev/null`.

**5. Expiração:** token expira em **15 minutos** (`app.jwt.expiration-ms: 900000`). Para testar rapidamente, altere o valor para `3000` no `api-gateway/src/main/resources/application.yml`, reinicie o gateway, gere outro token, aguarde mais de 3 segundos e tente acessar `/api/pedidos`: deve retornar `401`. Depois restaure `900000`.

**6. Emissor:** `JwtService` exige `iss = gateway-seguro`. Para demonstrar a validação, gere um token com a configuração original, altere `app.jwt.issuer` para outro valor e reinicie o gateway com a mesma `JWT_SECRET`; o token anterior deve retornar `401`. Restaure o valor original após o teste.

**7. Assinatura:** altere um caractere do token (sem alterar seus cabeçalhos HTTP) e confira `401`.

**8. HTTPS e headers:**

```powershell
curl.exe -k -i https://localhost:8443/api/pedidos
```

Confira `Strict-Transport-Security` e `Content-Security-Policy` na resposta. A porta **8443 só aceita TLS**; enviar HTTP puro para ela falha. O sistema não expõe porta HTTP para redirecionamento, portanto a proteção adotada é não servir tráfego HTTP no gateway.

Há também um arquivo [`testes.http`](testes.http) com requisições para IDEs; alguns clientes HTTP exigem ativar confiança no certificado de desenvolvimento.

## Decisões técnicas / segurança

- **JWT HS256 (JJWT):** segredo HMAC, assinatura, `exp`, `iss` e `sub` validados. Roles extraídas de claims e convertidas para `GrantedAuthority`; `/api/admin/**` requer `ROLE_ADMIN`.
- **Autenticação:** rota `POST /api/auth/login` com usuários em memória (apenas demonstração). Senhas armazenadas como hash BCrypt em memória durante a execução. Rotas restantes autenticadas.
- **Stateless + CSRF:** sem sessões/cookies para autenticação (somente `Authorization: Bearer`); CSRF desabilitado para esta API. Não é uma proteção genérica para APIs baseadas em cookies.
- **DoS:** Bucket4j, **20 requisições/minuto/IP**, implementado em memória (adequado para apenas uma instância). Utiliza IP efetivo da conexão, não confia em `X-Forwarded-For` arbitrário. Um deployment distribuído exigiria armazenamento compartilhado ou gateway dedicado. O cache de buckets não tem política de expiração, não indicado para tráfego de produção.
- **XSS / transporte:** CSP, HSTS, HTTPS com TLS autoassinado apenas em desenvolvimento. CSP não elimina necessidade de sanitização/escape na apresentação HTML.
- **Erros:** `401`, `403`, `429` padronizados em JSON; controlador global trata erros da aplicação com mensagens genéricas e sem stack trace.
- **Backend isolado:** escuta apenas no endereço local (`127.0.0.1`), verifica `X-Gateway-Authenticated: true` **e** `X-Gateway-Key` com segredo compartilhado. O gateway **não encaminha** headers de confiança recebidos dos clientes; recria-os. O segredo é demonstração, não substitui rede privada e autenticação mTLS/service-to-service em produção.

## Estrutura

```text
projeto-seguranca-microservico/
├── api-gateway/            # HTTPS, JWT, Spring Security, Rate Limiting, roteamento
├── backend-mock/           # GET /internal/pedidos, rejeita acesso sem segredo
├── testes.http             # Cenários manuais
└── README.md
```

## Escopo

Este exemplo é acadêmico, intencionalmente simples. Não implementa persistência de usuários, revogação/refresh tokens, segredo rotativo, certificado confiável por CA, mTLS ou storage distribuído de rate limiting. Também não inclui testes JUnit; o enunciado aceita demonstrações por curl, .http ou Postman.


## Painel web para apresentação

Após iniciar `backend-mock` (8081) e `api-gateway` (8443), abra **https://localhost:8443/** no navegador. Como o certificado é autoassinado, aceite a exceção apenas no ambiente local de desenvolvimento.

O painel foi adicionado em `api-gateway/src/main/resources/static/`:
- `index.html` organiza login, cenários, respostas e arquitetura.
- `css/style.css` contém os estilos responsivos.
- `js/app.js` chama os endpoints reais via `fetch`, guarda o JWT **somente na memória** e mostra códigos HTTP/JSON.

Demonstre nesta ordem: login `usuario/usuario123`, pedidos com token (200), pedidos sem token (401), JWT adulterado (401), rota administrativa com usuário comum (403), login `admin/admin123` e rota administrativa (200), backend sem confiança (403) e Rate Limiting (429). **Execute Rate Limiting por último**, porque o limite de 20 requisições/minuto é compartilhado por IP e também conta recursos estáticos e login. Caso receba 429, aguarde cerca de um minuto.

A rota adicional `GET /api/demo/backend-sem-confianca` é apenas para demonstração acadêmica: solicita pedidos ao backend **sem** os cabeçalhos de confiança, mostrando a rejeição (403) diretamente no navegador. Ela exige um JWT válido e não recebe URLs arbitrárias nem expõe a chave compartilhada. Remova-a em uma implantação real.

O frontend não substitui `testes.http`, usado para testes técnicos. JWTs não são armazenados no localStorage e não são impressos na tela. Os valores padrão de usuários e senhas são somente para o exercício.
