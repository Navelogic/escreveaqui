package br.com.escreveaqui.backend.services;

import br.com.escreveaqui.backend.utils.SlugUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class SseService {

    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String rawSlug) {
        String slug = SlugUtils.format(rawSlug);
        SseEmitter emitter = new SseEmitter(0L);

        emitters.computeIfAbsent(slug, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(slug, emitter));
        emitter.onTimeout(() -> removeEmitter(slug, emitter));
        emitter.onError((e) -> removeEmitter(slug, emitter));

        log.debug("Novo inscrito SSE para o slug='{}'", slug);
        return emitter;
    }

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
                removeEmitter(slug, emitter);
            }
        }
    }

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
