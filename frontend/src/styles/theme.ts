import { createTheme } from '@mui/material/styles';
import type { ThemeOptions } from '@mui/material/styles';

// Opções compartilhadas para ambos os temas (tipografia, formas, etc.)
const sharedThemeOptions: ThemeOptions = {
    typography: {
        fontFamily: '"Roboto", "Helvetica", "Arial", sans-serif',
        h4: { fontWeight: 700 },
        h5: { fontWeight: 600 },
        h6: { fontWeight: 600 },
    },
    shape: {
        borderRadius: 12, // Bordas um pouco mais arredondadas para um look moderno
    },
    components: {
        MuiButton: {
            styleOverrides: {
                root: {
                    textTransform: 'none',
                    fontWeight: 'bold',
                },
            },
        },
        MuiFab: {
            styleOverrides: {
                root: {
                    textTransform: 'none',
                },
            },
        },
    },
};

// TEMA CLARO
export const lightTheme = createTheme({
    ...sharedThemeOptions,
    palette: {
        mode: 'light',
        primary: {
            main: '#00796B', // Um verde-azulado (teal) mais sóbrio
            light: '#48a999',
            dark: '#004c40',
        },
        secondary: {
            main: '#FF8F00', // Âmbar para destaque
            light: '#ffc046',
            dark: '#c56000',
        },
        background: {
            default: '#F4F7F9', // Um cinza quase branco muito claro
            paper: '#FFFFFF',
        },
        text: {
            primary: '#2A3342', // Cinza escuro ao invés de preto puro
            secondary: '#5A6981',
        },
        success: { main: '#2E7D32' },
        error: { main: '#D32F2F' },
        warning: { main: '#ED6C02' },
        info: { main: '#0288D1' },
    },
});

// TEMA ESCURO
export const darkTheme = createTheme({
    ...sharedThemeOptions,
    palette: {
        mode: 'dark',
        primary: {
            main: '#4DD0E1', // Ciano vibrante
            light: '#88ffff',
            dark: '#009faf',
        },
        secondary: {
            main: '#FFC107', // Âmbar/Dourado
            light: '#fff350',
            dark: '#c79100',
        },
        background: {
            default: '#010409',   // Um tom quase preto, similar ao do GitHub
            paper: '#0D1117',     // Um cinza-azulado muito escuro
        },
        text: {
            primary: '#E0E1DD', // Branco com um leve tom quente
            secondary: '#A8B2C2',
        },
        success: { main: '#66BB6A' },
        error: { main: '#EF5350' },
        warning: { main: '#FFA726' },
        info: { main: '#29B6F6' },
    },
});