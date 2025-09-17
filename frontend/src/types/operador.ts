export interface Operador {
    id: string;
    nome: string;
    accessKey: string;
    secretKey: string;
}

export type CriarOperadorDTO = Omit<Operador, 'id'>;