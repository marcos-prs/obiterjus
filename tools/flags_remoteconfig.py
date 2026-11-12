#!/usr/bin/env python3
"""Acrescenta os flags do push ao Remote Config sem apagar o que já existe.

**O problema que este script existe para evitar:** publicar um template de
Remote Config **substitui o template inteiro**. Um arquivo contendo só os dois
flags novos apagaria `djen_base_url`, `datajud_api_key`, `djen_token` e todo o
resto — e o app cairia nos fallbacks compilados.

Então aqui o caminho é sempre: buscar o template que está no ar, acrescentar
apenas o que falta, e só então publicar.

Por padrão não publica nada: mostra o que mudaria. Use --aplicar para valer.

    python3 tools/flags_remoteconfig.py                   # mostra o que mudaria
    python3 tools/flags_remoteconfig.py --aplicar         # cria os flags em false
    python3 tools/flags_remoteconfig.py --ligar push_diario_enabled --aplicar

O template baixado fica fora do git (ver .gitignore): num projeto com valores
publicados ele conteria a APIKey do DataJud e o token do DJEN.

Quem empurra o push **não** mora aqui: é o serviço `Obiter-Push`, repositório e
container próprios na mesma VM. Este script cuida só dos flags do lado do app.

São dois interruptores com nomes parecidos: `push_diario_enabled` aqui manda o
aparelho se inscrever no tópico; `PUSH_DIARIO_ENABLED` no `.env` do Obiter-Push
manda o servidor enviar. Com só um ligado, nada acontece e não há erro.

Alternativa sem script: criar os dois parâmetros à mão no console do Firebase,
como Booleano, ambos em `false`.
"""

from __future__ import annotations

import argparse
import json
import pathlib
import shutil
import subprocess
import sys

# Nascem em false de propósito. O que liga cada um está no desenho:
# docs/plans/2026-11-11-push-das-7h.md
FLAGS = {
    "push_diario_enabled": (
        "Fase 1 do push diário: o aparelho se inscreve no tópico do FCM da sua "
        "hora e fuso. Ligar quando o Obiter-Push estiver no ar na VM."
    ),
    "push_pre_checagem_disponivel": (
        "Expõe no Perfil o opt-in da Fase 2, em que o servidor passa a guardar "
        "OAB e nome e a consultar o DJEN pelo advogado. Só ligar depois de a "
        "política de privacidade registrar isso."
    ),
}

# O `firebase deploy` lê o firebase.json da raiz, e o caminho do template lá é
# relativo a ela — então tudo acontece ancorado na raiz, não no diretório deste
# script.
RAIZ = pathlib.Path(__file__).resolve().parents[1]
TEMPLATE = RAIZ / "remoteconfig.template.json"


def firebase_cli() -> str:
    """Caminho completo do CLI.

    Resolvido com `which` porque no Windows o `firebase` é um `.cmd`, e o
    subprocess não o encontra pelo PATH como encontraria um executável.
    """
    caminho = shutil.which("firebase")
    if not caminho:
        sys.exit(
            "firebase CLI não encontrado no PATH.\n"
            "Instale com: npm install -g firebase-tools"
        )
    return caminho


def rodar(*args: str) -> str:
    cmd = (firebase_cli(), *args)
    resultado = subprocess.run(cmd, capture_output=True, text=True, cwd=RAIZ)
    if resultado.returncode != 0:
        sys.exit(
            f"Falhou: firebase {' '.join(args)}\n"
            f"{resultado.stderr.strip() or resultado.stdout.strip()}"
        )
    return resultado.stdout


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--projeto", default="obiterjus")
    parser.add_argument(
        "--aplicar",
        action="store_true",
        help="Publica de verdade. Sem isso, só mostra o que mudaria.",
    )
    parser.add_argument(
        "--ligar",
        action="append",
        default=[],
        choices=sorted(FLAGS),
        help="Põe o flag em true. Repetível. Exige --aplicar para valer.",
    )
    args = parser.parse_args(argv)

    print(f"Buscando o template que está no ar em {args.projeto}...")
    rodar("remoteconfig:get", "--project", args.projeto, "-o", str(TEMPLATE))

    template = json.loads(TEMPLATE.read_text(encoding="utf-8"))
    parametros = template.setdefault("parameters", {})

    existentes = sorted(parametros)
    print(f"Template no ar tem {len(existentes)} parâmetro(s): {', '.join(existentes)}")

    mudancas = []

    for chave, descricao in FLAGS.items():
        if chave not in parametros:
            parametros[chave] = {
                "defaultValue": {"value": "false"},
                "valueType": "BOOLEAN",
                "description": descricao,
            }
            mudancas.append(f"  + {chave} = false (BOOLEAN, criado)")

    for chave in args.ligar:
        atual = parametros[chave]["defaultValue"].get("value")
        if atual == "true":
            print(f"  = {chave} já está em true")
            continue
        parametros[chave]["defaultValue"]["value"] = "true"
        mudancas.append(f"  ~ {chave}: {atual} -> true")

    if not mudancas:
        print("\nNada a mudar: os flags existem e já estão como você pediu.")
        return 0

    print("\n".join(mudancas))

    TEMPLATE.write_text(
        json.dumps(template, indent=2, ensure_ascii=False) + "\n", encoding="utf-8"
    )

    if not args.aplicar:
        print(
            f"\nNada publicado. O template mesclado está em {TEMPLATE} "
            "(fora do git).\nPara publicar: python3 flags_remoteconfig.py --aplicar"
        )
        return 0

    print("\nPublicando...")
    rodar("deploy", "--only", "remoteconfig", "--project", args.projeto)
    print("Publicado.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
