import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { HttpResponse, http } from 'msw'
import { setupServer } from 'msw/node'
import { useState } from 'react'
import { afterAll, afterEach, beforeAll, describe, expect, it } from 'vitest'
import { cadastrarPonto } from '../api/recursos'
import { Campo } from '../componentes/Campo'
import { AvisoErro } from '../componentes/Estado'
import { useErrosDeCampo } from './useErrosDeCampo'

/**
 * O teste-ponte da aplicação.
 *
 * Ele exercita a cadeia inteira de uma requisição reprovada — fetch,
 * normalização das duas formas de erro, parser de `detalhes`, distribuição por
 * campo e renderização — contra o corpo de 400 que o backend produz de
 * verdade. Um teste, e nenhum navegador.
 *
 * Vale mais que os outros de componente somados: cada elo dessa corrente falha
 * em silêncio, e o sintoma é "o botão não faz nada".
 */
const servidor = setupServer()

beforeAll(() => servidor.listen({ onUnhandledRequest: 'error' }))
afterEach(() => servidor.resetHandlers())
afterAll(() => servidor.close())

/** Formulário mínimo com dois dos campos reais do cadastro de ponto. */
function FormularioDeTeste() {
  const [erro, setErro] = useState<unknown>(null)
  const erros = useErrosDeCampo(erro, ['codigo', 'localizacao.uf'])

  return (
    <form
      onSubmit={(evento) => {
        evento.preventDefault()
        cadastrarPonto({} as never).catch(setErro)
      }}
    >
      {erros.erro && <AvisoErro erro={erros.erro} extras={erros.gerais} />}

      <Campo name="codigo" rotulo="Código" erro={erros.de('codigo')}>
        {(atributos) => <input type="text" {...atributos} />}
      </Campo>

      <Campo name="localizacao.uf" rotulo="UF" erro={erros.de('localizacao.uf')}>
        {(atributos) => <input type="text" {...atributos} />}
      </Campo>

      <button type="submit">Cadastrar</button>
    </form>
  )
}

describe('erros de validacao no formulario', () => {
  it('mostra a mensagem de cada campo ao lado do input certo', async () => {
    servidor.use(
      http.post('/api/pontos', () =>
        HttpResponse.json(
          {
            instante: '2026-09-09T12:00:00Z',
            status: 400,
            erro: 'Requisicao invalida',
            mensagem: 'Um ou mais campos nao passaram na validacao',
            detalhes: [
              'codigo: codigo do ponto e obrigatorio',
              'localizacao.uf: uf deve ter duas letras',
            ],
          },
          { status: 400 },
        ),
      ),
    )

    render(<FormularioDeTeste />)
    await userEvent.click(screen.getByRole('button', { name: 'Cadastrar' }))

    const uf = await screen.findByLabelText('UF')

    // A mensagem tem que estar ASSOCIADA ao campo, nao apenas presente na
    // tela: e o aria-describedby que faz o leitor de tela ler as duas juntas.
    expect(uf).toHaveAccessibleDescription('uf deve ter duas letras')
    expect(uf).toBeInvalid()

    expect(screen.getByLabelText('Código')).toHaveAccessibleDescription(
      'codigo do ponto e obrigatorio',
    )
  })

  it('nao deixa desaparecer o erro que nenhum campo da tela exibe', async () => {
    // "leituras: ..." tem a forma "campo: mensagem" e entraria no mapa, mas
    // nenhum input se chama assim. Sem o bucket de sobras, este 400
    // renderizaria como absolutamente nada.
    servidor.use(
      http.post('/api/pontos', () =>
        HttpResponse.json(
          {
            instante: '2026-09-09T12:00:00Z',
            status: 400,
            erro: 'Requisicao invalida',
            mensagem: 'Um ou mais campos nao passaram na validacao',
            detalhes: [
              'leituras: informe ao menos um parametro medido',
              'campoQueNaoExisteNaTela: alguma regra nova',
            ],
          },
          { status: 400 },
        ),
      ),
    )

    render(<FormularioDeTeste />)
    await userEvent.click(screen.getByRole('button', { name: 'Cadastrar' }))

    const aviso = await screen.findByRole('alert')

    expect(aviso).toHaveTextContent('informe ao menos um parametro medido')
    expect(aviso).toHaveTextContent('alguma regra nova')
    expect(screen.getByLabelText('UF')).toBeValid()
  })

  it('mostra o 409 no aviso, que nao vem com detalhes por campo', async () => {
    servidor.use(
      http.post('/api/pontos', () =>
        HttpResponse.json(
          {
            instante: '2026-09-09T12:00:00Z',
            status: 409,
            erro: 'Regra de negocio violada',
            mensagem: 'Ja existe um ponto de monitoramento com o codigo PMA-001',
            detalhes: [],
          },
          { status: 409 },
        ),
      ),
    )

    render(<FormularioDeTeste />)
    await userEvent.click(screen.getByRole('button', { name: 'Cadastrar' }))

    const aviso = await screen.findByRole('alert')

    expect(aviso).toHaveTextContent('Regra de negocio violada')
    expect(aviso).toHaveTextContent('codigo PMA-001')
    // Nenhum campo fica marcado como invalido: o 409 nao aponta campo.
    expect(screen.getByLabelText('Código')).toBeValid()
  })
})
