"use strict";
// Painel demonstrativo: não usa localStorage, sessionStorage nem cookies para guardar JWT.
const state = { token: null, username: null, busy: false };
const $ = (id) => document.getElementById(id);
const base = window.location.origin;
const time = () => performance.now();

function setBusy(value) {
  state.busy = value;
  document.querySelectorAll(".test-card, #loginButton").forEach(b => b.disabled = value);
}
function badge(element, value, kind = "neutral") {
  element.className = "pill pill-" + kind;
  element.textContent = value;
}
function setConnection(ok) {
  $("connectionDot").className = "status-dot " + (ok ? "" : "error");
  $("connectionLabel").textContent = ok ? "Gateway respondeu" : "Falha de conexão";
}
function updateAuth() {
  badge($("authBadge"), state.token ? `Autenticado: ${state.username}` : "Não autenticado", state.token ? "ok" : "neutral");
  $("tokenInfo").textContent = state.token ? `JWT disponível • ${state.token.length} caracteres • Bearer` : "Nenhum token ativo";
  $("logoutButton").disabled = !state.token;
}
function tamper(token) {
  if (!token) return "token.invalido.aqui";
  const parts = token.split(".");
  if (parts.length !== 3 || !parts[2]) return "token.invalido.aqui";
  // Modifica o começo da assinatura, não o último caractere sujeito à ambiguidade Base64URL.
  parts[2] = (parts[2][0] === "A" ? "B" : "A") + parts[2].slice(1);
  return parts.join(".");
}
async function request(path, {method = "GET", token = null, body = undefined} = {}) {
  const headers = {"Accept": "application/json"};
  if (token) headers.Authorization = `Bearer ${token}`;
  if (body !== undefined) headers["Content-Type"] = "application/json";
  const start = time();
  try {
    const response = await fetch(path, {
      method, headers, body: body === undefined ? undefined : JSON.stringify(body),
      cache: "no-store", credentials: "omit"
    });
    const raw = await response.text();
    let data;
    try { data = JSON.parse(raw); } catch (_) { data = raw || "(resposta vazia)"; }
    return { status: response.status, data, duration: Math.round(time() - start), method, path, connected: true };
  } catch (e) {
    return {status: null, data: {erro: "Não foi possível conectar ao Gateway. Confirme se o Spring Boot está iniciado e se o certificado HTTPS foi aceito no navegador."},duration: Math.round(time()-start),method,path,connected:false};
  }
}
function showResult(result, description = "Resposta real do servidor") {
  $("responseDescription").textContent = description;
  badge($("httpStatus"), result.status === null ? "Sem conexão" : `HTTP ${result.status}`, result.status >= 200 && result.status < 300 ? "ok" : "error");
  $("requestMethod").textContent = result.method || "GET";
  $("requestUrl").textContent = result.path || "/";
  $("requestDuration").textContent = `${result.duration} ms`;
  $("responseBody").textContent = JSON.stringify(result.data, null, 2);
  $("rateSummary").hidden = true;
  setConnection(result.connected);
  $("resposta").scrollIntoView({behavior:"smooth",block:"start"});
}
async function login(event) {
  event.preventDefault();
  if (state.busy) return;
  state.token = null;state.username = null;updateAuth();
  setBusy(true);
  const username = $("username").value;
  const res = await request("/api/auth/login", {method:"POST",body:{username,password:$("password").value}});
  if (res.status === 200 && typeof res.data?.accessToken === "string") {
    state.token = res.data.accessToken;state.username = username;updateAuth();
    // Nunca exibe o valor integral do token no console ou no painel.
    showResult({...res,data:{tokenType:res.data.tokenType,accessToken:"[TOKEN OCULTO NO PAINEL]",length:state.token.length}}, "Login realizado. O JWT foi recebido e mantido apenas na memória desta página.");
  } else {
    showResult(res, "Tentativa de autenticação.");
  }
  setBusy(false);
}
async function runTest(type) {
  if (state.busy) return;
  setBusy(true);
  try {
    let res, description;
    switch(type) {
      case "pedidos":
        res = await request("/api/pedidos", {token:state.token});
        description = state.token ? "Consulta de pedidos com JWT." : "Sem login: a consulta deverá retornar 401.";
        break;
      case "semToken":
        res = await request("/api/pedidos");description="Requisição sem cabeçalho Authorization: esperado 401.";break;
      case "tokenInvalido":
        res = await request("/api/pedidos",{token:tamper(state.token)});
        description="Assinatura do token alterada (ou token malformado): esperado 401.";break;
      case "admin":
        res = await request("/api/admin/status",{token:state.token});
        description=state.username === "admin" ? "ADMIN autenticado: esperado 200." : "Usuário comum: esperado 403 (requer login com usuario).";
        break;
      case "backend":
        res = await request("/api/demo/backend-sem-confianca",{token:state.token});
        description="O Gateway consulta o backend intencionalmente sem cabeçalhos de confiança (apenas demonstração): esperado 403.";
        break;
      case "rate":
        if (!window.confirm("Serão feitas até 25 requisições em sequência. Isso consumirá o limite por IP de todas as rotas do Gateway. Deseja continuar?")) return;
        const statuses = [];
        for(let i=0;i<25;i++) {
          res=await request("/api/pedidos");
          statuses.push(res.status === null?"falha":res.status);
          if(!res.connected || res.status===429) break;
        }
        description=`Rate Limiting: ${statuses.length} chamadas; observe a resposta 429. Se o limite já estava parcialmente consumido, o 429 chega antes.`;
        showResult(res,description);
        $("rateSummary").hidden=false;
        $("rateSummary").textContent=`Códigos HTTP observados: ${statuses.join(" • ")}. O limitador é compartilhado por IP. Aguarde 60 segundos antes de outros testes.`;
        return;
      default:return;
    }
    showResult(res,description);
  } finally {setBusy(false);}
}
$("loginForm").addEventListener("submit",login);
$("logoutButton").addEventListener("click",()=>{state.token=null;state.username=null;updateAuth();$("responseDescription").textContent="Token removido da memória da página.";});
$("username").addEventListener("change",()=>{$("password").value=$("username").value==="admin"?"admin123":"usuario123";});
document.querySelectorAll("[data-test]").forEach(button=>button.addEventListener("click",()=>runTest(button.dataset.test)));
updateAuth();
