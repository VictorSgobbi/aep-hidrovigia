import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { Layout } from './componentes/Layout'
import { ColetasPage } from './paginas/coletas/ColetasPage'
import { OcorrenciasPage } from './paginas/ocorrencias/OcorrenciasPage'
import { PainelPage } from './paginas/painel/PainelPage'
import { PontosPage } from './paginas/pontos/PontosPage'

/**
 * As quatro telas.
 *
 * Os detalhes (ficha de um ponto, de uma ocorrência) não são rotas próprias:
 * são painéis laterais abertos por query param — `/ocorrencias?ocorrencia=<id>`.
 * Isso mantém quatro telas e ainda dá uma URL para voltar a um estado exato.
 *
 * Cada caminho aqui precisa existir também em RotasSpaConfig.java, senão um
 * refresh direto nele cai no 404 do Spring em vez de voltar para o index.html.
 */
export function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<Layout />}>
          <Route index element={<Navigate to="/painel" replace />} />
          <Route path="/painel" element={<PainelPage />} />
          <Route path="/pontos" element={<PontosPage />} />
          <Route path="/coletas" element={<ColetasPage />} />
          <Route path="/ocorrencias" element={<OcorrenciasPage />} />
          <Route path="*" element={<Navigate to="/painel" replace />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}
