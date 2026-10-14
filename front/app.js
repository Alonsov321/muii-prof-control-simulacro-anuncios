// Front mínimo. Nginx sirve esta página y reenvía /api/ al back, así que no hace falta CORS.
// config.js lo genera cognito.sh con la región y el ClientId del User Pool.
let accessToken = null;

const $ = (id) => document.getElementById(id);

async function cargar() {
  // Los mensajes requieren un access token: sin sesión no se piden.
  if (!accessToken) {
    $('lista').innerHTML = '<li>Inicia sesión para ver los mensajes.</li>';
    return;
  }
  const respuesta = await fetch('/api/mensajes', {
    headers: { Authorization: `Bearer ${accessToken}` },
  });
  if (!respuesta.ok) {
    $('estado').textContent = `Error ${respuesta.status}`;
    return;
  }
  const mensajes = await respuesta.json();
  $('lista').innerHTML = '';
  for (const m of mensajes) {
    const li = document.createElement('li');
    li.textContent = m.texto;
    const meta = document.createElement('div');
    meta.className = 'meta';
    meta.textContent = `${m.autor ?? 'anónimo'} · ${m.fecha ?? ''}`;
    li.appendChild(meta);
    $('lista').appendChild(li);
  }
}

// USER_PASSWORD_AUTH contra la API pública de Cognito. Devuelve ID token y access token.
async function entrar() {
  if (!window.COGNITO_CONFIG) {
    $('estado').textContent = 'Falta config.js (lo genera cognito.sh)';
    return;
  }
  const respuesta = await fetch(`https://cognito-idp.${window.COGNITO_CONFIG.region}.amazonaws.com/`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-amz-json-1.1',
      'X-Amz-Target': 'AWSCognitoIdentityProviderService.InitiateAuth',
    },
    body: JSON.stringify({
      AuthFlow: 'USER_PASSWORD_AUTH',
      ClientId: window.COGNITO_CONFIG.clientId,
      AuthParameters: { USERNAME: $('usuario').value, PASSWORD: $('contrasena').value },
    }),
  });
  const datos = await respuesta.json();
  if (!datos.AuthenticationResult) {
    $('estado').textContent = datos.message || 'No se pudo entrar';
    return;
  }
  accessToken = datos.AuthenticationResult.AccessToken;
  $('estado').textContent = 'Sesión iniciada';
  $('nuevo').hidden = false;
  await cargar();
}

async function enviar() {
  const respuesta = await fetch('/api/mensajes', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${accessToken}` },
    body: JSON.stringify({ texto: $('texto').value }),
  });
  if (respuesta.status === 201) {
    $('texto').value = '';
    await cargar();
  } else {
    $('estado').textContent = respuesta.status === 403
      ? 'Tu usuario no puede publicar (¿pertenece al grupo admin?)'
      : `Error ${respuesta.status}`;
  }
}

$('entrar').addEventListener('click', entrar);
$('enviar').addEventListener('click', enviar);
cargar();
