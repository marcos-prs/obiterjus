# Páginas jurídicas do ObiterJus

A Google Play exige, fora do aplicativo, uma Política de Privacidade acessível por URL e
um caminho de exclusão de conta. As páginas vivem no **site do app**, não aqui:

```
C:\Users\marcos\Xablau Iniciative\APPS\ObiterJus\site
```

Next.js com `output: 'export'`, publicado no Firebase Hosting do mesmo projeto `obiterjus`,
em `https://obiterjus.web.app`.

| Rota | Origem | Situação em 06/11/2026 |
| --- | --- | --- |
| `/exclusao-de-conta` | `src/app/exclusao-de-conta/page.tsx`, escrita à mão | **No ar** |
| `/privacidade` | gerada de `docs/juridico/politica-de-privacidade.md` | No ar com o **texto curto antigo** |
| `/termos` | gerada de `docs/juridico/termos-de-uso.md` | Não existe ainda |

## O que esta pasta faz

Só `build.py` e os dados que ele consome. A fonte da verdade dos dois documentos longos é
o markdown em `docs/juridico/`; `build.py` o converte e escreve, dentro do site,
`src/app/<rota>/conteudo.ts` **e** `src/app/<rota>/page.tsx`.

As duas páginas são geradas juntas de propósito: assim nunca existe no site uma `page.tsx`
importando um `conteudo.ts` que o gate de dados impediu de nascer — o que quebraria o
`npm run build`.

## Como publicar

1. Preencher os campos vazios de `dados-do-controlador.json`. O `build.py` **recusa gerar**
   enquanto faltar qualquer um — de propósito: documento jurídico com lacuna no ar é pior
   do que documento ausente.
2. `python web/build.py`
3. No site: `npm run build && firebase deploy --only hosting`

Vale publicar antes num canal de preview, que não toca a produção:

```
firebase hosting:channel:deploy preview-juridico --project obiterjus --expires 2d
```

## Armadilhas já pagas

- **O `firebase.json` tinha `rewrite` de `**` para `/index.html`.** Toda rota inexistente
  devolvia a home com **200** — `/termos` e `/exclusao-de-conta` "existiam" sem existir.
  Num site de exportação estática esse rewrite não serve para nada e esconde erro; foi
  removido, e agora o `404.html` do próprio Next responde. Se alguém reintroduzir isso,
  a URL de exclusão passa a enganar a revisão da Play.
- **Três e-mails de contato circulavam**: `suporte@obiterjus.com.br` na política antiga do
  site, `contato@xablauinitiative.com.br` no Hub e `obiterjus@xablauinitiative.com.br` nos
  documentos de `docs/juridico/`. As páginas novas usam o dos documentos. Falta decidir
  qual é o oficial e alinhar os três.
- **`obiterjus.duckdns.org` não é o site**, é a API Obiter-Minuta. O `BASE_URL` em
  `ObiterMinutaRetrofitFactory.kt` aponta para lá e deve continuar apontando. Uma versão
  anterior deste README dizia que as páginas eram servidas por aquele host; não são.
  Sobrou lá um `/exclusao-de-conta` publicado por engano, mais a configuração de Caddy que
  o serve — vale remover ou redirecionar para o site.

## Detalhe que se perde

A URL publicada tem de continuar batendo com `url_exclusao_conta`, em
`app/src/main/res/values/strings.xml` — é o link que o próprio aplicativo abre.
