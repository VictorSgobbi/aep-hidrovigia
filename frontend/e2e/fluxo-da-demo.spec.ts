import { expect, test, type Page } from '@playwright/test'

/**
 * O fluxo que o vídeo da entrega demonstra, ponta a ponta, contra o jar
 * empacotado: registrar uma coleta contaminada, ver a ocorrência crítica que o
 * sistema abre sozinho, tratá-la e encerrá-la.
 *
 * As asserções são **relativas**: o teste lê a contagem de pendências antes de
 * começar e verifica que ela sobe e volta. Fixar "3 pendências" faria o
 * cenário passar uma vez e falhar na segunda, porque ele grava no banco — e um
 * teste que só roda numa base virgem não é rodado durante os ensaios, que é
 * exatamente quando ele tem mais valor.
 */

/** Lê o número de um cartão do painel pelo rótulo. */
async function lerIndicador(page: Page, rotulo: string): Promise<number> {
  const cartao = page.getByRole('listitem').filter({ hasText: rotulo })
  const texto = await cartao.locator('.indicador__numero').innerText()
  // Formato pt-BR: o separador de milhar é ponto.
  return Number(texto.replace(/\./g, '').trim())
}

async function pendenciasNoPainel(page: Page): Promise<number> {
  await page.goto('/painel')
  await expect(
    page.getByRole('heading', { name: 'Painel de conformidade' }),
  ).toBeVisible()
  return lerIndicador(page, 'Pendências abertas')
}

test('registrar coleta contaminada abre, trata e encerra a ocorrencia', async ({
  page,
}) => {
  const pendenciasAntes = await pendenciasNoPainel(page)

  // 1. Registrar a coleta ------------------------------------------------
  await page.goto('/coletas')

  // O value da opcao e o codigo do ponto.
  await page.getByLabel('Ponto de coleta').selectOption('PMA-003')
  await page.getByLabel('Quem coletou').fill('Tecnico Bruno')

  // Uma linha por parâmetro do catálogo da norma. A de E. coli é a primeira
  // porque a API devolve os microbiológicos primeiro.
  const linhaEcoli = page
    .getByRole('listitem')
    .filter({ hasText: 'Escherichia coli' })
  await linhaEcoli.getByRole('checkbox').check()
  // 14 UFC/100mL: o valor precisa ser aceito mesmo com limiteMaximo 0 no
  // catálogo — é o que um atributo `max` derivado do limite impediria.
  await linhaEcoli.getByLabel(/Valor medido/).fill('14')
  await expect(linhaEcoli).toContainText('Prévia: fora do limite')

  const linhaColiformes = page
    .getByRole('listitem')
    .filter({ hasText: 'Coliformes totais' })
  await linhaColiformes.getByRole('checkbox').check()
  await linhaColiformes.getByLabel(/Valor medido/).fill('62')

  await page.getByRole('button', { name: 'Registrar coleta' }).click()

  // 2. O veredito do servidor -------------------------------------------
  const veredito = page.getByRole('status')
  await expect(veredito).toContainText('Não conforme')
  await expect(veredito).toContainText('PMA-003')
  await expect(veredito).toContainText(
    'Uma ocorrência foi aberta automaticamente',
  )
  // Gravidade pelo pior risco da amostra: microbiológico -> crítica, 24 h.
  await expect(veredito).toContainText('Crítica')
  await expect(veredito).toContainText('24 h')

  // 3. Seguir para a ocorrência que o sistema abriu ----------------------
  await veredito
    .getByRole('link', { name: 'Abrir na fila de pendências' })
    .click()

  await expect(page).toHaveURL(/\/ocorrencias\?ocorrencia=/)
  const urlDaOcorrencia = page.url()

  const ficha = page.getByRole('complementary')
  await expect(ficha).toContainText('Crítica')
  await expect(ficha).toContainText('Escherichia coli')
  await expect(ficha).toContainText('ausencia (UFC/100mL)')
  // Rastreabilidade da classificação.
  await expect(ficha).toContainText('pior risco sanitário da amostra')
  await expect(ficha).toContainText('Nenhuma ação registrada ainda.')

  // 4. A pendência entrou na conta do painel ----------------------------
  expect(await pendenciasNoPainel(page)).toBe(pendenciasAntes + 1)

  // 5. Registrar duas tratativas sucessivas -----------------------------
  // O deep link volta ao estado exato, que é o que permite retomar a gravação.
  await page.goto(urlDaOcorrencia)

  for (const acao of [
    'Ponto isolado da rede de distribuicao',
    'Recloracao do reservatorio executada',
  ]) {
    await page.getByLabel('O que foi feito').fill(acao)
    await page.getByLabel('Responsável').fill('Leonardo')
    await page.getByRole('button', { name: 'Registrar ação' }).click()
    // O campo é limpo e o formulário continua disponível: o domínio aceita
    // quantas ações forem necessárias antes de encerrar.
    await expect(page.getByLabel('O que foi feito')).toHaveValue('')
  }

  const fichaAtualizada = page.getByRole('complementary')
  await expect(fichaAtualizada).toContainText('Em tratativa')
  await expect(fichaAtualizada).toContainText('Ponto isolado da rede')
  await expect(fichaAtualizada).toContainText('Recloracao do reservatorio')

  // 6. Encerrar, com confirmação ----------------------------------------
  await page.getByLabel('O que foi feito').fill('Contraprova conforme')
  await page.getByLabel('Responsável').fill('Victor')
  await page.getByRole('button', { name: 'Encerrar ocorrência' }).click()

  // O primeiro clique só pede confirmação, porque não existe reabertura.
  await expect(page.getByRole('alert')).toContainText(
    'não pode ser reaberta',
  )
  await page.getByRole('button', { name: 'Confirmar encerramento' }).click()

  await expect(page.getByText(/Ocorrência encerrada/)).toBeVisible()
  // A ação que a API recusaria com 409 desaparece da tela.
  await expect(page.getByLabel('O que foi feito')).toBeHidden()

  // 7. O painel voltou ao número de antes -------------------------------
  expect(await pendenciasNoPainel(page)).toBe(pendenciasAntes)
})

test('um refresh numa rota da interface carrega a aplicacao, e nao um 404', async ({
  page,
}) => {
  // No jar, /ocorrencias é uma requisição de verdade para o Spring. Sem o
  // encaminhamento do RotasSpaConfig, um F5 devolveria 404 — e é o tipo de
  // coisa que só se descobre gravando.
  const resposta = await page.goto('/ocorrencias')

  expect(resposta?.status()).toBe(200)
  await expect(page.getByRole('heading', { name: 'Ocorrências' })).toBeVisible()
  // A aplicação realmente inicializou, e não apenas devolveu o HTML.
  await expect(page.getByRole('tab', { name: 'Pendentes' })).toHaveAttribute(
    'aria-selected',
    'true',
  )
})
