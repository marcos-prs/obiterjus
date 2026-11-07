# -*- coding: utf-8 -*-
"""Gera o conteúdo das páginas jurídicas do site ObiterJus.

A fonte da verdade é o markdown versionado em `docs/juridico/`. Este script o
converte e escreve `conteudo.ts` dentro do site Next.js, que é publicado no
Firebase Hosting (obiterjus.web.app). Os dois nunca divergem porque a página do
site não tem texto próprio: só renderiza o que sai daqui.

Uso:  python web/build.py [--site CAMINHO]
"""
import argparse
import io
import json
import os
import re
import sys

import markdown

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
WEB = os.path.join(RAIZ, 'web')

SITE_PADRAO = os.path.join(
    os.path.expanduser('~'), 'Xablau Iniciative', 'APPS', 'ObiterJus', 'site',
)

PAGINAS = [
    {
        'fonte': 'docs/juridico/politica-de-privacidade.md',
        'rota': 'privacidade',
        'componente': 'Privacidade',
        'titulo': 'Política de Privacidade | ObiterJus',
        'descricao': 'Como o ObiterJus trata dados pessoais: o que coleta, onde guarda, '
                     'com quem compartilha e como exercer seus direitos.',
        'irmas': [('/termos', 'Termos de Uso'), ('/exclusao-de-conta', 'Excluir conta')],
    },
    {
        'fonte': 'docs/juridico/termos-de-uso.md',
        'rota': 'termos',
        'componente': 'Termos',
        'titulo': 'Termos e Condições de Uso | ObiterJus',
        'descricao': 'Condições de uso do aplicativo ObiterJus, ferramenta de apoio ao '
                     'trabalho do advogado.',
        'irmas': [('/privacidade', 'Política de Privacidade'), ('/exclusao-de-conta', 'Excluir conta')],
    },
]

# A página é gerada junto com o conteúdo de propósito: assim ela nunca existe no
# site importando um `conteudo.ts` que o gate de dados impediu de nascer — o que
# deixaria o `npm run build` quebrado.
MOLDE_PAGINA = '''// GERADO por web/build.py no repositório do aplicativo. Não edite aqui.
import type {{ Metadata }} from "next";
import styles from "../juridico.module.css";
import {{ HTML }} from "./conteudo";

export const metadata: Metadata = {{
  title: {titulo},
  description: {descricao},
}};

export default function {componente}() {{
  return (
    <main className={{styles.pagina}}>
      <div className={{styles.container}}>
        <a href="/" className={{styles.voltar}}>
          &larr; Voltar para o início
        </a>

        <div
          className={{styles.corpo}}
          dangerouslySetInnerHTML={{{{ __html: HTML }}}}
        />

        <div className={{styles.navRodape}}>
{irmas}
        </div>
      </div>
    </main>
  );
}}
'''


def carregar_dados():
    caminho = os.path.join(WEB, 'dados-do-controlador.json')
    with io.open(caminho, encoding='utf-8') as fh:
        bruto = json.load(fh)
    dados = {k: v for k, v in bruto.items() if not k.startswith('_')}
    vazios = sorted(k for k, v in dados.items() if not str(v).strip())
    if vazios:
        print('Faltam dados do controlador em web/dados-do-controlador.json:\n')
        for k in vazios:
            print('  - {}: {}'.format(k, bruto.get('_' + k, '')))
        print('\nPreencha e rode de novo. Nada foi gerado.')
        sys.exit(1)
    return dados


def preparar_markdown(texto, dados):
    for chave, valor in dados.items():
        texto = texto.replace('{{%s}}' % chave, str(valor))
    # Links entre documentos viram as rotas publicadas.
    texto = texto.replace('(termos-de-uso.md)', '(/termos)')
    texto = texto.replace('(politica-de-privacidade.md)', '(/privacidade)')
    # Referências a arquivos internos do repositório não fazem sentido no ar.
    texto = re.sub(r'\[`?([^\]]+?)`?\]\(\.\./[^)]+\)', r'\1', texto)
    sobrando = re.findall(r'\{\{[A-Z_]+\}\}', texto)
    if sobrando:
        raise SystemExit('Token sem valor no texto: %s' % sorted(set(sobrando)))
    return texto


def gerar(site):
    app = os.path.join(site, 'src', 'app')
    if not os.path.isdir(app):
        raise SystemExit(
            'Não achei o site em %s.\n'
            'Passe o caminho com --site se ele estiver em outro lugar.' % site
        )

    dados = carregar_dados()
    md = markdown.Markdown(extensions=['tables', 'sane_lists', 'attr_list'])

    for pagina in PAGINAS:
        fonte = os.path.join(RAIZ, pagina['fonte'])
        with io.open(fonte, encoding='utf-8') as fh:
            texto = preparar_markdown(fh.read(), dados)
        md.reset()
        corpo = md.convert(texto)
        # Tabela larga precisa de um contêiner que role no celular.
        corpo = corpo.replace('<table>', '<div class="tabela"><table>')
        corpo = corpo.replace('</table>', '</table></div>')

        destino_dir = os.path.join(app, pagina['rota'])
        os.makedirs(destino_dir, exist_ok=True)
        destino = os.path.join(destino_dir, 'conteudo.ts')
        # JSON.stringify em vez de template literal: o texto tem crase e cifrão,
        # que quebrariam a string no TypeScript.
        conteudo = (
            '// GERADO por web/build.py no repositório do aplicativo.\n'
            '// Não edite aqui: a fonte é docs/juridico/%s\n'
            'export const HTML = %s;\n'
        ) % (os.path.basename(pagina['fonte']), json.dumps(corpo, ensure_ascii=False))
        with io.open(destino, 'w', encoding='utf-8', newline='\n') as fh:
            fh.write(conteudo)

        irmas = '\n'.join(
            '          <a href="%s">%s</a>' % (href, rotulo)
            for href, rotulo in pagina['irmas']
        )
        with io.open(os.path.join(destino_dir, 'page.tsx'), 'w',
                     encoding='utf-8', newline='\n') as fh:
            fh.write(MOLDE_PAGINA.format(
                titulo=json.dumps(pagina['titulo'], ensure_ascii=False),
                descricao=json.dumps(pagina['descricao'], ensure_ascii=False),
                componente=pagina['componente'],
                irmas=irmas,
            ))
        print('gerado: src/app/%s/ (conteudo.ts %d KB + page.tsx)'
              % (pagina['rota'], len(corpo) // 1024))

    print('\nAgora, no site:  npm run build  &&  firebase deploy --only hosting')


if __name__ == '__main__':
    p = argparse.ArgumentParser()
    p.add_argument('--site', default=SITE_PADRAO, help='pasta do site Next.js')
    gerar(p.parse_args().site)
