import axios from 'axios';
import type { Nota } from '../interface/nota';
import type { NotaRequest } from '../interface/notaRequest';

const api = axios.create({
    baseURL: "https://estefana-tungstous-layton.ngrok-free.dev/api/v1/notes", 
    headers: {
        'Content-Type': 'application/json',
        'ngrok-skip-browser-warning': 'true'
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