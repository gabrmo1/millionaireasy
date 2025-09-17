import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import { ThemeProvider } from './contexts/ThemeContext';

import ProtectedRoute from './components/router/ProtectedRoute';
import LoginPage from './pages/Auth/LoginPage';
import RegisterPage from './pages/Auth/RegisterPage';
import Welcome from './components/pages/Welcome';
import OperadoresPage from './pages/Operadores/OperadoresPage';
import OperadorForm from './pages/Operadores/OperadorForm';
import OperacaoForm from './pages/Operacoes/OperacaoForm';
import OperacoesPage from './pages/Operacoes/OperacoesPage';
import EstrategiasPage from './pages/Estrategias/EstrategiasPage';
import EstrategiaForm from './pages/Estrategias/EstrategiaForm';

// Componente para lidar com a rota de login quando já autenticado
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
                        {/* Rotas Públicas */}
                        <Route path="/login" element={<AuthRouteWrapper><LoginPage /></AuthRouteWrapper>} />
                        <Route path="/register" element={<AuthRouteWrapper><RegisterPage /></AuthRouteWrapper>} />

                        {/* Rotas Protegidas */}
                        <Route element={<ProtectedRoute />}>
                            <Route path="/" element={<Welcome />} />
                            <Route path="/operadores" element={<OperadoresPage />} />
                            <Route path="/operadores/novo" element={<OperadorForm />} />
                            <Route path="/operadores/editar/:id" element={<OperadorForm />} />

                            <Route path="/estrategias" element={<EstrategiasPage />} />
                            <Route path="/estrategias/novo" element={<EstrategiaForm />} />
                            <Route path="/estrategias/editar/:id" element={<EstrategiaForm />} />

                            <Route path="/operacoes" element={<OperacoesPage />} />
                            <Route path="/operacoes/novo" element={<OperacaoForm />} />
                            <Route path="/operacoes/editar/:id" element={<OperacaoForm />} />
                        </Route>

                        {/* Rota de fallback */}
                        <Route path="*" element={<Navigate to="/" replace />} />
                    </Routes>
                </Router>
            </AuthProvider>
        </ThemeProvider>
    );
}

export default App;