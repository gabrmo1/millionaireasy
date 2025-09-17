import React, { createContext, useState, useMemo, type ReactNode, useEffect } from 'react';
import { ThemeProvider as MUIThemeProvider, CssBaseline } from '@mui/material';
import { lightTheme, darkTheme } from '../styles/theme';

interface ThemeContextType {
    toggleTheme: () => void;
    mode: 'light' | 'dark';
}

export const ThemeContext = createContext<ThemeContextType>({
    toggleTheme: () => {},
    mode: 'light',
});

interface ThemeProviderProps {
    children: ReactNode;
}

export const ThemeProvider: React.FC<ThemeProviderProps> = ({ children }) => {
    const getInitialMode = (): 'light' | 'dark' => {
        try {
            const storedMode = localStorage.getItem('themeMode');
            return storedMode === 'dark' ? 'dark' : 'light';
        } catch (error) {
            console.error("Could not access localStorage: ", error);
            return 'light';
        }
    };

    const [mode, setMode] = useState<'light' | 'dark'>(getInitialMode);

    useEffect(() => {
        try {
            localStorage.setItem('themeMode', mode);
        } catch (error) {
            console.error("Could not access localStorage: ", error);
        }
    }, [mode]);


    const theme = useMemo(() => (mode === 'light' ? lightTheme : darkTheme), [mode]);

    const toggleTheme = () => {
        setMode((prevMode) => (prevMode === 'light' ? 'dark' : 'light'));
    };

    return (
        <ThemeContext.Provider value={{ toggleTheme, mode }}>
            <MUIThemeProvider theme={theme}>
                <CssBaseline />
                {children}
            </MUIThemeProvider>
        </ThemeContext.Provider>
    );
};