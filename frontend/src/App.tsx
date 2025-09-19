// frontend/src/App.tsx
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import { ThemeProvider } from './contexts/ThemeContext';

import ProtectedRoute from './components/router/ProtectedRoute';
import LoginPage from './pages/Auth/LoginPage';
import RegisterPage from './pages/Auth/RegisterPage';
import Welcome from './components/pages/Welcome';
import OperadoresPage from './pages/Operadores/OperadoresPage';
import OperacoesPage from './pages/Operacoes/OperacoesPage';
import EstrategiasPage from './pages/Estrategias/EstrategiasPage';

const AuthRouteWrapper = ({ children }: { children: React.ReactNode }) => {
    const { isAuthenticated } = useAuth();
    return isAuthenticated ? <Navigate to="/" replace /> : <>{children}</>;
};

function App() {
    return (
        <ThemeProvider>
            <AuthProvider>
                <Router>
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
                </Router>
            </AuthProvider>
        </ThemeProvider>
    );
}

export default App;