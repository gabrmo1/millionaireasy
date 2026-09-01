import React, { useState } from 'react';
import type { GridRenderCellParams } from '@mui/x-data-grid';
import { Chip, IconButton, Tooltip, CircularProgress } from '@mui/material';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import StopIcon from '@mui/icons-material/Stop';
import { StatusOperacoes } from '../types/operacao';
import { iniciarOperacao, pararOperacao } from '../services/operacaoService';

export const StatusChipRenderer = (params: GridRenderCellParams) => {
    const status = params.value as string;
    let color: 'default' | 'primary' | 'secondary' | 'error' | 'info' | 'success' | 'warning' = 'default';

    switch (status) {
        case 'AGUARDANDO':
            color = 'info';
            break;
        case StatusOperacoes.EM_ANDAMENTO:
            color = 'success';
            break;
        case StatusOperacoes.PARADO:
            color = 'warning';
            break;
        case 'ERRO':
            color = 'error';
            break;
        case StatusOperacoes.FINALIZADO:
            color = 'default';
            break;
    }

    return <Chip label={status} color={color} size="small" sx={{ fontWeight: 'bold' }} />;
};

export const BooleanRenderer = (params: GridRenderCellParams) => {
    return params.value ? "Sim" : "Não";
};

interface StartStopActionProps {
    params: GridRenderCellParams;
    onRefresh?: () => void;
}

export const StartStopAction = ({ params, onRefresh }: StartStopActionProps) => {
    const [loading, setLoading] = useState(false);
    const status = params.row.status;
    const id = params.row.id;

    const handleToggle = async (e: React.MouseEvent) => {
        e.stopPropagation();
        setLoading(true);
        try {
            if (status === StatusOperacoes.PARADO) {
                await iniciarOperacao(id);
            } else if (status === StatusOperacoes.EM_ANDAMENTO) {
                await pararOperacao(id);
            }

            if (onRefresh) {
                onRefresh();
            } else {
                window.location.reload();
            }
        } catch (error) {
            console.error("Erro ao alterar status", error);
        } finally {
            setLoading(false);
        }
    };

    if (status === StatusOperacoes.FINALIZADO) {
        return null;
    }

    if (loading) {
        return <CircularProgress size={24} />;
    }

    const isRunning = status === StatusOperacoes.EM_ANDAMENTO;

    return (
        <Tooltip title={isRunning ? "Parar Operação" : "Iniciar Operação"}>
            <IconButton
                onClick={handleToggle}
                color={isRunning ? "error" : "success"}
                size="small"
            >
                {isRunning ? <StopIcon /> : <PlayArrowIcon />}
            </IconButton>
        </Tooltip>
    );
};

export const StartStopActionRenderer = (params: GridRenderCellParams) => {
    return <StartStopAction params={params} />;
};