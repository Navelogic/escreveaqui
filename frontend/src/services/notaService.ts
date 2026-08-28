import axios from 'axios';
import type { Nota } from '../interface/nota';
import type { NotaRequest } from '../interface/notaRequest';

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1/notes';

const api = axios.create({
    baseURL: API_BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    }
});

/** Cabeçalhos HTTP só aceitam ASCII, então o segredo viaja em Base64. */
export function encodeSecret(secret: string): string {
    return btoa(String.fromCharCode(...new TextEncoder().encode(secret)));
}

export const notaService = {
    async getBySlug(slug: string, secret?: string | null, signal?: AbortSignal): Promise<Nota> {
        const response = await api.get<Nota>(`/${slug.trim()}`, {
            signal,
            headers: secret ? { 'X-Nota-Secret': encodeSecret(secret) } : undefined,
        });
        return response.data;
    },

    async upsert(slug: string, content: string, secret?: string | null): Promise<void> {
        const payload: NotaRequest = { content, ...(secret ? { secret } : {}) };
        await api.put(`/${slug.trim()}`, payload);
    }
};
