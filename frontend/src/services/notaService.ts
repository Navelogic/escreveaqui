import axios from 'axios';
import type { Nota } from '../interface/nota';
import type { NotaRequest } from '../interface/notaRequest';

const api = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1/notes',
    headers: {
        'Content-Type': 'application/json',
    }
});

export const notaService = {
    async getBySlug(slug: string): Promise<Nota> {
        const response = await api.get<Nota>(`/${slug.trim()}`);
        return response.data;
    },

    async upsert(slug: string, content: string): Promise<void> {
        const payload: NotaRequest = { content };
        await api.put(`/${slug.trim()}`, payload);
    }
};