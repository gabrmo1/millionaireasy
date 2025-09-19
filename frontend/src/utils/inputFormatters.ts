/**
 * Formata uma string para impedir zeros à esquerda em inputs numéricos,
 * seguindo as regras especificadas.
 * - Permite um único '0'.
 * - Permite '0.' para iniciar a digitação de um número decimal.
 * - Impede '00' (transforma em '0').
 * - Transforma '05' em '5'.
 * @param value O valor da string do input.
 * @returns O valor da string formatada.
 */
export const formatLeadingZeros = (value: string): string => {
    if (value === '') return '';
    if (value.startsWith('0.')) {
        return value;
    }
    if (value.startsWith('0') && value.length > 1) {
        return String(Number(value));
    }

    return value;
};