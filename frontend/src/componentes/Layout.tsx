import { NavLink, Outlet } from 'react-router-dom'

const TELAS = [
  { para: '/painel', rotulo: 'Painel' },
  { para: '/pontos', rotulo: 'Pontos' },
  { para: '/coletas', rotulo: 'Coletas' },
  { para: '/ocorrencias', rotulo: 'Ocorrências' },
]

/**
 * Moldura das quatro telas: cabeçalho, navegação e o conteúdo da rota.
 *
 * O `aria-current="page"` que o NavLink aplica no item ativo é o que o leitor
 * de tela anuncia; o CSS acrescenta sublinhado além da cor, para quem não
 * distingue as duas.
 */
export function Layout() {
  return (
    <>
      <a className="pular-para-conteudo" href="#conteudo">
        Ir para o conteúdo
      </a>

      <header className="app-cabecalho">
        <div className="app-cabecalho__interno">
          <p className="app-marca">
            HidroVigia
            <span className="app-marca__sufixo">
              vigilância da qualidade da água
            </span>
          </p>

          <nav className="app-nav" aria-label="Telas do sistema">
            {TELAS.map((tela) => (
              <NavLink key={tela.para} to={tela.para} className="app-nav__item">
                {tela.rotulo}
              </NavLink>
            ))}
          </nav>
        </div>
      </header>

      <main className="app-principal" id="conteudo">
        <Outlet />
      </main>
    </>
  )
}
