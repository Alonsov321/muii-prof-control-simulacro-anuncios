#!/bin/bash

# Deriva los parámetros individuales del control a partir del EXAM_ID.
# Es determinista, así que el profesor calcula los mismos valores al corregir y no hace
# falta intercambiar secretos.
#
# Uso (genera params.txt):
#   bash params.sh <EXAM_ID>
#
# Otros scripts lo cargan con source para usar las funciones.

sha() {
    if command -v sha256sum >/dev/null 2>&1; then
        sha256sum
    else
        shasum -a 256
    fi | cut -d' ' -f1
}

# hash_corto <texto> <n>  -> primeros n caracteres hexadecimales del SHA-256
hash_corto() {
    printf '%s' "$1" | sha | cut -c1-"$2"
}

# huella <contraseña>  -> lo mismo que devuelve /api/whoami
huella() {
    printf '%s' "$1" | sha
}

# derivar_parametros <EXAM_ID>
derivar_parametros() {
    EXAM_ID="$1"
    EXAM_USER="corrector"
    EXAM_USER_PASS="Corr-$(hash_corto "clave:$1" 8)-A1"
    SECRET_NAME="prod/anuncios/$1"
    DB_PASS="$(hash_corto "secreto:$1" 24)"
    DB_PASS_NUEVA="$(hash_corto "secreto2:$1" 24)"
}

# Solo como script: escribe params.txt junto a este fichero.
if [ "${BASH_SOURCE[0]}" = "$0" ]; then
    set -e
    DIR="$(cd -- "$(dirname -- "$0")" && pwd)"
    ENV_FILE="$DIR/params.txt"

    if [ -z "${1:-}" ]; then
        echo "Uso: $0 <EXAM_ID>"
        exit 1
    fi
    if ! [[ "$1" =~ ^[A-Za-z0-9-]{3,30}$ ]]; then
        echo "Error: EXAM_ID solo admite letras, números y guiones (3 a 30 caracteres)."
        exit 1
    fi

    derivar_parametros "$1"
    cat > "$ENV_FILE" <<ENV
EXAM_ID=$EXAM_ID
EXAM_USER=$EXAM_USER
EXAM_USER_PASS=$EXAM_USER_PASS
SECRET_NAME=$SECRET_NAME
DB_PASS=$DB_PASS
DB_PASS_NUEVA=$DB_PASS_NUEVA
ENV
    echo "Parámetros guardados en params.txt (no se sube al repositorio)."
    echo "  Usuario / contraseña . $EXAM_USER / $EXAM_USER_PASS"
    echo "  Nombre del secreto ... $SECRET_NAME"
    echo "  Contraseña de la BD .. $DB_PASS"

    # entrega.txt: el EXAM_ID lo escribe este script. La IP la escribe el estudiante a mano
    # (si ya estaban escritas, no se tocan).
    ENTREGA="$DIR/entrega.txt"
    if [ -f "$ENTREGA" ] && grep -q '^EXAM_ID=' "$ENTREGA"; then
        sed "s/^EXAM_ID=.*/EXAM_ID=$EXAM_ID/" "$ENTREGA" > "$ENTREGA.tmp"
        mv "$ENTREGA.tmp" "$ENTREGA"
    else
        cat > "$ENTREGA" <<ENTREGA_TXT
# EXAM_ID lo escribe bash params.sh. Escribe tú a mano la IP. Es lo que lee el corrector.
#   IP  la IP pública de la instancia (CLI o consola web de EC2)
EXAM_ID=$EXAM_ID
IP=
ENTREGA_TXT
    fi
    echo "  entrega.txt .......... EXAM_ID escrito (te falta la IP)"
fi
