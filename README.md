# Control práctico de arquitectura en AWS

**Duración:** 90 minutos · **Modalidad:** individual, con todo el material, internet y asistentes de IA permitidos · **Entrega:** un *pull request* con tu repositorio

Este control es de entrenamiento y no cuenta para la nota final. Sirve para que te familiarices con el formato del examen práctico.

Lo que se evalúa no es que sepas programar la aplicación (viene hecha y no tienes que tocar su código), sino que sepas **configurar la arquitectura** que la hace funcionar en AWS. En concreto, el servidor y su cortafuegos, la identidad con Cognito y los secretos con Secrets Manager.

## Cómo se reparten los puntos

La nota máxima es de **12 puntos**: 10 puntos (5 de despliegue y 5 de preguntas) más 2 puntos de dos preguntas adicionales.

| Parte | Puntos | Cómo se corrige |
|---|---|---|
| **Despliegue** (las 6 tareas) | **5** | Automáticamente, comprobando tu despliegue mediante un script que correrá el profesor |
| **Preguntas abiertas** (8 preguntas) | **5** (más **2** adicionales) | A mano, por el profesor. **Se responden en el cuestionario de Moodle**. |
| **Total** | **12** (10 + 2 adicionales) | |

## La aplicación

Una aplicación de anuncios mínima. Cualquier usuario autenticado puede leer anuncios, y el administrador es el único que puede publicarlos.

| Componente | Qué hace |
|---|---|
| `back/` | API en Spring Boot con tres endpoints. `GET /api/whoami` informa de la identidad, el grupo y la huella de la contraseña del secreto (para no mostrar la contraseña en texto plano en Internet, como veremos más adelante). `GET` y `POST /api/mensajes` listan y publican mensajes, respectivamente. |
| `front/` | Una página estática que permite iniciar sesión, listar mensajes y publicar. |
| `aws-scripts/` | Scripts de despliegue. En la cabecera de cada uno hay **huecos `TODO`** y valores `<...>` que debes rellenar a mano. Cada script termina en el primer error y te dice qué falta.|

La arquitectura que debes dejar desplegada es esta (la misma que hemos visto en clase):

```
  Navegador ──:80──▶ [ Grupo de seguridad ] ──▶ EC2 (Ubuntu)
      │                                           ├─ Nginx  :80   sirve el front y reenvía /api/
      │                                           └─ Spring :8080 solo escucha a Nginx
      │                                                 │   
      │                                                 │ perfil de Labs
      │                                                 ▼
      ▼                                          Secrets Manager
  Cognito (User Pool + App Client + grupo)
```

## Antes de empezar

Necesitas la AWS CLI con las credenciales del Learner Lab ya configuradas, `curl`, `ssh`, Java 21 y Maven.

**Todos los comandos de este documento son de bash** (los scripts, los *checkpoints*, `printf`, `curl`...). En macOS y Linux sirve la terminal normal. En Windows **no uses PowerShell ni CMD**, ya que los scripts y los comandos no funcionarán. Usa **Git Bash** (viene con Git para Windows) o WSL2 (Ubuntu), y ejecuta todo desde esa terminal. Clona el repositorio **en tu carpeta de usuario** (`~/`), no en OneDrive, Dropbox, una unidad distinta de `C:` ni en `/mnt/c/...` de WSL2, ya que en esos sitios `chmod` no tiene efecto y `ssh` rechazaría la clave. Comprueba que `aws`, `java` y `mvn` responden en esa terminal.

Coloca la clave SSH del laboratorio en `ssh-key/labsuser.pem`, pero no la subas al repositorio (ahora mismo, está listada en ``.gitignore``). Ejecuta `chmod 400 ssh-key/labsuser.pem` para que `ssh` la acepte.

1. Haz un *fork* de este repositorio y trabaja directamente en su rama `main`.
2. Tu **EXAM_ID** es un numero aleatorio de 6 dígitos, que tu escoges.
3. Ejecuta el script siguiente para generar una serie de parámetros individuales:

```bash
bash params.sh <EXAM_ID>
```

El script `params.sh` crea el fichero `params.txt`. Este fichero contiene varios parámetros, como el nombre de tu secreto, la contraseña del usuario de la aplicación, etc. Los valores que necesite cada script los **debes rellenar a mano**, en su lugar correspondiente. `params.txt` no se debe subir al repositorio (ahora mismo, está listado en ``.gitignore``). `params.sh` también escribe tu EXAM_ID en `entrega.txt`, que es un fichero que **SI SE ENTREGA**.

## Tareas (5 puntos)

Los scripts contienen huecos `TODO` relacionados con las tareas que se indican a continuación. Debes rellenar los valores adecuados. 

### Tarea 1. Servidor y cortafuegos · 1,5 puntos

Completa `aws-scripts/ec2.sh`.

- La instancia usa Ubuntu Server 26.04 LTS (la AMI ya viene resuelta en `ec2.sh`) y un **tipo de instancia que eliges tú**. Debe ser de la familia t2 y tener 1 vCPU y 1 GiB de RAM.
- La instancia usa el **perfil de instancia del laboratorio** para poder leer el secreto sin credenciales en el código más adelante. 
- Desde Internet **solo se debe poder acceder al puerto 80**. El 8080 de Spring Boot y el 443 no deben estar accesibles.
- Los scripts necesitan SSH para desplegar. El puerto 22 (SSH) **solo debe poder abrirse desde tu IP pública**, con una regla `/32`.

Cuando `ec2.sh` termine, **identifica la IP pública de la instancia** y **escríbela a mano en `entrega.txt`**, en el campo `IP`. El `EXAM_ID` ya lo ha escrito `params.sh` en ese fichero.

**Checkpoint.** Comprueba que puedes conectarte a la instancia por SSH. Si no recuerdas cómo hacerlo, búscalo en la consola web de EC2.

**Pregunta (0,5 puntos).** ¿Qué papel cumple el perfil de instancia? Si se lo quitaras a la instancia EC2, ¿qué parte del despliegue dejaría de funcionar y cómo lo notarías desde fuera? **Responde a esta pregunta al final del examen**.

### Tarea 2. Despliegue de la instancia · 0,5 puntos

Ejecuta `nginx.sh`, `back.sh` y `front.sh`, en ese orden. Los tres deberían terminar **sin errores**. Antes de ejecutar cada uno, copia a mano en su cabecera los valores que pide (los marcados con `<...>`).

| Script | Valor | De dónde sale |
|---|---|---|
| `nginx.sh` | `PUBLIC_IP` | La IP pública de la instancia, la que escribiste en `entrega.txt` |
| `back.sh` | `PUBLIC_IP` | La misma IP pública |
| `back.sh` | `SECRET_NAME` | `params.txt`. Es solo el nombre del secreto, que todavía no existe en esta tarea; la aplicación arranca igualmente |
| `back.sh` | `EXAM_ID` | `params.txt`. Es el mismo valor que devuelve `/api/whoami` |
| `front.sh` | `PUBLIC_IP` | La misma IP pública |

**Checkpoint.** Cuando terminen, `http://<PUBLIC_IP>/api/whoami` debe responder un JSON que incluya tu `EXAM_ID`. Si no responde, no sigas: el problema está en esta tarea. `http://<PUBLIC_IP>/` debe mostrar el front.

**Pregunta (0,5 puntos).** ¿Por qué `http://<PUBLIC_IP>/api/whoami` se puede ver sin iniciar sesión, pero `http://<PUBLIC_IP>/api/mensajes` no?

### Tarea 3. Identidad con Cognito · 0,75 puntos

Ejecuta `aws-scripts/cognito.sh`. Al ejecutarlo debe crear en Cognito un *User Pool* con un cliente que permita iniciar sesión con usuario y contraseña, y el usuario del examen. También genera `back/src/main/resources/cognito.properties` y `front/config.js`, que el back y el front usarán cuando los vuelvas a desplegar.

Antes de ejecutarlo, copia a mano en su cabecera los valores que pide.

| Valor | De dónde sale |
|---|---|
| `EXAM_USER` | `params.txt` |
| `EXAM_USER_PASS` | `params.txt` |

Después, vuelve a ejecutar `back.sh` y `front.sh` para que el back y el front recojan la configuración de Cognito.

**Checkpoint.** Obtén un *access token* del usuario del examen y compruébalo contra el back.

1. Pídele el token a Cognito con este comando, sustituyendo los tres valores entre `<...>`:

   ```bash
   aws cognito-idp initiate-auth \
     --client-id <CLIENT_ID> \
     --auth-flow USER_PASSWORD_AUTH \
     --auth-parameters USERNAME=<EXAM_USER>,PASSWORD=<EXAM_USER_PASS> \
     --query 'AuthenticationResult.AccessToken' \
     --output text
   ```

2. Llama al back con ese token:

   ```bash
   curl -s -H "Authorization: Bearer <TOKEN>" http://<PUBLIC_IP>/api/whoami
   ```

El campo `usuario` debe mostrar tu usuario. El campo `grupos` seguirá vacío hasta la tarea siguiente. Si no es así, no sigas, porque el problema está en esta tarea:

- Si `usuario` sale `null`, la llamada no ha llevado un token válido (no te has autenticado). La aplicación solo conoce tu usuario por el token que envías en la cabecera `Authorization`; si abres `http://<PUBLIC_IP>/api/whoami` en el navegador, no envía ningún token y `usuario` siempre será `null`. Revisa el paso 1 (que el comando haya devuelto el token y que hayas puesto bien `<CLIENT_ID>`, `<EXAM_USER>` y `<EXAM_USER_PASS>`) y que lo hayas pegado en el paso 2.
- Si recibes un **401**, el token no es válido para el back. Comprueba que has vuelto a ejecutar `back.sh` tras `cognito.sh`.

**Pregunta (1,5 puntos).** Decodifica el contenido del token que acabas de obtener. Cada dato del contenido se llama *claim*. **Pega en Moodle el claim (nombre y valor) que contiene cada uno de estos cuatro datos**: el identificador único de tu usuario en el *pool*, tu nombre de usuario, quién ha emitido el token y qué tipo de token es. No pegues el token original, que es una credencial válida durante una hora.

### Tarea 4. Grupo `admin` en la consola web · 0,75 puntos

Ningún script crea el grupo `admin`: hazlo tú en la **consola web de Cognito**.

1. Abre tu *User Pool* (`anuncios-pool`) y crea un grupo llamado exactamente `admin`.
2. Añade al grupo el usuario del examen.

**Checkpoint.** Pide un *access token* nuevo del usuario del examen (con el mismo comando del *checkpoint* de la Tarea 3) y llama con él a `http://<PUBLIC_IP>/api/whoami`. El campo `grupos` debe mostrar `admin`. Si no es así, no sigas: el problema está en esta tarea. Un token anterior a añadir al usuario al grupo no lo muestra.

**Pregunta (0,75 puntos).** Prueba a publicar un mensaje con este comando, primero con el token de la Tarea 3 (anterior al grupo) y después con el token nuevo. ¿Por qué ahora el `POST` funciona?

```bash
curl -s -o /dev/null -w '%{http_code}\n' -X POST \
  -H "Authorization: Bearer <TOKEN>" -H 'Content-Type: application/json' \
  -d '{"texto":"hola"}' http://<PUBLIC_IP>/api/mensajes
```

### Tarea 5. Secretos · 0,75 puntos

Completa `aws-scripts/secrets.sh`.

- Existe en Secrets Manager un secreto con el nombre `SECRET_NAME` de `params.txt` y con los campos que espera `application.properties`.
- `GET /api/whoami` devuelve en `huella` el SHA-256 de la contraseña que la aplicación ha leído de tu secreto. Solo puede coincidir si la aplicación ha leído tu secreto y, como un hash no permite recuperar la contraseña, **la respuesta no muestra la contraseña en texto plano**.

Después, vuelve a ejecutar `back.sh` para que el back lea el secreto.

**Checkpoint.** Calcula la huella de tu contraseña y compárala con la que devuelve la aplicación. Las dos deben ser iguales.

```bash
curl -s http://<PUBLIC_IP>/api/whoami
printf '%s' "<DB_PASS>" | shasum -a 256
```

(`printf '%s'` evita añadir un salto de línea, que cambiaría el resultado. En Linux, usa `sha256sum` en lugar de `shasum -a 256`.)

**Pregunta (0,75 puntos).** ¿De dónde lee Spring Boot la contraseña de la base de datos? Indica qué lee exactamente y de dónde.

**Pregunta adicional (+1 punto).** El back necesita saber qué secreto debe leer. ¿Cómo conoce el nombre del secreto? Indica qué dato se le pasa al servicio del back en la instancia, cómo se lo pasa y quién lo hace.

### Tarea 6. Cambio de contraseña · 0,75 puntos

La contraseña de la base de datos debe pasar a ser `DB_PASS_NUEVA` (la de `params.txt`). Esa contraseña existe en **dos sitios**. Los dos deben quedar con el valor nuevo, y la aplicación debe seguir funcionando con ella.

1. **En Secrets Manager.** Cambia a mano, en la consola web o con la CLI, el campo `password` del secreto `SECRET_NAME`. Deja sin tocar los otros campos del secreto (`username` y `dbname`).
2. **En la base de datos.** Cambia la contraseña del usuario de la base de datos desde la consola web de H2. Con la sesión abierta en la consola, escribe esta sentencia en el cuadro de texto y pulsa **Run**:

   ```sql
   ALTER USER admin SET PASSWORD '<DB_PASS_NUEVA>';
   ```

   Sustituye `<DB_PASS_NUEVA>` por el valor de `params.txt`, y déjalo entre comillas simples. A partir de ese momento, la base de datos solo acepta la contraseña nueva para el usuario `admin`.

La consola H2 forma parte de la aplicación, pero **no está publicada**: Nginx no la sirve y el puerto 8080 no está abierto. Se accede por un túnel SSH, que deja su puerto local 8082 conectado al 8080 de la instancia:

```bash
ssh -i ssh-key/labsuser.pem -L 8082:127.0.0.1:8080 ubuntu@<PUBLIC_IP>
```

Con el túnel abierto, entra en `http://localhost:8082/h2-console` con estos datos de conexión.

| Campo | Valor |
|---|---|
| JDBC URL | `jdbc:h2:file:/var/lib/anuncios/anuncios-admin` |
| Usuario | `admin` |
| Contraseña | la que tenga ahora la base de datos |

**Checkpoint.** La aplicación debe seguir respondiendo en `http://<PUBLIC_IP>/api/whoami`, y su `huella` debe ser el SHA-256 de tu contraseña nueva, calculado como en el *checkpoint* de la Tarea 5 pero con `<DB_PASS_NUEVA>`.

**Pregunta (1 punto).** Después de cambiar la contraseña en el secreto y en la base de datos, consulta `http://<PUBLIC_IP>/api/whoami`. ¿Por qué la `huella` no coincide con la de tu contraseña nueva?

**Pregunta adicional (+1 punto).** ¿Cómo se puede conseguir que la `huella` sea correcta?

## Cómo entregar

1. Comprueba que `entrega.txt` tiene los dos valores correctos. El EXAM_ID lo escribió `params.sh`; la IP la escribiste tú.
2. Responde a las preguntas abiertas en el cuestionario de Moodle (no en el repositorio).
3. Haz *commit* de los cambios realizados, así como de `entrega.txt`.
4. Abre un *pull request* desde la rama `main` de tu *fork* contra la rama `main` del repositorio original y rellena la plantilla.

El *pull request* es parte de la evidencia. Su fecha y su contenido nos permiten ver qué hiciste y cuándo.

## Recuerda

- No subas `params.txt` ni la clave SSH (`.pem`). Ya están en `.gitignore`.
- Si algo falla y necesitas empezar de nuevo, **borra los recursos desde la consola web de AWS** (no hay scripts para ello). Los reconocerás por su nombre (`anuncios-ec2` para la instancia, `anuncios-sg`, `anuncios-pool`, y el secreto con tu EXAM_ID) o porque su IP pública es la que escribiste en `entrega.txt`.
- Mientras no se corrija, no debes borrar los componentes (ec2, grupo de seguridad, ip estática, pool de Cognito) que has creado.
- Si cambias de red durante el control (por ejemplo, de ethernet a eduroam o de una Wi-Fi a otra), tu IP pública cambia y `ssh` dejará de conectar (se queda esperando hasta que expira). Añade tu IP nueva a la regla del puerto 22 del grupo de seguridad desde la consola web, con origen `<tu IP>/32`. Si al final dejas varias, deja solo la tuya.
