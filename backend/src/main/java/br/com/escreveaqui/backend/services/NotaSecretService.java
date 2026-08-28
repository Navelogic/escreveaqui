package br.com.escreveaqui.backend.services;

import br.com.escreveaqui.backend.exceptions.SegredoInvalidoException;
import br.com.escreveaqui.backend.models.Nota;
import br.com.escreveaqui.backend.repositories.NotaRepository;
import br.com.escreveaqui.backend.utils.SlugUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotaSecretService {

    /**
     * BCrypt custo padrão (~100ms por verificação) roda no caminho de cada leitura/escrita
     * de nota protegida. 
     * Se o auto-save de 1s pesar, cachear a validação por sessão.
     */
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    private final NotaRepository notaRepository;

    public String hashOf(String safeSlug) {
        return notaRepository.findBySlug(safeSlug).map(Nota::secretHash).orElse(null);
    }

    public void check(String hash, String secret) {
        if (hash == null) {
            return;
        }
        if (secret == null || !encoder.matches(secret, hash)) {
            throw new SegredoInvalidoException();
        }
    }

    public void verify(String rawSlug, String secret) {
        check(hashOf(SlugUtils.format(rawSlug)), secret);
    }

    public String encode(String secret) {
        return (secret == null || secret.isBlank()) ? null : encoder.encode(secret);
    }
}
