import { useState, type SyntheticEvent } from "react";
import { useNavigate } from "react-router-dom";
// @ts-ignore
import styles from "./styles.module.css";
import Modal from "../../components/Modal";
import Contributors from "../../components/Contributors";

export default function Home() {
  const [path, setPath] = useState("");
  const [activeModal, setActiveModal] = useState<'privacy' | 'cookies' | null>(null);
  const navigate = useNavigate();


  const handleSubmit = (e: SyntheticEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!path.trim()) return;
    navigate("/" + path);
  };

  return (
    <div className={styles.wrapper}>
      <div className={styles.content}>
        <h1 className={styles.title}>
          escreve <span className={styles.highlight}>aqui</span>
        </h1>

        <h2 className={styles.subtitle}>
          <a
            href="https://github.com/Navelogic/escreveaqui"
            target="_blank"
            rel="noopener noreferrer"
            className={styles.sourceLink}
          >
            Código aberto
          </a> • Feito no Brasil 🇧🇷
        </h2>

        <Contributors />

        <form onSubmit={handleSubmit} className={styles.form}>
          <span className={styles.prefix}>
            {window.location.host}/
          </span>

          <input
            value={path}
            onChange={(e) => setPath(e.target.value)}
            placeholder="minha-nota"
            className={styles.input}
          />

          <button type="submit" className={styles.button}>
            criar
          </button>
        </form>
      </div>

      <div className={styles.footer}>
        <button className={styles.footerLink} onClick={() => setActiveModal('privacy')}>
          Política de Privacidade
        </button>
        <button className={styles.footerLink} onClick={() => setActiveModal('cookies')}>
          Política de Cookies
        </button>
      </div>

      <Modal
        isOpen={activeModal === 'privacy'}
        onClose={() => setActiveModal(null)}
        title="Política de Privacidade"
      >
        <p>
          O <strong>escreveaqui</strong> é um serviço minimalista e focado na privacidade.
        </p>
        <ul>
          <li>Não solicitamos informações pessoais como nome, email ou telefone.</li>
          <li>O conteúdo das notas é armazenado associado apenas à URL que você criou.</li>
          <li>Qualquer pessoa com acesso à URL da nota poderá ler e editar seu conteúdo.</li>
          <li>Recomendamos não armazenar informações sensíveis (senhas, dados bancários, etc).</li>
        </ul>
      </Modal>

      <Modal
        isOpen={activeModal === 'cookies'}
        onClose={() => setActiveModal(null)}
        title="Política de Cookies"
      >
        <p>
          Utilizamos cookies e armazenamento local apenas para funcionalidades essenciais do sistema.
        </p>
        <ul>
          <li>Não utilizamos cookies de rastreamento ou publicidade.</li>
          <li>Podemos usar LocalStorage para salvar suas preferências de uso (como tema ou última nota acessada).</li>
        </ul>
      </Modal>
    </div>
  );
}
