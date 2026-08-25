package br.com.escreveaqui.backend.services;

import br.com.escreveaqui.backend.utils.SlugUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Serviço responsável por gerenciar transmissões em tempo real via Server-Sent Events (SSE).
 * Mantém conexões ativas agrupadas por slug de nota e dispara atualizações para os clientes.
 */
@Slf4j
@Service
public class SseService {

    /**
     * Mapa thread-safe contendo a lista de transmissores SSE ativos associados a cada slug.
     */
    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    /**
     * Inscreve um novo cliente para ouvir atualizações em tempo real de uma nota específica.
     *
     * @param rawSlug O slug da nota desejada.
     * @return O objeto {@link SseEmitter} para a conexão HTTP streaming.
     */
    public SseEmitter subscribe(String rawSlug) {
        String slug = SlugUtils.format(rawSlug);
        // Timeout 0L indica conexão aberta sem expiração prematura pelo servidor
        SseEmitter emitter = new SseEmitter(0L);

        emitters.computeIfAbsent(slug, k -> new CopyOnWriteArrayList<>()).add(emitter);

        // Callbacks de limpeza para evitar vazamento de memória (memory leak)
        emitter.onCompletion(() -> removeEmitter(slug, emitter));
        emitter.onTimeout(() -> removeEmitter(slug, emitter));
        emitter.onError((e) -> removeEmitter(slug, emitter));

        log.debug("Novo inscrito SSE para o slug='{}'", slug);
        return emitter;
    }

    /**
     * Transmite o novo conteúdo de uma nota para todos os clientes ativos inscritos naquele slug.
     *
     * @param rawSlug O slug da nota atualizada.
     * @param content O novo conteúdo da nota.
     */
    public void notify(String rawSlug, String content) {
        String slug = SlugUtils.format(rawSlug);
        List<SseEmitter> list = emitters.get(slug);
        if (list == null || list.isEmpty()) {
            return;
        }

        log.debug("Notificando {} inscrito(s) SSE para o slug='{}'", list.size(), slug);
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event()
                        .name("nota-update")
                        .data(content));
            } catch (Exception e) {
                // Em caso de falha de envio (cliente desconectado), remove o transmissor da lista
                removeEmitter(slug, emitter);
            }
        }
    }

    /**
     * Remove um transmissor inativo da lista de inscritos do slug e limpa o mapa se a lista ficar vazia.
     */
    private void removeEmitter(String slug, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(slug);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                emitters.remove(slug);
            }
        }
        log.debug("Inscrito SSE removido para o slug='{}'", slug);
    }
}
