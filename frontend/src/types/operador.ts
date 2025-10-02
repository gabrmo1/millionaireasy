export interface BaseEntity {
    id: string;
    [key: string]: any;
}

export interface Operador extends BaseEntity {
    id: string;
    nome: string;
    accessKey: string;
    secretKey: string;
}

export type CriarOperadorDTO = Omit<Operador, 'id'>;