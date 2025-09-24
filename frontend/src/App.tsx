import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import { ThemeProvider } from './contexts/ThemeContext';
import { Box, CircularProgress, GlobalStyles } from '@mui/material';

import ProtectedRoute from './components/router/ProtectedRoute';
import LoginPage from './pages/Auth/LoginPage';
import RegisterPage from './pages/Auth/RegisterPage';
import Welcome from './components/pages/Welcome';
import OperadoresPage from './pages/Operadores/OperadoresPage';
import OperacoesPage from './pages/Operacoes/OperacoesPage';
import EstrategiasPage from './pages/Estrategias/EstrategiasPage';

const scrollbarStyles = (theme: any) => ({
    '*::-webkit-scrollbar': {
        width: '10px',
        height: '10px',
    },
    '*::-webkit-scrollbar-track': {
        backgroundColor: theme.palette.background.paper,
    },
    '*::-webkit-scrollbar-thumb': {
        backgroundColor: theme.palette.primary.main,
        borderRadius: '10px',
        border: `3px solid ${theme.palette.background.paper}`,
    },
    '*::-webkit-scrollbar-thumb:hover': {
        backgroundColor: theme.palette.primary.dark,
    },
});

const AuthRouteWrapper = ({ children }: { children: React.ReactNode }) => {
    const { isAuthenticated } = useAuth();
    return isAuthenticated ? <Navigate to="/" replace /> : <>{children}</>;
};

const AppContent = () => {
    const { isLoading } = useAuth();

    if (isLoading) {
        return (
            <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
                <CircularProgress />
            </Box>
        );
    }

    return (
        <Routes>
            <Route path="/login" element={<AuthRouteWrapper><LoginPage /></AuthRouteWrapper>} />
            <Route path="/register" element={<AuthRouteWrapper><RegisterPage /></AuthRouteWrapper>} />

            <Route element={<ProtectedRoute />}>
                <Route path="/" element={<Welcome />} />
                <Route path="/operadores" element={<OperadoresPage />} />
                <Route path="/estrategias" element={<EstrategiasPage />} />
                <Route path="/operacoes" element={<OperacoesPage />} />
            </Route>

            <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
    );
}


function App() {
    return (
        <ThemeProvider>
            <GlobalStyles styles={scrollbarStyles} />
            <AuthProvider>
                <Router>
                    <AppContent />
                </Router>
            </AuthProvider>
        </ThemeProvider>
    );
}

export default App;