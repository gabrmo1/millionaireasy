import { createTheme } from '@mui/material/styles';
import type { ThemeOptions } from '@mui/material/styles';

const sharedThemeOptions: ThemeOptions = {
    typography: {
        fontFamily: '"Roboto", "Helvetica", "Arial", sans-serif',
        h4: {
            fontWeight: 700,
        },
    },
    shape: {
        borderRadius: 8,
    },
    components: {
        MuiButton: {
            styleOverrides: {
                root: {
                    textTransform: 'none',
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

export const lightTheme = createTheme({
    ...sharedThemeOptions,
    palette: {
        mode: 'light',
        primary: {
            main: '#1976d2',
        },
        background: {
            default: '#f4f6f8',
            paper: '#ffffff',
        },
    },
});

export const darkTheme = createTheme({
    ...sharedThemeOptions,
    palette: {
        mode: 'dark',
        primary: {
            main: '#90caf9',
        },
        background: {
            default: '#121212',
            paper: '#1e1e1e',
        },
    },
});