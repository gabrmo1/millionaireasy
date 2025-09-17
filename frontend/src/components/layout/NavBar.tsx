import React, { useContext } from 'react';
import { AppBar, Toolbar, Typography, IconButton, Box, Button } from '@mui/material';
import { Brightness4, Brightness7, AutoGraph, Logout } from '@mui/icons-material';
import { ThemeContext } from '../../contexts/ThemeContext';
import { useAuth } from '../../contexts/AuthContext';

const NavBar: React.FC = () => {
    const { mode, toggleTheme } = useContext(ThemeContext);
    const { logout } = useAuth();

    return (
        <AppBar position="fixed" elevation={1} sx={{
            backgroundColor: (theme) => theme.palette.background.paper,
            color: (theme) => theme.palette.text.primary,
            zIndex: (theme) => theme.zIndex.drawer + 1,
        }}>
            <Toolbar>
                <AutoGraph sx={{ mr: 1, color: 'primary.main' }} />
                <Typography variant="h6" component="div" sx={{ flexGrow: 1, fontWeight: 'bold' }}>
                    Millionaireasy
                </Typography>

                <Box>
                    <IconButton sx={{ ml: 1 }} onClick={toggleTheme} color="inherit">
                        {mode === 'dark' ? <Brightness7 /> : <Brightness4 />}
                    </IconButton>
                    <Button
                        color="inherit"
                        startIcon={<Logout />}
                        onClick={logout}
                    >
                        Sair
                    </Button>
                </Box>
            </Toolbar>
        </AppBar>
    );
};

export default NavBar;