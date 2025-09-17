import React, { useState, useContext } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { ThemeContext } from '../../contexts/ThemeContext';
import {
    Box,
    Button,
    Container,
    TextField,
    Typography,
    Alert,
    CircularProgress,
    InputAdornment,
    IconButton,
    Snackbar,
} from '@mui/material';
import {
    PersonOutline,
    LockOutlined,
    Visibility,
    VisibilityOff,
    Google as GoogleIcon,
    Brightness4,
    Brightness7
} from '@mui/icons-material';
import { styled } from '@mui/material/styles';

// Componente estilizado para o Container principal
const AuthContainer = styled(Container)(({ theme }) => ({
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: '100vh',
    position: 'relative',
    background: theme.palette.mode === 'dark'
        ? 'linear-gradient(135deg, #1a237e 0%, #121212 100%)'
        : 'linear-gradient(135deg, #1a237e 0%, #fff 100%)',
}));

// Componente estilizado para o Card de autenticação
const AuthCard = styled(Box)(({ theme }) => ({
    padding: theme.spacing(6),
    // @ts-ignore
    borderRadius: theme.shape.borderRadius * 3,
    backgroundColor: theme.palette.background.paper,
    boxShadow: theme.shadows[10],
    maxWidth: '450px',
    width: '100%',
    textAlign: 'center',
    position: 'relative',
    overflow: 'hidden',
    border: `1px solid ${theme.palette.divider}`,
}));

// Componente estilizado para os campos de texto
const StyledTextField = styled(TextField)(({ theme }) => ({
    marginBottom: theme.spacing(3),
    '& .MuiOutlinedInput-root': {
        // @ts-ignore
        borderRadius: theme.shape.borderRadius * 2,
        '& fieldset': {
            borderColor: theme.palette.grey[400],
        },
        '&:hover fieldset': {
            borderColor: theme.palette.primary.main,
        },
        '&.Mui-focused fieldset': {
            borderColor: theme.palette.primary.dark,
            borderWidth: 2,
        },
    },
    '& .MuiInputLabel-root': {
        color: theme.palette.text.secondary,
    },
}));

const RegisterPage: React.FC = () => {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [apiError, setApiError] = useState<string | null>(null);
    const [errors, setErrors] = useState({ email: '', password: '', confirmPassword: '' });
    const [loading, setLoading] = useState(false);
    const [showPassword, setShowPassword] = useState(false);
    const [showConfirmPassword, setShowConfirmPassword] = useState(false);
    const [snackbarOpen, setSnackbarOpen] = useState(false);

    const { register } = useAuth();
    const navigate = useNavigate();
    const { mode, toggleTheme } = useContext(ThemeContext);

    const validateForm = () => {
        const newErrors = { email: '', password: '', confirmPassword: '' };
        let isValid = true;

        // Validação de E-mail
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(email)) {
            newErrors.email = 'Por favor, insira um e-mail válido.';
            isValid = false;
        }

        // Validação de Senha Forte
        const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/;
        if (password.length < 8) {
            newErrors.password = 'A senha deve ter no mínimo 8 caracteres.';
            isValid = false;
        } else if (!passwordRegex.test(password)) {
            newErrors.password = 'A senha deve conter maiúscula, minúscula, número e caractere especial.';
            isValid = false;
        }

        // Validação de Confirmação de Senha
        if (password !== confirmPassword) {
            newErrors.confirmPassword = 'As senhas não coincidem.';
            isValid = false;
        }

        setErrors(newErrors);
        return isValid;
    };


    const handleSubmit = async (event: React.FormEvent) => {
        event.preventDefault();
        setApiError(null);

        if (!validateForm()) {
            return;
        }

        setLoading(true);
        try {
            await register({ email, password });
            navigate('/');
        } catch (err: any) {
            const message = err.response?.data?.message || 'Falha ao registrar. Tente outro e-mail.';
            setApiError(message);
            setSnackbarOpen(true);
        } finally {
            setLoading(false);
        }
    };

    const handleSnackbarClose = (_event?: React.SyntheticEvent | Event, reason?: string) => {
        if (reason === 'clickaway') {
            return;
        }
        setSnackbarOpen(false);
    };

    const handleChange = (setter: React.Dispatch<React.SetStateAction<string>>, field: keyof typeof errors) => (event: React.ChangeEvent<HTMLInputElement>) => {
        setter(event.target.value);
        if (errors[field]) {
            setErrors(prev => ({ ...prev, [field]: '' }));
        }
    };

    return (
        <AuthContainer disableGutters maxWidth={false}>
            <Box sx={{ position: 'absolute', top: 16, right: 16 }}>
                <IconButton sx={{ ml: 1, color: mode === 'light' ? '#000' : '#fff' }} onClick={toggleTheme}>
                    {mode === 'dark' ? <Brightness7 /> : <Brightness4 />}
                </IconButton>
            </Box>

            <Snackbar
                open={snackbarOpen}
                autoHideDuration={6000}
                onClose={handleSnackbarClose}
                anchorOrigin={{ vertical: 'top', horizontal: 'center' }}
            >
                <Alert onClose={handleSnackbarClose} severity="error" variant="filled" sx={{ width: '100%' }}>
                    {apiError}
                </Alert>
            </Snackbar>

            <AuthCard>
                <Typography component="h1" variant="h4" color="primary" sx={{ mb: 2, fontWeight: 'bold' }}>
                    Millionaireasy
                </Typography>
                <Typography variant="h6" color="text.secondary" sx={{ mb: 4 }}>
                    Crie sua conta
                </Typography>

                <Box component="form" onSubmit={handleSubmit} noValidate>
                    <StyledTextField
                        required
                        fullWidth
                        label="E-mail"
                        name="email"
                        autoComplete="email"
                        autoFocus
                        value={email}
                        onChange={handleChange(setEmail, 'email')}
                        error={!!errors.email}
                        helperText={errors.email || ' '}
                        InputProps={{
                            startAdornment: (
                                <InputAdornment position="start">
                                    <PersonOutline color="primary" />
                                </InputAdornment>
                            ),
                        }}
                    />
                    <StyledTextField
                        required
                        fullWidth
                        name="password"
                        label="Senha"
                        type={showPassword ? 'text' : 'password'}
                        value={password}
                        onChange={handleChange(setPassword, 'password')}
                        error={!!errors.password}
                        helperText={errors.password || ' '}
                        InputProps={{
                            startAdornment: (
                                <InputAdornment position="start">
                                    <LockOutlined color="primary" />
                                </InputAdornment>
                            ),
                            endAdornment: (
                                <InputAdornment position="end">
                                    <IconButton
                                        aria-label="toggle password visibility"
                                        onClick={() => setShowPassword(!showPassword)}
                                        onMouseDown={(event) => event.preventDefault()}
                                        edge="end"
                                    >
                                        {showPassword ? <VisibilityOff /> : <Visibility />}
                                    </IconButton>
                                </InputAdornment>
                            ),
                        }}
                    />
                    <StyledTextField
                        required
                        fullWidth
                        name="confirmPassword"
                        label="Confirmar Senha"
                        type={showConfirmPassword ? 'text' : 'password'}
                        value={confirmPassword}
                        onChange={handleChange(setConfirmPassword, 'confirmPassword')}
                        error={!!errors.confirmPassword}
                        helperText={errors.confirmPassword || ' '}
                        InputProps={{
                            startAdornment: (
                                <InputAdornment position="start">
                                    <LockOutlined color="primary" />
                                </InputAdornment>
                            ),
                            endAdornment: (
                                <InputAdornment position="end">
                                    <IconButton
                                        aria-label="toggle confirm password visibility"
                                        onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                                        onMouseDown={(event) => event.preventDefault()}
                                        edge="end"
                                    >
                                        {showConfirmPassword ? <VisibilityOff /> : <Visibility />}
                                    </IconButton>
                                </InputAdornment>
                            ),
                        }}
                    />

                    <Button
                        type="submit"
                        fullWidth
                        variant="contained"
                        size="large"
                        disabled={loading}
                        sx={{
                            mb: 2, borderRadius: 2,
                            py: 1.5,
                            fontSize: '1.1rem',
                            fontWeight: 'bold',
                            boxShadow: theme => theme.shadows[4],
                            '&:hover': {
                                boxShadow: theme => theme.shadows[6],
                            }
                        }}
                    >
                        {loading ? <CircularProgress size={24} color="inherit" /> : 'Registrar'}
                    </Button>

                    <Button
                        fullWidth
                        variant="outlined"
                        size="large"
                        startIcon={<GoogleIcon sx={{ color: 'primary.main' }} />}
                        sx={{
                            mb: 3, borderRadius: 2,
                            py: 1.5,
                            fontSize: '1.1rem',
                            fontWeight: 'bold',
                            borderColor: theme => theme.palette.grey[400],
                            color: theme => theme.palette.text.primary,
                            '&:hover': {
                                borderColor: theme => theme.palette.primary.main,
                                backgroundColor: theme => theme.palette.action.hover,
                            }
                        }}
                    >
                        Registrar com Google
                    </Button>

                    <Box sx={{ display: 'flex', justifyContent: 'center', mt: 2 }}>
                        <Link to="/login" style={{ textDecoration: 'none' }}>
                            <Typography variant="body2" sx={{
                                color: 'primary.main',
                                '&:hover': {
                                    textDecoration: 'underline'
                                }
                            }}>
                                Já tem uma conta? <span style={{ fontWeight: 'bold' }}>Faça o login</span>
                            </Typography>
                        </Link>
                    </Box>
                </Box>
            </AuthCard>
        </AuthContainer>
    );
};

export default RegisterPage;