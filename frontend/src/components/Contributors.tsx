import { useEffect, useState } from "react";
// @ts-ignore
import styles from "./Contributors.module.css";

interface Contributor {
    id: number;
    login: string;
    avatar_url: string;
    html_url: string;
}

export default function Contributors() {
    const [contributors, setContributors] = useState<Contributor[]>([]);

    useEffect(() => {
        fetch("https://api.github.com/repos/Navelogic/escreveaqui/contributors")
            .then((res) => res.json())
            .then((data) => {
                if (Array.isArray(data)) {
                    setContributors(data);
                }
            })
            .catch((err) => {
                console.error("Falha ao buscar contribuidores", err);
            });
    }, []);

    if (contributors.length === 0) return null;

    return (
        <div className={styles.container}>
            <div className={styles.title}>
                Contribuidores
            </div>
            {contributors.map((contributor) => (
                <a
                    key={contributor.id}
                    href={contributor.html_url}
                    target="_blank"
                    rel="noopener noreferrer"
                    className={styles.avatarLink}
                    title={contributor.login}
                >
                    <img
                        src={contributor.avatar_url}
                        alt={contributor.login}
                        className={styles.avatar}
                    />
                </a>
            ))}
        </div>
    );
}
