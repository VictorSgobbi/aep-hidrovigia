import { useEffect, useState } from 'react'

/**
 * Instante atual, atualizado periodicamente.
 *
 * Os contadores de prazo precisam andar entre dois fetches: uma ocorrência
 * crítica tem 24 horas, e a tela fica aberta durante a gravação do vídeo.
 *
 * @param intervaloMs de quanto em quanto tempo reavaliar (padrão: 30 s)
 */
export function useAgora(intervaloMs = 30_000): number {
  const [agora, setAgora] = useState(() => Date.now())

  useEffect(() => {
    const id = setInterval(() => setAgora(Date.now()), intervaloMs)
    return () => clearInterval(id)
  }, [intervaloMs])

  return agora
}
