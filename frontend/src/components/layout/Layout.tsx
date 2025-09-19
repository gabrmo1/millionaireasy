// frontend/src/components/layout/Layout.tsx
import React from 'react';
import { alpha } from '@mui/material/styles'
import { Box, Toolbar, Drawer, List, ListItemButton, ListItemText, Divider, ListItemIcon } from '@mui/material';
import { Link } from 'react-router-dom';
import NavBar from './NavBar';

import {
    Dashboard,
    People,
    StackedLineChart,
    Settings,
} from '@mui/icons-material';

const drawerWidth = 240;

interface LayoutProps {
    children: React.ReactNode;
}

const Layout: React.FC<LayoutProps> = ({ children }) => {
    const menuItems = [
        {
            title: 'Operações',
            icon: <StackedLineChart color="success" />,
            link: '/operacoes',
        },
        {
            title: 'Operadores',
            icon: <People color="info" />,
            link: '/operadores',
        },
        {
            title: 'Estratégias',
            icon: <Settings color="warning" />,
            link: '/estrategias',
        }
    ];

    return (
        <Box sx={{ display: 'flex' }}>
            <NavBar />
            <Drawer
                variant="permanent"
                sx={{
                    width: drawerWidth,
                    flexShrink: 0,
                    [`& .MuiDrawer-paper`]: { width: drawerWidth, boxSizing: 'border-box' },
                }}
            >
                <Toolbar />
                <Box sx={{ overflow: 'auto' }}>
                    <List>
                        <ListItemButton component={Link} to="/">
                            <ListItemIcon><Dashboard color="primary" /></ListItemIcon>
                            <ListItemText primary="Painel de Controle" />
                        </ListItemButton>

                        <Divider sx={{ my: 1 }} />

                        {menuItems.map((item) => (
                            <ListItemButton key={item.title} component={Link} to={item.link}>
                                <ListItemIcon>{item.icon}</ListItemIcon>
                                <ListItemText primary={item.title} />
                            </ListItemButton>
                        ))}
                    </List>
                </Box>
            </Drawer>
            <Box
                component="main"
                sx={{
                    flexGrow: 1,
                    p: 2,
                    width: { sm: `calc(100% - ${drawerWidth}px)` },
                    background: (theme) => `linear-gradient(145deg, ${alpha(theme.palette.primary.main, 0.1)} 0%, ${theme.palette.background.default} 30%)`,
                    minHeight: '100vh',
                }}
            >
                <Toolbar />
                {children}
            </Box>
        </Box>
    );
};

export default Layout;